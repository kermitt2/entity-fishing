package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * The most basic selector considering only ranking score for
 * selecting the entity linking or not.
 */
public class BaselineSelectionFeatureVector extends GenericSelectionFeatureVector {
	
	public BaselineSelectionFeatureVector() {
		super();
		title = "Baseline NERD selector";
		Add_nerd_score = true;
		target_numeric = true;
		target_class = false;
	}

}