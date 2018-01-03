package com.scienceminer.nerd.service;

import com.scienceminer.nerd.exceptions.CustomisationException;
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
 */
public class NerdRestCustomisation {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestCustomisation.class);

    /**
     * Return the list of existing customisations.
     */
    public static Response getCustomisations() {
        LOGGER.debug(">> getCustomisations");
        Response response = null;
        try {
            Customisations customisations = Customisations.getInstance();
            List<String> names = customisations.getCustomisations();

            String output = buildJsonRepresentation(names);

            response = Response.status(Status.OK).entity(output).type(MediaType.APPLICATION_JSON).build();
        } catch (Exception exp) {
            LOGGER.error("General error when accessing the list of existing customisations. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        LOGGER.debug("<< getCustomisations");
        return response;
    }

    private static String buildJsonRepresentation(List<String> names) {
        StringBuffer res = new StringBuffer();
        res.append("[");
        boolean begin = true;
        for (String name : names) {
            if (begin) {
                begin = false;
            } else {
                res.append(", ");
            }
            res.append("\"" + name + "\"");
        }
        res.append("]");

        return res.toString();
    }

    /**
     * Return the data of an existing customisation.
     */
    public static Response getCustomisation(String name) {
        Response response = null;
        try {
            Customisations customisations = Customisations.getInstance();
            String info = customisations.getCustomisation(name);
            if (info == null) {
                response = Response.status(Status.NOT_FOUND).build();
            } else {
                response = Response.status(Status.OK).entity(info)
                        .type(MediaType.APPLICATION_JSON).build();
            }
        } catch (CustomisationException ce) {
            response = Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(ce.getMessage())
                    .build();
        } catch (Exception exp) {
            LOGGER.error("Error when accessing the customisation " + name, exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;
    }

    public static Response createCustomisation(String name, String value) {
        Response response = null;

        if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Customisations customisations = Customisations.getInstance();
        try {
            customisations.createCustomisation(name, value);
        } catch (CustomisationException ce) {
            response = Response.status(Status.BAD_REQUEST)
                    .entity("{\"status\": \"" + ce.getMessage() + "\"}")
                    .build();
        }  catch (Exception e) {
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;

    }

    public static Response updateCustomisation(String name, String value) {
        Response response = null;


        if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Customisations customisations = Customisations.getInstance();

        try {
            customisations.updateCustomisation(name, value);
        } catch (CustomisationException ce) {
            response = Response.status(Status.BAD_REQUEST)
                    .entity("{\"status\": \"" + ce.getMessage() + "\"}")
                    .build();
        }  catch (Exception e) {
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        return response;
    }


    public static Response processDeleteNerdCustomisation(String name) {
        LOGGER.debug(">> processDeleteNerdCustomisation");
        Response response = null;
        try {
            Customisations customisations = Customisations.getInstance();
            customisations.deleteCustomisation(name);
        } catch (CustomisationException ce) {
            response = Response.status(Status.BAD_REQUEST)
                    .entity("{\"status\": \"" + ce.getMessage() + "\"}")
                    .build();
        }  catch (Exception e) {
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        LOGGER.debug("<< processDeleteNerdCustomisation");
        return response;
    }
}

