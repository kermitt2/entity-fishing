package com.scienceminer.nerd.disambiguation;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.disambiguation.NerdCandidate;
import com.scienceminer.nerd.utilities.NerdProperties;
import org.grobid.core.utilities.OffsetPosition;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.service.NerdQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dmilne.weka.wrapper.Dataset;
import org.dmilne.weka.wrapper.Decider;
import org.dmilne.weka.wrapper.DeciderBuilder;

import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.model.*;
import org.wikipedia.miner.annotation.ArticleCleaner;
import org.wikipedia.miner.annotation.TopicReference;
import org.wikipedia.miner.annotation.ArticleCleaner.SnippetLength;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;
import org.wikipedia.miner.model.Label.Sense;

import weka.classifiers.*;
import weka.classifiers.meta.Bagging;
import weka.core.* ;

import org.grobid.core.lang.Language;
import org.grobid.core.utilities.LanguageUtilities;

/**
 * 
 */
public class NerdDisambiguator {

	/**
	 * The class Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(NerdDisambiguator.class);

	private Classifier ranker_short = null;
	private Classifier ranker_long = null;

	private Classifier selector_short = null;
	private Classifier selector_long = null;

	// ranker model files
	private static final String rankerShortFile = "data/models/ranker-short.model"; 
	private static final String rankerLongFile = "data/models/ranker-long.model";
	
	// selector model files
	private static final String selectorShortFile = "data/models/selector-short.model"; 
	private static final String selectorLongFile = "data/models/selector-long.model";

	private Wikipedia wikipedia;
	private ArticleCleaner cleaner;
	private TextProcessor tp;
	private ArticleComparer comparer;

	private double minSenseProbability; 
	private int maxLabelLength = 20; 
	private double minLinkProbability;
	private int maxContextSize;

	public NerdDisambiguator(Wikipedia wikipedia, 
							double minSenseProbability,
							int maxLabelLength, 
							double minLinkProbability,
							int maxContextSize) throws Exception {
		this.wikipedia = wikipedia;
		this.minSenseProbability = minSenseProbability;
		this.maxLabelLength = maxLabelLength;
		this.minLinkProbability = minLinkProbability;
		this.maxContextSize = maxContextSize;
		
		WikipediaConfiguration conf = wikipedia.getConfig();
		comparer = new ArticleComparer(wikipedia);
		cleaner = new ArticleCleaner();
		tp = conf.getDefaultTextProcessor();
		
		decider = (Decider<Attributes, Boolean>) new DeciderBuilder<Attributes>("LinkDisambiguator", Attributes.class)
			.setDefaultAttributeTypeNumeric()
			.setClassAttributeTypeBoolean("isCorrectSense")
			.build();

		if (conf.getTopicDisambiguationModel() != null)
			loadClassifier(conf.getTopicDisambiguationModel());
		
		// rankers 
		/*ObjectInputStream in = null;
		try {
			BufferedInputStream inStream =
				new BufferedInputStream(new FileInputStream(rankerShortFile));
			in = new ObjectInputStream(inStream);
			ranker_short = (Classifier)in.readObject();
			
			if (in != null)
				in.close();
			
			inStream =
				new BufferedInputStream(new FileInputStream(selectorShortFile));
			in = new ObjectInputStream(inStream);
			selector_short = (Classifier)in.readObject();
		}
		catch(Exception e) {
			//throw new NerdResourceException("Error when opening the ranker model for short text: " 
			//	+ rankerShortFile, e);
		}
		finally { 
			if (in != null)
				in.close();
		}*/
	}
	
	
	private enum Attributes {commonness, relatedness, contextQuality};
	
	private Decider<Attributes, Boolean> decider;
	private Dataset<Attributes, Boolean> dataset;
	
	private int sensesConsidered = 0;

	//TODO: this should really be refactored as a separate filter
	@SuppressWarnings("unchecked")
	private void weightTrainingInstances() {

		double positiveInstances = 0;
		double negativeInstances = 0; 

		Enumeration<Instance> e = dataset.enumerateInstances();

		while (e.hasMoreElements()) {
			Instance i = (Instance) e.nextElement();

			double isValidSense = i.value(3);

			if (isValidSense == 0) 
				positiveInstances ++;
			else
				negativeInstances ++;
		}

		double p = (double) positiveInstances / (positiveInstances + negativeInstances);

		e = dataset.enumerateInstances();

		while (e.hasMoreElements()) {
			Instance i = (Instance) e.nextElement();

			double isValidSense = i.value(3);

			if (isValidSense == 0) 
				i.setWeight(0.5 * (1.0/p));
			else
				i.setWeight(0.5 * (1.0/(1-p)));
		}
	}

	public double getProbabilityOfSense(double commonness, double relatedness, NerdContext context) throws Exception {

		Instance i = decider.getInstanceBuilder()
			.setAttribute(Attributes.commonness, commonness)
			.setAttribute(Attributes.relatedness, relatedness)
			.setAttribute(Attributes.contextQuality, context.getQuality())
			.build();
		
		sensesConsidered++;
		
		return decider.getDecisionDistribution(i).get(true);
	}

	/**
	 * Trains the disambiguator on a set of Wikipedia articles. This only builds up the training data. 
	 * You will still need to build a classifier in order to use the trained disambiguator. 
	 * 
	 * @param articles the set of articles to use for training. You should make sure these are reasonably tidy, and roughly representative (in size, link distribution, etc) as the documents you intend to process automatically.
	 * @param snippetLength the portion of each article that should be considered for training (see ArticleCleaner).  
	 * @param datasetName a name that will help explain the set of articles and resulting model later.
	 * @param rc a cache in which relatedness measures will be saved so they aren't repeatedly calculated. Make this null if using extremely large training sets, so that caches will be reset from document to document, and won't grow too large.   
	 * @throws Exception 
	 */
	public void train(ArticleSet articles, SnippetLength snippetLength, String datasetName, RelatednessCache rc, String lang) throws Exception{

		dataset = decider.createNewDataset();
		
		ProgressTracker pn = new ProgressTracker(articles.size(), "training", NerdDisambiguator.class);
		for (Article art: articles) {
		
			train(art, snippetLength, rc, lang);	
			pn.update();
		}
		
		weightTrainingInstances();
		
		//training data is very likely to be skewed. So lets resample to even out class values
		//Resample resampleFilter = new Resample();
		//resampleFilter.setBiasToUniformClass(1);
		
		//decider.applyFilter(resampleFilter);
	}

	/**
	 * Saves the training data to an arff file, so that it can be used by Weka.
	 */
	public void saveTrainingData(File file) throws IOException, Exception {
		logger.info("saving training data");
		dataset.save(file);
	}
	
	/**
	 * Loads the training data from an arff file saved previously. 
	 */
	public void loadTrainingData(File file) throws Exception{
		logger.info("loading training data");
		dataset = decider.createNewDataset();
		dataset.load(file);
		
		weightTrainingInstances();
	}
	
	public void clearTrainingData() {
		dataset = null;
	}

	/**
	 * Saves the classifier to file, so that it can be reused.
	 * 
	 * @param file the file to save to. 
	 * @throws IOException if the file cannot be written to
	 * @throws Exception unless the disambiguator has been trained and a classifier has been built.
	 */
	public void saveClassifier(File file) throws IOException, Exception {
		logger.info("saving classifier");
		decider.save(file);
	}

	/**
	 * Loads a classifier that was previously saved. 
	 * 
	 * @param file the file in which 
	 * @throws IOException if there is a problem reading the file
	 * @throws Exception if the file does not contain a valid classifier. 
	 */
	public void loadClassifier(File file) throws IOException, Exception {
		logger.info("loading classifier");
		decider.load(file);
	}

	/**
	 * Builds a classifier of the given type using the previously built (or loaded) training data.
	 * 
	 * @param classifier a configured classifier, that is ready to be built.
	 * @throws Exception if there is no training data
	 */
	public void buildClassifier(Classifier classifier) throws Exception {
		logger.info("building classifier");
		decider.train(classifier, dataset);
	}
	
	public void buildDefaultClassifier() throws Exception {
		Classifier classifier = new Bagging();
		classifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W weka.classifiers.trees.J48 -- -U -M 2"));
		decider.train(classifier, dataset);
	}

	private void train(Article article, SnippetLength snippetLength, RelatednessCache rc, String lang) throws Exception {

		List<Label.Sense> unambigLabels = new ArrayList<Label.Sense>();
		Vector<TopicReference> ambigRefs = new Vector<TopicReference>();

		String content = cleaner.getMarkupLinksOnly(article, snippetLength);

		Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]"); 
		Matcher linkMatcher = linkPattern.matcher(content);

		// split references into ambiguous and unambiguous
		while (linkMatcher.find()) {			
			String linkText = content.substring(linkMatcher.start()+2, linkMatcher.end()-2);
			
			String labelText = linkText;
			String destText = linkText;

			int pos = linkText.lastIndexOf('|');
			if (pos>0) {
				destText = linkText.substring(0, pos);
				labelText = linkText.substring(pos+1);
			}

			//System.out.println(linkText + ", " + LabelText + ", " + destText);
			
			Label label = new Label(wikipedia.getEnvironment(), labelText, tp);
			Label.Sense[] senses = label.getSenses();
			Article dest = wikipedia.getArticleByTitle(destText);
			
			if (dest != null && senses.length >= 1) {
				TopicReference ref = new TopicReference(label, dest.getId(), new OffsetPosition(0, 0));

				if (senses.length == 1 || senses[0].getPriorProbability() >= (1-minSenseProbability))
					unambigLabels.add(senses[0]);
				else
					ambigRefs.add(ref);
			}
		}
		
		// use all terms as context
		//Context context = getContext(article, snippetLength, rc);
		
		//only use links
		//Context context = new Context(unambigLabels, rc, maxContextSize);

		// use all terms as context
		Relatedness relatedness = Relatedness.getInstance();
		//NerdContext context = relatedness.getContext(article, snippetLength);
		
		//only use links
		NerdContext context = new NerdContext(unambigLabels, null, lang);
		//NerdContext context = relatedness.getContext(unambigLabels, snippetLength);

		//resolve ambiguous links
		for (TopicReference ref: ambigRefs) {
			for (Sense sense:ref.getLabel().getSenses()) {

				if (sense.getPriorProbability() < minSenseProbability) break;

				Instance i = decider.getInstanceBuilder()
				.setAttribute(Attributes.commonness, sense.getPriorProbability())
				.setAttribute(Attributes.relatedness, context.getRelatednessTo(sense))
				.setAttribute(Attributes.contextQuality, (double)context.getQuality())
				.setClassAttribute(sense.getId() ==ref.getTopicId())
				.build();
				
				dataset.add(i);
			}
		}
	}

	/**
	 * Tests the disambiguator on a set of Wikipedia articles, to see how well it makes the same 
	 * decisions as the original article editors did. You need to train the disambiguator and build 
	 * a classifier before using this.
	 * 
	 * @param testSet the set of articles to use for testing. You should make sure these are reasonably tidy, and roughly representative (in size, link distribution, etc) as the documents you intend to process automatically.
	 * @param snippetLength the portion of each article that should be considered for testing (see ArticleCleaner).  
	 * @param rc a cache in which relatedness measures will be saved so they aren't repeatedly calculated. Make this null if using extremely large testing sets, so that caches will be reset from document to document, and won't grow too large.
	 * @return Result a result (including recall, precision, f-measure) of how well the classifier did.   
	 * @throws SQLException if there is a problem with the WikipediaMiner database.
	 * @throws Exception if there is a problem with the classifier
	 */
	public Result<Integer> test(ArticleSet testSet, Wikipedia wikipedia2, SnippetLength snippetLength, RelatednessCache rc, String lang) throws Exception{
		
		if (wikipedia2 == null)
			wikipedia2 = wikipedia;
		
		if (!decider.isReady()) 
			throw new WekaException("You must build (or load) classifier first.");

		Result<Integer> r = new Result<Integer>();
		
		double worstRecall = 1;
		double worstPrecision = 1;
		
		int articlesTested = 0;
		int perfectRecall = 0;
		int perfectPrecision = 0;
		
		ProgressTracker pn = new ProgressTracker(testSet.size(), "Testing", NerdDisambiguator.class);
		for (Article art: testSet) {
			
			articlesTested ++;
			
			Result<Integer> ir = test(art, snippetLength, rc, lang);
			
			if (ir.getRecall() ==1) perfectRecall++;
			if (ir.getPrecision() == 1) perfectPrecision++;
			
			worstRecall = Math.min(worstRecall, ir.getRecall());
			worstPrecision = Math.min(worstPrecision, ir.getPrecision());
			
			r.addIntermediateResult(ir);

			pn.update();
		}

		System.out.println("worstR:" + worstRecall + ", worstP:" + worstPrecision);
		System.out.println("tested:" + articlesTested + ", perfectR:" + perfectRecall + ", perfectP:" + perfectPrecision);
		
		return r;
	}

	private Result<Integer> test(Article article,  SnippetLength snippetLength, RelatednessCache rc, String lang) throws Exception {

		System.out.println(" - testing " + article);

		List<Label.Sense> unambigLabels = new ArrayList<Label.Sense>();
		Vector<TopicReference> ambigRefs = new Vector<TopicReference>();

		String content = cleaner.getMarkupLinksOnly(article, snippetLength);

		Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]"); 
		Matcher linkMatcher = linkPattern.matcher(content);

		Set<Integer> goldStandard = new HashSet<Integer>();
		Set<Integer> disambiguatedLinks = new HashSet<Integer>();

		while (linkMatcher.find()) {			
			String linkText = content.substring(linkMatcher.start()+2, linkMatcher.end()-2);

			String labelText = linkText;
			String destText = linkText;

			int pos = linkText.lastIndexOf('|');
			if (pos>0) {
				destText = linkText.substring(0, pos);
				labelText = linkText.substring(pos+1);
			}

			destText = Character.toUpperCase(destText.charAt(0)) + destText.substring(1);     // Get first char and capitalize

			Label label = new Label(wikipedia.getEnvironment(), labelText, tp);
			Label.Sense[] senses = label.getSenses();
			Article dest = wikipedia.getArticleByTitle(destText);

			if (senses.length > 0 && dest != null) {

				goldStandard.add(dest.getId());

				if (senses.length == 1 || senses[0].getPriorProbability() >= (1-minSenseProbability)) { 
					unambigLabels.add(senses[0]);
					disambiguatedLinks.add(dest.getId());
				} else {
					TopicReference ref = new TopicReference(label, dest.getId(), null);
					ambigRefs.add(ref);
				}
			}
		}

		// use all terms as context
		Relatedness relatedness = Relatedness.getInstance();
		//NerdContext context = relatedness.getContext(article, snippetLength);
		
		//only use links
		NerdContext context = new NerdContext(unambigLabels, null, lang);
		//NerdContext context = relatedness.getContext(unambigLabels, snippetLength);

		// resolve senses		
		for (TopicReference ref: ambigRefs) {

			TreeSet<Article> validSenses = new TreeSet<Article>();

			for (Sense sense:ref.getLabel().getSenses()) {

				if (sense.getPriorProbability() < minSenseProbability) break;

				double prob = getProbabilityOfSense(sense.getPriorProbability(), context.getRelatednessTo(sense), context); 

				if (prob>0.5) {
					Article art = new Article(wikipedia.getEnvironment(), sense.getId());
					art.setWeight(prob);
					validSenses.add(art);					
				}
				
				sensesConsidered ++;
			}

			//use most valid sense
			if (!validSenses.isEmpty()) 
				disambiguatedLinks.add(validSenses.first().getId());
		}

		Result<Integer> result = new Result<Integer>(disambiguatedLinks, goldStandard);

		System.out.println("   " + result);

		return result;
	}

	public int getSensesConsidered() {
		return sensesConsidered;
	}


}
