package com.scienceminer.nerd.training;

import com.scienceminer.nerd.disambiguation.NerdRankerWikidata;
import com.scienceminer.nerd.disambiguation.NerdSelectorWikidata;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;


/**
 * Train and evaluate a NerdRanker and a NerdSelector using Wikidata model as training data.
 */

public class WikidataTrainer {
    private static final Logger logger = LoggerFactory.getLogger(WikidataTrainer.class);
    private UpperKnowledgeBase wikidata = null;

    //directory in which files will be stored
    private File dataDir = null;

    //classes for performing annotation
    private NerdRankerWikidata ranker = null;
    private NerdSelectorWikidata selector = null;

    List<ArticleTrainingSample> articleSamples = null;



}
