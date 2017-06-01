package com.scienceminer.nerd.kb.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

//import org.apache.log4j.Logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.utilities.TextUtilities;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.utilities.NerdConfig;

import org.wikipedia.miner.db.struct.DbLabel;
import org.wikipedia.miner.db.struct.DbIntList;

import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.disambiguation.ProcessText.CaseContext;

import org.xml.sax.SAXException;

import com.scienceminer.nerd.kb.Property;
import com.scienceminer.nerd.kb.Relation;

/**
 * Represent a language specific instance of Wikipedia
 * 
 * -> to be replaced by com.scienceminer.nerd.kb.KnowledgeBase
 */
public class Wikipedia {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Wikipedia.class);

	private KBEnvironment env = null;
	private int wikipediaArticleCount = -1;

	public enum Direction {
		In, 
		Out
	}

	/**
	 * Initialises a newly created Wikipedia according to the given configuration. 
	 *  
	 * @param conf a Nerd configuration 
	 */
	public Wikipedia(NerdConfig conf) {
		this.env = new KBEnvironment(conf);
		try {
			this.env.buildEnvironment(conf, false);
		} catch(Exception e) {
			e.printStackTrace();
		} 
	}

	public int getArticleCount() {
		if (wikipediaArticleCount == -1)
			wikipediaArticleCount = new Long(this.env.retrieveStatistic(StatisticName.articleCount)).intValue();
		return wikipediaArticleCount;
	}

	/**
	 * Returns the environment that this is connected to
	 * 
	 * @return the environment that this is connected to
	 */
	public KBEnvironment getEnvironment() {
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
	 * 
	 * @return the configuration of this wikipedia dump
	 */
	public NerdConfig getConfig() {
		return env.getConfiguration();
	}

	/**
	 * Returns the root Category from which all other categories can be browsed.
	 * 
	 * @return the root category
	 */
	public Category getRootCategory() {
		return new Category(env, env.retrieveStatistic(StatisticName.rootCategoryId).intValue());
	}

	/**
	 * Returns the Page referenced by the given id. The page can be cast into the appropriate type for 
	 * more specific functionality. 
	 *  
	 * @param id	the id of the Page to retrieve.
	 * @return the Page referenced by the given id, or null if one does not exist. 
	 */
	public Page getPageById(int id) {
		return Page.createPage(env, id);
	}

	/**
	 * Return the list of properties associated to an article id
	 */
	public List<Property> getProperties(int id) {
		return env.getDbProperties().retrieve(id);
	}

	/**
	 * Return the list of relations associated to an article id
	 */
	public List<Relation> getRelations(int id) {
		return env.getDbRelations().retrieve(id);
	}

	/**
	 * Returns the Article referenced by the given (case sensitive) title. If the title
	 * matches a redirect, this will be resolved to return the redirect's target.
	 * <p>
	 * The given title must be matched exactly to return an article. If you want some more lee-way,
	 * use getMostLikelyArticle() instead. 
	 *  
	 * @param title	the title of an Article (or its redirect).
	 * @return the Article referenced by the given title, or null if one does not exist
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
	 * The given title must be matched exactly to return a Category. 
	 *  
	 * @param title	the title of an Article (or it's redirect).
	 * @return the Article referenced by the given title, or null if one does not exist
	 */
	public Category getCategoryByTitle(String title) {
		title = title.substring(0,1).toUpperCase() + title.substring(1);
		Integer id = env.getDbCategoriesByTitle().retrieve(title);

		if (id == null)
			return null;

		Page page = Page.createPage(env, id);
		if (page.getType() == PageType.category)
			return (Category) page;
		else
			return null;
	}

	/**
	 * Returns the Template referenced by the given (case sensitive) title. 
	 * 
	 * The given title must be matched exactly to return a Template. 
	 *  
	 * @param title the title of a Template.
	 * @return the Template referenced by the given title, or null if one does not exist
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
	 * Returns the most likely article for a given term. For example, searching for "tree" will return
	 * the article "30579: Tree", rather than "30806: Tree (data structure)" or "7770: Christmas tree"
	 * This is defined by the number of times the term is used as an anchor for links to each of these 
	 * destinations. 
	 *  <p>
	 * An optional text processor (may be null) can be used to alter the way labels are 
	 * retrieved (e.g. via stemming or case folding) 
	 * 
	 * @param term	the term to obtain articles for
	 * 
	 * @return the most likely sense of the given term.
	 * 
	 * for the given text processor.
	 */
	public Article getMostLikelyArticle(String term){
		Label label = new Label(env, term);
		if (!label.exists()) 
			return null;

		return label.getSenses()[0];
	}

	/**
	 * A convenience method for quickly finding out if the given text is ever used as a label
	 * in Wikipedia. If this returns false, then all of the getArticle methods will return null or empty sets. 
	 * 
	 * @param text the text to search for
	 * @return true if there is an anchor corresponding to the given text, otherwise false
	 */
	public boolean isLabel(String text)  {
		DbLabel lbl = env.getDbLabel().retrieve(text); 

		return lbl != null;
	}

	public Label getLabel(String text)  {
		return new Label(env, text);
	}

	/**
	 * Returns an iterator for all pages in the database, in order of ascending ids.
	 * 
	 * @return an iterator for all pages in the database, in order of ascending ids.
	 */
	public PageIterator getPageIterator() {
		return new PageIterator(env);
	}

	/**
	 * Returns an iterator for all pages in the database of the given type, in order of ascending ids.
	 * 
	 * @param type the type of page of interest
	 * @return an iterator for all pages in the database of the given type, in order of ascending ids.
	 */
	public PageIterator getPageIterator(PageType type) {
		return new PageIterator(env, type);		
	}

	/**
	 * Returns an iterator for all labels in the database, processed according to the given text processor (may be null), in alphabetical order.
	 * 
	 * @return an iterator for all labels in the database, processed according to the given text processor (may be null), in alphabetical order.
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

	public void close() {
		env.close();
		this.env = null;
	}

	@Override
	public void finalize() {
        try {
            if (this.env != null)
                LOGGER.warn("Unclosed wikipedia. You may be causing a memory leak.");
        } finally {
            try {
                super.finalize();
            } catch (Throwable ex) {
                LOGGER.warn("Unclosed wikipedia. You may be causing a memory leak.");
            }
        }
	}
}
