package com.scienceminer.nerd.kb;

import java.io.*;
import java.util.*;

/**
 *  Class for representing and exchanging a terminological variant.
 */
public class Variant {
	private String term = null;
	private String source = null;
	private String language = null;
	
    public Variant() {}
    
    public void setTerm(String term) { 
    	this.term = term; 
    }
    
    public void setSource(String s) { 
    	source = s; 
    }
    
    public void setLanguage(String lang) { 
    	this.language = lang; 
    }
	
	public String getTerm() { 
		return term; 
	}
	
	public String getSource() { 
		return source; 
	}
	
	public String getLanguage() { 
		return language; 
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
        if (term != null) {
			buffer.append(term);
		}
		if (source != null) {
			buffer.append(" (" + source + ") ");
		}
		if (language != null) {
			buffer.append(" (" + language + ") ");
		}
        return buffer.toString();
	}
}