package com.scienceminer.nerd.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.net.URL;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import com.scienceminer.nerd.exceptions.NerdPropertyException;
import com.scienceminer.nerd.exceptions.NerdResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class loads contains all names of Nerd-properties and provide methods
 * to load Nerd-properties from a property file. Each property will be copied
 * to a system property having the same name.
 * 
 * @author Patrice Lopez
 */
public class NerdProperties {

	public static final Logger LOGGER = LoggerFactory.getLogger(NerdProperties.class);

	/**
	 * The context of the application.
	 */
	protected static Context context;

	/**
	 * name of property which determines, if Nerd runs in test mode.
	 */
	public static final String PROP_TEST_MODE = "Nerd.testMode";

	/**
	 * A static {@link NerdProperties} object containing all properties used
	 * by Nerd.
	 */
	private static NerdProperties NerdProperties = null;

	/**
	 * Path to Nerd.property.
	 */
	protected static File NERD_PROPERTY_PATH = null;

	/**
	 * Internal property object, where all properties are defined.
	 */
	protected static Properties props = null;

	/**
	 * Resets this class and all its static fields. For instance sets the
	 * current object to null.
	 */
	public static void reset() {
		NerdProperties = null;
		props = null;
		NERD_PROPERTY_PATH = null;
	}

	/**
	 * Returns a static {@link NerdProperties} object. If no one is set, then
	 * it creates one. {@inheritDoc #NerdProperties()}
	 * 
	 * @return
	 */
	public static NerdProperties getInstance() {
		if (NerdProperties == null)
			return getNewInstance();
		else
			return NerdProperties;
	}

	/**
	 * Reload NerdServiceProperties.
	 */
	public static void reload() {
		getNewInstance();
	}

	/**
	 * Creates a new {@link NerdProperties} object, initializes it and returns
	 * it. {@inheritDoc #NerdProperties()}
	 * 
	 * @return NerdProperties
	 */
	protected static synchronized NerdProperties getNewInstance() {
		LOGGER.debug("synchronized getNewInstance");
		NerdProperties = new NerdProperties();
		return NerdProperties;
	}

	/**
	 * Returns all Nerd-properties.
	 * 
	 * @return properties object
	 */
	public static Properties getProps() {
		return props;
	}

	/**
	 * @param pProps
	 *            the props to set
	 */
	protected static void setProps(final Properties pProps) {
		props = pProps;
	}

	/**
	 * Return the context.
	 * 
	 * @return the context.
	 */
	public static Context getContext() {
		return context;
	}

	/**
	 * Set the context.
	 * 
	 * @param pContext
	 *            the context.
	 */
	public static void setContext(final Context pContext) {
		context = pContext;
	}

	/**
	 * Load the path to Nerd.properties from the env-entry set in web.xml.
	 */
	public static void loadNerdPropertiesPath() {
		LOGGER.debug("loading Nerd.properties");
		if (NERD_PROPERTY_PATH == null) {
			String NerdPropertyPath = null;
			try {
				NerdPropertyPath = (String) context.lookup("java:comp/env/" + NerdPropertyKeys.PROP_NERD_PROPERTY);
			} 
			catch (Exception exp) {
				//throw new NerdPropertyException("Could not load the path to Nerd.properties from the context", exp);
				LOGGER.error("Cannot set Nerd properties path from the context.");
			}
			
			if (NerdPropertyPath == null) {
				// we try from the project directory
				NerdPropertyPath = "./src/main/resources/nerd.properties";
			}
			
			File NerdPropertyFile = new File(NerdPropertyPath);

			// check in default ClassLoader
			if (NerdPropertyFile == null || !NerdPropertyFile.exists()) {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				URL NerdProp = classLoader.getResource("nerd.properties");
				NerdPropertyFile = new File(NerdProp.getPath());

				//NerdServicePropFile = new File("./nerd_service.properties");		
			}

			// exception if prop file does not exist
			if (NerdPropertyFile == null || !NerdPropertyFile.exists()) {
				throw new NerdPropertyException("Could not read nerd.properties, the file '" + NerdPropertyPath + "' does not exist.");
			}

			try {
				NERD_PROPERTY_PATH = NerdPropertyFile.getCanonicalFile();
			} catch (IOException e) {
				throw new NerdPropertyException("Cannot set nerd property path to the given one '" + NerdPropertyPath
						+ "', because it does not exist.");
			}
		}
	}

