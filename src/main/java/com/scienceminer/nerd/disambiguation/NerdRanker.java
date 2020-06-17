package com.scienceminer.nerd.disambiguation;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.util.regex.*;
import java.text.*;
import java.util.concurrent.*;  

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.disambiguation.NerdCandidate;
import com.scienceminer.nerd.utilities.NerdConfig;
import com.scienceminer.nerd.utilities.Utilities;
import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.evaluation.*;
import com.scienceminer.nerd.mention.*;
import com.scienceminer.nerd.embeddings.SimilarityScorer;
import com.scienceminer.nerd.disambiguation.NerdModel.PredictTask;

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.data.Entity;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.trainer.evaluation.LabelStat;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.UnicodeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.training.*;
import com.scienceminer.nerd.utilities.mediaWiki.MediaWikiParser;
import com.scienceminer.nerd.evaluation.*;

import com.scienceminer.nerd.kb.model.Label.Sense;
import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.features.*;

import smile.validation.ConfusionMatrix;
import smile.validation.FMeasure;
import smile.validation.Precision;
import smile.validation.Recall;
import smile.data.*;
import smile.data.parser.*;
import smile.regression.*;
import com.thoughtworks.xstream.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A machine learning model for ranking a list of ambiguous candidates for a given mention.
 */
public class NerdRanker extends NerdModel {
	/**
	 * The class Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(NerdRanker.class);

	// ranker model files
	private static String MODEL_PATH_LONG = "data/models/ranker-long";

	// selected feature set for this particular ranker
	private FeatureType featureType;

	private LowerKnowledgeBase wikipedia = null;

	static public int EMBEDDINGS_WINDOW_SIZE = 10; // size of word window to be considered when calculating
	// embeddings-based similiarity

	public NerdRanker(LowerKnowledgeBase wikipedia) {
		this.wikipedia = wikipedia;

		model = MLModel.GRADIENT_TREE_BOOST;
		//model = MLModel.RANDOM_FOREST;
		featureType = FeatureType.NERD;

		GenericRankerFeatureVector feature = getNewFeature();
		arffParser.setResponseIndex(feature.getNumFeatures()-1);
	}

	public GenericRankerFeatureVector getNewFeature() {
		GenericRankerFeatureVector feature = null;
		if (featureType == FeatureType.SIMPLE)
			feature = new SimpleRankerFeatureVector();
		else if (featureType == FeatureType.BASELINE)
			feature = new BaselineRankerFeatureVector();
		else if (featureType == FeatureType.EMBEDDINGS)
			feature = new EmbeddingsRankerFeatureVector();
		else if (featureType == FeatureType.MILNE_WITTEN)
			feature = new MilneWittenFeatureVector();
		else if (featureType == FeatureType.NERD)
			feature = new NerdRankerFeatureVector();
		else if (featureType == FeatureType.WIKIDATA)
			feature = new WikidataRankerFeatureVector();
		else if (featureType == FeatureType.MILNE_WITTEN_RELATEDNESS)
			feature = new MilneWittenRelatednessFeatureVector();
		return feature;
	}

	public double getProbability(double commonness, 
								 double relatedness, 
								 double quality, 
								 boolean bestCaseContext,
								 float embeddingsSimilarity,
								 String wikidataId,
								 String wikidataP31Id) throws Exception {
		// special cases with only one feature
		if (featureType == FeatureType.BASELINE) {
			// special case of baseline, we just need the prior conditional prob
			return commonness;
		}
		if (featureType == FeatureType.EMBEDDINGS) {
			// special case of embeddings only, we just need the embeddings similarity score
			return embeddingsSimilarity;
		}
		if (featureType == FeatureType.MILNE_WITTEN_RELATEDNESS) {
			// special case of embeddings only, we just need the embeddings similarity score
			return relatedness;
		}

		if (forest == null) {
			// load model
			File modelFile = new File(MODEL_PATH_LONG+"-"+wikipedia.getConfig().getLangCode()+".model"); 
			if (!modelFile.exists()) {
                logger.debug("Invalid model file for nerd ranker.");
			}
			InputStream xml = new FileInputStream(modelFile);

			if (model == MLModel.RANDOM_FOREST)
				forest = (RandomForest)xstream.fromXML(xml);
			else
				forest = (GradientTreeBoost)xstream.fromXML(xml);
			if (attributeDataset != null) 
				attributes = attributeDataset.attributes();
			else {
				StringBuilder arffBuilder = new StringBuilder();
				GenericRankerFeatureVector feature = getNewFeature();

				arffBuilder.append(feature.getArffHeader()).append("\n");
				arffBuilder.append(feature.printVector());
				String arff = arffBuilder.toString();
				attributeDataset = arffParser.parse(IOUtils.toInputStream(arff, StandardCharsets.UTF_8));
				attributes = attributeDataset.attributes();
				attributeDataset = null;
			}
			logger.info("Model for nerd ranker loaded: " + 
				MODEL_PATH_LONG+"-"+wikipedia.getConfig().getLangCode()+".model");
		}

		GenericRankerFeatureVector feature = getNewFeature();

		feature.prob_c = commonness;
		feature.relatedness = relatedness;
		feature.context_quality = quality; 
		//feature.dice_coef = dice_coef;
		feature.bestCaseContext = bestCaseContext;
		feature.embeddings_centroid_similarity = embeddingsSimilarity;
		feature.wikidata_id = wikidataId;
		feature.wikidata_P31_entity_id = wikidataP31Id;
		double[] features = feature.toVector(attributes);
		//smile.math.Math.setSeed(7);

		// we add some robustness when calling the prediction, with an Executor and timer
		// it appears that smile-ml on some cloud machine can randomly take minutes to predict 
		// a result that usually takes a few milliseconds. Reasons for this random super slowness
		// was not found.

		// -> note: we are finally not using it because it is expensive in term of number of used threads
		// for very large sequences, to be reviewed

		/*ExecutorService executor = Executors.newSingleThreadExecutor();
		PredictTask task = new PredictTask(forest, features);
		double score = -1.0;
		int counter = 0;
		while(score == -1.0) {
			Future<Double> future = executor.submit(task);
			try {
	    		score = future.get(50, TimeUnit.MILLISECONDS).doubleValue();
	    	} catch (TimeoutException ex) {
			   	// handle the timeout
			} catch (InterruptedException e) {
			   	// handle the interrupts
			} catch (ExecutionException e) {
			   	// handle other exceptions
			} finally {
			   	future.cancel(true); // may or may not desire this
			}
			if (counter == 5)
				score = 0.0;
			counter++;
		}*/

