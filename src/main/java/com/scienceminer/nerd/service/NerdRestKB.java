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
 * 
 * REST service to access data in the knowledge base
 * 
 * @author Patrice
 * 
 */
public class NerdRestKB {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestKB.class);

	/**
	 *  Get the information for a concept.
	 * 
	 *  @param id of the concept      
	 *
	 *  @return a response object containing the information related to the identified concept.
	 */
	public static Response getConceptInfo(String text) {
		LOGGER.debug(methodLogIn());       
		Response response = null;
		String retVal = null;
		try {
			LOGGER.debug(">> set raw text for stateless service'...");
			
			String json = null;
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
			LOGGER.error("Could not get a KB instance. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		} 
		/*catch(JSONException ex) {
			LOGGER.error("Error when building the JSON response string.", ex);  
			System.out.println("Error when building the JSON response string."); 
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();      
		}*/		
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
		return ">> " + NerdRestKB.class.getName() + "." + 
			Thread.currentThread().getStackTrace()[1].getMethodName();
	}

	/**
	 * @return
	 */
	public static String methodLogOut() {
		return "<< " + NerdRestKB.class.getName() + "." + 
			Thread.currentThread().getStackTrace()[1].getMethodName();
	}
}