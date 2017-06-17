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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.model.hadoop.*;

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
		 * Associates integer ids with the ids of articles that link to it
		 */
		pageLinksInNoSentences,

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
		 * Associates integer id of page with a {@link DbTranslations}. 
		 */
		translations,

		/**
		 * Associates integer id of page with its first paragraph/definition, in mediawiki markup format
		 */
		markup,

		/**
		 * Associates integer id of page with its full content/article, in mediawiki markup format
		 */
		markupFull,

		/**
		 * Associates integer {@link KBEnvironment.StatisticName#ordinal()} with the value relevant to this statistic.
		 */
		statistics,

		/**
		 * Associates integer id of page with the identifier of the corresponding language-independent concept in the KB
		 */
		conceptByPageId,

		/**
		 * Associates one concept id (the subject of the statetement) to a property and values (litteral values 
		 * and/or other concept identifiers)
		 */
		statements,

		/**
		 * Associates the string identifier of a property to a property description
		 */
		properties,

		/**
		 * Associates an integer id of a concept to the language-specific mapping to article id
		 */
		concepts
	}

	protected Env environment = null;
  	protected Database db = null;
  	protected String envFilePath = null;
  	protected boolean isLoaded = false;

	protected String name = null;
	protected DatabaseType type = null;
	protected KBEnvironment env = null;

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

		this.envFilePath = env.getConfiguration().getDbDirectory() + "/" + type.toString();
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

		this.envFilePath = env.getConfiguration().getDbDirectory() + "/" + name;
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
	 * Builds the persistent database from a file (CSV normally, or JSON).
	 * 
	 * @param dataFile the file (CSV , JSON or XML file) containing data to be loaded
	 * @param overwrite indicate if the existing database should be overwritten
	 */
	public abstract void loadFromFile(File dataFile, boolean overwrite) throws Exception;

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

	public boolean isLoaded() {
		return isLoaded;
	}
}