		double score = forest.predict(features);
		/*logger.debug("[Ranker] score: "+ score +
							", commonness: " + commonness +
							", relatedness: " + relatedness + 
							", context_quality: " + quality + 
							", context_quality: " + bestCaseContext + 
							", embeddingsSimilarity: " + embeddingsSimilarity);*/

		return score;
	}

	public void saveModel() throws Exception {
		logger.info("saving model");
		// save the model with XStream
		String xml = xstream.toXML(forest);
		File modelFile = new File(MODEL_PATH_LONG+"-"+wikipedia.getConfig().getLangCode()+".model"); 
		if (!modelFile.exists()) {
            logger.debug("Invalid file for saving author filtering model.");
		}
		FileUtils.writeStringToFile(modelFile, xml, StandardCharsets.UTF_8);
		System.out.println("Model saved under " + modelFile.getPath());
	}

	public void loadModel() throws IOException, Exception {
		logger.info("loading model");
		// load model
		File modelFile = new File(MODEL_PATH_LONG+"-"+wikipedia.getConfig().getLangCode()+".model"); 
		if (!modelFile.exists()) {
        	logger.debug("Model file for nerd ranker does not exist.");
        	throw new NerdResourceException("Model file for nerd ranker does not exist.");
		}
		String xml = FileUtils.readFileToString(modelFile, StandardCharsets.UTF_8);
		if (model == MLModel.RANDOM_FOREST)
			forest = (RandomForest)xstream.fromXML(xml);
		else
			forest = (GradientTreeBoost)xstream.fromXML(xml);
		logger.debug("Model for nerd ranker loaded.");
	}

	public void trainModel() throws Exception {
		// special cases, no need to train a model
		if (featureType == FeatureType.BASELINE || 
			featureType == FeatureType.EMBEDDINGS || 
			featureType == FeatureType.MILNE_WITTEN_RELATEDNESS) 
			return;

		if (attributeDataset == null) {
			logger.debug("Training data for nerd ranker has not been loaded or prepared");
			return;
			//throw new NerdResourceException("Training data for nerd ranker has not been loaded or prepared");
		}
		logger.info("building model");

		double[][] x = attributeDataset.toArray(new double[attributeDataset.size()][]);
		double[] y = attributeDataset.toArray(new double[attributeDataset.size()]);
		
		long start = System.currentTimeMillis();
		smile.math.Math.setSeed(7);
		if (model == MLModel.RANDOM_FOREST)
			forest = new RandomForest(attributeDataset.attributes(), x, y, 200);
		else {
			//nb trees: 200, maxNodes: 6, srinkage: 0.05, subsample: 0.5
			forest = new GradientTreeBoost(attributeDataset.attributes(), x, y, 
				GradientTreeBoost.Loss.LeastAbsoluteDeviation, 500, 6, 0.05, 0.5);
		}

        System.out.println("NERD ranker model created in " + 
			(System.currentTimeMillis() - start) / (1000.00) + " seconds");
	}

	public void train(ArticleTrainingSample articles) throws Exception {
		if (featureType == FeatureType.BASELINE || 
			featureType == FeatureType.EMBEDDINGS || 
			featureType == FeatureType.MILNE_WITTEN_RELATEDNESS) {
			// no model need to be trained
			return;
		}

		StringBuilder arffBuilder = new StringBuilder();

		GenericRankerFeatureVector feature = getNewFeature();

		arffBuilder.append(feature.getArffHeader()).append("\n");
		int nbArticle = 0;
		this.positives = 1;
		this.negatives = 0;
		if ( (articles.getSample() == null) || (articles.getSample().size() == 0) )
			return;
		for (Article article : articles.getSample()) {
			System.out.println("Training on " + (nbArticle+1) + "  / " + articles.getSample().size());
			if (article instanceof CorpusArticle)
				arffBuilder = trainCorpusArticle(article, arffBuilder);
			else
				arffBuilder = trainWikipediaArticle(article, arffBuilder);	
				
			nbArticle++;
		}
		arffDataset = arffBuilder.toString();
		attributeDataset = arffParser.parse(IOUtils.toInputStream(arffDataset, StandardCharsets.UTF_8));
	}

	private StringBuilder trainWikipediaArticle(Article article, StringBuilder arffBuilder) throws Exception {
		List<NerdEntity> refs = new ArrayList<NerdEntity>();
		String lang = wikipedia.getConfig().getLangCode();

		String content = MediaWikiParser.getInstance().
			toTextWithInternalLinksArticlesOnly(article.getFullWikiText(), lang);
		content = content.replace("''", "");
		StringBuilder contentText = new StringBuilder(); 

		Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]"); 
		Matcher linkMatcher = linkPattern.matcher(content);

		// gather reference gold values
		int head = 0;
		while (linkMatcher.find()) {			
			String link = content.substring(linkMatcher.start()+2, linkMatcher.end()-2);
			if (head != linkMatcher.start())
				contentText.append(content.substring(head, linkMatcher.start()));
			String labelText = link;
			String destText = link;

			int pos = link.lastIndexOf('|');
			if (pos > 0) {
				destText = link.substring(0, pos);
				// possible anchor #
				int pos2 = destText.indexOf('#');
				if (pos2 != -1) {
					destText = destText.substring(0,pos2);
				}
				labelText = link.substring(pos+1);
			} else {
				// labelText and destText are the same, but we could have an anchor #
				int pos2 = link.indexOf('#');
				if (pos2 != -1) {
					destText = link.substring(0,pos2);
				} else {
					destText = link;
				}
				labelText = destText;
			}
			contentText.append(labelText);

			head = linkMatcher.end();
			
			Label label = new Label(wikipedia.getEnvironment(), labelText);
			Label.Sense[] senses = label.getSenses();
			if (destText.length() > 1)
				destText = Character.toUpperCase(destText.charAt(0)) + destText.substring(1);
			else {
				// no article considered as single letter
				continue;
			}
			Article dest = wikipedia.getArticleByTitle(destText);
			if ((dest != null) && (senses.length > 1)) {
				NerdEntity ref = new NerdEntity();
				ref.setRawName(labelText);
				ref.setWikipediaExternalRef(dest.getId());
				ref.setOffsetStart(contentText.length()-labelText.length());
				ref.setOffsetEnd(contentText.length());
	
				refs.add(ref);
//System.out.println(link + ", " + labelText + ", " + destText + " / " + ref.getOffsetStart() + " " + ref.getOffsetEnd());
			}
		}
		contentText.append(content.substring(head));
		String contentString = contentText.toString();
		List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(contentString, new Language(lang, 1.0));
