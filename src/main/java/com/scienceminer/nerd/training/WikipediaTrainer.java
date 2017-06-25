package com.scienceminer.nerd.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.disambiguation.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.evaluation.*;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.trainer.LabelStat;

/**
 * Train and evaluate a NerdRanker and a NerdSelector using Wikipedia articles as 
 * traininig data.
 */
public class WikipediaTrainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaTrainer.class);

	private LowerKnowledgeBase wikipedia = null;

	//directory in which files will be stored
	private File dataDir = null;

	//classes for performing annotation
	private NerdRanker ranker = null;
	private NerdSelector selector = null;

	List<ArticleTrainingSample> articleSamples = null;

	//feature data files
	private File arffRanker = null;
	private File arffSelector = null;

	//model files
	private File modelRanker = null;
	private File modelSelector = null;

	private String lang = null;

	public WikipediaTrainer(File dataDir, String lang) throws Exception {
		// KB
		UpperKnowledgeBase upperKnowledgeBase = null;
		try {
			upperKnowledgeBase = UpperKnowledgeBase.getInstance();
		}
		catch(Exception e) {
			throw new NerdResourceException("Error instanciating the knowledge base. ", e);
		}
		this.dataDir = dataDir;
		this.wikipedia = upperKnowledgeBase.getWikipediaConf(lang);
		this.lang = lang;

		// load, and possibly create if not yet done, the full text of wikipedia articles
		// database
		LOGGER.info("Loading full wikitext content - this will take a while the first time");
		this.wikipedia.loadFullContentDB();

		this.ranker = new NerdRanker(this.wikipedia, 
			NerdEngine.minSenseProbability,
			NerdEngine.maxLabelLength, 
			NerdEngine.minLinkProbability,
			NerdEngine.maxContextSize);
		
		this.selector = new NerdSelector(this.wikipedia);

		arffRanker = new File(dataDir.getPath() + "/" + lang + "/ranker.arff");
		arffSelector = new File(dataDir.getPath() + "/" + lang + "/selector.arff");

		modelRanker = new File(dataDir.getPath() + "/" + lang + "/ranker.model");
		modelSelector = new File(dataDir.getPath() + "/" + lang + "/selector.model");
	}

	private void createArticleSamples() throws IOException{
		//int[] sizes = {500,100,100};
		//int[] sizes = {5000,1000,1000};
		List<Integer> sizes = Arrays.asList(500,100,100);
		ArticleTrainingSampleCriterias criterias = new ArticleTrainingSampleCriterias();
		criterias.setMinOutLinks(20);
		criterias.setMinInLinks(30);
		criterias.setMinWordCount(150);
		criterias.setMaxWordCount(1000);
		articleSamples = ArticleTrainingSample.buildExclusiveSamples(criterias, sizes, wikipedia);
	}

	private void createArffFiles(String datasetName) throws IOException, Exception {
	    ArticleTrainingSample trainingSample = articleSamples.get(0);

	    ranker.train(trainingSample, datasetName + "_disambiguation");
	    ranker.saveTrainingData(arffRanker);
	      
	    selector.train(trainingSample, datasetName + "_selection");
	    selector.saveTrainingData(arffSelector);
	}

	private void createModels() throws Exception {
	    ranker.trainModel();
	    ranker.saveModel(modelRanker);
		
		selector.trainModel();
	    selector.saveModel(modelSelector);
	}

	private void evaluate() throws Exception {
		ArticleTrainingSample rankerSample = articleSamples.get(1);
		System.out.println("-------------------------- evaluating ranker model --------------------------");
		LabelStat rankerStats = ranker.evaluate(rankerSample);
	    
	    ArticleTrainingSample selectorSample = articleSamples.get(1);
	    System.out.println("------------------------- evaluating selector model -------------------------");
	    LabelStat selectorResults = selector.evaluate(selectorSample, ranker);
	}

	public static void main(String args[]) throws Exception {
		File dataDir = new File(args[0]);
		String lang = args[1];
		WikipediaTrainer trainer = new WikipediaTrainer(dataDir, lang);

		System.out.println("Create article sets...");
		trainer.createArticleSamples();

		System.out.println("Create arff files...");
		trainer.createArffFiles("wikipedia");

		System.out.println("Create classifiers...");
		trainer.createModels();

		System.out.println("Evaluate classifiers...");
		trainer.evaluate();
	}
	
}