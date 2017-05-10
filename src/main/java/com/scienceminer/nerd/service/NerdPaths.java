package com.scienceminer.nerd.service;

/**
 * This interface only contains the path extensions for accessing the nerd service.
 *
 * @author Patrice Lopez
 *
 */
public interface NerdPaths {
	/**
	 * path extension for Nerd service
	 */
	public static final String PATH_NERD = "/";
	
	/**
	 * path extension for is alive request
	 */
	public static final String PATH_IS_ALIVE = "isalive";
	
	/**
	 * path extension for Nerd admin pages
	 */
	public static final String PATH_ADMIN = "admin";
	
	/**
	 * path extension for processing sha1
	 */
	public static final String PATH_SHA1 = "sha1";
	
	/**
	 * path extension for getting all properties
	 */
	public static final String PATH_ALL_PROPS = "allProperties";
	
	/**
	 * path extension to update property value
	 */
	public static final String PATH_CHANGE_PROPERTY_VALUE = "changePropertyValue";
	
	/**
	 * path extension for applying a language identification to a text
	 */
	public static final String PATH_LID= "processLIdText";
	
	/**
	 * path extension for applying a sentence segmentation to a text
	 */
	public static final String PATH_SENTENCE_SEGMENTATION= "processSentenceSegmentation";
	
	/**
	 * path extension for extracting named entities from a text
	 */
	//public static final String PATH_NER_TEXT= "processNERDText";
	
	/**
	 * path extension for extracting named entities from a text query.
	 */
	public static final String PATH_NERD_QUERY= "processNERDQuery";
	
	/**
	 * path extension for extracting named entities from a text
	 */
	//public static final String PATH_ERD_TEXT = "processERDText";
	
	/**
	 * path extension for extracting named entities from a text query
	 */
	//public static final String PATH_ERD_QUERY = "processERDQuery";
	
	/**
	 * path extension for extracting named entities from a text query
	 */
	//public static final String PATH_ERD_QUERY_TERMS = "processERDQueryTerms";
	
	/**
	 * path extension for extracting named entities from a text query
	 */
	//public static final String PATH_ERD_TERMS = "processERDTerms";
	
	/**
	 * path extension for extracting named entities from a text query
	 */
	//public static final String PATH_ERD_SEARCH_QUERY = "processERDSearchQuery";
	
	/**
	 * path extension for getting the list of existing customisations
	 */
	public static final String PATH_NER_CUSTOMISATIONS = "NERDCustomisations";
	
	/**
	 * path extension for getting the data of an existing customisation
	 */
	public static final String PATH_NER_CUSTOMISATION = "NERDCustomisation";
	
	/**
	 * path extension for adding a new customisation
	 */
	public static final String PATH_NER_CREATE_CUSTOMISATION = "createNERDCustomisation";
	
	/**
	 * path extension for adding more reference infomation to an existing customisation
	 */
	public static final String PATH_NER_EXTEND_CUSTOMISATION = "extendNERDCustomisation";
	
	/**
	 * path extension for deleting an existing customisation
	 */
	public static final String PATH_NER_DELETE_CUSTOMISATION = "NERDCustomisation";

	/**
	 * path extension for getting information about a concept
	 */
	public static final String PATH_KB_CONCEPT = "KBConcept";

	/**
	 * path extension for getting all ambiguous concepts associated to a single term (test purpose)
	 */
	public static final String PATH_KB_TERM_LOOKUP = "KBTermLookup";
}