package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.utilities.mediaWiki.MediaWikiParser;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.hadoop.record.CsvRecordInput;
import org.lmdbjava.Cursor;
import org.lmdbjava.SeekOp;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import static com.scienceminer.nerd.kb.db.KBEnvironment.deserialize;
import static java.nio.ByteBuffer.allocateDirect;

/**
 * A {@link KBDatabase} for associating page ids with page markup.
 */
public class MarkupDatabase extends KBDatabase<Integer, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KBDatabase.class);

    // by default we only store the first paragraph of the wiki text associated to a page,
    // not the full markup content
    private boolean full = false;

    private enum DumpTag {page, id, text, ignorable}

    ;

    public MarkupDatabase(KBEnvironment env) {
        super(env, DatabaseType.markup);
    }

    public MarkupDatabase(KBEnvironment env, DatabaseType type) {
        super(env, type);
        if (type == DatabaseType.markupFull) {
            full = true;
        } else {
            full = false;
        }
    }

    @Override
    public KBEntry<Integer, String> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadFromFile(File dataFile, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException();
    }

    // using standard LMDB copy mode
    @Override
    public String retrieve(Integer key) {
        final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
        ByteBuffer cachedData = null;
        String record = null;
        try (Txn<ByteBuffer> tx = environment.txnRead()) {
            keyBuffer.put(KBEnvironment.serialize(key)).flip();
            cachedData = db.get(tx, keyBuffer);
            if (cachedData != null) {
                record = (String) KBEnvironment.deserialize(cachedData);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve key " + key, e);
        }
        return record;
    }

    // using LMDB zero copy mode
    //@Override
    public String retrieve2(Integer key) {
        final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
        String record = null;
        try (Txn<ByteBuffer> tx = environment.txnRead();
             final Cursor<ByteBuffer> cursor = db.openCursor(tx)) {

            keyBuffer.put(KBEnvironment.serialize(key)).flip();
            if (cursor.seek(SeekOp.MDB_FIRST)) {
                record = (String) deserialize(cursor.val());
            }
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve key " + key, e);
        }
        return record;
    }

    /**
     * Builds the persistent markup database from th Wikipedia XML article dump
     *
     * @param dataFile  the XML wikipedia dump
     * @param overwrite true if the existing database should be overwritten, otherwise false
     */
    public void loadFromXmlFile(File dataFile, boolean overwrite) throws Exception {
        if (isLoaded && !overwrite)
            return;
        if (dataFile == null)
            throw new NerdResourceException("Markup file not found");
        System.out.println("Loading " + getName() + " database");

        Integer currId = null;
        String currMarkup = null;
        StringBuffer characters = new StringBuffer();

        InputStream reader;
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        if (dataFile.getName().endsWith(".bz2")) {
            FileInputStream fis = new FileInputStream(dataFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
            reader = input;
        } else {
            reader = new FileInputStream(dataFile);
        }

        XMLInputFactory xmlStreamFactory = XMLInputFactory.newInstance();
        CountingInputStream countingReader = new CountingInputStream(reader);
        XMLStreamReader xmlStreamReader = xmlStreamFactory.createXMLStreamReader(new InputStreamReader(countingReader, decoder));

        LowerKnowledgeBase wikipedia = null;
        if (full) {
            wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf(env.getConfiguration().getLangCode());
        }

        int nbToAdd = 0;
        int totalAdded = 0;
        boolean isArticle = false;
        Txn<ByteBuffer> tx = environment.txnWrite();
        while (xmlStreamReader.hasNext()) {
            int eventCode = xmlStreamReader.next();
            switch (eventCode) {
                case XMLStreamReader.START_ELEMENT:
                    switch (resolveDumpTag(xmlStreamReader.getLocalName())) {
                        case page: // nothing to do
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (resolveDumpTag(xmlStreamReader.getLocalName())) {
                        case id:
                            //only take the first id (there is a second one for the revision)
                            if (currId == null) {
                                currId = Integer.parseInt(characters.toString().trim());
                                if (full && (currId != null) && (wikipedia != null)) {
                                    Page page = wikipedia.getPageById(currId.intValue());
                                    if (page.getType() == Page.PageType.article)
                                        isArticle = true;
                                }
                            }
                            break;
                        case text:
                            currMarkup = characters.toString().trim();
                            break;
                        case page:
                            if (nbToAdd == 1000) {
                                tx.commit();
                                tx.close();
                                nbToAdd = 0;
                                tx = environment.txnWrite();
                                System.out.println(totalAdded + " / " + currId);
                            }
                            if (full && isArticle) {
                                // we store the complete text if we have an article
                                currMarkup = MediaWikiParser.getInstance().formatAllWikiText(currMarkup,
                                        env.getConfiguration().getLangCode());
                                // we don't consider articles when too short or too long
                                if ((currMarkup != null) && ((currMarkup.length() < 500) || (currMarkup.length() > 50000))) {
                                    currMarkup = null;
                                }

                            } else if (!full) {
                                // we only store the first paragraph/summary
                                currMarkup = MediaWikiParser.getInstance().formatFirstParagraphWikiText(currMarkup,
                                        env.getConfiguration().getLangCode());
                            } else {
                                // full and not article: we don't store that
                                currMarkup = null;
                            }

                            if ((currMarkup != null) && (currMarkup.trim().length() > 5)) {
                                try {
                                    final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
                                    keyBuffer.put(KBEnvironment.serialize(currId)).flip();
                                    final byte[] serializedValue = KBEnvironment.serialize(currMarkup);
                                    final ByteBuffer valBuffer = allocateDirect(serializedValue.length);
                                    valBuffer.put(serializedValue).flip();
                                    db.put(tx, keyBuffer, valBuffer);
                                    nbToAdd++;
                                    totalAdded++;
                                } catch (Exception e) {
                                    LOGGER.error("Markup addition failed: " + currId + " / " + currMarkup, e);
                                }
                            }

                            currId = null;
                            currMarkup = null;
                            isArticle = false;
                        default:
                            break;
                    }

                    characters = new StringBuffer();

                    break;
                case XMLStreamReader.CHARACTERS:
                    characters.append(xmlStreamReader.getText());
            }
        }
        tx.commit();
        tx.close();
        xmlStreamReader.close();

        isLoaded = true;
    }

    private DumpTag resolveDumpTag(String tagName) {
        try {
            return DumpTag.valueOf(tagName);
        } catch (IllegalArgumentException e) {
            return DumpTag.ignorable;
        }
    }
}
