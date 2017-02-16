package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * The most basic disambiguator considering only the conditional 
 * probability of the concept given the string to rank candidates.
 */
public class BaselineRankerFeatureVector extends GenericRankerFeatureVector {
	
	public BaselineRankerFeatureVector() {
		super();
		title = "Baseline NERD ranker";
		Add_prob_c = true;
		target_numeric = true;
		target_class = false;
	}

}