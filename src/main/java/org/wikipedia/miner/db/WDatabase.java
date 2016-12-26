package org.wikipedia.miner.db;

//import gnu.trove.map.hash.THashMap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.*;

import javax.xml.stream.XMLStreamException;

import org.apache.hadoop.record.CsvRecordInput;
import org.apache.log4j.Logger;
import org.wikipedia.miner.db.struct.*;
import org.wikipedia.miner.util.ProgressTracker;
import org.wikipedia.miner.util.WikipediaConfiguration;


import com.sleepycat.bind.EntryBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import java.util.concurrent.*;

/**
 * A wrapper for {@link Database} that adds the ability to load itself from data files and selectively caching itself to memory.
 *
 * It is unlikely that you will want to use this class directly.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public abstract class WDatabase<K,V> {

	/**
	 * Database types
	 */
	public enum DatabaseType  
	{
		/**
		 * Associates page ids with the title, type and generality of the page. 
		 */
		page, 

		/**
		 * Associates String labels with the statistics about the articles (senses) these labels could refer to 
		 */
		label,

		/**
		 * Associates Integer page ids with the labels used to refer to that page
		 */
		pageLabel,

		/**
		 * Associates String titles with the id of the page within the article namespace that this refers to
		 */
		articlesByTitle,

		/**
		 * Associates String titles with the id of the page within the category namespace that this refers to
		 */
		categoriesByTitle,

		/**
		 * Associates String titles with the id of the page within the template namespace that this refers to
		 */
		templatesByTitle,


		/**
		 * Associates integer ids with the ids of articles that link to it, and the sentence indexes where these links are found
		 */
		pageLinksIn, 

		/**
		 * Associates integer ids with the ids of articles that link to it
		 */
		pageLinksInNoSentences,


		/**
		 * Associates integer ids with the ids of articles that it links to, and the sentence indexes where these links are found
		 */
		pageLinksOut, 

		/**
		 * Associates integer ids with the ids of articles that it links to
		 */
		pageLinksOutNoSentences,

		/**
		 * Associates integer ids with counts of how many pages it links to or that link to it
		 */
		pageLinkCounts,

		/**
		 * Associates integer ids of categories with the ids of categories it belongs to
		 */
		categoryParents, 

		/**
		 * Associates integer ids of articles with the ids of categories it belongs to
		 */
		articleParents, 

		/**
		 * Associates integer ids of categories with the ids of categories that belong to it
		 */
		childCategories, 

		/**
		 * Associates integer ids of categories with the ids of articles that belong to it
		 */
		childArticles, 

		/**
		 * Associates integer id of redirect with the id of its target
		 */
		redirectTargetBySource,

		/**
		 * Associates integer id of article with the redirects that target it
		 */
		redirectSourcesByTarget,

		/**
		 * Associates integer id of page with the character indexes of sentence breaks within it
		 */
		sentenceSplits,

		/**
		 * Associates integer id of page with a {@link DbTranslations}. 
		 */
		translations,

		/**
		 * Associates integer id of page with its content, in mediawiki markup format
		 */
		markup,

		/**
		 * Associates integer {@link WEnvironment.StatisticName#ordinal()} with the value relevant to this statistic.
		 */
		statistics
	}


	/**
	 * Options for caching data to memory
	 */
	public enum CachePriority {

		/**
		 * Focus on speed, by storing values directly
		 */
		speed,

		/**
		 * Focus on memory, by compressing values before storing them.
		 */
		space
	}

	private String name ;
	private DatabaseType type ;

	protected WEnvironment env ;
	private Database database ;

	protected EntryBinding<K> keyBinding ;
	protected EntryBinding<V> valueBinding ;

	private boolean isCached = false ;
	private CachePriority cachePriority = CachePriority.space ;

	//private THashMap<K,byte[]> compactCache = null ;
	//private THashMap<K,V> fastCache = null ;
	private ConcurrentMap<K,byte[]> compactCache = null ;
	private ConcurrentMap<K,V> fastCache = null ;

	/**
	 * Creates or connects to a database, whose name will match the given {@link WDatabase.DatabaseType}
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param type the type of database
	 * @param keyBinding a binding for serialising and deserialising keys
	 * @param valueBinding a binding for serialising and deserialising values
	 */
	public WDatabase(WEnvironment env, DatabaseType type, EntryBinding<K> keyBinding, EntryBinding<V> valueBinding) {

		this.env = env ;
		this.type = type ;
		this.name = type.name() ;

		this.keyBinding = keyBinding ;
		this.valueBinding = valueBinding ;

		this.database = null ;
	}

	/**
	 * Creates or connects to a database with the given name.
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param type the type of database
	 * @param name the name of the database 
	 * @param keyBinding a binding for serialising and deserialising keys
	 * @param valueBinding a binding for serialising and deserialising values 
	 */
	public WDatabase(WEnvironment env, DatabaseType type, String name, EntryBinding<K> keyBinding, EntryBinding<V> valueBinding) {

		this.env = env ;
		this.type = type ;
		this.name = name ;

		this.keyBinding = keyBinding ;
		this.valueBinding = valueBinding ;

		this.database = null ;
	}

	/**
	 * Returns the type of this database
	 * 
	 * @return the type of this database
	 */
	public DatabaseType getType() {
		return type ;
	}

	/**
	 * Returns the name of this database
	 * 
	 * @return the name of this database
	 */
	public String getName() {
		return name ;
	}

	/**
	 * Returns the number of entries in the database
	 * 
	 * @return the number of entries in the database
	 */
	public long getDatabaseSize() {
		return getDatabase(true).count();
	}

	/**
	 * Returns the number of entries that have been cached to memory
	 * 
	 * @return the number of entries that have been cached to memory
	 */
	public long getCacheSize() {
		if (!isCached)
			return 0 ;

		if (cachePriority == CachePriority.space)
			return fastCache.size();
		else
			return compactCache.size();
	}

	/**
	 * Returns true if this has been cached to memory, otherwise false
	 * 
	 * @return true if this has been cached to memory, otherwise false
	 */
	public boolean isCached() {
		return isCached ;
	}

	/**
	 * Returns whether this has been cached for speed or memory efficiency
	 * 
	 * @return whether this has been cached for speed or memory efficiency
	 */
	public CachePriority getCachePriority() {
		return cachePriority ;
	}

	/**
	 * true if there is a persistent database underlying this, otherwise false
	 * 
	 * @return true if there is a persistent database underlying this, otherwise false
	 */
	public boolean exists() {
		try {
			getDatabase(true) ;
		} catch(DatabaseNotFoundException e) {
			return false ;
		}
		return true ;
	}

	/**
	 * Retrieves the value associated with the given key, either from the persistent database, or from memory if
	 * the database has been cached. This will return null if the key is not found, or has been excluded from the cache.
	 * 
	 * @param key the key to search for
	 * @return the value associated with the given key, or null if none exists.
	 */
	public V retrieve(K key) {

		if (isCached) {
			//System.out.println("c") ;
			return retrieveFromCache(key) ;
		} else {
			//System.out.println("d") ;
			Database db = getDatabase(true) ;

			DatabaseEntry dbKey = new DatabaseEntry() ;
			keyBinding.objectToEntry(key, dbKey) ;

			DatabaseEntry dbValue = new DatabaseEntry() ;

			OperationStatus os = db.get(null, dbKey, dbValue, LockMode.READ_COMMITTED) ; 

			if (!os.equals(OperationStatus.SUCCESS)) 
				return null ;
			else
				return valueBinding.entryToObject(dbValue) ;
		}
	}

	/**
	 * Deserialises a CSV record.
	 * 
	 * @param record the CSV record to deserialise
	 * @return the key,value pair encoded within the record
	 * @throws IOException if there is a problem decoding the record
	 */
	public abstract WEntry<K,V> deserialiseCsvRecord(CsvRecordInput record) throws IOException ;

	/**
	 * Decides whether an entry should be cached to memory or not, and optionally alters values before they are cached.
	 * 
	 * @param e the key,value pair to be filtered
	 * @param conf a configuration containing options for how the database is to be cached
	 * @param validIds the set of article ids that are valid and should be cached
	 * @return the value that should be cached along with the given key, or null if it should be excluded
	 */
	public abstract V filterCacheEntry(WEntry<K,V> e, WikipediaConfiguration conf) ;


	/**
	 * Builds the persistent database from a file.
	 * 
	 * @param dataFile the file (typically a CSV file) containing data to be loaded
	 * @param overwrite true if the existing database should be overwritten, otherwise false
	 * @param tracker an optional progress tracker (may be null)
	 * @throws IOException if there is a problem reading or deserialising the given data file.
	 */
	public void loadFromCsvFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException  {

		if (exists() && !overwrite)
			return ;

		if (tracker == null) tracker = new ProgressTracker(1, WDatabase.class) ;
		tracker.startTask(dataFile.length(), "Loading " + name + " database") ;

		Database db = getDatabase(false) ;

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8")) ;

		long bytesRead = 0 ;
		int lineNum = 0 ;

		String line ;
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() + 1 ;
			lineNum++ ;

			CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8"))) ;

			WEntry<K,V> entry = deserialiseCsvRecord(cri) ;

			if (entry != null) {
				DatabaseEntry k = new DatabaseEntry() ;
				keyBinding.objectToEntry(entry.getKey(), k) ;

				DatabaseEntry v = new DatabaseEntry() ;
				valueBinding.objectToEntry(entry.getValue(), v) ;

				db.put(null, k, v) ;
			}

			tracker.update(bytesRead) ;
		}

		input.close();

		env.cleanAndCheckpoint() ;
		getDatabase(true) ;
	}


	/**
	 * Selectively caches records from the database to memory, for much faster lookup.
	 * 
	 * @param conf a configuration specifying how items should be cached.
	 * @param validIds an optional set of article ids that should be cached. Any information about articles not in this list will be excluded from the cache. 
	 * @param tracker an optional progress tracker
	 * @throws IOException 
	 * @throws DatabaseException 
	 */
	public void cache(WikipediaConfiguration conf, ProgressTracker tracker) throws DatabaseException, IOException {

		Database db = getDatabase(true) ;

		this.cachePriority = conf.getCachePriority(type) ;

		initializeCache() ;

		if (tracker == null) 
			tracker = new ProgressTracker(1, WDatabase.class) ;

		tracker.startTask(db.count(), "caching " + name + " database") ;

		//first, try caching from file
		if (conf.getDatabaseDirectory() != null) {
			File dataFile = new File(conf.getDatabaseDirectory() + File.separator + name + ".csv") ; 

			if (dataFile.canRead()) {

				BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8")) ;

				long lineNum = 0 ;

				String line ;
				while ((line=input.readLine()) != null) {
					lineNum++ ;

					CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8"))) ;

					WEntry<K,V> entry = deserialiseCsvRecord(cri) ;

					if (entry != null) {
						V filteredValue = filterCacheEntry(entry, conf) ;

						if (filteredValue != null) {
							WEntry<K,V> filteredEntry = new WEntry<K,V>(entry.getKey(), filteredValue) ;
							addToCache(filteredEntry) ;
						}
					}

					tracker.update(lineNum) ;
				}

				input.close();	
				finalizeCache() ;

				return;
			}
		}



		//we haven't managed to cache from file, so let's do it from db
		WIterator<K,V> iter = getIterator() ;


		while (iter.hasNext()) {
			WEntry<K,V> entry = iter.next();

			V filteredValue = filterCacheEntry(entry, conf) ;

			if (filteredValue != null) {
				WEntry<K,V> filteredEntry = new WEntry<K,V>(entry.getKey(), filteredValue) ;
				addToCache(filteredEntry) ;
			}

			tracker.update() ;
		}

		iter.close();
		finalizeCache() ;
	}

	/**
	 * @return an iterator for the entries in this database, in ascending key order.
	 */
	public WIterator<K,V> getIterator() {
		return new WIterator<K,V>(this) ;
	}

	/**
	 * Closes the underlying database
	 */
	public void close() {

		if (database != null) {
			database.close() ;
			database = null ;
		}

		fastCache = null ;
		compactCache = null ;
	}

	@Override
	public void finalize() {
		if (database != null) {
			Logger.getLogger(WIterator.class).warn("Unclosed database '" + name + "'. You may be causing a memory leak.") ;
		}
	}


	protected V retrieveFromCache(K key) {

		if (cachePriority == CachePriority.speed) {
			return fastCache.get(key) ;
		} else {
			byte[] cachedData = compactCache.get(key) ;

			if (cachedData == null)
				return null ;

			DatabaseEntry dbValue = new DatabaseEntry(cachedData) ;
			return valueBinding.entryToObject(dbValue) ;
		}
	}


	protected void initializeCache() {

		if (cachePriority == CachePriority.speed) {
			fastCache = new ConcurrentHashMap<K,V>();
			//fastCache = new THashMap<K,V>() ;
		}
		else {
			compactCache = new ConcurrentHashMap<K,byte[]>();
			//compactCache = new THashMap<K,byte[]>() ;
		}
	}

	protected void addToCache(WEntry<K,V> entry) {

		if (cachePriority == CachePriority.speed) {
			fastCache.put(entry.getKey(), entry.getValue()) ;
		} else {
			DatabaseEntry cacheValue = new DatabaseEntry() ;
			valueBinding.objectToEntry(entry.getValue(), cacheValue) ;
			compactCache.put(entry.getKey(), cacheValue.getData()) ;
		}
	}

	protected void finalizeCache() {
		this.isCached = true ;
	}

	protected Database getDatabase(boolean readOnly) throws DatabaseException {

		DatabaseConfig conf = new DatabaseConfig() ;

		conf.setReadOnly(readOnly) ;
		conf.setAllowCreate(!readOnly) ;
		conf.setExclusiveCreate(!readOnly) ;

		if (database != null) {
			if (database.getConfig().getReadOnly() == readOnly) {
				//the database is already open as it should be.
				return database ;
			} else {
				//the database needs to be closed and re-opened.
				database.close();
			}
		}

		if (!readOnly) {
			try {
				env.getEnvironment().removeDatabase(null, name) ;
			} catch (DatabaseNotFoundException e) {} ;
		}

		database = env.getEnvironment().openDatabase(null, name, conf);
		return database ;
	}
}
