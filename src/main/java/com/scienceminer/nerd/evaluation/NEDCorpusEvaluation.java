package com.scienceminer.nerd.evaluation;

import java.util.*;
import java.io.*;
import java.text.*;
import java.nio.file.*;

import org.apache.commons.io.FileUtils;

import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lang.Language;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Entity;

import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.training.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.disambiguation.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluation of entity disambiguation (NED) against the standard dataset 
 * as provided by the WNED dataset: ace2004, aida-conll, aquaint, msnbc, clueweb12,
 * wikipedia (extract) following a common format.
 * 
 * So mentions are provided and the task is to identify the right entity.
 *
 * Produce accuracy, Micro and Macro scores.  
 * 
 * Example launch command:
 * mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.evaluation.NEDCorpusEvaluation -Dexec.args="ace"
 */

public class NEDCorpusEvaluation {
	private static final Logger LOGGER = LoggerFactory.getLogger(NEDCorpusEvaluation.class);

	private static List<String> corpora = Arrays.asList("ace", "aida", "aida-train", "aida-testa", "aida-testb", 
		"aquaint", "msnbc", "clueweb", "wikipedia");

	private NerdRanker ranker = null;
	private LowerKnowledgeBase wikipedia = null;
	
	public NEDCorpusEvaluation() {
		// init ranker model
		try {
			UpperKnowledgeBase.getInstance();
		} catch(Exception e) {
			throw new NerdResourceException("Error instanciating the knowledge base. ", e);
		}

		// all the corpus are in English so far
		wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf("en");
		try {
			ranker = new NerdRanker(wikipedia);
		} catch(Exception e) {
			throw new NerdResourceException("Error when opening the relatedness model", e);
		}
	}

