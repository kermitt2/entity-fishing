package com.scienceminer.nerd.mention;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.disambiguation.*;
import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.utilities.StringPos;
import com.scienceminer.nerd.utilities.Utilities;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Label;

import org.apache.commons.collections4.CollectionUtils;
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
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

import com.scienceminer.nerd.utilities.Stopwords;

// linnaeus
import uk.ac.man.documentparser.dataholders.Document;
import uk.ac.man.documentparser.input.DocumentIterator;
import uk.ac.man.documentparser.input.TextFile;
import uk.ac.man.entitytagger.doc.TaggedDocument;
import uk.ac.man.entitytagger.matching.MatchOperations;
import martin.common.ArgParser;
import martin.common.Loggers;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 
 * Everything we need to get the mentions and names entities from a text. From a text or a 
 * NerdQuery object, we generate a list of Mention objects corresponding to the potential 
 * mentions of entity to be considred by the further disambiguation stage. 
 * 
 * For producing these mentions, different recognition modules/algorithms can be used, the
 * default one being wikipedia via the Wikipedia anchors and titles. Other additional
 * possibilities are named entity (NER) or spcialised recognizers like species names, 
 * quantities, biomedical substances, physical formula, etc.
 *
 * The list of possible mention recognition methods are given by a list of MentionMethod
 * attributes. When processing a text, mentions will be produced sequentially by the 
 * application of each of these modules, resulting in a list of Mention objects associated
 * with the MentionMethod that produced it and usual mention data (position offset, 
 * raw text, normalized text, ...).
 */
