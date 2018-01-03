package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;

import org.grobid.core.data.Entity;
import org.grobid.core.data.Sense;
import com.scienceminer.nerd.kb.*;

import java.util.List;    
import java.util.ArrayList;

import com.fasterxml.jackson.core.io.*;

/**
 * This class represents a weighted term, i.e. a phrase associated to a probabilistic score and the
 * list of disambiguated entities corresponding to the term. 
 * 
 *
 */
public class WeightedTerm {
	
	private String term = null;
	private double score = 0.0;
	private List<NerdEntity> nerdEntities = null;
	
	public String getTerm() {
		return term;
	}
	
	public void setTerm(String term0) {
		term = term0;
	}
	
	public double getScore() {
		return score;
	}
	
	public void setScore(double score0) {
		score = score0;
	}
    
	public List<NerdEntity> getNerdEntities() {
		return nerdEntities;
	}
	
	public void setNerdEntities(List<NerdEntity> entities) {
		nerdEntities = entities;
	}
	
	public String toJson() {
		JsonStringEncoder encoder = JsonStringEncoder.getInstance();
		byte[] encodedTerm = encoder.quoteAsUTF8(term);
		String outputTerm = new String(encodedTerm); 
		StringBuilder buffer = new StringBuilder(); 
		buffer.append("{ \"term\" : \"" + outputTerm + "\", \"score\" : " + score);
		boolean begin = true;
		if ( (nerdEntities != null) && (nerdEntities.size() > 0) ) {
			buffer.append(", \"entities\":[");
			for(NerdEntity entity : nerdEntities) {
				if (!begin) 
					buffer.append(", ");
				else
					begin = false;
				buffer.append(entity.toJsonFull());
			}
			buffer.append("]");
		}
		buffer.append("}");
		 
		return buffer.toString();
	}
	
}