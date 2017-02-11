package com.scienceminer.nerd.disambiguation;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.disambiguation.NerdCandidate;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.data.Entity;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.service.NerdQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.model.*;
import org.wikipedia.miner.annotation.*;
import org.wikipedia.miner.annotation.ArticleCleaner.SnippetLength;
import org.wikipedia.miner.comparison.ArticleComparer;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.features.*;

import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;
import org.wikipedia.miner.model.Label.Sense;

import smile.validation.ConfusionMatrix;
import smile.validation.FMeasure;
import smile.validation.Precision;
import smile.validation.Recall;
import smile.data.*;
import smile.data.parser.*;
import smile.regression.*;
import com.thoughtworks.xstream.*;

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

	private static String MODEL_PATH = "data/models/ranker-long";
	/*private Classifier ranker_short = null;
	private Classifier ranker_long = null;

	private Classifier selector_short = null;
	private Classifier selector_long = null;*/

	// ranker model files
	//private static final String rankerShortFile = "data/models/ranker-short.model"; 
	//private static final String rankerLongFile = "data/models/ranker-long.model";
	
	// selector model files
	//private static final String selectorShortFile = "data/models/selector-short.model"; 
	//private static final String selectorLongFile = "data/models/selector-long.model";

	private Wikipedia wikipedia = null;
	private ArticleCleaner cleaner = null;
	private TextProcessor tp = null;
	private ArticleComparer comparer = null;

	private double minSenseProbability = 0.0; 
	private int maxLabelLength = 20; 
	private double minLinkProbability = 0.0;
	private int maxContextSize = -1;

	// regression model
	private RandomForest forest = null;

	// for serialization of the classifier
	private XStream xstream = null;
	private ArffParser arffParser = null;

	private String arffDataset = null;
	private AttributeDataset attributeDataset = null;
	//private enum Attributes {commonness, relatedness, contextQuality};
	
	//private Decider<Attributes, Boolean> decider = null;
	//private Dataset<Attributes, Boolean> dataset = null;
	
	//private int sensesConsidered = 0;

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
		
		/*decider = (Decider<Attributes, Boolean>) new DeciderBuilder<Attributes>("LinkDisambiguator", Attributes.class)
			.setDefaultAttributeTypeNumeric()
			.setClassAttributeTypeBoolean("isCorrectSense")
			.build();*/

		/*if (conf.getTopicDisambiguationModel() != null)
			loadClassifier(conf.getTopicDisambiguationModel());*/
		
		xstream = new XStream();
		arffParser = new ArffParser();
		MilneWittenFeatureVector feature = new MilneWittenFeatureVector();
		arffParser.setResponseIndex(feature.getNumFeatures()-1);

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

	//TODO: this should really be refactored as a separate filter
	@SuppressWarnings("unchecked")
	private void weightTrainingInstances() {
		double positiveInstances = 0;
		double negativeInstances = 0; 

		/*attributeDataset 

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
		}*/
	}

	public double getProbabilityOfSense(double commonness, double relatedness, NerdContext context) throws Exception {

		if (forest == null) {
			// load model
			File modelFile = new File(MODEL_PATH+"-"+wikipedia.getConfig().getLangCode()+".model"); 
			if (!modelFile.exists()) {
                logger.debug("Invalid model file for nerd disambiguator.");
			}
			String xml = FileUtils.readFileToString(modelFile, "UTF-8");
			forest = (RandomForest)xstream.fromXML(xml);
			logger.debug("Model for nerd disambiguation loaded.");
		}
		/*StringBuilder input = new StringBuilder();
		input.append(getHeader());
		int j = 0;
		for(String nameCandidate : nameCandidates) {
			 input.append(addFeatures(nameCandidate, new Double(sizes.get(j))) + "\t?\n");
			 j++;
		 }
		InputStream stream = new ByteArrayInputStream(input.toString().getBytes("UTF-8"));
		AttributeDataset authors = arffParser.parse(stream);
		Boolean result = new Boolean();
		for(int i=0; i<nameCandidates.size(); i++) {
			if (forest.predict(authors.toArray(new double[authors.size()][])[i]) == 0) 
				result.add(false);
			else 
				result.add(true);
		}*/

		MilneWittenFeatureVector feature = new MilneWittenFeatureVector();
		feature.prob_c = commonness;
		feature.relatedness = relatedness;
		feature.context_quality = context.getQuality();
		double[] features = feature.toMatrix();
		return forest.predict(features);

		/*Instance i = decider.getInstanceBuilder()
			.setAttribute(Attributes.commonness, commonness)
			.setAttribute(Attributes.relatedness, relatedness)
			.setAttribute(Attributes.contextQuality, context.getQuality())
			.build();
		
		//sensesConsidered++;
		
		return decider.getDecisionDistribution(i).get(true);*/
	}

	/**
	 * Saves the training data to an arff file, so that it can be used by Weka.
	 */
	public void saveTrainingData(File file) throws IOException, Exception {
		FileUtils.writeStringToFile(file, arffDataset);
		System.out.println("Training data saved under " + file.getPath());
	}
	
	/**
	 * Loads the training data from an arff file saved previously. 
	 */
	public void loadTrainingData(File file) throws Exception{
		//logger.info("loading training data");
		//dataset = decider.createNewDataset();
		//dataset.load(file);
		attributeDataset = arffParser.parse(new FileInputStream(file));
		System.out.println("Training data loaded from file " + file.getPath());
		weightTrainingInstances();
	}
	
	public void clearTrainingData() {
		//dataset = null;
		arffDataset = null;
		attributeDataset = null;
	}

	/**
	 * Saves the classifier to a file
	 * 
	 * @param file the file to save the classifier to 
	 * @throws IOException if the file cannot be written
	 * @throws Exception if the disambiguator has not been trained or has not been built
	 */
	public void saveModel(File file) throws IOException, Exception {
		logger.info("saving model");
		//decider.save(file);
		// save the model with XStream
		String xml = xstream.toXML(forest);
		File modelFile = new File(MODEL_PATH+"-"+wikipedia.getConfig().getLangCode()+".model"); 
		if (!modelFile.exists()) {
            logger.debug("Invalid file for saving author filtering model.");
		}
		FileUtils.writeStringToFile(modelFile, xml, "UTF-8");
		System.out.println("Model saved under " + file.getPath());
	}

	/**
	 * Loads a classifier from a file
	 * 
	 * @param file the file in which 
	 * @throws IOException if there is a problem reading the file
	 * @throws Exception if the file does not contain a valid classifier
	 */
	public void loadMoel(File file) throws IOException, Exception {
		logger.info("loading model");
		// load model
		File modelFile = new File(MODEL_PATH+"-"+wikipedia.getConfig().getLangCode()+".model"); 
		if (!modelFile.exists()) {
        	logger.debug("Model file for nerd disambiguator does not exist.");
        	throw new IOException();
		}
		String xml = FileUtils.readFileToString(modelFile, "UTF-8");
		forest = (RandomForest)xstream.fromXML(xml);
		logger.debug("Model for nerd disambiguation loaded.");
	}

	/**
	 * Build a classifier using loaded training data
	 * 
	 * @throws Exception if there is no training data
	 */
	public void trainModel() throws Exception {
		logger.info("building model");
		//decider.train(classifier, dataset);
		double[][] x = attributeDataset.toArray(new double[attributeDataset.size()][]);
		double[] y = attributeDataset.toArray(new double[attributeDataset.size()]);
		
		long start = System.currentTimeMillis();
		forest = new RandomForest(attributeDataset.attributes(), x, y, 200);
		
        System.out.println("NERD disambiguation model created in " + 
			(System.currentTimeMillis() - start) / (1000.00) + " seconds");
	}
	
	/*public void buildDefaultClassifier() throws Exception {
		Classifier classifier = new Bagging();
		classifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W weka.classifiers.trees.J48 -- -U -M 2"));
		decider.train(classifier, dataset);
	}*/

	/**
	 * Trains the disambiguator on a set of Wikipedia articles. This only builds up the training data. 
	 * You will still need to build a classifier in order to use the trained disambiguator. 
	 * 
	 * @param articles the set of articles to use for training. You should make sure these are reasonably tidy, and roughly representative (in size, link distribution, etc) as the documents you intend to process automatically.
	 * @param snippetLength the portion of each article that should be considered for training (see ArticleCleaner).  
	 * @param datasetName a name that will help explain the set of articles and resulting model later.
	 * @throws Exception 
	 */
	public void train(ArticleSet articles, SnippetLength snippetLength, String datasetName) throws Exception {
		StringBuilder arffBuilder = new StringBuilder();
		MilneWittenFeatureVector feat = new MilneWittenFeatureVector();
		arffBuilder.append(feat.getArffHeader()).append("\n");
		int nbArticle = 0;
		for (Article art: articles) {
			arffBuilder = train(art, snippetLength, arffBuilder);	
			nbArticle++;
System.out.println("nb article processed: " + nbArticle);
		}
		System.out.println(arffBuilder.toString());
		arffDataset = arffBuilder.toString();
		attributeDataset = arffParser.parse(IOUtils.toInputStream(arffDataset, "UTF-8"));
		weightTrainingInstances();
	}

	private StringBuilder train(Article article, SnippetLength snippetLength, StringBuilder arffBuilder) throws Exception {
		List<Label.Sense> unambigLabels = new ArrayList<Label.Sense>();
		Vector<TopicReference> ambigRefs = new Vector<TopicReference>();

		String content = cleaner.getMarkupLinksOnly(article, snippetLength);
		content = content.replace("''", "");
		StringBuilder contentText = new StringBuilder(); 
System.out.println(content);
		Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]"); 
		Matcher linkMatcher = linkPattern.matcher(content);

		// split references into ambiguous and unambiguous
		int head = 0;
		while (linkMatcher.find()) {			
			String linkText = content.substring(linkMatcher.start()+2, linkMatcher.end()-2);
			if (head != linkMatcher.start())
				contentText.append(content.substring(head, linkMatcher.start()));

			String labelText = linkText;
			String destText = linkText;

			int pos = linkText.lastIndexOf('|');
			if (pos>0) {
				destText = linkText.substring(0, pos);
				labelText = linkText.substring(pos+1);
			} else {
				// labelText and destText are the same, but we could have an anchor #
				int pos2 = linkText.indexOf('#');
				if (pos2 != -1) {
					destText = linkText.substring(0,pos2);
				} else {
					destText = linkText;
				}
				labelText = destText;
			}
			contentText.append(labelText);

			head = linkMatcher.end();
			
			Label label = new Label(wikipedia.getEnvironment(), labelText, tp);
			Label.Sense[] senses = label.getSenses();
			Article dest = wikipedia.getArticleByTitle(destText);
			
			if (dest != null && senses.length >= 1) {
				TopicReference ref = new TopicReference(label, 
						dest.getId(), 
						new OffsetPosition(contentText.length()-labelText.length(), contentText.length()));

				if (senses.length == 1 || senses[0].getPriorProbability() >= (1-minSenseProbability))
					unambigLabels.add(senses[0]);
				else {
					ambigRefs.add(ref);
System.out.println(linkText + ", " + labelText + ", " + 
	destText + " / " + ref.getOffsetStart() + " " + ref.getOffsetEnd());
				}
			}
		}
		contentText.append(content.substring(head));
		String contentString = contentText.toString();
