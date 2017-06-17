package com.scienceminer.nerd.kb.model;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.hadoop.DbPage;

/**
 * Represents disambiguation pages in Wikipedia, which lists possible senses for an ambiguous term
 *
 * -> to be removed 
 */
public class Disambiguation extends Article {

	public Disambiguation(KBLowerEnvironment env, int id) {
		super(env, id);
	}
	
	protected Disambiguation(KBLowerEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
	}
	
}
