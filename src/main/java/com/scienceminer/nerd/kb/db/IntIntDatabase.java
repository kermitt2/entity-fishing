package com.scienceminer.nerd.kb.db;

import org.apache.hadoop.record.CsvRecordInput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.exceptions.NerdResourceException;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;


public abstract class IntIntDatabase extends KBDatabase<Integer, Integer> {
	private static final Logger logger = LoggerFactory.getLogger(IntIntDatabase.class);

	public IntIntDatabase(KBEnvironment envi, DatabaseType type) {
		super(envi, type);
	}
	
	public IntIntDatabase(KBEnvironment envi, DatabaseType type, String name) {
		super(envi, type, name);
	}
		
	// using standard LMDB copy mode
	@Override
	public Integer retrieve(Integer key) {
		byte[] cachedData = null;
		Integer record = null;
		try (Transaction tx = environment.createReadTransaction()) {
			cachedData = db.get(tx, KBEnvironment.serialize(key));
			if (cachedData != null) {
				record = (Integer)KBEnvironment.deserialize(cachedData);
			}
		} catch(Exception e) {
			logger.error("Cannot retrieve key " + key, e);
		}
		return record;
	}

	// using LMDB zero copy mode
	//@Override
	public Integer retrieve2(Integer key) {
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

	public void loadFromFile(File dataFile, boolean overwrite) throws Exception {
		if (isLoaded && !overwrite)
			return;
		if (dataFile == null)
			throw new NerdResourceException("Resource file not found");

		System.out.println("Loading " + name + " database");

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
		long bytesRead = 0;

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

			bytesRead = bytesRead + line.length() + 1;

			CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8")));
			KBEntry<Integer,Integer> entry = deserialiseCsvRecord(cri);
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
