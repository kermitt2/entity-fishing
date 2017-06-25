package com.scienceminer.nerd.kb.model;

import java.util.concurrent.*;

import com.scienceminer.nerd.kb.model.hadoop.DbPage;
import com.scienceminer.nerd.kb.db.*;

/**
 * Represents redirects in Wikipedia.   
 *  
 *  -> to be replaced by com.scienceminer.nerd.kb.Variant
 */
public class Redirect extends Page {
	
	public Redirect(KBLowerEnvironment env, int id) {
		super(env, id);
	}
	
	protected Redirect(KBLowerEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
	}
	
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

