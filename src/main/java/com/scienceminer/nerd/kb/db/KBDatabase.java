package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.utilities.*;

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
import org.wikipedia.miner.util.*;

import java.util.concurrent.*;
import org.apache.hadoop.record.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public abstract class KBDatabase<K,V> {

	/**
	 * Database types
	 */
	public enum DatabaseType {
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
		 * Associates integer {@link KBEnvironment.StatisticName#ordinal()} with the value relevant to this statistic.
		 */
		statistics,

		/**
		 * Associates two integer id of articles via a relation type
		 */
		relations,

		/**
		 * Associates an integer id of article to a property (which is an attribute/value pair)
		 */
		properties
	}

	protected Env environment = null;
  	protected Database db = null;
  	protected String envFilePath = null;
  	protected boolean isLoaded = false;

	protected String name = null;
	protected DatabaseType type = null;
	protected KBEnvironment env = null;

	protected boolean isCached = false;
	protected ConcurrentMap<K,V> cache = null;

	/**
	 * Creates or connects to a database, whose name will match the given {@link KBDatabase.DatabaseType}
	 * 
	 * @param env the KBEnvironment surrounding this database
	 * @param type the type of database
	 */
	public KBDatabase(KBEnvironment env, DatabaseType type) {
		this.env = env;
		this.type = type;
		this.name = type.name();

		this.envFilePath = env.getConfiguration().getDatabaseDirectory() + "/" + type.toString();
		//System.out.println("db path: " + this.envFilePath);

		this.environment = new Env();
    	this.environment.setMapSize(100 * 1024 * 1024, ByteUnit.KIBIBYTES); 
    	File thePath = new File(this.envFilePath);
    	if (!thePath.exists()) {
    		thePath.mkdirs();
    		isLoaded = false;
    		System.out.println(type.toString() + " / isLoaded: " + isLoaded);
    	} else {
    		// we assume that if the DB files exist, it has been already loaded
    		isLoaded = true;
    	}
    	this.environment.open(envFilePath, Constants.NOTLS);
		db = this.environment.openDatabase();
	}

	/**
	 * Creates or connects to a database with the given name.
	 * 
	 * @param env the KBEnvironment surrounding this database
	 * @param type the type of database
	 * @param name the name of the database 
	 */
	public KBDatabase(KBEnvironment env, DatabaseType type, String name) {
		this.env = env;
		this.type = type;
		this.name = name;

		this.envFilePath = env.getConfiguration().getDatabaseDirectory() + "/" + name;
		this.environment = new Env();
    	this.environment.setMapSize(100 * 1024 * 1024, ByteUnit.KIBIBYTES); 
    	File thePath = new File(this.envFilePath);
    	if (!thePath.exists()) {
    		thePath.mkdirs();
    		isLoaded = false;
    		System.out.println(type.toString() + " / isLoaded: " + isLoaded);
    	} else {
    		// we assume that if the DB files exist, it has been already loaded
    		isLoaded = true;
    		System.out.println(type.toString() + " / isLoaded: " + isLoaded);
    	}
    	this.environment.open(envFilePath);
		db = this.environment.openDatabase();
	}

	public Database getDatabase() {
		return db;
	}

	public Env getEnvironment() {
		return environment;
	}

	/**
	 * Returns the type of this database
	 * 
	 * @return the type of this database
	 */
	public DatabaseType getType() {
		return type;
	}

	/**
	 * Returns the name of this database
	 * 
	 * @return the name of this database
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the number of entries in the database
	 * 
	 * @return the number of entries in the database
	 */
	public long getDatabaseSize() {
		//return getDatabase(true).count();

		Stat statistics = db.stat();
		return statistics.ms_entries;
	}

	/**
	 * Returns the number of entries that have been cached to memory
	 * 
	 * @return the number of entries that have been cached to memory
	 */
	public long getCacheSize() {
		if (!isCached)
			return 0;
		return cache.size();
	}

	/**
	 * Returns true if this has been cached to memory, otherwise false
	 * 
	 * @return true if this has been cached to memory, otherwise false
	 */
	public boolean isCached() {
		return isCached;
	}

	/**
	 * Retrieves the value associated with the given key, either from the persistent database, or from memory if
	 * the database has been cached. This will return null if the key is not found, or has been excluded from the cache.
	 * 
	 * @param key the key to search for
	 * @return the value associated with the given key, or null if none exists.
	 */
	public abstract V retrieve(K key);

	/**
	 * Deserialises a CSV record.
	 * 
	 * @param record the CSV record to deserialise
	 * @return the key,value pair encoded within the record
	 * @throws IOException if there is a problem decoding the record
	 */
	public abstract KBEntry<K,V> deserialiseCsvRecord(CsvRecordInput record) throws IOException;

	/**
	 * Decides whether an entry should be cached to memory or not, and optionally alters values before they are cached.
	 * 
	 * @param e the key,value pair to be filtered
	 * @param conf a configuration containing options for how the database is to be cached
	 * @return the value that should be cached along with the given key, or null if it should be excluded
	 */
	public V filterCacheEntry(KBEntry<K,V> e, WikipediaConfiguration conf) {
		// default, no filter
		return e.getValue();
	}

	/**
	 * Decides whether an entry should be indexed or not.
	 * 
	 * @param e the key,value pair to be filtered
	 * @param conf a configuration containing options for controling the indexing
	 * @return the value that should be cached along with the given key, or null if it should be excluded
	 */
	public V filterEntry(KBEntry<K,V> e) {
		// default, no filter
		return e.getValue();
	}

	/**
	 * Builds the persistent database from a file.
	 * 
	 * @param dataFile the file (here a CSV file) containing data to be loaded
	 * @param overwrite true if the existing database should be overwritten, otherwise false
	 * @throws IOException if there is a problem reading or deserialising the given data file.
	 */
	public abstract void loadFromCsvFile(File dataFile, boolean overwrite) throws IOException;

	/**
	 * @return an iterator for the entries in this database, in ascending key order.
	 */
	public KBIterator getIterator() {
		return new KBIterator(this);
	}

	/**
	 * Closes the underlying database
	 */
	public void close() {
		if (db != null)
			db.close();
    	if (environment != null)
	    	environment.close();
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
	public void caching(WikipediaConfiguration conf) {
		cache = new ConcurrentHashMap<K,V>();

		KBIterator iter = getIterator();
		while (iter.hasNext()) {
			Entry entry = iter.next();
			byte[] keyData = entry.getKey();
			byte[] valueData = entry.getValue();
			try {
				V pa = (V)Utilities.deserialize(valueData);
				K keyId = (K)Utilities.deserialize(keyData);

				KBEntry<K,V> kbEntry = new KBEntry<K,V>(keyId, pa);
				V filteredValue = filterCacheEntry(kbEntry, conf);

				if (filteredValue != null) {
					cache.put(keyId, filteredValue);
				}
			} catch(Exception exp) {
				Logger.getLogger(KBDatabase.class).error("Failed caching deserialize");
			}
		}

		iter.close();
		isCached = true;
	}

	public boolean isLoaded() {
		return isLoaded;
	}
}
