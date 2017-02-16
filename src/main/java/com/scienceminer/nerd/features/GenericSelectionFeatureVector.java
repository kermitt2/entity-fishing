package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;
import java.text.*;
import java.util.regex.*;

public class GenericSelectionFeatureVector {

	public String title = "Generic selector";

	public String string = null; // lexical feature, here the FreeBase ID of the entity candidate
	public double label = 0.0; // numerical label if known
	public String classes = "N"; // class label if known
	
	// mask of features
	public boolean Add_nerd_score = false;  // NERd score produced by the disambiguator
	public boolean Add_prob_anchor_string = false; // probability of the string to be an anchor in Wikipedia
	public boolean Add_prob_c = false;

	// decision types
	public boolean target_numeric = false;
	public boolean target_class = true;
	
	// features		
	public double nerd_score = 0.0;
	public double prob_anchor_string = 0.0;
	public double prob_c = 0.0;

	/**
	 *  Write header of ARFF files.
	 */
	public String getArffHeader() throws IOException {
		StringBuilder header = new StringBuilder();
		header.append("% 1. Title: " + title + " \n");
		header.append("%\n"); 
		header.append("% 2. Sources: \n"); 
		header.append("% (a) Creator: (N)ERD \n"); 

		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		Date date = new Date();
		header.append("% (c) Date: " + dateFormat.format(date) + " \n"); 

		header.append("%\n");
		header.append("@RELATION @NERD_"+ title.replace(" ","_") +" \n");
		
		if (Add_nerd_score)
			header.append("@attribute nerd_score REAL\n");
		if (Add_prob_anchor_string)		
			header.append("@attribute prob_anchor_string REAL\n");
		if (Add_prob_c)		
			header.append("@attribute prob_c REAL\n");

		if (target_numeric)
			header.append("@attribute entity? REAL\n\n"); // target variable for regression
		else 
			header.append("@attribute entity? {Y, N}\n\n"); // target variable for binary classification
		header.append("@data\n");
		return header.toString();
	}

	public int getNumFeatures() {
		int num = 0;
		if (Add_nerd_score)
			num++;
		if (Add_prob_anchor_string)
			num++;
		if (Add_prob_c)
			num++;
		// class
		num++;	
		return num;
	}

	public String printVector() {
		/*if (string == null) return null;
		if (string.length() == 0) return null;*/
		boolean first = true;
		
		StringBuffer res = new StringBuffer();
		
		// token string (1)
		//res.append(string);

		// nerd score produced by the disambiguator model
		if (Add_nerd_score) {
			res.append(nerd_score);
			first = false;	
		}
		
		// probability of the string to be an anchor 
		if (Add_prob_anchor_string) {
			if (first) {
				res.append(prob_anchor_string);	
				first = false;	
			}
			else 
				res.append("," + prob_anchor_string);	
		}
		
		// conditional probability of the concept given the string
		if (Add_prob_c) {
			if (first) {
				res.append(prob_c);
				first = false;	
			} else {
				res.append(", " + prob_c);
			}
		}

		// target variable - for training data (regression: 1.0 or 0.0 for training data)
		if (target_numeric)
			res.append("," + label);
		else
			res.append("," + classes);
			
		return res.toString();	
	}

	public double[] toVector() {
		double[] result = new double[this.getNumFeatures()];
		int i = 0;
		if (Add_nerd_score) {
			result[i] = nerd_score;
			i++;
		}
		if (Add_prob_anchor_string) {
			result[i] = prob_anchor_string;
			i++;
		}
		if (Add_prob_c) {
			result[i] = prob_c;
			i++;
		}
		return result;
	}
}