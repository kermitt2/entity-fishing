package com.scienceminer.nerd.service;

import com.scienceminer.nerd.disambiguation.NerdCategories;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.exceptions.ResourceNotFound;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.db.WikipediaDomainMap;
import com.scienceminer.nerd.kb.model.Article;
import com.scienceminer.nerd.kb.model.Label;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.kb.model.Page.PageType;
import org.apache.commons.lang3.ArrayUtils;
import org.grobid.core.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.scienceminer.nerd.kb.UpperKnowledgeBase.TARGET_LANGUAGES;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * REST service to access data in the knowledge base
 */
public class NerdRestKB {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestKB.class);

    /**
     * Get the information for a concept.
     *
     * @param id identifier of the concept
     * @return a response object containing the information related to the identified concept.
     */
    public String getConceptInfo(String id, String lang) {
        String response = null;
        if (id.startsWith("Q")) {
            // we have a concept
            response = getWikidataConceptInfo(id);
        } else if (id.startsWith("P")) {
            // we have a property
            response = getWikidataConceptInfo(id);
        } else {
            // we have a wikipedia page id, and the lang field matters
            response = getWikipediaConceptInfo(id, lang);
        }

        return response;
    }

    private String getWikipediaConceptInfo(String id, String lang) throws QueryException {
        Integer identifier = null;
        try {
            identifier = Integer.parseInt(id);
        } catch (Exception e) {
            LOGGER.error("Could not parse the concept identifier.");
            throw new QueryException("Invalid format of the supplied identifier.", QueryException.WRONG_IDENTIFIER);
        }

        NerdEntity entity = new NerdEntity();
        entity.setLang(lang);

        LowerKnowledgeBase wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf(lang);

        if (wikipedia == null) {
            LOGGER.error("The knowledge base does not cover the language " + lang + ".");
            throw new QueryException("The knowledge base does not cover the language " + lang + ".", QueryException.LANGUAGE_ISSUE);
        }
        Page page = wikipedia.getPageById(identifier);

        // check the type of the page - it must be an article
        PageType type = page.getType();
        if (type != PageType.article) {
            return null;
        }
        Article article = (Article) page;
        entity.setPreferredTerm(article.getTitle());
        entity.setRawName(article.getTitle());

        // definition
        Definition definition = new Definition();
        try {
            definition.setDefinition(article.getFirstParagraphWikiText());
        } catch (Exception e) {
            LOGGER.debug("Error when getFirstParagraphWikiText for page id " + identifier);
        }
        definition.setSource("wikipedia-" + lang);
        definition.setLang(lang);
        entity.addDefinition(definition);

        entity.setWikipediaExternalRef(identifier);

        // categories
        com.scienceminer.nerd.kb.model.Category[] parentCategories = article.getParentCategories();
        handleCategories(entity, String.valueOf(identifier), parentCategories);

        // translations
        //Map<String, Wikipedia> wikipedias = Lexicon.getInstance().getWikipediaConfs();
        Map<String, LowerKnowledgeBase> wikipedias = UpperKnowledgeBase.getInstance().getWikipediaConfs();
        //Map<String, WikipediaDomainMap> wikipediaDomainMaps = Lexicon.getInstance().getWikipediaDomainMaps();
        Map<String, WikipediaDomainMap> wikipediaDomainMaps =
                UpperKnowledgeBase.getInstance().getWikipediaDomainMaps();
        WikipediaDomainMap wikipediaDomainMap = wikipediaDomainMaps.get(lang);

        if (wikipediaDomainMap == null)
            System.out.println("wikipediaDomainMap is null for " + lang);
        else
            entity.setDomains(wikipediaDomainMap.getDomains(entity.getWikipediaExternalRef()));

        entity.setWikipediaMultilingualRef(article.getTranslations(), TARGET_LANGUAGES, wikipedias);
        entity.setWikidataId(article.getWikidataId());

        List<Statement> statements = UpperKnowledgeBase.getInstance().getStatements(entity.getWikidataId());
        entity.setStatements(statements);

        return entity.toJsonFull();

    }

    private void handleCategories(NerdEntity entity, String identifier, com.scienceminer.nerd.kb.model.Category[] parentCategories) {
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
                    entity.addCategory(new Category(theCategory));
							/*else {
								break;
							}*/
            }
        }
    }

    private String getWikidataConceptInfo(String id) {
        NerdEntity entity = new NerdEntity();
        entity.setLang(Language.EN);
        UpperKnowledgeBase knowledgeBase = UpperKnowledgeBase.getInstance();

        if (id.startsWith("Q")) {
            Concept concept = knowledgeBase.getConcept(id);
            if (concept == null) {
                LOGGER.error("There is no resource for this Id.");
                throw new ResourceNotFound("The requested resource could not be found in the current version of the KB.");
            }

            Integer pageId = concept.getPageIdByLang(Language.EN);
            if (pageId != null) {
                LowerKnowledgeBase wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf(Language.EN);
                Page page = wikipedia.getPageById(pageId);

                entity.setRawName(page.getTitle());
                entity.setPreferredTerm(page.getTitle());
                entity.setWikipediaExternalRef(pageId);

                PageType pageType = page.getType();

                if (pageType == PageType.article) {
                    Article article = (Article) page;

                    // definition
                    Definition definition = new Definition();
                    try {
                        definition.setDefinition(article.getFirstParagraphWikiText());
                    } catch (Exception e) {
                        LOGGER.debug("Error when getFirstParagraphWikiTextfor page id " + id);
                    }
                    definition.setSource("wikipedia-en");
                    definition.setLang(Language.EN);
                    entity.addDefinition(definition);

                    // categories
                    com.scienceminer.nerd.kb.model.Category[] parentCategories = article.getParentCategories();
                    handleCategories(entity, id, parentCategories);

                    // translations
                    Map<String, LowerKnowledgeBase> wikipedias =
                            UpperKnowledgeBase.getInstance().getWikipediaConfs();

                    Map<String, WikipediaDomainMap> wikipediaDomainMaps =
                            UpperKnowledgeBase.getInstance().getWikipediaDomainMaps();
                    WikipediaDomainMap wikipediaDomainMap = wikipediaDomainMaps.get(Language.EN);
                    if (wikipediaDomainMap == null)
                        LOGGER.warn("wikipediaDomainMap is null for en");
                    else
                        entity.setDomains(wikipediaDomainMap.getDomains(entity.getWikipediaExternalRef()));

                    entity.setWikipediaMultilingualRef(article.getTranslations(), TARGET_LANGUAGES, wikipedias);
                } /*else {
                    // if it's not an article, but it still a concept
                    String json = null;
                    StringBuilder jsonBuilder = new StringBuilder();
                    jsonBuilder.append("{ \"message\": \"The requested resource for identifier "  + id + " could not be found but may be available in the future.\" }");
                    json = jsonBuilder.toString();
                    return json;
                }*/
            }
        } else if (id.startsWith("P")) {
            Property property = knowledgeBase.getProperty(id);
            entity.setPreferredTerm(property.getName());
            entity.setRawName(property.getName());

        } else {
            LOGGER.error("The supplied wikidata identifier does not start with Q or P");
            throw new QueryException("Invalid format of the supplied wikidata identifier.", QueryException.WRONG_IDENTIFIER);
        }

        entity.setWikidataId(id);

        List<Statement> statements = UpperKnowledgeBase.getInstance().getStatements(id);
        entity.setStatements(statements);

        return entity.toJsonFull();
    }

    /**
     * Get the list of all ambiguous concepts that can be realized by a given term with
     * associated conditional probability.
     *
     * @param term the term for which a semantic look-up is realised
     * @return a response object containing the concept information related to the term.
     */
    public String getTermLookup(String term, String lang) {
        String json = null;

        LowerKnowledgeBase wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf(lang);

        if (isBlank(term)) {
            LOGGER.error("Empty term value.");
            throw new QueryException("The supplied term is empty or null.", QueryException.INVALID_TERM);
        } else if (wikipedia == null) {
            LOGGER.error("The knowledge base does not cover the language " + lang + ".");
            throw new QueryException("The knowledge base does not cover the language " + lang + ".", QueryException.LANGUAGE_ISSUE);
        }
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{ \"term\": \"" + term + "\", \"lang\": \"" + lang + "\", \"senses\" : [");

        Label lbl = new Label(wikipedia.getEnvironment(), term.trim());
        if (lbl.exists()) {
            Label.Sense[] senses = lbl.getSenses();
            if ((senses != null) && (senses.length > 0)) {
                boolean first = true;
                for (int i = 0; i < senses.length; i++) {
                    Label.Sense sense = senses[i];
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
                            if (theCategory.getTitle().toLowerCase().contains("disambiguation")) {
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

        return json;
    }

    public String methodLogIn() {
        return ">> " + NerdRestKB.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public String methodLogOut() {
        return "<< " + NerdRestKB.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public String getWikidataIDByDOI(String doi) {
        if (isEmpty(doi)) {
            return "";
        }
        String wikidataID = UpperKnowledgeBase.getInstance().getEntityIdPerDoi(doi);

        if (isEmpty(wikidataID)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"").append("doi").append("\"").append(":").append("\"").append(doi).append("\"");
        sb.append(",");
        sb.append("\"").append("wikidataID").append("\"").append(":").append("\"").append(wikidataID).append("\"");
        sb.append("}");
        return sb.toString();
    }
}