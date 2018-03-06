package com.scienceminer.nerd.embeddings;

import java.util.*;
import java.io.*;

import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.disambiguation.NerdCandidate;
import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.utilities.NerdConfig;
import com.scienceminer.nerd.utilities.Stopwords;

import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.core.layout.LayoutToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instance class for computing similarity between entities and context based on word
 * and entity embeddings, the context being terms and/or other entities.
 * Embeddings and thus similarity scoring depend on the language. The class uses as resources
 * pre-trained word and entity embeddings produced by the class EntityDescription, which have
 * been quantized.
 */
public class SimilarityScorer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimilarityScorer.class);

	private static volatile SimilarityScorer instance = null;

	// mapping language to vectors
	//private Map<String, CompressedW2V> entityEmbeddings = null;
	//private Map<String, CompressedW2V> wordEmbeddings = null;

	// mapping language to similarity scorers
	private Map<String, LREntityScorer> lrscorers = null;
    private Map<String, CentroidEntityScorer> centroidScorers = null;

	// note: we could add some caches :)

	public static SimilarityScorer getInstance() {
	    if (instance == null) {
			getNewInstance();
	    }
	    return instance;
	}

	/**
	 * Creates a new instance.
	 */
	private static synchronized void getNewInstance() {
		LOGGER.debug("Get new instance of SimilarityScorer");
		instance = new SimilarityScorer();
	}

	/**
	 * Hidden constructor
	 */
	private SimilarityScorer() {
		//entityEmbeddings = new HashMap<>();
		//wordEmbeddings = new HashMap<>();

		lrscorers = new HashMap<>();
		centroidScorers = new HashMap<>();

		UpperKnowledgeBase knowledgeBase = UpperKnowledgeBase.getInstance();

		for(String lang : UpperKnowledgeBase.TARGET_LANGUAGES) {
			try {
				LowerKnowledgeBase lowerKnowledgeBase = knowledgeBase.getWikipediaConf(lang);
				lrscorers.put(lang, new LREntityScorer(lowerKnowledgeBase));
				centroidScorers.put(lang, new CentroidEntityScorer(lowerKnowledgeBase));
			} catch(Exception e) {
				throw new NerdException("Fails to initialize embeddings for " + lang, e);
			}
		}
	}

	public float getLRScore(NerdCandidate candidate, List<LayoutToken> tokens, String lang) {
		if (candidate.getWikidataId() == null)
			return 0.0F;
		//System.out.println("LR score (" +lang+ "): " + candidate.getWikidataId() + tokens.toString());
		LREntityScorer scorer = lrscorers.get(lang);
		if (scorer != null) {
			List<String> terms = toStringEmbeddings(tokens, lang);
			return scorer.score(candidate.getWikidataId(), terms);
		} else {
			LOGGER.warn(lang + " LR scorer is null!");
			return 0.0F;
		}
	}

	public float getCentroidScore(NerdCandidate candidate, List<LayoutToken> tokens, String lang) {
		if (candidate.getWikidataId() == null)
			return 0.0F;
		CentroidEntityScorer scorer = centroidScorers.get(lang);
		if (scorer != null) {
			List<String> terms = toStringEmbeddings(tokens, lang);
            //System.out.println("\n"+candidate.toString());
            //System.out.println(terms.toString());
			float score = scorer.score(candidate.getWikidataId(), terms);
			//System.out.println("score: " + score);
			if (score < 0.0F)
				score = 0.0F;
			return score;
		} else {
			LOGGER.warn(lang + " centroid scorer is null!");
			return 0.0F;
		}
	}

	/**
	 * Normalise LayoutTokens sequence as an array of words correspond to word embeddings
	 */
	private List<String> toStringEmbeddings(List<LayoutToken> tokens, String lang) {
		List<String> toks = new ArrayList<String>();
		for(LayoutToken token : tokens) {
			String word = token.getText();

			if (word == null || word.trim().length() == 0)
				continue;
			if (ProcessText.delimiters.indexOf(word) != -1)
				continue;

			// unicode normalization
			word = UnicodeUtil.normaliseText(word);

			// remove possible remaining punctuations
			word = word.replaceAll("\\p{P}", "");

			// flatten numerical chars
			word = word.replaceAll("\\d", "0");

			// lower case everything (to be evaluated!)
			word = word.toLowerCase();
			word = word.replace("\t", "");

			if (word.trim().length() == 0)
				continue;

			try {
				if (!Stopwords.getInstance().isStopword(word, lang))
					toks.add(word);
			} catch(Exception e) {
				LOGGER.warn("Problem getting Stopwords instance", e);
				toks.add(word);
			}
		}
		return toks;
	}
}