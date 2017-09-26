package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * A features set for NERD ranker with basic features and entity embeddings similarity score
 */
public class NerdRankerFeatureVector extends GenericRankerFeatureVector {

	public NerdRankerFeatureVector() {
		super();
		title = "(N)ERD ranker with embeddings";
		//Add_prob_c = true;
		Add_relatedness = true;
		Add_context_quality = true;
		//Add_ner = true;
		Add_bestCaseContext = true;
		Add_embeddings_LR_similarity = true;
		target_numeric = true;
		target_class = false;
	}
}