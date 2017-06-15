package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.utilities.*;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

import org.nustaq.serialization.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.kb.model.hadoop.*; 
import com.scienceminer.nerd.kb.model.Wikipedia;

import org.apache.hadoop.record.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A KB corresponding to a Wikipedia instance, which is concretely stored as a set of LMDB databases.
 * 
 */
public abstract class KBEnvironment {
	private static final Logger LOGGER = LoggerFactory.getLogger(KBEnvironment.class);	
	
	// this is the singleton FST configuration for all serialization operations with the KB
	protected static FSTConfiguration singletonConf = FSTConfiguration.createDefaultConfiguration();
    //protected static FSTConfiguration singletonConf = FSTConfiguration.createUnsafeBinaryConfiguration(); 

    public static FSTConfiguration getFSTConfigurationInstance() {
        return singletonConf;
    }
	
	/**
	 * Serialization in the KBEnvironment with FST
	 */
    public static byte[] serialize(Object obj) throws IOException {
    	byte data[] = getFSTConfigurationInstance().asByteArray(obj);
		return data;
	}

	/**
	 * Deserialization in the KBEnvironment with FST. The returned Object needs to be casted
	 * in the expected actual object. 
	 */
	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		return getFSTConfigurationInstance().asObject(data);
	}

	// NERD configuration for the KB instance
	protected NerdConfig conf = null;

	// database registry for the environment
	protected Map<DatabaseType, KBDatabase> databasesByType = null;
	
	/**
	 * Constructor
	 */	
	public KBEnvironment(NerdConfig conf) {
		this.conf = conf;
		// register classes to be serialized
		//singletonConf.registerClass(DbPage.class, DbIntList.class, DbTranslations.class);
		//initDatabases();
	}

	/**
	 * Returns the configuration of this environment
	 */
	public NerdConfig getConfiguration() {
		return conf;
	}
	
	protected abstract void initDatabases();

	protected KBDatabase getDatabase(DatabaseType dbType) {
		return databasesByType.get(dbType);
	}
	
	public void close() {
		for (KBDatabase db:this.databasesByType.values()) {
			db.close();
		}
	}
	
	/**
	 * @param sn the name of the desired statistic
	 * @return the value of the desired statistic
	 */
	public abstract Long retrieveStatistic(StatisticName sn);

	/**
	 * Builds a KBEnvironment, by loading all of the data files stored in the given directory into persistent databases.
	 * 
	 * It will not create the environment or any databases unless all of the required files are found in the given directory. 
	 * 
	 * It will not delete any existing databases, and will only overwrite them if explicitly specified (even if they are incomplete).
	 * 
	 * @param conf a configuration specifying where the databases are to be stored, etc.
	 * @param overwrite true if existing databases should be overwritten, otherwise false
	 * @throws IOException if any of the required files cannot be read
	 * @throws XMLStreamException if the XML dump of wikipedia cannot be parsed
	 */
	public abstract void buildEnvironment(NerdConfig conf, boolean overwrite) throws Exception;

	protected static File getDataFile(File dataDirectory, String fileName) throws IOException {
		File file = new File(dataDirectory + File.separator + fileName);
		if (!file.canRead()) {
			LOGGER.info(file + " is not readable");
			return null;
		} else
			return file;
	}

	/**
	 * Statistics available about a wikipedia dump
	 */
	public enum StatisticName {
		/**
		 * The number of articles (not disambiguations or redirects) available
		 */
		articleCount,
		
		/**
		 * The number of categories available
		 */
		categoryCount,
		
		/**
		 * The number of disambiguation pages available
		 */
		disambiguationCount,
		
		/**
		 * The number of redirects available
		 */
		redirectCount,
		
		/**
		 * A long value representation of the date and time this dump was last edited -- use new Date(long) to get to parse
		 */
		lastEdit,
		
		/**
		 * The maximum path length between articles and the root category 
		 */
		maxCategoryDepth,
		
		/**
		 * The id of root category, below which all articles should be organized 
		 */
		rootCategoryId 
	}

}
