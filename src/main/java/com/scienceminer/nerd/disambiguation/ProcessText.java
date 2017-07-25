package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.utilities.StringPos;
import com.scienceminer.nerd.utilities.TextUtilities;
import com.scienceminer.nerd.utilities.Utilities;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.data.Entity;
import org.grobid.core.data.Sense;
import org.grobid.core.engines.NERParsers;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.*;
import org.grobid.core.mock.*;
import org.grobid.core.main.*;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.utilities.LanguageUtilities;

import com.scienceminer.nerd.utilities.Stopwords;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * 
 * Everything we need to get the mentions and names entities from a text. From a text or a 
 * NerdQuery object, we generate a list of Entity objects corresponding to the potential 
 * mentions of entity to be considred by the further disambiguation stage. 
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
	
	public static final int NGRAM_LENGTH = 6;

	// default indo-european delimiters, should be moved to language specific analysers
	private static String delimiters = " \n\t" + TextUtilities.fullPunctuations;

	public static ProcessText getInstance() throws Exception {
        if (instance == null)
			getNewInstance();
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
		tokenizer = EngineGetter.getTokenizer(language, new FileInputStream(dictionaryFile));	
	}
	
	/**
	 *  Case context where a token appears 
	 */
	public enum CaseContext { 
		/* token is found in lower cased text */
		lower, 
		
		/* token is found in UPPER CASED TEXT */
		upper, 
		
		/* token is found in Text Where Every Word Starts With A Capital Letter */
		upperFirst, 
		
		/* token is found in text with a Healthy mixture of capitalization (probably normal text) */
		mixed
	}

	public static boolean isAllUpperCase(String text) {
        if (text.equals(text.toUpperCase()))
	        return true;
	    else
	    	return false;
    }

    public static boolean isAllLowerCase(String text) {
        if (text.equals(text.toLowerCase()))
	        return true;
        else
        	return false;
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
			//System.out.println("The length of the text to be processed is 0.");
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
	 * @param nerdQuery the NERD query to be processed
	 * @return the list of identified entities
	 */
	public List<Entity> process(NerdQuery nerdQuery) throws NerdException { 
		String text = nerdQuery.getText();

		//TODO: maybe this should be done at controller level
		if (isBlank(text)) {
			text = nerdQuery.getShortText();
		}

		List<LayoutToken> tokens = nerdQuery.getTokens();
		
		if (isBlank(text) && CollectionUtils.isEmpty(tokens)) {
			throw new NerdException("Cannot parse the content, because it is null.");
		} else if (isBlank(text)) {
			LOGGER.error("The length of the text to be parsed is 0.");
			return null;
		} else if (CollectionUtils.isEmpty(tokens)) {
			LOGGER.error("The number of tokens to be processed is 0.");
			return null;
		}

		if (isBlank(text))
			return processText(nerdQuery);
		else
			return processTokens(nerdQuery);
	}

	/**
	 *  Precondition: text in the query object is not empty and 
	 *  we assume here that the text has been dehyphenized before calling this method.
	 */
	private List<Entity> processText(NerdQuery nerdQuery) throws NerdException { 
		String text = nerdQuery.getText();
		if (text == null)
			text = nerdQuery.getShortText();

		List<Entity> results = null;
		
		Language language = nerdQuery.getLanguage();
		String lang = null;
		if (language != null)
			lang = language.getLang();
		if (!lang.equals("en") && !lang.equals("fr"))
			return results;
		Integer[] processSentence = nerdQuery.getProcessSentence();
		List<Sentence> sentences = nerdQuery.getSentences();

		// do we need to process the whole text only a sentence?
		if (ArrayUtils.isNotEmpty(processSentence) && CollectionUtils.isNotEmpty(sentences) ) {
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
					if ( CollectionUtils.isNotEmpty(localResults)) {
						for(Entity entity : localResults) {
							entity.setOffsetStart(sentence.getOffsetStart() + entity.getOffsetStart());
							entity.setOffsetEnd(sentence.getOffsetStart() + entity.getOffsetEnd());
						}
						if (results == null) {
							results = new ArrayList<>();
						}

						results.addAll(localResults);
					}
				}
				catch(Exception e) {
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
				results = nerParsers.extractNE(text, language);
			}
			catch(Exception e) {
				throw new NerdException("NERD error when processing text.", e);
			}
		}
		return results;
	}

	/**
	 *  Precondition: list of LayoutToken in the query object is not empty
	 */
	private List<Entity> processTokens(NerdQuery nerdQuery) throws NerdException { 
		List<LayoutToken> tokens = nerdQuery.getTokens();

		List<Entity> results = null;
		
		Language language = nerdQuery.getLanguage();
		String lang = null;
		if (language != null)
			lang = language.getLang();

		if (!lang.equals("en") && !lang.equals("fr"))
			return results;
		
		Integer[] processSentence = nerdQuery.getProcessSentence();
		List<Sentence> sentences = nerdQuery.getSentences();

		// do we need to process the whole text only a sentence?
		/*if ( (processSentence != null) && (processSentence.length > 0) &&
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
		else {*/
			// we process the whole text
			try {
				if (nerParsers == null) {
					//Utilities.initGrobid();
					nerParsers = new NERParsers();	
				}
				results = nerParsers.extractNE(tokens, language);
			}
			catch(Exception e) {
				e.printStackTrace();
				throw new NerdException("NERD error when processing text.", e);
			}
		//}
		return results;
	}
	
	/**
	 * Processing of some raw text by extracting all non-trivial ngrams. We do not
	 * control here the textual mentions by a NER. Generate a list of entity mentions. 
	 * We assume here that the text has been dehyphenized before calling this method.
	 *
	 * @param text 
	 *		the raw text to be parsed
	 * @return 
	 * 		the list of identified entities.
	 */
	public List<Entity> processBrutal(String text, Language lang) throws NerdException { 
		if (text == null) {
			throw new NerdException("Cannot parse the text, because it is null.");
		}
		else if (text.length() == 0) {
			//System.out.println("The length of the text to be processed is 0.");
			LOGGER.error("The length of the text to be parsed is 0.");
			return null;
		}
		
		List<Entity> results = new ArrayList<Entity>();
		try {
			List<StringPos> pool = ngrams(text, NGRAM_LENGTH, lang);

			// candidates which start and end with a stop word are removed. 
			// beware not to be too aggressive.
			List<Integer> toRemove = new ArrayList<Integer>();
			
			for(int i=0; i<pool.size(); i++) {
				StringPos termPosition = pool.get(i);
				String termValue = termPosition.string;
				String termValueLowercase = termValue.toLowerCase();

				/*if (termValueLowercase.indexOf("\n") != -1) {
					toRemove.add(new Integer(i));
					continue;
				}*/

				// remove term starting or ending with a stopword, and term starting with a separator (conservative
				// it should never be the case)
				if (stopwords != null) {
					if ( (delimiters.indexOf(termValueLowercase.charAt(0)) != -1) ||
						 stopwords.startsWithStopword(termValueLowercase, lang.getLang()) ||
						 stopwords.endsWithStopword(termValueLowercase, lang.getLang())
					) {
						toRemove.add(new Integer(i));
						continue;
					} 
				}

				// remove term ending with a separator (conservative it should never be the case)
				while (delimiters.indexOf(termValueLowercase.charAt(termValueLowercase.length()-1)) != -1) {
					termPosition.string = termPosition.string.substring(0,termPosition.string.length()-1);
					termValueLowercase = termValueLowercase.substring(0,termValueLowercase.length()-1);
					if (termValueLowercase.length() == 0) {
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

			// Calculating the positions
			for(StringPos candidate : subPool) {
				Entity entity = new Entity(candidate.string);
				
				org.grobid.core.utilities.OffsetPosition pos = 
					new org.grobid.core.utilities.OffsetPosition();
				pos.start = candidate.pos;
				pos.end = pos.start + candidate.string.length();
				entity.setOffsets(pos);
				// we have an additional check of validy based on language
				if (validEntity(entity, lang.getLang())) {
					results.add(entity);
				}
			}
		} catch(Exception e) {
			throw new NerdException("NERD error when processing text.", e);
		}
		
		return results;
	}
	
	/**
	 * Processing of some raw text by extracting all non-trivial ngrams. We do not
	 * control here the textual mentions by a NER. Generate a list of entity mentions. 
	 *
	 * @param tokens 
	 *		the sequence of tokens to be parsed
	 * @return 
	 * 		the list of identified entities.
	 */
	public List<Entity> processBrutal(List<LayoutToken> tokens, Language lang) throws NerdException { 
		if ( (tokens == null) || (tokens.size() == 0) ) {
			//System.out.println("Content to be processed is empty.");
			LOGGER.error("Content to be processed is empty.");
			return null;
		}
		String text = LayoutTokensUtil.toText(tokens);
		List<Entity> results = new ArrayList<Entity>();
		try {
			List<StringPos> pool = ngrams(text, NGRAM_LENGTH, lang);
		
			// candidates which start and end with a stop word are removed. 
			// beware not to be too agressive. 
			List<Integer> toRemove = new ArrayList<Integer>();
			
			for(int i=0; i<pool.size(); i++) {
				StringPos term1 = pool.get(i);
				String term = term1.string;
				term = term.replace("\n", " ");
				String termLow = term.toLowerCase();

				/*if (termLow.indexOf("\n") != -1) {
					toRemove.add(new Integer(i));
					continue;
				} */

				if (stopwords != null) {
					if ( (delimiters.indexOf(termLow.charAt(0)) != -1) ||
						 stopwords.startsWithStopword(termLow, lang.getLang()) ||
						 stopwords.endsWithStopword(termLow, lang.getLang()) 
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

			int tokenPos = 0;
			int lastTokenIndex = 0;
			int lastTokenPos = 0;

			Collections.sort(subPool);
			for(StringPos candidate : subPool) {
				Entity entity = new Entity(candidate.string);				
				org.grobid.core.utilities.OffsetPosition pos = 
					new org.grobid.core.utilities.OffsetPosition();
				pos.start = candidate.pos;
				pos.end = pos.start + candidate.string.length();
				entity.setOffsets(pos);
				// synchronize layout token with the selected ngrams
				List<LayoutToken> entityTokens = null;
				tokenPos = lastTokenPos;
				for(int j=lastTokenIndex; j<tokens.size(); j++) {
					if (tokenPos < pos.start) {
						tokenPos += tokens.get(j).getText().length();
						continue;
					}
					if (tokenPos + tokens.get(j).getText().length() > pos.end) {
						break;
					}

					if (tokenPos == pos.start) {
						entityTokens = new ArrayList<LayoutToken>();
						entityTokens.add(tokens.get(j));
						lastTokenIndex = j;
						lastTokenPos = tokenPos;
					} else if ( (tokenPos >= pos.start) && (tokenPos <= pos.end) ) {
						if (entityTokens == null) {
							entityTokens = new ArrayList<LayoutToken>();
							lastTokenIndex = j;
							lastTokenPos = tokenPos;
						}
						entityTokens.add(tokens.get(j));
					} 

					tokenPos += tokens.get(j).getText().length();
				}
				if (entityTokens != null)
					entity.setBoundingBoxes(BoundingBoxCalculator.calculate(entityTokens));
				else 
					LOGGER.warn("LayoutToken sequence not found for mention: " + candidate.string);
				// we have an additional check of validy based on language
				if (validEntity(entity, lang.getLang()))
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
		String shortText = nerdQuery.getShortText();
		List<LayoutToken> tokens = nerdQuery.getTokens();

		//if ((text == null) && (shortText == null)) {
			//LOGGER.info("Cannot parse the given text, because it is null.");
		//}
		
		if ( (text == null) || (text.length() == 0) ) 
			text = shortText;

		if ( (text == null) || (text.length() == 0) ) {
			//LOGGER.info("The length of the text to be parsed is 0. Look at the layout tokens.");
			if ( (tokens != null) && (tokens.size() > 0) )
				text = LayoutTokensUtil.toText(tokens);
			else {
				LOGGER.error("All possible content to process are empty - process stops.");
				return null;
			}
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
			}
		}
		
		if (lang == null) {
			// default - it might be better to raise an exception?
			lang = "en";
			language = new Language(lang, 1.0);
		}
		
		Integer[] processSentence = nerdQuery.getProcessSentence();
		List<Sentence> sentences = nerdQuery.getSentences();
		
		List<Entity> results = new ArrayList<Entity>();
		
		// do we need to process the whole text only a sentence?
		if ( ArrayUtils.isNotEmpty(processSentence) && CollectionUtils.isNotEmpty(sentences) ) {
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
					List<Entity> localResults = processBrutal(text, language);

					// we "shift" the entities offset in case only specific sentences are processed
					if ( CollectionUtils.isNotEmpty(localResults) ) {
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
			if (CollectionUtils.isNotEmpty (tokens) )
				return processBrutal(tokens, language);
			else
				return processBrutal(text, language);
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

	public static List<StringPos> ngrams(String str, int ngram, Language lang) {
		int actualNgram = (ngram * 2) - 1; // for taking into account separators
		List<StringPos> ngrams = new ArrayList<StringPos>();
		if (str == null) {
			return ngrams;
		}
		GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();
		List<String> words = analyzer.tokenize(str, lang);
		for (int n = 1; n <= actualNgram; n++) {
			int currentPos = 1;
	        for (int i = 0; i < words.size() - n + 1; i++) {
	        	if (words.get(i).length() == 0)
	        		continue;
				currentPos = str.indexOf(words.get(i), currentPos-1);
				StringPos stringp = new StringPos();
				stringp.string = concat(words, i, i+n);
				stringp.pos = currentPos;
	            ngrams.add(stringp);
			}
		}
        return ngrams;
    }

    public static String concat(List<String> words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(words.get(i));
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

    /**
    *  Detect possible explicit acronym introductions based on patterns
    */
   	public static Map<Entity, Entity> acronymCandidates(NerdQuery nerdQuery) {
    	String text = nerdQuery.getText();
		List<LayoutToken> tokens = nerdQuery.getTokens();

		if ( (text == null) || (text.length() == 0) ) {
			//LOGGER.info("The length of the text to be parsed is 0. Look at the layout tokens.");
			if ( (tokens != null) && (tokens.size() > 0) )
				text = LayoutTokensUtil.toText(tokens);
			else {
				LOGGER.error("All possible content to process are empty - process stops.");
				return null;
			}
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
			}
		}
		
		if (lang == null) {
			// default - it might be better to raise an exception?
			lang = "en";
			language = new Language(lang, 1.0);
		}
		
		if (CollectionUtils.isNotEmpty (tokens) ) 
			return acronymCandidates(tokens);
		else
			return acronymCandidates(text, language);
	}


	public static Map<Entity, Entity> acronymCandidates(String text, Language language) {
		List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, language);
		return acronymCandidates(tokens);
    }

    public static Map<Entity, Entity> acronymCandidates(List<LayoutToken> tokens) {
    	Map<Entity, Entity> acronyms = null;

    	// detect possible acronym
    	boolean openParenthesis = false;
    	int posParenthesis = -1;
    	int i =0;
    	LayoutToken acronym = null;
    	for(LayoutToken token : tokens) {
    		if (token.getText() == null) {
    			i++;
    			continue;
    		}
			if (token.getText().equals("(")) {
				openParenthesis = true;
				posParenthesis = i;
				acronym = null;
			} else if (token.getText().equals(")")) {
				openParenthesis = false;
			} else if (openParenthesis) {
				if (TextUtilities.isAllUpperCase(token.getText())) {
					acronym = token;
				} else {
					acronym = null;
				}
			}

			if ( (acronym != null) && (!openParenthesis) ) {
				// check if this possible acronym matches an immediatly preceeding term
				int j = posParenthesis;
				int k = acronym.getText().length();
				boolean stop = false;
				while ( (k > 0) && (!stop) ) {
					k--;
					char c = acronym.getText().toLowerCase().charAt(k);
					while((j>0) && (!stop)) {
						j--;
						if (tokens.get(j) != null) {
							String tok = tokens.get(j).getText();
							if (tok.trim().length() == 0)
								continue;
							if (tok.toLowerCase().charAt(0) == c) {
								if (k == 0) {
									if (acronyms == null) 
										acronyms = new HashMap<Entity,Entity>();
									StringBuilder builder = new StringBuilder();
									for(int l = j; l < posParenthesis; l++) {
										builder.append(tokens.get(l));
									}

									Entity entityAcronym = new Entity();
									entityAcronym.setRawName(acronym.getText());
									entityAcronym.setNormalisedName(builder.toString().trim());
									entityAcronym.setOffsetStart(acronym.getOffset());
									entityAcronym.setOffsetEnd(acronym.getOffset() + acronym.getText().length());
									entityAcronym.setType(null);

									Entity entityBase = new Entity(builder.toString().trim());
									entityBase.setOffsetStart(tokens.get(j).getOffset());
									entityBase.setOffsetEnd(tokens.get(j).getOffset() + entityBase.getRawName().length());

									acronyms.put(entityAcronym, entityBase); 
									stop = true;
								} else
									break;
							} else {
								stop = true;
							}
						}
					}
				}
				acronym = null;
				posParenthesis = -1;
			} 
    		i++;
    	}

    	return acronyms;
    }
    
    /**
     * Add entities corresponding to acronym defintions to a query
     */
    public static List<Entity> propagateAcronyms(NerdQuery nerdQuery, Map<Entity, Entity> acronyms) {
    	List<Entity> entities = null;
    	String text = nerdQuery.getText();
		List<LayoutToken> tokens = nerdQuery.getTokens();

		if ( (text == null) || (text.length() == 0) ) {
			//LOGGER.info("The length of the text to be processed is 0. Look at the layout tokens.");
			if ( (tokens != null) && (tokens.size() > 0) )
				text = LayoutTokensUtil.toText(tokens);
			else {
				LOGGER.error("All possible content to process are empty - process stops.");
				return null;
			}
		}

		Map<Entity, Entity> toBeAdded = new HashMap<Entity, Entity>();
		for (Map.Entry<Entity, Entity> entry : acronyms.entrySet()) {
            Entity acronym = entry.getKey();
            Entity base = entry.getValue();
			Pattern linkPattern = Pattern.compile(acronym.getRawName()); 
			Matcher linkMatcher = linkPattern.matcher(text);
			while (linkMatcher.find()) {
				// we need to ignore the current acronym to avoid having it twice
				if ( (linkMatcher.start() == acronym.getOffsetStart())	&&
					(linkMatcher.end() == acronym.getOffsetEnd()) )
					continue;
				String entityText = text.substring(linkMatcher.start(), linkMatcher.end());
				Entity entity = new Entity(entityText);
				entity.setNormalisedName(base.getRawName());
				entity.setOffsetStart(linkMatcher.start());
				entity.setOffsetEnd(linkMatcher.end());
				entity.setType(null);
				if (entities == null)
					entities = new ArrayList<Entity>();
				entities.add(entity);
				toBeAdded.put(entity, base);
			}
		}

		for (Map.Entry<Entity, Entity> entry : toBeAdded.entrySet()) {
			Entity entity = entry.getKey();
            Entity base = entry.getValue();
            if (acronyms.get(entity) == null)
	            acronyms.put(entity, base);
		}
		return entities;
    }
}