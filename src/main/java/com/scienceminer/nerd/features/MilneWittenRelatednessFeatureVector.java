package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * Features corresponding only to Milne and Witten relatedness measure.
 */
public class MilneWittenRelatednessFeatureVector extends GenericRankerFeatureVector {

	public MilneWittenRelatednessFeatureVector() {
		super();
		title = "Milne and Witten relatedness-only disambiguator";
		Add_relatedness = true;
		target_numeric = true;
		target_class = false;
	}
}