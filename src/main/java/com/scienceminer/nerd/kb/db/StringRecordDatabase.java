package com.scienceminer.nerd.kb.db;

import org.apache.hadoop.record.CsvRecordInput;
import org.apache.hadoop.record.Record;

import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public abstract class StringRecordDatabase<Record> extends KBDatabase<String, Record> {

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
			e.printStackTrace();
		}
		return record;
	}

	// using LMDB zero copy mode
	//@Override
	public Record retrieve2(String key) {
		byte[] cachedData = null;
		Record record = null;
		try (Transaction tx = environment.createReadTransaction();
			BufferCursor cursor = db.bufferCursor(tx)) {
			cursor.keyWriteBytes(KBEnvironment.serialize(key));
			if (cursor.seekKey()) {
				record = (Record)KBEnvironment.deserialize(cursor.valBytes());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return record;
	}
	
	/*public void add(KBEntry<String,Record> entry) {
		try (Transaction tx = environment.createWriteTransaction()) {
			db.put(tx, bytes(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
			tx.commit();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}*/

	public void loadFromFile(File dataFile, boolean overwrite) throws Exception  {
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + name + " database");

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
					e.printStackTrace();
					System.out.println("We skip this particular invalid (and awful) entry and continue loading...");
				}
			}
		}
		tx.commit();
		tx.close();
		input.close();
		isLoaded = true;
	}

}
