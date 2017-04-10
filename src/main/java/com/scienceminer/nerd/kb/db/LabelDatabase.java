package com.scienceminer.nerd.kb.db;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import java.io.*;
import java.util.*;

import org.apache.hadoop.record.CsvRecordInput;
import org.apache.hadoop.record.CsvRecordOutput;

import org.apache.log4j.Logger;

import org.wikipedia.miner.db.struct.DbLabel;
import org.wikipedia.miner.db.struct.DbSenseForLabel;

import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A {@link KBDatabase} for associating Strings with statistics about the articles (senses) this string could refer to. 
 */
public class LabelDatabase extends StringRecordDatabase<DbLabel> {

	/**
	 * Creates or connects to a database, whose name and type will be {@link KBDatabase.DatabaseType#label}. 
	 * This will index label statistics according to their raw, unprocessed texts. 
	 * 
	 * @param env the KBEnvironment surrounding this database
	 */
	public LabelDatabase(KBEnvironment env) {
		super(env, DatabaseType.label);
	}

	/**
	 * Retrieves the label statistics associated with the given text key. 
	 * 
	 * <p>Note:<b> you should NOT apply text processors to the key; that will be done internally within this method.
	 * 
	 * @return true if the database has been prepared for use, otherwise false
	 */
	@Override
	public DbLabel retrieve(String key) {
		return super.retrieve(key);
	}

	@Override
	public KBEntry<String,DbLabel> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
		String text = record.readString(null);
		DbLabel l = new DbLabel();
		l.deserialize(record);
		
		return new KBEntry<String,DbLabel>(text, l);
	}

}
