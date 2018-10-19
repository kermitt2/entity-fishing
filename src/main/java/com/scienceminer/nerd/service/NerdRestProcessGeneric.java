package com.scienceminer.nerd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class NerdRestProcessGeneric {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestProcessGeneric.class);
	
	/**
	 * Returns a string containing true, if the service is alive.
	 * 
	 * @return returns a response object containing the string true if service
	 *         is alive.
	 */
	public static Response isAlive() {
		Response response = null;
		try {
			LOGGER.debug("Called isAlive()...");

			String retVal = null;
			try {
				retVal = Boolean.valueOf(true).toString();
			} catch (Exception e) {
				LOGGER.error("Nerd service is not alive. ", e);
				retVal = Boolean.valueOf(false).toString();
			}
			response = Response.status(Status.OK).entity(retVal).build();
		} catch (Exception e) {
			LOGGER.error("Exception occurred while check if the service is alive. " + e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		return response;
	}

	/**
	 * Returns a string containing true, if the service is alive.
	 *
	 * @return returns a response object containing the string true if service
	 *         is alive.
	 */
	public static Response version() {
		Response response = null;
		NerdRestProcessQuery nerdRestProcessQuery = null;
		SoftwareInfo softwareInfo = null;
		try {
			LOGGER.debug("Called version...");
			nerdRestProcessQuery = new NerdRestProcessQuery();
			softwareInfo = nerdRestProcessQuery.getSoftwareInfo();

		} catch (Exception e) {
			LOGGER.error("Exception occurred while getting the version of this application. " + e);
		}
		return response;
	}

}
