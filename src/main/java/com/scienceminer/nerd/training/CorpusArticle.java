package com.scienceminer.nerd.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;

/**
 * An article part of an annotated corpus.
 */
public class CorpusArticle extends Article {
	public static final Logger LOGGER = LoggerFactory.getLogger(CorpusArticle.class);	

	private String corpus = null;
	private String path = null;

	public CorpusArticle(String corpus, LowerKnowledgeBase wikipedia) {
		super(wikipedia.getEnvironment(), -1);
		this.corpus = corpus;
	}

	public String getCorpus() {
		return this.corpus;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}