package com.scienceminer.nerd.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.TreeMap;
import java.io.*;

import org.grobid.core.utilities.TextUtilities;
import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.erd.ErdAnnotationShort;
import com.scienceminer.nerd.erd.ErdUtilities;
import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.mention.Mention;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.utilities.NerdConfig;
import org.grobid.core.data.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.model.Article;

/**
 * 
 */
public class ErdEvaluate {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ErdEvaluate.class);

	private static volatile ErdEvaluate instance = null;
	
	private NerdEngine annotator = null; 
	private ProcessText processText = null; 
	
	public static ErdEvaluate getInstance() throws Exception {
	    if (instance == null) {
			getNewInstance();	        
	    }
	    return instance;
	}

	/**
	 * Creates a new instance.
	 */
	private static synchronized void getNewInstance() throws Exception {
		LOGGER.debug("Get new instance of ErdAnnotator");		
		instance = new ErdEvaluate();
	}

	/**
	 * Hidden constructor
	 */
	private ErdEvaluate() throws Exception {		
		annotator = NerdEngine.getInstance();
		processText = ProcessText.getInstance();
	}

	public void evaluateShort(String dataset) {
		String path = null;
		String pathRef = null;
		if (dataset.equals("trec_beta")) {
			 path = "data/corpus/corpus-short/Trec_beta.query.txt";
			 pathRef = "data/corpus/corpus-short/Trec_beta.annotation.txt";
		}
		else if (dataset.equals("yahooL24")) {
			path = "data/corpus/corpus-short/yahoo/Webscope_L24/L24_queries.txt";
		 	pathRef = "data/corpus/corpus-short/yahoo/Webscope_L24/L24_annotations.txt";
		}
		else if (dataset.equals("trec_test")) {
			 path = "data/corpus/corpus-short/trec-test_queries.txt";
			 pathRef = "data/corpus/corpus-short/trec-test_annotations.txt";
		}
		else if (dataset.equals("trec_500")) {
			 path = "data/corpus/corpus-short/trec-500.queries.txt";
			 pathRef = "data/corpus/corpus-short/trec-500.annotations.txt";
		}
		else {
			System.err.println("Error: unknown evaluation dataset");
			return;
		}
		BufferedReader dis = null;
		try {
			int nbRetrieved = 0;
			dis = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            String l = null;
			Map<String, List<ErdAnnotationShort>> results = new TreeMap<String, List<ErdAnnotationShort>>();
			Map<String, List<ErdAnnotationShort>> references = new TreeMap<String, List<ErdAnnotationShort>>();
			List<String> queryList = new ArrayList<String>();
            while ((l = dis.readLine()) != null) {
				if (l.length() == 0) {
					continue;
				}
				System.out.println("\n" + l);
				String[] tokens = l.split("\t");
				if (tokens.length != 2) {
					continue;
				}
				String textId = tokens[0];
				queryList.add(textId);
				String text = tokens[1];
				try {
					NerdQuery nerdQuery = new NerdQuery();
					nerdQuery.setNbest(false);
					nerdQuery.setSentence(false);
					nerdQuery.setShortText(text);
					nerdQuery.addMention(ProcessText.MentionMethod.wikipedia);

					ProcessText processText = ProcessText.getInstance();
					List<Mention> entities = processText.process(nerdQuery);
					List<NerdEntity> disambiguatedEntities = new ArrayList<NerdEntity>();

					if (entities != null) {
						for (Mention entity : entities) {
							NerdEntity nerdEntity = new NerdEntity(entity);
							disambiguatedEntities.add(nerdEntity);
						}

						NerdEngine engine = NerdEngine.getInstance();
						nerdQuery.setEntities(disambiguatedEntities);
						disambiguatedEntities = engine.disambiguate(nerdQuery);


						// NerdEntity need to be converted into ErdAnnotationShort
						/*List<ErdAnnotationShort> annotations = annotator.annotateShort("0", textId, text);
						results.put(textId, annotations);
						//if (annotations != null)		
						//System.out.println(ErdUtilities.encodeAnnotations(annotations));
						nbRetrieved += annotations.size();*/
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}

			// check the reference data now
			int nbExpected = 0;
			dis = new BufferedReader(new InputStreamReader(new FileInputStream(pathRef), "UTF-8"));
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
				
				List<ErdAnnotationShort> annotations = references.get(textId);
				if (annotations == null) {
					annotations = new ArrayList<ErdAnnotationShort>();
				}
				annotations.add(a);
				references.put(textId, annotations);
								
				/*String res = null;
				if ( (annotations !=null) && (annotations.size() != 0)) {
				 	res = ErdUtilities.encodeAnnotations(annotations);
					System.out.println(res);
				}*/

				nbExpected++;
			}
		
			// we now evaluate predicted results
			//Iterator entries = results.entrySet().iterator();
			int nbRelevant = 0;
			double accumulatedF1 = 0.0;
			int accumulatedQueries = 0;
			
			int i = 0;
			//while (entries.hasNext()) {
			for(String queryId : queryList) {
				System.out.println("\n\nQuery number " + queryId + " -----------------------");
				i++;
				
				int localRelevant = 0;
				int localExpected = 0;
				int localRetrieved = 0;
				
				List<ErdAnnotationShort> annotations = results.get(queryId);
				
				String res = null;
				if ( (annotations !=null) && (annotations.size() != 0)) {
				 	res = ErdUtilities.encodeAnnotationsShort(annotations);
					System.out.println("Predicted: \n" + res);
					localRetrieved = annotations.size();
				}
									
				List<ErdAnnotationShort> annotationsRef = references.get(queryId);
				
				String res2 = null;
				if ( (annotationsRef !=null) && (annotationsRef.size() != 0)) {
				 	res2 = ErdUtilities.encodeAnnotationsShort(annotationsRef);
					System.out.println("Expected: \n" + res2);
					localExpected = annotationsRef.size();
				}
				
				if (annotationsRef != null) {
					for(ErdAnnotationShort annotationRef : annotationsRef) {
						// we check if the expected annotations are present in the suggested ones
						for(ErdAnnotationShort annotation: annotations) {
							if (annotation.getPrimaryId() != null) {
								if (annotation.getPrimaryId().equals(annotationRef.getPrimaryId())) {
									nbRelevant++;
									localRelevant++;
									break;
								}
							}
						}
					}
				}
				
				// compute precision/recall/f1 for this query
				double localPrecision = 0.0;
				double localRecall = 0.0;
				double localF1 = 0.0;
				
				// when no result at all are produced, we ignore the query
				if ( (localExpected == 0) && (localRetrieved == 0) ) {
					continue;
				}
				else if (localRetrieved == 0) {
					// no result, so we don't match any expected results and all metrics are at 0.0
					System.out.println("Query-level -----------------------");
					System.out.println("Query Precision: 0.0");
					System.out.println("Query Recall: 0.0");
					System.out.println("Query f1: 0.0");
				}
				else if (localExpected == 0) { 
					System.out.println("Query-level -----------------------");
					System.out.println("Query Precision: 0.0");
					System.out.println("Query Recall: 0.0");
					System.out.println("Query f1: 0.0");
				}
				else {
					localPrecision = (double) (localRelevant) / localRetrieved;
					localRecall = (double) (localRelevant) / localExpected;
					
					if ( (localPrecision == 0.0) && (localRecall == 0.0)) {
						localF1 = 0.0;
					}
					else 
						localF1 = (2 * localPrecision * localRecall) / (localPrecision + localRecall);
					
					System.out.println("Query-level -----------------------");
					System.out.println("Query Precision: " + TextUtilities.formatTwoDecimals(localPrecision * 100));
					System.out.println("Query Recall: " + TextUtilities.formatTwoDecimals(localRecall * 100));
					System.out.println("Query f1: " + TextUtilities.formatTwoDecimals(localF1 * 100));
				}
				
				accumulatedF1 += localF1;
				accumulatedQueries++;
			}

			System.out.println("\nGlobal values -----------------------");
			System.out.println("nbRelevant: " + nbRelevant);				
			System.out.println("nbRetrieved: " + nbRetrieved);				
			System.out.println("nbExpected: " + nbExpected);
			
			// micro-level: global precision/recall/f1
			double precision = (double) (nbRelevant) / nbRetrieved;
			double recall = (double) (nbRelevant) / nbExpected;
			double f1 = (2 * precision * recall) / (precision + recall);
	
			System.out.println("\nMicro-level -----------------------");
			System.out.println("Precision: " + TextUtilities.formatTwoDecimals(precision * 100));
			System.out.println("Recall: " + TextUtilities.formatTwoDecimals(recall * 100));
			System.out.println("f1: " + TextUtilities.formatTwoDecimals(f1 * 100));
			
			// macro level, we average the precision/recall/f1 for each individual query
			System.out.println("\nMacro-level -----------------------");
			f1 = accumulatedF1/ accumulatedQueries;
			System.out.println("f1: " + TextUtilities.formatTwoDecimals(f1 * 100) + "\n");
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (dis != null) {
					dis.close();
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void evaluateLong() {

	}
	
	public void evaluateMentions() {

	}
	
	/**
     *	Launch the ERD evaluation.
     */
    public static void main(String[] args)
        throws IOException, ClassNotFoundException, 
               InstantiationException, IllegalAccessException {
		try {
			long start = System.currentTimeMillis();
        	ErdEvaluate eval = ErdEvaluate.getInstance();
			// dataset: trec_beta, yahooL24, trec_test
			eval.evaluateShort("trec_beta");
			long end = System.currentTimeMillis();
			System.out.println("Evaluation done in " + (end - start) + " milliseconds");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}


