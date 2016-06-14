package com.scienceminer.nerd.erd;

import java.util.*;
import java.io.*;

import com.scienceminer.nerd.disambiguation.*;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.lexicon.Lexicon;
import com.scienceminer.nerd.exceptions.*;
import org.grobid.core.data.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Category;

import weka.core.*;
import weka.classifiers.*;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.trees.J48;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.meta.MultiBoostAB;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.RegressionByDiscretization;

/**
 * @author Patrice Lopez
 * 
 */
public class ErdTrainer {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ErdTrainer.class);

	private static volatile ErdTrainer instance = null;
	
	// training data file
	private static final String trainingDataRankerShortFile = "data/erd/models/entities-ranker-short.train.arff";
	private static final String trainingDataRankerLongFile = "data/erd/models/entities-ranker-long.train.arff";
	
	private static final String trainingDataSelectorShortFile = "data/erd/models/entities-selector-short.train.arff";
	private static final String trainingDataSelectorLongFile = "data/erd/models/entities-selector-long.train.arff";
	
	// model file
	private static final String rankerShortFile = "data/erd/models/ranker-short.model"; 
	private static final String rankerLongFile = "data/erd/models/ranker-long.model"; 
	
	private static final String selectorShortFile = "data/erd/models/selector-short.model"; 
	private static final String selectorLongFile = "data/erd/models/selector-long.model"; 
	
	private Lexicon lexicon = null;
	
	public static ErdTrainer getInstance() throws Exception {
	    if (instance == null) {
			getNewInstance();	        
	    }
	    return instance;
	}

	/**
	 * Creates a new instance.
	 */
	private static synchronized void getNewInstance() throws Exception {
		LOGGER.debug("Get new instance of ErdTrainer");		
		instance = new ErdTrainer();
	}

	/**
	 * Hidden constructor
	 */
	private ErdTrainer() throws Exception {		
		try {
			lexicon = Lexicon.getInstance();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *  Create the ARFF file given a set of ERD training files
	 */
	public void createTrainingRankerShortARFFFile(String dataset) {
		// we read the training file and for each entity candidate we create a feature vector with the 
		// expected entity status - if present
		List<String> pathQueries = new ArrayList<String>();
		List<String> pathAnnotations = new ArrayList<String>();
		if (dataset.equals("yahooL24")) {
			pathQueries.add("data/erd2014/corpus-short/yahoo/Webscope_L24/L24_queries.txt");
			pathAnnotations.add("data/erd2014/corpus-short/yahoo/Webscope_L24/L24_annotations.txt");
		}
		else if (dataset.equals("trec_web")) {
			pathQueries.add("data/erd2014/corpus-short/web-track/trec-web.queries.txt");
			pathAnnotations.add("data/erd2014/corpus-short/web-track/trec-web.annotations.txt");
		}
		else if (dataset.equals("trec_beta")) {
			pathQueries.add("data/erd2014/corpus-short/Trec_beta.query.txt");
			pathAnnotations.add("data/erd2014/corpus-short/Trec_beta.annotation.txt");
		}
		else if (dataset.equals("trec_million")) {
			pathQueries.add("data/erd2014/corpus-short/million-query/trec-million.queries.txt");
			pathAnnotations.add("data/erd2014/corpus-short/million-query/trec-million.annotations.txt");
		}
		else if (dataset.equals("all")) {
			pathQueries.add("data/erd2014/corpus-short/yahoo/Webscope_L24/L24_queries.txt");
			pathAnnotations.add("data/erd2014/corpus-short/yahoo/Webscope_L24/L24_annotations.txt");
			//pathQueries.add("data/erd2014/corpus-short/web-track/trec-web.queries.txt");
			//pathAnnotations.add("data/erd2014/corpus-short/web-track/trec-web.annotations.txt");
			//pathQueries.add("data/erd2014/corpus-short/million-query/trec-million.queries.txt");
			//pathAnnotations.add("data/erd2014/corpus-short/million-query/trec-million.annotations.txt");
			pathQueries.add("data/erd2014/corpus-short/Trec_beta.query.txt");
			pathAnnotations.add("data/erd2014/corpus-short/Trec_beta.annotation.txt");
		}
		else if (dataset.equals("all_not_beta")) {
			pathQueries.add("data/erd2014/corpus-short/yahoo/Webscope_L24/L24_queries.txt");
			pathAnnotations.add("data/erd2014/corpus-short/yahoo/Webscope_L24/L24_annotations.txt");
			pathQueries.add("data/erd2014/corpus-short/web-track/trec-web.queries.txt");
			pathAnnotations.add("data/erd2014/corpus-short/web-track/trec-web.annotations.txt");
			pathQueries.add("data/erd2014/corpus-short/million-query/trec-million.queries.txt");
			pathAnnotations.add("data/erd2014/corpus-short/million-query/trec-million.annotations.txt");
		}
		else {
			throw new NerdResourceException("The dataset does not exist.");
		}
		
		List<String> queries = new ArrayList<String>();
		for(String path : pathQueries) {
			BufferedReader dis = null;
			try {
				dis = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
	            String l = null;
	            while ((l = dis.readLine()) != null) {
					if (l.length() == 0) {
						continue;
					}
					queries.add(l);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			finally {
				try {
					dis.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		Map<String,List<ErdAnnotationShort>> annotations = new TreeMap<String,List<ErdAnnotationShort>>();
		for(String path : pathAnnotations) {
			BufferedReader dis = null;
			try {
				dis = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
	            String l = null;
	            while ((l = dis.readLine()) != null) {
					if (l.length() == 0) {
						continue;
					}
					String[] tokens = l.split("\t");

					if (tokens.length != 5) {
						continue;
					}
					String textId = tokens[0];
					// second token is to be ignored
					String freebaseId = tokens[2];
					String mention = tokens[3];

					ErdAnnotationShort a = new ErdAnnotationShort();
					a.setQid(textId);
					a.setInterpretationSet(0);
					a.setPrimaryId(freebaseId);
					a.setMentionText(mention);
					a.setScore(1);				

					List<ErdAnnotationShort> references = annotations.get(textId);
					if (references == null) {
						references = new ArrayList<ErdAnnotationShort>();
					}
					references.add(a);
					annotations.put(textId, references);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			finally {
				try {
					dis.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}	
		}
		
		NerdEngine annotator = null;
		try {
		 	annotator = NerdEngine.getInstance();
		}
		catch(Exception e) {
			throw new NerdException("The NerdDisambiguator cannot be instanciated.");
		}
		
		ProcessText processText = null;
		try {
			processText = ProcessText.getInstance();
		}
		catch(Exception e) {
			throw new NerdException("ProcessText cannot be instanciated.");
		}

		Relatedness erdRelatedness = null;
		try {
			erdRelatedness = Relatedness.getInstance();
		}
		catch(Exception e) {
			throw new NerdException("The ERD relatedness class cannot be instanciated.");
		}

		// output file for writing the ARFF training data
		Writer arffWriter = null;
		try {
			OutputStream arffOS = null;
	        if (trainingDataRankerShortFile != null) {
	            arffOS = new FileOutputStream(trainingDataRankerShortFile);
	            arffWriter = new OutputStreamWriter(arffOS, "UTF8");
				FeatureRankerShortVector features = new FeatureRankerShortVector();
				features.writeHeader(arffWriter);
	        }
		}
		catch(Exception e) {
			throw new NerdResourceException("Error when opening the ARFF training file: " 
				+ trainingDataSelectorShortFile, e);
		}
		
		// we have all the queries and annotations for the specified dataset, we can generate the training data
		for(String query : queries) {
			// id of the query
			String[] tokens = query.split("\t");

			if (tokens.length != 2) {
				continue;
			}
			String textId = tokens[0];
			String text = tokens[1].toLowerCase(); // we lowercase all queries to fit ERD queries
			
			// generate candidates
			//List<NerdCandidate> candidates = annotator.generateCandidates(text, true);
			List<Entity> entities = processText.processBrutal(text, "en");
			List<NerdEntity> disambiguatedEntities = new ArrayList<NerdEntity>();
			for (Entity entity : entities) {
				NerdEntity nerdEntity = new NerdEntity(entity);
				disambiguatedEntities.add(nerdEntity);
			}

			Map<NerdEntity, List<NerdCandidate>> candidateMap = annotator.generateCandidates(text, disambiguatedEntities, "en");
			for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidateMap.entrySet()) {
				// we retrieve the annotations
				List<NerdCandidate> candidates = entry.getValue();
				NerdEntity toDisambiguate = entry.getKey();
				List<ErdAnnotationShort> references = annotations.get(textId);
				if ( (candidates != null) && (candidates.size() > 0)) {
					for(NerdCandidate candidate : candidates) {
						Vector<Article> context = null; //erdRelatedness.collectContextTerms(candidates, candidate);
						
						if ( (candidate.getEntity() == null) || (candidate.getEntity().getRawName() == null) ) {
							throw new NerdException("The mention text of the entity candidate is null.");
						}
						FeatureRankerShortVector features = new FeatureRankerShortVector();
						if (features.Add_prob_c) {
							features.prob_c = candidate.getProb_c();
						}
						if (features.Add_prob_i) {
							features.prob_i = candidate.getProb_i();
						}
						if (features.Add_frequencyStrict) {
							features.frequencyStrict = candidate.getFreq();
						}
						if (features.Add_termLength) {							
							StringTokenizer st = new StringTokenizer(candidate.getEntity().getRawName(), " -,");
							features.termLength = st.countTokens();
						}
						if (features.Add_inDictionary) {
							boolean inDict = false;				
							if (lexicon.inDictionary(candidate.getEntity().getRawName())) {
								inDict = true;
							}
							else {
								String[] toks = candidate.getEntity().getRawName().split(" -,");
								boolean allDict = false;
								for (int i=0; i<toks.length; i++) {
									if (!lexicon.inDictionary(toks[i])) {
										allDict = false;
										break;
									}
									else {
										allDict = true;
									}
								}
								if (allDict) {
									inDict = true;
								}
							}
						
							features.inDictionary = inDict;
						}
						if (features.Add_relatedness) {
							double relatedness = 0;//erdRelatedness.getRelatednessTo(candidate, context);
							features.relatedness = relatedness;
						}
					
						if (features.Add_isSubTerm) { 
							boolean val = false;
							
							if (candidate.isSubTerm) {
								val = true;
							}
							else {
								String surface1 = candidate.getEntity().getRawName();

								// we check if the raw string is a substring of another NerdCandidate from the ERD method
								for(int j=0; j<candidates.size(); j++) {									
									NerdCandidate term2 = candidates.get(j);

									String surface2 = term2.getEntity().getRawName();
									if ((surface2.length() > surface1.length()) && (surface2.indexOf(surface1) != -1)) {
										val = true;
										break;
									}
								}
							}
							
							if (val)
								features.isSubTerm = true;
							else 
								features.isSubTerm = false;
						}
						if (features.Add_ner_st) {
							if (candidate.getEntity() != null) {
								if (candidate.getEntity().getType() != null) {
									features.ner_st = true;
								}
							}
						}
						if (features.Add_ner_id) {
							if (candidate.getEntity() != null) {
								if (candidate.getEntity().getType() != null) {
									features.ner_st = true;
								}
							}
						}
						if (features.Add_ner_type) {
							if (candidate.getEntity() != null) {
								if (candidate.getEntity().getType() != null) {
									if (candidate.getEntity().getType().equals("person/N1"))
										features.ner_type = "PERSON";
									else if (candidate.getEntity().getType().equals("location/N1"))
										features.ner_type = "LOCATION";	
									else if (candidate.getEntity().getType().equals("organizational_unit/N1"))
										features.ner_type = "ORGANIZATION";	
								}
							}
						}
						if (features.Add_ner_subtype) {
							if (candidate.getEntity() != null) {
								List<String> subTypes = candidate.getEntity().getSubTypes();
								if ( (subTypes != null) && (subTypes.size()>0) ) {
									features.ner_subtype = subTypes.get(0);
								}
							}
						}				
						if (features.Add_NERType_relatedness) {
							double relatedness = 0.0;//erdRelatedness.getRelatednessTo(candidate, context);
							features.NERType_relatedness = relatedness;						
						}
						if (features.Add_NERSubType_relatedness) {
							double relatedness = 0.0;//erdRelatedness.getRelatednessTo(candidate, context);
							features.NERSubType_relatedness = relatedness;
						}
						if (features.Add_occ_term) {
							long frequency = 0L;
							if (candidate.getEntity() != null)
								frequency = erdRelatedness.getTermOccurrence(candidate.getEntity().getRawName(), "en");
							else 
								System.out.println("Warning: no surface form for " + candidate.toString());
							features.occ_term = frequency;
						}
						 
						// is this candidate present in the reference annotations?
						boolean correct = false;
						if ( (references != null) && (references.size() > 0) ) {		
							for(ErdAnnotationShort reference : references) {
								if (reference.getPrimaryId().equals(candidate.getFreeBaseExternalRef())) {
									correct = true;
									break;
								}
							}
						}
						if (correct) {
							// the candidate is a valid entity
							features.classes = "Y";
							features.label = 1.0;
						}
						else {
							// default, not a valid entity
							features.classes = "N";
							features.label = 0.0;
						}
					
						String vectorString = features.printVector();
						try {						
							arffWriter.write(vectorString+ "\n");
							arffWriter.flush();
						}
						catch(Exception e) {
							throw new NerdException("Error when writing the ARFF training file. ", e);
						}
					}
				}
			}
		}
		try {
			arffWriter.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		if (erdRelatedness != null) {
			erdRelatedness.close();
		}
	}

	/**
	 *  Train a model given ARFF files - ranker model
	 */
	public void train_ranker_short(String dataset) {
		try {
			// create ARFF data file 
			createTrainingRankerShortARFFFile(dataset);
			// Shall we imply use also for training the data instance instead of using a training file?
		
			// read again the training data
			DataSource source = new DataSource(trainingDataRankerShortFile);
			Instances data = source.getDataSet();
		 	// setting class attribute (if the data format does not already provide this information?)
		 	if (data.classIndex() == -1)
		   		data.setClassIndex(data.numAttributes() - 1);
		
			Classifier classifier = new Bagging() ;
			classifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W weka.classifiers.trees.J48 -- -U -M 1")) ;
			//classifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W weka.classifiers.functions.MultilayerPerceptron -i -k -t")) ;
			//classifier.setOptions(Utils.splitOptions("-P 100 -S 1 -I 10 -W weka.classifiers.functions.LibSVM -S 4 -i -k")) ;
			classifier.buildClassifier(data);
		
			// saving the classifier
			File file = null;
			try {
				file = new File(rankerShortFile);
			}
			catch(Exception e) {
				throw new 
					NerdResourceException("Error when opening the file for saving the ranker model for short text: " 
					+ rankerShortFile, e);
			}
			try {
				//classifier.save(file);
				
				BufferedOutputStream bufferedOut = new BufferedOutputStream(
						new FileOutputStream(file));
				ObjectOutputStream out = new ObjectOutputStream(bufferedOut);
				out.writeObject(classifier);
				out.flush();
				out.close();
			}
			catch(Exception e) {
				throw new NerdException("Error when saving the ranker classifier. ", e);
			}
		}
		catch(Exception e) {
			throw new NerdException("Error when training the ranker classifier. ", e);
		}
	}
	
	/**
	 *  Train a model given ARFF files - selector model
	 */
	/*public void train_selector_short(String dataset) {
		try {
			// create ARFF data file 
			createTrainingSelectorShortARFFFile(dataset);
			// Shall we imply use also for training the data instance instead of using a training file?
		
			// read again the training data
			DataSource source = new DataSource(trainingDataSelectorShortFile);
			Instances data = source.getDataSet();
		 	// setting class attribute (if the data format does not already provide this information?)
		 	if (data.classIndex() == -1)
		   		data.setClassIndex(data.numAttributes() - 1);
		
			Classifier classifier = new Bagging() ;
			classifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W weka.classifiers.trees.J48 -- -U -M 1")) ;
			//classifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W weka.classifiers.functions.MultilayerPerceptron -i -k -t")) ;
			//classifier.setOptions(Utils.splitOptions("-P 100 -S 1 -I 10 -W weka.classifiers.functions.LibSVM -S 4 -i -k")) ;
			classifier.buildClassifier(data);
		
			// saving the classifier
			File file = null;
			try {
				file = new File(selectorShortFile);
			}
			catch(Exception e) {
				throw new 
					NerdResourceException("Error when opening the file for saving the selector model for short text: " 
					+ selectorShortFile, e);
			}
			try {
				//classifier.save(file);
				
				BufferedOutputStream bufferedOut = new BufferedOutputStream(
						new FileOutputStream(file));
				ObjectOutputStream out = new ObjectOutputStream(bufferedOut);
				out.writeObject(classifier);
				out.flush();
				out.close();
			}
			catch(Exception e) {
				throw new NerdException("Error when saving the selector classifier. ", e);
			}
		}
		catch(Exception e) {
			throw new NerdException("Error when training the selector classifier. ", e);
		}
	}*/
	
	/**
     *	Launch the ERD training.
     */
    public static void main(String[] args)
        throws IOException, ClassNotFoundException, 
               InstantiationException, IllegalAccessException {
		ErdLexicon lex = null;
		try {
			long start = System.currentTimeMillis();
        	ErdTrainer train = ErdTrainer.getInstance();
			// dataset: trec_beta, yahooL24, trec_web, trec_million, all, all-not-beta
			//train.train_ranker_short("trec_beta");
			//train.train_ranker_short("all");
			//train.train_selector_short("trec_beta");
			//train.train_selector_short("yahooL24");
			//train.train_selector_short("all");
			long end = System.currentTimeMillis();
			System.out.println("Training done in " + (end - start) + " milliseconds");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}


