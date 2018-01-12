package com.scienceminer.nerd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scienceminer.nerd.exceptions.CustomisationException;
import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.kb.Customisations;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Page;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Class for implemeting the services to manage the customization in NERD.
 */
public class NerdRestCustomisation {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestCustomisation.class);

    /**
     * Return the list of existing customisations.
     */
    public static String getCustomisations() {
        Customisations customisations = Customisations.getInstance();
        List<String> names = customisations.getCustomisations();

        return buildJsonRepresentation(names);
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
    public static String getCustomisation(String name) {
        Customisations customisations = Customisations.getInstance();
        return customisations.getCustomisation(name);
    }

    public static boolean createCustomisation(String name, String value) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
            LOGGER.error("The request is empty or null. ");
            throw new CustomisationException("The request is empty or null");
        }

        Customisations customisations = Customisations.getInstance();
        return customisations.createCustomisation(name, value);
    }

    public static boolean updateCustomisation(String name, String value) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
            LOGGER.error("The request is empty or null. ");
            throw new CustomisationException("The request is empty or null");
        }

        Customisations customisations = Customisations.getInstance();
        return customisations.updateCustomisation(name, value);
    }


    public static boolean deleteCustomisation(String name) {
        Customisations customisations = Customisations.getInstance();
        return customisations.deleteCustomisation(name);
    }
}

