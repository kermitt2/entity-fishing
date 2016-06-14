package org.wikipedia.miner.db;

//import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLStreamException;

import org.apache.hadoop.record.CsvRecordInput;
import org.wikipedia.miner.db.struct.DbLinkLocation;
import org.wikipedia.miner.db.struct.DbLinkLocationList;
import org.wikipedia.miner.db.struct.DbPageLinkCounts;
import org.wikipedia.miner.util.ProgressTracker;
import org.wikipedia.miner.util.WikipediaConfiguration;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;

public class PageLinkCountDatabase extends IntObjectDatabase<DbPageLinkCounts>{

	public PageLinkCountDatabase(WEnvironment env) {
		super(env, 
				DatabaseType.pageLinkCounts, 
				new RecordBinding<DbPageLinkCounts>(){
			@Override
			public DbPageLinkCounts createRecordInstance() {
				return new DbPageLinkCounts() ;
			}
		}
		);
	}

	@Override
	public WEntry<Integer, DbPageLinkCounts> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException() ;
	}

	private WEntry<Integer, DbLinkLocationList> deserializePageLinkCsvRecord(CsvRecordInput record) throws IOException {
		Integer id = record.readInt(null) ;

		DbLinkLocationList l = new DbLinkLocationList() ;
		l.deserialize(record) ;

		return new WEntry<Integer, DbLinkLocationList>(id, l) ;
	}

	private WEntry<Integer, DbPageLinkCounts> buildLinkSummaryEntry(
			WEntry<Integer, DbLinkLocationList> inLinkEntry, 
			WEntry<Integer, DbLinkLocationList> outLinkEntry
	) throws IOException {


		if (inLinkEntry==null && outLinkEntry==null)
			throw new IOException("both inlink and outlink entries are null") ;

		if (inLinkEntry != null && outLinkEntry != null && !inLinkEntry.getKey().equals(outLinkEntry.getKey()))
			throw new IOException("inlink and outlink records are not for the same page") ;


		Integer id = null ;
		DbPageLinkCounts linkCounts = new DbPageLinkCounts(0,0,0,0) ;

		if (inLinkEntry != null) {

			id = inLinkEntry.getKey() ;

			int total = 0 ;
			int distinct = 0 ;

			for (DbLinkLocation ll:inLinkEntry.getValue().getLinkLocations()) {
				distinct++ ;
				total+=ll.getSentenceIndexes().size();
			}
			linkCounts.setTotalLinksIn(total) ;
			linkCounts.setDistinctLinksIn(distinct) ;
		}

		if (outLinkEntry != null) {

			id = outLinkEntry.getKey() ;

			int total = 0 ;
			int distinct = 0 ;

			for (DbLinkLocation ll:outLinkEntry.getValue().getLinkLocations()) {
				distinct++ ;
				total+=ll.getSentenceIndexes().size();
			}
			linkCounts.setTotalLinksOut(total) ;
			linkCounts.setDistinctLinksOut(distinct) ;
		}


		return new WEntry<Integer, DbPageLinkCounts>(id, linkCounts) ;
	}

	@Override
	public DbPageLinkCounts filterCacheEntry(
			WEntry<Integer, DbPageLinkCounts> e,
			WikipediaConfiguration conf) {

		//TIntHashSet validIds = conf.getArticlesOfInterest() ;
		ConcurrentMap validIds = conf.getArticlesOfInterest();

		//if (validIds != null && !validIds.contains(e.getKey()))
		if (validIds != null && (validIds.get(e.getKey()) == null) )
			return null ; 

		return e.getValue();
	}

	@Override 
	public void loadFromCsvFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException  {
		throw new UnsupportedOperationException() ;
	}

	public void loadFromCsvFiles(File linksInFile, File linksOutFile, boolean overwrite, ProgressTracker tracker) throws IOException  {

		if (exists() && !overwrite)
			return ;
		
		if (tracker == null) tracker = new ProgressTracker(1, WDatabase.class) ;
		tracker.startTask(linksInFile.length()+linksOutFile.length(), "Loading " + getName() + " database") ;

		Database db = getDatabase(false) ;

		BufferedReader linksInInput = new BufferedReader(new InputStreamReader(new FileInputStream(linksInFile), "UTF-8")) ;
		BufferedReader linksOutInput = new BufferedReader(new InputStreamReader(new FileInputStream(linksOutFile), "UTF-8")) ;

		long bytesRead = 0 ;


		String inLinkLine = linksInInput.readLine() ;
		bytesRead += (inLinkLine.length() + 1) ;
		CsvRecordInput linksInRecord = new CsvRecordInput(new ByteArrayInputStream((inLinkLine + "\n").getBytes("UTF-8"))) ;
		WEntry<Integer, DbLinkLocationList> inLinkEntry = deserializePageLinkCsvRecord(linksInRecord) ;

		String outLinkLine = linksOutInput.readLine() ;
		bytesRead += (outLinkLine.length() + 1) ;
		CsvRecordInput linksOutRecord = new CsvRecordInput(new ByteArrayInputStream((outLinkLine + "\n").getBytes("UTF-8"))) ;
		WEntry<Integer, DbLinkLocationList> outLinkEntry = deserializePageLinkCsvRecord(linksOutRecord) ;


		while (inLinkEntry != null && outLinkEntry != null) {

			WEntry<Integer, DbPageLinkCounts> linkCountEntry = null;
			boolean advanceInLinks = false;
			boolean advanceOutLinks = false ;


			if (inLinkEntry == null || outLinkEntry.getKey() < inLinkEntry.getKey()) {
				linkCountEntry = buildLinkSummaryEntry(null, outLinkEntry) ;
				advanceOutLinks = true ;
			}

			if (outLinkEntry == null || inLinkEntry.getKey() < outLinkEntry.getKey()) {
				linkCountEntry = buildLinkSummaryEntry(inLinkEntry, null) ;
				advanceInLinks = true ;
			}

			if (inLinkEntry.getKey().equals(outLinkEntry.getKey())) {
				linkCountEntry = buildLinkSummaryEntry(inLinkEntry, outLinkEntry) ;
				advanceInLinks = true ;
				advanceOutLinks = true ;
			}

			if (linkCountEntry != null) {
				DatabaseEntry k = new DatabaseEntry() ;
				keyBinding.objectToEntry(linkCountEntry.getKey(), k) ;

				DatabaseEntry v = new DatabaseEntry() ;
				valueBinding.objectToEntry(linkCountEntry.getValue(), v) ;

				db.put(null, k, v) ;
			}

			if (advanceInLinks) {
				inLinkLine = linksInInput.readLine() ;
				if (inLinkLine != null) {
					bytesRead += (inLinkLine.length() + 1) ;
					linksInRecord = new CsvRecordInput(new ByteArrayInputStream((inLinkLine + "\n").getBytes("UTF-8"))) ;
					inLinkEntry = deserializePageLinkCsvRecord(linksInRecord) ;
				} else {
					inLinkEntry = null ;
				}
			}

			if (advanceOutLinks) {
				outLinkLine = linksOutInput.readLine() ;
				if (outLinkLine != null) {
					bytesRead += (outLinkLine.length() + 1) ;
					linksOutRecord = new CsvRecordInput(new ByteArrayInputStream((outLinkLine + "\n").getBytes("UTF-8"))) ;
					outLinkEntry = deserializePageLinkCsvRecord(linksOutRecord) ;
				} else {
					outLinkEntry = null ;
				}
			}

			tracker.update(bytesRead) ;
		}

		linksInInput.close();
		linksOutInput.close() ;

		env.cleanAndCheckpoint() ;
		getDatabase(true) ;
	}

}
