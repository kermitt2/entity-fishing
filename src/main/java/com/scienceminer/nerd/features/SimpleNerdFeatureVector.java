package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * A simple working features set for NERD ranker.
 */
public class SimpleNerdFeatureVector extends GenericRankerFeatureVector {

	public SimpleNerdFeatureVector() {
		super();
		title = "(N)ERD ranker";
		Add_prob_c = true;
		Add_relatedness = true;
		Add_context_quality = true;
		//Add_ner = true;
		target_numeric = true;
		target_class = false;
	}
}