package com.scienceminer.nerd.mention;

import com.google.common.collect.Iterables;
import com.scienceminer.nerd.disambiguation.NerdContext;
import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Label;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.utilities.Stopwords;
import com.scienceminer.nerd.utilities.StringPos;
import com.scienceminer.nerd.utilities.StringProcessor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Entity;
import org.grobid.core.engines.NERParsers;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.*;
import org.grobid.core.utilities.GrobidConfig.ModelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Everything we need to get the mentions and names entities from a text. From a text or a
 * NerdQuery object, we generate a list of Mention objects corresponding to the potential
 * mentions of entity to be considred by the further disambiguation stage.
 * <p>
 * For producing these mentions, different recognition modules/algorithms can be used, the
 * default one being wikipedia via the Wikipedia anchors and titles. Other additional
 * possibilities are named entity (NER) or spcialised recognizers like species names,
 * quantities, biomedical substances, physical formula, etc.
 * <p>
 * The list of possible mention recognition methods are given by a list of MentionMethod
 * attributes. When processing a text, mentions will be produced sequentially by the
 * application of each of these modules, resulting in a list of Mention objects associated
 * with the MentionMethod that produced it and usual mention data (position offset,
 * raw text, normalized text, ...).
 */
public class ProcessText {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessText.class);
    public static final List<String> GROBID_NER_SUPPORTED_LANGUAGES = Arrays.asList(Language.FR, Language.EN);

    public static final int DEFAULT_NGRAM_LENGTH = 6;
    public static final int DEFAULT_TARGET_SEGMENT_SIZE = 1000;

    private static volatile ProcessText instance;

    private NERParsers nerParsers = null;

    private Stopwords stopwords = Stopwords.getInstance();

    // default indo-european delimiters, should be moved to language specific analysers
    public static String delimiters = " \n\t" + TextUtilities.fullPunctuations + "。、，・";

    public static ProcessText getInstance() {
        if (instance == null)
            getNewInstance();
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        LOGGER.debug("Get new instance of ProcessText");

        instance = new ProcessText();
    }

    /**
     * Hidden constructor
     */
    private ProcessText() {
        String grobidHome = com.scienceminer.nerd.utilities.Utilities.initGrobid();
        Path grobidHomePath = Paths.get(grobidHome);
        Path grobidNerPath = grobidHomePath.resolve("../../grobid-ner/");

        // the following will ensure that the Grobid environment and config are loaded
        // independently from the NER models
        GrobidProperties.getInstance();

        // load default grobid-ner config based on the grobid-home path and init the module
        GrobidNerConfiguration grobidNerConfiguration = GrobidNerConfiguration.getInstance(grobidNerPath.toString());
        for (ModelParameters theModel : grobidNerConfiguration.getModels()) {
            GrobidProperties.getInstance().addModel(theModel);
        }
        LibraryLoader.load();

        nerParsers = new NERParsers();
    }

    /**
     * Case context where a token appears
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
        String text = nerdQuery.getTextOrShortText();

        List<LayoutToken> tokens = nerdQuery.getTokens();

        if (isBlank(text) && isEmpty(tokens)) {
            LOGGER.warn("No content to process.");
            return new ArrayList<>();
        }

        if (isNotBlank(text))
            return processText(nerdQuery);
        else
            return processTokens(nerdQuery);
    }


    /**
     * Precondition: text in the query object is not empty and
     * we assume here that the text has been dehyphenized before calling this method.
     */
    private List<Mention> processText(NerdQuery nerdQuery) throws NerdException {
        String text = nerdQuery.getTextOrShortText();
        text = UnicodeUtil.normaliseText(text);

        List<Mention> results = new ArrayList<>();

        Language language = nerdQuery.getLanguage();

        Integer[] processSentence = nerdQuery.getProcessSentence();
        List<Sentence> sentences = nerdQuery.getSentences();

        // get the list of requested mention types
        List<ProcessText.MentionMethod> mentionTypes = nerdQuery.getMentions();

        // do we need to process the whole text only a sentence?
        if (ArrayUtils.isNotEmpty(processSentence) && CollectionUtils.isNotEmpty(sentences)) {
            // we process only the indicated sentences
            String text2tag = null;
            for (int i = 0; i < processSentence.length; i++) {
                Integer index = processSentence[i];

                // for robustness, we have to consider index out of the current sentences range
                // here we ignore it, but we might better raise an exception and return an error
                // message to the client
                if (index >= sentences.size())
                    continue;

                Sentence sentence = sentences.get(index);
                try {
                    text2tag = text.substring(sentence.getOffsetStart(), sentence.getOffsetEnd());
                } catch (StringIndexOutOfBoundsException sioobe) {
                    throw new QueryException("The sentence offsets are not correct", sioobe);
                }

                try {
                    for (ProcessText.MentionMethod mentionType : mentionTypes) {
                        List<Mention> localResults = getMentions(text2tag, language, mentionType, nerdQuery.getNgramLength());

                        // we "shift" the entities offset in case only specific sentences are processed
                        for (Mention entity : localResults) {
                            Mention mention = new Mention(entity);
                            mention.setOffsetStart(sentence.getOffsetStart() + entity.getOffsetStart());
                            mention.setOffsetEnd(sentence.getOffsetStart() + entity.getOffsetEnd());
                            //mention.setSource(entity.getSource());
                            results.add(mention);
                        }
                    }
                } catch (Exception e) {
                    throw new NerdException("NERD error when processing text.", e);
                }
            }
        } else {
            // we process the whole text
            try {
                for (ProcessText.MentionMethod mentionType : mentionTypes) {
                    List<Mention> localResults = getMentions(text, language, mentionType, nerdQuery.getNgramLength());
                    results.addAll(localResults);
                }
            } catch (Exception e) {
                throw new NerdException("NERD error when processing text.", e);
            }
        }

        return results;
    }

    /**
     * Precondition: list of LayoutToken in the query object is not empty
     */
    private List<Mention> processTokens(NerdQuery nerdQuery) throws NerdException {
        List<LayoutToken> tokens = nerdQuery.getTokens();
        List<Mention> results = new ArrayList<>();

        Language language = nerdQuery.getLanguage();

        // get the list of requested mention types
        List<ProcessText.MentionMethod> mentionTypes = nerdQuery.getMentions();

        // we process the whole text, sentence info does not apply to layout documents
        try {
            for (ProcessText.MentionMethod mentionType : mentionTypes) {
                List<Mention> localResults = getMentions(tokens, language, mentionType, nerdQuery.getNgramLength());
                results.addAll(localResults);
            }
        } catch (Exception e) {
            throw new NerdException("NERD error when processing text.", e);
        }

        return results;
    }

    private List<Mention> getMentions(String text, Language language, MentionMethod mentionType, Integer ngramLength) {
        List<Mention> localResults = new ArrayList<>();

        if (mentionType == MentionMethod.ner) {
            localResults = processNER(text, language);
        } else if (mentionType == MentionMethod.wikipedia) {
            localResults = processWikipedia(text, language, ngramLength);
        } /*else if (mentionType == ProcessText.MentionMethod.species) {
            localResults = processSpecies(text, language);
        }*/
        return localResults;
    }

    private List<Mention> getMentions(List<LayoutToken> tokens, Language language, MentionMethod mentionType, Integer ngramLength) {
        List<Mention> localResults = new ArrayList<>();

        if (mentionType == MentionMethod.ner) {
            localResults = processNER(tokens, language);
        } else if (mentionType == MentionMethod.wikipedia) {
            localResults = processWikipedia(tokens, language, ngramLength);
        } /*else if (mentionType == ProcessText.MentionMethod.species) {
            localResults = processSpecies(tokens, language);
        }*/
        return localResults;
    }

    /**
     * NER processing of some raw text. Generate list of named entity mentions.
     *
     * @param text the raw text to be parsed
     * @return the list of identified mentions
     */
    public List<Mention> processNER(String text, Language language) throws NerdException {
        final List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, language);

        return extractNER(tokens, language);
    }

    /**
     * Utility method to process a list of layout tokens and return the NER mentions
     **/
    private List<Mention> extractNER(List<LayoutToken> tokens, Language language) {
        List<Mention> results = new ArrayList<>();

        if (isEmpty(tokens)) {
            LOGGER.warn("Trying to extract NE mention from empty content. Returning empty list.");
            return results;
        }

        String lang = language.getLang();
        if ((lang == null) || (!lang.equals("en") && !lang.equals("fr")))
            return new ArrayList<>();

        try {
            List<Entity> entityResults = nerParsers.extractNE(tokens, language);
            for (Entity entityResult : entityResults) {
                Mention mention = new Mention(entityResult);
                mention.setSource(MentionMethod.ner);
                results.add(mention);
            }
        } catch (Exception e) {
            LOGGER.error("NER extraction failed", e);
        }

        return results;
    }


    /**
     * NER processing of a sequence of LayoutTokens. Generate list of named entity
     * mentions.
     *
     * @param tokens the sequence of LayoutToken objects
     * @return the list of identified mentions
     */
    public List<Mention> processNER(List<LayoutToken> tokens, Language language) throws NerdException {
        List<Mention> results = extractNER(tokens, language);

        Collections.sort(results);

        // associate bounding boxes to identified mentions
        List<Mention> finalResults = new ArrayList<>();

        for (Mention entity : results) {
            // synchronize layout token with the selected n-grams
            List<LayoutToken> entityTokens = entity.getLayoutTokens();

            if (entityTokens != null)
                entity.setBoundingBoxes(BoundingBoxCalculator.calculate(entityTokens));
            else
                LOGGER.warn("processNER: LayoutToken sequence not found for mention: " + entity.getRawName());
            // we have an additional check of validity based on language
            if (validEntity(entity, language.getLang())) {
                if (!finalResults.contains(entity)) {
                    finalResults.add(entity);
                }
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
     * @param text the raw text to be parsed
     * @return the list of identified entities.
     */
    public List<Mention> processWikipedia(String text, Language lang) throws NerdException {
        return processWikipedia(text, lang, null);
    }
    public List<Mention> processWikipedia(String text, Language lang, Integer ngramLength) throws NerdException {
        List<Mention> results = new ArrayList<>();
        if (StringUtils.isBlank(text)) {
            LOGGER.warn("Trying to extract Wikipedia mentions from empty content. Returning empty list. ");
            return results;
        }

        final GrobidAnalyzer grobidAnalyzer = GrobidAnalyzer.getInstance();
        return processWikipedia(grobidAnalyzer.tokenizeWithLayoutToken(text, lang), lang, ngramLength);
    }

    protected List<Mention> extractMentionsWikipedia(List<LayoutToken> tokens, Language lang, Integer ngramLength) {
        if (ngramLength == null)
            ngramLength = DEFAULT_NGRAM_LENGTH;

        List<StringPos> pool = ngrams(tokens, ngramLength);
        List<Mention> results = new ArrayList<>();

        // candidates which start and end with a stop word are removed.
        // beware not to be too aggressive.
        for (int i = 0; i < pool.size(); i++) {
            StringPos pos = pool.get(i);

            String rawName = pos.getString();
            String rawNameLowerCase = rawName.toLowerCase();

            // remove term starting or ending with a stop-word
            if (stopwords != null) {
                if (stopwords.startsWithStopword(rawNameLowerCase, lang.getLang())) {
                    if (!Character.isUpperCase(pos.getLayoutTokens().get(0).getText().codePointAt(0))) {
                        continue;
                    }
                } else if (stopwords.endsWithStopword(rawNameLowerCase, lang.getLang())) {
                    if (!Character.isUpperCase(Iterables.getLast(pos.getLayoutTokens()).getText().codePointAt(0))) {
                        continue;
                    }
                }
            }

            // remove term ending or starting (conservative, it should never be the case) with a separator
            if (delimiters.indexOf(rawNameLowerCase.charAt(0)) != -1
                || delimiters.indexOf(rawNameLowerCase.charAt(rawNameLowerCase.length() - 1)) != -1) {
                continue;
            }

            Mention mention = new Mention(pos.getString(), MentionMethod.wikipedia);
            mention.setOffsetStart(pos.getOffsetStart());
            mention.setOffsetEnd(pos.getOffsetStart() + pos.getString().length());
            mention.setLayoutTokens(pos.getLayoutTokens());

            // remove invalid mentions
            if (!validEntity(mention, lang.getLang())) {
                continue;
            }

            results.add(mention);
        }

        return results;
    }

    /**
     * Use extractMentionsWikipedia(List<LayoutToken> tokens, String lang)
     */
    @Deprecated
    /*protected List<StringPos> extractMentionsWikipedia(String text, Language lang) {
        List<StringPos> pool = ngrams(text, NGRAM_LENGTH, lang);

        // candidates which start and end with a stop word are removed.
        // beware not to be too aggressive.
        List<Integer> toRemove = new ArrayList<>();

        for (int i = 0; i < pool.size(); i++) {
            StringPos termPosition = pool.get(i);
            String termValue = termPosition.getString();
            //term = term.replace("\n", " ");
            String termValueLowercase = termValue.toLowerCase();

            // remove term starting or ending with a stop-word, and term starting with a separator (conservative
            // it should never be the case)
            if (stopwords != null) {
                if ((delimiters.indexOf(termValueLowercase.charAt(0)) != -1) ||
                        stopwords.startsWithStopword(termValueLowercase, lang.getLang()) ||
                        stopwords.endsWithStopword(termValueLowercase, lang.getLang())
                        ) {
                    toRemove.add(i);
                    continue;
                }
            }

            // remove term ending with a separator (conservative it should never be the case)
            while (delimiters.indexOf(termValueLowercase.charAt(termValueLowercase.length() - 1)) != -1) {
                termPosition.setString(termPosition.getString().substring(0, termPosition.getString().length() - 1));
                termValueLowercase = termValueLowercase.substring(0, termValueLowercase.length() - 1);
                if (termValueLowercase.length() == 0) {
                    toRemove.add(i);
                    continue;
                }
            }
        }

        List<StringPos> subPool = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            if (!toRemove.contains(i)) {
                subPool.add(pool.get(i));
            }
        }
        return subPool;
    }*/

    /**
     * Processing of some raw text by extracting all non-trivial ngrams.
     * Generate a list of entity mentions that will be instanciated by
     * Wikipedia labels (anchors and titles).
     *
     * @param tokens the sequence of tokens to be parsed
     * @return the list of identified entities.
     */
    public List<Mention> processWikipedia(List<LayoutToken> tokens, Language lang) throws NerdException {
        return processWikipedia(tokens, lang, null);
    }
    public List<Mention> processWikipedia(List<LayoutToken> tokens, Language lang, Integer ngramLength) throws NerdException {
        if ((tokens == null) || (tokens.size() == 0)) {
            //System.out.println("Content to be processed is empty.");
            LOGGER.error("Content to be processed is empty.");
            return null;
        }

        List<Mention> results = new ArrayList<>();
        try {
            List<Mention> subPool = extractMentionsWikipedia(tokens, lang, ngramLength);

            Collections.sort(subPool);
            for (Mention candidate : subPool) {
                List<LayoutToken> entityTokens = candidate.getLayoutTokens();

                if (entityTokens != null)
                    candidate.setBoundingBoxes(BoundingBoxCalculator.calculate(entityTokens));
                else
                    LOGGER.warn("processWikipedia: LayoutToken sequence not found for mention: " + candidate.rawName);
                // we have an additional check of validity based on language
                if (validEntity(candidate, lang.getLang())) {
                    if (!results.contains(candidate))
                        results.add(candidate);
                }
            }
        } catch (Exception e) {
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


    /**
     *  Sentence segmentation based on Grobid sentence segmenter.
     * 
     **/
    public List<Sentence> sentenceSegmentation(String text) {
        return sentenceSegmentation(text, null);
    }
    
    public List<Sentence> sentenceSegmentation(String text, Language lang) {
        List<OffsetPosition> sentencePositions = null;
        if (lang == null) {
            sentencePositions = SentenceUtilities.getInstance().runSentenceDetection(text);
        } else {
            sentencePositions = SentenceUtilities.getInstance().runSentenceDetection(text, lang);
        }

        List<Sentence> result = new ArrayList<>();
        for(OffsetPosition sentencePosition : sentencePositions) {
            Sentence localSentence = new Sentence();
            localSentence.setOffsets(sentencePosition);
            result.add(localSentence);
        }

        return result;
    }

    public List<StringPos> ngrams(List<LayoutToken> layoutTokens, int ngram) {
        List<StringPos> ngrams = new ArrayList<>();

        if (isEmpty(layoutTokens)) return ngrams;

        int actualNgram = (ngram * 2) - 1; // for taking into account separators

        for (int i = 0; i < layoutTokens.size(); i++) {
            if (StringUtils.isEmpty(layoutTokens.get(i).getText()))
                continue;

            int tmpNgram = Math.min(layoutTokens.size() - i, actualNgram);

            for (int n = 1; n <= tmpNgram; n++) {
                final List<LayoutToken> tokens = layoutTokens.subList(i, i + n);
                StringPos stringPos = new StringPos(LayoutTokensUtil.toText(tokens), tokens.get(0).getOffset(), tokens);

                ngrams.add(stringPos);
            }
        }
        return ngrams;
    }


    public static List<StringPos> ngrams(String str, int ngram, Language lang) {
        int actualNgram = (ngram * 2) - 1; // for taking into account separators
        List<StringPos> ngrams = new ArrayList<>();
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
                currentPos = str.indexOf(words.get(i), currentPos - 1);
                StringPos stringp = new StringPos();
                stringp.setString(StringProcessor.concat(words, i, i + n));
                stringp.setOffsetStart(currentPos);
                ngrams.add(stringp);
            }
        }
        return ngrams;
    }



    /**
     * Validity criteria for a raw entity. The entity raw string must not be
     * null, with additional requirements depending on language
     */
    private static boolean validEntity(Mention entity, String lang) {
        if ((entity == null) || (entity.getRawName() == null) || (entity.getRawName().length() == 0))
            return false;
        // we need in general to remove as valid mention:
        // * one letter tokens
        // * numerical tokens
        //if (entity.getRawName().length() <= 1 || TextUtilities.test_digit(entity.getRawName()))
        if (entity.getRawName().length() <= 1 || TextUtilities.countDigit(entity.getRawName()) == entity.getRawName().length())
            return false;

        return true;
    }

    /**
     * Detect possible explicit acronym introductions based on patterns, and update
     * the list of mentions accordingly.
     *
     * @param nerdQuery the NERD query
     * @param entities  the current list of mentions to complete with acronyms
     * @return the updated list of Entity
     */
    public List<Mention> acronymCandidates(NerdQuery nerdQuery, List<Mention> entities) {
        if (entities == null)
            entities = new ArrayList<>();

        String text = nerdQuery.getText();
        List<LayoutToken> tokens = nerdQuery.getTokens();

        if ((text == null) || (text.length() == 0)) {
            //LOGGER.info("The length of the text to be parsed is 0. Look at the layout tokens.");
            if ((tokens != null) && (tokens.size() > 0))
                text = LayoutTokensUtil.toText(tokens);
            else {
                LOGGER.error("All possible content to process are empty - process stops.");
                return entities;
            }
        }
        Language language = getLanguage(nerdQuery, text);

        Map<Mention, Mention> acronyms = null;
        if (CollectionUtils.isNotEmpty(tokens))
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
                LOGGER.debug("acronym: " + acronym.getRawName() + " / base: " + base.getRawName());

                Mention localEntity = new Mention(acronym);
                localEntity.setIsAcronym(true);
                localEntity.setNormalisedName(base.getRawName());
                localEntity.setSource(base.getSource());
                entities.add(localEntity);
            }

            // propagate back mentions
            List<Mention> acronymEntities = propagateAcronyms(nerdQuery);
            if (acronymEntities != null) {
                entities.addAll(acronymEntities);
            }
        }

        return entities;
    }

    private Language getLanguage(NerdQuery nerdQuery, String text) {
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
            } catch (Exception e) {
                LOGGER.debug("exception language identifier for: " + text);
            }
        }

        if (lang == null) {
            // default - it might be better to raise an exception?
            lang = Language.EN;
            language = new Language(lang, 1.0);
        }
        return language;
    }

    public Map<Mention, Mention> acronymCandidates(String text, Language language) {
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, language);
        return acronymCandidates(tokens);
    }

    public Map<Mention, Mention> acronymCandidates(List<LayoutToken> tokens) {
        Map<Mention, Mention> acronyms = null;

        // detect possible acronym
        boolean openParenthesis = false;
        int posParenthesis = -1;
        int i = 0;
        LayoutToken acronym = null;
        for (LayoutToken token : tokens) {
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
                if (isAllUpperCaseOrDigitOrDot(token.getText())) {
                    acronym = token;
                } else {
                    acronym = null;
                }
            }

            if ((acronym != null) && (!openParenthesis)) {
                // check if this possible acronym matches an immediately preceeding term
                int j = posParenthesis;
                int k = acronym.getText().length();
                boolean stop = false;
                while ((k > 0) && (!stop)) {
                    k--;
                    char c = acronym.getText().toLowerCase().charAt(k);
                    while ((j > 0) && (!stop)) {
                        j--;
                        if (tokens.get(j) != null) {
                            String tok = tokens.get(j).getText();
                            if (tok.trim().length() == 0 || delimiters.contains(tok))
                                continue;
                            boolean numericMatch = false;
                            if ((tok.length() > 1) && StringUtils.isNumeric(tok)) {
                                //System.out.println("acronym: " + acronym.getText());
                                //System.out.println("tok: " + tok);
                                // when the token is all digit, it often appears in full as such in the
                                // acronym (e.g. GDF15)
                                String acronymCurrentPrefix = acronym.getText().substring(0, k + 1);
                                //System.out.println("acronymCurrentPrefix: " + acronymCurrentPrefix);
                                if (acronymCurrentPrefix.endsWith(tok)) {
                                    // there is a full number match
                                    k = k - tok.length() + 1;
                                    numericMatch = true;
                                    //System.out.println("numericMatch is: " + numericMatch);
                                }
                            }

                            if ((tok.toLowerCase().charAt(0) == c) || numericMatch) {
                                if (k == 0) {
                                    if (acronyms == null)
                                        acronyms = new HashMap<>();
                                    List<LayoutToken> baseTokens = new ArrayList<>();
                                    StringBuilder builder = new StringBuilder();
                                    for (int l = j; l < posParenthesis; l++) {
                                        builder.append(tokens.get(l));
                                        baseTokens.add(tokens.get(l));
                                    }

                                    Mention entityAcronym = new Mention();
                                    entityAcronym.setRawName(acronym.getText());
                                    entityAcronym.setNormalisedName(builder.toString().trim());
                                    entityAcronym.setOffsetStart(acronym.getOffset());
                                    entityAcronym.setOffsetEnd(acronym.getOffset() + acronym.getText().length());
                                    entityAcronym.setType(null);
                                    entityAcronym.setIsAcronym(true);
                                    entityAcronym.setLayoutTokens(Arrays.asList(acronym));

                                    Mention entityBase = new Mention(builder.toString().trim());
                                    entityBase.setOffsetStart(tokens.get(j).getOffset());
                                    entityBase.setOffsetEnd(tokens.get(j).getOffset() + entityBase.getRawName().length());
                                    entityBase.setLayoutTokens(baseTokens);

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
     * Add entities corresponding to acronym definitions to a query
     */
    public List<Mention> propagateAcronyms(NerdQuery nerdQuery) {
        if ((nerdQuery == null) || (nerdQuery.getContext() == null))
            return null;
        Map<Mention, Mention> acronyms = nerdQuery.getContext().getAcronyms();
        if (acronyms == null)
            return null;

        String text = nerdQuery.getText();
        List<LayoutToken> tokens = nerdQuery.getTokens();
        if (CollectionUtils.isEmpty(tokens)) {
            if (StringUtils.isEmpty(text)) {
                LOGGER.error("All possible content to process are empty - process stops.");
                return null;
            } else {
                Language language = getLanguage(nerdQuery, text);
                tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, language);
            }
        }

        List<Mention> entities = new ArrayList<>();
        // iterate for every token in layout token list
        //outer:
        for (int i = 0; i < tokens.size(); i++) {
            // get the text and the offsets for every token
            final LayoutToken token = tokens.get(i);

            // find the acronym saved in the map to be compared with the current token
            for (Map.Entry<Mention, Mention> entry : acronyms.entrySet()) {
                Mention acronym = entry.getKey();
                Mention base = entry.getValue();

                List<LayoutToken> layoutTokensAcronym = acronym.getLayoutTokens();

                //we check whether the sequence correspond to the acronym and if so we get it as result
                final List<LayoutToken> matchedSequence = getSequenceMatch(tokens, i, layoutTokensAcronym);
                if (isEmpty(matchedSequence)) {
                    continue;
                }

                int offsetStart = token.getOffset();
                int offsetEnd = offsetStart + LayoutTokensUtil.toText(matchedSequence).length();

                Mention entity = new Mention(acronym.getRawName());
                entity.setNormalisedName(base.getRawName());
                entity.setOffsetStart(offsetStart);
                entity.setOffsetEnd(offsetEnd);
                entity.setLayoutTokens(matchedSequence);
                entity.setBoundingBoxes(BoundingBoxCalculator.calculate(entity.getLayoutTokens()));

                entities.add(entity);

                // Since we matched the acronym, we look forward and move on
                i += layoutTokensAcronym.size();
                //continue outer;
                break;
            }
        }
        return entities;
    }

    protected List<LayoutToken> getSequenceMatch(List<LayoutToken> tokens, int i, List<LayoutToken> layoutTokensAcronym) {
        final LayoutToken firstLayoutTokenAcronym = layoutTokensAcronym.get(0);

        List<LayoutToken> matchingList = new ArrayList<>();
        if (StringUtils.equals(firstLayoutTokenAcronym.getText(), tokens.get(i).getText())
                && firstLayoutTokenAcronym.getOffset() != tokens.get(i).getOffset()) {
            matchingList.add(tokens.get(i));
            int localCount = 1;
            int tokenStart = i + 1;
            //We try to match from i+1 (the second element) for n elements (n = the size of the acronym)
            for (int j = tokenStart; j < i + layoutTokensAcronym.size(); j++) {
                LayoutToken tok = tokens.get(j);
                if (StringUtils.equals(layoutTokensAcronym.get(localCount).getText(), tok.getText())) {
                    matchingList.add(tokens.get(j));
                } else {
                    matchingList = new ArrayList<>();
                    break;
                }
                localCount++;
            }


            return matchingList;
        }
        return Collections.emptyList();
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
            avFreqTerm = (double) label.getOccCount();

        // tokenise according to language and remove punctuations/delimeters
        List<String> tokens = GrobidAnalyzer.getInstance().tokenize(term, new Language(lang, 1.0));
        List<String> newTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token.trim().length() == 0 || delimiters.contains(token))
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

        for (String component : tokens) {
            Label labelComponent = NerdEngine.bestLabel(component, wikipedia);
            if (labelComponent.getOccCount() < avFreqTerm) {
                //avFreqComponent += (double)labelComponent.getOccCount() + avFreqTerm;
                avFreqComponent = Math.min(avFreqComponent, (double) labelComponent.getOccCount() + avFreqTerm);
            } else {
                //avFreqComponent += (double)labelComponent.getOccCount();
                avFreqComponent = Math.min(avFreqComponent, (double) labelComponent.getOccCount());
            }

//System.out.println(component + " - labelComponent.getOccCount(): " + labelComponent.getOccCount() );
        }

        // compute generalized DICE coef.
        double dice = 0.0;

        if (avFreqTerm == 0.0)
            dice = 0.0;
        else if ((avFreqTerm <= 1.0) & (termLength > 1)) {
            // we don't want to have dice == 0 when only 1 occurence, but at least a very small value
            dice = (Math.log10(1.1) * termLength) / avFreqComponent;
        } else {
            // get the default generalized DICE's coef.
            dice = (avFreqTerm * Math.log10((double) avFreqTerm) * termLength) / avFreqComponent;
        }

        // single word needs to be reduced (see [Park & al., 2002])
        if ((termLength == 1) && (avFreqTerm != 0.0)) {
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

    /*private uk.ac.man.entitytagger.matching.Matcher matcher = null;

    public List<Mention> processSpecies(String text, Language language) {
    	if (!language.getLang().equals("en"))
    		return null;
    	List<Mention> results = new ArrayList<>();

    	if (matcher == null) {
    		ArgParser ap = new ArgParser(new String[]{});
			//ap.addProperties("internal:/resources-linnaeus/properties.conf");
			ap.addProperties("data/species/properties.conf");
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
			//ap.addProperties("internal:/resources-linnaeus/properties.conf");
			ap.addProperties("data/species/properties.conf");
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
    }*/


    private static Pattern P2EOL = Pattern.compile("\\n\\s*\\n");

    /**
     * Divide a text into segments following a target segment size:
     * - try to segment into paragraphs first
     * - use sentence segmentation to then create segments following the target segment size
     * - if a sentence is still too large regarding the target segment size, apply a segmentation
     *   based on a punctuation mark
     * - if we still have a sentence as a too large monolithic block, we have some pathological 
     *   text -> arbitrary segment based on a delimiter  
     **/
    public List<OffsetPosition> segment(String text, List<Sentence> sentences, int targetSegmentSize, Language lang) {
        List<OffsetPosition> result = new ArrayList<>();
        OffsetPosition localPos = new OffsetPosition(0, text.length());
        result.add(localPos);

        if (text.length() < targetSegmentSize) {    
            return result;
        }

        // some segmentation is necessary 
        int target_number_segments = (text.length() / targetSegmentSize) + 1;
        //System.out.println("target number segments: " + target_number_segments);

        // if not available, prepare sentence segmentation
        if (sentences == null || sentences.size() == 0) {
            sentences = sentenceSegmentation(text, lang);
        }
        //System.out.println("nb sentences: " + sentences.size());

        while(result.size() < target_number_segments) {
            //System.out.println("start segmentation round with: " + result.size() + " segments");

            // select largest segment
            int i = 0;
            int largestSegmentSize = 0;
            int largestSegmentIndex = 0;
            for(OffsetPosition segment : result) {
                if (segment.end - segment.start > largestSegmentSize) {
                    largestSegmentSize = segment.end - segment.start;
                    largestSegmentIndex = i;
                }
                i++;
            }

            OffsetPosition largestSegment = result.get(largestSegmentIndex);
            //System.out.println("\ntext length: " + text.length());
            //System.out.println("largest segment: " + largestSegment.start + " : " + largestSegment.end);
            String localText = text.substring(largestSegment.start, largestSegment.end);
            List<Sentence> localSentences = new ArrayList<>();
            for (Sentence sentence : sentences) {
                if (largestSegment.start <= sentence.getOffsetStart() && sentence.getOffsetEnd() <= largestSegment.end) {
                    // add sentence with shifted offsets
                    Sentence shiftedSentence = new Sentence();
                    shiftedSentence.setOffsetStart(sentence.getOffsetStart()-largestSegment.start);
                    shiftedSentence.setOffsetEnd(sentence.getOffsetEnd()-largestSegment.start);
                    localSentences.add(shiftedSentence);
                }
            }
            //System.out.println("nb local sentences: " + localSentences.size());
            List<OffsetPosition> localResult = segmentOne(localText, localSentences, targetSegmentSize, lang);
            if (localResult == null) {
                // we can't segment further
                break;
            } else {
                // update offsets
                for(OffsetPosition newPos : localResult) {
                    newPos.start += largestSegment.start;
                    newPos.end += largestSegment.start;
                }

                // replace largestSegment by its segmented 2 parts
                result.set(largestSegmentIndex, localResult.get(0));
                if (result.size() > largestSegmentIndex+1)
                    result.add(largestSegmentIndex+1, localResult.get(1));
                else
                    result.add(localResult.get(1));
            }

            /*System.out.println("after segmentation: " + result.size() + " segments");
            for(OffsetPosition pos : result) {
                System.out.println("" + pos.start + ", " + pos.end);
                //System.out.println(text.substring(pos.start, pos.end));
            }*/
        }

        return result;
    }

    private List<OffsetPosition> segmentOne(String text, List<Sentence> sentences, int targetSegmentSize, Language lang) {

        // first try to segment based on double line
        Matcher matcher = P2EOL.matcher(text); 
        List<Integer> positionsStart = new ArrayList<>();
        List<Integer> positionsEnd = new ArrayList<>();
        while(matcher.find()) {
            positionsStart.add(matcher.start());
            positionsEnd.add(matcher.end());
        }

        int selectedPositionStart = -1;
        int selectedPositionEnd = -1;
        int midPosition = text.length() / 2;
        if (positionsStart.size() > 0) {
            //System.out.println("double EOL");
            // select the most central split point
            int i = 0;
            for(Integer position : positionsStart) {
                if (selectedPositionStart == -1) {
                    selectedPositionStart = position;
                    selectedPositionEnd = positionsEnd.get(i);
                }
                else if (Math.abs(position-midPosition)<Math.abs(selectedPositionStart-midPosition)) {
                    selectedPositionStart = position;
                    selectedPositionEnd = positionsEnd.get(i);
                }
                i++;
            }
        }

        if (selectedPositionStart == -1) {
            // try segmenting with the simple EOL
            //System.out.println("simple EOL");
            positionsStart = new ArrayList<>();
            int ind = 0;
            while(ind != -1) {
                ind = text.indexOf("\n", ind);
                if (ind != -1) {
                    positionsStart.add(ind);
                    ind += 1;
                }
            }

            if (positionsStart.size() > 0) {
                // select the most central split point
                int i = 0;
                for(Integer position : positionsStart) {
                    if (selectedPositionStart == -1) {
                        selectedPositionStart = position;
                    }
                    else if (Math.abs(position-midPosition)<Math.abs(selectedPositionStart-midPosition)) {
                        selectedPositionStart = position;
                    }
                    i++;
                }
            }
        }

        // if still not segmented enough, 
        if (selectedPositionStart == -1 && sentences != null && sentences.size()>0) {
            // try using sentence segmentation as segmentation points
            //System.out.println("segmentation via sentences");

            positionsStart = new ArrayList<>();
            positionsEnd = new ArrayList<>();
            int ind = 0;
            for(Sentence sentence : sentences) {
                positionsStart.add(sentence.getOffsetEnd());
                if (sentences.size() > ind+1)
                    positionsEnd.add(sentences.get(ind+1).getOffsetStart());
                else
                    positionsEnd.add(sentence.getOffsetEnd());
                ind++;
            }

            // remove last one
            if (positionsStart.size()>0) {
                positionsStart.remove(positionsStart.size()-1);
                positionsEnd.remove(positionsEnd.size()-1);
            }

            if (positionsStart.size() > 0) {
                // select the most central split point
                int i = 0;
                for(Integer position : positionsStart) {
                    if (selectedPositionStart == -1) {
                        selectedPositionStart = position;
                        selectedPositionEnd = positionsEnd.get(i);
                    }
                    else if (Math.abs(position-midPosition)<Math.abs(selectedPositionStart-midPosition)) {
                        selectedPositionStart = position;
                        selectedPositionEnd = positionsEnd.get(i);
                    }
                    i++;
                }
            }
        }

        // if still something too long, it means we have sentences extremely long
        // -> we do an arbitrary segmentation based on delimiters 
        // to avoid side effect for this pathological text
        if (selectedPositionStart == -1) {
            // we use simple space as splitter, to avoid being too much language dependent
            positionsStart = new ArrayList<>();
            int ind = 0;
            while(ind != -1) {
                ind = text.indexOf(" ", ind);
                if (ind != -1) {
                    positionsStart.add(ind);
                    ind += 1;
                }
            }

            if (positionsStart.size() > 0) {
                // select the most central split point
                int i = 0;
                for(Integer position : positionsStart) {
                    if (selectedPositionStart == -1) {
                        selectedPositionStart = position;
                    }
                    else if (Math.abs(position-midPosition)<Math.abs(selectedPositionStart-midPosition)) {
                        selectedPositionStart = position;
                    }
                    i++;
                }
            }
        }

        if (selectedPositionStart != -1) {
            List<OffsetPosition> newResult = new ArrayList<>();

            OffsetPosition localPos = new OffsetPosition(0, selectedPositionStart);
            newResult.add(localPos);

            if (selectedPositionEnd == -1)
                selectedPositionEnd = selectedPositionStart;
            OffsetPosition lastPos = new OffsetPosition(selectedPositionEnd, text.length());
            newResult.add(lastPos);

            return newResult;
        }
        return null;
    }


    // the following is for segmenting (in paragraphs) text as list of Layout tokens
    // this is not used for the moment

    public static int MINIMAL_PARAGRAPH_LENGTH = 100;
    public static int MAXIMAL_PARAGRAPH_LENGTH = 250;

    public static List<List<LayoutToken>> segmentInParagraphs(List<LayoutToken> tokens) {
        // heuristics: double end of line, if not simple end of line (not aligned with
        // previous line), and if still not we segment arbitrarly the monolithic block
        List<List<LayoutToken>> result = new ArrayList<>();
        result.add(tokens);

        // we recursively segment too large segments, starting with one unique segment
        // which is the whole text

        while (true) {
            result = subSsegmentInParagraphs(result);
            if (!containsTooLargeSegmentLayoutTokens(result))
                break;
        }

        return result;
    }

    private static boolean containsTooLargeSegmentLayoutTokens(List<List<LayoutToken>> segments) {
        for (List<LayoutToken> segment : segments) {
            if (segment.size() > MAXIMAL_PARAGRAPH_LENGTH) {
                return true;
            }
        }
        return false;
    }

    /*private static boolean containsTooLargeSegment(List<OffsetPosition> segments, int targetSegmentSize) {
        for (OffsetPosition segment : segments) {
            if (segment.end - segment.start > targetSegmentSize) {
                return true;
            }
        }
        return false;
    }*/

    private static List<List<LayoutToken>> subSsegmentInParagraphs(List<List<LayoutToken>> segments) {
        List<List<LayoutToken>> result = new ArrayList<>();

        for (List<LayoutToken> segment : segments) {
            if (segment.size() > MAXIMAL_PARAGRAPH_LENGTH) {
                // let's try to slice this guy
                boolean previousEOL = false;
                int n = 0; // current position in the segment
                List<LayoutToken> currentParagraph = new ArrayList<LayoutToken>();
                for (LayoutToken token : segment) {
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

        if (!containsTooLargeSegmentLayoutTokens(result))
            return result;

        // if we fail to to slice with double EOL, let's see if we can do something
        // with simple EOL
        segments = result;
        result = new ArrayList<>();
        for (List<LayoutToken> segment : segments) {
            if (segment.size() > MAXIMAL_PARAGRAPH_LENGTH) {
                // let's try to slice this guy
                int n = 0; // current position in the segment
                List<LayoutToken> currentParagraph = new ArrayList<LayoutToken>();
                for (LayoutToken token : segment) {
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

        if (!containsTooLargeSegmentLayoutTokens(result))
            return result;

        segments = result;
        result = new ArrayList<>();
        for (List<LayoutToken> segment : segments) {
            if (segment.size() > MAXIMAL_PARAGRAPH_LENGTH) {
                // if failure again, we arbitrarly segment
                int n = 0;
                List<LayoutToken> currentParagraph = new ArrayList<LayoutToken>();
                for (LayoutToken token : segment) {
                    currentParagraph.add(token);
                    if (n == MAXIMAL_PARAGRAPH_LENGTH - 1) {
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

    /**
     * Use TextUtilities.isAllUpperCaseOrDigitOrDot from grobid > 0.5.1
     * @return
     */
    @Deprecated
    public static boolean isAllUpperCaseOrDigitOrDot(String text) {
        for (int i = 0; i < text.length(); i++) {
            final char charAt = text.charAt(i);
            if (!Character.isUpperCase(charAt) && !Character.isDigit(charAt) && charAt != '.') {
                return false;
            }
        }
        return true;
    }

    // mention recognition methods
    public enum MentionMethod {
        wikipedia("wikipedia"),
        ner("ner"),
        wikidata("wikidata"),
        quantities("quantities"),
        grobid("grobid"),
        species("species"),
        user("user");

        private String name;

        MentionMethod(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}