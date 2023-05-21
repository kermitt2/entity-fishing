package com.scienceminer.nerd.kb;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.lang.Language;
import org.grobid.core.utilities.LanguageUtilities;

import com.scienceminer.nerd.kb.db.KBUpperEnvironment;
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
  			Language.EN, Language.FR, Language.DE, Language.IT, Language.ES, "ar", "zh", "ja", "ru", "pt", "fa", "uk", "sv", "bn", "hi");
 
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
            File yamlFile = new File("data/config/kb.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            NerdConfig conf = mapper.readValue(yamlFile, NerdConfig.class);
			this.env = new KBUpperEnvironment(conf);
			this.env.buildEnvironment(conf, false);

			wikipedias = new HashMap<>();
            wikipediaDomainMaps = new HashMap<>();

            LOGGER.info("Init English lower Knowledge-base layer");
            yamlFile = new File("data/config/wikipedia-en.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);
			LowerKnowledgeBase wikipedia_en = new LowerKnowledgeBase(conf);

			wikipedias.put(Language.EN, wikipedia_en);
            WikipediaDomainMap wikipediaDomainMaps_en = new WikipediaDomainMap(Language.EN, conf.getDbDirectory());
            wikipediaDomainMaps_en.setWikipedia(wikipedia_en);
            wikipediaDomainMaps_en.createAllMappings();
            wikipediaDomainMaps.put(Language.EN, wikipediaDomainMaps_en);
			
			LOGGER.info("Init German lower Knowledge-base layer (if present)");
			yamlFile = new File("data/config/wikipedia-de.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_de = new LowerKnowledgeBase(conf);
			wikipedias.put(Language.DE, wikipedia_de);
            wikipediaDomainMaps.put(Language.DE, wikipediaDomainMaps_en);

            LOGGER.info("Init French lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-fr.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_fr = new LowerKnowledgeBase(conf);
			wikipedias.put(Language.FR, wikipedia_fr);
            wikipediaDomainMaps.put(Language.FR, wikipediaDomainMaps_en);

            LOGGER.info("Init Spanish lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-es.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_es = new LowerKnowledgeBase(conf);
			wikipedias.put(Language.ES, wikipedia_es);
            wikipediaDomainMaps.put(Language.ES, wikipediaDomainMaps_en);

            LOGGER.info("Init Italian lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-it.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_it = new LowerKnowledgeBase(conf);
			wikipedias.put(Language.IT, wikipedia_it);
            wikipediaDomainMaps.put(Language.IT, wikipediaDomainMaps_en);

            LOGGER.info("Init Arabic lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-ar.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_ar = new LowerKnowledgeBase(conf);
			wikipedias.put("ar", wikipedia_ar);
            wikipediaDomainMaps.put("ar", wikipediaDomainMaps_en);

            LOGGER.info("Init Madarin lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-zh.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_zh = new LowerKnowledgeBase(conf);
			wikipedias.put("zh", wikipedia_zh);
            wikipediaDomainMaps.put("zh", wikipediaDomainMaps_en);

            LOGGER.info("Init Japanese lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-ja.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_ja = new LowerKnowledgeBase(conf);
			wikipedias.put("ja", wikipedia_ja);
            wikipediaDomainMaps.put("ja", wikipediaDomainMaps_en);

			LOGGER.info("Init Russian lower Knowledge-base layer (if present)");
			yamlFile = new File("data/config/wikipedia-ru.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_ru = new LowerKnowledgeBase(conf);
			wikipedias.put("ru", wikipedia_ru);
            wikipediaDomainMaps.put("ru", wikipediaDomainMaps_en);

            LOGGER.info("Init Portuguese lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-pt.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_pt = new LowerKnowledgeBase(conf);
			wikipedias.put("pt", wikipedia_pt);
            wikipediaDomainMaps.put("pt", wikipediaDomainMaps_en);

            LOGGER.info("Init Farsi lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-fa.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_fa = new LowerKnowledgeBase(conf);
			wikipedias.put("fa", wikipedia_fa);
            wikipediaDomainMaps.put("fa", wikipediaDomainMaps_en);

            LOGGER.info("Init Swedish lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-sv.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_sv = new LowerKnowledgeBase(conf);
			wikipedias.put("sv", wikipedia_sv);
            wikipediaDomainMaps.put("sv", wikipediaDomainMaps_en);

            LOGGER.info("Init Ukrainian lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-uk.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_uk = new LowerKnowledgeBase(conf);
			wikipedias.put("uk", wikipedia_uk);
            wikipediaDomainMaps.put("uk", wikipediaDomainMaps_en);

            LOGGER.info("Init Bengali lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-bn.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_bn = new LowerKnowledgeBase(conf);
			wikipedias.put("bn", wikipedia_bn);
            wikipediaDomainMaps.put("bn", wikipediaDomainMaps_en);

            LOGGER.info("Init Hindi lower Knowledge-base layer (if present)");
            yamlFile = new File("data/config/wikipedia-hi.yaml");
            yamlFile = new File(yamlFile.getAbsolutePath());
            conf = mapper.readValue(yamlFile, NerdConfig.class);;
			LowerKnowledgeBase wikipedia_hi = new LowerKnowledgeBase(conf);
			wikipedias.put("hi", wikipedia_hi);
            wikipediaDomainMaps.put("hi", wikipediaDomainMaps_en);

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