package com.scienceminer.nerd.erd.corpus;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.mention.Mention;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.erd.ErdAnnotationShort;
import com.scienceminer.nerd.erd.ErdUtilities;
import com.scienceminer.nerd.exceptions.*;
import org.grobid.core.data.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

/**
 * Class for generating and managing the ERD corpus.
 * 
 * 
 */
public class ErdCorpus {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ErdCorpus.class);

	private static volatile ErdCorpus instance = null;
	
	public static ErdCorpus getInstance() throws Exception {
	    if (instance == null) {
			getNewInstance();	        
	    }
	    return instance;
	}

	/**
	 * Creates a new instance.
	 */
	private static synchronized void getNewInstance() throws Exception {
		LOGGER.debug("Get new instance of ErdCorpus");		
		instance = new ErdCorpus();
	}

	/**
	 * Hidden constructor
	 */
	private ErdCorpus() throws Exception {		
	}

	/**
	 * Generate corpus from the Yahoo L24 dataset 
	 */ 
	private int generateCorpusYahooL24() {
		int nb = 0;
		
		String inputPath = "data/erd/corpus-short/yahoo/Webscope_L24/ydata-search-query-log-to-entities-v1_0.xml";
		String outputPathQueries = "data/erd/corpus-short/yahoo/Webscope_L24/L24_queries.txt";
		String outputPathAnnotations = "data/erd/corpus-short/yahoo/Webscope_L24/L24_annotations.txt";
		
		try {
		
			// get a factory for SAX parser
	        SAXParserFactory spf = SAXParserFactory.newInstance();
	        YahooL24SaxHandler saxHandler = new YahooL24SaxHandler();

	       	// get a new instance of parser
	       	final SAXParser p = spf.newSAXParser();
        
			// parse
			p.parse(inputPath, saxHandler);
		
			List<String> theQueries = saxHandler.getQueries();
			List<ErdAnnotationShort> theAnnotations = saxHandler.getAnnotations();
			if ( (theQueries != null) && (theQueries.size() > 0) && 
				 (theAnnotations != null) && (theAnnotations.size() > 0) ) {
		
				Writer queriesWriter = null;
				Writer annotationsWriter = null;
				try {
					// the file for the queries
			        OutputStream queriesOS = null;
			        if (outputPathQueries != null) {
			            queriesOS = new FileOutputStream(outputPathQueries);
			            queriesWriter = new OutputStreamWriter(queriesOS, "UTF8");
		
						for(String query : theQueries) {
							queriesWriter.write("yahoo-" + nb + "\t" + query + "\n");
							nb++;
						}
			        }

			        // the file for writing the annotations
			        OutputStream annotationsOS = null;
			        if (outputPathAnnotations != null) {
			            annotationsOS = new FileOutputStream(outputPathAnnotations);
			            annotationsWriter = new OutputStreamWriter(annotationsOS, "UTF8");

						annotationsWriter.write(ErdUtilities.encodeAnnotationsShort(theAnnotations) + "\n");
			        }

				}
	        	catch(Exception e) {
					e.printStackTrace();
				}
				finally {
					try {
						queriesWriter.close();
						annotationsWriter.close();
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return nb;
	} 
	
	/**
	 *  Generate corpus data based on the current system output.
	 *  dataset: - "trec_web" for queries of TREC web-track (several years), 
	 *           - "trec_million" for million-query track (be careful with the size)
	 *  max: maximum number of queries to be considered for the set
	 */
	public int generateCorpus(String dataset, int max) {
		String path = null;
		int nb = 0;
		NerdEngine engine = null;	
		try {
		 	engine = NerdEngine.getInstance();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		// we first store existing queries from the trec_beta set
		path = "data/erd2014/corpus-short/Trec_beta.query.txt";
		List<String> betas = new ArrayList<String>();
		BufferedReader dis = null;
		try {
			dis = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            String l = null;
            while ((l = dis.readLine()) != null) {
				if (l.length() == 0) {
					continue;
				}
				String[] tokens = l.split("\t");
				if (tokens.length != 2) 
					continue;
				betas.add(tokens[1].trim());
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	
		if (dataset.equals("trec_web")) {
			path = "data/erd2014/corpus-short/web-track/all.txt";
		}
		else if (dataset.equals("trec_million")) {
			path = "data/erd2014/corpus-short/million-query/09.million-query-.topics.20001-60000.txt";
		}
		else if (dataset.equals("trec_test")) {
			path = "data/erd2014/corpus-short/test.queries.txt";
		}
		else if (dataset.equals("trec_500")) {
			path = "data/erd2014/corpus-short/trec-500.txt";
		}
		
		dis = null;
		try {
			int nbRetrieved = 0;
			dis = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            String l = null;
			List<ErdAnnotationShort> annotations = new ArrayList<ErdAnnotationShort>();
			List<String> queries = new ArrayList<String>();
			
            while ((l = dis.readLine()) != null) {
				if (l.length() == 0) {
					continue;
				}
				String query = null;
				if (dataset.equals("trec_500")) {
					query = l.trim();
				}
				else if (l.startsWith("<narrative>")) {
					query = l.replace("<narrative>", "");
					query = query.replace("</narrative>","");
					query = query.replace(".", "");
					query = query.replace("?", "");
					query = query.replace(",", "");
					query = query.replace("(", "");
					query = query.replace(")", "");
					query = query.replace("Find homepages of", "");
					query = query.replace("I would like to see the homepages of the", "");
					query = query.replace("I want to see the homepages of", "");
					if (query.startsWith("Find")) {
						query = query.replace("Find ", "");
					}
					if (query.startsWith("What")) {
						query = query.replace("What ", "");
					}
					if (query.startsWith("Who are")) {
						query = query.replace("Who are ", "");
					}
					if (query.startsWith("Who has")) {
						query = query.replace("Who has ", "");
					}
					if (query.startsWith("the")) {
						query = query.replace("the ", "");
					}
					query = query.trim();
				}
				else if (l.startsWith("<title>")) {
					query = l.replace("<title> ", "");
				}
				else if ( (!l.startsWith("<")) && (l.lastIndexOf(":") != -1) ) {
					query = l.substring(l.lastIndexOf(":")+1, l.length());
				}
				else if (l.startsWith(" ")) { 
				 	query = l.substring(3, l.length());
				}
				
				if ( (query == null) || (query.length() == 0) ) {
					continue;
				}
				// we keep queries with at least 3 tokens
				String[] tokens = l.split(" ");
				if (tokens.length < 3) {
					continue;
				}
				
				// and we don't want queries already in the beta_trec dataset
				if (betas.contains(query)) {
					continue;
				}
				
				System.out.println(query);
				queries.add(query);
				nb++;
				try {
					NerdQuery nerdQuery = new NerdQuery();
					nerdQuery.setShortText(query);
					nerdQuery.setNbest(false);
					nerdQuery.setSentence(false);
					nerdQuery.addMention(ProcessText.MentionMethod.wikipedia);
					//nerdQuery.setFormat(output);
					//nerdQuery.setCustomisation(customisation);

					ProcessText processText = ProcessText.getInstance();
					List<Mention> entities = processText.process(nerdQuery);
					List<NerdEntity> disambiguatedEntities = new ArrayList<NerdEntity>();

					if (entities != null) {
						for (Mention entity : entities) {
							NerdEntity nerdEntity = new NerdEntity(entity);
							disambiguatedEntities.add(nerdEntity);
						}

						engine = NerdEngine.getInstance();
						nerdQuery.setEntities(disambiguatedEntities);
						disambiguatedEntities = engine.disambiguate(nerdQuery);

						// TBD: with the new disambiguator, disambiguated NerdEntity need to be converted into ErdAnnotationShort
						/*List<ErdAnnotationShort> newAnnotations = 
							annotator.annotateShort("0", "trec-web-"+(queries.size()-1), query);
						nbRetrieved += annotations.size();
						for(ErdAnnotationShort annot : newAnnotations) {
							annotations.add(annot);
						}*/
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			// we write the results
			String outputPathQueries = null;
			String outputPathAnnotations = null;
			if (dataset.equals("trec_web")) {
				outputPathQueries = "data/erd2014/corpus-short/web-track/trec-web_queries.txt";
				outputPathAnnotations = "data/erd2014/corpus-short/web-track/trec-web_annotations.txt";
			}
			else if (dataset.equals("trec_test")) {
				outputPathQueries = "data/erd2014/corpus-short/trec-test_queries.txt";
				outputPathAnnotations = "data/erd2014/corpus-short/trec-test_annotations.txt";
			}
			else if (dataset.equals("trec_million")) {
				outputPathQueries = "data/erd2014/corpus-short/web-million/trec-million_queries.txt";
				outputPathAnnotations = "data/erd2014/corpus-short/web-million/trec-million_annotations.txt";
			}
			else if (dataset.equals("trec_500")) {
				outputPathQueries = "data/erd2014/corpus-short/trec-500.queries.txt";
				outputPathAnnotations = "data/erd2014/corpus-short/trec-500.annotations.txt";
			}
			Writer queriesWriter = null;
			Writer annotationsWriter = null;
			try {
				// the file for the queries
		        OutputStream queriesOS = null;
		        if (outputPathQueries != null) {
		            queriesOS = new FileOutputStream(outputPathQueries);
		            queriesWriter = new OutputStreamWriter(queriesOS, "UTF8");
	
					int i = 0;
					for(String query : queries) {
						queriesWriter.write("trec-" + i + "\t" + query + "\n");
						i++;
					}
		        }

		        // the file for writing the annotations
		        OutputStream annotationsOS = null;
		        if (outputPathAnnotations != null) {
		            annotationsOS = new FileOutputStream(outputPathAnnotations);
		            annotationsWriter = new OutputStreamWriter(annotationsOS, "UTF8");

					annotationsWriter.write(ErdUtilities.encodeAnnotationsShort(annotations) + "\n");
		        }

			}
        	catch(Exception e) {
				e.printStackTrace();
			}
			finally {
				try {
					queriesWriter.close();
					annotationsWriter.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return nb;
	}
	
	/**
     *	Launch the ERD corpus generation.
     */
    public static void main(String[] args)
        throws IOException, ClassNotFoundException, 
               InstantiationException, IllegalAccessException {
		try {
        	ErdCorpus corpus = ErdCorpus.getInstance();
			
			long start = System.currentTimeMillis();
			//int nb = corpus.generateCorpusYahooL24();
			int nb = 0;
			System.out.println(nb + " queries generated");
			long end = System.currentTimeMillis();
			System.out.println((end - start) + " milliseconds");
			
			// one of "trec_web", "trec_million", trec_test
			start = System.currentTimeMillis();
			nb = corpus.generateCorpus("trec_500", 3000);
			System.out.println(nb + " queries generated");
			end = System.currentTimeMillis();
			System.out.println((end - start) + " milliseconds");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}