	/**
	 * Return the nerd properties path.
	 * 
	 * @return
	 */
	public static File getNerdPropertiesPath() {
		return NERD_PROPERTY_PATH;
	}

	/**
	 * Set the nerd properties path.
	 * 
	 * @return
	 */
	public static void setNerdPropertiesPath(final String pNerdPropertiesPath) {
		if (StringUtils.isBlank(pNerdPropertiesPath))
			throw new NerdPropertyException("Cannot set property '" + pNerdPropertiesPath + "' to null or empty.");

		File NerdPropPath = new File(pNerdPropertiesPath);
		// exception if prop file does not exist
		if (NerdPropPath == null || !NerdPropPath.exists()) {
			throw new NerdPropertyException("Could not read nerd.properties, the file '" + pNerdPropertiesPath + "' does not exist.");
		}

		try {
			NERD_PROPERTY_PATH = NerdPropPath.getCanonicalFile();
		} catch (IOException e) {
			throw new NerdPropertyException("Cannot set the nerd properties path to the given one '" + pNerdPropertiesPath
					+ "', because it does not exist.");
		}
	}

	/**
	 * Return the value corresponding to the property key. If this value is
	 * null, return the default value.
	 * 
	 * @param pkey
	 *            the property key
	 * @return the value of the property.
	 */
	protected static String getPropertyValue(final String pkey) {
		return getProps().getProperty(pkey);
	}

	/**
	 * Return the value corresponding to the property key. If this value is
	 * null, return the default value.
	 * 
	 * @param pkey
	 *            the property key
	 * @param pDefaultVal
	 *            the default value
	 * @return the value of the property, pDefaultVal else.
	 */
	protected static String getPropertyValue(final String pkey, final String pDefaultVal) {
		String prop = getProps().getProperty(pkey);
		return StringUtils.isNotBlank(prop) ? prop.trim() : pDefaultVal;
	}

	/**
	 * Return the value corresponding to the property key. If this value is
	 * null, return the default value.
	 * 
	 * @param pkey
	 *            the property key
	 * @return the value of the property.
	 */
	public static void setPropertyValue(final String pkey, final String pValue) {
		if (StringUtils.isBlank(pValue))
			throw new NerdPropertyException("Cannot set property '" + pkey + "' to null or empty.");
		getProps().put(pkey, pValue);
	}

	/**
	 * First step is to check if the system property {@link #NerdPropertyKeys.PROP_NERD_HOME}
	 * is set, than the path matching to that property is used. Otherwise, the
	 * method will search a folder named {@link #FILE_NERD_PROPERTIES_PRIVATE}
	 * , if this is is also not set, the method will search for a folder named
	 * {@link #FILE_NERD_PROPERTIES} in the current project (current project
	 * means where the system property <em>user.dir</em> points to.)
	 */
	public NerdProperties() {
		init();
	}

	public NerdProperties(final Context pContext) {
		init(pContext);
	}

	protected static void init(final Context pContext) {
		setContext(pContext);

		setProps(new Properties());

		loadNerdPropertiesPath();
		setContextExecutionServer(false);

		try {
			getProps().load(new FileInputStream(getNerdPropertiesPath()));
		} catch (IOException exp) {
			throw new NerdPropertyException("Cannot open file of Nerd.properties at location'" + NERD_PROPERTY_PATH.getAbsolutePath()
					+ "'", exp);
		} catch (Exception exp) {
			throw new NerdPropertyException("Cannot open file of Nerd properties" + getNerdPropertiesPath().getAbsolutePath(), exp);
		}

		initializePaths();
		checkProperties();
	}

	/**
	 * Loads all properties given in property file {@link #NERD_HOME_PATH}.
	 */
	protected static void init() {
		LOGGER.debug("Initiating property loading");

		Context ctxt;
		try {
			ctxt = new InitialContext();
		} catch (NamingException nexp) {
			throw new NerdPropertyException("Could not get the initial context", nexp);
		}
		init(ctxt);
	}

