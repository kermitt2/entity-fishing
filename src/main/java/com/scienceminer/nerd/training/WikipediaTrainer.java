package com.scienceminer.nerd.training;

import com.scienceminer.nerd.disambiguation.NerdRanker;
import com.scienceminer.nerd.disambiguation.NerdSelector;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import org.grobid.trainer.evaluation.LabelStat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Train and evaluate a NerdRanker and a NerdSelector using Wikipedia articles as training data.
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

		arffRanker = new File(dataDir.getPath() + File.separator + lang + File.separator + "ranker.arff");
		arffSelector = new File(dataDir.getPath() + File.separator + lang + File.separator + "selector.arff");

		//modelRanker = new File(dataDir.getPath() + "/" + lang + "/ranker.model");
		//modelSelector = new File(dataDir.getPath() + "/" + lang + "/selector.model");
	}

	private void createArticleSamples() throws IOException{
		//List<Integer> sampleSizes = Arrays.asList(5000,5000,1000);
		//List<Integer> sampleSizes = Arrays.asList(500,500,100,100,100);
		List<Integer> sampleSizes = Arrays.asList(5000,500,500,100,100);
		//List<Integer> sampleSizes = Arrays.asList(500,100,100,100,100);
		// training ranker, training selector, eval ranker, eval selector, eval end-to-end

		ArticleTrainingSampleCriterias criteriaTraining = new ArticleTrainingSampleCriterias();
		criteriaTraining.setMinOutLinks(50);
		criteriaTraining.setMinInLinks(20);
		criteriaTraining.setMinWordCount(200);
		criteriaTraining.setMaxWordCount(1000);

		ArticleTrainingSampleCriterias criteriaEvaluation = new ArticleTrainingSampleCriterias();
		criteriaEvaluation.setMinOutLinks(20);
		criteriaEvaluation.setMinInLinks(20);
		criteriaEvaluation.setMinWordCount(200);
		criteriaEvaluation.setMaxWordCount(1000);

		/*criterias.setMinOutLinks(50);
		criterias.setMinInLinks(100);
		criterias.setMinWordCount(300);
		criterias.setMaxWordCount(2000);*/

		articleSamples = ArticleTrainingSample.buildExclusiveSamples(criteriaTraining, 
			criteriaEvaluation, sampleSizes, wikipedia);
	}

	private void createRankerArffFiles() throws IOException, Exception {
	    ArticleTrainingSample trainingSample = articleSamples.get(0);
	    ranker.train(trainingSample);
	    ranker.saveTrainingData(arffRanker);
	}

	private void createSelectorArffFiles() throws IOException, Exception {
	    ArticleTrainingSample trainingSample = articleSamples.get(1);
	    selector.train(trainingSample, arffSelector);
	}

	private void createRankerModel() throws Exception {
	    ranker.trainModel();
	    ranker.saveModel();
	}

	private void createSelectorModel() throws Exception {
		selector.trainModel();
	    selector.saveModel();
	}

	private void evaluateRanker() throws Exception {
		ArticleTrainingSample rankerSample = articleSamples.get(2);
		System.out.println("-------------------------- evaluating ranker model --------------------------");
		LabelStat rankerStats = ranker.evaluate(rankerSample);
	}

	private void evaluateSelector() throws Exception {
	    ArticleTrainingSample selectorSample = articleSamples.get(3);
	    System.out.println("------------------------- evaluating selector model -------------------------");
	    LabelStat selectorResults = selector.evaluate(selectorSample, ranker, false);

	    ArticleTrainingSample end2endSample = articleSamples.get(4);
	    System.out.println("--------------------------- evaluating end-to-end ---------------------------");
	    LabelStat finalResults = selector.evaluate(end2endSample, ranker, true);
	}

	public static void main(String args[]) throws Exception {
		File dataDir = new File(args[0]);
		String lang = args[1];
		WikipediaTrainer trainer = new WikipediaTrainer(dataDir, lang);

		System.out.println("Create article sets...");
		trainer.createArticleSamples();

		System.out.println("Create Ranker arff files...");
		trainer.createRankerArffFiles();
		System.out.println("Create Ranker classifier...");
		trainer.createRankerModel();

		System.out.println("Create Selector arff files...");
		trainer.createSelectorArffFiles();
		System.out.println("Create Selector classifier...");
		trainer.createSelectorModel();

		System.out.println("Evaluate classifiers...");
		trainer.evaluateRanker();
		trainer.evaluateSelector();
	}
	
}