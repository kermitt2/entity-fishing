/*
 *    Wikipedia.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.wikipedia.miner.model;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;


import org.apache.log4j.Logger;
import org.wikipedia.miner.db.WEnvironment;
import org.wikipedia.miner.db.WIterator;
import org.wikipedia.miner.db.WEnvironment.StatisticName;
import org.wikipedia.miner.db.struct.DbLabel;
import org.wikipedia.miner.model.Page.PageType;
import org.wikipedia.miner.util.LabelIterator;
import org.wikipedia.miner.util.NGrammer.CaseContext;
import org.wikipedia.miner.util.NGrammer.NGramSpan;
import org.wikipedia.miner.util.PageIterator;
import org.wikipedia.miner.util.ProgressTracker;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.util.text.TextProcessor;
import org.xml.sax.SAXException;

import com.sleepycat.je.EnvironmentLockedException;


/**
 * Represents a single dump or instance of Wikipedia
 */
public class Wikipedia {

	private WEnvironment env ;

	/**
	 * Initialises a newly created Wikipedia according to the given configuration. 
	 * 
	 * This can be a time consuming process if the given configuration specifies databases that need to be cached to memory.
	 * 
	 * This preparation can be done in a separate thread if required, in which case progress can be tracked using {@link #getProgress()}, {@link #getPreparationTracker()} and {@link #isReady()}.
	 *  
	 * @param conf a configuration that describes where the databases are located, etc. 
	 * @param threadedPreparation true if preparation (connecting to databases, caching data to memory) should be done in a separate thread, otherwise false
	 * @throws EnvironmentLockedException if the underlying database environment is unavailable.
	 */
	public Wikipedia(WikipediaConfiguration conf, boolean threadedPreparation) throws EnvironmentLockedException{
		this.env = new WEnvironment(conf, threadedPreparation) ; 
	}

	/**
	 * Initialises a newly created Wikipedia according to the given configuration file. 
	 * 
	 * This can be a time consuming process if the given configuration specifies databases that need to be cached to memory.
	 * 
	 * This preparation can be done in a separate thread if required, in which case progress can be tracked using {@link #getProgress()}, {@link #getPreparationTracker()} and {@link #isReady()}.
	 *  
	 * @param confFile an xml file that describes where the databases are located, etc. 
	 * @param threadedPreparation true if preparation (connecting to databases, caching data to memory) should be done in a separate thread, otherwise false
	 * @throws EnvironmentLockedException if the underlying database environment is unavailable.
	 */
	public Wikipedia(File confFile, boolean threadedPreparation) throws EnvironmentLockedException, ParserConfigurationException, SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException{
		WikipediaConfiguration conf = new WikipediaConfiguration(confFile) ;
		this.env = new WEnvironment(conf, threadedPreparation) ; 
	}


	/**
	 * Returns the environment that this is connected to
	 * 
	 * @return the environment that this is connected to
	 */
	public WEnvironment getEnvironment() {
		return env ;
	}

	/**
	 * Returns the configuration of this wikipedia dump
	 * 
	 * @return the configuration of this wikipedia dump
	 */
	public WikipediaConfiguration getConfig() {
		return env.getConfiguration() ;
	}

	/**
	 * Returns true if the preparation work has been completed, otherwise false
	 * 
	 * @return true if the preparation work has been completed, otherwise false
	 */
	public boolean isReady() {
		return env.isReady() ;

	}

	/**
	 * Returns a number between 0 (just started) and 1 (completed) indicating progress of the preparation work.
	 * 
	 * @return a number between 0 (just started) and 1 (completed) indicating progress of the preparation work. 
	 */
	public double getProgress() {
		return env.getProgress() ;
	}

	/**
	 * Returns a tracker for progress of the preparation work. 
	 * 
	 * @return a tracker for progress of the preparation work. 
	 */
	public ProgressTracker getPreparationTracker() {
		return env.getPreparationTracker() ;
	}

	/**
	 * Returns the root Category from which all other categories can be browsed.
	 * 
	 * @return the root category
	 */
	public Category getRootCategory() {

		return new Category(env, env.retrieveStatistic(StatisticName.rootCategoryId).intValue()) ;
	}

