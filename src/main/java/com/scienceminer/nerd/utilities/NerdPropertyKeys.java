package com.scienceminer.nerd.utilities;

/**
 * This class contains all the keys of the properties files.
 * 
 * 
 */
public interface NerdPropertyKeys {

	String PROP_NERD_IS_CONTEXT_SERVER = "com.scienceminer.nerd.is.context.server";

	String PROP_TMP_PATH = "com.scienceminer.nerd.temp.path";
	String PROP_TEST_PATH = "com.scienceminer.nerd.test.path";
	String PROP_USE_LANG_ID = "com.scienceminer.nerd.use_language_id";
	String PROP_LANG_DETECTOR_FACTORY = "com.scienceminer.nerd.language_detector_factory";

	String PROP_PROXY_HOST = "com.scienceminer.nerd.proxy_host";
	String PROP_PROXY_PORT = "com.scienceminer.nerd.proxy_port";

	String PROP_REST_CACHE = "com.scienceminer.nerd.cache_rest";
	String PROP_MONGODB_HOST = "com.scienceminer.nerd.mongodb_host";
	String PROP_MONGODB_PORT = "com.scienceminer.nerd.mongodb_port";

	String PROP_MYSQL_HOST = "com.scienceminer.nerd.mysql_host";
	String PROP_MYSQL_PORT = "com.scienceminer.nerd.mysql_port";
	String PROP_MYSQL_USERNAME = "com.scienceminer.nerd.mysql_username";
	String PROP_MYSQL_PASSWD = "com.scienceminer.nerd.mysql_passwd";
	String PROP_MYSQL_DBNAME = "com.scienceminer.nerd.mysql_dbname";

	String PROP_GROBID_HOME = "com.scienceminer.nerd.grobid_home";
	String PROP_GROBID_PROPERTIES = "com.scienceminer.nerd.grobid_properties";

	//String PROP_IDILIA_ACCESS_KEY = "com.scienceminer.nerd.idilia_access_key";
	//String PROP_IDILIA_PRIVATE_KEY = "com.scienceminer.nerd.idilia_private_key";

	String PROP_ES_HOST = "com.scienceminer.nerd.elasticSearch_host";
	String PROP_ES_PORT = "com.scienceminer.nerd.elasticSearch_port";
	String PROP_ES_KB_NAME = "com.scienceminer.nerd.KnowledgeBaseESName";
	String PROP_ES_CLUSTER = "com.scienceminer.nerd.elasticSearch_cluster";
	String PROP_ES_LEXICON_NAME = "com.scienceminer.nerd.LexiconESName";
	
	String PROP_WIKIPEDIA_MINER_CONFIG = "com.scienceminer.nerd.WikipediaMinerConfigPath";

	String PROP_MAPDB_PATH = "com.scienceminer.nerd.mapdb.path";
	String PROP_MAPS_PATH = "com.scienceminer.nerd.maps.path";
	
	/**
	 * Path to wordnet domain file.
	 */
	//String PROP_NERD_WORDNET_DOMAINS ="com.scienceminer.nerd.wordnet_path";

	/**
	 * The name of the env-entry located in the web.xml, via which the
	 * nerd-service.propeties path is set.
	 */
	String PROP_NERD_HOME = "com.scienceminer.nerd.home";

	/**
	 * The name of the env-entry located in the web.xml, via which the
	 * com.scienceminer.nerd.properties path is set.
	 */
	String PROP_NERD_PROPERTY = "com.scienceminer.nerd.property";

	/**
	 * The name of the system property, via which the NERD home folder can be
	 * located.
	 */
	String PROP_NERD_SERVICE_PROPERTY = "com.scienceminer.nerd.property.service";

	/**
	 * name of the property setting the admin password
	 */
	String PROP_NERD_SERVICE_ADMIN_PW = "com.scienceminer.nerd.service.admin.pw";

	/**
	 * If set to true, parallel execution will be done, else a queuing of
	 * requests will be done.
	 */
	String PROP_NERD_SERVICE_IS_PARALLEL_EXEC = "com.scienceminer.nerd.service.is.parallel.execution";

	/**
	 * The defined paths to create.
	 */
	String[] PATHES_TO_CREATE = { PROP_TMP_PATH };

}
