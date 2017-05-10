package com.scienceminer.nerd.service;

import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Collections;
import java.io.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.HttpHeaders; 

import com.scienceminer.nerd.utilities.NerdRestUtils;
import com.scienceminer.nerd.utilities.NerdServiceProperties;

import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.lang.Language;

import com.scienceminer.nerd.disambiguation.ProcessText;
import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.disambiguation.Sentence;
import com.scienceminer.nerd.disambiguation.WeightedTerm;
import org.grobid.core.data.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONException;
import org.json.JSONStringer;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

/**
 *  DEPRECATED ! DEPRECATED ! DEPRECATED ! DEPRECATED ! DEPRECATED ! DEPRECATED ! DEPRECATED ! DEPRECATED ! 
 * 
 * @author Patrice
 * 
 */
public class NerdRestProcessString {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestProcessString.class);

	/**
	 *  Apply a language identification on the raw text and return the identified language with a 
	 *  confidence score.
	 * 
	 *  @param text
	 *            raw text string      
	 *
	 *  @return a response object containing the identified language with a confidence score.
	 */
	public static Response processLIdText(String text) {
		LOGGER.debug(methodLogIn());       
		Response response = null;
		String retVal = null;
		try {
			LOGGER.debug(">> set raw text for stateless service'...");
System.out.println("lang id:" + text);			
			LanguageUtilities languageIdentifier = LanguageUtilities.getInstance();       				
			Language result = null;
			synchronized (languageIdentifier) {       
				result = languageIdentifier.runLanguageId(text);  
			}
       
	 		if (result != null) {
				JSONStringer stringer = new JSONStringer();     
			
				stringer.object();  
				stringer.key("lang").value(result.getLang());  			
				stringer.key("conf").value(result.getConf());  		   	
				stringer.endObject();
			
				retVal = stringer.toString(); 
			}
			
			if (!NerdRestUtils.isResultOK(retVal)) {
				response = Response.status(Status.NO_CONTENT).build();
			} 
			else {
				response = Response.status(Status.OK).entity(retVal)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
					.build();
			}
		}
		catch(NoSuchElementException nseExp) {
			LOGGER.error("Could not get an WSD tagger instance. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		} 
		catch(JSONException ex) {
			LOGGER.error("Error when building the JSON response string.");  
			System.out.println("Error when building the JSON response string.");  
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();      
		}		
		catch(Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug(methodLogOut());        
		
		return response;
	}	
		
	
	/**
	 *  Apply a sentence segmentation on the raw text and return offset of the different 
	 *  sentences.
	 * 
	 *  @param text
	 *            raw text string      
	 *
	 *  @return a response object containing the offsets of the identified sentences.
	 */
	public static Response processSentenceSegmentation(String text) {
		LOGGER.debug(methodLogIn());       
		Response response = null;
		String retVal = null;
		try {
			LOGGER.debug(">> set raw text for stateless service'...");
			
			ProcessText processText = ProcessText.getInstance();
			List<Sentence> sentences = processText.sentenceSegmentation(text);
			StringBuffer buffer = new StringBuffer();
			
			if ( (text == null) || (text.length() == 0) ) {
				response = Response.status(Status.NO_CONTENT).build();
			}
			else if ( (sentences != null) && (sentences.size() > 0) ) {
				buffer.append("{ \"sentences\" : [ ");
				boolean start = true;
				for(Sentence sentence : sentences) {
					if (start) {
						buffer.append(sentence.toJSON());
						start = false;
					}
					else
						buffer.append(", " + sentence.toJSON());
				}
				buffer.append(" ] }");
				
				response = Response.status(Status.OK).entity(buffer.toString())
						.type(MediaType.APPLICATION_JSON).build();
			}
			else {
				// only one sentence
				buffer.append("{ \"sentences\" : [ ");
				buffer.append(" { \"offsetStart\" : 0, \"offsetEnd\" : " + text.length() + " } ");
				buffer.append(" ] }");
				
				response = Response.status(Status.OK).entity(buffer.toString())
						.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
						.build();
			} 
		}
		catch(NoSuchElementException nseExp) {
			LOGGER.error("Could not get an WSD tagger instance. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		} 
		catch(JSONException ex) {
			LOGGER.error("Error when building the JSON response string.");  
			System.out.println("Error when building the JSON response string.");  
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();      
		}		
		catch(Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug(methodLogOut());        
	
		return response;
	}

	/**
	 * Apply NER and disambiguation on a textual object.
	 * 
	 * @param the
	 *            raw text to be processed
	 * @return a response JSON object containing the identified and resolved entities.
	 */
	public static Response processNERDText(String text, 
										boolean onlyNER, 
										boolean nbest,
										boolean sentenceSegmentation,
										NerdRestUtils.Format output,
										String customisation) {
		LOGGER.debug(methodLogIn()); 
		Response response = null;
		String retVal = null;
		try {
			LOGGER.debug(">> set raw text for stateless service processNERDText...");
			long start = System.currentTimeMillis();
//System.out.println(text);
			NerdQuery nerdQuery = new NerdQuery();
			nerdQuery.setText(text);
			nerdQuery.setOnlyNER(onlyNER);
			nerdQuery.setNbest(nbest);
			nerdQuery.setSentence(sentenceSegmentation);
			nerdQuery.setFormat(output);
			nerdQuery.setCustomisation(customisation);
			
			// language identification
			{
				LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
				Language lang = languageUtilities.runLanguageId(text);
				nerdQuery.setLanguage(lang);
			
				if ( (lang == null) || (lang.getLang() == null) ) {
					response = Response.status(Status.NOT_ACCEPTABLE).build();
					LOGGER.debug(methodLogOut());  
					return response;
				}
				else {
					LOGGER.debug(">> identified language: " + lang.toString());
					String theLang = lang.getLang();
					double theScore	= lang.getConf();
					if ( !theLang.equals("en") && !theLang.equals("de") && !theLang.equals("fr") ) {
						response = Response.status(Status.NOT_ACCEPTABLE).build();
						LOGGER.debug(methodLogOut());  
						return response;
					}
					if (theScore < 0.7) {
						response = Response.status(Status.NOT_ACCEPTABLE).build();
						LOGGER.debug(methodLogOut());  
						return response;
					}
				}
			}
			
			// ner
			ProcessText processText = ProcessText.getInstance();
			if (sentenceSegmentation) {
				List<Sentence> sentences = processText.sentenceSegmentation(text);
				nerdQuery.setSentences(sentences);
			}
			List<Entity> entities = new ArrayList<Entity>();
			if (nerdQuery.getLanguage().getLang().equals("en") || nerdQuery.getLanguage().getLang().equals("fr")) {
				entities = processText.process(nerdQuery);
			}
			if (!onlyNER) {
				List<Entity> entities2 = processText.processBrutal(nerdQuery);
				for(Entity entity : entities2) {
					// we add entities only if the mention is not already present
					if (!entities.contains(entity))
						entities.add(entity);
				}
			}
//for(Entity entity : entities)
//System.out.println(entity.toString());
			if (entities != null) {
				// disambiguate and solve entity mentions
				List<NerdEntity> disambiguatedEntities = null;
				disambiguatedEntities = new ArrayList<NerdEntity>();
				for (Entity entity : entities) {
					NerdEntity nerdEntity = new NerdEntity(entity);
					disambiguatedEntities.add(nerdEntity);
				}

				if (!onlyNER) {
					NerdEngine disambiguator = NerdEngine.getInstance();
					nerdQuery.setEntities(disambiguatedEntities);
					disambiguatedEntities = disambiguator.disambiguate(nerdQuery);
				}
				else {
					for (NerdEntity entity : disambiguatedEntities) {
						entity.setNerdScore(entity.getNer_conf());
					}
				}
				nerdQuery.setEntities(disambiguatedEntities);
			}

			long end = System.currentTimeMillis();
			nerdQuery.setRuntime(end - start);
System.out.println("runtime: " + (end - start));
			
			// sort the entities
			Collections.sort(nerdQuery.getEntities());
			String json = nerdQuery.toJSONCompactClean();
			if (json == null) {
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			else {
				response = Response.status(Status.OK).entity(json)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
					.build();
			}
		}
		catch(NoSuchElementException nseExp) {
			LOGGER.error("Could not get an WSD tagger instance. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		} 
		catch(JSONException ex) {
			LOGGER.error("Error when building the JSON response string.", ex);  
			System.out.println("Error when building the JSON response string."); 
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();      
		}		
		catch(Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug(methodLogOut());        
		
		return response;
	}


	/**
	 * Disambiguation on a textual object without applying NER.
	 * 
	 * @param the
	 *            raw text to be processed
	 * @return a response JSON object containing the identified and resolved entities.
	 */
	public static Response processERDText(String text, 
										boolean onlyNER, 
//										boolean shortText, 
										boolean nbest,
										boolean sentenceSegmentation,
										NerdRestUtils.Format output,
										String customisation) {
		LOGGER.debug(methodLogIn()); 
		Response response = null;
		String retVal = null;
		try {
			LOGGER.debug(">> set raw text for stateless service processERDText...");
			long start = System.currentTimeMillis();
//System.out.println(text);			
			NerdQuery nerdQuery = new NerdQuery();
			nerdQuery.setText(text);
			nerdQuery.setOnlyNER(onlyNER);
			nerdQuery.setNbest(nbest);
			nerdQuery.setSentence(sentenceSegmentation);
//			nerdQuery.setShortText(shortText);
			nerdQuery.setFormat(output);
			nerdQuery.setCustomisation(customisation);
			
			//if (!shortText) 
			{
				// language identification
				LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
				Language lang = languageUtilities.runLanguageId(text);
				nerdQuery.setLanguage(lang);
				LOGGER.debug(">> identified language: " + lang.toString());
			
				if ( (lang == null) || (lang.getLang() == null) ) {
					response = Response.status(Status.NOT_ACCEPTABLE).build();
					LOGGER.debug(methodLogOut());  
					return response;
				}
				else {
					String theLang = lang.getLang();
					if ( !theLang.equals("en") && !theLang.equals("de") && !theLang.equals("fr") ) {
						response = Response.status(Status.NOT_ACCEPTABLE).build();
						LOGGER.debug(methodLogOut());  
						return response;
					}
				}
			}
			
			// ner
			ProcessText processText = ProcessText.getInstance();
			if (sentenceSegmentation) {
				List<Sentence> sentences = processText.sentenceSegmentation(text);
				nerdQuery.setSentences(sentences);
			}
			List<Entity> entities = processText.processBrutal(nerdQuery);
			
			if (entities != null) {
				// disambiguate and solve entity mentions
				List<NerdEntity> disambiguatedEntities = null;
				disambiguatedEntities = new ArrayList<NerdEntity>();
				for (Entity entity : entities) {
					NerdEntity nerdEntity = new NerdEntity(entity);
					disambiguatedEntities.add(nerdEntity);
				}

				NerdEngine disambiguator = NerdEngine.getInstance();
				nerdQuery.setEntities(disambiguatedEntities);
				disambiguatedEntities = disambiguator.disambiguate(nerdQuery);
	
				nerdQuery.setEntities(disambiguatedEntities);
			}

			long end = System.currentTimeMillis();
			nerdQuery.setRuntime(end - start);
			
			// sort the entities
			Collections.sort(nerdQuery.getEntities());
			String json = nerdQuery.toJSONCompactClean();
			if (json == null) {
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			else {
				response = Response.status(Status.OK).entity(json)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
					.build();
			}
		}
		catch(NoSuchElementException nseExp) {
			LOGGER.error("Could not get an WSD tagger instance. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		} 
		catch(JSONException ex) {
			LOGGER.error("Error when building the JSON response string.", ex);  
			System.out.println("Error when building the JSON response string."); 
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();      
		}		
		catch(Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug(methodLogOut());        
		
		return response;
	}


	/**
	 * Disambiguation on a weighted term vector.
	 * 
	 * @param the weighted
	 *            term vector to be processed
	 * @return a response JSON object containing the identified and resolved entities as a weighted term vector.
	 */
	public static Response processWeigthedTermVector(String text, 
										boolean onlyNER, 
										boolean nbest,
										NerdRestUtils.Format output,
										String customisation) {
		LOGGER.debug(methodLogIn()); 
		Response response = null;
		String retVal = null;
		try {
			LOGGER.debug(">> set raw text for stateless service processWeigthedTermVector...");
			long start = System.currentTimeMillis();
//System.out.println(text);			
			NerdQuery nerdQuery = new NerdQuery();
			// parse the JSON chunk
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonRoot = mapper.readTree(text);
			List<WeightedTerm> terms = new ArrayList<WeightedTerm>();
			if (jsonRoot.isArray()) {
			    for (final JsonNode termNode : jsonRoot) {
					String term = null;
					double score = 0.0;
					
                    JsonNode idNode2 = termNode.findPath("term");
					if ((idNode2 != null) && (!idNode2.isMissingNode())) {
            			term = idNode2.textValue();
					}
					
                    idNode2 = termNode.findPath("score");
					if ((idNode2 != null) && (!idNode2.isMissingNode())) {
            			score = idNode2.doubleValue();
					}
					
					if ( (term != null) && (score != 0.0) ) {
						WeightedTerm wTerm = new WeightedTerm();
						wTerm.setTerm(term);
						wTerm.setScore(score);
						terms.add(wTerm);
					}
			    }
			}
			
			nerdQuery.setTermVector(terms);
			
			nerdQuery.setOnlyNER(onlyNER);
			nerdQuery.setNbest(nbest);
			nerdQuery.setFormat(output);
			nerdQuery.setCustomisation(customisation);
			
			// reformat text content
			StringBuilder textContent = new StringBuilder();
			for(WeightedTerm wt : terms) {
				textContent.append(" " + wt.getTerm());
			}
			text = textContent.toString();
			
			// language identification
			LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
			Language lang = languageUtilities.runLanguageId(text);
			nerdQuery.setLanguage(lang);
			LOGGER.debug(">> identified language: " + lang.toString());
		
			if ( (lang == null) || (lang.getLang() == null) ) {
				response = Response.status(Status.NOT_ACCEPTABLE).build();
				LOGGER.debug(methodLogOut());  
				return response;
			}
			else {
				String theLang = lang.getLang();
				if ( !theLang.equals("en") && !theLang.equals("de") && !theLang.equals("fr") ) {
					response = Response.status(Status.NOT_ACCEPTABLE).build();
					LOGGER.debug(methodLogOut());  
					return response;
				}
			}
			
			// create the entities from the weighted terms
			//ProcessText processText = ProcessText.getInstance();
			//List<Entity> entities = processText.processWeightedTerms(terms);			
			
			if (terms != null) {
				// disambiguate and solve term mentions
				/*List<NerdEntity> disambiguatedEntities = null;
				disambiguatedEntities = new ArrayList<NerdEntity>();
				for (Entity entity : entities) {
					NerdEntity nerdEntity = new NerdEntity(entity);
					disambiguatedEntities.add(nerdEntity);
				}*/

				NerdEngine disambiguator = NerdEngine.getInstance();
				//nerdQuery.setEntities(disambiguatedEntities);
				//disambiguatedEntities = disambiguator.disambiguate(nerdQuery, true, false);
				// add the NerdEntity objects to the WeightedTerm object if disambiguation 
				// is successful
				disambiguator.disambiguateWeightedTerms(nerdQuery);
				//nerdQuery.setEntities(disambiguatedEntities);
			}

			long end = System.currentTimeMillis();
			nerdQuery.setRuntime(end - start);
			
			// sort the entities
			//Collections.sort(nerdQuery.getEntities());
			String json = nerdQuery.toJSONCompactClean();
			if (json == null) {
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			else {
				response = Response.status(Status.OK).entity(json)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
					.build();
			}
		}
		catch(NoSuchElementException nseExp) {
			LOGGER.error("Could not get an WSD tagger instance. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		} 
		catch(JSONException ex) {
			LOGGER.error("Error when building the JSON response string.", ex);  
			System.out.println("Error when building the JSON response string."); 
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();      
		}		
		catch(Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug(methodLogOut());        
		
		return response;
	}


	/**
	 * @return
	 */
	public static String methodLogIn() {
		return ">> " + NerdRestProcessString.class.getName() + "." + 
			Thread.currentThread().getStackTrace()[1].getMethodName();
	}

	/**
	 * @return
	 */
	public static String methodLogOut() {
		return "<< " + NerdRestProcessString.class.getName() + "." + 
			Thread.currentThread().getStackTrace()[1].getMethodName();
	}

}
