package com.scienceminer.nerd.service;

import com.scienceminer.nerd.kb.Customisations;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

/**
 * Class for implemeting the services to manage the customization in NERD.
 *
 */
public class NerdRestCustomisation {

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
            for (String name : names) {
                if (begin)
                    begin = false;
                else
                    res.append(", ");
                res.append("\"" + name + "\"");
            }
            res.append("]");
            response = Response.status(Status.OK).entity(res.toString()).type(MediaType.APPLICATION_JSON).build();
        } catch (Exception exp) {
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
        } catch (Exception exp) {
            LOGGER.error("Error when accessing the list of existing customisations. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;
    }

    public static Response createNewCustomisation(String name, String profile) {
        Response response = null;

        if (StringUtils.isBlank(name) || StringUtils.isBlank(profile)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Customisations customisations = Customisations.getInstance();
        String message = customisations.createCustomisation(name, profile);
        if (message.equals("OK")) {
            response = Response.status(Status.OK).build();
        } else if (message.startsWith("Invalid request")) {
            response = Response.status(Status.BAD_REQUEST).entity("{\"status\": \"" + message + "\"}").build();
        } else if (message.startsWith("Customisation already created")) {
            response = Response.status(Status.BAD_REQUEST).entity("{\"status\": \"" + message + "\"}").build();
        } else {
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;

    }

    public static Response updateCustomisation(String name, String profile) {
        Response response = null;


        if (StringUtils.isBlank(name) || StringUtils.isBlank(profile)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Customisations customisations = Customisations.getInstance();
        String message = customisations.updateCustomisation(name, profile);
        if (message.equals("OK")) {
            response = Response.status(Status.OK).build();
        } else if (message.startsWith("Invalid request")) {
            response = Response.status(Status.BAD_REQUEST)
                    .entity("{\"status\": \"" + message + "\"}").build();
        } else if (message.startsWith("Customisation already created")) {
            response = Response.status(Status.BAD_REQUEST)
                    .entity("{\"status\": \"" + message + "\"}").build();
        } else {
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }


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
        } catch (Exception exp) {
            LOGGER.error("Error deleting customisation: " + name, exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        LOGGER.debug("<< processDeleteNerdCustomisation");
        return response;
    }
}