//System.out.println("Cleaned content: " + contentString);
		
		// get candidates for this content
		NerdEngine nerdEngine = NerdEngine.getInstance();
		Relatedness relatedness = Relatedness.getInstance();

		// process the text
		ProcessText processText = ProcessText.getInstance();
		List<Mention> entities = new ArrayList<Mention>();
		Language language = new Language(lang, 1.0);
		if (lang.equals("en") || lang.equals("fr")) {
			entities = processText.processNER(tokens, language);
		}
//System.out.println("number of NE found: " + entities.size());	
		List<Mention> entities2 = processText.processWikipedia(tokens, language);
//System.out.println("number of non-NE found: " + entities2.size());	
		for(Mention entity : entities2) {
			// we add entities only if the mention is not already present
			if (!entities.contains(entity))
				entities.add(entity);
		}

		if (entities == null) 
			return arffBuilder;

		// disambiguate and solve entity mentions
		List<NerdEntity> disambiguatedEntities = new ArrayList<NerdEntity>();
		for (Mention entity : entities) {
			NerdEntity nerdEntity = new NerdEntity(entity);
			disambiguatedEntities.add(nerdEntity);
		}
//System.out.println("total entities to disambiguate: " + disambiguatedEntities.size());	

		Map<NerdEntity, List<NerdCandidate>> candidates = 
			nerdEngine.generateCandidatesSimple(disambiguatedEntities, lang);
