package com.scienceminer.nerd.utilities;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.lang.Language;
import com.scienceminer.nerd.lang.LanguageDetectorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * Class for using language guessers.
 * 
 * @author Patrice Lopez
 */
public class LanguageUtilities {
	public static final Logger LOGGER = LoggerFactory.getLogger(LanguageUtilities.class);

	private static LanguageUtilities instance = null;

	private boolean useLanguageId = false;
	private LanguageDetectorFactory ldf = null;

	/**
	 * The list of accepted languages as input query
	 */
	//private List<String> acceptedLanguages = null;

	public static/* synchronized */LanguageUtilities getInstance() {
		if (instance == null) {
			getNewInstance();
		}
		return instance;
	}

	/**
	 * Return a new instance.
	 */
	protected static synchronized void getNewInstance() {
		LOGGER.debug("synchronized getNewInstance");
		instance = new LanguageUtilities();
	}

	/**
	 * Hidden constructor
	 */
	private LanguageUtilities() {
		//useLanguageId = GrobidProperties.isUseLanguageId();
		//if (useLanguageId) {
			String className = NerdProperties.getLanguageDetectorFactory();
			try {
				ldf = (LanguageDetectorFactory) Class.forName(className)
						.newInstance();
			} catch (ClassCastException e) {
				throw new NerdException("Class " + className
						+ " must implement "
						+ LanguageDetectorFactory.class.getName());
			} catch (ClassNotFoundException e) {
				throw new NerdException("Class "
								+ className
								+ " were not found in the classpath. "
								+ "Make sure that it is provided correctly is in the classpath");
			} catch (InstantiationException e) {
				throw new NerdException("Class " + className
						+ " should have a default constructor");
			} catch (IllegalAccessException e) {
				throw new NerdException(e);
			}
		//}
		
		/*acceptedLanguages = new ArrayList<String>();
		acceptedLanguages.add(Language.EN);
		acceptedLanguages.add(Language.FR);
		acceptedLanguages.add(Language.DE);*/
	}

	/**
	 * Basic run for language identification, return the language code and
	 * confidence score separated by a semicolon
	 * 
	 * @param text
	 *            text to classify
	 * @return language ids concatenated with ;
	 */
	public Language runLanguageId(String text) {
		/*if (!useLanguageId) {
			return null;
		}*/
		return ldf.getInstance().detect(text);
	}

	/**
	 * Run for language identification where the possible languages are given in a restricted list, 
	 * return the language code and confidence score separated by a semicolon
	 * 
	 * @param text
	 *            text to classify
	 * @return language ids concatenated with ;
	 */
	public Language runLanguageIdRestricted(String text) {
		/*if (!useLanguageId) {
			return null;
		}*/
		return ldf.getInstance().detect(text);
	}

}