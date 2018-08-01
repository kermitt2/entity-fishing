package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.kb.Property;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.kb.model.hadoop.DbIntList;
import com.scienceminer.nerd.kb.model.hadoop.DbPage;
import com.scienceminer.nerd.kb.model.hadoop.DbTranslations;
import com.scienceminer.nerd.utilities.NerdConfig;
import org.nustaq.serialization.FSTConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.util.Map;

/**
 * A KB corresponding to a Wikipedia instance, which is concretely stored as a set of LMDB databases.
 *
 * -> should be renamed LowerKBEnvironment
 * 
 */
public abstract class KBEnvironment implements Closeable {
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
    public static byte[] serialize(Object obj) {
    	byte data[] = getFSTConfigurationInstance().asByteArray(obj);
		return data;
	}

	/**
	 * Deserialization in the KBEnvironment with FST. The returned Object needs to be casted
	 * in the expected actual object. 
	 */
	public static Object deserialize(byte[] data) {
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
		singletonConf.registerClass(DbPage.class, DbIntList.class, DbTranslations.class, Property.class, Statement.class);
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
	
	public abstract Long retrieveStatistic(StatisticName sn);

	public abstract void buildEnvironment(NerdConfig conf, boolean overwrite) throws Exception;

	protected static File getDataFile(File dataDirectory, String fileName) {
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
	public enum 	StatisticName {
		/**
		 * number of articles (not disambiguations or redirects)
		 */
		articleCount,
		
		/**
		 * number of categories
		 */
		categoryCount,
		
		/**
		 * number of disambiguation pages
		 */
		disambiguationCount,
		
		/**
		 * number of redirects
		 */
		redirectCount,
		
		/**
		 * date and time this dump was last edited, use new Date(long)
		 */
		lastEdit,
		
		/**
		 * maximum path length between articles and the root category 
		 */
		maxCategoryDepth,
		
		/**
		 * id of root category
		 */
		rootCategoryId 
	}

}
