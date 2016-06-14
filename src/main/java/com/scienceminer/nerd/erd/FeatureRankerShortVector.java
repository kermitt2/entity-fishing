package com.scienceminer.nerd.erd;

import java.io.*;

import java.sql.*;
import javax.naming.*;
import javax.sql.*;

import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import java.util.regex.*;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class FeatureRankerShortVector {
	public String string = null; // lexical feature, here the FreeBase ID of the entity candidate
	public double label = 0.0; // numerical label if known
	public String classes = "N"; // class label if known
	
	// mask of features
	public boolean Add_prob_c = true;  // conditional probability of the concept given the string
	public boolean Add_prob_i = false; // conditional probability of the string given the concept 
												 // (i.e. reverse prob_c)
	public boolean Add_frequencyStrict = false;  // frequency of usage of the string to refer to the 
													 // concept in a reference corpus (usually wikipedia)
	public boolean Add_frequencyConcept = false; // frequency of usage of the concept
	public boolean Add_termLength = false;
	public boolean Add_inDictionary = false; // true if the words of the term are all in the dictionary
													// of common words
	public boolean Add_relatedness = true;	// semantic relatedness measure bewteen candidate and context											
													
	public boolean Add_ner_st = false; // boolean indicating if identified as NER mention by Stanford NER
	public boolean Add_ner_id = false; // boolean indicating if identified as NER mention by Idilia service 
	public boolean Add_ner_type = false; // wordnet ner type
	public boolean Add_ner_subtype = false; // wordnet ner subtype
	
	public boolean Add_isSubTerm = false; // true if sub-string of another term candidate
	
	public boolean Add_NERType_relatedness = false; // relateness between the concept and the estimated NER type
	public boolean Add_NERSubType_relatedness = false; // relateness between the concept and the estimated NER subtype
	
	public boolean Add_occ_term = false; // term frequency 
		
	// relateness with the context:
	// - with context terms
	// - with all other candidates (average)
	// - between NER subtype and context
	// - between NER subtype and context WSD
	// quality of the context:
	// - minimum depth of concept in wikipedia category tree
	// - general term frequency
	// - tf/idf
	// - lexical cohesion (e.g. dice coefficient)
	// quality of the NE property:
	// - mention marked by NER
	
	// decision types
	public boolean target_numeric = false;
	public boolean target_class = true;
	
	// features		
	public double prob_c = 0.0;
	public double prob_i = 0.0;
	public int frequencyStrict = 0; 
	public int frequencyConcept = 0; 
	
	public int termLength = 0; 
	public boolean inDictionary = false; 
	
	public double relatedness = 0.0;
	
	public boolean ner_st = false;
	public boolean ner_id = false;
	public String ner_type = "NotNER";
	public String ner_subtype = "NotNER";

	public boolean isSubTerm = false;
	
	public double NERType_relatedness = 0.0;
	public double NERSubType_relatedness = 0.0;

	public long occ_term = 0;

	/**
	 *  Write header of ARFF files.
	 */
	public void writeHeader(Writer writer) throws IOException {
		writer.write("% 1. Title: Ranker Entities Short \n");
		writer.write("%\n"); 
		writer.write("% 2. Sources: \n"); 
		writer.write("% (a) Creator: Patrice Lopez \n"); 
		writer.write("% (c) Date: May 2014 \n"); 
		writer.write("%\n");
		writer.write("@RELATION erd2014 \n");
		
		if (Add_prob_c)
			writer.write("@attribute prob_c REAL\n");
		if (Add_prob_i)		
			writer.write("@attribute prob_i REAL\n");
		if (Add_frequencyStrict)	
			writer.write("@attribute frequencyStrict NUMERIC\n");
		if (Add_frequencyConcept)	
			writer.write("@attribute frequencyConcept NUMERIC\n");	
		if (Add_termLength)	
			writer.write("@attribute termLength NUMERIC\n"); 
		if (Add_inDictionary)	
			writer.write("@attribute inDictionary {false, true}\n");
		if (Add_relatedness)	
			writer.write("@attribute relatedness REAL\n");
		if (Add_isSubTerm)	
			writer.write("@attribute isSubTerm {false, true}\n");	
		if (Add_ner_st) 
			writer.write("@attribute ner_st {false, true}\n");
		if (Add_ner_id)
			writer.write("@attribute ner_id {false, true}\n");
		if (Add_ner_type)
			writer.write("@attribute ner_type {NotNER, PERSON, LOCATION, ORGANIZATION}\n");
		if (Add_ner_subtype)	
			writer.write("@attribute ner_subtype string\n");
		if (Add_NERType_relatedness)
			writer.write("@attribute NERType_relatedness REAL\n");
		if (Add_NERSubType_relatedness)
			writer.write("@attribute NERSubType_relatedness REAL\n");
		if (Add_occ_term) 
			writer.write("@attribute occ_term NUMERIC\n");			
		
		if (target_numeric)
			writer.write("@attribute entity? REAL\n\n"); // target variable for regression
		else 
			writer.write("@attribute entity? {Y, N}\n\n"); // target variable for binary classification
		writer.write("@data\n");
		writer.flush();
	}
	
	/**
	 *  Set the instance profile in the weka data instance format
	 */
	public FastVector setHeaderInstance() {
		FastVector atts = new FastVector();
		if (Add_prob_c)
			atts.addElement(new Attribute("prob_c"));
		if (Add_prob_i)
			atts.addElement(new Attribute("prob_i"));
		if (Add_frequencyStrict)	
			atts.addElement(new Attribute("frequencyStrict"));
		if (Add_frequencyConcept)	
			atts.addElement(new Attribute("frequencyConcept"));
		if (Add_termLength)	
			atts.addElement(new Attribute("termLength"));
		if (Add_inDictionary) {
			FastVector vals = new FastVector(2);
			vals.addElement("true");
			vals.addElement("false");
			atts.addElement(new Attribute("inDictionary", vals));
		}
		if (Add_relatedness)	
			atts.addElement(new Attribute("relatedness"));
		if (Add_isSubTerm) {
			FastVector vals = new FastVector(2);
			vals.addElement("true");
			vals.addElement("false");
			atts.addElement(new Attribute("isSubTerm", vals));
		}
		if (Add_ner_st) {
			FastVector vals = new FastVector(2);
			vals.addElement("true");
			vals.addElement("false");
			atts.addElement(new Attribute("ner_st", vals));
		}
		if (Add_ner_id) {
			FastVector vals = new FastVector(2);
			vals.addElement("true");
			vals.addElement("false");
			atts.addElement(new Attribute("ner_id", vals));
		}
		if (Add_ner_type) {
			FastVector vals = new FastVector(2);
			vals.addElement("NotNER");
			vals.addElement("PERSON");
			vals.addElement("LOCATION");
			vals.addElement("ORGANIZATION");
			atts.addElement(new Attribute("ner_type", vals));
		}
		if (Add_ner_subtype)	
			atts.addElement(new Attribute("ner_subtype"));					
		if (Add_NERType_relatedness)
			atts.addElement(new Attribute("NERType_relatedness"));		
		if (Add_NERSubType_relatedness)
			atts.addElement(new Attribute("NERSubType_relatedness"));
		if (Add_occ_term) 
			atts.addElement(new Attribute("occ_term"));

		if (target_numeric)	
			atts.addElement(new Attribute("entity?"));
		else {
			FastVector vals = new FastVector(2);
			vals.addElement("Y");
			vals.addElement("N");
			atts.addElement(new Attribute("entity?", vals));
		} 
			
		return atts;
	}
	
	public int getNumFeatures() {
		int num = 0;
		if (Add_prob_c)
			num++;
		if (Add_prob_i)
			num++;
		if (Add_frequencyStrict)	
			num++;
		if (Add_frequencyConcept)	
			num++;
		if (Add_termLength)	
			num++;
		if (Add_inDictionary)
			num++;
		if (Add_relatedness)
			num++;
		if (Add_isSubTerm)
			num++;
		if (Add_ner_st) 
			num++;
		if (Add_ner_id)
			num++;
		if (Add_ner_type)
			num++;
		if (Add_ner_subtype)	
			num++;
		if (Add_NERType_relatedness)
			num++;
		if (Add_NERSubType_relatedness)	
			num++;
		if (Add_occ_term) 
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

		// conditional probability of the concept given the string
		if (Add_prob_c) {
			res.append(prob_c);
			first = false;	
		}
		
		// conditional probability of the string given the concept (i.e. reverse prob_c)
		if (Add_prob_i) {
			if (first) {
				res.append(prob_i);	
				first = false;	
			}
			else 
				res.append("," + prob_i);	
		}
			
		// frequency of usage of the string to refer to the concept in a reference corpus (usually wikipedia)
		if (Add_frequencyStrict) {
			if (first) {
				res.append(frequencyStrict);	
				first = false;
			}
			else	
				res.append("," + frequencyStrict);
		}
			
		// frequency of usage of the concept
		if (Add_frequencyConcept) {
			if (first) {
				res.append(frequencyConcept);
				first = false;
			}
			else		
				res.append("," + frequencyConcept);
		}
		
		// term length
		if (Add_termLength) {
			if (first) {
				res.append(termLength);
				first = false;
			}
			else
				res.append("," + termLength);
		}

		if (Add_inDictionary) {
			if (inDictionary) 
				res.append(",true");
			else 
				res.append(",false");
		}
		
		if (Add_relatedness) {
			res.append(","+relatedness);
		}
		
		if (Add_isSubTerm) {
			if (isSubTerm) 
				res.append(",true");
			else 
				res.append(",false");
		}
		if (Add_ner_st) {
			if (ner_st) 
				res.append(",true");
			else 
				res.append(",false");
		}
		if (Add_ner_id) {
			if (ner_id) 
				res.append(",true");
			else 
				res.append(",false");
		}
		if (Add_ner_type) {
			res.append(","+ner_type);
		}
		if (Add_ner_subtype) {
			res.append(","+ner_subtype);
		}
		if (Add_NERType_relatedness) {
			res.append("," + NERType_relatedness);
		}
		if (Add_NERType_relatedness) {
			res.append("," + NERSubType_relatedness);
		}
		if (Add_occ_term) {
			res.append("," + occ_term);
		}
		
		// target variable - for training data (regression: 1.0 or 0.0 for training data)
		if (target_numeric)
			res.append("," + label);
		else
			res.append("," + classes);
			
		return res.toString();	
	}
	
	/**
	 *  Return the feature values for Weka data instance 
	 */
	public Instance getFeatureValues(Instances classifierData) {	
		Instance inst = new Instance(getNumFeatures());
		inst.setDataset(classifierData);
		int i = 0;
		if (Add_prob_c) {
			inst.setValue(i, prob_c);  
			i++;
		}
		if (Add_prob_i) {
			inst.setValue(i, prob_i);  
			i++;
		}
		if (Add_frequencyStrict) {
			inst.setValue(i, frequencyStrict);
			i++;
		}
		if (Add_frequencyConcept) {
			inst.setValue(i, frequencyConcept);
			i++;
		}
		if (Add_termLength) {
			inst.setValue(i, termLength);
			i++;
		}
		if (Add_inDictionary) {
			if (inDictionary)
				inst.setValue(i,"true");
			else 
				inst.setValue(i,"false");
			i++;
		}
		if (Add_relatedness) {
			inst.setValue(i, relatedness);  
			i++;
		}
		if (Add_isSubTerm) {
			if (isSubTerm)
				inst.setValue(i,"true");
			else 
				inst.setValue(i,"false");
			i++;
		}
		if (Add_ner_st) {
			if (ner_st)
				inst.setValue(i,"true");
			else 
				inst.setValue(i,"false");
			i++;
		}
		if (Add_ner_id) {
			if (ner_id)
				inst.setValue(i,"true");
			else 
				inst.setValue(i,"false");
			i++;
		}
		if (Add_ner_type) {
			inst.setValue(i, ner_type);
			i++;
		}
		if (Add_ner_subtype) {
			inst.setValue(i, ner_subtype);
			i++;
		}				
		if (Add_NERType_relatedness)  {
			inst.setValue(i, NERType_relatedness);
			i++;
		}
		if (Add_NERSubType_relatedness)  {
			inst.setValue(i, NERSubType_relatedness);
			i++;
		}		
		if (Add_occ_term) {
			inst.setValue(i, occ_term);
			i++;
		}
		
		// entity?
		if (target_numeric) {
			inst.setValue(i, 0.0);
		}
		else
			inst.setValue(i, "N");
		
		return inst;
	}

	
}