public class ProcessText {
	public final String language = AbstractReader.LANG_EN;
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ProcessText.class);
	
  	private static volatile ProcessText instance;
	
  	// mention recognition methods
	public enum MentionMethod {
		wikipedia	("wikipedia"),
		ner			("ner"),
		wikidata	("wikidata"),
		quantities	("quantities"),
		grobid 		("grobid"),
		species		("species"),
		user 		("user");
		
		private String name;

		MentionMethod(String name) {
          	this.name = name;
		}

		public String getName() {
			return name;
		}
	};

	// ClearParser components for sentence segmentation
	private AbstractTokenizer tokenizer = null;
	
	// GROBID tokenizer
	//private GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance(); 

	private NERParsers nerParsers = null;

	private Stopwords stopwords = Stopwords.getInstance();
	
	public static final int NGRAM_LENGTH = 6;

	// default indo-european delimiters, should be moved to language specific analysers
	public static String delimiters = " \n\t" + TextUtilities.fullPunctuations;

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
	 * This is the entry point for a NerdQuery to have its textual content processed. 
	 * The mthod will generate a list of recognized named entities produced by a list
	 * of mention recognition modules specified in the list field 'mention' of the NerdQuery 
	 * object. Each mention recognition method will be applied sequencially in the order 
	 * given in the list field 'mention'.
	 *
	 * @param nerdQuery the NERD query to be processed
	 * @return the list of identified mentions
	 */
	public List<Mention> process(NerdQuery nerdQuery) throws NerdException { 
		String text = nerdQuery.getText();

		if (text == null) {
			text = nerdQuery.getShortText();
		}

		List<LayoutToken> tokens = nerdQuery.getTokens();
		
		if (isBlank(text) && CollectionUtils.isEmpty(tokens)) {
			LOGGER.warn("No content to process.");
			return null;
		} 

		if (isNotBlank(text))
			return processText(nerdQuery);
		else 
			return processTokens(nerdQuery);
	}


	/**
	 *  Precondition: text in the query object is not empty and 
	 *  we assume here that the text has been dehyphenized before calling this method.
	 */
	private List<Mention> processText(NerdQuery nerdQuery) throws NerdException { 
		String text = nerdQuery.getText();
		if (text == null)
			text = nerdQuery.getShortText();

		text = UnicodeUtil.normaliseText(text);

		List<Mention> results = new ArrayList<>();
		
		Language language = nerdQuery.getLanguage();
		String lang = null;
		if (language != null)
			lang = language.getLang();
		
		Integer[] processSentence = nerdQuery.getProcessSentence();
		List<Sentence> sentences = nerdQuery.getSentences();

		// get the list of requested mention types
		List<ProcessText.MentionMethod> mentionTypes = nerdQuery.getMentions();
		
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
					for (ProcessText.MentionMethod mentionType : mentionTypes) {
						List<Mention> localResults = null;

						if (mentionType == ProcessText.MentionMethod.ner) {
							localResults = processNER(text2tag, language);
						} else if (mentionType == ProcessText.MentionMethod.wikipedia) {
							localResults = processWikipedia(text2tag, language);
						} else if (mentionType == ProcessText.MentionMethod.species) {
							localResults = processSpecies(text2tag, language);
						}

						// we "shift" the entities offset in case only specific sentences are processed
						if (CollectionUtils.isNotEmpty(localResults)) {
							for(Mention entity : localResults) {
								Mention mention = new Mention(entity);
								mention.setOffsetStart(sentence.getOffsetStart() + entity.getOffsetStart());
								mention.setOffsetEnd(sentence.getOffsetStart() + entity.getOffsetEnd());
								//mention.setSource(entity.getSource());
								results.add(mention);
							}
						}
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
				for (ProcessText.MentionMethod mentionType : mentionTypes) {
					List<Mention> localResults = null;

					if (mentionType == ProcessText.MentionMethod.ner) {
						localResults = processNER(text, language);
					} else if (mentionType == ProcessText.MentionMethod.wikipedia) {
						localResults = processWikipedia(text, language);
					} else if (mentionType == ProcessText.MentionMethod.species) {
						localResults = processSpecies(text, language);
					}
					if (CollectionUtils.isNotEmpty(localResults)) {
						results.addAll(localResults);
					}
				}
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
	private List<Mention> processTokens(NerdQuery nerdQuery) throws NerdException { 
		List<LayoutToken> tokens = nerdQuery.getTokens();

		List<Mention> results = new ArrayList<>();
		
		Language language = nerdQuery.getLanguage();
		String lang = null;
		if (language != null)
			lang = language.getLang();

		if (!lang.equals("en") && !lang.equals("fr"))
			return results;
		
		// get the list of requested mention types
		List<ProcessText.MentionMethod> mentionTypes = nerdQuery.getMentions();

		// we process the whole text, sentence info does not apply to layout documents
		try {
			/*if (nerParsers == null) {
				//Utilities.initGrobid();
				nerParsers = new NERParsers();	
			}
			List<Entity> entityResults = nerParsers.extractNE(tokens, language);
			for(Entity entityResult : entityResults) {
				Mention mention = new Mention(entityResult);
				mention.setSource(MentionMethod.NER);
				if (results == null) {
					results = new ArrayList<>();
				}
				results.add(mention);
			}*/
			for (ProcessText.MentionMethod mentionType : mentionTypes) {
				List<Mention> localResults = null;

				if (mentionType == ProcessText.MentionMethod.ner) {
					localResults = processNER(tokens, language);
				} else if (mentionType == ProcessText.MentionMethod.wikipedia) {
					localResults = processWikipedia(tokens, language);
				} else if (mentionType == ProcessText.MentionMethod.species) {
					localResults = processSpecies(tokens, language);
				}

				results.addAll(localResults);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new NerdException("NERD error when processing text.", e);
		}
		
		return results;
	}
	
		/**
	 * NER processing of some raw text. Generate list of named entity mentions.
	 *
	 * @param text 
	 *		the raw text to be parsed
	 * @return 
	 * 		the list of identified mentions
	 */
	public List<Mention> processNER(String text, Language language) throws NerdException { 
		if (text == null) {
			throw new NerdException("Cannot parse the sentence, because it is null.");
		}
		else if (text.length() == 0) {
			//System.out.println("The length of the text to be processed is 0.");
			LOGGER.error("The length of the text to be parsed is 0.");
			return null;
		}
		String lang = language.getLang();
		if (!lang.equals("en") && !lang.equals("fr"))
			return null;

		List<Mention> results = new ArrayList<>();
		try {
			if (nerParsers == null) {
				//Utilities.initGrobid();
				nerParsers = new NERParsers();	
			}
			List<Entity> entityResults = nerParsers.extractNE(text, language);
			for(Entity entityResult : entityResults) {
				Mention mention = new Mention(entityResult);
				mention.setSource(MentionMethod.ner);
				results.add(mention);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new NerdException("NERD error when processing text.", e);
		}
		
		return results;
	}


	/**
	 * NER processing of a sequence of LayoutTokens. Generate list of named entity 
	 * mentions.
	 *
	 * @param tokens 
	 *		the sequence of LayoutToken objects
	 * @return 
	 * 		the list of identified mentions
	 */
	public List<Mention> processNER(List<LayoutToken> tokens, Language language) throws NerdException { 
		if (tokens == null) {
			throw new NerdException("Cannot parse the sequence, because it is null.");
		}
		else if (tokens.size() == 0) {
			//System.out.println("The length of the text to be processed is 0.");
			LOGGER.error("The size of the sequence to be parsed is 0.");
			return null;
		}
		String lang = language.getLang();
		if (!lang.equals("en") && !lang.equals("fr"))
			return null;

		List<Mention> results = new ArrayList<>();
		try {
			if (nerParsers == null) {
				//Utilities.initGrobid();
				nerParsers = new NERParsers();	
			}
			List<Entity> entityResults = nerParsers.extractNE(LayoutTokensUtil.toText(tokens), language);
			for(Entity entityResult : entityResults) {
				Mention mention = new Mention(entityResult);
				mention.setSource(MentionMethod.ner);
				results.add(mention);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new NerdException("NERD error when processing text.", e);
		}
		
		// associate bounding boxes to identified mentions 
    	List<Mention> finalResults = new ArrayList<>();
    	Collections.sort(results);
    	int tokenPos = 0;
		int lastTokenIndex = 0;
		int lastTokenPos = 0;
		for(Mention entity : results) {
			/*org.grobid.core.utilities.OffsetPosition pos = 
				new org.grobid.core.utilities.OffsetPosition();
			pos.start = entity.getOffsetStart();
			pos.end = entity.getOffsetEnd();
			entity.setOffsets(pos);*/
			// synchronize layout token with the selected ngrams
			List<LayoutToken> entityTokens = null;
			tokenPos = lastTokenPos;
			for(int j=lastTokenIndex; j<tokens.size(); j++) {
				if (tokenPos < entity.getOffsetStart()) {
					tokenPos += tokens.get(j).getText().length();
					continue;
				}
				if (tokenPos + tokens.get(j).getText().length() > entity.getOffsetEnd()) {
					break;
				}

				if (tokenPos == entity.getOffsetStart()) {
					entityTokens = new ArrayList<LayoutToken>();
					entityTokens.add(tokens.get(j));
					lastTokenIndex = j;
					lastTokenPos = tokenPos;
				} else if ( (tokenPos >= entity.getOffsetStart()) && (tokenPos <= entity.getOffsetEnd()) ) {
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
				LOGGER.warn("LayoutToken sequence not found for mention: " + entity.getRawName(
					));
			// we have an additional check of validy based on language
			//if (validEntity(entity, lang.getLang()))
			if (!finalResults.contains(entity)) {
				finalResults.add(entity);
			}
		}

		return finalResults;
	}


	/**
	 * Processing of some raw text by extracting all non-trivial ngrams. 
	 * Generate a list of entity mentions that will be instanciated by 
	 * Wikipedia labels (anchors and titles). 
	 * We assume here that the text has been dehyphenized before calling this method.
	 *
	 * @param text 
	 *		the raw text to be parsed
	 * @return 
	 * 		the list of identified entities.
	 */
	public List<Mention> processWikipedia(String text, Language lang) throws NerdException { 
		if (text == null) {
			throw new NerdException("Cannot parse the text, because it is null.");
		}
		else if (text.length() == 0) {
			//System.out.println("The length of the text to be processed is 0.");
			LOGGER.error("The length of the text to be parsed is 0.");
			return null;
		}
		
		List<Mention> results = new ArrayList<Mention>();
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
				Mention entity = new Mention(candidate.string, MentionMethod.wikipedia);
				
				org.grobid.core.utilities.OffsetPosition pos = 
					new org.grobid.core.utilities.OffsetPosition();
				pos.start = candidate.pos;
				pos.end = pos.start + candidate.string.length();
				entity.setOffsets(pos);
				// we have an additional check of validy based on language
				if (validEntity(entity, lang.getLang())) {
					if (!results.contains(entity))
						results.add(entity);
				}

if (pos.start == -1)
System.out.println("!!!!!!!!!!!!!!!!!!!!! start pos is -1 for " + entity.getRawName());	
			}
		} catch(Exception e) {
			throw new NerdException("NERD error when processing text.", e);
		}
		return results;
	}
	
	/**
	 * Processing of some raw text by extracting all non-trivial ngrams. 
	 * Generate a list of entity mentions that will be instanciated by 
	 * Wikipedia labels (anchors and titles). 
	 *
	 * @param tokens 
	 *		the sequence of tokens to be parsed
	 * @return 
	 * 		the list of identified entities.
	 */
	public List<Mention> processWikipedia(List<LayoutToken> tokens, Language lang) throws NerdException { 
		if ( (tokens == null) || (tokens.size() == 0) ) {
			//System.out.println("Content to be processed is empty.");
			LOGGER.error("Content to be processed is empty.");
			return null;
		}
		String text = LayoutTokensUtil.toText(tokens);
		List<Mention> results = new ArrayList<Mention>();
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
				Mention entity = new Mention(candidate.string, MentionMethod.wikipedia);				
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
					if (!results.contains(entity))
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
	/*public List<Mention> processWeightedTerms(List<WeightedTerm> terms) throws NerdException { 
		if (terms == null) {
			throw new NerdException("Cannot parse the weighted terms, because it is null.");
		}
		else if (terms.length() == 0) {
			System.out.println("The length of the term vector to be processed is 0.");
			LOGGER.error("The length of the term vector to be processed is 0.");
			return null;
		}
		
		List<Mention> results = new ArrayList<Mention>();
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
	 * Processing of some raw text by extracting all non-trivial ngrams. 
	 * Generate a list of entity mentions that will be instanciated by 
	 * Wikipedia labels (anchors and titles). 
	 *
	 * @param nerdQuery 
	 *		the NERD query to be processed
	 * @return 
	 * 		the list of identified entities.
	 */
	/*public List<Mention> processWikipedia(NerdQuery nerdQuery) throws NerdException { 
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
		
		List<Mention> results = null;
		
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
					List<Mention> localResults = processBrutal(text2tag, language);

					// we "shift" the entities offset in case only specific sentences are processed
					if ( CollectionUtils.isNotEmpty(localResults) ) {
						for(Mention entity : localResults) {
							entity.setOffsetStart(sentence.getOffsetStart() + entity.getOffsetStart());
							entity.setOffsetEnd(sentence.getOffsetStart() + entity.getOffsetEnd());
						}
						for(Mention entity : localResults) {
							if (validEntity(entity, lang)) {
								if (results == null)
									results = new ArrayList<>();
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
			if (CollectionUtils.isNotEmpty(tokens) )
				return processBrutal(tokens, language);
			else
				return processBrutal(text, language);
		}
		return results;
	}*/
	
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
    private static boolean validEntity(Mention entity, String lang) {
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
    *  Detect possible explicit acronym introductions based on patterns, and update
    *  the list of mentions accordingly.
    *  @param nerdQuery the NERD query
    *  @param entities the current list of mentions to complete with acronyms
    *  @return the updated list of Entity 
    */
   	public static List<Mention> acronymCandidates(NerdQuery nerdQuery, List<Mention> entities) {
   		if (entities == null)
   			entities = new ArrayList<Mention>();

    	String text = nerdQuery.getText();
		List<LayoutToken> tokens = nerdQuery.getTokens();

		if ( (text == null) || (text.length() == 0) ) {
			//LOGGER.info("The length of the text to be parsed is 0. Look at the layout tokens.");
			if ( (tokens != null) && (tokens.size() > 0) )
				text = LayoutTokensUtil.toText(tokens);
			else {
				LOGGER.error("All possible content to process are empty - process stops.");
				return entities;
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
		
		Map<Mention, Mention> acronyms = null;
		if (CollectionUtils.isNotEmpty(tokens) ) 
			acronyms = acronymCandidates(tokens);
		else
			acronyms = acronymCandidates(text, language);

		if (acronyms != null) {
			if (nerdQuery.getContext() == null)
				nerdQuery.setContext(new NerdContext());
			nerdQuery.getContext().setAcronyms(acronyms);
    		for (Map.Entry<Mention, Mention> entry : acronyms.entrySet()) {
        		Mention base = entry.getValue();
        		Mention acronym = entry.getKey();
System.out.println("acronym: " + acronym.getOffsetStart() + " " + acronym.getOffsetEnd() + " / base: " + base.getRawName());
				
				Mention localEntity = new Mention();
				localEntity.setRawName(acronym.getRawName());
				localEntity.setOffsetStart(acronym.getOffsetStart());
				localEntity.setOffsetEnd(acronym.getOffsetEnd());
				localEntity.setIsAcronym(true);
				localEntity.setNormalisedName(base.getRawName());
				localEntity.setSource(base.getSource());
				entities.add(localEntity);
			}

			// propagate back mentions
			List<Mention> acronymEntities = ProcessText.propagateAcronyms(nerdQuery);
			if (acronymEntities != null) {
				for(Mention entity : acronymEntities) {
					entities.add(entity);
				}
			}
		}

		return entities;
	}

	public static Map<Mention, Mention> acronymCandidates(String text, Language language) {
		List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, language);
		return acronymCandidates(tokens);
    }

    public static Map<Mention, Mention> acronymCandidates(List<LayoutToken> tokens) {
    	Map<Mention, Mention> acronyms = null;

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
				if (TextUtilities.isAllUpperCaseOrDigitOrDot(token.getText())) {
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
							if ( (tok.trim().length() == 0) || (delimiters.indexOf(tok) != -1) )
								continue;
							boolean numericMatch = false;
							if ((tok.length() > 1) &&  StringUtils.isNumeric(tok)) {
//System.out.println("acronym: " + acronym.getText());
//System.out.println("tok: " + tok);
								// when the token is all digit, it often appears in full as such in the
								// acronym (e.g. GDF15)
								String acronymCurrentPrefix = acronym.getText().substring(0, k+1);
//System.out.println("acronymCurrentPrefix: " + acronymCurrentPrefix);								
								if (acronymCurrentPrefix.endsWith(tok)) {
									// there is a full number match
									k = k - tok.length() + 1;
									numericMatch = true;
//System.out.println("numericMatch is: " + numericMatch);									
								}
							}

							if ( (tok.toLowerCase().charAt(0) == c) || numericMatch) {
								if (k == 0) {
									if (acronyms == null) 
										acronyms = new HashMap<Mention,Mention>();
									StringBuilder builder = new StringBuilder();
									for(int l = j; l < posParenthesis; l++) {
										builder.append(tokens.get(l));
									}

									Mention entityAcronym = new Mention();
									entityAcronym.setRawName(acronym.getText());
									entityAcronym.setNormalisedName(builder.toString().trim());
									entityAcronym.setOffsetStart(acronym.getOffset());
									entityAcronym.setOffsetEnd(acronym.getOffset() + acronym.getText().length());
									entityAcronym.setType(null);

									Mention entityBase = new Mention(builder.toString().trim());
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
    public static List<Mention> propagateAcronyms(NerdQuery nerdQuery) {

    	if ( (nerdQuery == null) || (nerdQuery.getContext() == null) )
    		return null;

    	Map<Mention, Mention> acronyms = nerdQuery.getContext().getAcronyms();

    	if (acronyms == null)
    		return null;

    	List<Mention> entities = null;
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

		Map<Mention, Mention> toBeAdded = new HashMap<Mention, Mention>();
		for (Map.Entry<Mention, Mention> entry : acronyms.entrySet()) {
            Mention acronym = entry.getKey();
            Mention base = entry.getValue();
			Pattern linkPattern = Pattern.compile(acronym.getRawName()); 
			Matcher linkMatcher = linkPattern.matcher(text);
			while (linkMatcher.find()) {
				// we need to ignore the current acronym to avoid having it twice
				if ( (linkMatcher.start() == acronym.getOffsetStart())	&&
					(linkMatcher.end() == acronym.getOffsetEnd()) )
					continue;
				String entityText = text.substring(linkMatcher.start(), linkMatcher.end());
				Mention entity = new Mention(entityText);
				entity.setNormalisedName(base.getRawName());
				entity.setOffsetStart(linkMatcher.start());
				entity.setOffsetEnd(linkMatcher.end());
				entity.setType(null);
				if (entities == null)
					entities = new ArrayList<Mention>();
				entities.add(entity);
				toBeAdded.put(entity, base);
			}
		}

		for (Map.Entry<Mention, Mention> entry : toBeAdded.entrySet()) {
			Mention entity = entry.getKey();
            Mention base = entry.getValue();
            if (acronyms.get(entity) == null)
	            acronyms.put(entity, base);
		}
		return entities;
    }

    /**
     * Compute modified generalized DICE coefficient of a term given global Wikipedia frequencies,
     * in order to capture lexical cohesion of the term.
     * see [Park and al., 2002] for formula of Generalized DICE coefficients, 
     * http://aclweb.org/anthology/C02-1142 
     */
    public static double getDICECoefficient(String term, String lang) {
		// term frequency
		LowerKnowledgeBase wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf(lang);
		Label label = NerdEngine.bestLabel(term, wikipedia);

		double avFreqTerm = 0.0;
		if (label.getOccCount() != 0)
			avFreqTerm = (double)label.getOccCount();

		// tokenise according to language and remove punctuations/delimeters
		List<String> tokens = GrobidAnalyzer.getInstance().tokenize(term, new Language(lang, 1.0));
		List<String> newTokens = new ArrayList<String>();
		for(String token : tokens) {
			if ((token.trim().length() == 0) || (delimiters.indexOf(token) != -1))
				continue;
			newTokens.add(token);
		}
		tokens = newTokens;
		int termLength = tokens.size();

		double avFreqComponent = 1000000000.0;

		// get component frequencies

		// The modification with respect to Generalized DICE coefficient is here - we take the min rather
		// the sum of component frequencies, the idea is to better capture terms where one component is
		// very frequent and the second appear only with the first component (component frequency then 
		// equals full term frequency) which mean normally a very high lexical cohesion for the term.
		
		for(String component : tokens) {
			Label labelComponent = NerdEngine.bestLabel(component, wikipedia);
			if (labelComponent.getOccCount() < avFreqTerm) {
				//avFreqComponent += (double)labelComponent.getOccCount() + avFreqTerm;
				avFreqComponent = Math.min(avFreqComponent, (double)labelComponent.getOccCount() + avFreqTerm);
			}
			else {
				//avFreqComponent += (double)labelComponent.getOccCount();
				avFreqComponent = Math.min(avFreqComponent, (double)labelComponent.getOccCount());
			}

//System.out.println(component + " - labelComponent.getOccCount(): " + labelComponent.getOccCount() );
		}
				
		// compute generalized DICE coef.
		double dice = 0.0;
		
		if (avFreqTerm == 0.0)
			dice = 0.0;
		else if ( (avFreqTerm <= 1.0) & (termLength > 1) ) {
			// we don't want to have dice == 0 when only 1 occurence, but at least a very small value
			dice = (Math.log10(1.1) * termLength) / avFreqComponent; 
		}
		else {
			// get the default generalized DICE's coef.
			dice = (avFreqTerm * Math.log10((double)avFreqTerm) * termLength) / avFreqComponent;
		}
		
		// single word needs to be reduced (see [Park & al., 2002])
		if ( (termLength == 1) && (avFreqTerm != 0.0) ) {
			dice = dice / avFreqTerm;
		}

//System.out.println("label.getOccCount(): " + label.getOccCount());
//System.out.println("avFreqTerm: " + avFreqTerm);
//System.out.println("avFreqComponent: " + avFreqComponent);
//System.out.println("termLength: " + termLength);
	
		if (dice > 1.0)
			dice = 1.0;

		return dice;
    }

    private uk.ac.man.entitytagger.matching.Matcher matcher = null;

    public List<Mention> processSpecies(String text, Language language) {
    	if (!language.getLang().equals("en"))
    		return null;
    	List<Mention> results = new ArrayList<>();

    	if (matcher == null) {
    		ArgParser ap = new ArgParser(new String[]{});
			ap.addProperties("internal:/resources-linnaeus/properties.conf");
			java.util.logging.Logger logger = Loggers.getDefaultLogger(ap);
    		matcher = uk.ac.man.entitytagger.EntityTagger.getMatcher(ap, logger);
    	}

    	DocumentIterator doc = new TextFile(text);
    	TaggedDocument taggedDocument = MatchOperations.matchDocument(matcher, doc.next()); 

    	ArrayList<uk.ac.man.entitytagger.Mention> mentions = taggedDocument.getAllMatches(); 
    	if (mentions != null) {
	    	for(uk.ac.man.entitytagger.Mention mention : mentions) {
	    		int start = mention.getStart();
	    		int end = mention.getEnd();
	    		Mention entity = new Mention(mention.getText(), MentionMethod.species);
	    		entity.setOffsetStart(start);
	    		entity.setOffsetEnd(end);
	    		entity.setNormalisedName(mention.getIdsToString());
	   			results.add(entity);
	   			System.out.println(entity.toString());
	    	}
	    }

    	return results;
    }

    public List<Mention> processSpecies(List<LayoutToken> tokens, Language language) {	
    	if (!language.getLang().equals("en"))
    		return null;
    	List<Mention> results = new ArrayList<>();

    	if (matcher == null) {
    		ArgParser ap = new ArgParser(new String[]{});
			ap.addProperties("internal:/resources-linnaeus/properties.conf");
			java.util.logging.Logger logger = Loggers.getDefaultLogger(ap);
    		matcher = uk.ac.man.entitytagger.EntityTagger.getMatcher(ap, logger);
    	}

    	String text = LayoutTokensUtil.toText(tokens);
    	DocumentIterator doc = new TextFile(text);
    	TaggedDocument taggedDocument = MatchOperations.matchDocument(matcher, doc.next()); 

    	ArrayList<uk.ac.man.entitytagger.Mention> mentions = taggedDocument.getAllMatches(); 
    	if (mentions != null) {
	    	for(uk.ac.man.entitytagger.Mention mention : mentions) {
	    		int start = mention.getStart();
	    		int end = mention.getEnd();
	    		Mention entity = new Mention(mention.getText(), MentionMethod.species);
	    		entity.setOffsetStart(start);
	    		entity.setOffsetEnd(end);
	    		entity.setNormalisedName(mention.getIdsToString());
	   			results.add(entity);
	   			System.out.println(entity.toString());
	    	}
	    }

    	// associate bounding boxes to identified mentions 
    	List<Mention> finalResults = new ArrayList<>();
    	Collections.sort(results);
    	int tokenPos = 0;
		int lastTokenIndex = 0;
		int lastTokenPos = 0;
		for(Mention entity : results) {
			/*org.grobid.core.utilities.OffsetPosition pos = 
				new org.grobid.core.utilities.OffsetPosition();
			pos.start = entity.getOffsetStart();
			pos.end = entity.getOffsetEnd();
			entity.setOffsets(pos);*/
			// synchronize layout token with the selected ngrams
			List<LayoutToken> entityTokens = null;
			tokenPos = lastTokenPos;
			for(int j=lastTokenIndex; j<tokens.size(); j++) {
				if (tokenPos < entity.getOffsetStart()) {
					tokenPos += tokens.get(j).getText().length();
					continue;
				}
				if (tokenPos + tokens.get(j).getText().length() > entity.getOffsetEnd()) {
					break;
				}

				if (tokenPos == entity.getOffsetStart()) {
					entityTokens = new ArrayList<LayoutToken>();
					entityTokens.add(tokens.get(j));
					lastTokenIndex = j;
					lastTokenPos = tokenPos;
				} else if ( (tokenPos >= entity.getOffsetStart()) && (tokenPos <= entity.getOffsetEnd()) ) {
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
				LOGGER.warn("LayoutToken sequence not found for mention: " + entity.getRawName(
					));
			// we have an additional check of validy based on language
			//if (validEntity(entity, lang.getLang()))
			if (!finalResults.contains(entity)) {
				finalResults.add(entity);
			}
		}

    	return finalResults;
    }

    public static int MINIMAL_PARAGRAPH_LENGTH = 100;
    public static int MAXIMAL_PARAGRAPH_LENGTH = 600;

    public static List<List<LayoutToken>> segmentInParagraphs(List<LayoutToken> tokens) {
    	// heuristics: double end of line, if not simple end of line (not aligned with 
    	// previous line), and if still not we segment arbitrarly the monolithic block
    	List<List<LayoutToken>> result = new ArrayList<>();
    	result.add(tokens);

    	// we recursively segment too large segments, starting with one unique segment
    	// which is the whole text

    	while(true) {
    		result = subSsegmentInParagraphs(result);
    		if (!containsTooLargeSegment(result))
    			break;
    	}

    	return result;
    }

    private static boolean containsTooLargeSegment(List<List<LayoutToken>> segments) {
    	for(List<LayoutToken> segment : segments) {
    		if (segment.size() > MAXIMAL_PARAGRAPH_LENGTH) {
    			return true;
    		}
    	} 
    	return false;
    }


    private static List<List<LayoutToken>> subSsegmentInParagraphs(List<List<LayoutToken>> segments) {
    	List<List<LayoutToken>> result = new ArrayList<>();

    	for(List<LayoutToken> segment : segments) {
    		if (segment.size() > MAXIMAL_PARAGRAPH_LENGTH) {
    			// let's try to slice this guy
    			boolean previousEOL = false;
		    	int n = 0; // current position in the segment
		    	List<LayoutToken> currentParagraph = new ArrayList<LayoutToken>();
		    	for(LayoutToken token : segment) {
		    		currentParagraph.add(token);
		    		if (token.getText().equals("\n")) {
		    			if (previousEOL) {
		    				if (n > MINIMAL_PARAGRAPH_LENGTH) {
		    					// new paragraph
		    					result.add(currentParagraph);
		    					currentParagraph = new ArrayList<LayoutToken>();
		    					n = 0;
		    				}
		    				previousEOL = false;
		    			} else
		    			 	previousEOL = true;
		    		} else 
		    			previousEOL = false;
		    		n++;
		    	}
		    	result.add(currentParagraph);
			}
		}

		if (!containsTooLargeSegment(result)) 
			return result;

		// if we fail to to slice with double EOL, let's see if we can do something
		// with simple EOL
		segments = result;
		result = new ArrayList<>();
		for(List<LayoutToken> segment : segments) {
    		if (segment.size() > MAXIMAL_PARAGRAPH_LENGTH) {
    			// let's try to slice this guy
		    	int n = 0; // current position in the segment
		    	List<LayoutToken> currentParagraph = new ArrayList<LayoutToken>();
		    	for(LayoutToken token : segment) {
		    		currentParagraph.add(token);
		    		if (token.getText().equals("\n")) {
	    				if (n > MINIMAL_PARAGRAPH_LENGTH) {
	    					// new paragraph
	    					result.add(currentParagraph);
	    					currentParagraph = new ArrayList<LayoutToken>();
	    					n = 0;
	    				}
		    		}
		    		n++;
		    	}
		    	result.add(currentParagraph);
			}
		}

		if (!containsTooLargeSegment(result)) 
			return result;

		segments = result;
		result = new ArrayList<>();
		for(List<LayoutToken> segment : segments) {
			if (segment.size() > MAXIMAL_PARAGRAPH_LENGTH) {
		    	// if failure again, we arbitrarly segment
		    	int n = 0;
				List<LayoutToken> currentParagraph = new ArrayList<LayoutToken>();
		    	for(LayoutToken token : segment) {
		    		currentParagraph.add(token);
    				if (n == MAXIMAL_PARAGRAPH_LENGTH-1) {
    					// new paragraph
    					result.add(currentParagraph);
    					currentParagraph = new ArrayList<LayoutToken>();
    					n = 0;
    				}
		    		n++;
		    	}
		    	result.add(currentParagraph);
    		} else {
    			// no need to further segment
    			result.add(segment);
    		}
    	}

    	return result;
    }

}