	public String eval(String corpus) {
		StringBuilder report = new StringBuilder();

		String corpusPath = "data/corpus/corpus-long/" + corpus + "/";
		String corpusRefPath = corpusPath + corpus + ".xml";
		File corpusRefFile = new File(corpusRefPath);

		if (!corpusRefFile.exists()) {
			System.out.println("The reference file for corpus " + corpus + " is not found: " + corpusRefFile.getPath());
			return null;
		}

		// first we parse the result to get the documents and mentions
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

		Language lang = new Language("en", 1.0);

		//get the root element and the document node
		Element root = dom.getDocumentElement();
		NodeList docs = root.getElementsByTagName("document");
		if (docs == null || docs.getLength() <= 0) {
			LOGGER.error("the list documents for this corpus is empty");
			return null;
		}

		int totalExpected = 0;
		int totalCorrect = 0;
		int totalFound = 0;
			
		// priors
		int totalCorrectPrior = 0;
		int totalFoundPrior = 0;
		double precisionPrior = 0.0;
		double recallPrior = 0.0;
		double f1Prior = 0.0;

		// mention-only recall
		int totalFoundMention = 0;

		// ranker
		double precision = 0.0;
		double recall = 0.0;
		double f1 = 0.0;

		for (int i = 0; i < docs.getLength(); i++) {
			//get the annotations of each document.
			Element docElement = (Element)docs.item(i);
			String docName = docElement.getAttribute("docName");
			String docPath = corpusPath + "RawText/" + docName;
			File docFile = new File(docPath);
			if (!docFile.exists()) {
				System.out.println("The document file " + docPath + " for corpus " + corpus + " is not found: ");
				/*String prefix = docName.substring(0,3);
				try {
					Path path1 = FileSystems.getDefault().getPath("/mnt/data/resources/reuters/RCV1_CD1/"+prefix+"/", docName.replace(".txt", ".xml"));
					Path path2 = FileSystems.getDefault().getPath(corpusPath + "RawText/", docName.replace(".txt", ".xml"));
					Files.copy(path1, path2, StandardCopyOption.REPLACE_EXISTING);
				} catch(Exception e) {
					e.printStackTrace();
				}*/
				continue;
			}

			String docContent = null;
			try {
				docContent = FileUtils.readFileToString(docFile, "UTF-8");
			} catch(Exception e) {
				e.printStackTrace();
			}

			if (docContent == null || docContent.length() == 0) {
				System.out.println("Document is empty: " + docPath);
			}

			// if the corpus is AIDA, we need to ignore the two first lines 
			if (corpus.startsWith("aida")) {
				int ind = docContent.indexOf("\n");
				ind = docContent.indexOf("\n", ind +1);
				docContent = docContent.substring(ind+1);
				docContent = docContent.replace("\n\n", "\n");
				docContent = docContent.replace("\n", "\n\n");
				docContent = docContent.replace("&amp;", "&");
			}

			// get the annotations, mentions + entity
			NodeList annotations = docElement.getElementsByTagName("annotation");
			if (annotations == null || annotations.getLength() <= 0)
				continue;

			Set<Integer> referenceDisamb = new HashSet<Integer>();
			Set<Integer> producedDisamb = new HashSet<Integer>();
			List<NerdEntity> referenceEntities = new ArrayList<NerdEntity>();

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
				Article article = wikipedia.getArticleByTitle(wikiName);
				if (article == null) {
					System.out.println("Invalid article name - article not found in Wikipedia: " + wikiName);
					continue;
				} else 
					pageId = article.getId();

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

				referenceEntities.add(ref);
				referenceDisamb.add(new Integer(pageId));
			}

			List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(docContent, lang);
			// be sure to have the entities to be ranked
			List<Entity> nerEntities = new ArrayList<Entity>();
			for(NerdEntity refEntity : referenceEntities) {
				Entity localEntity = new Entity(refEntity.getRawName());
				localEntity.setOffsetStart(refEntity.getOffsetStart());
				localEntity.setOffsetEnd(refEntity.getOffsetEnd());
				nerEntities.add(localEntity);
			}
			List<NerdEntity> entities = new ArrayList<NerdEntity>();
			for (Entity entity : nerEntities) {
				NerdEntity theEntity = new NerdEntity(entity);
				entities.add(theEntity);
			}
			
			try {
				// process the text for building actual context for evaluation
				ProcessText processText = ProcessText.getInstance();
				nerEntities = new ArrayList<Entity>();
				Language language = new Language("en", 1.0);
				nerEntities = processText.process(docContent, lang);
				for(Entity entity : nerEntities) {
					// we add entities only if the mention is not already present
					NerdEntity theEntity = new NerdEntity(entity);
					if (!entities.contains(theEntity))
						entities.add(theEntity);
				}
				//System.out.println("number of NE found: " + entities.size());	
				// add non NE terms
				List<Entity> entities2 = processText.processBrutal(docContent, lang);
				for(Entity entity : entities2) {
					// we add entities only if the mention is not already present
					NerdEntity theEntity = new NerdEntity(entity);
					if (!entities.contains(theEntity))
						entities.add(theEntity);
				}

				NerdEngine engine = NerdEngine.getInstance();
				Map<NerdEntity, List<NerdCandidate>> candidates = 
					engine.generateCandidates(entities, "en");	

				for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
					List<NerdCandidate> cands = entry.getValue();
					NerdEntity entity = entry.getKey();
					int start = entity.getOffsetStart();
					int end = entity.getOffsetEnd(); 
					for(NerdEntity refEntity : referenceEntities) {
						int startRef = refEntity.getOffsetStart();
						int endRef = refEntity.getOffsetEnd(); 
						if ((start == startRef) && (end == endRef)) {
							if (cands == null || cands.size() == 0) {
								System.out.println("found no candidate for mention: " + entity.getRawName());
							}
							break;
						}
					}
				}

				// do we have the expected result in the candidates for the mentions?
				for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
					List<NerdCandidate> cands = entry.getValue();
					NerdEntity entity = entry.getKey();
					if (cands.size() > 0) {
						// check that we have a reference result for the same chunck
						int start = entity.getOffsetStart();
						int end = entity.getOffsetEnd(); 
						for(NerdEntity refEntity : referenceEntities) {
							int startRef = refEntity.getOffsetStart();
							int endRef = refEntity.getOffsetEnd(); 
							if ((start == startRef) && (end == endRef)) {
								for(NerdCandidate cand : cands) {
									if (cand.getWikipediaExternalRef() == refEntity.getWikipediaExternalRef()) {
										totalFoundMention++;
										break;
									}
								}
							}
						}
					} 
				}


				// evaluate priors and mention selection recall
				int foundPrior = 0;
				int correctPrior = 0;
				for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
					List<NerdCandidate> cands = entry.getValue();
					NerdEntity entity = entry.getKey();
					if (cands.size() > 0) {
						// check that we have a reference result for the same chunck
						int start = entity.getOffsetStart();
						int end = entity.getOffsetEnd(); 
						for(NerdEntity refEntity : referenceEntities) {
							int startRef = refEntity.getOffsetStart();
							int endRef = refEntity.getOffsetEnd(); 
							if ((start == startRef) && (end == endRef)) {
								foundPrior++;
								totalFoundPrior++;
								if (cands.get(0).getWikipediaExternalRef() == refEntity.getWikipediaExternalRef()) {
									correctPrior++;
									totalCorrectPrior++;
								}
								break;
							}
						}
					} 
				}