	/**
	 * Initialize the different paths set in the configuration file
	 * Nerd.properties.
	 */
	protected static void initializePaths() {
		Enumeration<?> properties = getProps().propertyNames();
		for (String propKey; properties.hasMoreElements();) {
			propKey = (String) properties.nextElement();
			String propVal = getPropertyValue(propKey, StringUtils.EMPTY);
			if (propKey.endsWith(".path")) {
				File path = new File(propVal.toString());
				if (!path.isAbsolute()) {
					try {
						getProps().put(propKey,
								new File(path.getPath()).getCanonicalFile().toString());
					} catch (IOException e) {
						throw new NerdResourceException("Cannot read the path of '" + propKey + "'.");
					}
				}
			}
		}

		// start: creating all necessary folders
		for (String path2create : NerdPropertyKeys.PATHES_TO_CREATE) {
			String prop = getProps().getProperty(path2create);
			if (prop != null) {
				File path = new File(prop);
				if (!path.exists()) {
					LOGGER.debug("creating directory {}", path);
					if (!path.mkdirs())
						throw new NerdResourceException("Cannot create the folder '" + path.getAbsolutePath() + "'.");
				}
			}
		}
		// end: creating all necessary folders
	}

	/**
	 * Checks if the given properties contains non-empty and non-null values for
	 * the properties of list {@link NerdProperties#NOT_NULL_PROPERTIES}.
	 * 
	 */
	protected static void checkProperties() {
		LOGGER.debug("Checking Properties");
		Enumeration<?> properties = getProps().propertyNames();
		for (String propKey; properties.hasMoreElements();) {
			propKey = (String) properties.nextElement();
			String propVal = getPropertyValue(propKey, StringUtils.EMPTY);
			if (StringUtils.isBlank(propVal)) {
				throw new NerdPropertyException("The property '" + propKey + "' is null or empty. Please set this value.");
			}
		}
	}

	/**
	 * Returns the temprorary path of Nerd
	 * 
	 * @return a directory for temp files
	 */
	public static File getTempPath() {
		return new File(getPropertyValue(NerdPropertyKeys.PROP_TMP_PATH, System.getProperty("java.io.tmpdir")));
	}

	/**
	 * Returns the host for a proxy connection, given in the Nerd-property
	 * file.
	 * 
	 * @return host for connecting crossref
	 */
	public static String getProxyHost() {
		return getPropertyValue(NerdPropertyKeys.PROP_PROXY_HOST);
	}

	/**
	 * Sets the host a proxy connection, given in the Nerd-property file.
	 * 
	 * @param host
	 *            for connecting crossref
	 */
	public static void setProxyHost(final String host) {
		setPropertyValue(NerdPropertyKeys.PROP_PROXY_HOST, host);
	}

	/**
	 * Returns the port for a proxy connection, given in the Nerd-property
	 * file.
	 * 
	 * @return port for connecting crossref
	 */
	public static String getProxyPort() {
		return getPropertyValue(NerdPropertyKeys.PROP_PROXY_PORT);
	}

	/**
	 * Sets the port for a proxy connection, given in the Nerd-property file.
	 * 
	 * @param port
	 *            for connecting crossref
	 */
	public static void setProxyPort(final String port) {
		setPropertyValue(NerdPropertyKeys.PROP_PROXY_PORT, port);
	}

	/**
	 * Returns path to grobid home.
	 * 
	 * @return path to the grobid home directory.
	 */
	public static String getGrobidHome() {
		return getPropertyValue(NerdPropertyKeys.PROP_GROBID_HOME);
	}

	/**
	 * Sets the path to grobid home.
	 * 
	 * @param home
	 *            path to the grobid home directory. 
	 */
	public static void setGrobidHome(String home) {
		setPropertyValue(NerdPropertyKeys.PROP_GROBID_HOME, home);
	}
     
	/**
	 * Returns path to the grobid property file.
	 * 
	 * @return path to the grobid property file.   
	 */
	public static String getGrobidProperties() {
		return getPropertyValue(NerdPropertyKeys.PROP_GROBID_PROPERTIES);
	}

	/**
	 * Sets the path to the grobid property file. 
	 * 
	 * @param home
	 *            path to the the grobid property file. 
	 */
	public static void setGrobidProperties(String properties) {
		setPropertyValue(NerdPropertyKeys.PROP_GROBID_PROPERTIES, properties);
	}

