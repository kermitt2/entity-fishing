package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * Features corresponding to Milne and Witten NERD disambiguator
 * as implemented in Wikipedia Miner.
 */
public class MilneWittenFeatureVector extends GenericFeatureVector {

	public MilneWittenFeatureVector() {
		super();
		title = "Milne and Witten disambiguator";
		Add_prob_c = true;
		Add_relatedness = true;
		Add_context_quality = true;
		target_numeric = true;
		target_class = false;
	}
	
	public double[] toMatrix() {
		double[] result = new double[this.getNumFeatures()];
		result[0] = prob_c;
		result[1] = relatedness;
		result[2] = context_quality;
		return result;
	}
}