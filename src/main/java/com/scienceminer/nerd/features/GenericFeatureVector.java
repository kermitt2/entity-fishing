package com.scienceminer.nerd.features;

import java.io.*;
import java.util.*;
import java.text.*;
import java.util.regex.*;

import smile.data.Attribute;

public class GenericFeatureVector {
    public String title = "Generic";

    public String string = null; // lexical feature
    public double label = 0.0; // numerical label if known
    public String classes = "N"; // class label if known
}