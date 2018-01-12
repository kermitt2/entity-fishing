package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.CustomisationException;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.kb.model.Article;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.*;

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
    public void createNerdCustomisation(String customisationJsonData) {
        JsonNode root = null;

        try {
            root = parseAndValidate(customisationJsonData);
        } catch(CustomisationException ce) {
            throw new QueryException("The selected customisation cannot be applied.");
        }

        JsonNode langNode = root.findPath("lang");
        String lang = langNode.textValue().trim();

        Map<String, LowerKnowledgeBase> wikipedias = UpperKnowledgeBase.getInstance().getWikipediaConfs();
        LowerKnowledgeBase wikipedia = wikipedias.get(lang);

        JsonNode wikipediaNode = root.findPath("wikipedia");
        Iterator<JsonNode> ite = wikipediaNode.elements();
        while (ite.hasNext()) {
            JsonNode idNode = ite.next();
            if ((idNode != null) && (!idNode.isMissingNode())) {
                String articleId = trim(idNode.asText());
                int id = Integer.parseInt(articleId);
                Page page = wikipedia.getPageById(id);

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


    public void setName(String theName) {
        name = theName;
    }

    public String getName() {
        return name;
    }

    /**
     * Method to validate the customisation JSON sent by the user.
     */
    public static JsonNode parseAndValidate(String customisationJsonData) throws CustomisationException {

        ObjectMapper mapper = new ObjectMapper();

        // check parsing
        JsonNode jsonRoot = null;
        try {
            jsonRoot = mapper.readTree(customisationJsonData);
        } catch (IOException e) {
            LOGGER.error("Cannot parse the customisation JSON data.", e);
            throw new CustomisationException("Cannot parse the customisation JSON data");
        }

        //check language
        String lang = null;
        JsonNode langNode = jsonRoot.findPath("lang");
        if (langNode != null && !langNode.isMissingNode()) {
            lang = langNode.textValue().trim();
        }

        if (lang == null) {
            throw new CustomisationException("Language not specified or not supported in the customisation.");
        }
        if (!UpperKnowledgeBase.TARGET_LANGUAGES.contains(lang)) {
            throw new CustomisationException("Language specified in the customisation is not supported: " + lang);
        }

        Map<String, LowerKnowledgeBase> wikipedias = UpperKnowledgeBase.getInstance().getWikipediaConfs();
        LowerKnowledgeBase wikipedia = wikipedias.get(lang);

        JsonNode wikipediaNode = jsonRoot.findPath("wikipedia");
        if ((wikipediaNode == null) || (wikipediaNode.isMissingNode())) {
            throw new CustomisationException("Missing list of disambiguated entries from wikipedia page id. ");
        }

        Iterator<JsonNode> ite = wikipediaNode.elements();
        while (ite.hasNext()) {
            JsonNode idNode = ite.next();
            if ((idNode != null) && (!idNode.isMissingNode())) {
                String articleId = trim(idNode.asText());
                int id = -1;
                try {
                    id = Integer.parseInt(articleId);
                } catch (Exception e) {
                    throw new CustomisationException("Invalid wikipedia article identifier ID: " + articleId + " - must be an integer");
                }
                Page page = wikipedia.getPageById(id);
                if (page == null) {
                    throw new CustomisationException("Invalid wikipedia article identifier ID: " + articleId + " - article does not exist for language: " + lang);
                }
                if (page.getType() != Page.PageType.article) {
                    throw new CustomisationException("Invalid wikipedia article identifier ID: " + articleId + " - the page is not an article for language: " + lang);
                }
            }
        }
        return jsonRoot;
    }
}