package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import org.apache.hadoop.record.CsvRecordInput;

import org.fusesource.lmdbjni.BufferCursor;
import org.fusesource.lmdbjni.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public abstract class IntRecordDatabase<Record> extends KBDatabase<Integer, Record> {
	private static final Logger logger = LoggerFactory.getLogger(IntRecordDatabase.class);

	public IntRecordDatabase(KBEnvironment envi, DatabaseType type) {
		super(envi, type);
	}
	
	public IntRecordDatabase(KBEnvironment envi, DatabaseType type, String name) {
		super(envi, type, name);
	}

	// using standard LMDB copy mode
	@Override
	public Record retrieve(Integer key) {
		byte[] cachedData = null;
		Record record = null;
		try (Transaction tx = environment.createReadTransaction()) {
			cachedData = db.get(tx, KBEnvironment.serialize(key));
			if (cachedData != null) {
				record = (Record)KBEnvironment.deserialize(cachedData);
			}
		} catch(Exception e) {
			logger.error("Cannot retrieve key " + key, e);
		}
		return record;
	}
	
	// using LMDB zero copy mode
	//@Override
	public Record retrieve2(Integer key) {
		byte[] cachedData = null;
		Record record = null;
		try (Transaction tx = environment.createReadTransaction();
			 BufferCursor cursor = db.bufferCursor(tx)) {
			cursor.keyWriteBytes(KBEnvironment.serialize(key));
			if (cursor.seekKey()) {
				record = (Record)KBEnvironment.deserialize(cursor.valBytes());
			}
		} catch(Exception e) {
			logger.error("cannot retrieve " + key, e);
		}
		return record;
	}

	public void loadFromFile(File dataFile, boolean overwrite) throws Exception  {
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
			try {
				KBEntry<Integer,Record> entry = deserialiseCsvRecord(cri);
				if (entry != null) {
					try {
						db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
						nbToAdd++;
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			} catch(Exception e) {
				System.out.println("Error deserialising: " + line);
				e.printStackTrace();
			}
		}
		tx.commit();
		tx.close();
		input.close();
		isLoaded = true;
	}

}