//System.out.println("total entities with candidates: " + candidates.size());
		
		// set the expected concept to the NerdEntity
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			//List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();

			/*for (NerdCandidate cand : cands) {
				System.out.println(cand.toString());
			}*/

			int start = entity.getOffsetStart();
			int end = entity.getOffsetEnd();
//System.out.println("entity: " + start + " / " + end + " - " + contentString.substring(start, end));
			for(NerdEntity ref : refs) {
				int start_ref = ref.getOffsetStart();
				int end_ref = ref.getOffsetEnd();
				if ( (start_ref == start) && (end_ref == end) ) {
					entity.setWikipediaExternalRef(ref.getWikipediaExternalRef());
					break;
				} 
			}
		}

		// get context for this content
//System.out.println("get context for this content");		
		NerdContext context = null;
		try {
			 context = relatedness.getContext(candidates, null, lang, false);
		} catch(Exception e) {
			e.printStackTrace();
		}

		double quality = (double)context.getQuality();
		int nbInstance = 0;
		// second pass for producing the disambiguation observations
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			int expectedId = entity.getWikipediaExternalRef();
			int nbCandidate = 0;
			if (expectedId == -1) {
				// we skip cases when no gold entity is present (nothing to rank against)
				continue;
			}
			if ((cands == null) || (cands.size() <= 1)) {
				// if no or only one candidate, nothing to rank and the example is not 
				// useful for training
				continue;
			}
			
			// get a window of layout tokens around without target tokens
			List<LayoutToken> subTokens = Utilities.getWindow(entity, tokens, NerdRanker.EMBEDDINGS_WINDOW_SIZE, lang);

			for(NerdCandidate candidate : cands) {
				try {
					nbCandidate++;
//System.out.println(nbCandidate + " candidate / " + cands.size());
					Label.Sense sense = candidate.getWikiSense();
					if (sense == null)
						continue;

					double commonness = sense.getPriorProbability();
//System.out.println("commonness: " + commonness);

					double related = relatedness.getRelatednessTo(candidate, context, lang);
//System.out.println("relatedness: " + related);

					boolean bestCaseContext = true;
					// actual label used
					Label bestLabel = candidate.getLabel();
					if (!entity.getNormalisedName().equals(bestLabel.getText())) {
						bestCaseContext = false;
					}

					float embeddingsSimilarity = 0.0F;
					// computed only if needed because it takes time
					GenericRankerFeatureVector feature = getNewFeature();
					if (feature.Add_embeddings_centroid_similarity) {
						//embeddingsSimilarity = SimilarityScorer.getInstance().getLRScore(candidate, subTokens, lang);
						embeddingsSimilarity = SimilarityScorer.getInstance().getCentroidScore(candidate, subTokens, lang);
					}

					feature.prob_c = commonness;
					feature.relatedness = related;
					feature.context_quality = quality;
					feature.bestCaseContext = bestCaseContext;
					feature.embeddings_centroid_similarity = embeddingsSimilarity;
					if (candidate.getWikidataId() != null)	
						feature.wikidata_id = candidate.getWikidataId();
					else
						feature.wikidata_id = "Q0"; // undefined entity

					if (candidate.getWikidataP31Id() != null)
						feature.wikidata_P31_entity_id = candidate.getWikidataP31Id();
					else
						feature.wikidata_P31_entity_id = "Q0"; // undefined entity

					feature.label = (expectedId == candidate.getWikipediaExternalRef()) ? 1.0 : 0.0;

					// addition of the example is constrained by the sampling ratio
					if ( ((feature.label == 0.0) && ((double)this.negatives / this.positives < sampling)) ||
						 ((feature.label == 1.0) && ((double)this.negatives / this.positives >= sampling)) ) {
						arffBuilder.append(feature.printVector()).append("\n");
						nbInstance++;
						if (feature.label == 0.0)
							this.negatives++;
						else
							this.positives++;
					}
					
/*System.out.println("*"+candidate.getWikiSense().getTitle() + "* " + 
							entity.toString());
					System.out.println("\t\t" + "commonness: " + commonness + 
						", relatedness: " + related + 
						", quality: " + quality);*/
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			Collections.sort(cands);
		}

		System.out.println("article contribution: " + nbInstance + " training instances");
		return arffBuilder;
	}

	private StringBuilder trainCorpusArticle(Article article, StringBuilder arffBuilder) throws Exception {
		String docPath = ((CorpusArticle)article).getPath();
		String corpus = ((CorpusArticle)article).getCorpus();
		File docFile = new File(docPath);
		String lang = wikipedia.getConfig().getLangCode();
System.out.println(docPath);
		if (!docFile.exists()) {
			System.out.println("File invalid: " + docPath);
			return arffBuilder;
		}

		String docContent = null;
		try {
			docContent = FileUtils.readFileToString(docFile, StandardCharsets.UTF_8);
		} catch(Exception e) {
			e.printStackTrace();
		}

		if (docContent == null || docContent.length() == 0) {
			System.out.println("Document is empty: " + docPath);
			return arffBuilder;
		}

		// if the corpus is AIDA, we need to ignore the two first lines and "massage" 
		// a bit the text
		if (corpus.startsWith("aida")) {
			int ind = docContent.indexOf("\n");
			ind = docContent.indexOf("\n", ind +1);
			docContent = docContent.substring(ind+1);
			docContent = docContent.replace("\n\n", "\n");
			docContent = docContent.replace("\n", "\n\n");
			docContent = docContent.replace("&amp;", "&");
		}

		docContent = UnicodeUtil.normaliseText(docContent);
//System.out.println(docContent.length() + " characters");

		// xml annotation file
		String corpusPath = "data/corpus/corpus-long/" + corpus + "/";
		String corpusRefPath = corpusPath + corpus + ".xml";
		File corpusRefFile = new File(corpusRefPath);

		if (!corpusRefFile.exists()) {
			System.out.println("The reference file for corpus " + corpus + " is not found: " + corpusRefFile.getPath());
			return null;
		}

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		org.w3c.dom.Document dom = null;
		try {
			//Using factory get an instance of document builder
			db = dbf.newDocumentBuilder();
			//parse using builder to get DOM representation
			dom = db.parse(corpusRefFile);
		} catch(Exception e) {
			e.printStackTrace();
		}

		//get the root element and the document node
		Element root = dom.getDocumentElement();
		NodeList docs = root.getElementsByTagName("document");
		if (docs == null || docs.getLength() <= 0) {
			logger.error("the list documents for this corpus is empty");
			return null;
		}

		Set<Integer> referenceDisamb = new HashSet<Integer>();
		Set<Integer> producedDisamb = new HashSet<Integer>();
		List<NerdEntity> referenceEntities = new ArrayList<NerdEntity>();

		// find the annotations for the training file
		for (int i = 0; i < docs.getLength(); i++) {
			//get the annotations of each document.
			Element docElement = (Element)docs.item(i);
			String docName = docElement.getAttribute("docName");

			if (!docName.equals(docFile.getName()))
				continue;
//System.out.println("found annotations!");
			// get the annotations, mentions + entity
			NodeList annotations = docElement.getElementsByTagName("annotation");
			if (annotations == null || annotations.getLength() <= 0)
				continue;
//System.out.println(annotations.getLength() + " annotations in total");
			for (int j = 0; j < annotations.getLength(); j++) {
				Element element = (Element) annotations.item(j);

				String wikiName = null;
				NodeList nl = element.getElementsByTagName("wikiName");
				if (nl != null && nl.getLength() > 0) {
					Element elem = (Element) nl.item(0);
					if (elem.hasChildNodes())
						wikiName = elem.getTextContent();
				}

				String mentionName = null;
				nl = element.getElementsByTagName("mention");
				if (nl != null && nl.getLength() > 0) {
					Element elem = (Element) nl.item(0);
					if (element.hasChildNodes())
						mentionName = elem.getTextContent();
				}

				if (wikiName != null && (wikiName.equals("NIL") || wikiName.isEmpty()))
					wikiName = null;
				
				// ignore mentions with no true entity
				if (wikiName == null)
					continue;
				if (mentionName == null || mentionName.isEmpty()) {
					continue;
				}

				int pageId = -1;
				Article theArticle = wikipedia.getArticleByTitle(wikiName);
				if (theArticle == null) {
					System.out.println(docName + ": Invalid article name - article not found in Wikipedia: " + wikiName);
					continue;
				} else 
					pageId = theArticle.getId();

				// offset info
				int start = -1;
				int end = -1;				
				nl = element.getElementsByTagName("offset");
				if (nl != null && nl.getLength() > 0) {
					Element elem = (Element) nl.item(0);
					if (elem.hasChildNodes()) {
						String startString = elem.getFirstChild().getNodeValue();
						try {
							start = Integer.parseInt(startString);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}

				nl = element.getElementsByTagName("length");
				if (nl != null && nl.getLength() > 0) {
					Element elem = (Element) nl.item(0);
					if (elem.hasChildNodes()) {
						String lengthString = elem.getFirstChild().getNodeValue();
						try {
							end = start + Integer.parseInt(lengthString);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}

				if (!mentionName.equals(docContent.substring(start, end))) {
					System.out.println(docPath + ": " + mentionName + " =/= " + docContent.substring(start, end));
				}

				// create expected entity
				NerdEntity ref = new NerdEntity();
				ref.setRawName(mentionName);
				ref.setWikipediaExternalRef(pageId);
				ref.setOffsetStart(start);
				ref.setOffsetEnd(end);
//System.out.println("reference entity: " + start + " / " + end + " - " + docContent.substring(start, end) + " - " + pageId);
				referenceEntities.add(ref);
				referenceDisamb.add(new Integer(pageId));
			}
		}

		List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(docContent, new Language(lang, 1.0));
		
		// get candidates for this content
		NerdEngine nerdEngine = NerdEngine.getInstance();
		Relatedness relatedness = Relatedness.getInstance();

		// process the text
		ProcessText processText = ProcessText.getInstance();
		List<Mention> entities = new ArrayList<Mention>();
		Language language = new Language(lang, 1.0);
		if (lang.equals("en") || lang.equals("fr")) {
			entities = processText.processNER(tokens, language);
		}
//System.out.println("number of NE found: " + entities.size());	
		List<Mention> entities2 = processText.processWikipedia(tokens, language);
//System.out.println("number of non-NE found: " + entities2.size());	
		for(Mention entity : entities2) {
			// we add entities only if the mention is not already present
			if (!entities.contains(entity))
				entities.add(entity);
		}

		if (entities == null) 
			return arffBuilder;

		// disambiguate and solve entity mentions
		List<NerdEntity> disambiguatedEntities = new ArrayList<NerdEntity>();
		for (Mention entity : entities) {
			NerdEntity nerdEntity = new NerdEntity(entity);
			disambiguatedEntities.add(nerdEntity);
		}
//System.out.println("total entities to disambiguate: " + disambiguatedEntities.size());	

		Map<NerdEntity, List<NerdCandidate>> candidates = 
			nerdEngine.generateCandidatesSimple(disambiguatedEntities, lang);
//System.out.println("total entities with candidates: " + candidates.size());
		
		// set the expected concept to the NerdEntity
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			//List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();

			/*for (NerdCandidate cand : cands) {
				System.out.println(cand.toString());
			}*/

			int start = entity.getOffsetStart();
			int end = entity.getOffsetEnd();

			for(NerdEntity ref : referenceEntities) {
				int start_ref = ref.getOffsetStart();
				int end_ref = ref.getOffsetEnd();
				if ( (start_ref == start) && (end_ref == end) ) {
//System.out.println("entity: " + start + " / " + end + " - " + docContent.substring(start, end));
					entity.setWikipediaExternalRef(ref.getWikipediaExternalRef());
					break;
				} 
			}
		}

		// get context for this content
//System.out.println("get context for this content");		
		NerdContext context = null;
		try {
			 context = relatedness.getContext(candidates, null, lang, false);
		} catch(Exception e) {
			e.printStackTrace();
		}

		double quality = (double)context.getQuality();
		int nbInstance = 0;
		// second pass for producing the disambiguation observations
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			int expectedId = entity.getWikipediaExternalRef();
			int nbCandidate = 0;
			if (expectedId == -1) {
				// we skip cases when no gold entity is present (nothing to rank against)
				continue;
			}
			if ((cands == null) || (cands.size() <= 1)) {
				// if no or only one candidate, nothing to rank and the example is not 
				// useful for training
				continue;
			}
			
			// get a window of layout tokens around without target tokens
			List<LayoutToken> subTokens = Utilities.getWindow(entity, tokens, NerdRanker.EMBEDDINGS_WINDOW_SIZE, lang);

			for(NerdCandidate candidate : cands) {
				try {
					nbCandidate++;
//System.out.println(nbCandidate + " candidate / " + cands.size());
					Label.Sense sense = candidate.getWikiSense();
					if (sense == null)
						continue;

					double commonness = sense.getPriorProbability();
//System.out.println("commonness: " + commonness);

					double related = relatedness.getRelatednessTo(candidate, context, lang);
//System.out.println("relatedness: " + related);

					boolean bestCaseContext = true;
					// actual label used
					Label bestLabel = candidate.getLabel();
					if (!entity.getNormalisedName().equals(bestLabel.getText())) {
						bestCaseContext = false;
					}

					float embeddingsSimilarity = 0.0F;
					// computed only if needed because it takes time
					GenericRankerFeatureVector feature = getNewFeature();
					if (feature.Add_embeddings_centroid_similarity) {
						//embeddingsSimilarity = SimilarityScorer.getInstance().getLRScore(candidate, subTokens, lang);
						embeddingsSimilarity = SimilarityScorer.getInstance().getCentroidScore(candidate, subTokens, lang);
					}

					feature.prob_c = commonness;
					feature.relatedness = related;
					feature.context_quality = quality;
					feature.bestCaseContext = bestCaseContext;
					feature.embeddings_centroid_similarity = embeddingsSimilarity;
					if (candidate.getWikidataId() != null)	
						feature.wikidata_id = candidate.getWikidataId();
					else
						feature.wikidata_id = "Q0"; // undefined entity

					if (candidate.getWikidataP31Id() != null)
						feature.wikidata_P31_entity_id = candidate.getWikidataP31Id();
					else
						feature.wikidata_P31_entity_id = "Q0"; // undefined entity

					feature.label = (expectedId == candidate.getWikipediaExternalRef()) ? 1.0 : 0.0;

					// addition of the example is constrained by the sampling ratio
					if ( ((feature.label == 0.0) && ((double)this.negatives / this.positives < sampling)) ||
						 ((feature.label == 1.0) && ((double)this.negatives / this.positives >= sampling)) ) {
						arffBuilder.append(feature.printVector()).append("\n");
						nbInstance++;
						if (feature.label == 0.0)
							this.negatives++;
						else
							this.positives++;
					}
/*System.out.println("*"+candidate.getWikiSense().getTitle() + "* " + 
							entity.toString());
					System.out.println("\t\t" + "commonness: " + commonness + 
						", relatedness: " + related + 
						", quality: " + quality);*/
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			Collections.sort(cands);
		}

		System.out.println("article contribution: " + nbInstance + " training instances");
		return arffBuilder;
	}

	public LabelStat evaluate(ArticleTrainingSample testSet) throws Exception {	
		List<LabelStat> stats = new ArrayList<LabelStat>();
		int n = 0;
		for (Article article : testSet.getSample()) {
			System.out.println("Evaluating on article " + (n+1) + " / " + testSet.getSample().size());
			if (article instanceof CorpusArticle)
				stats.add(evaluateCorpusArticle(article));
			else
				stats.add(evaluateWikipediaArticle(article));
			n++;
		}
		return EvaluationUtil.evaluate(testSet, stats);
	}

	private LabelStat evaluateWikipediaArticle(Article article) throws Exception {
		String lang = wikipedia.getConfig().getLangCode();
		String content = MediaWikiParser.getInstance()
			.toTextWithInternalLinksArticlesOnly(article.getFullWikiText(), lang);

		Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]"); 
		Matcher linkMatcher = linkPattern.matcher(content);

		Set<Integer> referenceDisamb = new HashSet<Integer>();
		Set<Integer> producedDisamb = new HashSet<Integer>();
		List<NerdEntity> referenceEntities = new ArrayList<NerdEntity>();
		List<String> labelTexts = new ArrayList<String>();
		int head = 0;
		StringBuilder contentText = new StringBuilder(); 
		while (linkMatcher.find()) {			
			String link = content.substring(linkMatcher.start()+2, linkMatcher.end()-2);
			if (head != linkMatcher.start())
				contentText.append(content.substring(head, linkMatcher.start()));
			String labelText = link;
			String destText = link;

			int pos = link.lastIndexOf('|');
			if (pos > 0) {
				destText = link.substring(0, pos);
				// possible anchor #
				int pos2 = destText.indexOf('#');
				if (pos2 != -1) {
					destText = destText.substring(0,pos2);
				}
				labelText = link.substring(pos+1);
			} else {
				// labelText and destText are the same, but we could have an anchor #
				int pos2 = link.indexOf('#');
				if (pos2 != -1) {
					destText = link.substring(0,pos2);
				} else {
					destText = link;
				}
				labelText = destText;
			}
			contentText.append(labelText);

			head = linkMatcher.end();
			
			if (destText.length() > 1)
				destText = Character.toUpperCase(destText.charAt(0)) + destText.substring(1);
			else {
				// no article considered as single character
				continue;
			}
			Article dest = wikipedia.getArticleByTitle(destText);
			if ((dest != null)) {// && (senses.length > 0)) {
				NerdEntity ref = new NerdEntity();
				ref.setRawName(labelText);
				ref.setWikipediaExternalRef(dest.getId());
				ref.setOffsetStart(contentText.length()-labelText.length());
				ref.setOffsetEnd(contentText.length());

				referenceDisamb.add(dest.getId());
				referenceEntities.add(ref);
			}
		}

		contentText.append(content.substring(head));
		String contentString = contentText.toString();
		contentString = UnicodeUtil.normaliseText(contentString);

		List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(contentString, new Language(lang, 1.0));
		// be sure to have the entities to be ranked
		List<Mention> nerEntities = new ArrayList<Mention>();
		for(NerdEntity refEntity : referenceEntities) {
			Mention localEntity = new Mention(refEntity.getRawName());
			localEntity.setOffsetStart(refEntity.getOffsetStart());
			localEntity.setOffsetEnd(refEntity.getOffsetEnd());
			nerEntities.add(localEntity);
		}
		List<NerdEntity> entities = new ArrayList<NerdEntity>();
		for (Mention entity : nerEntities) {
			NerdEntity theEntity = new NerdEntity(entity);
			entities.add(theEntity);
		}
		
		// process the text for building actual context for evaluation
		ProcessText processText = ProcessText.getInstance();
		nerEntities = new ArrayList<Mention>();
		Language language = new Language(lang, 1.0);
		if (lang.equals("en") || lang.equals("fr")) {
			nerEntities = processText.processNER(tokens, language);
		}
		for(Mention entity : nerEntities) {
			// we add entities only if the mention is not already present
			NerdEntity theEntity = new NerdEntity(entity);
			if (!entities.contains(theEntity))
				entities.add(theEntity);
		}
		//System.out.println("number of NE found: " + entities.size());	
		// add non NE terms
		List<Mention> entities2 = processText.processWikipedia(tokens, language);
		//System.out.println("number of non-NE found: " + entities2.size());
		for(Mention entity : entities2) {
			// we add entities only if the mention is not already present
			NerdEntity theEntity = new NerdEntity(entity);
			if (!entities.contains(theEntity))
				entities.add(theEntity);
		}

		NerdEngine engine = NerdEngine.getInstance();
		Map<NerdEntity, List<NerdCandidate>> candidates = 
			engine.generateCandidatesSimple(entities, wikipedia.getConfig().getLangCode());	
		engine.rank(candidates, wikipedia.getConfig().getLangCode(), null, false, tokens);
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			if (cands.size() > 0) {
				// check that we have a reference result for the same chunck
				int start = entity.getOffsetStart();
				int end = entity.getOffsetEnd(); 
				boolean found = false;
				for(NerdEntity refEntity : referenceEntities) {
					int startRef = refEntity.getOffsetStart();
					int endRef = refEntity.getOffsetEnd(); 
					if ((start == startRef) && (end == endRef)) {
						found = true;
						//producedDisamb.add(new Integer(refEntity.getWikipediaExternalRef()));
						break;
					}
				}
				if (found) {
					producedDisamb.add(new Integer(cands.get(0).getWikipediaExternalRef()));
				}
			}
		}
 
		LabelStat stats = new LabelStat();
		stats.setObserved(producedDisamb.size());
		for(Integer index : producedDisamb) {
			if (!referenceDisamb.contains(index)) {
				stats.incrementFalsePositive();
			}
		}

		stats.setExpected(referenceDisamb.size());
		for(Integer index : referenceDisamb) {
			if (!producedDisamb.contains(index)) {
				stats.incrementFalseNegative();
			}
		}

		return stats;
	}

	private LabelStat evaluateCorpusArticle(Article article) throws Exception {
		LabelStat stats = new LabelStat();

		return stats;
	}

}
