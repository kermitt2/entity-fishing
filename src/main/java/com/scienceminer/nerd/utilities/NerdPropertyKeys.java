package com.scienceminer.nerd.utilities;

/**
 * This class contains all the keys of the properties files.
 * 
 * @author Patrice
 * 
 */
public interface NerdPropertyKeys {

	public static final String PROP_NERD_IS_CONTEXT_SERVER = "com.scienceminer.nerd.is.context.server";

	public static final String PROP_TMP_PATH = "com.scienceminer.nerd.temp.path";
	public static final String PROP_TEST_PATH = "com.scienceminer.nerd.test.path";
	public static final String PROP_USE_LANG_ID = "com.scienceminer.nerd.use_language_id";
	public static final String PROP_LANG_DETECTOR_FACTORY = "com.scienceminer.nerd.language_detector_factory";

	public static final String PROP_PROXY_HOST = "com.scienceminer.nerd.proxy_host";
	public static final String PROP_PROXY_PORT = "com.scienceminer.nerd.proxy_port";

	public static final String PROP_REST_CACHE = "com.scienceminer.nerd.cache_rest";
	public static final String PROP_MONGODB_HOST = "com.scienceminer.nerd.mongodb_host";
	public static final String PROP_MONGODB_PORT = "com.scienceminer.nerd.mongodb_port";

	public static final String PROP_MYSQL_HOST = "com.scienceminer.nerd.mysql_host";
	public static final String PROP_MYSQL_PORT = "com.scienceminer.nerd.mysql_port";
	public static final String PROP_MYSQL_USERNAME = "com.scienceminer.nerd.mysql_username";
	public static final String PROP_MYSQL_PASSWD = "com.scienceminer.nerd.mysql_passwd";
	public static final String PROP_MYSQL_DBNAME = "com.scienceminer.nerd.mysql_dbname";

	public static final String PROP_GROBID_HOME = "com.scienceminer.nerd.grobid_home";
	public static final String PROP_GROBID_PROPERTIES = "com.scienceminer.nerd.grobid_properties";

	//public static final String PROP_IDILIA_ACCESS_KEY = "com.scienceminer.nerd.idilia_access_key";
	//public static final String PROP_IDILIA_PRIVATE_KEY = "com.scienceminer.nerd.idilia_private_key";

	public static final String PROP_ES_HOST = "com.scienceminer.nerd.elasticSearch_host";
	public static final String PROP_ES_PORT = "com.scienceminer.nerd.elasticSearch_port";
	public static final String PROP_ES_KB_NAME = "com.scienceminer.nerd.KnowledgeBaseESName";
	public static final String PROP_ES_CLUSTER = "com.scienceminer.nerd.elasticSearch_cluster";
	public static final String PROP_ES_LEXICON_NAME = "com.scienceminer.nerd.LexiconESName";
	
	public static final String PROP_WIKIPEDIA_MINER_CONFIG = "com.scienceminer.nerd.WikipediaMinerConfigPath";

	public static final String PROP_MAPDB_PATH = "com.scienceminer.nerd.mapdb.path";
	
	/**
	 * Path to wordnet domain file.
	 */
	//public static final String PROP_NERD_WORDNET_DOMAINS ="com.scienceminer.nerd.wordnet_path";

	/**
	 * The name of the env-entry located in the web.xml, via which the
	 * nerd-service.propeties path is set.
	 */
	public static final String PROP_NERD_HOME = "com.scienceminer.nerd.home";

	/**
	 * The name of the env-entry located in the web.xml, via which the
	 * com.scienceminer.nerd.properties path is set.
	 */
	public static final String PROP_NERD_PROPERTY = "com.scienceminer.nerd.property";

	/**
	 * The name of the system property, via which the NERD home folder can be
	 * located.
	 */
	public static final String PROP_NERD_SERVICE_PROPERTY = "com.scienceminer.nerd.property.service";

	/**
	 * name of the property setting the admin password
	 */
	public static final String PROP_NERD_SERVICE_ADMIN_PW = "com.scienceminer.nerd.service.admin.pw";

	/**
	 * If set to true, parallel execution will be done, else a queuing of
	 * requests will be done.
	 */
	public static final String PROP_NERD_SERVICE_IS_PARALLEL_EXEC = "com.scienceminer.nerd.service.is.parallel.execution";

	/**
	 * The defined paths to create.
	 */
	public static final String[] PATHES_TO_CREATE = { PROP_TMP_PATH };

}
