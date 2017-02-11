package com.scienceminer.nerd.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;

import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.comparison.ComparisonDataSet;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;

import weka.classifiers.Classifier;
import weka.core.Utils;

/**
 * Train and evaluate a Wikipedia article comparer.
 */
public class WikipediaComparerTrainer {
	
	private Wikipedia wikipedia = null;

	//directory in which files will be stored
	private File dataDir = null;
	private String lang = null;
	private File datasetFile = null;
	
	private ComparisonDataSet dataset = null;
	
	//classes for performing comparison
	private ArticleComparer artComparer = null;
	//private LabelComparer labelComparer = null;
	
	private File _arffArtCompare, _arffLabelDisambig, _arffLabelCompare;
	
	private File _modelArtCompare, _modelLabelDisambig, _modelLabelCompare;
	
	private DecimalFormat df = new DecimalFormat("0.0000");

	public WikipediaComparerTrainer(File dataDir, ComparisonDataSet dataset, Wikipedia wikipedia) throws Exception {
		this.dataDir = dataDir;
		this.wikipedia = wikipedia;
		this.dataset = dataset;
		this.lang = wikipedia.getConfig().getLangCode();
		this.artComparer = new ArticleComparer(this.wikipedia);
		//this.labelComparer = new LabelComparer(this.wikipedia, this.artComparer);
		
		_arffArtCompare = new File(dataDir.getPath() + "/" + lang + "/art_compare.arff");
		//_arffLabelDisambig = new File(dataDir.getPath() + "/lbl_disambig.arff");
		//_arffLabelCompare = new File(dataDir.getPath() + "/lbl_compare.arff");
		
		_modelArtCompare = new File(dataDir.getPath() + "/" + lang + "/art_compare.model");
		//_modelLabelDisambig = new File(dataDir.getPath() + "/lbl_disambig.model");
		//_modelLabelCompare = new File(dataDir.getPath() + "/lbl_compare.model");
	}
	
	private void createArffFiles(String datasetName) throws IOException, Exception {
		artComparer.train(dataset);
		artComparer.buildDefaultClassifier();
		artComparer.saveTrainingData(_arffArtCompare);
			
		/*labelComparer.train(dataset, datasetName);
		labelComparer.buildDefaultClassifiers();
		labelComparer.saveDisambiguationTrainingData(_arffLabelDisambig);
		labelComparer.saveComparisonTrainingData(_arffLabelCompare);*/
	}

	private void createClassifiers(String confArtCompare, String confLabelDisambig, String confLabelCompare) throws Exception {
		//if (!_arffArtCompare.canRead() || !_arffLabelDisambig.canRead() || !_arffLabelCompare.canRead())
	    if (!_arffArtCompare.canRead())
	        throw new Exception("Arff files have not yet been created");
		
	    if (confArtCompare == null || confArtCompare.trim().length() == 0) {	
	    	artComparer.buildDefaultClassifier();
	    } else {
	        Classifier classifier = buildClassifierFromOptString(confArtCompare);
	        artComparer.buildClassifier(classifier);
	    }
	    artComparer.saveClassifier(_modelArtCompare);
		  
	    /*if (confLabelDisambig == null || confLabelDisambig.trim().length() == 0) {
	    	labelComparer.buildDefaultClassifiers();
	    } else {
	        Classifier classifierLabelDisambig = buildClassifierFromOptString(confLabelDisambig);
	        Classifier classifierLabelCompare = buildClassifierFromOptString(confLabelCompare);
	          
	        //TODO: need to use provided classifiers
	        labelComparer.buildDefaultClassifiers();
	    }
	      
	    labelComparer.saveDisambiguationClassifier(_modelLabelDisambig);
	    labelComparer.saveComparisonClassifier(_modelLabelCompare);*/
	}
	
	 private Classifier buildClassifierFromOptString(String optString) throws Exception {
	    String[] options = Utils.splitOptions(optString);
	    String classname = options[0];
	    options[0] = "";
	    return (Classifier) Utils.forName(Classifier.class, classname, options);
	  }

	private void evaluate() throws Exception {
		ComparisonDataSet[][] folds = dataset.getFolds();
		
		double totalArtCompare = 0;
		//double totalLabelDisambig = 0;
		//double totalLabelCompare = 0;
		
		int foldIndex = 0;
		for (ComparisonDataSet[] fold : folds) {
			System.out.println("Fold " + foldIndex);
			foldIndex++;
			
			ComparisonDataSet trainingData = fold[0];
			ComparisonDataSet testData = fold[1];
			
			ArticleComparer artComparer = new ArticleComparer(wikipedia);
			artComparer.train(trainingData);
			artComparer.buildDefaultClassifier();
			
			double corrArtCompare = artComparer.test(testData);
			System.out.println(" - art comparison: " + df.format(corrArtCompare));
			totalArtCompare += corrArtCompare;
			
			/*LabelComparer lblComparer = new LabelComparer(wikipedia, artComparer);
			lblComparer.train(trainingData, "");
			lblComparer.buildDefaultClassifiers(); 
			
			double accLabelDisambig = lblComparer.testDisambiguationAccuracy(testData);
			System.out.println(" - label disambiguation: " + df.format(accLabelDisambig));
			totalLabelDisambig += accLabelDisambig;
			
			double corrLabelCompare = lblComparer.testRelatednessPrediction(testData);
			System.out.println(" - label comparison: " + df.format(corrLabelCompare));
			totalLabelCompare += corrLabelCompare;*/
		}
		
		System.out.println();
		System.out.println("art comparison (correllation); " + df.format(totalArtCompare/folds.length));
		//System.out.println("label disambiguation (accuracy); " + df.format(totalLabelDisambig/folds.length));
		//System.out.println("label comparison (correllation); " + df.format(totalLabelCompare/folds.length));
	}
	
	public static void main(String args[]) throws Exception {
		File dataDir = new File(args[0]);
		File datasetFile = new File(args[1]);
		int maxRelatedness = Integer.parseInt(args[2]);
		ComparisonDataSet dataset = new ComparisonDataSet(datasetFile, maxRelatedness);

		WikipediaConfiguration conf = new WikipediaConfiguration(new File(args[3]));
		//conf.addDatabaseToCache(DatabaseType.label);
		//conf.addDatabaseToCache(DatabaseType.pageLinksInNoSentences);

		Wikipedia wikipedia = new Wikipedia(conf, false);
		WikipediaComparerTrainer trainer = new WikipediaComparerTrainer(dataDir, dataset, wikipedia);
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Create arff files...");
		trainer.createArffFiles("wikipedia");

		System.out.println("Create classifiers...");
		trainer.createClassifiers(null, null, null);

		System.out.println("Evaluate classifiers...");
		trainer.evaluate();
	}
	
}
