package com.scienceminer.nerd.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scienceminer.nerd.disambiguation.NerdContext;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.disambiguation.WeightedTerm;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.kb.Category;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.main.Main;
import com.scienceminer.nerd.mention.Mention;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.mention.Sentence;
import com.scienceminer.nerd.utilities.Filter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.grobid.core.document.Document;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.Page;
import org.grobid.core.utilities.KeyGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static com.scienceminer.nerd.kb.UpperKnowledgeBase.TARGET_LANGUAGES;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * This is the POJO object for representing input and output "enriched" query.
 * Having Jersey supporting JSON/object mapping, this permits to consume JSON post query.
 *
 * @author Patrice
 */
public class NerdQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdQuery.class);

    public static final String QUERY_TYPE_TEXT = "text";
    public static final String QUERY_TYPE_SHORT_TEXT = "shortText";
    public static final String QUERY_TYPE_TERM_VECTOR = "termVector";
    public static final String QUERY_TYPE_LAYOUT_TOKENS = "layoutToken";
    public static final String QUERY_TYPE_INVALID = "invalid";

    private String software = null;

    private String version = null;

    private String date = null;

    // main text component
    private String text = null;

    // alternative text components for patent input
    private String abstract_ = null;
    private String claims = null;
    private String description = null;

    // search query
    private String shortText = null;

    // language of the query
    private Language language = null;

    // the result of the query disambiguation and enrichment for each identified entities
    private List<NerdEntity> entities = null;

    // the sentence position if such segmentation is to be realized
    private List<Sentence> sentences = null;
    
    // a list of optional language codes for having multilingual Wikipedia sense correspondences
    // note that the source language is by default the language of results, here ae additional
    // result correspondences in target languages for each entities
    // *NOTE*: This field is deprecated since the multilingual information will be provided using the knowledge base API for all supported languages
    @Deprecated
    private List<String> resultLanguages = null;

    // runtime in ms of the last processing
    private long runtime = 0;

    // mention techniques, specify the method for which the mentions are extracted
    private List<ProcessText.MentionMethod> mentions =
            Arrays.asList(ProcessText.MentionMethod.ner, ProcessText.MentionMethod.wikipedia);

    private boolean nbest = false;
    private boolean sentence = false;
    private String customisation = "generic";

    // list of sentences to be processed
    private Integer[] processSentence = null;

    // weighted vector to be disambiguated
    private List<WeightedTerm> termVector = null;

    // distribution of (Wikipedia) categories corresponding to the disambiguated object
    // (text, term vector or search query)
    private List<Category> globalCategories = null;

    // in case the input to be processed is a list of LayoutToken (text then ust be null)
    private List<LayoutToken> tokens = null;

    private NerdContext context = null;

    // only the entities fulfilling the constraints expressed in the filter will be
    // disambiguated and output
    private Filter filter = null;

    // indicate if the full description of the entities should be included in the result
    @Deprecated
    private boolean full = false;

    // query-based threshold, override default values in the config file only for the present query
    private double minSelectorScore = 0.0;
    private double minRankerScore = 0.0;

    // the type of document structure to be considered in case of processing 
    // a complete document 
    private String structure = "grobid";

    public NerdQuery() {
    }

    public NerdQuery(NerdQuery query) {
        this.software = query.getSoftware();
        this.version = query.getVersion();
        this.date = query.getDate();
        this.text = query.getText();
        this.shortText = query.getShortText();
        this.tokens = query.getTokens();

        this.abstract_ = query.getAbstract_();
        this.claims = query.getClaims();
        this.description = query.getDescription();

        this.language = query.getLanguage();
        this.entities = query.getEntities();
        this.sentences = query.getSentences();

        this.mentions = query.getMentions();
        this.nbest = query.getNbest();
        this.sentence = query.getSentence();
        this.customisation = query.getCustomisation();
        this.processSentence = query.getProcessSentence();

        this.termVector = query.getTermVector();
        this.globalCategories = query.getGlobalCategories();

        this.filter = query.getFilter();
        this.context = query.getContext();

        this.minSelectorScore = query.getMinSelectorScore();
        this.minRankerScore = query.getMinRankerScore();

        this.structure = query.getStructure();
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Shortcut to fetch either text or shortText, this logic is the first approach to accessing the content of the
     * query text. The method getQueryType() shall be synchronised with the type of result of this method.
     */
    @JsonIgnore
    public String getTextOrShortText() {
        if (text == null) {
            return shortText;
        }

        return text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAbstract_() {
        return abstract_;
    }

    public void setAbstract_(String abstract_) {
        this.abstract_ = abstract_;
    }

    public String getClaims() {
        return claims;
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLanguage(Language lang) {
        this.language = lang;
    }

    public Language getLanguage() {
        return language;
    }

    public void setResultLanguages(List<String> langs) {
        this.resultLanguages = langs;
    }

    public List<String> getResultLanguages() {
        return this.resultLanguages;
    }

    public List<ProcessText.MentionMethod> getMentions() {
        return this.mentions;
    }

    public void setMentions(List<ProcessText.MentionMethod> mentions) {
        this.mentions = mentions;
    }

    public void addMention(ProcessText.MentionMethod mention) {
        if (this.mentions == null)
            mentions = new ArrayList<ProcessText.MentionMethod>();
        mentions.add(mention);
    }

    public void setRuntime(long tim) {
        runtime = tim;
    }

    public long getRuntime() {
        return runtime;
    }

    public List<WeightedTerm> getTermVector() {
        return termVector;
    }

    public void setTermVector(List<WeightedTerm> termVector) {
        this.termVector = termVector;
    }

    public List<NerdEntity> getEntities() {
        return entities;
    }

    public void setAllEntities(List<Mention> nerEntities) {
        if (nerEntities != null) {
            this.entities = new ArrayList<NerdEntity>();
            for (Mention entity : nerEntities) {
                this.entities.add(new NerdEntity(entity));
            }
        }
    }

    public void setEntities(List<NerdEntity> entities) {
        this.entities = entities;
    }

    public void addNerdEntities(List<NerdEntity> theEntities) {
        if (theEntities != null) {
            if (this.entities == null) {
                this.entities = new ArrayList<NerdEntity>();
            }
            for (NerdEntity entity : theEntities) {
                this.entities.add(entity);
            }
        }
    }

    public List<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences = sentences;
    }

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public boolean getNbest() {
        return nbest;
    }

    public void setNbest(boolean nbest) {
        this.nbest = nbest;
    }

    public boolean getSentence() {
        return sentence;
    }

    public void setSentence(boolean sentence) {
        this.sentence = sentence;
    }

    public String getCustomisation() {
        return customisation;
    }

    public void setCustomisation(String customisation) {
        this.customisation = customisation;
    }

    public Integer[] getProcessSentence() {
        return processSentence;
    }

    public void setProcessSentence(Integer[] processSentence) {
        this.processSentence = processSentence;
    }

    public void addEntities(List<NerdEntity> newEntities) {
        if (entities == null) {
            entities = new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(newEntities)) {
            return;
        }
        for (NerdEntity entity : newEntities) {
            entities.add(entity);
        }
    }

    public void addEntity(NerdEntity entity) {
        if (entities == null) {
            entities = new ArrayList<>();
        }
        entities.add(entity);
    }

    public List<Category> getGlobalCategories() {
        return globalCategories;
    }

    public void setGlobalCategories(List<Category> globalCategories) {
        this.globalCategories = globalCategories;
    }

    public void addGlobalCategory(Category category) {
        if (globalCategories == null) {
            globalCategories = new ArrayList<Category>();
        }
        globalCategories.add(category);
    }

    public List<LayoutToken> getTokens() {
        return tokens;
    }

    public void setTokens(List<LayoutToken> tokens) {
        this.tokens = tokens;
    }

    public NerdContext getContext() {
        return this.context;
    }

    public void setContext(NerdContext nerdContext) {
        this.context = nerdContext;
    }

    public Filter getFilter() {
        return this.filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * @deprecated Will be removed after the next release.
     */
    @Deprecated
    public boolean getFull() {
        return this.full;
    }

    /**
     * @deprecated Will be removed after the next release.
     */
    @Deprecated
    public void setFull(boolean full) {
        this.full = full;
    }

    public double getMinSelectorScore() {
        return this.minSelectorScore;
    }

    public void setMinSelectorScore(double minSelectorScore) {
        this.minSelectorScore = minSelectorScore;
    }

    public double getMinRankerScore() {
        return this.minRankerScore;
    }

    public void setMinRankerScore(double minRankerScore) {
        this.minRankerScore = minRankerScore;
    }

    public String getStructure() {
        return this.structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        mapper.enable(JsonParser.Feature.IGNORE_UNDEFINED);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        String json = null;
        try {
            json = mapper.writeValueAsString(this);

        } catch (IOException e) {
            throw new QueryException("Cannot serialise the nerd query to JSON", e);
        }
        return json;
    }

    /*public String toJSONFullClean() {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");

        // server runtime is always present (even at 0.0)
        buffer.append("\"runtime\": " + runtime);

        // parameters
        buffer.append(", \"nbest\": " + nbest);

        // parameters
        if ((processSentence != null) && (processSentence.length > 0)) {
            buffer.append(", \"processSentence\": [");
            for (int i = 0; i < processSentence.length; i++) {
                if (i != 0) {
                    buffer.append(", ");
                }
                buffer.append(processSentence[i].intValue());
            }
            buffer.append("]");
        }

        // surface form
        if (text != null) {
            byte[] encoded = encoder.quoteAsUTF8(text);
            String output = new String(encoded);
            buffer.append(", \"text\": \"" + output + "\"");
            if (CollectionUtils.isNotEmpty(sentences)) {
                buffer.append(",").append(Sentence.listToJSON(sentences));
            }
        }

        if (shortText != null) {
            byte[] encoded = encoder.quoteAsUTF8(shortText);
            String output = new String(encoded);
            buffer.append(", \"shortText\": \"" + output + "\"");
        }

        if (CollectionUtils.isNotEmpty(termVector)) {
            buffer.append(", \"termVector\": [ ");
            boolean begin = true;
            for (WeightedTerm term : termVector) {
                if (!begin)
                    buffer.append(", ");
                else
                    begin = false;
                buffer.append(term.toJson());
            }
            buffer.append(" ]");
        }

        String lang = "en"; // default language
        if (language != null) {
            buffer.append(", \"language\": " + language.toJSON());
            lang = language.getLang();
        }

        // if available, document level distribution of categories
        if (CollectionUtils.isNotEmpty(globalCategories)) {
            buffer.append(", \"global_categories\": [");
            boolean first = true;
            for (com.scienceminer.nerd.kb.Category category : globalCategories) {
                byte[] encoded = encoder.quoteAsUTF8(category.getName());
                String output = new String(encoded);
                if (first) {
                    first = false;
                } else
                    buffer.append(", ");
                buffer.append("{\"weight\" : " + category.getWeight() + ", \"source\" : \"wikipedia-" + lang
                        + "\", \"category\" : \"" + output + "\", ");
                buffer.append("\"page_id\" : " + category.getWikiPageID() + "}");
            }
            buffer.append("]");
        }

        if (CollectionUtils.isNotEmpty(entities)) {
            buffer.append(", \"entities\": [");
            boolean first = true;
            for (NerdEntity entity : entities) {
                if (filter != null) {
                    List<Statement> statements = entity.getStatements();
                    if ( (statements == null) && 
                         ( (filter.getValueMustNotMatch() == null) || (filter.getValueMustMatch() != null) ) )
                        continue;
                    if (statements != null) {
                        if (!filter.valid(statements))
                            continue;
                    }
                }

                if (first)
                    first = false;
                else
                    buffer.append(", ");
                buffer.append(entity.toJsonFull());
            }
            buffer.append("]");
        }

        // possible page information
        // page height and width
        if (doc != null) {
            List<Page> pages = doc.getPages();
            boolean first = true;
            if (pages != null) {
                buffer.append(", \"pages\":[");
                for (Page page : pages) {
                    if (first)
                        first = false;
                    else
                        buffer.append(", ");
                    buffer.append("{\"page_height\":" + page.getHeight());
                    buffer.append(", \"page_width\":" + page.getWidth() + "}");
                }
                buffer.append("]");
            }
        }

        buffer.append("}");

        return buffer.toString();
    }*/

    public String toJSONClean() {
        return toJSONClean(null);
    }

    public String toJSONClean(Document doc) {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");

        // for metadata
        if (software != null) {
            byte[] encoded = encoder.quoteAsUTF8(software);
            String output = new String(encoded);
            buffer.append("\"software\": \"" + output + "\"");
        }
        if (version != null) {
            byte[] encoded = encoder.quoteAsUTF8(version);
            String output = new String(encoded);
            buffer.append(", \"version\": \"" + output + "\"");
        }
        if (date != null) {
            byte[] encoded = encoder.quoteAsUTF8(date);
            String output = new String(encoded);
            buffer.append(", \"date\": \"" + output + "\"");
        }

        // server runtime is always present (even at 0.0)
        buffer.append(", \"runtime\": " + runtime);

        // parameters
        buffer.append(", \"nbest\": " + nbest);

        // parameters
        if (ArrayUtils.isNotEmpty(processSentence)) {
            buffer.append(", \"processSentence\": [");
            for (int i = 0; i < processSentence.length; i++) {
                if (i != 0) {
                    buffer.append(", ");
                }
                buffer.append(processSentence[i].intValue());
            }
            buffer.append("]");
        }

        // surface form
        if (text != null) {
            byte[] encoded = encoder.quoteAsUTF8(text);
            String output = new String(encoded);
            buffer.append(", \"text\": \"" + output + "\"");
            if (CollectionUtils.isNotEmpty(sentences)) {
                buffer.append(",").append(Sentence.listToJSON(sentences));
            }
        }

        if (shortText != null) {
            byte[] encoded = encoder.quoteAsUTF8(shortText);
            String output = new String(encoded);
            buffer.append(", \"shortText\": \"" + output + "\"");
        }

        if (CollectionUtils.isNotEmpty(termVector)) {
            buffer.append(", \"termVector\": [ ");
            boolean begin = true;
            for (WeightedTerm term : termVector) {
                if (!begin)
                    buffer.append(", ");
                else
                    begin = false;
                buffer.append(term.toJson());
            }
            buffer.append(" ]");
        }

        String lang = "en"; // default language
        if (language != null) {
            buffer.append(", \"language\": " + language.toJSON());
            lang = language.getLang();
        }

        // if available, document level distribution of categories
        if (CollectionUtils.isNotEmpty(globalCategories)) {
            buffer.append(", \"global_categories\": [");
            boolean first = true;
            for (com.scienceminer.nerd.kb.Category category : globalCategories) {
                byte[] encoded = encoder.quoteAsUTF8(category.getName());
                String output = new String(encoded);
                if (first) {
                    first = false;
                } else
                    buffer.append(", ");
                buffer.append("{\"weight\" : " + category.getWeight() + ", \"source\" : \"wikipedia-" + lang
                        + "\", \"category\" : \"" + output + "\", ");
                buffer.append("\"page_id\" : " + category.getWikiPageID() + "}");
            }
            buffer.append("]");
        }

        if (CollectionUtils.isNotEmpty(entities)) {
            buffer.append(", \"entities\": [");
            boolean first = true;
            for (NerdEntity entity : entities) {
                //if (KBUtilities.isPlant(entity.getWikidataId()))
                //    continue;
                if (filter != null) {
                    List<Statement> statements = entity.getStatements();
                    if ((statements == null) &&
                            ((filter.getValueMustNotMatch() == null) || (filter.getValueMustMatch() != null)))
                        continue;
                    if (statements != null) {
                        if (!filter.valid(statements))
                            continue;
                    }
                }

                if (first)
                    first = false;
                else
                    buffer.append(", ");
                if (this.full) {
                    buffer.append(entity.toJsonFull());
                    //TODO: remove after release
                    //LOGGER.warn("The full json is a deprecated option and will be removed next release. ");
                } else
                    buffer.append(entity.toJsonCompact());
            }
            buffer.append("]");
        }

        // possible page information
        // page height and width
        if (doc != null) {
            List<Page> pages = doc.getPages();
            boolean first = true;
            if (pages != null) {
                buffer.append(", \"pages\":[");
                for (Page page : pages) {
                    if (first)
                        first = false;
                    else
                        buffer.append(", ");
                    buffer.append("{\"page_height\":" + page.getHeight());
                    buffer.append(", \"page_width\":" + page.getWidth() + "}");
                }
                buffer.append("]");
            }
        }

        buffer.append("}");

        return buffer.toString();
    }

    @Override
    public String toString() {
        return "Query [text=" + text + ", shortText=" + shortText + ", terms=" + "]";
    }

    /**
     * Export of standoff annotated text in TEI format
     */
    public String toTEI() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><tei xmlns:ng=\"http://relaxng.org/ns/structure/1.0\" xmlns:exch=\"http://www.epo.org/exchange\" xmlns=\"http://www.tei-c.org/ns/1.0\">");
        buffer.append("<teiHeader>");
        buffer.append(" </teiHeader>");
        String idP = KeyGen.getKey();
        buffer.append("<standoff>");
        int n = 0;
        for (NerdEntity entity : entities) {
            //buffer.append(entity.toTEI(idP, n));
            n++;
        }
        buffer.append("</standoff>");
        if (text != null) {
            buffer.append("<text>");
            buffer.append(text);
            buffer.append("</text>");
        }
        if (shortText != null) {
            buffer.append("<text>");
            buffer.append(shortText);
            buffer.append("</text>");
        }
        buffer.append("</tei>");

        return buffer.toString();
    }

    /**
     * Check that language has been correctly set
     */
    public boolean hasValidLanguage() {
        return language != null && language.getLang() != null
                && TARGET_LANGUAGES.contains(language.getLang());
    }

    public static NerdQuery fromJson(String theQuery) throws QueryException {
        if (StringUtils.isEmpty(theQuery)) {
            throw new QueryException("The query cannot be null or empty:\n " + theQuery);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.readValue(theQuery, NerdQuery.class);
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error("The JSON query cannot be processed\n " + theQuery + "\n ", e);
            throw new QueryException("The JSON query is invalid, please check the format.");
        } catch (IOException e) {
            LOGGER.error("Some serious error when deserialize the JSON object: \n" + theQuery, e);
            throw new QueryException("Some serious error when deserialize the JSON object. Please check the format.");
        }
    }

    /**
     * if text is valid, shortText is set to null... and vice-versa
     */
    @JsonIgnore
    public String getQueryType() {
        if (isNotBlank(text) && text.trim().length() > 1) {
            shortText = null;
            return QUERY_TYPE_TEXT;
        } else if (isNotEmpty(shortText)) {
            text = null;
            return QUERY_TYPE_SHORT_TEXT;
        } else if (CollectionUtils.isNotEmpty(termVector)) {
            return QUERY_TYPE_TERM_VECTOR;
        } else if (isNotBlank(text) && (CollectionUtils.isNotEmpty(getTokens()))) {     // We could have text and tokens
            return QUERY_TYPE_LAYOUT_TOKENS;
        } else {
            return QUERY_TYPE_INVALID;
        }
    }
}