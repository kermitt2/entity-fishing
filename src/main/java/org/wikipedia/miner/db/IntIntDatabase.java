package org.wikipedia.miner.db;

import org.apache.hadoop.record.CsvRecordInput;

import java.math.BigInteger;
import java.io.*;

import com.scienceminer.nerd.utilities.*;
import org.wikipedia.miner.util.ProgressTracker;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A {@link WDatabase} for associating Integer keys with a Long value object.
 *
 */
public abstract class IntIntDatabase extends WDatabase<Integer, Integer> {

	/**
	 * Creates or connects to a database, whose name will match the given {@link WDatabase.DatabaseType}
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param type the type of database
	 */
	public IntIntDatabase(WEnvironment envi, DatabaseType type) {
		super(envi, type) ;
	}
	
	/**
	 * Creates or connects to a database with the given name.
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param type the type of database
	 * @param name the name of the database 
	 */
	public IntIntDatabase(WEnvironment envi, DatabaseType type, String name) {
		super(envi, type, name) ;
	}
		
	// using standard LMDB copy mode
	//@Override
	public Integer retrieve2(Integer key) {
		/*if (isCached) {
			return cache.get(key);
		}*/

		byte[] cachedData = null;
		Integer record = null;
		try (Transaction tx = environment.createReadTransaction()) {
			cachedData = db.get(tx, BigInteger.valueOf(key).toByteArray());
			if (cachedData != null)
				record = new BigInteger(cachedData).intValue();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return record;
	}

	// using LMDB zero copy mode
	@Override
	public Integer retrieve(Integer key) {
		byte[] cachedData = null;
		Integer record = null;
		try (Transaction tx = environment.createReadTransaction();
			 BufferCursor cursor = db.bufferCursor(tx)) {
			cursor.keyWriteBytes(BigInteger.valueOf(key).toByteArray());
			if (cursor.seekKey()) {
				record = new BigInteger(cursor.valBytes()).intValue();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return record;
	}
	
	protected void add(WEntry<Integer,Integer> entry) {
		try (Transaction tx = environment.createWriteTransaction()) {
			db.put(tx, BigInteger.valueOf(entry.getKey()).toByteArray(), BigInteger.valueOf(entry.getValue()).toByteArray());
			tx.commit();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds the persistent database from a file.
	 * 
	 * @param dataFile the file (typically a CSV file) containing data to be loaded
	 * @param overwrite true if the existing database should be overwritten, otherwise false
	 * @param tracker an optional progress tracker (may be null)
	 * @throws IOException if there is a problem reading or deserialising the given data file.
	 */
	public void loadFromCsvFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException  {
		if (isLoaded && !overwrite)
			return ;

		if (tracker == null) tracker = new ProgressTracker(1, WDatabase.class) ;
		tracker.startTask(dataFile.length(), "Loading " + name + " database") ;

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8")) ;

		long bytesRead = 0 ;

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

			bytesRead = bytesRead + line.length() + 1 ;

			CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8"))) ;
			WEntry<Integer,Integer> entry = deserialiseCsvRecord(cri) ;
			if (entry != null) {
				try {
					db.put(tx, BigInteger.valueOf(entry.getKey()).toByteArray(), BigInteger.valueOf(entry.getValue()).toByteArray());
					nbToAdd++;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			tracker.update(bytesRead) ;
		}
		tx.commit();
		tx.close();
		input.close();
		isLoaded = true;
	}

}
