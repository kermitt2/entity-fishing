package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

import javax.xml.stream.XMLStreamException;

import org.apache.hadoop.record.CsvRecordInput;

import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.db.*;
import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.util.*;

import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class RelationDatabase extends IntRecordDatabase<Relation> {

	public RelationDatabase(WEnvironment env) {
		super(env, DatabaseType.relations);
	}

	@Override
	public WEntry<Integer, Relation> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	private WEntry<Integer, Relation> deserializePageLinkCsvRecord(CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	/*@Override
	public DbPageLinkCounts filterCacheEntry(
			WEntry<Integer, DbPageLinkCounts> e,
			WikipediaConfiguration conf) {

		//TIntHashSet validIds = conf.getArticlesOfInterest() ;
		ConcurrentMap validIds = conf.getArticlesOfInterest();

		//if (validIds != null && !validIds.contains(e.getKey()))
		if (validIds != null && (validIds.get(e.getKey()) == null) )
			return null ; 

		return e.getValue();
	}*/

	@Override 
	public void loadFromCsvFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException  {
		throw new UnsupportedOperationException() ;
	}

}
