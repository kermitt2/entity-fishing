package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.exceptions.NerdException;

import org.grobid.core.utilities.OffsetPosition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

/**
 * Representation of a description (e.g. definition) for a concept or similar entity 
 * associated to a given source.
 * 
 * 
 */
public class Definition {
	
	// definition string                
	private String definition = null;
	
	// domain information
	private String source = null;
	
	// language information
	private String lang = null;
	
    public Definition() {
    }
	
	public String getDefinition() {
		return definition;
	}
	
	public String getSource() {
		return source;
	}
	
	public String getLang() {
		return lang;
	}
	
	public void setDefinition(String definition) {
		if (definition == null)
			return;
		definition = definition.replace("__NOTOC__", " ");
		definition = definition.replaceAll(" \\([\\s,;:or]*\\)", "");
		this.definition = definition.trim();
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	public void setLang(String lang) {
		this.lang = lang;
	}

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (definition != null) {
			buffer.append(definition);
		}
		if (source != null) {
			buffer.append(" (" + source + ") ");
		}
		if (lang != null) {
			buffer.append(" (" + lang + ") ");
		}
        return buffer.toString();
    }
}