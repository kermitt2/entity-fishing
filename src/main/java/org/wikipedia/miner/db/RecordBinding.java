package org.wikipedia.miner.db;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.je.*;
import org.apache.hadoop.record.* ;



import java.io.* ;

import org.apache.log4j.Logger; 



/**
 * An {@link EntryBinding} capable of converting between {@link Record Records} and {@link DatabaseEntry DatabaseEntries}.
 *
 * This takes advantage of <a href="http://hadoop.apache.org/common/docs/current/api/org/apache/hadoop/record/package-summary.html">Hadoop's record package</a> to compress Records when storing them. 
 *
 * @param <T> the type of record 
 */
public abstract class RecordBinding<T extends Record> implements EntryBinding<T>{
	
	/**
	 * Constructs an empty instance of T.
	 * 
	 * @return an empty instance of T.
	 */
	public abstract T createRecordInstance() ;
	
	private T bytesToObject(byte[] bytes) {
		
		BinaryRecordInput bri = new BinaryRecordInput(new ByteArrayInputStream(bytes)) ;

		T record = createRecordInstance() ;

		try {
			record.deserialize(bri) ;
			return record ;
		} catch (IOException e) {
			Logger.getLogger(RecordBinding.class).error("Could not deserialize byte data record", e) ;
		}
		return null ;
	}
	
	public T entryToObject(DatabaseEntry input) {

		return bytesToObject(input.getData()) ;
	}

	public void objectToEntry(T object, DatabaseEntry entry) {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ;
		BinaryRecordOutput recordOutput = new BinaryRecordOutput(outputStream) ;

		try {
			object.serialize(recordOutput) ;
			entry.setData(outputStream.toByteArray()) ;
		} catch (IOException e) {
			Logger.getLogger(RecordBinding.class).error("Could not serialize record for database entry", e) ;
		}

		entry = null ;
	}

}
