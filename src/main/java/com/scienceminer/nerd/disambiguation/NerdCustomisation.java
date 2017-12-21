package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.kb.Customisations;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.kb.model.Article;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import java.io.BufferedReader;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.io.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Representation of a customization. A customisation is currently a list of wikipedia articles that
 * will weight the relatedness score when ranking candidates for disambiguation so it is a NerdContext.
 */
public class NerdCustomisation extends NerdContext {

    protected static final Logger LOGGER = LoggerFactory.getLogger(NerdCustomisation.class);

    public static final String GENERIC_CUSTOMISATION = "generic";

    private String name = null;

    public NerdCustomisation() {
        super();
    }

    /**
     * Instantiate a NerdCustomisation object from a user specified JSON customisation
     */
    public void createNerdCustomisation(String customisationName, String customisationJsonData) {

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonRoot = mapper.readTree(customisationJsonData);

            String lang = null;
            JsonNode langNode = jsonRoot.findPath("lang");
            if (langNode != null && !langNode.isMissingNode()) {
                lang = langNode.textValue().trim();
            }

            if (lang == null) {
                throw new NerdException("Language not specified in the customisation: " + customisationName);
            }

            Map<String, LowerKnowledgeBase> wikipedias = UpperKnowledgeBase.getInstance().getWikipediaConfs();
            LowerKnowledgeBase wikipedia = wikipedias.get(lang);
            if (wikipedia == null) {
                throw new NerdException("Language is not supported: " + lang);
            }

            JsonNode wikipediaNode = jsonRoot.findPath("wikipedia");
            if ((wikipediaNode != null) && (!wikipediaNode.isMissingNode())) {
                // we have the list of article ID as json array
                Iterator<JsonNode> ite = wikipediaNode.elements();
                while (ite.hasNext()) {
                    JsonNode idNode = ite.next();
                    if ((idNode != null) && (!idNode.isMissingNode())) {
                        String articleId = trim(idNode.asText());
                        int id = -1;
                        try {
                            id = Integer.parseInt(articleId);
                        } catch (Exception e) {
                            LOGGER.warn("Invalid wikipedia article identifier ID: " + articleId + " - must be an integer");
                            continue;
                        }
                        Page page = wikipedia.getPageById(id);
                        if (page == null) {
                            LOGGER.warn("Invalid wikipedia article identifier ID: " + articleId + " - article does not exist for language: " + lang);
                            continue;
                        }
                        if (page.getType() != PageType.article) {
                            LOGGER.warn("Invalid wikipedia article identifier ID: " + articleId + " - the page is not an article for language: " + lang);
                            continue;
                        }

                        Article article = (Article) page;
                        if (contextArticles == null) {
                            contextArticles = new ArrayList<>();
                            contextArticlesIds = new ArrayList<>();
                        }
                        // default weight of the article in the context to 1 - this should be reviewed!
                        // this implies that there is no use to sort the list
                        article.setWeight(1.0);
                        contextArticles.add(article);
                        contextArticlesIds.add(new Integer(id));
                    }
                }
            }
        } catch (Exception e) {
            throw new NerdException("Cannot parse the customisation.", e);
        }
    }

    public void setName(String theName) {
        name = theName;
    }

    public String getName() {
        return name;
    }
}