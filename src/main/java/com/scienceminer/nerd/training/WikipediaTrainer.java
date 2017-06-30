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

		this.ranker = new NerdRanker(this.wikipedia);
		
		this.selector = new NerdSelector(this.wikipedia);

		arffRanker = new File(dataDir.getPath() + "/" + lang + "/ranker.arff");
		arffSelector = new File(dataDir.getPath() + "/" + lang + "/selector.arff");

		modelRanker = new File(dataDir.getPath() + "/" + lang + "/ranker.model");
		modelSelector = new File(dataDir.getPath() + "/" + lang + "/selector.model");
	}

	private void createArticleSamples() throws IOException{
		//List<Integer> sizes = Arrays.asList(5000,5000,1000);
		List<Integer> sizes = Arrays.asList(100,100,200);
		//List<Integer> sizes = Arrays.asList(20,20,10);
		ArticleTrainingSampleCriterias criterias = new ArticleTrainingSampleCriterias();
		criterias.setMinOutLinks(20);
		criterias.setMinInLinks(50);
		criterias.setMinWordCount(200);
		criterias.setMaxWordCount(2000);
		articleSamples = ArticleTrainingSample.buildExclusiveSamples(criterias, sizes, wikipedia);
	}

	private void createRankerArffFiles(String datasetName) throws IOException, Exception {
	    ArticleTrainingSample trainingSample = articleSamples.get(0);
	    ranker.train(trainingSample, datasetName + "_disambiguation");
	    ranker.saveTrainingData(arffRanker);
	}

	private void createSelectorArffFiles(String datasetName) throws IOException, Exception {
	    ArticleTrainingSample trainingSample = articleSamples.get(1);
	    selector.train(trainingSample, datasetName + "_selection");
	    selector.saveTrainingData(arffSelector);
	}

	private void createRankerModel() throws Exception {
	    ranker.trainModel();
	    ranker.saveModel(modelRanker);
	}

	private void createSelectorModel() throws Exception {
		selector.trainModel();
	    selector.saveModel(modelSelector);
	}

	private void evaluateRanker() throws Exception {
		ArticleTrainingSample rankerSample = articleSamples.get(2);
		System.out.println("-------------------------- evaluating ranker model --------------------------");
		LabelStat rankerStats = ranker.evaluate(rankerSample);
	}

	private void evaluateSelector() throws Exception {
	    ArticleTrainingSample selectorSample = articleSamples.get(2);
	    System.out.println("------------------------- evaluating selector model -------------------------");
	    LabelStat selectorResults = selector.evaluate(selectorSample, ranker, false);

	    System.out.println("--------------------------- evaluating end-to-end ---------------------------");
	    LabelStat finalResults = selector.evaluate(selectorSample, ranker, true);
	}

	public static void main(String args[]) throws Exception {
		File dataDir = new File(args[0]);
		String lang = args[1];
		WikipediaTrainer trainer = new WikipediaTrainer(dataDir, lang);

		System.out.println("Create article sets...");
		trainer.createArticleSamples();

		System.out.println("Create Ranker arff files...");
		//trainer.createRankerArffFiles("wikipedia");
		System.out.println("Create Ranker classifier...");
		//trainer.createRankerModel();

		System.out.println("Create Selector arff files...");
		//trainer.createSelectorArffFiles("wikipedia");
		System.out.println("Create Selector classifier...");
		//trainer.createSelectorModel();

		System.out.println("Evaluate classifiers...");
		trainer.evaluateRanker();
		trainer.evaluateSelector();
	}
	
}