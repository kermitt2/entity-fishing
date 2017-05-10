package com.scienceminer.nerd.service;

import java.io.InputStream;
import java.io.IOException;
import java.util.List; 

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.Lexicon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;

/**
 * 
 * RESTful service for the NERD system.
 *
 * @author Patrice
 *
 */

@Singleton
@Path(NerdPaths.PATH_NERD)
public class NerdRestService implements NerdPaths {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestService.class);

	private static final String SHA1 = "sha1";
	private static final String QUERY = "query";
	private static final String XML = "xml";
	private static final String NERD = "nerd";
	private static final String TEXT = "text"; 
	private static final String TERM = "term"; 
	private static final String ID = "id"; 
	private static final String LANG = "lang"; 
	private static final String ONLY_NER = "onlyNER";
	private static final String NBEST = "nbest";
	private static final String SENTENCE = "sentence";
	private static final String FORMAT = "format";
	private static final String CUSTOMISATION = "customisation";

	public NerdRestService() {
		LOGGER.info("Init Servlet NerdRestService.");
		NerdServiceProperties.getInstance();
		//Utilities.initGrobid();
		LOGGER.info("Init of Servlet NerdRestService finished.");
		LOGGER.info("Init lexicon and KB resources.");
		Lexicon.getInstance();
		LOGGER.info("Init lexicon and KB resources finished.");
	}

	/**
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessGeneric#isAlive()
	 */
	@Path(NerdPaths.PATH_IS_ALIVE)
	@Produces(MediaType.TEXT_PLAIN)
	@GET
	public Response isAlive() {
		return NerdRestProcessGeneric.isAlive();
	}

	/**
	 * 
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessGeneric#getDescription_html(UriInfo)
	 */
	@Produces(MediaType.TEXT_HTML)
	@GET
	@Path(NERD)
	public Response getDescription_html(@Context UriInfo uriInfo) {
		return NerdRestProcessGeneric.getDescription_html(uriInfo);
	}

	/**
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessAdmin#getAdminParams(String)
	 */
	@Path(PATH_ADMIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_HTML)
	@POST
	public Response getAdmin_htmlPost(@FormParam(SHA1) String sha1) {
		return NerdRestProcessAdmin.getAdminParams(sha1);
	}

	/**
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessAdmin#getAdminParams(String)
	 */
	@Path(PATH_ADMIN)
	//@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_HTML)
	@GET
	public Response getAdmin_htmlGet(@QueryParam(SHA1) String sha1) {
		return NerdRestProcessAdmin.getAdminParams(sha1);
	}

	/**
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessAdmin#processSHA1(String)
	 */
	@Path(PATH_SHA1)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	@POST
	public Response processSHA1Post(@FormParam(SHA1) String sha1) {
		return NerdRestProcessAdmin.processSHA1(sha1);
	}

	/**
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessAdmin#processSHA1(String)
	 */
	@Path(PATH_SHA1)
	//@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	@GET
	public Response processSHA1Get(@QueryParam(SHA1) String sha1) {
		return NerdRestProcessAdmin.processSHA1(sha1);
	}

	/**
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessAdmin#getAllPropertiesValues(String)
	 */
	@Path(PATH_ALL_PROPS)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	@POST
	public Response getAllPropertiesValuesPost(@FormParam(SHA1) String sha1) {
		return NerdRestProcessAdmin.getAllPropertiesValues(sha1);
	}

	/**
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessAdmin#getAllPropertiesValues(String)
	 */
	@Path(PATH_ALL_PROPS)
	//@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	@GET
	public Response getAllPropertiesValuesGet(@QueryParam(SHA1) String sha1) {
		return NerdRestProcessAdmin.getAllPropertiesValues(sha1);
	}

	/**
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessAdmin#changePropertyValue(String)
	 */
	@Path(PATH_CHANGE_PROPERTY_VALUE)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	@POST
	public Response changePropertyValuePost(@FormParam(XML) String xml) {
		return NerdRestProcessAdmin.changePropertyValue(xml);
	}

	/**
	 * @see com.scienceminer.nerd.service.process.NerdRestProcessAdmin#changePropertyValue(String)
	 */
	@Path(PATH_CHANGE_PROPERTY_VALUE)
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	@GET
	public Response changePropertyValueGet(@QueryParam(XML) String xml) {
		return NerdRestProcessAdmin.changePropertyValue(xml);
	}

	@Path(PATH_NERD_QUERY)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@GET
	public Response processQueryGet(String query) {
		return NerdRestProcessQuery.processQuery(query);
	}

	@POST
	@Path(PATH_NERD_QUERY)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processQuery(String query) {
		return NerdRestProcessQuery.processQuery(query);
	}

	// deprecated
	/*@Path(PATH_ERD_QUERY)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@GET
	public Response processERDQueryGet(String query) {
		return NerdRestProcessQuery.processERDQuery(query);
	}*/

	// deprecated
	/*@POST
	@Path(PATH_ERD_QUERY)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processERDQuery(String query) {
		return NerdRestProcessQuery.processERDQuery(query);
	}*/

	// deprecated
	/*@POST
	@Path(PATH_ERD_QUERY_TERMS)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processQueryTermVector(String query) {
		return NerdRestProcessQuery.processQueryTermVector(query);
	}*/

	// deprecated
	/*@Path(PATH_ERD_QUERY_TERMS)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@GET
	public Response processQueryTermVectorGet(String query) {
		return NerdRestProcessQuery.processQueryTermVector(query);
	}*/

	// deprecateds
	/*@Path(PATH_ERD_SEARCH_QUERY)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@GET
	public Response processERDSearchQueryGet(String query) {
		return NerdRestProcessQuery.processSearchQuery(query);        
	}*/

	// deprecated
	/*@POST
	@Path(PATH_ERD_SEARCH_QUERY)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processERDSearchQuery(String query) {
		return NerdRestProcessQuery.processSearchQuery(query);        
	}*/

	// deprecated
	/*@POST
	@Path(PATH_NER_TEXT)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processNERDTextPost(@QueryParam(TEXT) String text, 
									@DefaultValue("false") @QueryParam(ONLY_NER) boolean onlyNER,
									@DefaultValue("false") @QueryParam(NBEST) boolean nbest,
									@DefaultValue("false") @QueryParam(SENTENCE) boolean sentence,
									@DefaultValue("json") @QueryParam(FORMAT) String format,
									@DefaultValue("") @QueryParam(CUSTOMISATION) String customisation) {
		NerdRestUtils.Format form = null;
		if ( (format != null) && (format.length() > 0) ) {
			format = format.toUpperCase();
			form = NerdRestUtils.Format.valueOf(format);
		}
		return NerdRestProcessString.processNERDText(text, 
												onlyNER, 	// onlyNER 
												nbest, 		// nbest
												sentence, 	// sentence segmentation
												form, // output format
												customisation); 	// field customisation
	}*/

	// deprecated
	/*@GET
	@Path(PATH_NER_TEXT)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processNERDText(@QueryParam(TEXT) String text, 
									@DefaultValue("false") @QueryParam(ONLY_NER) boolean onlyNER,
									@DefaultValue("false") @QueryParam(NBEST) boolean nbest,
									@DefaultValue("false") @QueryParam(SENTENCE) boolean sentence,
									@DefaultValue("JSON") @QueryParam(FORMAT) String format,
									@DefaultValue("") @QueryParam(CUSTOMISATION) String customisation) {
		NerdRestUtils.Format form = null;
		if ( (format != null) && (format.length() > 0) ) {
			format = format.toUpperCase();
			form = NerdRestUtils.Format.valueOf(format);
		}
		return NerdRestProcessString.processNERDText(text, 
												onlyNER, 	// onlyNER 
												nbest, 		// nbest
												sentence, 	// sentence segmentation
												form, // output format
												customisation); 	// field customisation
	}*/

	/**
	 * @see com.scienceminer.nerd.service.NerdRestProcessString#processLIdText(String)
	 */
	@GET
	@Path(PATH_LID)	
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processLIdText(@QueryParam(TEXT) String text) {
		return NerdRestProcessString.processLIdText(text);        
	}
	
	/**
	 * @see com.scienceminer.nerd.service.NerdRestProcessString#processLIdText(String)
	 */
	@Path(PATH_LID)
	@POST
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processLIdText_post(@QueryParam(TEXT) String text) {
		return NerdRestProcessString.processLIdText(text);        
	}
	
	/**
	 */
	@GET
	@Path(PATH_SENTENCE_SEGMENTATION)	
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processSentenceSegmentation(@QueryParam(TEXT) String text) {
		return NerdRestProcessString.processSentenceSegmentation(text);        
	}

	/**
	 */
	@Path(PATH_SENTENCE_SEGMENTATION)
	@POST
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processSentenceSegmentation_post(@QueryParam(TEXT) String text) {
		return NerdRestProcessString.processSentenceSegmentation(text);        
	}
	
	/**
	 */
	@GET
	@Path(PATH_NER_CUSTOMISATIONS)	
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processNerdCustomisations() {
		return NerdRestCustomisation.processNerdCustomisations();        
	}
	
	/**
	 */
	@GET
	@Path(PATH_NER_CUSTOMISATION+"/{name}")	
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processNerdCustomisation(@PathParam("name") String name) {
		return NerdRestCustomisation.processNerdCustomisation(name);        
	}
	
	/**
	 */
	@PUT
	@Path("createNERDCustomisation/{name}")	
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processCreateNerdCustomisation(@PathParam("name") String name, String profile) {
		return NerdRestCustomisation.processCreateNerdCustomisation(name, profile);        
	}

	/**
	 */
	@POST
	@Path("createNERDCustomisation/{name}")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processCreateNerdCustomisation_post(@PathParam("name") String name, String profile) {
		return NerdRestCustomisation.processCreateNerdCustomisation(name, profile);        
	}

	/**
	 */
	@PUT
	@Path(PATH_NER_EXTEND_CUSTOMISATION+"/{name}")	
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processExtendNerdCustomisation(@PathParam("name") String name, String profile) {
		return NerdRestCustomisation.processExtendNerdCustomisation(name, profile);        
	}

	/**
	 */
	@POST
	@Path(PATH_NER_EXTEND_CUSTOMISATION+"/{name}")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processExtendNerdCustomisation_post(@PathParam("name") String name, String profile) {
		return NerdRestCustomisation.processExtendNerdCustomisation(name, profile);        
	}

	/**
	 */
	@Path(PATH_NER_DELETE_CUSTOMISATION+"/{name}")
	@DELETE
	public Response processDeleteNerdCustomisation(@PathParam("name") String name) {
		return NerdRestCustomisation.processDeleteNerdCustomisation(name);        
	}	
	
	/**
	 */
	/*@Path(PATH_NER_TERM_VECTOR+"/{name}")
	@DELETE
	public Response processWeigthedTermVector(@PathParam("name") String name) {
		return NerdRestCustomisation.processDeleteNerdCustomisation(name);        
	}*/
	
	/*@POST
	@Path(PATH_ERD_TERMS)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processWeigthedTermVectorPost(@QueryParam(TEXT) String text, 
									@DefaultValue("false") @QueryParam(ONLY_NER) boolean onlyNER,
									@DefaultValue("false") @QueryParam(NBEST) boolean nbest,
									@DefaultValue("JSON") @QueryParam(FORMAT) String format,
									@DefaultValue("") @QueryParam(CUSTOMISATION) String customisation) {
		NerdRestUtils.Format form = null;
		if ( (format != null) && (format.length() > 0) ) {
			format = format.toUpperCase();
			form = NerdRestUtils.Format.valueOf(format);
		}
		return NerdRestProcessString.processWeigthedTermVector(text, 
												onlyNER, 	// onlyNER 
												nbest, 		// nbest
												form, // output format
												customisation); 	// field customisation
	}
	*/
		/*
	@GET
	@Path(PATH_ERD_TERMS)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response processWeigthedTermVector(@QueryParam(TEXT) String text, 
									@DefaultValue("false") @QueryParam(ONLY_NER) boolean onlyNER,
									@DefaultValue("false") @QueryParam(NBEST) boolean nbest,
									@DefaultValue("JSON") @QueryParam(FORMAT) String format,
									@DefaultValue("") @QueryParam(CUSTOMISATION) String customisation) {
		NerdRestUtils.Format form = null;
		if ( (format != null) && (format.length() > 0) ) {
			format = format.toUpperCase();
			form = NerdRestUtils.Format.valueOf(format);
		}
		return NerdRestProcessString.processWeigthedTermVector(text, 
												onlyNER, 	// onlyNER 
												nbest, 		// nbest
												form, // output format
												customisation); 	// field customisation
	}
	*/

	@GET
	@Path(PATH_KB_CONCEPT)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response getConceptInformation(@QueryParam(ID) String identifier, 
									@DefaultValue("en") @QueryParam(LANG) String lang, 
									@DefaultValue("json") @QueryParam(FORMAT) String format) {
		NerdRestUtils.Format form = null;
		if ( (format != null) && (format.length() > 0) ) {
			format = format.toUpperCase();
			form = NerdRestUtils.Format.valueOf(format);
		}
		return NerdRestKB.getConceptInfo(identifier, lang, form); 	// field customisation
	}
	
	@GET
	@Path(PATH_KB_TERM_LOOKUP)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response getTermLookup(@QueryParam(TERM) String term, 
									@DefaultValue("en") @QueryParam(LANG) String lang, 
									@DefaultValue("json") @QueryParam(FORMAT) String format) {
		NerdRestUtils.Format form = null;
		if ( (format != null) && (format.length() > 0) ) {
			format = format.toUpperCase();
			form = NerdRestUtils.Format.valueOf(format);
		}
		return NerdRestKB.getTermLookup(term, lang, form); 	// field customisation
	}

}
