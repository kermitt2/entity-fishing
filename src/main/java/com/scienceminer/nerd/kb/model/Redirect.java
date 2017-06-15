package com.scienceminer.nerd.kb.model;

import java.util.concurrent.*;

import com.scienceminer.nerd.kb.model.hadoop.DbPage;
import com.scienceminer.nerd.kb.db.*;

/**
 * Represents redirects in Wikipedia; the links that have been defined to connect synonyms to the correct article
 * (i.e <em>Farming</em> redirects to <em>Agriculture</em>).   
 *  
 *  -> to be removed
 */
public class Redirect extends Page {
	
	/**
	 * Initialises a newly created Redirect so that it represents the article given by <em>id</em>.
	 * 
	 * @param env	an active KBLowerEnvironment
	 * @param id	the unique identifier of the article
	 */
	public Redirect(KBLowerEnvironment env, int id) {
		super(env, id);
	}
	
	protected Redirect(KBLowerEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
	}
	
	/**
	 * Returns the Article that this redirect points to. This will continue following redirects until it gets to an article 
	 * (so it deals with double redirects). If a dead-end or loop of redirects is encountered, null is returned
	 * 
	 * @return	the equivalent Article for this redirect.
	 */	//TODO: should just resolve double redirects during extraction.
	public Article getTarget() {

		int currId = id;

		ConcurrentMap redirectsFollowed = new ConcurrentHashMap();

		while (redirectsFollowed.get(currId) == null) {
			redirectsFollowed.put(currId, currId);

			Integer targetId = env.getDbRedirectTargetBySource().retrieve(currId);

			if (targetId == null) 
				return null;
			
			Page target = Page.createPage(env, targetId);
			
			if (!target.exists())
				return null;
			
			if (target.getType() == PageType.redirect)
				currId = targetId;
			else if (target.getType() == PageType.article)
				return (Article)target;
		}

		return null;		
	}
	
}

