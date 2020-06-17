package com.scienceminer.nerd.evaluation;

import org.grobid.core.lang.Language;
import org.grobid.trainer.evaluation.LabelStat;
import org.grobid.core.lang.LanguageDetectorFactory;

import com.scienceminer.nerd.disambiguation.*;
import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.service.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;
import java.text.DecimalFormat;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.io.*;
import org.codehaus.jackson.node.*;

/**
 * Method for evaluating the disambiguation of a vector of weighted terms
 *
 */
public class TermVectorEvaluation {
	public static final Logger LOGGER = LoggerFactory.getLogger(TermVectorEvaluation.class);
	
	private NerdEngine engine = null;
	private DecimalFormat df = new DecimalFormat("#.####");
	
	private int accumulated_tp = 0;
	private int accumulated_tn = 0;
	private int accumulated_fp = 0;
	private int accumulated_fn = 0;
	private double accumulated_accuracy = 0.0;
	private double accumulated_precision = 0.0;
	private double accumulated_recall = 0.0;
	
	// statistics
	private int nbKeywords = 0;
	private int nbExpectedEntities = 0;
	private int nbAbsent = 0;
	private int nbWeak = 0;
	private int nbRedundant = 0;
	private int nbActualResults = 0;
	private int nbMatches = 0;
	
