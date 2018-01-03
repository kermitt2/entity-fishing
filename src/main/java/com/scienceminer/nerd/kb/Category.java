package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.exceptions.NerdException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.io.*;

/**
 * Representation of a category (here an uncontrolled typing, as in Wikipedia) for a concept or similar 
 * entity associated to a given source.
 * 
 * 
 */
public class Category { 
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(Category.class);
	
	private String name = null;
	private com.scienceminer.nerd.kb.model.Category wikiCategory = null;
	private int wikiPageID = -1;
	
	// an optional weight to be associated to the category
	private double weight = 0.0;
	
	public Category() {}

	public Category(String name) {
		this.name= name;
	}

    public Category(String name, com.scienceminer.nerd.kb.model.Category wikiCategory, int wikiPageID) {
		this.wikiCategory = wikiCategory;
		this.name = name;
		this.wikiPageID = wikiPageID;
    }
	
	public Category(com.scienceminer.nerd.kb.model.Category wikiCategory) {
		this.wikiCategory = wikiCategory;
		name = wikiCategory.getTitle();
		wikiPageID = wikiCategory.getId();
    }

	public String toString() {
		return wikiCategory.toString();
	}
	
	public String getName() {
		return name;
	}
	
	public int getWikiPageID() {
		return wikiPageID;
	}
	
	public com.scienceminer.nerd.kb.model.Category getWikiCategory() {
		return wikiCategory;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
}