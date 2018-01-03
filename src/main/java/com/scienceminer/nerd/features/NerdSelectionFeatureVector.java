package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * A selector with basic features and entity embeddings similarity score
 */
public class NerdSelectionFeatureVector extends GenericSelectionFeatureVector {
	
	public NerdSelectionFeatureVector() {
		super();
		title = "NERD selector with basic and embeddings features";
		Add_nerd_score = true;
		Add_prob_anchor_string = true;
		//Add_prob_c = true;
		//Add_nb_tokens = true;
		Add_relatedness = true;
		Add_inContext = true;
		Add_isNe = true;
		Add_tf_idf = true;
		Add_dice = true;
		//Add_embeddings_LR_similarity = true;
		target_numeric = true;
		target_class = false;
	}
}