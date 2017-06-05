package com.scienceminer.nerd.kb.model;

import com.scienceminer.nerd.kb.db.*;
import org.wikipedia.miner.db.struct.DbPage;

public class Template extends Page {
	/**
	 * Initialises a newly created Template so that it represents the template given by <em>id</em>.
	 * 
	 * @param env	an active KBLowerEnvironment
	 * @param id	the unique identifier of the template
	 */
	public Template(KBLowerEnvironment env, int id) {
		super(env, id);
	}
	
	protected Template(KBLowerEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
	}

}
