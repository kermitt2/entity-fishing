package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * A simple working features set for NERD ranker.
 */
public class SimpleRankerFeatureVector extends GenericRankerFeatureVector {

	public SimpleRankerFeatureVector() {
		super();
		title = "Simple NERD ranker";
		Add_prob_c = true;
		Add_relatedness = true;
		Add_context_quality = true;
		//Add_ner = true;
		//Add_bestCaseContext = true;
		target_numeric = true;
		target_class = false;
	}
}