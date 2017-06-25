package com.scienceminer.nerd.kb.model;

import java.util.*; 

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.Statement;

import com.scienceminer.nerd.kb.model.hadoop.*;

/**
 * Represents a page of type article in Wikipedia, e.g. en entity or a concept
 * 
 *  -> to be replaced by a LanguageConcept object
 */
public class Article extends Page {

	public Article(KBLowerEnvironment env, int id) {
		super(env, id);
	}

	protected Article(KBLowerEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
	}

	/**
	 * Returns a array of {@link Redirect Redirects}, sorted by id, that point to this article.
	 * 
	 * @return	an array of Redirects, sorted by id
	 */
	public Redirect[] getRedirects()  {
		DbIntList tmpRedirects = env.getDbRedirectSourcesByTarget().retrieve(id);
		if (tmpRedirects == null || tmpRedirects.getValues() == null) 
			return new Redirect[0];

		Redirect[] redirects = new Redirect[tmpRedirects.getValues().size()];
		for (int i=0; i<tmpRedirects.getValues().size(); i++)
			redirects[i] = new Redirect(env, tmpRedirects.getValues().get(i));	

		return redirects;	
	}

	/**
	 * Returns an array of {@link Category Categories} that this article belongs to. 
	 * 
	 */
	public Category[] getParentCategories() {
		DbIntList tmpParents = env.getDbArticleParents().retrieve(id);
		if (tmpParents == null || tmpParents.getValues() == null) 
			return new Category[0];

		Category[] parentCategories = new Category[tmpParents.getValues().size()];
		int index = 0;
		for (int id : tmpParents.getValues()) {
			parentCategories[index] = new Category(env, id);
			index++;
		}

		return parentCategories;	
	}

	public Article[] getLinksIn() {			
		DbIntList tmpLinks = env.getDbPageLinkInNoSentences().retrieve(id);
		if (tmpLinks == null || tmpLinks.getValues() == null) 
			return new Article[0];

		Article[] links = new Article[tmpLinks.getValues().size()];

		int index = 0;
		for (Integer id : tmpLinks.getValues()) {
			links[index] = new Article(env, id.intValue());
			index++;
		}

		return links;	
	}

	public Article[] getLinksOut()  {
		DbIntList tmpLinks = env.getDbPageLinkOutNoSentences().retrieve(id);
		if (tmpLinks == null || tmpLinks.getValues() == null) 
			return new Article[0];

		Article[] links = new Article[tmpLinks.getValues().size()];
		int index = 0;
		for (Integer id : tmpLinks.getValues()) {
			links[index] = new Article(env, id.intValue());
			index++;
		}

		return links;	
	}

	/**
	 * Returns the title of the article translated into the language given by ISO 639-1 language code. 
	 */	
	public String getTranslation(String languageCode)  {		
		DbTranslations t = env.getDbTranslations().retrieve(id);
		if (t == null)
			return null;

		if (t.getTranslationsByLangCode() == null)
			return null;

		return t.getTranslationsByLangCode().get(languageCode.toLowerCase());
	}

	/**
	 * Returns a map associating language code with translated title of the article. 
	 * 
	 */	
	public Map<String,String> getTranslations() {
		DbTranslations t = env.getDbTranslations().retrieve(id);
		if (t == null)
			return new TreeMap<String,String>();
		else
			return t.getTranslationsByLangCode();
	}

	/**
	 * @return the total number of links that are made to this article 
	 */
	public int getTotalLinksInCount()  {
		DbPageLinkCounts lc = env.getDbPageLinkCounts().retrieve(id);
		if (lc == null) 
			return 0;
		else
			return lc.getTotalLinksIn();
	}

	/**
	 * @return the number of distinct articles which contain a link to this article 
	 */
	public int getDistinctLinksInCount()  {
		DbPageLinkCounts lc = env.getDbPageLinkCounts().retrieve(id);
		if (lc == null) 
			return 0;
		else
			return lc.getDistinctLinksIn();
	}

	/**
	 * @return the total number links that this article makes to other articles 
	 */
	public int getTotalLinksOutCount() {
		DbPageLinkCounts lc = env.getDbPageLinkCounts().retrieve(id);
		if (lc == null) 
			return 0;
		else
			return lc.getTotalLinksOut();
	}

	/**
	 * @return the number of distinct articles that this article links to 
	 */
	public int getDistinctLinksOutCount() {
		DbPageLinkCounts lc = env.getDbPageLinkCounts().retrieve(id);
		if (lc == null) 
			return 0;
		else
			return lc.getDistinctLinksOut();
	}
}
