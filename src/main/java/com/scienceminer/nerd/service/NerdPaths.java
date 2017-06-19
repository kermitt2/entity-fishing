package com.scienceminer.nerd.service;

/**
 * This interface only contains the path extensions for accessing the nerd service.
 *
 * @author Patrice Lopez
 *
 */
public interface NerdPaths {
	/**
	 * root path
	 */
	String ROOT = "/";
	
	/**
	 * path extension for is alive request
	 */
	String IS_ALIVE = "isalive";
	
	/**
	 * Language identification entry point
	 */
	String LANGUAGE = "language";

	/**
	 * Sentence segmentation entry point
	 */
	String SEGMENTATION= "segmentation";

	/**
	 * admin entry point
	 */
	String ADMIN = "admin";

	/**
	 * admin properties
	 */
	String ADMIN_PROPERTIES = ADMIN + "/properties";

	/**
	 * NERD disambiguation (query, text, shortText, PDF) entry point
	 */
	String DISAMBIGUATE= "disambiguate";

	/**
	 * Customisation entry points:
	 *  - GET /customisations
	 *  - POST /customisation/id
	 */
	String CUSTOMISATIONS = "customisations";
	String CUSTOMISATION = "customisation";

	/**
	 * Concept management and retrieval entry point
	 */
	String CONCEPT = "concept";
	String KB = "kb";


	/**
	 * Term lookup
	 */
	String LOOKUP = "lookup";
	String TERM = "term";

}