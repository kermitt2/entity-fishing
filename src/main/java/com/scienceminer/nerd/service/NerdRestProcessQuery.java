package com.scienceminer.nerd.service;

import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Collections;
import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.HttpHeaders; 

import com.scienceminer.nerd.utilities.NerdRestUtils;
import com.scienceminer.nerd.utilities.NerdServiceProperties;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.lang.Language;
import org.grobid.core.data.Entity;

import com.scienceminer.nerd.disambiguation.ProcessText;
import com.scienceminer.nerd.disambiguation.Sentence;

import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.disambiguation.NerdCategories;
import com.scienceminer.nerd.disambiguation.WeightedTerm;
import com.scienceminer.nerd.disambiguation.NerdEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

/**
 * 
 * @author Patrice
 * 
 */
public class NerdRestProcessQuery {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestProcessQuery.class);

	/**
	 * Parse a structured query and return the corresponding normalized enriched and disambiguated query object.
	 * 
	 * @param the
	 *            POJO query object
	 * @return a response query object containing the structured representation of
	 *         the enriched and disambiguated query.
	 */
	public static Response processQuery(String theQuery) {
		LOGGER.debug(methodLogIn());
		Response response = null;
		//boolean isparallelExec = NerdServiceProperties.isParallelExec();
		
//		System.out.println(theQuery);		
		LOGGER.debug(">> received query to process: " + theQuery);
		try {
			NerdQuery nerdQuery = null; 
			try {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				nerdQuery = mapper.readValue(theQuery, NerdQuery.class);
			}
			catch(JsonGenerationException e) {
				e.printStackTrace();
			}
			catch (JsonMappingException e) {
		       	e.printStackTrace();
			}
			catch(IOException e) {
				e.printStackTrace();
			}

			// we analyze the query object in order to determine the kind of object
			// to be processed
			LOGGER.debug(">> set query object for stateless service...");
			if (nerdQuery == null) 
				return Response.status(Status.BAD_REQUEST).build();	 

			/*if ( ((nerdQuery.getText() == null) || (nerdQuery.getText().trim().length() < 6)) &&
				 ((nerdQuery.getShortText() == null) || (nerdQuery.getShortText().trim().length() < 2)) &&
				 ((nerdQuery.getTermVector() == null) || (nerdQuery.getTermVector().getSize() == 0)) ) {
				return Response.status(Status.BAD_REQUEST).build();	
			}*/
			
			// the input types are currently mutually exclusive
			if ((nerdQuery.getText() != null) && (nerdQuery.getText().trim().length() > 5)) {
				nerdQuery.setShortText(null);
				response = processQueryText(nerdQuery);
			}
			else if ((nerdQuery.getShortText() != null) && (nerdQuery.getShortText().trim().length() > 1)) {
				nerdQuery.setText(null);
				response = processSearchQuery(nerdQuery);
			}
			else if ((nerdQuery.getTermVector() != null) && (nerdQuery.getTermVector().size() != 0)) {
				response = processQueryTermVector(nerdQuery);
			}
			else
				response = Response.status(Status.BAD_REQUEST).build();	 
		} catch (NoSuchElementException nseExp) {
			LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		} catch (Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
		}
		LOGGER.debug(methodLogOut());
		return response;
	}


	/**
	 * Parse a structured query and return the corresponding normalized enriched and disambiguated query object.
	 * 
	 * @param the
	 *            POJO query object
	 * @return a response query object containing the structured representation of
	 *         the enriched and disambiguated query.
	 */
	public static Response processQueryText(NerdQuery nerdQuery) {
		LOGGER.debug(methodLogIn());
		Response response = null;
		try {
			long start = System.currentTimeMillis();

			// language identification
			Language lang = nerdQuery.getLanguage();
			if ( (nerdQuery.getLanguage() == null) || (nerdQuery.getLanguage().getLang() == null) ) {
				LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
				lang = languageUtilities.runLanguageId(nerdQuery.getText());
				nerdQuery.setLanguage(lang);
				LOGGER.debug(">> identified language: " + lang.toString());
			}
			else {
				System.out.println("lang is already defined");
				LOGGER.debug(">> language already identified: " + nerdQuery.getLanguage().getLang().toString());
			}
			
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
			
			// entities originally from the query are marked as such
			List<NerdEntity> originalEntities = null;
			if  ( (nerdQuery.getEntities() != null) && (nerdQuery.getEntities().size() > 0) ) {
				for(NerdEntity entity : nerdQuery.getEntities()) {
					entity.setNer_conf(1.0);
					
					// do we have disambiguated entity information for the entity?
					if (entity.getWikipediaExternalRef() != -1) {
						entity.setOrigin(NerdEntity.Origin.USER);
						entity.setNerdScore(1.0);
					}
				}
				originalEntities = nerdQuery.getEntities();
			}
			
			// ner
			ProcessText processText = ProcessText.getInstance();
			Integer[] processSentence =  nerdQuery.getProcessSentence();
			List<Sentence> sentences = nerdQuery.getSentences();
			if ( (sentences == null) && (nerdQuery.getSentence() || (processSentence != null)) ) {
				sentences = processText.sentenceSegmentation(nerdQuery.getText());
				nerdQuery.setSentences(sentences);
			}
			List<Entity> entities = processText.process(nerdQuery);
			if (!nerdQuery.getOnlyNER()) {
				List<Entity> entities2 = processText.processBrutal(nerdQuery);
				for(Entity entity : entities2) {
					// we add entities only if the mention is not already present
					if (!entities.contains(entity))
						entities.add(entity);
				}
			}

			// we keep only entities not conflicting with the ones already present in the query
			List<NerdEntity> newEntities = new ArrayList<NerdEntity>();			
			if (entities != null) {
				int offsetPos = 0;
				int ind = 0;
				
				if (originalEntities == null)
					nerdQuery.setAllEntities(entities);
				else {
					for(Entity entity : entities) {
						int begin = entity.getOffsetStart();
						int end = entity.getOffsetEnd();
					
						if (ind >= originalEntities.size()) {
							NerdEntity theEntity = new NerdEntity(entity);
							newEntities.add(theEntity);
						}
						else if (end < originalEntities.get(ind).getOffsetStart()) {
							NerdEntity theEntity = new NerdEntity(entity);
							newEntities.add(theEntity);
						}
						else if ( (begin > originalEntities.get(ind).getOffsetStart()) &&
							(begin < originalEntities.get(ind).getOffsetEnd()) ) {
							continue;
						}
						else if ( (end > originalEntities.get(ind).getOffsetStart()) &&
						(end < originalEntities.get(ind).getOffsetEnd()) ) {
							continue;
						}
						else if (begin > originalEntities.get(ind).getOffsetEnd()) {
							while(ind < originalEntities.size()) {
								ind++;
								if (ind >= originalEntities.size()) {
									NerdEntity theEntity = new NerdEntity(entity);
									newEntities.add(theEntity);
									break;
								}
								if (begin < originalEntities.get(ind).getOffsetEnd()) {
									if (end < originalEntities.get(ind).getOffsetStart()) {
										NerdEntity theEntity = new NerdEntity(entity);
										newEntities.add(theEntity);
									}
									break;
								}
							}
						}
					}
					for(NerdEntity entity : originalEntities) {
						newEntities.add(entity);
					}
					nerdQuery.setEntities(newEntities);
				}
			}
			else {
				nerdQuery.setEntities(originalEntities);
			}
			
			// sort the entities
			Collections.sort(nerdQuery.getEntities());
			
			// disambiguate
			if (entities != null) {
				// disambiguate and solve entity mentions
				if (!nerdQuery.getOnlyNER()) {
					NerdEngine disambiguator = NerdEngine.getInstance();
					List<NerdEntity> disambiguatedEntities = disambiguator.disambiguate(nerdQuery);
					nerdQuery.setEntities(disambiguatedEntities);
					nerdQuery = NerdCategories.addCategoryDistribution(nerdQuery);
				}
				else {
					for (NerdEntity entity : nerdQuery.getEntities()) {
						entity.setNerdScore(entity.getNer_conf());
					}
				}
			}
			
			long end = System.currentTimeMillis();
			nerdQuery.setRuntime(end - start);
System.out.println("runtime: " + (end - start));
			
			Collections.sort(nerdQuery.getEntities());
			String json = nerdQuery.toJSONCompactClean(null);	
			if (json == null) {
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			else {
				response = Response.status(Status.OK).entity(json)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
					.build();
			}
		} 
		catch (NoSuchElementException nseExp) {
			LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		}
		catch (Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
		}
		LOGGER.debug(methodLogOut());
		return response;
	}



	// DEPRECATED - TO BE REMOVED
	/**
	 * Parse a structured query and return the corresponding normalized enriched and disambiguated query object.
	 * 
	 * @param the
	 *            POJO query object
	 * @return a response query object containing the structured representation of
	 *         the enriched and disambiguated query.
	 */
	/*public static Response processERDQuery(String theQuery) {
		LOGGER.debug(methodLogIn());
		Response response = null;
		//boolean isparallelExec = NerdServiceProperties.isParallelExec();
		
//		System.out.println(theQuery);		
		LOGGER.debug(">> received query to process: " + theQuery);
		try {
			long start = System.currentTimeMillis();
			NerdQuery nerdQuery = null; 

			try {
				ObjectMapper mapper = new ObjectMapper();
				//System.out.println(theQuery);				
				nerdQuery = mapper.readValue(theQuery, NerdQuery.class);
			}
			catch(JsonGenerationException e) {
				e.printStackTrace();
			}
			catch (JsonMappingException e) {
		       	e.printStackTrace();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			
			if ( (nerdQuery == null) || 
			  	 (nerdQuery.getText() == null) || 
				 (nerdQuery.getText().trim().length() < 6) ) {
				return Response.status(Status.BAD_REQUEST).build();	 
			}
			
			LOGGER.debug(">> set query object for stateless service...");
			
			//boolean shortText = nerdQuery.getShortText();
			
			// language identification
			// test first if the language is already indicated in the query structure
			//if (!shortText) 
			{
				Language lang = nerdQuery.getLanguage();
				if ( (lang == null) || (lang.getLang() == null) ) {
					LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
					try {
						lang = languageUtilities.runLanguageId(nerdQuery.getText());
					}
					catch(Exception e) {
						LOGGER.debug("exception language identifier for: " + nerdQuery.getText());
						//e.printStackTrace();
					}
					if ( (lang != null) && (lang.getLang() != null) ) {
						nerdQuery.setLanguage(lang);
						LOGGER.debug(">> identified language: " + lang.toString());
					}
				}
				else {
					System.out.println("lang is already defined");
					LOGGER.debug(">> language already identified: " + nerdQuery.getLanguage().getLang().toString());
				}
			
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
			
			// entities originally from the query are marked as such
			List<NerdEntity> originalEntities = null;
			if  ( (nerdQuery.getEntities() != null) && (nerdQuery.getEntities().size() > 0) ) {
				for(NerdEntity entity : nerdQuery.getEntities()) {
					entity.setNer_conf(1.0);
					
					// do we have disambiguated entity information for the entity?
					if (entity.getWikipediaExternalRef() != -1) {
						entity.setOrigin(NerdEntity.Origin.USER);
						entity.setNerdScore(1.0);
					}
				}
				originalEntities = nerdQuery.getEntities();
			}
			
			// possible entity mentions
			ProcessText processText = ProcessText.getInstance();
			Integer[] processSentence =  nerdQuery.getProcessSentence();
			List<Sentence> sentences = nerdQuery.getSentences();
			if ( (sentences == null) && (nerdQuery.getSentence() || (processSentence != null)) ) {
				sentences = processText.sentenceSegmentation(nerdQuery.getText());
				nerdQuery.setSentences(sentences);
			}
			List<Entity> entities = processText.processBrutal(nerdQuery);
			List<NerdEntity> newEntities = new ArrayList<NerdEntity>();
			
			if (entities != null) {
				// we keep only entities not conflicting with the ones already present in the query
				int offsetPos = 0;
				int ind = 0;
				
				if (originalEntities == null)
					nerdQuery.setAllEntities(entities);
				else {
					for(Entity entity : entities) {
						int begin = entity.getOffsetStart();
						int end = entity.getOffsetEnd();
					
						if (ind >= originalEntities.size()) {
							NerdEntity theEntity = new NerdEntity(entity);
							newEntities.add(theEntity);
						}
						else if (end < originalEntities.get(ind).getOffsetStart()) {
							NerdEntity theEntity = new NerdEntity(entity);
							newEntities.add(theEntity);
						}
						else if ( (begin > originalEntities.get(ind).getOffsetStart()) &&
							(begin < originalEntities.get(ind).getOffsetEnd()) ) {
							continue;
						}
						else if ( (end > originalEntities.get(ind).getOffsetStart()) &&
						(end < originalEntities.get(ind).getOffsetEnd()) ) {
							continue;
						}
						else if (begin > originalEntities.get(ind).getOffsetEnd()) {
							while(ind < originalEntities.size()) {
								ind++;
								if (ind >= originalEntities.size()) {
									NerdEntity theEntity = new NerdEntity(entity);
									newEntities.add(theEntity);
									break;
								}
								if (begin < originalEntities.get(ind).getOffsetEnd()) {
									if (end < originalEntities.get(ind).getOffsetStart()) {
										NerdEntity theEntity = new NerdEntity(entity);
										newEntities.add(theEntity);
									}
									break;
								}
							}
						}
					}
					for(NerdEntity entity : originalEntities) {
						newEntities.add(entity);
					}
					nerdQuery.setEntities(newEntities);
				}
			}
			else {
				nerdQuery.setEntities(originalEntities);
			}
			
			// sort the entities
			Collections.sort(nerdQuery.getEntities());
			
			if (entities != null) {
				// disambiguate and solve entity mentions
				NerdEngine disambiguator = NerdEngine.getInstance();
				List<NerdEntity> disambiguatedEntities = 
					disambiguator.disambiguate(nerdQuery, false);
				nerdQuery.setEntities(disambiguatedEntities);
				// calculate the global categories
				nerdQuery = NerdCategories.addCategoryDistribution(nerdQuery);
			}
			
			long end = System.currentTimeMillis();
			nerdQuery.setRuntime(end - start);

			Collections.sort(nerdQuery.getEntities());
			String json = nerdQuery.toJSONCompactClean();
			
			if (json == null) {
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			else {
				response = Response.status(Status.OK).entity(json)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
					.header("Access-Control-Allow-Origin", "*")
            		.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
					.build();
			}
		} 
		catch (NoSuchElementException nseExp) {
			LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		}
		catch (Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			e.printStackTrace();
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
		}
		LOGGER.debug(methodLogOut());
		return response;
	}*/
 
	/**
	 * Disambiguation a structured query specifying a weighted term vector and return the enriched term vector
	 * with the corresponding normalized and disambiguated terms.
	 * 
	 * @param the POJO query object with the weighted
	 *            term vector to be processed
	 * @return a response JSON object containing the weighted term vector with the resolved entities.
	 */
	public static Response processQueryTermVector(NerdQuery nerdQuery) {
		LOGGER.debug(methodLogIn());
		Response response = null;
		try {
			long start = System.currentTimeMillis();
			
			// language identification
			// test first if the language is already indicated in the query structure
			
			// reformat text content
			StringBuilder textContent = new StringBuilder();
			for(WeightedTerm wt : nerdQuery.getTermVector()) {
				textContent.append(" " + wt.getTerm());
			}
			String text = textContent.toString();
			Language lang = nerdQuery.getLanguage();
			if ( (lang == null) || (lang.getLang() == null) ) {
				LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
				try {
					lang = languageUtilities.runLanguageId(text);
				}
				catch(Exception e) {
					LOGGER.debug("exception language identifier for: " + text);
					//e.printStackTrace();
				}
				if ( (lang != null) && (lang.getLang() != null) ) {
					nerdQuery.setLanguage(lang);
					LOGGER.debug(">> identified language: " + lang.toString());
				}
			}
			else {
				System.out.println("lang is already defined");
				//lang.setConfidence(1.0);
				LOGGER.debug(">> language already identified: " + nerdQuery.getLanguage().getLang().toString());
			}
		
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
			
			NerdEngine disambiguator = NerdEngine.getInstance();
			//nerdQuery.setEntities(disambiguatedEntities);
			//disambiguatedEntities = disambiguator.disambiguate(nerdQuery, true, false);
			// add the NerdEntity objects to the WeightedTerm object if disambiguation 
			// is successful
			disambiguator.disambiguateWeightedTerms(nerdQuery);
			nerdQuery = NerdCategories.addCategoryDistribution(nerdQuery);

			long end = System.currentTimeMillis();
			nerdQuery.setRuntime(end - start);

			//Collections.sort(nerdQuery.getEntities());
			String json = nerdQuery.toJSONCompactClean(null);
			if (json == null) {
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			else {
				response = Response.status(Status.OK).entity(json)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
					.build();
			}
		} 
		catch (NoSuchElementException nseExp) {
			LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		}
		catch (Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			e.printStackTrace();
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
		}
		LOGGER.debug(methodLogOut());
		return response;
	}

	/**
	 * Disambiguation of the terms of a seach query.
	 * 
	 * @param the POJO query object with the search query and additional optional contextual information
	 * @return a response JSON object containing the search terms with the resolved entities.
	 */
	public static Response processSearchQuery(NerdQuery nerdQuery) {
		LOGGER.debug(methodLogIn());
		Response response = null;	
		try {
			long start = System.currentTimeMillis();
			
			//nerdQuery.setShortText(true);

			// language identification
			// test first if the language is already indicated in the query structure
			Language lang = nerdQuery.getLanguage();
			if ( (lang == null) || (lang.getLang() == null) ) {
				LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
				try {
					lang = languageUtilities.runLanguageId(nerdQuery.getShortText());
				}
				catch(Exception e) {
					LOGGER.debug("exception language identifier for: " + nerdQuery.getShortText());
					//e.printStackTrace();
				}
				if ( (lang != null) && (lang.getLang() != null) ) {
					nerdQuery.setLanguage(lang);
					LOGGER.debug(">> identified language: " + lang.toString());
				}
			}
			else {
				System.out.println("lang is already defined");
				LOGGER.debug(">> language already identified: " + nerdQuery.getLanguage().getLang().toString());
			}

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
			
			// entities originally from the query are marked as such
			List<NerdEntity> originalEntities = null;
			if  ( (nerdQuery.getEntities() != null) && (nerdQuery.getEntities().size() > 0) ) {
				for(NerdEntity entity : nerdQuery.getEntities()) {
					entity.setNer_conf(1.0);
					
					// do we have disambiguated entity information for the entity?
					if (entity.getWikipediaExternalRef() != -1) {
						entity.setOrigin(NerdEntity.Origin.USER);
						entity.setNerdScore(1.0);
					}
				}
				originalEntities = nerdQuery.getEntities();
			}
			
			// possible entity mentions
			ProcessText processText = ProcessText.getInstance();
			/*Integer[] processSentence =  nerdQuery.getProcessSentence();
			List<Sentence> sentences = nerdQuery.getSentences();
			if ( (sentences == null) && (nerdQuery.getSentence() || (processSentence != null)) ) {
				sentences = processText.sentenceSegmentation(nerdQuery.getText());
				nerdQuery.setSentences(sentences);
			}*/
			List<Entity> entities = processText.processBrutal(nerdQuery);
			List<NerdEntity> newEntities = new ArrayList<NerdEntity>();
			
			if (entities != null) {
				// we keep only entities not conflicting with the ones already present in the query
				int offsetPos = 0;
				int ind = 0;
				
				if (originalEntities == null)
					nerdQuery.setAllEntities(entities);
				else {
					for(Entity entity : entities) {
						int begin = entity.getOffsetStart();
						int end = entity.getOffsetEnd();
					
						if (ind >= originalEntities.size()) {
							NerdEntity theEntity = new NerdEntity(entity);
							newEntities.add(theEntity);
						}
						else if (end < originalEntities.get(ind).getOffsetStart()) {
							NerdEntity theEntity = new NerdEntity(entity);
							newEntities.add(theEntity);
						}
						else if ( (begin > originalEntities.get(ind).getOffsetStart()) &&
							(begin < originalEntities.get(ind).getOffsetEnd()) ) {
							continue;
						}
						else if ( (end > originalEntities.get(ind).getOffsetStart()) &&
						(end < originalEntities.get(ind).getOffsetEnd()) ) {
							continue;
						}
						else if (begin > originalEntities.get(ind).getOffsetEnd()) {
							while(ind < originalEntities.size()) {
								ind++;
								if (ind >= originalEntities.size()) {
									NerdEntity theEntity = new NerdEntity(entity);
									newEntities.add(theEntity);
									break;
								}
								if (begin < originalEntities.get(ind).getOffsetEnd()) {
									if (end < originalEntities.get(ind).getOffsetStart()) {
										NerdEntity theEntity = new NerdEntity(entity);
										newEntities.add(theEntity);
									}
									break;
								}
							}
						}
					}
					for(NerdEntity entity : originalEntities) {
						newEntities.add(entity);
					}
					nerdQuery.setEntities(newEntities);
				}
			}
			else {
				nerdQuery.setEntities(originalEntities);	
			}
			
			// sort the entities
			Collections.sort(nerdQuery.getEntities());
			
			if (entities != null) {
				// disambiguate and solve entity mentions
				NerdEngine disambiguator = NerdEngine.getInstance();
				List<NerdEntity> disambiguatedEntities = disambiguator.disambiguate(nerdQuery); 
				nerdQuery.setEntities(disambiguatedEntities);
				// calculate the global categories
				nerdQuery = NerdCategories.addCategoryDistribution(nerdQuery);
			}
			
			long end = System.currentTimeMillis();
			nerdQuery.setRuntime(end - start);

			Collections.sort(nerdQuery.getEntities());
			String json = nerdQuery.toJSONCompactClean(null);
			
			if (json == null) {
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			else {
				response = Response.status(Status.OK).entity(json)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
					.header("Access-Control-Allow-Origin", "*")
            		.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
					.build();
			}
		} 
		catch (NoSuchElementException nseExp) {
			LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		}
		catch (Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			e.printStackTrace();
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
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