	/**
	 * Returns if a language id shall be used, given in the Nerd-property
	 * file.
	 * 
	 * @return true if a language id shall be used
	 */
	public static Boolean isUseLanguageId() {
		return Utilities.stringToBoolean(getPropertyValue(NerdPropertyKeys.PROP_USE_LANG_ID));
	}

	public static String getLanguageDetectorFactory() {
		String factoryClassName = getPropertyValue(NerdPropertyKeys.PROP_LANG_DETECTOR_FACTORY);
		if (isUseLanguageId() && (StringUtils.isBlank(factoryClassName))) {
			throw new NerdPropertyException("Language detection is enabled but a factory class name is not provided");
		}
		return factoryClassName;
	}

	/**
	 * Sets if a language id shall be used, given in the Nerd-property file.
	 * 
	 * @param useLanguageId
	 *            true, if a language id shall be used
	 */
	public static void setUseLanguageId(final String useLanguageId) {
		setPropertyValue(NerdPropertyKeys.PROP_USE_LANG_ID, useLanguageId);
	}

	/**
	 * Returns if the execution context is stand alone or server.
	 * 
	 * @return the context of execution. Return false if the property value is
	 *         not readable.
	 */
	public static Boolean isContextExecutionServer() {
		return Utilities.stringToBoolean(getPropertyValue(NerdPropertyKeys.PROP_NERD_IS_CONTEXT_SERVER, "false"));
	}

	/**
	 * Set if the execution context is stand alone or server.
	 * 
	 * @param state
	 *            true to set the context of execution to server, false else.
	 */
	public static void setContextExecutionServer(Boolean state) {
		setPropertyValue(NerdPropertyKeys.PROP_NERD_IS_CONTEXT_SERVER, state.toString());
	}
	
	/**
	 * Returns the host of the JSON store instance to be used for caching REST calls, given in the Nerd.property
	 * file.
	 * 
	 * @return host name for REST cache
	 */
	public static String getRESTCache() {
		return getPropertyValue(NerdPropertyKeys.PROP_REST_CACHE);
	}

	/**
	 * Returns the host of the MongoDB instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return host name for MongoDB
	 */
	public static String getMongoDBHost() {
		return getPropertyValue(NerdPropertyKeys.PROP_MONGODB_HOST);
	}

	/**
	 * Returns the port of the MongoDB instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return port for MongoDB
	 */
	public static int getMongoDBPort() {
		String port = getPropertyValue(NerdPropertyKeys.PROP_MONGODB_PORT);
		int numb = 0;
		try {
			numb = Integer.parseInt(port);
		}
		catch(Exception e) {
			LOGGER.debug("port number for MongoDB instance is not a valid integer");
		}
		return numb;
	}
	
	/**
	 * Returns the host of the MySQL instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return host name for MySQL
	 */
	public static String getMySQLHost() {
		return getPropertyValue(NerdPropertyKeys.PROP_MYSQL_HOST);
	}

	/**
	 * Returns the port of the MySQL instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return port for MySQL
	 */
	public static int getMySQLPort() {
		String port = getPropertyValue(NerdPropertyKeys.PROP_MYSQL_PORT);
		int numb = 0;
		try {
			numb = Integer.parseInt(port);
		}
		catch(Exception e) {
			LOGGER.debug("port number for MySQL instance is not a valid integer");
		}
		return numb;
	}
	
	/**
	 * Returns the user name of the MySQL instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return user name for MySQL
	 */
	public static String getMySQLUsername() {
		return getPropertyValue(NerdPropertyKeys.PROP_MYSQL_USERNAME);
	}
	
	/**
	 * Returns the pass word of the MySQL instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return pass word for MySQL
	 */
	public static String getMySQLPassword() {
		return getPropertyValue(NerdPropertyKeys.PROP_MYSQL_PASSWD);
	}
	
	/**
	 * Returns the name od the database of the MySQL instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return data base name for MySQL
	 */
	public static String getMySQLDBName() {
		return getPropertyValue(NerdPropertyKeys.PROP_MYSQL_DBNAME);
	}
	
	/**
	 * Returns the host of the ElasticSearch instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return host name for ElasticSearch
	 */
	public static String getElasticSearchHost() {
		return getPropertyValue(NerdPropertyKeys.PROP_ES_HOST);
	}

