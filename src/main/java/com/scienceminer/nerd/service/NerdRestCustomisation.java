package com.scienceminer.nerd.service;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.scienceminer.nerd.utilities.NerdProperty;
import com.scienceminer.nerd.utilities.SHA1;
import com.scienceminer.nerd.utilities.ChangePropertyParser;
import com.scienceminer.nerd.utilities.NerdPropertiesUtil;
import com.scienceminer.nerd.utilities.NerdProperty;
import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.utilities.NerdRestUtils;
import com.scienceminer.nerd.utilities.NerdServiceProperties;
import com.scienceminer.nerd.kb.Customisations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class for implemeting the services to manage the customization in NERD.  
 *
 * 
 */
public class NerdRestCustomisation {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestCustomisation.class);

	/**
     * Return the list of existing customisations.
	 */
	public static Response processNerdCustomisations() {
		LOGGER.debug(">> processNerdCustomisations");
		Response response = null;
		try {
			Customisations customisations = Customisations.getInstance();
			List<String> names = customisations.getCustomisations();
			StringBuffer res = new StringBuffer();
			res.append("[");
			boolean begin = true;
			for(String name : names) {
				if (begin)
					begin = false;
				else 
					res.append(", ");
				res.append("\""+name+"\"");
			}
			res.append("]");
			response = Response.status(Status.OK).entity(res.toString()).type(MediaType.APPLICATION_JSON).build();
		}
		catch (Exception exp) {
			LOGGER.error("Error when accessing the list of existing customisations. ", exp);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug("<< processNerdCustomisations");
		return response;
	}
	
	/**
     * Return the data of an existing customisation.
	 */
	public static Response processNerdCustomisation(String name) {
		LOGGER.debug(">> processNerdCustomisation");
		Response response = null;
		try {
			Customisations customisations = Customisations.getInstance();
			String info = customisations.getCustomisation(name);
			if (info.startsWith("Resource was not found")) 
				response = Response.status(Status.NOT_FOUND).build();
			else if (info.startsWith("Server error")) 
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
			else
				response = Response.status(Status.OK).entity(info).type(MediaType.APPLICATION_JSON).build();
		}
		catch (Exception exp) {
			LOGGER.error("Error when accessing the list of existing customisations. ", exp);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug("<< processNerdCustomisation");
		return response;
	}
	
	/**
     * Return the list of existing customisations.
	 */
	public static Response processCreateNerdCustomisation(String name, String profile) {
		LOGGER.debug(">> processCreateNerdCustomisation");
		Response response = null;
		try {
			Customisations customisations = Customisations.getInstance();
			String message = customisations.createCustomisation(name, profile);
			if (message.equals("OK"))
				response = Response.status(Status.OK).build();
			else if (message.startsWith("Invalid request")) 
				response = Response.status(Status.BAD_REQUEST).entity(message).type(MediaType.TEXT_PLAIN).build();
			else if (message.startsWith("Customisation already created")) {
				response = Response.status(Status.BAD_REQUEST).entity(message).type(MediaType.TEXT_PLAIN).build();
			}
			else 
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		catch (Exception exp) {
			LOGGER.error("Error creating customisation. ", exp);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		LOGGER.debug("<< processCreateNerdCustomisation");
		return response;
	}

	/**
     * ...
	 */
	public static Response processExtendNerdCustomisation(String name, String profile) {
		LOGGER.debug(">> processExtendNerdCustomisation");
		Response response = null;
		
		LOGGER.debug("<< processExtendNerdCustomisation");
		return response;
	}

	/**
     * 
	 */
	public static Response processDeleteNerdCustomisation(String name) {
		LOGGER.debug(">> processDeleteNerdCustomisation");
		Response response = null;
		try {
			Customisations customisations = Customisations.getInstance();
			String message = customisations.deleteCustomisation(name);
			if (message.equals("OK"))
				response = Response.status(Status.OK).build();
			else if (message.startsWith("Resource was not found"))
				response = Response.status(Status.NOT_FOUND).build();
			else
				response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		catch (Exception exp) {
			LOGGER.error("Error deleting customisation: " + name, exp);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		LOGGER.debug("<< processDeleteNerdCustomisation");
		return response;
	}
}

