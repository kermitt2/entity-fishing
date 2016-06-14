package org.wikipedia.miner.model;

import org.wikipedia.miner.db.WEnvironment;
import org.wikipedia.miner.db.struct.DbPage;

public class Template extends Page{

	/**
	 * Initialises a newly created Template so that it represents the template given by <em>id</em>.
	 * 
	 * @param env	an active WEnvironment
	 * @param id	the unique identifier of the template
	 */
	public Template(WEnvironment env, int id) {
		super(env, id) ;
	}
	
	protected Template(WEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
	}

	
}
