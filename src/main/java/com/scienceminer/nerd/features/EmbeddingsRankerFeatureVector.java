package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * A features set for NERD ranker with only entity embeddings similarity score
 */
public class EmbeddingsRankerFeatureVector extends GenericRankerFeatureVector {

	public EmbeddingsRankerFeatureVector() {
		super();
		title = "NERD ranker with embeddings";
		//Add_prob_c = true;
		Add_embeddings_centroid_similarity = true;
		target_numeric = true;
		target_class = false;
	}
}