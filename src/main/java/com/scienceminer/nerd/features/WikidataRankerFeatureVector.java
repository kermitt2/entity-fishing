package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * A features set for NERD ranker with basic features, entity embeddings similarity score 
 * and Wikidata entity identifiers (e.g via P31 relation)
 */
public class WikidataRankerFeatureVector extends GenericRankerFeatureVector {

	public WikidataRankerFeatureVector() {
		super();
		title = "NERD ranker with basic embeddings and Wikidata features";
		//Add_prob_c = true;
		Add_relatedness = true;
		Add_context_quality = true;
		//Add_ner = true;
		Add_bestCaseContext = true;
		Add_embeddings_LR_similarity = true;
		//Add_wikidata_id = true;
		Add_wikidata_P31_entity_id = true;
		target_numeric = true;
		target_class = false;
	}
}