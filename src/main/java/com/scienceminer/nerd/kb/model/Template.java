package com.scienceminer.nerd.kb.model;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.hadoop.DbPage;

/**
 * To be removed
 */
public class Template extends Page {

	public Template(KBLowerEnvironment env, int id) {
		super(env, id);
	}
	
	protected Template(KBLowerEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
	}

}