	public TermVectorEvaluation() {
		try {
			engine = NerdEngine.getInstance();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void evaluate(String corpusPath) {
		evaluate(corpusPath, 0.0);
	}
	
	/**
	 * Launch the evaluation against an annotated term vector set. 
	 * A threshold value can be specified for pruning the predicted entities. 
	 */
	public void evaluate(String corpusPath, double threshold) {
		reinit();
        File directory = new File(corpusPath);
		if (!directory.exists()) {
            System.err.println("Path to the evaluation data directory is not valid");
            return;
		}
		final File[] refFiles = directory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".json");
			}
		}); 

		if (refFiles == null) {
			throw new IllegalStateException("Folder " + directory.getAbsolutePath()
					+ " does not seem to contain json evaluation data.");
		}

		long start = System.currentTimeMillis();

		System.out.println(refFiles.length + " json files");
		int n = 0;
		StringBuilder builder = new StringBuilder();
		for (; n < refFiles.length; n++) {
			final File jsonfile = refFiles[n];
//System.out.println("processing: " + jsonfile.getName());
			evaluate(jsonfile, builder, threshold);
		}
		
		long end = System.currentTimeMillis();

		// add the global evaluation
		// local metrics & accumulation
		builder.append("\n\ntotal number of documents: " + refFiles.length + "\n");
		builder.append("total number of keywords: " + nbKeywords + "\n");
		builder.append("total number of expected entities: " + nbExpectedEntities + "\n");

		builder.append("\ntotal number of absent keywords: " + nbAbsent + "\n");
		builder.append("total number of weak keywords: " + nbWeak + "\n");
		builder.append("total number of redundant keywords: " + nbRedundant + "\n");

		builder.append("\ntotal number of entities predicted: " + nbActualResults + "\n");
		builder.append("\ntotal number of expected entities matches: " + nbMatches + "\n");

		builder.append("\nGlobal metrics\n");
		builder.append("* Micro-average - ");
        double accuracy = 
			(double) (accumulated_tp + accumulated_tn) / 
				(accumulated_tp + accumulated_fp + accumulated_tn + accumulated_fn);
        double precision = (double) (accumulated_tp) / (accumulated_tp + accumulated_fp);
        double recall = (double) (accumulated_tp) / (accumulated_tp + accumulated_fn);
        double f0 = (2 * precision * recall) / (precision + recall);

		builder.append("Accuracy: " + df.format(accuracy) + "\tPrecision: " + df.format(precision) + 
			"\tRecall: " + df.format(recall) + "\tF1: " + df.format(f0) + "\n"); 

		builder.append("* Macro-average - ");
		accuracy = accumulated_accuracy / refFiles.length;
		precision = accumulated_precision / refFiles.length;
		recall = accumulated_recall / refFiles.length;
		f0 = (2 * precision * recall) / (precision + recall);

		builder.append("Accuracy: " + df.format(accuracy) + "\tPrecision: " + df.format(precision) + 
			"\tRecall: " + df.format(recall) + "\tF1: " + df.format(f0) + "\n"); 
		System.out.println("\n****** Threshold: " + threshold);
		System.out.println(builder.toString());
		
		System.out.println("Runtime: " + (end - start) + " ms");
	}
	
	/**
	 * Launch the evaluation against on term vector
	 */
	private void evaluate(File jsonFile, StringBuilder builder, double threshold) {
		try {
			String json = FileUtils.readFileToString(jsonFile, "UTF-8");
			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonRoot = mapper.readTree(json);
			List<WeightedTerm> terms = new ArrayList<WeightedTerm>();
			
			JsonNode idNode = jsonRoot.findPath("publication_id");
			if ((idNode != null) && (!idNode.isMissingNode())) {
    			String docid = idNode.getTextValue();
				
				JsonNode id = jsonRoot.findPath("_id");
				JsonNode inventionTitle = jsonRoot.findPath("invention_title");
				JsonNode keywordsNode = jsonRoot.findPath("keywords");
				
				// optional textual elements
				JsonNode textNode = jsonRoot.findPath("text"); 
				JsonNode abstractNode = jsonRoot.findPath("abstract"); 
				JsonNode claimsNode = jsonRoot.findPath("claims"); 
				JsonNode descriptionNode = jsonRoot.findPath("description"); 
				
				List<List<String>> expected = new ArrayList<List<String>>();
				if (keywordsNode.isArray()) {
				    for (final JsonNode termNode : keywordsNode) {
						String keyword = null;
						double weight = 0.0;

		                JsonNode idNode2 = termNode.findPath("keyword");
						if ((idNode2 != null) && (!idNode2.isMissingNode())) {
		        			keyword = idNode2.getTextValue();
							keyword = keyword.replace("_", " ");
						}

		                idNode2 = termNode.findPath("weight");
						if ((idNode2 != null) && (!idNode2.isMissingNode())) {
		        			weight = idNode2.getDoubleValue();
						}
		                
						idNode2 = termNode.findPath("status");
						if ((idNode2 != null) && (!idNode2.isMissingNode())) {
		        			String status = idNode2.getTextValue();
							if (status.equals("absent"))
								nbAbsent++;
							else if (status.equals("weak"))
								nbWeak++;
							else if (status.equals("redundant"))
								nbRedundant++;
						}
						
						if ( (keyword != null) && (weight != 0.0) ) {
							WeightedTerm wTerm = new WeightedTerm();
							wTerm.setTerm(keyword);
							wTerm.setScore(weight);
							terms.add(wTerm);
							nbKeywords++;
							List<String> expectedPages = null;
							
							// expected disambiguation
							JsonNode entityNodes = termNode.findPath("entities");
							if ( (entityNodes != null) && (entityNodes.isArray()) ) {
								
								for (final JsonNode entityNode : entityNodes) {
									JsonNode wikiNode = entityNode.findPath("wikipediaExternalRef");
									String page = wikiNode.getTextValue();
									if (expectedPages == null)
										expectedPages = new ArrayList<String>();
									expectedPages.add(page);
									nbExpectedEntities++;
								}
							}
							expected.add(expectedPages);
						}
				    }
				}
				
				NerdQuery query = new NerdQuery();
				Language lang = new Language("en", 1.0);
				query.setLanguage(lang);
				query.setTermVector(terms);
				query.setNbest(false);
				if ((textNode != null) && (!textNode.isMissingNode())) {
					query.setText(textNode.getTextValue());
				}
				if ((abstractNode != null) && (!abstractNode.isMissingNode())) {
					query.setAbstract_(abstractNode.getTextValue());
				}
				/*if ((claimsNode != null) && (!claimsNode.isMissingNode())) {
					query.setClaims(claimsNode.getTextValue());
				}*/
				if ((descriptionNode != null) && (!descriptionNode.isMissingNode())) {
					query.setDescription(descriptionNode.getTextValue());
				}
				// we disambiguate the term vector
				engine.disambiguateWeightedTerms(query);
				
				List<WeightedTerm> resultTerms = query.getTermVector();
				int p = 0; // keyword index
				int tp = 0; // true positive
				int fp = 0; // false positive
				int tn = 0; // true negative
				int fn = 0; // false negative
				if ( (resultTerms != null) && (resultTerms.size() > 0) ) {
					for(WeightedTerm term : resultTerms) {
						// current expected
						List<String> localExpected = expected.get(p);
						List<NerdEntity> entities = term.getNerdEntities();
						if ( (entities != null) && (entities.size() > 0) ) {
							nbActualResults++;
							NerdEntity entity = entities.get(0);
							if (entity.getNerdScore() > threshold) {
								String actualPage = "" + entity.getWikipediaExternalRef();
								if (localExpected == null) {
									// no result expected for this keyword
									fp++;
								}
								else if (localExpected.contains(actualPage)) {
									nbMatches++;
									tp++;
								}
								else {
									fp++;
								}
							}
							else {
								if (localExpected == null) {
									tn++;
								}
								else {
									fn++;
								}
							}
						}
						else {
							if (localExpected == null) {
								tn++;
							}
							else {
								fn++;
							}
						}
						p++;
					}
				}
				// local metrics & accumulation
				builder.append("\nDocument: " + docid + "\n");
				
                double accuracy = 0.0;
				if (tp + fp + tn + fn != 0) {
					accuracy = (double) (tp + tn) / (tp + fp + tn + fn);
				}
                double precision = 0.0;
				if (tp + fp != 0)
					precision = (double) (tp) / (tp + fp);
				double recall = 0.0;
				if (tp + fn != 0)
 				   recall = (double) (tp) / (tp + fn);
                double f0 = 0.0;
				if (precision + recall != 0.0)
					f0 = (2 * precision * recall) / (precision + recall);

				builder.append("A: " + df.format(accuracy) + "\tP: " + df.format(precision) + 
					"\tR: " + df.format(recall) + "\tF1: " + df.format(f0) + "\n"); 

				accumulated_tp += tp;
				accumulated_tn += tn;
				accumulated_fp += fp;
				accumulated_fn += fn;
			
				accumulated_accuracy += accuracy;
				accumulated_precision += precision;
				accumulated_recall += recall;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void reinit() {
	    accumulated_tp = 0;
	   	accumulated_tn = 0;
	   	accumulated_fp = 0;
	   	accumulated_fn = 0;
		accumulated_accuracy = 0.0;
		accumulated_precision = 0.0;
		accumulated_recall = 0.0;
		
		nbKeywords = 0;
		nbExpectedEntities = 0;
		nbAbsent = 0;
		nbWeak = 0;
		nbRedundant =0;
	}
	
}