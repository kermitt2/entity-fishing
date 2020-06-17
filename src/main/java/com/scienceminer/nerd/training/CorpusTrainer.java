package com.scienceminer.nerd.training;

import com.scienceminer.nerd.disambiguation.NerdRanker;
import com.scienceminer.nerd.disambiguation.NerdSelector;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.evaluation.NEDCorpusEvaluation;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Train and evaluate a NerdRanker and a NerdSelector using annotated corpus as training data.
 *
 */
public class CorpusTrainer {
	//private static final Logger LOGGER = LoggerFactory.getLogger(CorpusTrainer.class);

	private LowerKnowledgeBase wikipedia = null;

	//feature data files
	private File arffRanker = null;
	private File arffSelector = null;

	//model files
	private File modelRanker = null;
	private File modelSelector = null;

	private NerdRanker ranker = null;
	private NerdSelector selector = null;

	private String lang = null;

	List<ArticleTrainingSample> articleSets = null;

	public CorpusTrainer(String lang) {
		this.lang = lang;

		arffRanker = new File("data/wikipedia/training" + File.separator + lang + File.separator+ "ranker.arff");
		arffSelector = new File("data/wikipedia/training" + File.separator + lang + File.separator + "selector.arff");

		try {
			wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf(lang);
			if(wikipedia == null) {
				System.out.println("Problem initalizing language-dependent knowledge base for language " + lang);
			}

			this.ranker = new NerdRanker(this.wikipedia);
			this.selector = new NerdSelector(this.wikipedia);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void createArticleSets(String corpus, double ratio, LowerKnowledgeBase wikipedia) throws IOException{
		articleSets = ArticleTrainingSample.buildExclusiveCorpusSets(corpus, ratio, wikipedia);
	}


	public void train(String corpus) {
		String corpusPath = "data/corpus/corpus-long/" + corpus + File.separator;
		String corpusRefPath = corpusPath + corpus + ".xml";
		File corpusRefFile = new File(corpusRefPath);

		if (!corpusRefFile.exists()) {
			System.out.println("The reference file for corpus " + corpus + " is not found: " + corpusRefFile.getPath());
			return;
		}

		try {
			createArticleSets(corpus, 1.0, wikipedia); // all for ranker

			System.out.println("Create Ranker arff files...");
			createRankerArffFiles();
			System.out.println("Create Ranker classifier...");
			createRankerModel();

			/*System.out.println("Create Selector arff files...");
			createSelectorArffFiles();
			System.out.println("Create Selector classifier...");
			createSelectorModel();*/
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	private void createRankerArffFiles() throws IOException, Exception {
		if (articleSets.size() == 0) {
			System.out.println("No training file for ranker");
			return;
		}
	    ArticleTrainingSample trainingSample = articleSets.get(0);
	    ranker.train(trainingSample);
	    ranker.saveTrainingData(arffRanker);
	}

	private void createSelectorArffFiles() throws IOException, Exception {
		if (articleSets.size() < 2) {
			System.out.println("No training file for selector");
			return;
		}
	    ArticleTrainingSample trainingSample = articleSets.get(1);
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

	public static void main(String args[]) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: command [name_of_corpus] [lang]");
			System.err.println("corpus must be one of: " + NEDCorpusEvaluation.corpora.toString());
			System.err.println("lang must be one of: " + UpperKnowledgeBase.TARGET_LANGUAGES.toString());
			System.exit(-1);
		}
		String corpus = args[0].toLowerCase();
		if (!NEDCorpusEvaluation.corpora.contains(corpus)) {
			System.err.println("corpus must be one of: " + NEDCorpusEvaluation.corpora.toString());
			System.exit(-1);
		}
		String lang = args[1].toLowerCase();
		if (!UpperKnowledgeBase.TARGET_LANGUAGES.contains(lang)) {
			System.err.println("lang must be one of: " + UpperKnowledgeBase.TARGET_LANGUAGES.toString());
			System.exit(-1);
		}

		CorpusTrainer trainer = new CorpusTrainer(lang);
		System.out.println("Training with corpus " + args[0]);
		trainer.train(corpus);
	}
	
}