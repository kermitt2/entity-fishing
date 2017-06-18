package com.scienceminer.nerd.kb;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.Page.PageType;

/**
 * An language-independent atomic element of the (N)ERD Knowledge Base.
 * 
 */
public class Concept {
	protected static final Logger LOGGER = LoggerFactory.getLogger(Concept.class);

	private KBUpperEnvironment env = null;
	private String wikidataId = null;

	public Concept(KBUpperEnvironment env, String wikidataId) {
		this.env = env;
		this.wikidataId = wikidataId;
	}

	public String getId() {
		return wikidataId;
	}

	public Integer getPageIdByLang(String lang) {
		Map<String,Integer> localMap = env.getDbConcepts().retrieve(wikidataId);
		return localMap.get(lang); 
	}

	/**
	 * Return the list of statements associated to the concept
	 */
	public List<Statement> getStatements() {
		return env.getDbStatements().retrieve(wikidataId);
	}
}