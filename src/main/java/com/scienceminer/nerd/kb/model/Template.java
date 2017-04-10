package com.scienceminer.nerd.kb.model;

import com.scienceminer.nerd.kb.db.KBEnvironment;
import org.wikipedia.miner.db.struct.DbPage;

public class Template extends Page {
	/**
	 * Initialises a newly created Template so that it represents the template given by <em>id</em>.
	 * 
	 * @param env	an active KBEnvironment
	 * @param id	the unique identifier of the template
	 */
	public Template(KBEnvironment env, int id) {
		super(env, id);
	}
	
	protected Template(KBEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
	}

}
