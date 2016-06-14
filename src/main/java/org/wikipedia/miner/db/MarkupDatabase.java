package org.wikipedia.miner.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.hadoop.record.CsvRecordInput;
import org.wikipedia.miner.util.ProgressTracker;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.model.Page;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import org.apache.tools.bzip2.* ;

/**
 * A {@link WDatabase} for associating page ids with page markup. 
 * 
 * This will throw {@link UnsupportedOperationException}s if any attempt is made to cache this database to memory.
 */
public class MarkupDatabase extends WDatabase<Integer, String> {

	private enum DumpTag {page, id, text, ignorable} ;

	/**
	 * Creates or connects to a database, whose name and type will be {@link WDatabase.DatabaseType#markup}.
	 * 
	 * @param env the WEnvironment surrounding this database
	 */
	public MarkupDatabase(WEnvironment env) {

		super (env, DatabaseType.markup, new IntegerBinding(), new StringBinding()) ;
	}

	@Override
	public String filterCacheEntry(WEntry<Integer, String> e,
			WikipediaConfiguration conf) {
		throw new UnsupportedOperationException() ;
	}


	@Override
	public WEntry<Integer,String> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException() ;
	}
	
	@Override 
	public void loadFromCsvFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException  {
		throw new UnsupportedOperationException() ;
	}

	

	/**
	 * Builds the persistent markup database from an XML dump
	 * 
	 * @param dataFile the XML file containing a wikipedia dump 
	 * @param overwrite true if the existing database should be overwritten, otherwise false
	 * @param tracker an optional progress tracker (may be null)
	 * @throws IOException if there is a problem reading or deserialising the given data file.
	 * @throws XMLStreamException if the XML within the data file cannot be parsed.
     * @throws org.apache.commons.compress.compressors.CompressorException
	 */
	public void loadFromXmlFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException, XMLStreamException, CompressorException  {
//            overwrite=true;
		if (exists() && !overwrite)
			return ;
		
		if (tracker == null) tracker = new ProgressTracker(1, MarkupDatabase.class) ;
		tracker.startTask(dataFile.length(), "Loading " + getName() + " database") ;

		Database db = getDatabase(false) ;

		Integer currId = null ;
		String currMarkup = null ;
		StringBuffer characters = new StringBuffer() ;
		
		InputStream reader ;
		 CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
                 
		if (dataFile.getName().endsWith(".bz2")){
                 
                    FileInputStream fin=new FileInputStream(dataFile);
                    BufferedInputStream bis=new BufferedInputStream(fin);
                    CompressorInputStream input=new CompressorStreamFactory().createCompressorInputStream(bis);
                    reader = input;
                }  else{
			reader = new FileInputStream(dataFile) ;
                }
		XMLInputFactory xmlStreamFactory = XMLInputFactory.newInstance() ;
		CountingInputStream countingReader = new CountingInputStream(reader) ;
               // To work around a bug in XERCES (XERCESJ-1257), we assume the XML is always UTF8, so we simply provide reader.
		XMLStreamReader xmlStreamReader = xmlStreamFactory.createXMLStreamReader(new InputStreamReader(countingReader,decoder)) ;
                System.out.println("Parser class: " + xmlStreamReader.getClass().toString());

		int pageTotal = 0 ;
		long charTotal = 0 ;
		long maxChar = 0 ;

		while (xmlStreamReader.hasNext()) {

			int eventCode = xmlStreamReader.next();

			switch (eventCode) {
			case XMLStreamReader.START_ELEMENT :
				switch(resolveDumpTag(xmlStreamReader.getLocalName())) {
				case page:
					//System.out.println(" - " + countingReader.getByteCount()) ;
				}

				break;
			case XMLStreamReader.END_ELEMENT :

				switch(resolveDumpTag(xmlStreamReader.getLocalName())) {

				case id:
					//only take the first id (there is a 2nd one for the revision) 
					if (currId == null) 
						currId = Integer.parseInt(characters.toString().trim()) ;
					break ;
				case text:
					currMarkup = characters.toString().trim() ;
					break ;
				case page:
					currMarkup = Page.formatFirstParagraphMarkup(currMarkup);
					DatabaseEntry key = new DatabaseEntry() ;
					keyBinding.objectToEntry(currId, key) ;

					DatabaseEntry value = new DatabaseEntry() ;
					valueBinding.objectToEntry(currMarkup, value) ;

					pageTotal++ ;
					charTotal = charTotal + currMarkup.length();

					maxChar = Math.max(maxChar, currMarkup.length()) ;
					db.put(null, key, value) ;

					currId = null ;
					currMarkup = null ;

					tracker.update(countingReader.getByteCount()) ;
				}

				characters = new StringBuffer() ;

				break;
			case XMLStreamReader.CHARACTERS :
				characters.append(xmlStreamReader.getText()) ;
			}
		}
		xmlStreamReader.close();

		env.cleanAndCheckpoint() ;
		getDatabase(true) ;
	}

	private DumpTag resolveDumpTag(String tagName) {

		try {
			return DumpTag.valueOf(tagName) ;
		} catch (IllegalArgumentException e) {
			return DumpTag.ignorable ;
		}
	}
}
