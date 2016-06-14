package com.scienceminer.nerd.service;

import java.util.Properties;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Patrice Lopez
 * 
 */
public class NerdRestProcessAdmin {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(NerdRestProcessAdmin.class);

	/**
	 * Returns the admin view of all properties used for running Nerd.
	 * 
	 * @param sha1
	 *            the password
	 * 
	 * @return returns a response object containing the admin infos in html
	 *         syntax.
	 */
	public static Response getAdminParams(String sha1) {
		Response response = null;
		try {
			LOGGER.debug("called getDescription_html()...");
			String pass = NerdServiceProperties.getAdminPw();
			if (StringUtils.isNotBlank(pass) && StringUtils.isNotBlank(sha1)
					&& pass.equals(SHA1.getSHA1(sha1))) {
				StringBuffer htmlCode = new StringBuffer();

				htmlCode.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
				htmlCode.append("<html>");
				htmlCode.append("<head>");
				htmlCode.append("<title>nerd-service - admin</title>");
				htmlCode.append("</head>");
				htmlCode.append("<body>");
				htmlCode.append("<table border=\"1\">");
				htmlCode.append("<tr><td>property</td><td>value</td></tr>");
				htmlCode.append("<th><td colspan=\"2\">java properties</td></th>");
				htmlCode.append("<tr><td>os name</td><td>"
						+ System.getProperty("os.name") + "</td></tr>");
				htmlCode.append("<tr><td>os version</td><td>"
						+ System.getProperty("sun.arch.data.model")
						+ "</td></tr>");
				htmlCode.append("<th><td colspan=\"2\">Nerd_service.properties</td></th>");

				Properties props = NerdServiceProperties.getProps();
				for (Object property : props.keySet()) {
					htmlCode.append("<tr><td>" + property + "</td><td>"
							+ props.getProperty((String) property)
							+ "</td></tr>");
				}
				htmlCode.append("<th><td colspan=\"2\">nerd.properties</td></th>");
				props = NerdProperties.getProps();
				for (Object property : props.keySet()) {
					htmlCode.append("<tr><td>" + property + "</td><td>"
							+ props.getProperty((String) property)
							+ "</td></tr>");
				}

				htmlCode.append("</table>");
				htmlCode.append("</body>");
				htmlCode.append("</html>");

				response = Response.status(Status.OK)
						.entity(htmlCode.toString()).type(MediaType.TEXT_HTML)
						.build();
			} else {
				response = Response.status(Status.FORBIDDEN).build();
			}
		} catch (Exception e) {
			LOGGER.error(
					"Cannot response the description for Nerd-service. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		return response;
	}

	/**
	 * Process SHA1.
	 * 
	 * @param sha1
	 *            string to hash
	 * @return Response containing the value hashed.
	 */
	public static Response processSHA1(String sha1) {
		LOGGER.debug(">> processSHA1");
		Response response = null;
		String retVal = null;
		try {
			retVal = SHA1.getSHA1(sha1);
			if (NerdRestUtils.isResultOK(retVal)) {
				response = Response.status(Status.OK).entity(retVal)
						.type(MediaType.TEXT_PLAIN).build();
			} else {
				response = Response.status(Status.NO_CONTENT).build();

			}
		} catch (Exception exp) {
			LOGGER.error("An unexpected exception occurs. ", exp);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug("<< processSHA1");
		return response;
	}

	/**
	 * Return all properties key/value/type in xml format.
	 * 
	 * @param sha1
	 *            password
	 * @return Response containing the properties.
	 */
	public static Response getAllPropertiesValues(String sha1) {
		LOGGER.debug(">> getAllPropertiesValues");
		Response response = null;
		String retVal = null;
		try {
			if (StringUtils.equals(NerdServiceProperties.getAdminPw(),
					SHA1.getSHA1(sha1))) {
				retVal = NerdPropertiesUtil.getAllPropertiesListXml();
				response = Response.status(Status.OK).entity(retVal)
						.type(MediaType.TEXT_PLAIN).build();
			} else {
				response = Response.status(Status.FORBIDDEN).build();
			}
		} catch (Exception exp) {
			LOGGER.error("An unexpected exception occurs. ", exp);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug("<< getAllPropertiesValues");
		return response;
	}

	/**
	 * Dynamically update the value of the property given in XML input file.
	 * Also update the property file.
	 * 
	 * @param pXml
	 *            the xml containing the changes. Xml has to follow that schema:
	 *            <code>
	 * 	<changeProperty>
	 * 		<password>password</password>
	 * 		<property>
	 * 			<key>key</key>
	 * 			<value>value</value>
	 * 			<type>type</type>
	 * 		</property>
	 * 	</changeProperty>
	 * </code>
	 * @return the changed value if processing was a success. HTTP error code
	 *         else.
	 */
	public static Response changePropertyValue(String pXml) {
		LOGGER.debug(">> changePropertyValue");
		Response response = null;
		try {
			String result = StringUtils.EMPTY;
			ChangePropertyParser parser = new ChangePropertyParser(pXml);
			if (StringUtils.equals(NerdServiceProperties.getAdminPw(),
					SHA1.getSHA1(parser.getPassword()))) {

				if (parser.getKey().contains("com.scite_it.Nerd.service")) {
					if (StringUtils.equals(parser.getType(),
							NerdProperty.TYPE.PASSWORD.toString())) {
						String newPwd = SHA1.getSHA1(parser.getValue());
						NerdServiceProperties.updatePropertyFile(
								parser.getKey(), newPwd);
						NerdServiceProperties.reload();
						result = newPwd;
					} else {
						NerdServiceProperties.updatePropertyFile(
								parser.getKey(), parser.getValue());
						NerdServiceProperties.reload();
						result = NerdServiceProperties.getProps().getProperty(parser.getKey(), StringUtils.EMPTY);
					}
					
				} else {
					NerdProperties.setPropertyValue(parser.getKey(),
							parser.getValue());
					NerdProperties.updatePropertyFile(parser.getKey(),
							parser.getValue());
					NerdProperties.reload();
					result = NerdProperties.getProps().getProperty(parser.getKey(), StringUtils.EMPTY);
				}
				response = Response.status(Status.OK).entity(result)
						.type(MediaType.TEXT_PLAIN).build();

			} else {
				response = Response.status(Status.FORBIDDEN).build();
			}
		} catch (Exception exp) {
			LOGGER.error("An unexpected exception occurs. ", exp);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		LOGGER.debug("<< changePropertyValue");
		return response;
	}
}
