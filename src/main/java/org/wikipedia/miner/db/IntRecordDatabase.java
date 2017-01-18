package org.wikipedia.miner.db;

//import gnu.trove.map.hash.TIntObjectHashMap;

/*import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.DatabaseEntry;*/

import org.apache.hadoop.record.Record;
import org.apache.hadoop.record.CsvRecordInput;

//import org.apache.commons.lang.SerializationUtils;

//import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.ConcurrentHashMap;

import java.math.BigInteger;
import java.io.*;

import com.scienceminer.nerd.utilities.*;
import org.wikipedia.miner.util.ProgressTracker;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A {@link WDatabase} for associating Integer keys with some generic record value type.
 *
 * @param <V> the type of object to store as values
 */
public abstract class IntRecordDatabase<Record> extends WDatabase<Integer, Record> {

	/**
	 * Creates or connects to a database, whose name will match the given {@link WDatabase.DatabaseType}
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param type the type of database
	 */
	public IntRecordDatabase(WEnvironment envi, DatabaseType type) {
		super(envi, type) ;
	}
	
	
	/**
	 * Creates or connects to a database with the given name.
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param type the type of database
	 * @param name the name of the database 
	 */
	public IntRecordDatabase(WEnvironment envi, DatabaseType type, String name) {
		super(envi, type, name) ;
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

	protected void add(WEntry<Integer,Record> entry) {
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
	 * @param dataFile the file (typically a CSV file) containing data to be loaded
	 * @param overwrite true if the existing database should be overwritten, otherwise false
	 * @param tracker an optional progress tracker (may be null)
	 * @throws IOException if there is a problem reading or deserialising the given data file.
	 */
	public void loadFromCsvFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException  {
		if (isLoaded && !overwrite)
			return ;
		if (tracker == null) tracker = new ProgressTracker(1, WDatabase.class);
		tracker.startTask(dataFile.length(), "Loading " + name + " database");

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
			try {
				WEntry<Integer,Record> entry = deserialiseCsvRecord(cri);

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

			tracker.update(bytesRead) ;
		}
		tx.commit();
		tx.close();
		input.close();
		isLoaded = true;
	}

}
