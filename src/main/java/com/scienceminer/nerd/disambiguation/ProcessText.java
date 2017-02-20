package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.utilities.StringPos;
import com.scienceminer.nerd.utilities.TextUtilities;
import com.scienceminer.nerd.utilities.Utilities;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.LanguageUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 

import java.io.*;
import java.util.*;
  
import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;

import org.apache.commons.lang3.StringUtils;

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.data.Entity;
import org.grobid.core.data.Sense;
import org.grobid.core.engines.NERParsers;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.*;
import org.grobid.core.mock.*;
import org.grobid.core.main.*;
import org.grobid.core.utilities.GrobidProperties;
//import org.grobid.core.analyzers.GrobidAnalyzer;

import com.scienceminer.nerd.utilities.Stopwords;

// this is for version 1.3.0 of ClearNLP
//import com.googlecode.clearnlp.nlp.NLPLib;

/**
 * 
 * Everything we need to get the mentions and names entities from a text. From a text or a 
 * NerdQuery object, we generate a list of Entity objects corresponding to the potential 
 * mentions of entity to be considred by the further disambiguation stage. 
 *
 * @author Patrice Lopez
 *
 */
public class ProcessText {
	public final String language = AbstractReader.LANG_EN;
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ProcessText.class);
	
  	private static volatile ProcessText instance;
	
	// ClearParser components for sentence segmentation
	private AbstractTokenizer tokenizer = null;
	
	// GROBID tokenizer
	//private GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance(); 

	private NERParsers nerParsers = null;

	private Stopwords stopwords = Stopwords.getInstance();
	
	public static final int NGRAM_LENGTH = 4;

	// default indo-european delimiters, should be moved to language specific analysers
	private static String delimiters = " \n\t" + TextUtilities.fullPunctuations;

	public static ProcessText getInstance() throws Exception {
        if (instance == null) {
            //double check idiom
            // synchronized (instanceController) {
                if (instance == null)
					getNewInstance();
            // }
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
	private static synchronized void getNewInstance() throws Exception {
		LOGGER.debug("Get new instance of ProcessText");
		
		instance = new ProcessText();
	}
	
	/**
     * Hidden constructor 
     */
    private ProcessText() throws Exception {	
		String dictionaryFile = "data/clearNLP/dictionary-1.3.1.zip";
		tokenizer = 
			EngineGetter.getTokenizer(language, new FileInputStream(dictionaryFile));	
	}
	
	/**
	 * NER processing of some raw text. Generate list of named entities.
	 *
	 * @param text 
	 *		the raw text to be parsed
	 * @return 
	 * 		the list of identified entities.
	 */
	public List<Entity> process(String text, Language lang) throws NerdException { 
		if (text == null) {
			throw new NerdException("Cannot parse the sentence, because it is null.");
		}
		else if (text.length() == 0) {
			System.out.println("The length of the text to be processed is 0.");
			LOGGER.error("The length of the text to be parsed is 0.");
			return null;
		}
		
		List<Entity> results = null;
		try {
			if (nerParsers == null) {
				//Utilities.initGrobid();
				nerParsers = new NERParsers();	
			}
			results = nerParsers.extractNE(text, lang);
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new NerdException("NERD error when processing text.", e);
		}
		
		return results;
	}
	
	/**
	 * NER processing of a structured NERD query (text with already annotations, sentence 
	 * segmentation, etc.). Generate a list of recongnized named entities. 
	 *
	 * @param nerdQuery 
	 *		the NERD query to be processed
	 * @return 
	 * 		the list of identified entities
	 */
	public List<Entity> process(NerdQuery nerdQuery) throws NerdException { 
		String text = nerdQuery.getText();
		Language language = nerdQuery.getLanguage();
		String lang = null;
		if (language != null)
			lang = language.getLang();
		Integer[] processSentence = nerdQuery.getProcessSentence();
		List<Sentence> sentences = nerdQuery.getSentences();
		if (text == null) {
			throw new NerdException("Cannot parse the sentence, because it is null.");
		}
		else if (text.length() == 0) {
			System.out.println("The length of the text to be processed is 0.");
			LOGGER.error("The length of the text to be parsed is 0.");
			return null;
		}
		
		List<Entity> results = null;
		
		// do we need to process the whole text only a sentence?
		if ( (processSentence != null) && (processSentence.length > 0) &&
			(sentences != null) && (sentences.size() > 0) ) {
			// we process only the indicated sentences
			String text2tag = null;
			for(int i=0; i<processSentence.length; i++) {
				Integer index = processSentence[i];
				
				// for robustness, we have to consider index out of the current sentences range
				// here we ignore it, but we might better raise an exception and return an error
				// message to the client
				if (index.intValue() >= sentences.size())
					continue;
				Sentence sentence = sentences.get(index.intValue());
				text2tag = text.substring(sentence.getOffsetStart(), sentence.getOffsetEnd());
				try {
					if (nerParsers == null) {
						//Utilities.initGrobid();			
						nerParsers = new NERParsers();	
					}
					List<Entity> localResults = nerParsers.extractNE(text2tag, language);

					// we "shift" the entities offset in case only specific sentences are processed
					if ( (localResults != null) && (localResults.size() > 0) ) {
						for(Entity entity : localResults) {
							entity.setOffsetStart(sentence.getOffsetStart() + entity.getOffsetStart());
							entity.setOffsetEnd(sentence.getOffsetStart() + entity.getOffsetEnd());
						}
						for(Entity entity : localResults) {
							if (results == null)
								results = new ArrayList<Entity>();
							results.add(entity);
						}
					}
				}
				catch(Exception e) {
					e.printStackTrace();
					throw new NerdException("NERD error when processing text.", e);
				}
			}
		}
		else {
			// we process the whole text
			try {
				if (nerParsers == null) {
					//Utilities.initGrobid();
					nerParsers = new NERParsers();	
				}
System.out.println(language.toString());
System.out.println(text);
				results = nerParsers.extractNE(text, language);
System.out.println(results.size() + " NER entities found...");				
			}
			catch(Exception e) {
				e.printStackTrace();
				throw new NerdException("NERD error when processing text.", e);
			}
		}
		return results;
	}
	
	/**
	 * Processing of some raw text by extracting all non-trivial ngrams. We do not
	 * control here the textual mentions by a NER. Generate a list of entity mentions. 
	 *
	 * @param text 
	 *		the raw text to be parsed
	 * @return 
	 * 		the list of identified entities.
	 */
	public List<Entity> processBrutal(String text, String lang) throws NerdException { 
		if (text == null) {
			throw new NerdException("Cannot parse the sentence, because it is null.");
		}
		else if (text.length() == 0) {
			System.out.println("The length of the text to be processed is 0.");
			LOGGER.error("The length of the text to be parsed is 0.");
			return null;
		}
		
		List<Entity> results = new ArrayList<Entity>();
		try {
			List<StringPos> pool = ngrams(text, NGRAM_LENGTH);
		
			// candidates which start and end with a stop word are removed. 
			// beware not to be too agressive. 
			List<Integer> toRemove = new ArrayList<Integer>();
			
			for(int i=0; i<pool.size(); i++) {
				StringPos term1 = pool.get(i);
				String term = term1.string;
				String termLow = term.toLowerCase();

				if (termLow.indexOf("\n") != -1) {
					toRemove.add(new Integer(i));
					continue;
				} 

				if (stopwords != null) {
					if ( (delimiters.indexOf(termLow.charAt(0)) != -1) ||
						 stopwords.startsWithStopword(termLow, lang) ||
						 stopwords.endsWithStopword(termLow, lang) 
					) {
						toRemove.add(new Integer(i));
						continue;
					} 
				}

				while (delimiters.indexOf(termLow.charAt(termLow.length()-1)) != -1) {
					term1.string = term1.string.substring(0,term1.string.length()-1);
					termLow = termLow.substring(0,termLow.length()-1);
					if (termLow.length() == 0) {
						toRemove.add(new Integer(i));
						continue;
					}
				}
			}

			List<StringPos> subPool = new ArrayList<StringPos>();
			for(int i=0; i<pool.size(); i++) {
				if (toRemove.contains(new Integer(i))) {
					continue;
				}
				else {
					subPool.add(pool.get(i));
				}
			}
		
			for(StringPos candidate : subPool) {
				Entity entity = new Entity(candidate.string);
				
				org.grobid.core.utilities.OffsetPosition pos = 
					new org.grobid.core.utilities.OffsetPosition();
				pos.start = candidate.pos;
				pos.end = pos.start + candidate.string.length();
				entity.setOffsets(pos);
				// we have an additional check of validy based on language
				if (validEntity(entity, lang))
					results.add(entity);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new NerdException("NERD error when processing text.", e);
		}
		
		return results;
	}
	
	/**
	 * Processing of a vector of weighted terms. We do not control here the textual 
	 * mentions by a NER. 
	 *
	 * @param terms 
	 *		a list of weighted terms
	 * @return 
	 * 		the list of identified entities.
	 */
	/*public List<Entity> processWeightedTerms(List<WeightedTerm> terms) throws NerdException { 
		if (terms == null) {
			throw new NerdException("Cannot parse the weighted terms, because it is null.");
		}
		else if (terms.length() == 0) {
			System.out.println("The length of the term vector to be processed is 0.");
			LOGGER.error("The length of the term vector to be processed is 0.");
			return null;
		}
		
		List<Entity> results = new ArrayList<Entity>();
		try {
			List<StringPos> pool = ngrams(text, NGRAM_LENGTH);
		
			// candidates which start and end with a stop word are removed. 
			// beware not to be too agressive. 
			List<Integer> toRemove = new ArrayList<Integer>();
	
			for(int i=0; i<pool.size(); i++) {
				StringPos term1 = pool.get(i);
				String term = term1.string;
		
				List<String> stopwords = allStopwords.get(lang);
				if (stopwords != null) {
					for (String stopword : stopwords) {
						String termLow = term.toLowerCase();
						if ( (termLow.equals(stopword)) ||
							 (termLow.startsWith(stopword+ " ")) || 
							 (termLow.endsWith(" " + stopword)) ) {
							toRemove.add(new Integer(i));
						} 
					}
				}
			}

			List<StringPos> subPool = new ArrayList<StringPos>();
			for(int i=0; i<pool.size(); i++) {
				if (toRemove.contains(new Integer(i))) {
					continue;
				}
				else {
					subPool.add(pool.get(i));
				}
			}
		
			for(StringPos candidate : subPool) {
		
				Entity entity = new Entity(candidate.string);
				
				org.grobid.core.utilities.OffsetPosition pos = 
					new org.grobid.core.utilities.OffsetPosition();
				pos.start = candidate.pos;
				pos.end = pos.start + candidate.string.length();
				entity.setOffsets(pos);
				results.add(entity);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new NerdException("NERD error when processing text.", e);
		}
		
		return results;
	}*/
	
	/**
	 * Processing of some raw text by extracting all non-trivial ngrams. We do not
	 * control here the textual mentions by a NER. Generate a list of entity mentions. 
	 *
	 * @param nerdQuery 
	 *		the NERD query to be processed
	 * @return 
	 * 		the list of identified entities.
	 */
	public List<Entity> processBrutal(NerdQuery nerdQuery) throws NerdException { 
		String text = nerdQuery.getText();
		if (text == null) {
			throw new NerdException("Cannot parse the sentence, because it is null.");
		}
		else if (text.length() == 0) {
			System.out.println("The length of the text to be processed is 0.");
			LOGGER.error("The length of the text to be parsed is 0.");
			return null;
		}
		
		// source language 
		String lang = null;
		Language language = nerdQuery.getLanguage();
		if (language != null) 
			lang = language.getLang();
		
		if (lang == null) {
			// the language recognition has not been done upstream of the call to this method, so
			// let's do it
			LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
			try {
				language = languageUtilities.runLanguageId(text);
				nerdQuery.setLanguage(language);
				lang = language.getLang();
				LOGGER.debug(">> identified language: " + lang);
			}
			catch(Exception e) {
				LOGGER.debug("exception language identifier for: " + text);
				//e.printStackTrace();
			}
		}
		
		if (lang == null) {
			// default - it might be better to raise an exception?
			lang = "en";
		}
		
		Integer[] processSentence = nerdQuery.getProcessSentence();
		List<Sentence> sentences = nerdQuery.getSentences();
		
		List<Entity> results = new ArrayList<Entity>();
		
		// do we need to process the whole text only a sentence?
		if ( (processSentence != null) && (processSentence.length > 0) &&
			(sentences != null) && (sentences.size() > 0) ) {
			// we process only the indicated sentences
			String text2tag = null;
			for(int i=0; i<processSentence.length; i++) {
				Integer index = processSentence[i];
			
				// for robustness, we have to consider index out of the current sentences range
				// here we ignore it, but we might better raise an exception and return an error
				// message to the client
				if (index.intValue() >= sentences.size())
					continue;
				Sentence sentence = sentences.get(index.intValue());
				text2tag = text.substring(sentence.getOffsetStart(), sentence.getOffsetEnd());
				try {
					List<Entity> localResults = processBrutal(text, lang);

					// we "shift" the entities offset in case only specific sentences are processed
					if ( (localResults != null) && (localResults.size() > 0) ) {
						for(Entity entity : localResults) {
							entity.setOffsetStart(sentence.getOffsetStart() + entity.getOffsetStart());
							entity.setOffsetEnd(sentence.getOffsetStart() + entity.getOffsetEnd());
						}
						for(Entity entity : localResults) {
							if (validEntity(entity, lang)) {
								if (results == null)
									results = new ArrayList<Entity>();
								results.add(entity);
							}
						}
					}
				}
				catch(Exception e) {
					e.printStackTrace();
					throw new NerdException("NERD error when processing text.", e);
				}
			}
		}
		else {
			return processBrutal(text, lang);
		}
		return results;
	}
	
	public List<Sentence> sentenceSegmentation(String text) {
		AbstractSegmenter segmenter = EngineGetter.getSegmenter(language, tokenizer);
		// convert String into InputStream
		InputStream is = new ByteArrayInputStream(text.getBytes());
		// read it with BufferedReader
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		List<List<String>> sentences = segmenter.getSentences(br);
		List<Sentence> results = new ArrayList<Sentence>();
		
		if ( (sentences == null) || (sentences.size() == 0) ) {
			// there is some text but not in a state so that a sentence at least can be
			// identified by the sentence segmenter, so we parse it as a single sentence
			Sentence sentence = new Sentence();
			OffsetPosition pos = new OffsetPosition();
			pos.start = 0;
			pos.end = text.length();
			sentence.setOffsets(pos);
			results.add(sentence);
			return results; 
		}
		
		// we need to realign with the original sentences, so we have to match it from the text 
		// to be parsed based on the tokenization
		int offSetSentence = 0;  
	   	//List<List<String>> trueSentences = new ArrayList<List<String>>();
		for(List<String> theSentence : sentences) {  
			int next = offSetSentence;
			for(String token : theSentence) { 					
				next = text.indexOf(token, next);
				next = next+token.length();
			} 
			List<String> dummy = new ArrayList<String>();                    
			//dummy.add(text.substring(offSetSentence, next));   
			//trueSentences.add(dummy);   
			Sentence sentence = new Sentence();
			OffsetPosition pos = new OffsetPosition();
			pos.start = offSetSentence;
			pos.end = next;
			sentence.setOffsets(pos);
			results.add(sentence);
			offSetSentence = next;
		} 
		return results; 
	}

	public static List<StringPos> ngrams(String str, int ngram) {
		List<StringPos> ngrams = new ArrayList<StringPos>();
		if (str == null) {
			return ngrams;
		}
		String[] words = str.split(" ");
		for (int n = 1; n <= ngram; n++) {
			int currentPos = 1;
	        for (int i = 0; i < words.length - n + 1; i++) {
	        	if (words[i].length() == 0)
	        		continue;
				currentPos = str.indexOf(words[i], currentPos-1);
				StringPos stringp = new StringPos();
				stringp.string = concat(words, i, i+n);
				stringp.pos = currentPos;
	            ngrams.add(stringp);
			}
		}
        return ngrams;
    }

    public static String concat(String[] words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append((i > start ? " " : "") + words[i]);
		}
        return sb.toString();
    }
    
    /**
     * Validity criteria for a raw entity. The entity raw string must not be
     * null, with additional requirements depending on language
     */
    private static boolean validEntity(Entity entity, String lang) {
    	if ( (entity == null) || (entity.getRawName() == null) )
    		return false;
    	if (lang.equals("fr")) {
    		// given the French Wikipedia, we need to remove 
    		// * one letter tokens
    		// * numerical tokens
    		if ( (entity.getRawName().length() <= 1) || TextUtilities.test_digit(entity.getRawName()) )
    			return false;
    		else 
    			return true;
    	}

    	return true;
    }
}