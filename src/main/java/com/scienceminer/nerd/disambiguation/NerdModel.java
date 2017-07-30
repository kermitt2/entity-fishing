package com.scienceminer.nerd.disambiguation;

import java.util.*;
import java.io.*;
import java.util.regex.*;
import java.text.*;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.disambiguation.NerdCandidate;
import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.utilities.NerdConfig;
import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.evaluation.*;

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.data.Entity;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.trainer.LabelStat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.training.*;
import com.scienceminer.nerd.utilities.mediaWiki.MediaWikiParser;
import com.scienceminer.nerd.evaluation.*;

import com.scienceminer.nerd.kb.model.Label.Sense;
import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.features.*;

import smile.validation.ConfusionMatrix;
import smile.validation.FMeasure;
import smile.validation.Precision;
import smile.validation.Recall;
import smile.data.*;
import smile.data.parser.*;
import smile.regression.*;
import com.thoughtworks.xstream.*;

/**
 * Class for sharing data structures and methods to be used by the machine learning models
 */
public class NerdModel {

	public enum MLModel {
    	RANDOM_FOREST, GRADIENT_TREE_BOOST
	}

	// default model type
	protected MLModel model = MLModel.RANDOM_FOREST;
	
	// regression model
	protected Regression<double[]> forest = null;

	// for serialization of the classifier
	protected XStream xstream = null;
	protected ArffParser arffParser = null;

	// data
	protected String arffDataset = null;
	protected AttributeDataset attributeDataset = null;
	protected Attribute[] attributes = null;
	protected int positives = 0; // nb of positive examples for the dataset
	protected int negatives =0; // nb of positive examples for the dataset
	
	// for balanced dataset, sampling is 1.0, 
	// for sample < 1.0, positive increases correspondingly
	protected double sampling = 1.0;

	public NerdModel() {
		xstream = new XStream();
		XStream.setupDefaultSecurity(xstream);
		Class[] classArray = new Class[] {
			GradientTreeBoost.class, RandomForest.class, 
			RegressionTree.class, NumericAttribute.class, 
			NominalAttribute.class, Attribute.class};
		xstream.allowTypes(classArray);
		arffParser = new ArffParser();
	}

	public void saveTrainingData(File file) throws IOException, Exception {
		FileUtils.writeStringToFile(file, arffDataset);
		System.out.println("Training data saved under " + file.getPath());
	}
	
	public void loadTrainingData(File file) throws Exception{
		attributeDataset = arffParser.parse(new FileInputStream(file));
		System.out.println("Training data loaded from file " + file.getPath());
	}
	
	public void clearTrainingData() {
		//dataset = null;
		arffDataset = null;
		attributeDataset = null;
	}
}