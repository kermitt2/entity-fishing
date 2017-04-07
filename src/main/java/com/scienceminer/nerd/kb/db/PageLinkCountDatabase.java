package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

import javax.xml.stream.XMLStreamException;

import org.apache.hadoop.record.CsvRecordInput;

import org.wikipedia.miner.db.struct.DbLinkLocation;
import org.wikipedia.miner.db.struct.DbLinkLocationList;
import org.wikipedia.miner.db.struct.DbPageLinkCounts;

import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class PageLinkCountDatabase extends IntRecordDatabase<DbPageLinkCounts>{

	public PageLinkCountDatabase(KBEnvironment env) {
		super(env, DatabaseType.pageLinkCounts);
	}

	@Override
	public KBEntry<Integer, DbPageLinkCounts> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	private KBEntry<Integer, DbLinkLocationList> deserializePageLinkCsvRecord(CsvRecordInput record) throws IOException {
		Integer id = record.readInt(null);

		DbLinkLocationList l = new DbLinkLocationList();
		l.deserialize(record);

		return new KBEntry<Integer, DbLinkLocationList>(id, l);
	}

	private KBEntry<Integer, DbPageLinkCounts> buildLinkSummaryEntry(
			KBEntry<Integer, DbLinkLocationList> inLinkEntry, 
			KBEntry<Integer, DbLinkLocationList> outLinkEntry
	) throws IOException {


		if (inLinkEntry==null && outLinkEntry==null)
			throw new IOException("both inlink and outlink entries are null");

		if (inLinkEntry != null && outLinkEntry != null && !inLinkEntry.getKey().equals(outLinkEntry.getKey()))
			throw new IOException("inlink and outlink records are not for the same page");


		Integer id = null;
		DbPageLinkCounts linkCounts = new DbPageLinkCounts(0,0,0,0);

		if (inLinkEntry != null) {

			id = inLinkEntry.getKey();

			int total = 0;
			int distinct = 0;

			for (DbLinkLocation ll : inLinkEntry.getValue().getLinkLocations()) {
				distinct++;
				total += ll.getSentenceIndexes().size();
			}
			linkCounts.setTotalLinksIn(total);
			linkCounts.setDistinctLinksIn(distinct);
		}

		if (outLinkEntry != null) {

			id = outLinkEntry.getKey();

			int total = 0;
			int distinct = 0;

			for (DbLinkLocation ll:outLinkEntry.getValue().getLinkLocations()) {
				distinct++;
				total+=ll.getSentenceIndexes().size();
			}
			linkCounts.setTotalLinksOut(total);
			linkCounts.setDistinctLinksOut(distinct);
		}

		return new KBEntry<Integer, DbPageLinkCounts>(id, linkCounts);
	}

	@Override 
	public void loadFromCsvFile(File dataFile, boolean overwrite) throws IOException  {
		throw new UnsupportedOperationException();
	}

	public void loadFromCsvFiles(File linksInFile, File linksOutFile, boolean overwrite) throws IOException  {
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + getName() + " database");

		BufferedReader linksInInput = new BufferedReader(new InputStreamReader(new FileInputStream(linksInFile), "UTF-8"));
		BufferedReader linksOutInput = new BufferedReader(new InputStreamReader(new FileInputStream(linksOutFile), "UTF-8"));

		long bytesRead = 0;

		String inLinkLine = linksInInput.readLine();
		bytesRead += (inLinkLine.length() + 1);
		CsvRecordInput linksInRecord = new CsvRecordInput(new ByteArrayInputStream((inLinkLine + "\n").getBytes("UTF-8")));
		KBEntry<Integer, DbLinkLocationList> inLinkEntry = deserializePageLinkCsvRecord(linksInRecord);

		String outLinkLine = linksOutInput.readLine();
		bytesRead += (outLinkLine.length() + 1);
		CsvRecordInput linksOutRecord = new CsvRecordInput(new ByteArrayInputStream((outLinkLine + "\n").getBytes("UTF-8")));
		KBEntry<Integer, DbLinkLocationList> outLinkEntry = deserializePageLinkCsvRecord(linksOutRecord);

		int nbToAdd = 0;
		Transaction tx = environment.createWriteTransaction();
		while (inLinkEntry != null && outLinkEntry != null) {
			if (nbToAdd == 10000) {
				tx.commit();
				tx.close();
				nbToAdd = 0;
				tx = environment.createWriteTransaction();
			}
			KBEntry<Integer, DbPageLinkCounts> linkCountEntry = null;
			boolean advanceInLinks = false;
			boolean advanceOutLinks = false;


			if (inLinkEntry == null || outLinkEntry.getKey() < inLinkEntry.getKey()) {
				linkCountEntry = buildLinkSummaryEntry(null, outLinkEntry);
				advanceOutLinks = true;
			}

			if (outLinkEntry == null || inLinkEntry.getKey() < outLinkEntry.getKey()) {
				linkCountEntry = buildLinkSummaryEntry(inLinkEntry, null);
				advanceInLinks = true;
			}

			if (inLinkEntry.getKey().equals(outLinkEntry.getKey())) {
				linkCountEntry = buildLinkSummaryEntry(inLinkEntry, outLinkEntry);
				advanceInLinks = true;
				advanceOutLinks = true;
			}

			if (linkCountEntry != null) {
				try {
					db.put(tx, BigInteger.valueOf(linkCountEntry.getKey()).toByteArray(), Utilities.serialize(linkCountEntry.getValue()));
					nbToAdd++;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			if (advanceInLinks) {
				inLinkLine = linksInInput.readLine();
				if (inLinkLine != null) {
					bytesRead += (inLinkLine.length() + 1);
					linksInRecord = new CsvRecordInput(new ByteArrayInputStream((inLinkLine + "\n").getBytes("UTF-8")));
					inLinkEntry = deserializePageLinkCsvRecord(linksInRecord);
				} else {
					inLinkEntry = null;
				}
			}

			if (advanceOutLinks) {
				outLinkLine = linksOutInput.readLine();
				if (outLinkLine != null) {
					bytesRead += (outLinkLine.length() + 1);
					linksOutRecord = new CsvRecordInput(new ByteArrayInputStream((outLinkLine + "\n").getBytes("UTF-8")));
					outLinkEntry = deserializePageLinkCsvRecord(linksOutRecord);
				} else {
					outLinkEntry = null;
				}
			}
		}
		tx.commit();
		tx.close();
		linksInInput.close();
		linksOutInput.close();
		isLoaded = true;
	}

}
