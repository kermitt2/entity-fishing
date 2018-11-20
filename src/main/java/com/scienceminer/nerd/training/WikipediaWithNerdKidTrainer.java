package com.scienceminer.nerd.training;

import com.scienceminer.nerd.disambiguation.NerdRanker;
import com.scienceminer.nerd.disambiguation.NerdSelector;
import com.scienceminer.nerd.evaluation.NEDCorpusEvaluation;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class WikipediaWithNerdKidTrainer {
    public static final Logger LOGGER = LoggerFactory.getLogger(WikipediaWithNerdKidTrainer.class);

    private LowerKnowledgeBase lowerKnowledgeBase = null;

    //feature data files
    private File arffRanker = null;
    private File arffSelector = null;

    //model files
    private File modelRanker = null;
    private File modelSelector = null;

    private NerdRanker ranker = null;
    private NerdSelector selector = null;

    List<ArticleTrainingSample> articleSets = null;

    public WikipediaWithNerdKidTrainer() {
        arffRanker = new File("data/nerdKid/training/ranker.arff");
        arffSelector = new File("data/nerdKid/training/selector.arff");

        try {
            lowerKnowledgeBase = UpperKnowledgeBase.getInstance().getWikipediaConf("en");
            if(lowerKnowledgeBase == null) {
                System.out.println("Problem initalizing language-dependent knowledge base for language: English");
            }

            this.ranker = new NerdRanker(this.lowerKnowledgeBase);
            this.selector = new NerdSelector(this.lowerKnowledgeBase);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void createArticleSets(String corpus, double ratio, LowerKnowledgeBase lowerKnowledgeBase) throws IOException{
        articleSets = ArticleTrainingSample.buildExclusiveCorpusSets(corpus, ratio, lowerKnowledgeBase);
    }


    public void train(String corpus) {
        String corpusPath = "data/corpus/corpus-long/" + corpus + "/";
        String corpusRefPath = corpusPath + corpus + ".xml";
        File corpusRefFile = new File(corpusRefPath);

        if (!corpusRefFile.exists()) {
            System.out.println("The reference file for corpus " + corpus + " is not found: " + corpusRefFile.getPath());
            return;
        }

        try {
            createArticleSets(corpus, 1.0, lowerKnowledgeBase); // all for ranker

            System.out.println("Create Ranker arff files...");
            createRankerArffFiles();
            System.out.println("Create Ranker classifier...");
            createRankerModel(corpus);
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

    private void createRankerModel(String corpus) throws Exception {
        ranker.trainModel();
        ranker.saveModelNerdKid(corpus);
    }

    public static void main(String args[]) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: command [name_of_corpus]");
            System.err.println("corpus must be one of: " + NEDCorpusEvaluation.corpora.toString());
            System.exit(-1);
        }
        String corpus = args[0].toLowerCase();
        if (!NEDCorpusEvaluation.corpora.contains(corpus)) {
            System.err.println("corpus must be one of: " + NEDCorpusEvaluation.corpora.toString());
            System.exit(-1);
        }

        WikipediaWithNerdKidTrainer trainer = new WikipediaWithNerdKidTrainer();
        System.out.println("Training with corpus " + args[0]);
        trainer.train(corpus);
    }

}
