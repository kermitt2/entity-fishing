package com.scienceminer.nerd.embeddings;

import java.util.*;
import java.io.*;

import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.disambiguation.ProcessText;
import com.scienceminer.nerd.disambiguation.NerdCandidate;
import com.scienceminer.nerd.utilities.NerdConfig;
import com.scienceminer.nerd.exceptions.*;

import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.core.layout.LayoutToken;

import it.cnr.isti.hpc.LREntityScorer;
import it.cnr.isti.hpc.CentroidEntityScorer;
import it.cnr.isti.hpc.Word2VecCompress;
import it.unimi.dsi.fastutil.io.BinIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instance class for computing similarity between entities and context based on word 
 * and entity embeddings, the context being terms and/or other entities.
 * Embeddings and thus similarity scoring depend on the language. The class uses as resources
 * pre-trained word and entity embeddings produced by the grisp library, which have been 
 * quantized and compressed.
 */
public class SimilarityScorer {
	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(SimilarityScorer.class);

	private static volatile SimilarityScorer instance = null;
	
	// mapping language to vectors
	private Map<String, CompressedW2V> entityEmbeddings = null;
	private Map<String, CompressedW2V> wordEmbeddings = null;

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
		entityEmbeddings = new HashMap<String, CompressedW2V>();
		wordEmbeddings = new HashMap<String, CompressedW2V>();

		lrscorers = new HashMap<String, LREntityScorer>();
		centroidScorers = new HashMap<String, CentroidEntityScorer>();

		for(String lang : UpperKnowledgeBase.getInstance().targetLanguages) {
			try {
				File wvFile = new File("data/wikipedia/embeddings/"+lang+"/word.embeddings."+lang+".quantized.compressed");
				if (!wvFile.exists()) {
					LOGGER.error("The word embeddings file for " + lang + " does not exist");
					continue;
				}

				CompressedW2V wordVectors = new CompressedW2V(wvFile.getPath());
				wordEmbeddings.put(lang, wordVectors);

				File entityFile = new File("data/wikipedia/embeddings/"+lang+"/entity.embeddings."+lang+".quantized.compressed");
				if (!entityFile.exists()) {
					LOGGER.error("The entity embeddings file for " + lang + " does not exist");
					continue;
				}

				CompressedW2V entityVectors = new CompressedW2V(entityFile.getPath());
				entityEmbeddings.put(lang, entityVectors);
				lrscorers.put(lang, new LREntityScorer(wordVectors.getWord2VecCompress(), entityVectors.getWord2VecCompress()));
				centroidScorers.put(lang, new CentroidEntityScorer(wordVectors.getWord2VecCompress(), entityVectors.getWord2VecCompress()));
			} catch(Exception e) {
				throw new NerdException("Fails to initialize embeddings for " + lang, e);
			}
		}
	}

	public float getLRScore(NerdCandidate candidate, List<LayoutToken> tokens, String lang) {
		LREntityScorer scorer = lrscorers.get(lang);
		if (scorer != null) {
			List<String> terms = toStringEmbeddings(tokens);
			return scorer.score(candidate.getWikidataId(), terms);
		} else {
			return 0.0F;
		}
	}

	public float getCentroidScore(NerdCandidate candidate, List<LayoutToken> tokens, String lang) {
		CentroidEntityScorer scorer = centroidScorers.get(lang);
		if (scorer != null) {
			List<String> terms = toStringEmbeddings(tokens);
			return scorer.score(candidate.getWikidataId(), terms);
		} else {
			return 0.0F;
		}
	}

	/**
	 * Normalise LayoutTokens sequence as an array of words correspond to word embeddings
	 */
	private List<String> toStringEmbeddings(List<LayoutToken> tokens) {
		List<String> toks = new ArrayList<String>();
		for(LayoutToken token : tokens) {
			if (ProcessText.delimiters.indexOf(token.getText()) != -1)
				continue;
			String word = token.getText();
			// unicode normalization
			word = UnicodeUtil.normaliseText(word);

			// remove possible remaining punctuations
			word = word.replaceAll("\\p{P}", "");

			// flatten numerical chars
			word = word.replaceAll("\\d", "0");

			// lower case everything (to be evaluated!)
			word = word.toLowerCase();
			word = word.replace("\t", "");

			toks.add(word);
		}
		return toks;
	}
}