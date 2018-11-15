package com.scienceminer.nerd.service;

import com.scienceminer.nerd.disambiguation.*;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.kb.Customisations;
import com.scienceminer.nerd.main.Main;
import com.scienceminer.nerd.main.data.SoftwareInfo;
import com.scienceminer.nerd.mention.Mention;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.mention.Sentence;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.LanguageUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static com.scienceminer.nerd.disambiguation.NerdCustomisation.GENERIC_CUSTOMISATION;
import static com.scienceminer.nerd.exceptions.QueryException.LANGUAGE_ISSUE;
import static com.scienceminer.nerd.mention.ProcessText.GROBID_NER_SUPPORTED_LANGUAGES;
import static shadedwipo.org.apache.commons.lang3.StringUtils.isEmpty;

public class NerdRestProcessQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestProcessQuery.class);
    SoftwareInfo softwareInfo = SoftwareInfo.getInstance();

    /**
     * Parse a structured query and return the corresponding normalized enriched and disambiguated query object.
     *
     * @param theQuery POJO query object
     * @return a response query object containing the structured representation of
     * the enriched and disambiguated query.
     */
    public String processQuery(String theQuery) {

        LOGGER.debug(methodLogIn());
        LOGGER.debug(">> received query to process: " + theQuery);
        NerdQuery nerdQuery = NerdQuery.fromJson(theQuery);

        // we analyze the query object in order to determine the kind of object to be processed
        LOGGER.debug(">> set query object for stateless service...");

        // tuning the species only mention selection
        NerdRestProcessFile.tuneSpeciesMentions(nerdQuery);

        //checking customisation
        processCustomisation(nerdQuery);

        String output = null;

        switch (nerdQuery.getQueryType()) {
            case NerdQuery.QUERY_TYPE_TEXT:
                if (nerdQuery.getText().length() > 5) {
                    output = processQueryText(nerdQuery);
                } else {
                    throw new QueryException("Text query too short, use shortText instead.");
                }
                break;
            case NerdQuery.QUERY_TYPE_SHORT_TEXT:
                output = processSearchQuery(nerdQuery);
                break;
            case NerdQuery.QUERY_TYPE_TERM_VECTOR:
                output = processQueryTermVector(nerdQuery);
                break;
            case NerdQuery.QUERY_TYPE_INVALID:
                throw new QueryException();
        }

        LOGGER.debug(methodLogOut());
        return output;
    }

    /**
     * Validate and create a new context based on the customisation provided
     **/
    public static void processCustomisation(NerdQuery nerdQuery) {

        final String customisation = nerdQuery.getCustomisation();

        if (isEmpty(customisation) || StringUtils.equals(customisation, GENERIC_CUSTOMISATION)) {
            return;
        }

        Customisations customisations = Customisations.getInstance();
        final String customisationData = customisations.getCustomisation(customisation);

        if (customisationData == null) {
            throw new QueryException("The specified customisation in the query " + customisation + " doesn't exists");
        }

        NerdCustomisation customisationObj = new NerdCustomisation();
        customisationObj.createNerdCustomisation(customisationData);

        nerdQuery.setContext(customisationObj);
    }

    /**
     * Parse a structured query and return the corresponding normalized enriched and disambiguated query object.
     *
     * @param nerdQuery POJO query object
     * @return a response query object containing the structured representation of
     * the enriched and disambiguated query.
     */
    public String processQueryText(NerdQuery nerdQuery) {
        LOGGER.debug(methodLogIn());
        long start = System.currentTimeMillis();

        // language identification
        languageIdentificationAndValidation(nerdQuery, nerdQuery.getText());

        // entities originally from the query are marked as such
        List<NerdEntity> originalEntities = null;
        if (CollectionUtils.isNotEmpty(nerdQuery.getEntities())) {
            markUserEnteredEntities(nerdQuery, nerdQuery.getText().length());
            originalEntities = nerdQuery.getEntities();
        }

        ProcessText processText = ProcessText.getInstance();
        Integer[] processSentence = nerdQuery.getProcessSentence();
        List<Sentence> sentences = nerdQuery.getSentences();
        //If not previously segmented, call the sentence segmentation and set it back
        if ((sentences == null) && (nerdQuery.getSentence() || (processSentence != null))) {
            sentences = processText.sentenceSegmentation(nerdQuery.getText());
            nerdQuery.setSentences(sentences);
        }

        // first process all mentions
        List<Mention> mentions = processText.process(nerdQuery);

        // inject explicit acronyms
        mentions = processText.acronymCandidates(nerdQuery, mentions);

        if (originalEntities == null) {
            nerdQuery.setAllEntities(mentions);
        } else {
            List<NerdEntity> selectedMentions = selectEntities(originalEntities, mentions);
            nerdQuery.setEntities(selectedMentions);
        }

        // sort the entities
        Collections.sort(nerdQuery.getEntities());

        // disambiguate and solve entity mentions
        NerdEngine disambiguator = NerdEngine.getInstance();
        List<NerdEntity> disambiguatedEntities = disambiguator.disambiguate(nerdQuery);
        nerdQuery.setEntities(disambiguatedEntities);
        nerdQuery = NerdCategories.addCategoryDistribution(nerdQuery);


        long end = System.currentTimeMillis();
        nerdQuery.setRuntime(end - start);
        // for metadata
        nerdQuery.setSoftware(softwareInfo.getName());
        nerdQuery.setVersion(softwareInfo.getVersion());
        nerdQuery.setDate(java.time.Clock.systemUTC().instant().toString());

        LOGGER.info("runtime: " + (end - start));

        Collections.sort(nerdQuery.getEntities());
        LOGGER.debug(methodLogOut());
        return nerdQuery.toJSONClean();
    }

    /**
     * Mark (confidence 1.0) the user defined entities as long as:
     * - they have a valid offset (end > start and != -1)
     * - they have a valid wikipedia or wikidata ID
     **/
    public void markUserEnteredEntities(NerdQuery nerdQuery, long maxOffsetValue) {

        for (NerdEntity entity : nerdQuery.getEntities()) {
            if (entity.getOffsetStart() == -1 || entity.getOffsetEnd() == -1
                    || entity.getOffsetEnd() < entity.getOffsetStart() || entity.getOffsetEnd() > maxOffsetValue) {
                LOGGER.warn("The entity " + entity.toJsonCompact() + " doesn't have valid offset. Ignoring it.");
            } else {
                entity.setNer_conf(1.0);

                // do we have disambiguated entity information for the entity?
                if (entity.getWikipediaExternalRef() != -1 || StringUtils.isNotBlank(entity.getWikidataId())) {
                    entity.setSource(ProcessText.MentionMethod.user);
                    entity.setNerdScore(1.0);
                }
            }
        }
    }


    public static void languageIdentificationAndValidation(NerdQuery nerdQuery, String text) {
        Language lang = nerdQuery.getLanguage();
        if (lang == null || lang.getLang() == null) {
            LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
            lang = languageUtilities.runLanguageId(text);
            nerdQuery.setLanguage(lang);
            LOGGER.debug(">> identified language: " + lang.toString());
        } else {
            LOGGER.debug(">> language already identified: " + lang.getLang().toString());
        }

        if (!nerdQuery.hasValidLanguage()) {
            throw new QueryException("Language not invalid or not supported", LANGUAGE_ISSUE);
        }
    }

    /**
     * This method select entities not conflicting with the ones already provided in the query
     */
    protected static List<NerdEntity> selectEntities(List<NerdEntity> originalEntities, List<Mention> newMentions) {
        List<NerdEntity> resultingEntities = new ArrayList<>();
        if (CollectionUtils.isEmpty(newMentions)) {
            return originalEntities;
        }

        int offsetPos = 0;
        int ind = 0;

        for (Mention mention : newMentions) {
            int begin = mention.getOffsetStart();
            int end = mention.getOffsetEnd();

            if (ind >= originalEntities.size()) {
                NerdEntity theEntity = new NerdEntity(mention);
                resultingEntities.add(theEntity);
            } else if (end < originalEntities.get(ind).getOffsetStart()) {
                NerdEntity theEntity = new NerdEntity(mention);
                resultingEntities.add(theEntity);
            } else if ((begin > originalEntities.get(ind).getOffsetStart()) &&
                    (begin < originalEntities.get(ind).getOffsetEnd())) {
                continue;
            } else if ((end > originalEntities.get(ind).getOffsetStart()) &&
                    (end < originalEntities.get(ind).getOffsetEnd())) {
                continue;
            } else if (begin > originalEntities.get(ind).getOffsetEnd()) {
                while (ind < originalEntities.size()) {
                    ind++;
                    if (ind >= originalEntities.size()) {
                        NerdEntity theEntity = new NerdEntity(mention);
                        resultingEntities.add(theEntity);
                        break;
                    }
                    if (begin < originalEntities.get(ind).getOffsetEnd()) {
                        if (end < originalEntities.get(ind).getOffsetStart()) {
                            NerdEntity theEntity = new NerdEntity(mention);
                            resultingEntities.add(theEntity);
                        }
                        break;
                    }
                }
            }
        }
        for (NerdEntity entity : originalEntities) {
            resultingEntities.add(entity);
        }

        return resultingEntities;
    }

    /**
     * Disambiguation a structured query specifying a weighted term vector and return the enriched term vector
     * with the corresponding normalized and disambiguated terms.
     *
     * @param nerdQuery query object with the weighted term vector to be processed
     * @return a response JSON object containing the weighted term vector with the resolved entities.
     */
    public String processQueryTermVector(NerdQuery nerdQuery) {
        LOGGER.debug(methodLogIn());
        long start = System.currentTimeMillis();

        // language identification
        // test first if the language is already indicated in the query structure

        // reformat text content
        StringBuilder textContent = new StringBuilder();
        for (WeightedTerm wt : nerdQuery.getTermVector()) {
            textContent.append(" " + wt.getTerm());
        }
        String text = textContent.toString();

        languageIdentificationAndValidation(nerdQuery, text);

        NerdEngine disambiguator = NerdEngine.getInstance();
        //nerdQuery.setEntities(disambiguatedEntities);
        //disambiguatedEntities = disambiguator.disambiguate(nerdQuery, true, false);
        // add the NerdEntity objects to the WeightedTerm object if disambiguation
        // is successful
        disambiguator.disambiguateWeightedTerms(nerdQuery);
//System.out.println(nerdQuery.toJSONClean(null));	
        nerdQuery = NerdCategories.addCategoryDistribution(nerdQuery);

        long end = System.currentTimeMillis();
        nerdQuery.setRuntime(end - start);
        // for metadata
        nerdQuery.setSoftware(softwareInfo.getName());
        nerdQuery.setVersion(softwareInfo.getVersion());
        nerdQuery.setDate(java.time.Clock.systemUTC().instant().toString());

        //Collections.sort(nerdQuery.getEntities());
        LOGGER.debug(methodLogOut());
        return nerdQuery.toJSONClean();

    }

    /**
     * Disambiguation of the terms of a seach query.
     *
     * @param nerdQuery POJO query object with the search query and additional optional contextual information
     * @return a response JSON object containing the search terms with the resolved entities.
     */
    public String processSearchQuery(NerdQuery nerdQuery) {
        long start = System.currentTimeMillis();

        //nerdQuery.setShortText(true);

        // language identification
        languageIdentificationAndValidation(nerdQuery, nerdQuery.getShortText());

        // entities originally from the query are marked as such
        List<NerdEntity> originalEntities = null;
        if (CollectionUtils.isNotEmpty(nerdQuery.getEntities())) {
            markUserEnteredEntities(nerdQuery, nerdQuery.getShortText().length());
            originalEntities = nerdQuery.getEntities();
        }

        // possible entity mentions
        ProcessText processText = ProcessText.getInstance();
        List<Mention> entities = processText.process(nerdQuery);

        // we keep only entities not conflicting with the ones already present in the query
        if (originalEntities == null) {
            nerdQuery.setAllEntities(entities);
        } else {
            List<NerdEntity> selectedMentions = selectEntities(originalEntities, entities);
            nerdQuery.setEntities(selectedMentions);
        }

        // sort the entities
        if (CollectionUtils.isNotEmpty(nerdQuery.getEntities()))
            Collections.sort(nerdQuery.getEntities());

        if (nerdQuery.getEntities() != null) {
            // disambiguate and solve entity mentions
            NerdEngine disambiguator = NerdEngine.getInstance();
            List<NerdEntity> disambiguatedEntities = disambiguator.disambiguate(nerdQuery);
            nerdQuery.setEntities(disambiguatedEntities);
            // calculate the global categories
            nerdQuery = NerdCategories.addCategoryDistribution(nerdQuery);
        }

        long end = System.currentTimeMillis();
        nerdQuery.setRuntime(end - start);

        // for metadata
        nerdQuery.setSoftware(softwareInfo.getName());
        nerdQuery.setVersion(softwareInfo.getVersion());
        nerdQuery.setDate(java.time.Clock.systemUTC().instant().toString());

        if (nerdQuery.getEntities() != null)
            Collections.sort(nerdQuery.getEntities());
        return nerdQuery.toJSONClean(null);
    }


    public static String methodLogIn() {
        return ">> " + NerdRestProcessString.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public static String methodLogOut() {
        return "<< " + NerdRestProcessString.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public static void main(String[] args) {
        SoftwareInfo softwareInfo = SoftwareInfo.getInstance();
        System.out.println(softwareInfo.getName());
    }
}
