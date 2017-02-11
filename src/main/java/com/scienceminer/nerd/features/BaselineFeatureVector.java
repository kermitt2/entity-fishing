package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * The most basic classifier considering only the conditional 
 * probability of the concept given the string to rank candidates.
 */
public class BaselineFeatureVector extends GenericFeatureVector {
	
	public BaselineFeatureVector() {
		super();
		title = "Baseline NERD classifier";
		Add_prob_c = true;
		target_numeric = true;
		target_class = false;
	}

}