/*for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
	List<NerdCandidate> cands = entry.getValue();
	NerdEntity entity = entry.getKey();
System.out.println("Surface: " + entity.getRawName());	
for(NerdCandidate cand : cands) {
	System.out.println("rank: " + cand.toString());
}
System.out.println("--");
}*/

				engine.rank(candidates, wikipedia.getConfig().getLangCode(), null, false, tokens);
			
				int found = 0;
				int correct = 0;
				int expected = referenceEntities.size();
		 		totalExpected += expected;
		 		if (expected == 0)	
		 			continue;

				for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
					List<NerdCandidate> cands = entry.getValue();
					NerdEntity entity = entry.getKey();
					if (cands.size() > 0) {
						// check that we have a reference result for the same chunck
						int start = entity.getOffsetStart();
						int end = entity.getOffsetEnd(); 
						for(NerdEntity refEntity : referenceEntities) {
							int startRef = refEntity.getOffsetStart();
							int endRef = refEntity.getOffsetEnd(); 
							if ((start == startRef) && (end == endRef)) {
								found++;
								totalFound++;

								if (cands.get(0).getWikipediaExternalRef() == refEntity.getWikipediaExternalRef()) {
									correct++;
									totalCorrect++;
								}
								break;
							}
						}
					} 
				}
		 		
		 		// prior stat update
				if (foundPrior == 0)
					precisionPrior += 0;
				else
					precisionPrior += correctPrior * 1.0 / foundPrior;
				recallPrior += correctPrior * 1.0 / expected;

		 		// ranker stat update
				if (found == 0)
					precision += 0;
				else
					precision += correct * 1.0 / found;
				recall += correct * 1.0 / expected;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		report.append("\nEvaluation on " + docs.getLength() + " documents and " + totalExpected + " expected entities\n");


		// recall for candidate selection
		double recallMention = totalFoundMention*1.0/totalExpected;
		report.append("\ncandidate gold recall: " + TextUtilities.formatTwoDecimals(recallMention * 100) + "\n");

		// micro-level measures
		report.append("\n** micro average measures **\n");
		List<Result> results = new ArrayList<Result>();

		// prior
		double accuracyPrior = totalCorrectPrior*1.0/totalExpected;
		precisionPrior = precisionPrior / docs.getLength();
		recallPrior = recallPrior / docs.getLength();
		f1Prior = 2 * precisionPrior * recallPrior / (precisionPrior + recallPrior);
		Result result = new Result("prior", accuracyPrior, precisionPrior, recallPrior, f1Prior);
		results.add(result);

		// ranker
		double accuracy = totalCorrect*1.0/totalExpected;
		precision = precision / docs.getLength();
		recall = recall / docs.getLength();
		f1 = 2 * precision * recall / (precision + recall);
		result = new Result("ranker", accuracy, precision, recall, f1);
		results.add(result);
		
		reportMetrics(report, corpus, results);

		// macro-level measures
		report.append("\n** macro average measures **\n");
		results = new ArrayList<Result>();

		// prior
		precisionPrior = totalCorrectPrior * 1.0 / totalFoundPrior;
		recallPrior = totalCorrectPrior * 1.0 / totalExpected;
		f1Prior = 2 * precisionPrior * recallPrior / (precisionPrior + recallPrior);
		result = new Result("prior", accuracyPrior, precisionPrior, recallPrior, f1Prior);
		results.add(result);
		
		// ranker
		precision = totalCorrect * 1.0 / totalFound;
		recall = totalCorrect * 1.0 / totalExpected;
		f1 = 2 * precision * recall / (precision + recall);
		result = new Result("ranker", accuracy, precision, recall, f1);
		results.add(result);

		reportMetrics(report, corpus, results);

		return report.toString();
	}

	private void reportMetrics(StringBuilder report, 
			String corpus,
			List<Result> results) {		
		report.append(String.format("\n%-20s %-12s %-12s %-12s %-7s\n\n",
				corpus,
				"accuracy",
				"precision",
				"recall",
				"f1"));
		for(Result result : results) {
			report.append(String.format("%-20s %-12s %-12s %-12s %-7s\n",
				result.method,
				TextUtilities.formatTwoDecimals(result.accuracy * 100),
				TextUtilities.formatTwoDecimals(result.precision * 100),
				TextUtilities.formatTwoDecimals(result.recall * 100),
				TextUtilities.formatTwoDecimals(result.f1 * 100)));	
		}
	}

	/** just a dummy class to hold the results */
	public class Result {
		public String method = null;
		public double accuracy = 0.0;
		public double precision = 0.0;
		public double recall = 0.0;
		public double f1 = 0.0;

		public Result(String method, double accuracy, double precision, double recall, double f1) {
			this.method = method;
			this.accuracy = accuracy;
			this.precision = precision;
			this.recall = recall;
			this.f1 = f1;
		}
	} 

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: command [name_of_corpus]");
			System.err.println("corpus must be one of: " + NEDCorpusEvaluation.corpora.toString());
			System.exit(-1);
		}
		String corpus = args[0].toLowerCase();
		if (!corpora.contains(corpus)) {
			System.err.println("corpus must be one of: " + NEDCorpusEvaluation.corpora.toString());
			System.exit(-1);
		}

		NEDCorpusEvaluation nedEval = new NEDCorpusEvaluation();
		String report = nedEval.eval(corpus);
		if (report == null) {
			System.out.println("\nThe evaluation fails for corpus " + corpus);
		} else {
			System.out.println(report);
		}
	}

}