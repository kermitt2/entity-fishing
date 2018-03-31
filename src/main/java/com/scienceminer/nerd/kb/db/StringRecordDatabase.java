package com.scienceminer.nerd.kb.db;

import org.apache.hadoop.record.CsvRecordInput;
import org.fusesource.lmdbjni.BufferCursor;
import org.fusesource.lmdbjni.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.exceptions.NerdResourceException;

import java.io.*;

import static org.fusesource.lmdbjni.Constants.bytes;

public abstract class StringRecordDatabase<Record> extends KBDatabase<String, Record> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringRecordDatabase.class);

	public StringRecordDatabase(KBEnvironment envi, DatabaseType type) {
		super(envi, type);
	}

	public StringRecordDatabase(KBEnvironment envi, DatabaseType type, String name) {
		super(envi, type, name);
	}
		
	@Override
	public Record retrieve(String key) {
		byte[] cachedData = null;
		Record record = null;
		try (Transaction tx = environment.createReadTransaction()) {
			cachedData = db.get(tx, KBEnvironment.serialize(key));
			if (cachedData != null) {
				record = (Record)KBEnvironment.deserialize(cachedData);
			}
		} catch(Exception e) {
			LOGGER.error("Cannot retrieve key " + key, e);
		}
		return record;
	}

	// using LMDB zero copy mode
	//@Override
	public Record retrieve2(String key) {
		Record record = null;
		try (Transaction tx = environment.createReadTransaction();
			BufferCursor cursor = db.bufferCursor(tx)) {
			cursor.keyWriteBytes(KBEnvironment.serialize(key));
			if (cursor.seekKey()) {
				record = (Record)KBEnvironment.deserialize(cursor.valBytes());
			}
		} catch(Exception e) {
            LOGGER.error("Cannot retrieve key " + key, e);
		}
		return record;
	}

	public void loadFromFile(File dataFile, boolean overwrite) throws Exception  {
		if (dataFile == null || (isLoaded && !overwrite))
			return;
        LOGGER.info("Loading " + name + " database");

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
			KBEntry<String,Record> entry = deserialiseCsvRecord(cri);
			if (entry != null) {
				try {
					db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
					nbToAdd++;
				} catch(Exception e) {
					//System.out.println("Invalid input line: " + line);
					//e.printStackTrace();
					LOGGER.warn("Invalid key: " + entry.getKey());
					LOGGER.warn("but don't worry, we skip this particular invalid (and awful) entry and continue loading...");
				}
			}
		}
		tx.commit();
		tx.close();
		input.close();
		isLoaded = true;
	}

}
