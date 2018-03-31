package com.scienceminer.nerd.kb.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.record.CsvRecordInput;
import org.apache.hadoop.record.Record;

import java.io.*;

import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.exceptions.NerdResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public abstract class StringIntDatabase extends KBDatabase<String, Integer> {
	private static final Logger logger = LoggerFactory.getLogger(StringIntDatabase.class);	

	public StringIntDatabase(KBEnvironment envi, DatabaseType type) {
		super(envi, type);
	}
	
	public StringIntDatabase(KBEnvironment envi, DatabaseType type, String name) {
		super(envi, type, name);
	}
		
	@Override
	public Integer retrieve(String key) {
		byte[] cachedData = null;
		Integer record = null;
		try (Transaction tx = environment.createReadTransaction()) {
			cachedData = db.get(tx, KBEnvironment.serialize(key));
			if (cachedData != null) {
				record = (Integer)KBEnvironment.deserialize(cachedData);
			}
		} catch(Exception e) {
			logger.error("cannot retrieve " + key, e);
		}
		return record;
	}

	// using LMDB zero copy mode
	//@Override
	public Integer retrieve2(String key) {
		byte[] cachedData = null;
		Integer record = null;
		try (Transaction tx = environment.createReadTransaction();
			BufferCursor cursor = db.bufferCursor(tx)) {
			cursor.keyWriteBytes(KBEnvironment.serialize(key));
			if (cursor.seekKey()) {
				record = (Integer)KBEnvironment.deserialize(cursor.valBytes());
			}
		} catch(Exception e) {
			logger.error("cannot retrieve " + key, e);
		}
		return record;
	}

	public void loadFromFile(File dataFile, boolean overwrite) throws Exception  {
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + name + " database");

		if (dataFile == null)
			throw new NerdResourceException("Resource file not found");

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
		String line = null;
		int nbToAdd = 0;
		Transaction tx = environment.createWriteTransaction();
		while ((line=input.readLine()) != null) {
			if (nbToAdd == 10000) {
				tx.commit();
				tx.close();
				nbToAdd = 0;
				tx = environment.createWriteTransaction();
			}

			CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8")));
			KBEntry<String,Integer> entry = deserialiseCsvRecord(cri);

			if (entry != null) {
				try {
					db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
					nbToAdd++;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		tx.commit();
		tx.close();
		input.close();
		isLoaded = true;
	}

}
