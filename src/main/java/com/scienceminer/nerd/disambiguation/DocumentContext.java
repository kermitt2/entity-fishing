package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.grobid.core.data.Entity;

import java.util.*; 
import java.text.*;
import java.text.*;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.model.*;

import com.fasterxml.jackson.core.io.*;

/**
 * This class represents a context in relation to a document. 
 * 
 * @author Patrice Lopez
 *
 */
public class DocumentContext extends NerdContext {
		
	public DocumentContext() {
		super();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(Article article : contextArticles) {
			builder.append(article.getTitle() + "\t" + article.getId() + "\t" + article.getWeight() + "\n");
		}
		return builder.toString();
	}
}