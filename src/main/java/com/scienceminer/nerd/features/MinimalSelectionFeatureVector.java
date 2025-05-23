package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;

import java.util.regex.*;

/**
 * A selector considering ranking score and lexical cohesion for
 * selecting the entity linking or not.
 */
public class MinimalSelectionFeatureVector extends GenericSelectionFeatureVector {
    
    public MinimalSelectionFeatureVector() {
        super();
        title = "Minimal NERD selector";
        Add_nerd_score = true;
        //Add_prob_anchor_string = true;
        Add_prob_c = true;
        Add_nb_tokens = true;
        //Add_relatedness = true;
        Add_inContext = true;
        //Add_isNe = true;
        Add_tf_idf = true;
        Add_dice = true;
        target_numeric = true;
        target_class = false;
    }
}