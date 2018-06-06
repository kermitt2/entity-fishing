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
		title = "NERD ranker with basic and embeddings features";
		Add_prob_c = true;
		Add_relatedness = true;
		Add_context_quality = true;
		//Add_ner = true;
		Add_bestCaseContext = true;
		Add_embeddings_centroid_similarity = true;

		//Add_wikidata_id = true;
		//Add_ner_type = true;
		// for Nerd-Kid
		Add_nerKid_type = true;

		target_numeric = true;
		target_class = false;
	}
}