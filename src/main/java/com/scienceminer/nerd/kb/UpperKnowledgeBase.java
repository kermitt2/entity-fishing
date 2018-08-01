package com.scienceminer.nerd.kb;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.lang.Language;
import org.grobid.core.utilities.LanguageUtilities;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.model.Page.PageType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * The class offering an access to the (N)ERD upper-level Knowledge Base, concepts and 
 * all the language indenpendent semantic information, and access to language-specific data 
 * level. There is a unique instance of this class in a (N)ERD service.
 * 
 */
public class UpperKnowledgeBase {
	protected static final Logger LOGGER = LoggerFactory.getLogger(UpperKnowledgeBase.class);
    private static volatile UpperKnowledgeBase instance;

	private KBUpperEnvironment env = null;

	private Map<String, LowerKnowledgeBase> wikipedias = null;
    private Map<String, WikipediaDomainMap> wikipediaDomainMaps = null;

	private long conceptCount = -1;

	// this is the list of supported languages 
  	public static final List<String> TARGET_LANGUAGES = Arrays.asList(
  			Language.EN, Language.FR, Language.DE, Language.IT, Language.ES);
 
	 public static UpperKnowledgeBase getInstance() {
        if (instance == null) {
			getNewInstance();
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
	private static synchronized void getNewInstance() {
		LOGGER.debug("Get new instance of UpperKnowledgeBase");
		instance = new UpperKnowledgeBase();
	}

    /**
     * Hidden constructor
     * Initialises a newly created Upper-level knowledge base
     */
    private UpperKnowledgeBase() {
    	try {
    		LOGGER.info("Init Lexicon");
    		Lexicon.getInstance();
    		LOGGER.info("Lexicon initialized");
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

            LOGGER.info("\nInit Upper Knowledge-base layer");
            NerdConfig conf = mapper.readValue(new File("data/config/kb.yaml"), NerdConfig.class);
			this.env = new KBUpperEnvironment(conf);
			this.env.buildEnvironment(conf, false);

			wikipedias = new HashMap<>();
            wikipediaDomainMaps = new HashMap<>();

            LOGGER.info("Init English lower Knowledge-base layer");
            conf = mapper.readValue(new File("data/config/wikipedia-en.yaml"), NerdConfig.class);
			LowerKnowledgeBase wikipedia_en = new LowerKnowledgeBase(conf);

			wikipedias.put(Language.EN, wikipedia_en);
            WikipediaDomainMap wikipediaDomainMaps_en = new WikipediaDomainMap(Language.EN, conf.getDbDirectory());
            wikipediaDomainMaps_en.setWikipedia(wikipedia_en);
            wikipediaDomainMaps_en.createAllMappings();
            wikipediaDomainMaps.put(Language.EN, wikipediaDomainMaps_en);
			
			LOGGER.info("Init German lower Knowledge-base layer");
            conf = mapper.readValue(new File("data/config/wikipedia-de.yaml"), NerdConfig.class);;
			LowerKnowledgeBase wikipedia_de = new LowerKnowledgeBase(conf);
			wikipedias.put(Language.DE, wikipedia_de);
            wikipediaDomainMaps.put(Language.DE, wikipediaDomainMaps_en);

            LOGGER.info("Init French lower Knowledge-base layer");
            conf = mapper.readValue(new File("data/config/wikipedia-fr.yaml"), NerdConfig.class);;
			LowerKnowledgeBase wikipedia_fr = new LowerKnowledgeBase(conf);
			wikipedias.put(Language.FR, wikipedia_fr);
            wikipediaDomainMaps.put(Language.FR, wikipediaDomainMaps_en);

            LOGGER.info("Init Spanish lower Knowledge-base layer");
            conf = mapper.readValue(new File("data/config/wikipedia-es.yaml"), NerdConfig.class);;
			LowerKnowledgeBase wikipedia_es = new LowerKnowledgeBase(conf);
			wikipedias.put(Language.ES, wikipedia_es);
            wikipediaDomainMaps.put(Language.ES, wikipediaDomainMaps_en);

            LOGGER.info("Init Italian lower Knowledge-base layer");
            conf = mapper.readValue(new File("data/config/wikipedia-it.yaml"), NerdConfig.class);;
			LowerKnowledgeBase wikipedia_it = new LowerKnowledgeBase(conf);
			wikipedias.put(Language.IT, wikipedia_it);
            wikipediaDomainMaps.put(Language.IT, wikipediaDomainMaps_en);

			LOGGER.info("End of Initialization of Wikipedia environments");

			LOGGER.info("Init Grobid") ;
            Utilities.initGrobid();
		} catch(Exception e) {
			e.printStackTrace();
		} 
	}

	public LowerKnowledgeBase getWikipediaConf(String lang) {
		return wikipedias.get(lang);
	}
	
	public Map<String, LowerKnowledgeBase> getWikipediaConfs() {
		return wikipedias;
	}

    public Map<String, WikipediaDomainMap> getWikipediaDomainMaps () {
        return wikipediaDomainMaps;
    }

	public long getEntityCount() {
		if (conceptCount == -1)
			conceptCount = env.getDbConcepts().getDatabaseSize();
		return conceptCount;
	}

	/**
	 * Return the concept object corresponding to a given wikidata ID
	 */
	public Concept getConcept(String wikidataId) {
		if (env.getDbConcepts().retrieve(wikidataId) == null) 
			return null;
		else
			return new Concept(env, wikidataId);
	}

	/**
	 * Return the page id corresponding to a given concept id and a target lang
	 */
	public Integer getPageIdByLang(String wikidataId, String lang) {
		if (env.getDbConcepts().retrieve(wikidataId) == null) 
			return null;
		else {
			Concept concept = new Concept(env, wikidataId);
			return concept.getPageIdByLang(lang);
		}
	}

	/**
	 * Return the definition of a property
	 */
	public Property getProperty(String propertyId) {
		return env.getDbProperties().retrieve(propertyId);
	}

	/**
	 * Return the list of statements associated to a given concept id as head
	 */
	public List<Statement> getStatements(String wikidataId) {
		//System.out.println("get statements for: " + wikidataId);
		//List<Statement> statements = env.getDbStatements().retrieve(wikidataId);
		//if (statements != null)
		//	System.out.println(statements.size() + " statements: ");

		return env.getDbStatements().retrieve(wikidataId);
	}

	/**
	 * Return the list of statements associated to a given concept id as tail
	 */
	public List<Statement> getReverseStatements(String wikidataId) {
		//System.out.println("get reverse statements for: " + wikidataId);
		//List<Statement> statements = env.getDbReverseStatements().retrieve(wikidataId);
		//if (statements != null)
		//	System.out.println(statements.size() + " statements: ");
		return env.getDbReverseStatements().retrieve(wikidataId);
	}

	/**
	 * Returns an iterator for all pages in the database, in order of ascending ids.
	 * 
	 */
	public KBIterator getEntityIterator() {
		return new KBIterator(env.getDbConcepts());
	}

	/**
	 * Load on demand the reverse statement database (get statements by the tail entities), 
	 * which is not loaded by default.
	 */
	public void loadReverseStatementDatabase(boolean overwrite) {
		env.loadReverseStatementDatabase(overwrite);
	}

	public String getEntityIdPerDoi(String doi) {
		return env.getDbBiblio().retrieve(doi);
	}

	/**
	 * Return the list of immediate parent taxons (P171) for a given taxon, null for empty list and non-taxon
	 */
	public List<String> getParentTaxons(String wikidataId) {
		return env.getDbTaxonParent().retrieve(wikidataId);
	}

	/**
	 * Return the full list of parent taxons (P171) for a given taxon along the taxon hierarchy, null for empty list and non-taxon
	 */
	public List<String> getFullParentTaxons(String wikidataId) {
		List<String> taxons = env.getDbTaxonParent().retrieve(wikidataId);
		if (CollectionUtils.isEmpty(taxons)) {
			return null;
		}
		List<String> result = new ArrayList<String>();
		for(String taxonId : taxons) {
			if (!result.contains(taxonId))
				result.add(taxonId);
			List<String> parents = getFullParentTaxons(taxonId);
			if (!CollectionUtils.isEmpty(parents)) {
				for(String parentId : parents) {
					if (!result.contains(parentId))
						result.add(parentId);
				}
			}
		}
		return result;
	}

	public void close() {
		// close wikipedia instances
		for (Map.Entry<String, LowerKnowledgeBase> entry : wikipedias.entrySet()) {
			LowerKnowledgeBase wikipedia = entry.getValue();
			wikipedia.close();
		}
		env.close();
		this.env = null;
	}
}