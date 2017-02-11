package com.scienceminer.nerd.kb.db;

import org.apache.hadoop.record.Record;
import org.apache.hadoop.record.CsvRecordInput;

import java.math.BigInteger;
import java.io.*;

import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A {@link KBDatabase} for associating Integer keys with some generic record value type.
 *
 */
public abstract class IntRecordDatabase<Record> extends KBDatabase<Integer, Record> {
	/**
	 * Creates or connects to a database, whose name will match the given {@link KBDatabe.DatabaseType}
	 * 
	 * @param env the KBEnvironment surrounding this database
	 * @param type the type of database
	 */
	public IntRecordDatabase(KBEnvironment envi, DatabaseType type) {
		super(envi, type);
	}
	
	/**
	 * Creates or connects to a database with the given name.
	 * 
	 * @param env the KBEnvironment surrounding this database
	 * @param type the type of database
	 * @param name the name of the database 
	 */
	public IntRecordDatabase(KBEnvironment envi, DatabaseType type, String name) {
		super(envi, type, name);
	}

	// using standard LMDB copy mode
	//@Override
	public Record retrieve2(Integer key) {
		byte[] cachedData = null;
		Record record = null;
		try (Transaction tx = environment.createReadTransaction()) {
			cachedData = db.get(tx, BigInteger.valueOf(key).toByteArray());
			if (cachedData != null)
				record = (Record)Utilities.deserialize(cachedData);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return record;
	}
	
	// using LMDB zero copy mode
	@Override
	public Record retrieve(Integer key) {
		byte[] cachedData = null;
		Record record = null;
		try (Transaction tx = environment.createReadTransaction();
			 BufferCursor cursor = db.bufferCursor(tx)) {
			cursor.keyWriteBytes(BigInteger.valueOf(key).toByteArray());
			if (cursor.seekKey()) {
				record = (Record)Utilities.deserialize(cursor.valBytes());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return record;
	}

	protected void add(KBEntry<Integer,Record> entry) {
		try (Transaction tx = environment.createWriteTransaction()) {
			db.put(tx, BigInteger.valueOf(entry.getKey()).toByteArray(), Utilities.serialize(entry.getValue()));
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
			try {
				KBEntry<Integer,Record> entry = deserialiseCsvRecord(cri);
				if ( (entry != null) && (filterEntry(entry) != null) ) {
					try {
						db.put(tx, BigInteger.valueOf(entry.getKey()).toByteArray(), Utilities.serialize(entry.getValue()));
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
