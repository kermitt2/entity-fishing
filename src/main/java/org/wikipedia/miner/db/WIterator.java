package org.wikipedia.miner.db;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.sleepycat.je.*;

//TODO: doesn't this always miss out on the last item?
/*TODO: what if the database is cached to memory? 
 * 
 * currently ignored, which is bad for speed, but more critically, would cause mismatches between data
 * available via lookup, and data available via iteration (the former might be filtered, and the latter will not be)
 * 
 * 
 */

/**
 * An iterator that will cycle through all entries in a WDatabase.
 * 
 * 
 *
 * @param <K>
 * @param <V>
 */
public class WIterator<K,V> implements Iterator<WEntry<K,V>> {

	WDatabase<K,V> db ;
	Cursor cursor ;
	
	WEntry<K,V> nextEntry ;
	
	DatabaseEntry key = new DatabaseEntry() ;
	DatabaseEntry value = new DatabaseEntry() ;

	/**
	 * Creates an iterator that will cycle through all entries the given WDatabase.
	 * 
	 * @param database an active (connected) WDatabase.
	 */
	public WIterator(WDatabase<K,V> database) {
		
		this.db = database ;
		cursor = db.getDatabase(true).openCursor(null, null) ;
		cursor.setCacheMode(CacheMode.UNCHANGED) ;

		queueNext() ;	
	}

	public boolean hasNext() {
		return (nextEntry != null);
	}

	public void remove() {
		throw new UnsupportedOperationException() ;
	}
	
	/**
	 * Tidily closes the {@link Cursor} underlying this iterator. This should be called before 
	 * destroying the object.
	 */
	public void close() {
		cursor.close();
		this.cursor = null ;
	}
	
	public void finalize() {
		if (this.cursor != null) {
			Logger.getLogger(WIterator.class).warn("Unclosed iterator. You may be causing a memory leak.") ;
		}
	}
	
	

	public WEntry<K,V> next()  {
		
		if (nextEntry == null)
			throw new NoSuchElementException() ;

		WEntry<K,V> e = nextEntry ;
		queueNext() ;

		return e ;
	}
	
	private void queueNext()  {
		
		if (cursor.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			
			K k = db.keyBinding.entryToObject(key) ;
			V v = db.valueBinding.entryToObject(value) ;
			
			nextEntry = new WEntry<K,V>(k,v) ;
		} else {
			nextEntry = null ;
		}
	}
}
