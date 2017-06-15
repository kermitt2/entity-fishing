package com.scienceminer.nerd.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.disambiguation.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.evaluation.*;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.model.Wikipedia;

/**
 * Train and evaluate a NerdRanker and a NerdSelector using Wikipedia articles as 
 * traininig data.
 */
public class WikipediaTrainer {

	private Wikipedia wikipedia = null;

	//directory in which files will be stored
	private File dataDir = null;

	//classes for performing annotation
	private NerdRanker ranker = null; 
	private NerdSelector selector = null;

	ArticleTrainingSample[] articleSamples = null;

	//feature data files
	private File arffRanker = null;
	private File arffSelector = null;

	//model files
	private File modelRanker = null;
	private File modelSelector = null;

	private String lang = null;

	public WikipediaTrainer(File dataDir, String lang) throws Exception {
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

		// load, and possibly create if not yet done, the full text of wikipedia articles
		// database
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
		int[] sizes = {500,100,100};
		//int[] sizes = {5000,1000,1000};
		ArticleTrainingSampleCriterias criterias = new ArticleTrainingSampleCriterias();
		criterias.setMinOutLinks(20);
		criterias.setMinInLinks(30);
		criterias.setMinWordCount(150);
		criterias.setMaxWordCount(1000);
		articleSamples = ArticleTrainingSample.buildExclusiveSamples(criterias, sizes, wikipedia);
	}

	private void createArffFiles(String datasetName) throws IOException, Exception {
	    ArticleTrainingSample trainingSample = articleSamples[0];

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
		ArticleTrainingSample rankerSample = articleSamples[1];
	    Result<Integer> rankerResults = ranker.test(rankerSample);
		
	    ArticleTrainingSample selectorSample = articleSamples[1];
	    Result<Integer> selectorResults = selector.test(selectorSample, ranker);
		
	    System.out.println("------------------------------------------------");
	    System.out.println("Ranker results: " + rankerResults);
	    System.out.println("Selector results: " + selectorResults);
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