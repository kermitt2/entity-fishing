package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.kb.db.KBLowerEnvironment;
import com.scienceminer.nerd.kb.db.LabelIterator;
import com.scienceminer.nerd.kb.db.PageIterator;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.kb.model.hadoop.DbIntList;
import com.scienceminer.nerd.utilities.NerdConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent the language specific resources of the Knowledge Base, e.g. a 
 * Wikipedia instance, including corresponding word and entity embeddings. 
 * 
 */
public class LowerKnowledgeBase {

	protected static final Logger LOGGER = LoggerFactory.getLogger(LowerKnowledgeBase.class);

	private KBLowerEnvironment env = null;
	private int wikipediaArticleCount = -1;

	public enum Direction {
		In, 
		Out
	}

	/**
	 * Initialises a newly created Wikipedia according to the given configuration. 
	 *  
	 */
	public LowerKnowledgeBase(NerdConfig conf) {
		this.env = new KBLowerEnvironment(conf);
		try {
			this.env.buildEnvironment(conf, false);
		} catch(Exception e) {
			e.printStackTrace();
		} 
	}

	public int getArticleCount() {
		if (wikipediaArticleCount == -1)
			wikipediaArticleCount = this.env.retrieveStatistic(StatisticName.articleCount).intValue();
		return wikipediaArticleCount;
	}

	/**
	 * Returns the environment that this is connected to
	 */
	public KBLowerEnvironment getEnvironment() {
		return env;
	}

	/**
	 * Make ready the full content database of articles
	 * 
	 */
	public void loadFullContentDB() {
		try {
			if (this.env != null)
				this.env.buildFullMarkup(false);
			else 
				LOGGER.error("Environment for Wikipedia full content article DB is null");
		} catch(Exception e) {
			e.printStackTrace();
		} 
	}

	/**
	 * Returns the configuration of this wikipedia dump
	 */
	public NerdConfig getConfig() {
		return env.getConfiguration();
	}

	/**
	 * Returns the root Category from which all other categories can be browsed.
	 * 
	 */
	public com.scienceminer.nerd.kb.model.Category getRootCategory() {
		return new com.scienceminer.nerd.kb.model.Category(env, env.retrieveStatistic(StatisticName.rootCategoryId).intValue());
	}

	/**
	 * Returns the Page referenced by the given id. T
	 */
	public Page getPageById(int id) {
		return Page.createPage(env, id);
	}

	/**
	 * Returns the Page referenced by the given Wikidata id for the language of the Wikipedia. 
	 * The page can be cast into the appropriate type for more specific functionality. 
	 *  
	 * @param id the Wikidata id of the Page to retrieve.
	 * @return the Page referenced by the given id, or null if one does not exist. 
	 */
	/*public Page getPageByWikidataId(String wikidataId) {
		return Page.createPage(env, wikidataId);
	}*/

	/**
	 * Return the list of statements associated to an article id
	 */
	/*public List<Statement> getStatements(int id) {
		// get the concept id first
		String conceptId = env.getDbConceptByPageId().retrieve(id);
		if (conceptId == null)
			return null;
		else
			return env.getDbStatements().retrieve(conceptId);
	}*/

	/**
	 * Returns the Article referenced by the given (case sensitive) title. If the title
	 * matches a redirect, this will be resolved to return the final target.
	 * 
	 */
	public Article getArticleByTitle(String title) {
		if (title == null || title.length() == 0)
			return null;

		title = title.substring(0,1).toUpperCase() + title.substring(1);
		Integer id = env.getDbArticlesByTitle().retrieve(title);

		if (id == null)
			return null;

		Page page = Page.createPage(env, id);
		if (!page.exists())
			return null;

		if (page.getType() == PageType.redirect)
			return ((Redirect)page).getTarget();
		else
			return (Article)page;
	}

	/**
	 * Returns the Category referenced by the given (case sensitive) title. 
	 *
	 */
	public com.scienceminer.nerd.kb.model.Category getCategoryByTitle(String title) {
		title = title.substring(0,1).toUpperCase() + title.substring(1);
		Integer id = env.getDbCategoriesByTitle().retrieve(title);

		if (id == null)
			return null;

		Page page = Page.createPage(env, id);
		if (page.getType() == PageType.category)
			return (com.scienceminer.nerd.kb.model.Category) page;
		else
			return null;
	}

	/**
	 * Returns the Template referenced by the given (case sensitive) title. 
	 *  
	 */
	public Template getTemplateByTitle(String title) {
		title = title.substring(0,1).toUpperCase() + title.substring(1);
		Integer id = env.getDbTemplatesByTitle().retrieve(title);

		if (id == null)
			return null;

		Page page = Page.createPage(env, id);
		if (page.getType() == PageType.template)
			return (Template) page;
		else
			return null;
	}


	/**
	 * Returns the most probable article for a given term. 
	 */
	public Article getMostProbableArticle(String term) {
		Label label = new Label(env, term);
		if (!label.exists()) 
			return null;

		return label.getSenses()[0];
	}

	/**
	 * A convenience method for quickly finding out if the given text is ever used as a label
	 * in Wikipedia. If this returns false, then all of the getArticle methods will return null or empty sets. 
	 * 
	 */
	/*public boolean isLabel(String text)  {
		DbLabel lbl = env.getDbLabel().retrieve(text); 
		return lbl != null;
	}*/

	public Label getLabel(String text)  {
		return new Label(env, text);
	}

	/**
	 * Returns an iterator for all pages in the database, in order of ascending ids.
	 * 
	 */
	public PageIterator getPageIterator() {
		return new PageIterator(env);
	}

	/**
	 * Returns an iterator for all pages in the database of the given type, in order of ascending ids.
	 * 
	 */
	public PageIterator getPageIterator(PageType type) {
		return new PageIterator(env, type);		
	}

	/**
	 * Returns an iterator for all labels in the database, processed according to the given text processor (may be null), in alphabetical order.
	 * 
	 */
	public LabelIterator getLabelIterator() {
		return new LabelIterator(env);
	}

	/**
	 * Returns the list of links in relation to artId with the specified direction (in or out).
	 * 
	 */
	public List<Integer> getLinks(int artId, Direction dir) {
		DbIntList ids = null;
		if (dir == Direction.In)
			ids = env.getDbPageLinkInNoSentences().retrieve(artId);
		else
			ids = env.getDbPageLinkOutNoSentences().retrieve(artId);

		if (ids == null || ids.getValues() == null) 
			return new ArrayList<Integer>();

		return ids.getValues();
	}

	/**
     * Returns the vector for a given word
     * @param word the word
     * @return word vector or null if not found
     */
    public short[] getWordEmbeddings(String word) {
        return env.getDbWordEmbeddings().retrieve(word);
    }

    /**
     * Returns the vector for a given entity
     * @param entity the entity identifier
     * @return entity vector or null if not found
     */
    public short[] getEntityEmbeddings(String entityId) {
        return env.getDbEntityEmbeddings().retrieve(entityId);
    }

    /**
     * @return number of dimensions of the vectors for both word and entity embeddings
     */
    public int getEmbeddingsSize() {
        return env.getEmbeddingsSize();
    }

	public void close() {
		env.close();
		this.env = null;
	}

}
