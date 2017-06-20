package com.scienceminer.nerd.service;

import com.scienceminer.nerd.disambiguation.NerdCategories;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.kb.Definition;
import com.scienceminer.nerd.kb.Lexicon;
import com.scienceminer.nerd.kb.db.WikipediaDomainMap;
import com.scienceminer.nerd.kb.model.Article;
import com.scienceminer.nerd.kb.model.Label;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.kb.model.Wikipedia;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * REST service to access data in the knowledge base
 *
 * @author Patrice
 */
public class NerdRestKB {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestKB.class);

    /**
     * Get the information for a concept.
     *
     * @param id   identifier of the concept
     * @param lang language identification
     * @return a response object containing the information related to the identified concept.
     */
    public static Response getConceptInfo(String id, String lang) {
        Response response = null;
        String retVal = null;
        if (isBlank(id)) {
            return Response.status(Status.NOT_FOUND).build();
        }

        try {
            Integer identifier = null;
            try {
                identifier = Integer.parseInt(id);
            } catch (Exception e) {
                LOGGER.error("Could not parse the concept identifier for: " + identifier + ". Bad request.", e);
                return Response.status(Status.BAD_REQUEST).build();
            }

            NerdEntity entity = new NerdEntity();
            entity.setLang(lang);
            Wikipedia wikipedia = Lexicon.getInstance().getWikipediaConf(lang);

            if (wikipedia == null) {
                LOGGER.error("The specified language " + lang + " is not supported. Bad request.");
                return Response.status(Status.BAD_REQUEST).build();
            }

            Page page = wikipedia.getPageById(identifier.intValue());

            // check the type of the page - it must be an article
            PageType type = page.getType();
            if (type != PageType.article) {
                LOGGER.error("The concept is not an article. Not found. ");
                return Response.status(Status.NOT_FOUND).build();
            }

            Article article = (Article) page;
            entity.setPreferredTerm(article.getTitle());
            entity.setRawName(article.getTitle());

            // definition
            Definition definition = new Definition();
            try {
                definition.setDefinition(article.getFirstParagraphMarkup());
            } catch (Exception e) {
                LOGGER.debug("Error when getFirstParagraphMarkup for PageID " + identifier);
            }
            definition.setSource("wikipedia-" + lang);
            definition.setLang(lang);
            entity.addDefinition(definition);

            entity.setWikipediaExternalRef(identifier);

            // categories
            processCategories(identifier, entity, article);

            // translations
            Map<String, Wikipedia> wikipedias = Lexicon.getInstance().getWikipediaConfs();
            Map<String, WikipediaDomainMap> wikipediaDomainMaps = Lexicon.getInstance().getWikipediaDomainMaps();
            WikipediaDomainMap wikipediaDomainMap = wikipediaDomainMaps.get(lang);
            if (wikipediaDomainMap == null) {
                LOGGER.warn("wikipediaDomainMap is null for " + lang);
            } else {
                entity.setDomains(wikipediaDomainMap.getDomains(entity.getWikipediaExternalRef()));
            }

            entity.setWikipediaMultilingualRef(article.getTranslations(), targetLanguages, wikipedias);

//						entity.setProperties(wikipedia.getProperties(identifier.intValue())); 
//						entity.setRelations(wikipedia.getRelations(identifier.intValue()), wikipedia); 

            String json = entity.toJsonFull();
            if (json == null) {
                response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } else {
                response = Response.status(Status.OK)
                        .entity(json)
                        .build();
            }


        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }

    private static void processCategories(Integer identifier, NerdEntity entity, Article article) {
        com.scienceminer.nerd.kb.model.Category[] parentCategories = article.getParentCategories();
        if (ArrayUtils.isNotEmpty(parentCategories)) {
            for (com.scienceminer.nerd.kb.model.Category theCategory : parentCategories) {
                // not a valid sense if a category of the sense contains "disambiguation" -> this is then a disambiguation page
                if (theCategory == null) {
                    LOGGER.warn("Invalid category page for article: " + identifier);
                    continue;
                }
                if (theCategory.getTitle() == null) {
                    LOGGER.warn("Invalid category content for article: " + identifier);
                    continue;
                }
                if (!NerdCategories.categoryToBefiltered(theCategory.getTitle()))
                    entity.addCategory(new com.scienceminer.nerd.kb.Category(theCategory));
            }
        }
    }


    /**
     * Get the list of all ambiguous concepts that can be realized by a given term with
     * associated conditional probability.
     *
     * @param term the term for which a semantic look-up is realised
     * @return a response object containing the concept information related to the term.
     */
    public static Response getTermLookup(String term, String lang) {
        Response response = null;
        String retVal = null;
        String json = null;
        try {
            //LOGGER.debug(">> set raw text for stateless service'...");
            Wikipedia wikipedia = Lexicon.getInstance().getWikipediaConf(lang);

            if (StringUtils.isBlank(term)) {
                LOGGER.error("Empty term. Bad request.");
                return Response.status(Status.BAD_REQUEST).build();
            }

            if (wikipedia == null) {
                LOGGER.error("Language " + lang + " is not supported. Bad request.");
                return Response.status(Status.BAD_REQUEST).build();
            }

            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{ \"term\": \"" + term + "\", \"lang\": \"" + lang + "\", \"senses\" : [");

            Label lbl = new Label(wikipedia.getEnvironment(), term);
            if (lbl.exists()) {
                Label.Sense[] senses = lbl.getSenses();
                if (ArrayUtils.isNotEmpty(senses)) {
                    boolean first = true;
                    for (int i = 0; i < senses.length; i++) {
                        Label.Sense sense = senses[i];
                        //PageType pageType = PageType.values()[sense.getType()];
                        PageType pageType = sense.getType();
                        if (pageType != PageType.article)
                            continue;

                        // in case we use a min sense probability filter
                        //if (sense.getPriorProbability() < minSenseProbability)
                        //	continue;

                        // not a valid sense if title is a list of ...
                        String title = sense.getTitle();
                        if ((title == null) || title.startsWith("List of") || title.startsWith("Liste des"))
                            continue;

                        boolean invalid = false;
//System.out.println("check categories for " + sense.getId());							
                        com.scienceminer.nerd.kb.model.Category[] parentCategories = sense.getParentCategories();
                        if (ArrayUtils.isNotEmpty(parentCategories)) {
                            for (com.scienceminer.nerd.kb.model.Category theCategory : parentCategories) {
                                // not a valid sense if a category of the sense contains "disambiguation" -> this is then a disambiguation page
                                if (theCategory == null) {
                                    LOGGER.warn("Invalid category page for sense: " + title);
                                    continue;
                                }
                                if (theCategory.getTitle() == null) {
                                    LOGGER.warn("Invalid category content for sense: " + title);
                                    continue;
                                }
//System.out.println("categ: " + theCategory.getTitle());
                                if (theCategory.getTitle().toLowerCase().indexOf("disambiguation") != -1) {
                                    invalid = true;
                                    break;
                                }
                            }
                        }
                        if (invalid)
                            continue;
                        if (first)
                            first = false;
                        else
                            jsonBuilder.append(", ");

                        jsonBuilder.append("{ \"pageid\": " + sense.getId() +
                                ", \"preferred\" : \"" + sense.getTitle() + "\", \"prob_c\" : " + sense.getPriorProbability() + " }");
                    }
                }
            }
            jsonBuilder.append("] }");
            json = jsonBuilder.toString();


            if (json == null) {
                response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } else {
                response = Response.status(Status.OK).entity(json)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .build();
            }

        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get a KB instance. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }


    private static List<String> targetLanguages = Arrays.asList("en", "de", "fr");

    public static String methodLogIn() {
        return ">> " + NerdRestKB.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public static String methodLogOut() {
        return "<< " + NerdRestKB.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }
}