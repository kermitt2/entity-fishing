package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.math.BigInteger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.hadoop.record.CsvRecordInput;
import com.scienceminer.nerd.kb.model.Page;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import org.apache.tools.bzip2.*;

import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A {@link KBDatababe} for associating page ids with page markup. 
 * 
 */
public class MarkupDatabase extends KBDatabase<Integer, String> {

	private boolean full = false;

	private enum DumpTag {page, id, text, ignorable};

	/**
	 * Creates or connects to a database, whose name and type will be {@link KBDatabase.DatabaseType#markup}.
	 * If full, the complete text content will be indexed, otherwise only the first paragraph
	 * 
	 * @param env the KBEnvironment surrounding this database
	 */
	public MarkupDatabase(KBEnvironment env) {
		super (env, DatabaseType.markup); 
	}

	public MarkupDatabase(KBEnvironment env, DatabaseType type) {
		super (env, type);
		if (type == DatabaseType.markupFull) {
			full = true; 
		}
		else {
			full = false;
		}
	}

	@Override
	public KBEntry<Integer,String> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override 
	public void loadFromCsvFile(File dataFile, boolean overwrite) throws IOException  {
		throw new UnsupportedOperationException();
	}

	// using standard LMDB copy mode
	@Override
	public String retrieve(Integer key) {
		//byte[] cachedData = null;
		String theString = null;
		try (Transaction tx = environment.createReadTransaction()) {
			//theString = string(db.get(tx, BigInteger.valueOf(key).toByteArray()));
			theString = string(db.get(tx, KBEnvironment.serialize(key)));
		} catch(Exception e) {
			e.printStackTrace();
		}
		return theString;
	}

	// using LMDB zero copy mode
	//@Override
	public String retrieve2(Integer key) {
		byte[] cachedData = null;
		String theString = null;
		try (Transaction tx = environment.createReadTransaction();
			BufferCursor cursor = db.bufferCursor(tx)) {
			//cursor.keyWriteBytes(BigInteger.valueOf(key).toByteArray());
			cursor.keyWriteBytes(KBEnvironment.serialize(key));
			if (cursor.seekKey()) {
				theString = string(cursor.valBytes());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return theString;
	}

	/**
	 * Builds the persistent markup database from an XML dump
	 * 
	 * @param dataFile the XML file containing a wikipedia dump 
	 * @param overwrite true if the existing database should be overwritten, otherwise false
	 * @throws IOException if there is a problem reading or deserialising the given data file.
	 * @throws XMLStreamException if the XML within the data file cannot be parsed.
     * @throws org.apache.commons.compress.compressors.CompressorException
	 */
	public void loadFromXmlFile(File dataFile, boolean overwrite) throws IOException, XMLStreamException, CompressorException  {
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + getName() + " database");

		Integer currId = null;
		String currMarkup = null;
		StringBuffer characters = new StringBuffer();
		
		InputStream reader;
		CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
                 
		if (dataFile.getName().endsWith(".bz2")){
            FileInputStream fis = new FileInputStream(dataFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
            reader = input;
        } else{
			reader = new FileInputStream(dataFile);
        }

		XMLInputFactory xmlStreamFactory = XMLInputFactory.newInstance();
		CountingInputStream countingReader = new CountingInputStream(reader);
		XMLStreamReader xmlStreamReader = xmlStreamFactory.createXMLStreamReader(new InputStreamReader(countingReader,decoder));
        //System.out.println("Parser class: " + xmlStreamReader.getClass().toString());

		int pageTotal = 0;
		int nbToAdd = 0;
		Transaction tx = environment.createWriteTransaction();
		while (xmlStreamReader.hasNext()) {
			int eventCode = xmlStreamReader.next();
			switch (eventCode) {
				case XMLStreamReader.START_ELEMENT :
					switch(resolveDumpTag(xmlStreamReader.getLocalName())) {
						case page:
					}

					break;
				case XMLStreamReader.END_ELEMENT :

					switch(resolveDumpTag(xmlStreamReader.getLocalName())) {

						case id:
							//only take the first id (there is a 2nd one for the revision) 
							if (currId == null) 
								currId = Integer.parseInt(characters.toString().trim());
							break;
						case text:
							currMarkup = characters.toString().trim();
							break;
						case page:
							if (nbToAdd == 1000) {
								tx.commit();
								tx.close();
								nbToAdd = 0;
								tx = environment.createWriteTransaction();
							}
							if (full) {
								// we only store the first paragraph/summary
								currMarkup = Page.formatAllMarkup(currMarkup);
							} else {
								// we only store the first paragraph/summary
								currMarkup = Page.formatFirstParagraphMarkup(currMarkup);
							}
							pageTotal++;

							if (currMarkup.trim().length() > 5) {
								try {
									//db.put(tx, BigInteger.valueOf(currId).toByteArray(), bytes(currMarkup));
									db.put(tx, KBEnvironment.serialize(currId), bytes(currMarkup));
									nbToAdd++;
								} catch(Exception e) {
									System.out.println("Markup addition failed: " + currId + " / " + currMarkup);
									e.printStackTrace();
								}
							}

							currId = null;
							currMarkup = null;
						default:
							break;
					}

					characters = new StringBuffer();

					break;
				case XMLStreamReader.CHARACTERS :
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
