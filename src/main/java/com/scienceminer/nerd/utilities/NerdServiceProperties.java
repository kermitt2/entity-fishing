package com.scienceminer.nerd.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.net.URL;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;

import com.scienceminer.nerd.exceptions.NerdPropertyException;
import com.scienceminer.nerd.exceptions.NerdServicePropertyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the properties which can be set for the Nerd-service. 
 * It is directly extended by the {@link NerdProperties} class and
 * therefore also contains all necessary properties. 
 * 
 * 
 */
public class NerdServiceProperties {

	/**
	 * The Logger.
	 */
	public static final Logger LOGGER = LoggerFactory
			.getLogger(NerdServiceProperties.class);

	/**
	 * Internal property object, where all properties are defined.
	 */
	protected static Properties props = null;

	/**
	 * Path to Nerd_service.property.
	 */
	protected static File NERD_SERVICE_PROPERTY_PATH = null;

	/**
	 * The context of the application.
	 */
	protected static Context context;

	/**
	 * A static {@link NerdProperties} object containing all properties used
	 * by Nerd.
	 */
	private static NerdServiceProperties NerdServiceProperties = null;

	/**
	 * Returns a static {@link NerdServiceProperties} object. If no one is
	 * set, than it creates one. {@inheritDoc #NerdServiceProperties()}
	 * 
	 * @return
	 */
	public static NerdServiceProperties getInstance() {
		if (NerdServiceProperties == null)
			return getNewInstance();
		else
			return NerdServiceProperties;
	}
	
	/**
	 * Reload NerdServiceProperties.
	 */
	public static void reload() {
		getNewInstance();
	}

	/**
	 * Creates a new {@link NerdServiceProperties} object, initializes it and
	 * returns it. {@inheritDoc #NerdServiceProperties()} First checks to find
	 * the Nerd home folder by resolving the given context.
	 * 
	 */
	protected static synchronized NerdServiceProperties getNewInstance() {
		LOGGER.debug("Start NerdServiceProperties.getNewInstance");
		try {
			NerdServiceProperties = new NerdServiceProperties();
		} catch (NamingException nexp) {
			throw new NerdPropertyException(
					"Could not get the initial context", nexp);
		}
		return NerdServiceProperties;
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
	 * @param pProps the props to set
	 */
	protected static void setProps(Properties pProps) {
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
	public static void setContext(Context pContext) {
		context = pContext;
	}

	/**
	 * Loads all properties given in property file {@link #NERD_HOME_PATH}.
	 */
	protected static void init() {
		LOGGER.debug("Initiating property loading");
		try {
			setContext(new InitialContext());
		} catch (NamingException nexp) {
			throw new NerdPropertyException(
					"Could not get the initial context", nexp);
		}
	}

	/**
	 * Initializes a {@link NerdServiceProperties} object by reading the
	 * property file.
	 * 
	 * @throws NamingException
	 * 
	 */
	public NerdServiceProperties() throws NamingException {
		LOGGER.debug("Instanciating NerdServiceProperties");
		init();
		setProps(new Properties());
		String nerdServicePath = null;
		try {
			nerdServicePath = (String) context.lookup("java:comp/env/"
					+ NerdPropertyKeys.PROP_NERD_SERVICE_PROPERTY);
		} catch (Exception exp) {
			//throw new NerdServicePropertyException(
			//		"Could not load the path to nerd_service.properties from the context",
			//		exp);
		}
		if (nerdServicePath == null) {
			// default path
			nerdServicePath = "src/main/resources/nerd_services.properties";
		}
		File nerdServicePropFile = new File(nerdServicePath);

		// check in default ClassLoader
		if (nerdServicePropFile == null || !nerdServicePropFile.exists()) {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			URL nerdServiceProp = classLoader.getResource("nerd_service.properties");
			nerdServicePropFile = new File(nerdServiceProp.getPath());
			
			//NerdServicePropFile = new File("./Nerd_service.properties");		
		}
		
		// exception if prop file does not exist
		if (nerdServicePropFile == null || !nerdServicePropFile.exists()) {
			throw new NerdServicePropertyException(
					"Could not read nerd_service.properties, the file '"
							+ nerdServicePropFile + "' does not exist.");
		}

		// load server properties and copy them to this properties
		try {
			NERD_SERVICE_PROPERTY_PATH = nerdServicePropFile
					.getCanonicalFile();
			Properties serviceProps = new Properties();
			serviceProps.load(new FileInputStream(nerdServicePropFile));
			getProps().putAll(serviceProps);
		} catch (FileNotFoundException e) {
			throw new NerdServicePropertyException(
					"Cannot load properties from file " + nerdServicePropFile
							+ "''.");
		} catch (IOException e) {
			throw new NerdServicePropertyException(
					"Cannot load properties from file " + nerdServicePropFile
							+ "''.");
		}

		// prevent NullPointerException if NerdProperties is not yet
		// instantiated
		//if (NerdProperties.getNerdHomePath() == null) 
		{
			NerdProperties.getInstance();
		}
		//NerdProperty.setContextExecutionServer(true);
	}

	public static File getNerdPropertiesPath() {
		return NERD_SERVICE_PROPERTY_PATH;
	}

	/**
	 * Return the value corresponding to the property key. If this value is
	 * null, return the default value.
	 * 
	 * @param pkey
	 *            the property key
	 * @return the value of the property.
	 */
	protected static String getPropertyValue(String pkey) {
		return getProps().getProperty(pkey);
	}

	/**
	 * Return the value corresponding to the property key. If this value is
	 * null, return the default value.
	 * 
	 * @param pkey
	 *            the property key
	 * @return the value of the property.
	 */
	public static void setPropertyValue(String pkey, String pValue) {
		if (StringUtils.isBlank(pValue))
			throw new NerdPropertyException("Cannot set property '" + pkey
					+ "' to null or empty.");
		getProps().put(pkey, pValue);
	}

	/**
	 * Returns the password for admin page given by property
	 * {@value #PROP_NERD_SERVICE_ADMIN_PW}.
	 * 
	 * @return password for admin page
	 */
	public static String getAdminPw() {
		return getPropertyValue(NerdPropertyKeys.PROP_NERD_SERVICE_ADMIN_PW);
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
		NerdProperties.updatePropertyFile(getNerdPropertiesPath(), pKey,
				pValue);
	}

}