	/**
	 * Returns the Page referenced by the given id. The page can be cast into the appropriate type for 
	 * more specific functionality. 
	 *  
	 * @param id	the id of the Page to retrieve.
	 * @return the Page referenced by the given id, or null if one does not exist. 
	 */
	public Page getPageById(int id) {
		return Page.createPage(env, id) ;
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
			return null ;

		title = title.substring(0,1).toUpperCase() + title.substring(1) ;

		Integer id = env.getDbArticlesByTitle().retrieve(title) ;

		if (id == null)
			return null ;

		Page page = Page.createPage(env, id) ;
		if (!page.exists())
			return null ;

		if (page.getType() == PageType.redirect)
			return ((Redirect)page).getTarget() ;
		else
			return (Article)page ;
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

		title = title.substring(0,1).toUpperCase() + title.substring(1) ;

		Integer id = env.getDbCategoriesByTitle().retrieve(title) ;

		if (id == null)
			return null ;

		Page page = Page.createPage(env, id) ;

		if (page.getType() == PageType.category)
			return (Category) page ;
		else
			return null ;
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

		title = title.substring(0,1).toUpperCase() + title.substring(1) ;

		Integer id = env.getDbTemplatesByTitle().retrieve(title) ;

		if (id == null)
			return null ;

		Page page = Page.createPage(env, id) ;

		if (page.getType() == PageType.template)
			return (Template) page ;
		else
			return null ;
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
	 * @param tp	an optional TextProcessor to modify how the term is searched for. 
	 * 
	 * @return the most likely sense of the given term.
	 * 
	 * for the given text processor.
	 */
	public Article getMostLikelyArticle(String term, TextProcessor tp){

		Label label = new Label(env, term, tp) ;

		if (!label.exists()) 
			return null ;

		return label.getSenses()[0] ;
	}

	/**
	 * A convenience method for quickly finding out if the given text is ever used as a label
	 * in Wikipedia. If this returns false, then all of the getArticle methods will return null or empty sets. 
	 * 
	 * @param text the text to search for
	 * @param tp an optional TextProcessor (may be null)
	 * @return true if there is an anchor corresponding to the given text, otherwise false
	 */
	public boolean isLabel(String text, TextProcessor tp)  {
		DbLabel lbl = env.getDbLabel(tp).retrieve(text) ; 

		return lbl != null ;
	}
	
	public Label getLabel(NGramSpan span, String sourceText) {
		
		//System.out.println("context: " + span.getCaseContext()) ;
		
		String ngram = span.getNgram(sourceText) ;
		
		Label bestLabel = getLabel(ngram) ;
		
		//don't bother trying out different casing variations if we are using casefolder as text processor
		//TextProcessor tp = getConfig().getDefaultTextProcessor() ;
		//if (tp != null && (tp.class. == TextProcessor.class)
		///	return bestLabel ;
		
		//if this starts with capital letter and is at start of sentence, try lower-casing that first letter.
		if (span.getCaseContext() == CaseContext.mixed && span.isSentenceStart() && Character.isUpperCase(ngram.charAt(0))) {
			//System.out.println("trying lower first letter first token") ;
			char tmpNgram[] = ngram.toCharArray() ;
			tmpNgram[0] = Character.toLowerCase(tmpNgram[0]) ;
			
			Label label = getLabel(new String(tmpNgram)) ;
			
			//System.out.println(label.getText()) ;
			
			if (label.exists() && (!bestLabel.exists() || label.getLinkOccCount() > bestLabel.getLinkOccCount())) {
				bestLabel = label ;
				//System.out.println("using lower first letter first token") ;
			}
		}
			
		
		
		//if surrounding text is all lower case or ALL UPPER CASE, try with First Letter Of Each Token Uppercased.
		if (span.getCaseContext() == CaseContext.lower || span.getCaseContext() == CaseContext.upper) {
			//System.out.println("trying upper first letter all tokens") ;
			Label label = getLabel(span.getNgramUpperFirst(sourceText)) ;
			
			if (label.exists() && (!bestLabel.exists() || label.getLinkOccCount() > bestLabel.getLinkOccCount())) {
				bestLabel = label ;
				//System.out.println("using upper first letter all tokens") ;
			}
		}
		
		//if surrounding text is ALL UPPER CASE or Has First Letter Of Each Token Uppercased, try with all lower case 
		if (span.getCaseContext() == CaseContext.upperFirst || span.getCaseContext() == CaseContext.upper) {
			//System.out.println("trying lower") ;
			Label label = getLabel(ngram.toLowerCase()) ;
			
			if (label.exists() && (!bestLabel.exists() || label.getLinkOccCount() > bestLabel.getLinkOccCount())) {
				bestLabel = label ;
				//System.out.println("using lower") ;
			}
		}
		
		//if surrounding text is all lower case, try with ALL UPPER CASE
		if (span.getCaseContext() == CaseContext.lower) {
			//System.out.println("trying upper") ;
			Label label = getLabel(ngram.toUpperCase()) ;
			
			if (label.exists() && (!bestLabel.exists() || label.getLinkOccCount() > bestLabel.getLinkOccCount())) {
				//System.out.println("using upper") ;
				bestLabel = label ;
			}
		}
		
		return bestLabel ;
	}

	public Label getLabel(String text)  {
		return new Label(env, text) ;
	}

	public Label getLabel(String text, TextProcessor tp)  {

		return new Label(env, text, tp) ;
	}


	/**
	 * Returns an iterator for all pages in the database, in order of ascending ids.
	 * 
	 * @return an iterator for all pages in the database, in order of ascending ids.
	 */
	public PageIterator getPageIterator() {
		return new PageIterator(env) ;
	}

	/**
	 * Returns an iterator for all pages in the database of the given type, in order of ascending ids.
	 * 
	 * @param type the type of page of interest
	 * @return an iterator for all pages in the database of the given type, in order of ascending ids.
	 */
	public PageIterator getPageIterator(PageType type) {
		return new PageIterator(env, type) ;		
	}

	/**
	 * Returns an iterator for all labels in the database, processed according to the given text processor (may be null), in alphabetical order.
	 * 
	 * @param tp the text processor
	 * @return an iterator for all labels in the database, processed according to the given text processor (may be null), in alphabetical order.
	 */
	public LabelIterator getLabelIterator(TextProcessor tp) {
		return new LabelIterator(env, tp) ;
	}

	/**
	 * Tidily closes the database environment behind this wikipedia instance. This should be done whenever
	 * one is finished using it. 
	 */
	public void close() {
		env.close();
		this.env = null ;
	}

	@Override
	public void finalize() {
            try {
                if (this.env != null)
                    Logger.getLogger(WIterator.class).warn("Unclosed wikipedia. You may be causing a memory leak.") ;
            } finally {
                try {
                    super.finalize();
                } catch (Throwable ex) {
                    Logger.getLogger(WIterator.class).warn("Unclosed wikipedia. You may be causing a memory leak.") ;
                }
            }
	}
}
