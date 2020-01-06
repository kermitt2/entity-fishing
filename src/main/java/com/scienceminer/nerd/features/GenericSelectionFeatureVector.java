package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;
import java.text.*;
import java.util.regex.*;

import smile.data.Attribute;

public class GenericSelectionFeatureVector extends GenericFeatureVector {

	public String title = "Generic selector";

	/*public String string = null; // lexical feature, here the FreeBase ID of the entity candidate
	public double label = 0.0; // numerical label if known
	public String classes = "N"; // class label if known*/
	
	// mask of features
	public boolean Add_nerd_score = false;  // NERd score produced by the disambiguator
	public boolean Add_prob_anchor_string = false; // probability of the string to be an anchor in Wikipedia
	public boolean Add_prob_c = false; // conditional probability of the concept given the surface string
	public boolean Add_dice = false; // Dice coefficient for lexical cohesion
	public boolean Add_tf_idf = false; // term frequency inverse document frequency, tf-idf
	public boolean Add_total_occ_number = false; // the total number of occurrence of the mention in language-specific Wikipedia
	public boolean Add_nb_tokens = false; // number of tokens of the mention
	public boolean Add_relatedness = false;	// semantic relatedness measure bewteen candidate and context	
	public boolean Add_inContext = false; // true if entity present in the relatedness context
	public boolean Add_isNe = false; // if the entity is recognized as a NE
	public boolean Add_embeddings_LR_similarity = false; // entity/term context LR score similarity
	public boolean Add_embeddings_centroid_similarity = false; // entity/term context centroid score similarity

	// decision types
	public boolean target_numeric = false;
	public boolean target_class = true;
	
	// features		
	public double nerd_score = 0.0;
	public double prob_anchor_string = 0.0;
	public double prob_c = 0.0;
	public double dice = 0.0; // Dice coefficient
	public double tf_idf = 0.0; // tf-idf
	public int total_occ_number = 0; // shall we use some discretized, log, or relative version instead? 
	public int nb_tokens = 0;
	public double relatedness = 0.0;
	public boolean inContext = false;
	public boolean isNe = false;
	public float embeddings_LR_similarity = 0.0F;
	public float embeddings_centroid_similarity = 0.0F;

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
		if (Add_dice)	
			header.append("@attribute dice REAL\n");
		if (Add_tf_idf)	
			header.append("@attribute tf_idf REAL\n");
		if (Add_total_occ_number)	
			header.append("@attribute total_occ_number NUMERIC\n");
		if (Add_nb_tokens)
			header.append("@attribute nb_tokens NUMERIC\n");
		if (Add_relatedness)	
			header.append("@attribute relatedness REAL\n");
		if (Add_inContext)	
			header.append("@attribute inContext {false, true}\n");
		if (Add_isNe)	
			header.append("@attribute isNe {false, true}\n");
		if (Add_embeddings_LR_similarity) 
			header.append("@attribute embeddings_LR_similarity REAL\n");
		if (Add_embeddings_centroid_similarity) 
			header.append("@attribute embeddings_centroid_similarity REAL\n");

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
		if (Add_dice)
			num++;
		if (Add_tf_idf)
			num++;
		if (Add_total_occ_number)
			num++;
		if (Add_nb_tokens)
			num++;
		if (Add_relatedness)
			num++;
		if (Add_inContext)
			num++;
		if (Add_isNe)
			num++;
		if (Add_embeddings_LR_similarity)
			num++;
		if (Add_embeddings_centroid_similarity)
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
				res.append("," + prob_c);
			}
		}

		// Dice coefficient for lexical cohesion
		if (Add_dice) {
			if (first) {
				res.append(dice);
				first = false;	
			} else {
				res.append("," + dice);
			}
		}

		// term frequency inverse document frequency, tf-idf
		if (Add_tf_idf) {
			if (first) {
				res.append(tf_idf);
				first = false;	
			} else {
				res.append("," + tf_idf);
			}
		}

		// the total number of occurrence of the mention in language-specific Wikipedia
		if (Add_total_occ_number) {
			if (first) {
				res.append(total_occ_number);
				first = false;	
			} else {
				res.append("," + total_occ_number);
			}
		}

		// the number of tokens of the mention
		if (Add_nb_tokens) {
			if (first) {
				res.append(nb_tokens);
				first = false;	
			} else {
				res.append("," + nb_tokens);
			}
		}

		if (Add_relatedness) {
			if (first) {
				res.append(relatedness);
				first = false;	
			} else {
				res.append("," + relatedness);
			}
		}

		// true if the entity is present in the relatedness context
		if (Add_inContext) {
			if (first) {
				if (inContext) 
					res.append("true");
				else 
					res.append("false");
				first = false;	
			} else {
				if (inContext) 
					res.append(",true");
				else 
					res.append(",false");
			}
		}

		// true if the entity is recognized as a NE
		if (Add_isNe) {
			if (first) {
				if (isNe) 
					res.append("true");
				else 
					res.append("false");
				first = false;	
			} else {
				if (isNe) 
					res.append(",true");
				else 
					res.append(",false");
			}
		}
		if (Add_embeddings_LR_similarity) {
			res.append(","+embeddings_LR_similarity);
		}
		if (Add_embeddings_centroid_similarity) {
			res.append(","+embeddings_centroid_similarity);
		}

		// target variable - for training data (regression: 1.0 or 0.0 for training data)
		if (target_numeric)
			res.append("," + label);
		else
			res.append("," + classes);
			
		return res.toString();	
	}

	public double[] toVector(Attribute[] attributes) {
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
		if (Add_dice) {
			result[i] = dice;
			i++;
		}
		if (Add_tf_idf) {
			result[i] = tf_idf;
			i++;
		}
		if (Add_total_occ_number) {
			result[i] = total_occ_number;
			i++;
		}
		if (Add_nb_tokens) {
			result[i] = (double)nb_tokens;
			i++;
		}
		if (Add_relatedness) {
			result[i] = relatedness;
			i++;
		}
		if (Add_inContext) {
			Attribute att = attributes[i];
			double trueVal = 1.0;
			double falseVal = 0.0;
			try {
				trueVal = att.valueOf("true");
				falseVal = att.valueOf("false");
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (inContext)
				result[i] = trueVal;
			else
				result[i] = falseVal;

			/*if (inContext)
				result[i] = 1.0;
			else
				result[i] = 0.0;*/
			i++;
		}
		if (Add_isNe) {
			Attribute att = attributes[i];
			double trueVal = 1.0;
			double falseVal = 0.0;
			try {
				trueVal = att.valueOf("true");
				falseVal = att.valueOf("false");
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (isNe)
				result[i] = trueVal;
			else
				result[i] = falseVal;

			/*if (isNe)
				result[i] = 1.0;
			else
				result[i] = 0.0;*/
			i++;
		}
		if (Add_embeddings_LR_similarity) {
			result[i] = embeddings_LR_similarity;
			i++;
		}
		if (Add_embeddings_centroid_similarity) {
			result[i] = embeddings_centroid_similarity;
			i++;
		}

		return result;
	}
}