package com.scienceminer.nerd.kb.db;

import org.apache.hadoop.record.CsvRecordInput;
import org.apache.hadoop.record.Record;

import java.math.BigInteger;
import java.io.*;

import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A {@link KBDatabase} for associating String keys with an Integer object.
 *
 */
public abstract class StringIntDatabase extends KBDatabase<String, Integer> {

	/**
	 * Creates or connects to a database, whose name will match the given {@link KBDatabase.DatabaseType}
	 * 
	 * @param env the KBEnvironment surrounding this database
	 * @param type the type of database
	 */
	public StringIntDatabase(KBEnvironment envi, DatabaseType type) {
		super(envi, type);
	}
	
	/**
	 * Creates or connects to a database with the given name.
	 * 
	 * @param env the KBEnvironment surrounding this database
	 * @param type the type of database
	 * @param name the name of the database 
	 */
	public StringIntDatabase(KBEnvironment envi, DatabaseType type, String name) {
		super(envi, type, name);
	}
		
	//@Override
	public Integer retrieve2(String key) {
		/*if (isCached) {
			return cache.get(key);
		}*/
		byte[] cachedData = null;
		int record = -1;
		try (Transaction tx = environment.createReadTransaction()) {
			cachedData = db.get(tx, bytes(key));
			if (cachedData != null)
				record = new BigInteger(cachedData).intValue();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return new Integer(record);
	}

	// using LMDB zero copy mode
	@Override
	public Integer retrieve(String key) {
		byte[] cachedData = null;
		Integer record = null;
		try (Transaction tx = environment.createReadTransaction();
			 BufferCursor cursor = db.bufferCursor(tx)) {
			cursor.keyWriteBytes(bytes(key));
			if (cursor.seekKey()) {
				record = new BigInteger(cursor.valBytes()).intValue();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return record;
	}
	
	public void add(KBEntry<String,Integer> entry) {
		try (Transaction tx = environment.createWriteTransaction()) {
			db.put(tx, bytes(entry.getKey()), Utilities.serialize(entry.getValue()));
			tx.commit();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds the persistent database from a file.
	 * 
	 * @param dataFile the file (here a CSV file) containing data to be loaded
	 * @param overwrite true if the existing database should be overwritten, otherwise false
	 * @throws IOException if there is a problem reading or deserialising the given data file.
	 */
	public void loadFromCsvFile(File dataFile, boolean overwrite) throws IOException  {
		if (isLoaded && !overwrite)
			return;
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
			KBEntry<String,Integer> entry = deserialiseCsvRecord(cri);

			if (entry != null) {
				try {
					db.put(tx, bytes(entry.getKey()), BigInteger.valueOf(entry.getValue()).toByteArray());
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
