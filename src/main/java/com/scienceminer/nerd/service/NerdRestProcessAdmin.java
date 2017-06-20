package com.scienceminer.nerd.service;

import com.scienceminer.nerd.utilities.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 */
public class NerdRestProcessAdmin {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestProcessAdmin.class);

    /**
     * Returns the admin view of all properties used for running Nerd.
     *
     * @param authToken the password
     * @return returns a response object containing the admin infos in html
     * syntax.
     */
    public static Response getAdminParams(String authToken) {
        Response response = null;
        try {
            LOGGER.debug("called getDescriptionAsHtml()...");
            String pass = NerdServiceProperties.getAdminPw();
            if (isNotBlank(pass) && isNotBlank(authToken) && pass.equals(SHA1.getSHA1(authToken))) {
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
     * @param sha1 string to hash
     * @return Response containing the value hashed.
     */
    @Deprecated
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
     * @param authToken password
     * @return Response containing the properties.
     */
    public static Response getAllPropertiesValues(String authToken) {
        LOGGER.debug(">> getAllPropertiesValues");
        Response response = null;
        String retVal = null;
        if (!isAuthTokenOK(authToken)) return Response.status(Status.NOT_FOUND).build();

        try {
            retVal = NerdPropertiesUtil.getAllPropertiesListJson();
            response = Response.status(Status.OK).entity(retVal).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs when fetching property values. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        LOGGER.debug("<< getAllPropertiesValues");
        return response;
    }

    public static Response getProperty(String authToken, String name) {
        if (!isAuthTokenOK(authToken)) return Response.status(Status.NOT_FOUND).build();

        NerdProperty prop = getProperty(name);
        if (prop == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response
                .status(Status.OK)
                .entity(prop.toJson())
                .build();

    }

    public static NerdProperty getProperty(String name) {
        for (NerdProperty property : NerdPropertiesUtil.getAllPropertiesList()) {
            if (StringUtils.equals(property.getKey(), name)) {
                return property;
            }
        }

        return null;
    }

    /**
     * Dynamically update the value of the property given in XML input file.
     * Also update the property file.
     *
     * @return the changed value if processing was a success. HTTP error code
     * else.
     */
    public static Response changePropertyValue(String authToken, String key, String newValue) {
        if (!isAuthTokenOK(authToken)) return Response.status(Status.NOT_FOUND).build();

        NerdProperty property = getProperty(key);
        if (property == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        try {
            NerdServiceProperties.updatePropertyFile(key, newValue);
            NerdServiceProperties.reload();

            property.setValue(newValue);

        } catch (Exception e) {
            LOGGER.error("Something went wrong while updating property " + key + " with value " + newValue, e);
        }

        return Response
                .status(Status.OK)
                .entity(property.toJson())
                .build();

    }

    private static boolean isAuthTokenOK(String authToken) {
        if (isNotBlank(authToken) && StringUtils.equals(NerdServiceProperties.getAdminPw(), SHA1.getSHA1(authToken))) {
            return true;
        }
        return false;
    }
}
