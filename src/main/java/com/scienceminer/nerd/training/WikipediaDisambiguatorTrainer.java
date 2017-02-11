package com.scienceminer.nerd.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.disambiguation.*;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.exceptions.NerdResourceException;

import org.wikipedia.miner.annotation.ArticleCleaner.SnippetLength;
//import org.wikipedia.miner.annotation.TopicDetector;
//import org.wikipedia.miner.annotation.weighting.LinkDetector;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.*;

import weka.classifiers.Classifier;
import weka.core.Utils;

/**
 * Train and evaluate a NerdDisambiguator classifier using Wikipedia articles as 
 * traininig data.
 */
public class WikipediaDisambiguatorTrainer {

	private Wikipedia wikipedia = null;

	//directory in which files will be stored
	private File dataDir = null;

	//classes for performing annotation
	private NerdDisambiguator disambiguator = null;
	//private TopicDetector topicDetector;
	//private LinkDetector linkDetector;

	//article set files
	//private File _artsTrain, _artsTestDisambig, _artsTestDetect;

	ArticleSet[] articleSets = null;

	//feature data files
	private File arffDisambig, arffDetect;

	//model files
	private File modelDisambig, modelDetect;

	private String lang = null;

	public WikipediaDisambiguatorTrainer(File dataDir, String lang) throws Exception {
		// lexicon
		Lexicon lexicon = null;
		try {
			lexicon = Lexicon.getInstance();
		}
		catch(Exception e) {
			throw new NerdResourceException("Error instanciating the lexicon. ", e);
		}
		this.dataDir = dataDir;
		this.wikipedia = lexicon.getWikipediaConf(lang);
		this.lang = lang;

		this.disambiguator = new NerdDisambiguator(this.wikipedia, 
			NerdEngine.minSenseProbability,
			NerdEngine.maxLabelLength, 
			NerdEngine.minLinkProbability,
			NerdEngine.maxContextSize);
		//topicDetector = new TopicDetector(wikipedia, disambiguator);
		//linkDetector = new LinkDetector(wikipedia);

		//_artsTrain = new File(dataDir.getPath() + "/" + lang + "/articlesTrain.csv");
		//_artsTestDisambig = new File(dataDir.getPath() + "/" + lang + "/articlesTestDisambig.csv");
		//_artsTestDetect = new File(dataDir.getPath() + "/models-" + lang + "/articlesTestDetect.csv");

		arffDisambig = new File(dataDir.getPath() + "/" + lang + "/disambig.arff");
		//arffDetect = new File(dataDir.getPath() + "/models-" + lang + "/detect.arff");

		modelDisambig = new File(dataDir.getPath() + "/" + lang + "/disambig.model");
		//modelDetect = new File(dataDir.getPath() + "/models-" + lang + "/detect.model");
	}

	private void gatherArticleSets() throws IOException{
		int[] sizes = {100,50};
		 //int[] sizes = {500,100,100};
		 //int[] sizes = {5000,1000,1000};

	      /*ArticleSet[] articleSets = new ArticleSetBuilder()
	          .setMinOutLinks(15)
	          .setMinInLinks(20)
	          .setMaxListProportion(0.1)
	          .setMinWordCount(200)
	          .setMaxWordCount(2000)
	          .buildExclusiveSets(sizes, wikipedia);*/
		  
	    articleSets = new ArticleSetBuilder()
	        .setMinOutLinks(20)
	        .setMinInLinks(30)
	        .setMaxListProportion(0.1)
	        .setMinWordCount(150)
	        .setMaxWordCount(1000)
	        .buildExclusiveSets(sizes, wikipedia);
		
	      //articleSets[0].save(_artsTrain);
	      //articleSets[1].save(_artsTestDisambig);
	      //articleSets[2].save(_artsTestDetect);
	}

	private void createArffFiles(String datasetName) throws IOException, Exception {
		/*if (!_artsTrain.canRead()) 
	          throw new Exception("Article sets have not yet been created");
	    ArticleSet trainingSet = new ArticleSet(_artsTrain, wikipedia);*/
		
	    ArticleSet trainingSet = articleSets[0];

	    disambiguator.train(trainingSet, SnippetLength.full, datasetName + "_disambiguation");
	    disambiguator.saveTrainingData(arffDisambig);
	    //disambiguator.trainClassifier();
	      
	    //linkDetector.train(trainingSet, SnippetLength.full, datasetName + "_detection", topicDetector, null);
	    //linkDetector.saveTrainingData(arffDetect);
	}

	private void createModels(String configDisambig, String configDetect) throws Exception {
		//if (!arffDisambig.canRead() || !arffDetect.canRead())
		if (!arffDisambig.canRead())
	        throw new Exception("Arff files have not yet been created");
		
	    //disambiguator.loadTrainingData(arffDisambig);
	    disambiguator.trainModel();
	    /*if (configDisambig == null || configDisambig.trim().length() == 0) {
	        disambiguator.trainClassifier();
	    } else {
	        //Classifier classifier = buildClassifierFromOptString(configDisambig);
	       	disambiguator.trainClassifier();
	    }*/
	    disambiguator.saveModel(modelDisambig);
		
	    /*linkDetector.loadTrainingData(arffDetect);
	    if (configDetect == null || configDisambig.trim().length() == 0) {
	        linkDetector.buildDefaultClassifier();
	    } else {
	        Classifier classifier = buildClassifierFromOptString(configDisambig);
	        linkDetector.buildClassifier(classifier);
	    }
	    linkDetector.saveClassifier(modelDetect);*/
	}
	
	/*private Classifier buildClassifierFromOptString(String optString) throws Exception {
	    String[] options = Utils.splitOptions(optString);
	    String classname = options[0];
	    options[0] = "";
	    return (Classifier) Utils.forName(Classifier.class, classname, options);
	}*/

	private void evaluate() throws Exception {
		//if (!modelDisambig.canRead() || !modelDetect.canRead()) 
		if (!modelDisambig.canRead()) 	
	        throw(new Exception("Classifier models have not yet been created"));
		
	    //if (!_artsTestDisambig.canRead() || !_artsTestDetect.canRead()) 
	    /*if (!_artsTestDisambig.canRead()) 	
	        throw(new Exception("Article sets have not yet been created"));*/
		
		ArticleSet disambigSet = articleSets[1];
	    //ArticleSet disambigSet = new ArticleSet(_artsTestDisambig, wikipedia);
	    //disambiguator.loadClassifier(modelDisambig);
	    Result<Integer> disambigResults = disambiguator.test(disambigSet, wikipedia, SnippetLength.full);
		
	    /*ArticleSet detectSet = new ArticleSet(_artsTestDetect, wikipedia);
	    linkDetector.loadClassifier(modelDetect);
	    Result<Integer> detectResults = linkDetector.test(detectSet, SnippetLength.full, topicDetector, null);*/
		
	    System.out.println();
	    System.out.println("Disambig results: " + disambigResults);
	    //System.out.println("Detect results: " + detectResults);
	}

	public static void main(String args[]) throws Exception {
		File dataDir = new File(args[0]);
		String lang = args[1];
		WikipediaDisambiguatorTrainer trainer = new WikipediaDisambiguatorTrainer(dataDir, lang);

		System.out.println("Create article sets...");
		trainer.gatherArticleSets();

		System.out.println("Create arff files...");
		trainer.createArffFiles("wikipedia");

		System.out.println("Create classifiers...");
		trainer.createModels(null, null);

		System.out.println("Evaluate classifiers...");
		trainer.evaluate();
	}
	
}