	/**
	 * Returns the port of the ElasticSearch instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return port for ElasticSearch
	 */
	public static String getElasticSearchPort() {
		String port = getPropertyValue(NerdPropertyKeys.PROP_ES_PORT);
		int numb = 0;
		try {
			numb = Integer.parseInt(port);
		}
		catch(Exception e) {
			LOGGER.debug("port number for ElasticSearch instance is not a valid integer");
		}
		return port;
	}
	
	/**
	 * Returns the name of the ElasticSearch cluster to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return cluster name for ElasticSearch
	 */
	public static String getElasticSearchClusterName() {
		return getPropertyValue(NerdPropertyKeys.PROP_ES_CLUSTER);
	}
	
	/**
	 * Returns the host of the ElasticSearch instance to be used, given in the Nerd.property
	 * file.
	 * 
	 * @return host name for ElasticSearch
	 */
	public static String getElasticSearchKBName() {
		return getPropertyValue(NerdPropertyKeys.PROP_ES_KB_NAME);
	}
		
	/**
	 * Returns the name of the ElasticSearch index to be used, given in the Nerd.property
	 * file for the lexicon index.
	 * 
	 * @return host name for lexicon ElasticSearch index
	 */ 
	public static String getElasticSearchLexiconName() {
		return getPropertyValue(NerdPropertyKeys.PROP_ES_LEXICON_NAME);
	}
	
	/**
	 * Returns the path to the Wikipedia Miner configuration file.
	 * 
	 * @return host name for lexicon ElasticSearch index
	 */ 
	public static String getWikipediaMinerConfigPath() {
		return getPropertyValue(NerdPropertyKeys.PROP_WIKIPEDIA_MINER_CONFIG);
	}
	
	/**
	 * Returns the access key to Idilia wsd service, given in the Nerd.property
	 * file.
	 * 
	 * @return Idilia access key
	 */
	/*public static String getIdiliaAccessKey() {
		return getPropertyValue(NerdPropertyKeys.PROP_IDILIA_ACCESS_KEY);
	}*/

	/**
	 * Returns the private key to Idilia wsd service, given in the Nerd.property
	 * file.
	 * 
	 * @return Idilia private key
	 */
	/*public static String getIdiliaPrivateKey() {
		return getPropertyValue(NerdPropertyKeys.PROP_IDILIA_PRIVATE_KEY);
	}*/
	
	/**
	 * Returns the mapdb path
	 * 
	 * @return a directory for mapdb files
	 */
	public static String getMapDBPath() {
		return getPropertyValue(NerdPropertyKeys.PROP_MAPDB_PATH);
	}

	/**
	 * Returns the test path
	 * 
	 * @return a directory for test files
	 */
	public static String getTestPath() {
		return getPropertyValue(NerdPropertyKeys.PROP_TEST_PATH);
	}
	
	/**
	 * Update the input file with the key and value given as argument.
	 * 
	 * @param pPropertyFile
	 *            file to update.
	 * 
	 * @param pKey
	 *            key to replace
	 * @param pValue
	 *            value to replace
	 * @throws IOException
	 */
	public static void updatePropertyFile(File pPropertyFile, String pKey, String pValue) throws IOException {
		File propFile = pPropertyFile;
		BufferedReader reader = new BufferedReader(new FileReader(propFile));
		String line = StringUtils.EMPTY, content = StringUtils.EMPTY, lineToReplace = StringUtils.EMPTY;
		while ((line = reader.readLine()) != null) {
			if (line.contains(pKey)) {
				lineToReplace = line;
			}
			content += line + "\r\n";
		}
		reader.close();

		if (!StringUtils.EMPTY.equals(lineToReplace)) {
			String newContent = content.replaceAll(lineToReplace, pKey + "=" + pValue);
			FileWriter writer = new FileWriter(pPropertyFile.getAbsoluteFile());
			writer.write(newContent);
			writer.close();
		}
	}

	/**
	 * Update Nerd.properties with the key and value given as argument.
	 * 
	 * @param pKey
	 *            key to replace
	 * @param pValue
	 *            value to replace
	 * @throws IOException
	 */
	public static void updatePropertyFile(String pKey, String pValue) throws IOException {
		updatePropertyFile(getNerdPropertiesPath(), pKey, pValue);
	}

}