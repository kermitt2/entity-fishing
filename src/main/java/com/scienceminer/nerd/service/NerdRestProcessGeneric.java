package com.scienceminer.nerd.service;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Patrice Lopez
 *
 */
public class NerdRestProcessGeneric {
	
	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(NerdRestProcessGeneric.class);
	
	/**
	 * Returns a string containing true, if the service is alive.
	 * 
	 * @return returns a response object containing the string true if service
	 *         is alive.
	 */
	public static Response isAlive() {
		Response response = null;
		try {
			LOGGER.debug("called isAlive()...");

			String retVal = null;
			try {
				retVal = Boolean.valueOf(true).toString();
			} catch (Exception e) {
				LOGGER.error("Nerd service is not alive, because of: ", e);
				retVal = Boolean.valueOf(false).toString();
			}
			response = Response.status(Status.OK).entity(retVal).build();
		} catch (Exception e) {
			LOGGER.error("" + e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		return response;
	}
	
	/**
	 * Returns the description of how to use the nerd-service in a human
	 * readable way (html).
	 * 
	 * @return returns a response object containing a html description
	 */
	public static Response getDescription_html(UriInfo uriInfo) {
		Response response = null;
		try {
			LOGGER.debug("called getDescription_html()...");

			StringBuffer htmlCode = new StringBuffer();

			htmlCode.append("<h4>nerd-service documentation</h4>");
			htmlCode.append("This service provides a RESTful interface for using the query " + 
				"enrichment and disambiguation system, also know as (N)ERD. ");
			
			response = Response.status(Status.OK).entity(htmlCode.toString())
					.type(MediaType.TEXT_HTML).build();
		} catch (Exception e) {
			LOGGER.error(
					"Cannot response the description for nerd-service. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		return response;
	}

}