System.out.println(contentString);
		
		// get candidates for this content
		NerdEngine nerdEngine = NerdEngine.getInstance();
		Relatedness relatedness = Relatedness.getInstance();

		// process the text
		ProcessText processText = ProcessText.getInstance();
		List<Entity> entities = new ArrayList<Entity>();
		String lang = wikipedia.getConfig().getLangCode();
		if (lang.equals("en") || lang.equals("fr")) {
			entities = processText.process(contentString, new Language(lang, 1.0));
		}
		List<Entity> entities2 = processText.processBrutal(contentString, lang);
		for(Entity entity : entities2) {
			// we add entities only if the mention is not already present
			if (!entities.contains(entity))
				entities.add(entity);
		}

		if (entities == null) 
			return arffBuilder;

		// disambiguate and solve entity mentions
		List<NerdEntity> disambiguatedEntities = new ArrayList<NerdEntity>();
		for (Entity entity : entities) {
			NerdEntity nerdEntity = new NerdEntity(entity);
			disambiguatedEntities.add(nerdEntity);
		}

		Map<NerdEntity, List<NerdCandidate>> candidates = 
			nerdEngine.generateCandidates(contentText.toString(), disambiguatedEntities, lang);
System.out.println("entities: " + candidates.size());
		// set the expected concept to the NerdEntity
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();

			/*for (NerdCandidate cand : cands) {
				System.out.println(cand.toString());
			}*/

			int start = entity.getOffsetStart();
			int end = entity.getOffsetEnd();
