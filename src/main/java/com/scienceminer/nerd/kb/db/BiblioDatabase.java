package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.Statement;
import org.apache.hadoop.record.CsvRecordInput;
import org.lmdbjava.CursorIterator;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static java.nio.ByteBuffer.allocateDirect;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class BiblioDatabase extends StringRecordDatabase<String> {
    private static final Logger logger = LoggerFactory.getLogger(BiblioDatabase.class);

    public BiblioDatabase(KBEnvironment env) {
        super(env, DatabaseType.biblio);
    }

    @Override
    public KBEntry<String, String> deserialiseCsvRecord(
            CsvRecordInput record) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Load the bilbiographical index
     */
    public void fillBiblioDb(ConceptDatabase conceptDb, StatementDatabase statementDb, boolean overwrite) throws Exception {
        if (isLoaded && !overwrite)
            return;
        System.out.println("Loading " + name + " database");

        if (conceptDb == null)
            throw new NerdResourceException("conceptDb not found");

        if (statementDb == null)
            throw new NerdResourceException("statementDb not found");

        // iterate through concepts
        KBIterator iter = new KBIterator(conceptDb);
        Txn<ByteBuffer> tx = environment.txnWrite();
        try {
            int nbToAdd = 0;
            int n = 0; // total entities
            int nbDoi = 0; // total doi found
            while (iter.hasNext()) {
                if (nbToAdd > 10000) {
                    tx.commit();
                    tx.close();
                    tx = environment.txnWrite();
                    nbToAdd = 0;
                }
                CursorIterator.KeyVal<ByteBuffer> entry = iter.next();
                ByteBuffer keyData = entry.key();
                ByteBuffer valueData = entry.val();
                //Page p = null;
                try {
                    String entityId = (String) KBEnvironment.deserialize(keyData);
                    // check the statements for a property P356 (DOI)
                    String doi = null;

                    List<Statement> statements = statementDb.retrieve(entityId);
                    if (isNotEmpty(statements)) {
                        for (Statement statement : statements) {
                            if (statement.getPropertyId().equals("P356")) {
                                doi = statement.getValue();
                                //System.out.println("found DOI: " + doi);
                                if (doi.startsWith("\""))
                                    doi = doi.substring(1, doi.length());
                                if (doi.endsWith("\""))
                                    doi = doi.substring(0, doi.length() - 1);
                            }
                        }
                    }

                    if (doi != null) {
                        KBEntry<String, String> theEntry = new KBEntry<>(doi, entityId);
                        try {
                            final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
                            keyBuffer.put(KBEnvironment.serialize(theEntry.getKey())).flip();
                            final byte[] serializedValue = KBEnvironment.serialize(theEntry.getValue());
                            final ByteBuffer valBuffer = allocateDirect(serializedValue.length);
                            valBuffer.put(serializedValue).flip();
                            db.put(tx, keyBuffer, valBuffer);
                            nbToAdd++;
                            nbDoi++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    logger.error("fail to write entity description", e);
                }
                n++;
            }
            System.out.println("total nb entities visited: " + n);
            System.out.println("total nb DOI found: " + nbDoi);
        } catch (Exception e) {
            logger.error("Error when creating biblioDb", e);
        } finally {
            if (iter != null)
                iter.close();
            tx.commit();
            tx.close();
            isLoaded = true;
        }
    }
}