//System.out.println("entity: " + start + " / " + end + " - " + contentString.substring(start, end));
			for(TopicReference ref : ambigRefs) {
				int start_ref = ref.getOffsetStart();
				int end_ref = ref.getOffsetEnd();
				if ( (start_ref == start) && (end_ref == end) ) {
					entity.setWikipediaExternalRef(ref.getTopicId());
					break;
				} 
			}
		}

		// get context for this content
		NerdContext context = null;
		try {
			 context = relatedness.getContext(candidates, null, lang);
		} catch(Exception e) {
			e.printStackTrace();
		}

		int nbInstance = 0;
		// second pass for producing the disambiguation observations
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			int expectedId = entity.getWikipediaExternalRef();
			if (expectedId == -1)
				continue;
			if ((cands == null) || (cands.size() <= 1)) {
				// do not considerer unambiguous entities
				continue;
			}
			
			for(NerdCandidate candidate : cands) {
				try {
					double commonness = candidate.getWikiSense().getPriorProbability();
					double related = relatedness.getRelatednessTo(candidate, context, lang);
					double quality = (double)context.getQuality();
					
					MilneWittenFeatureVector feature = new MilneWittenFeatureVector();
					feature.prob_c = commonness;
					feature.relatedness = related;
					feature.context_quality = context.getQuality();
					feature.label = (expectedId == candidate.getWikipediaExternalRef()) ? 1.0 : 0.0;

					/*Instance i = decider.getInstanceBuilder()
						.setAttribute(Attributes.commonness, commonness)
						.setAttribute(Attributes.relatedness, related)
						.setAttribute(Attributes.contextQuality, quality)
						.setClassAttribute(expectedId == candidate.getWikipediaExternalRef())
						.build();*/
						
					//dataset.add(i);
					arffBuilder.append(feature.printVector()).append("\n");
					nbInstance++;
					System.out.println(nbInstance + " training instances");

					System.out.println("*"+candidate.getWikiSense().getTitle() + "* " + 
							candidate.getNerd_score() +  " " + 
							entity.toString());
					System.out.println("\t\t" + "commonness: " + commonness + 
						", relatedness: " + related + 
						", quality: " + quality);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
							}
			Collections.sort(cands);
		}

		System.out.println("Final Article: " + nbInstance + " training instances");

		// use all terms as context
		//Context context = getContext(article, snippetLength, rc);
		
		//only use links
		//Context context = new Context(unambigLabels, rc, maxContextSize);

		// use all terms as context
		//NerdContext context = relatedness.getContext(article, snippetLength);
		
		//only use links
		//NerdContext context = new NerdContext(unambigLabels, null, lang);
		//NerdContext context = relatedness.getContext(unambigLabels, snippetLength);

		//resolve ambiguous links
		/*for (TopicReference ref : ambigRefs) {
			for (Sense sense:ref.getLabel().getSenses()) {
				if (sense.getPriorProbability() < minSenseProbability) break;

				Instance i = decider.getInstanceBuilder()
				.setAttribute(Attributes.commonness, sense.getPriorProbability())
				.setAttribute(Attributes.relatedness, context.getRelatednessTo(sense))
				.setAttribute(Attributes.contextQuality, (double)context.getQuality())
				.setClassAttribute(sense.getId() == ref.getTopicId())
				.build();
				
				dataset.add(i);
			}
		}*/
		return arffBuilder;
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
	public Result<Integer> test(ArticleSet testSet, Wikipedia wikipedia2, SnippetLength snippetLength) throws Exception{
		if (wikipedia2 == null)
			wikipedia2 = wikipedia;
		
		/*if (!decider.isReady()) 
			throw new WekaException("You must build (or load) classifier first.");*/

		Result<Integer> r = new Result<Integer>();
		
		double worstRecall = 1;
		double worstPrecision = 1;
		
		int articlesTested = 0;
		int perfectRecall = 0;
		int perfectPrecision = 0;
		
		for (Article art: testSet) {			
			articlesTested ++;
			
			Result<Integer> ir = test(art, snippetLength);
			
			if (ir.getRecall() ==1) perfectRecall++;
			if (ir.getPrecision() == 1) perfectPrecision++;
			
			worstRecall = Math.min(worstRecall, ir.getRecall());
			worstPrecision = Math.min(worstPrecision, ir.getPrecision());
			
			r.addIntermediateResult(ir);
			System.out.println("articlesTested: " + articlesTested);
		}

		System.out.println("worstR:" + worstRecall + ", worstP:" + worstPrecision);
		System.out.println("tested:" + articlesTested + ", perfectR:" + perfectRecall + ", perfectP:" + perfectPrecision);
		
		return r;
	}

	private Result<Integer> test(Article article,  SnippetLength snippetLength) throws Exception {

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
		NerdContext context = new NerdContext(unambigLabels, null, wikipedia.getConfig().getLangCode());
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
				
				//sensesConsidered ++;
			}

			//use most valid sense
			if (!validSenses.isEmpty()) 
				disambiguatedLinks.add(validSenses.first().getId());
		}

		Result<Integer> result = new Result<Integer>(disambiguatedLinks, goldStandard);

		System.out.println("   " + result);

		return result;
	}

}
