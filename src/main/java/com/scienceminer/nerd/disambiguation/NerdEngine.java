package com.scienceminer.nerd.disambiguation;

import java.util.*;

import com.scienceminer.nerd.utilities.NerdConfig;
import com.scienceminer.nerd.utilities.StringProcessor;
import org.apache.commons.collections4.CollectionUtils;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.*;
import org.grobid.core.data.*;
import org.grobid.core.engines.EngineParsers;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lexicon.NERLexicon;
import org.grobid.core.lexicon.NERLexicon.NER_Type;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.db.WikipediaDomainMap;
import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.mention.*;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.embeddings.SimilarityScorer;
import com.scienceminer.nerd.features.GenericRankerFeatureVector;
import com.scienceminer.nerd.utilities.Utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;

import org.apache.commons.text.WordUtils;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 *
 * Main class to use for applying a disambiguation on a NerdQuery, the NerdQuery containing
 * all the input information (text, existing annotations, customization, languages) and the
 * parameters for the disambiguation.
 *
 */
public class NerdEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(NerdEngine.class);

	private static volatile NerdEngine instance = null;

	private EngineParsers parsers = null;

	private Map<String, LowerKnowledgeBase> wikipedias = null;
	private Map<String, NerdRanker> rankers = null;
	private Map<String, NerdSelector> selectors = null;
	private Relatedness relatedness = null;
	private Map<String, WikipediaDomainMap> wikipediaDomainMaps = null;

	private PruningService pruningService = null;

	static public int maxContextSize = 30;
	static public int maxLabelLength = 50;
	public static int MAX_SENSES = 4; // maximum level of ambiguity for an entity

	public static NerdEngine getInstance() {
	    if (instance == null) {
			getNewInstance();
	    }
	    return instance;
	}

	/**
	 * Creates a new instance.
	 */
	private static synchronized void getNewInstance() {
		LOGGER.debug("Get new instance of Engine");
		instance = new NerdEngine();
	}

	/**
	 * Hidden constructor
	 */
	private NerdEngine() {
		try {
			UpperKnowledgeBase.getInstance();
			parsers = new EngineParsers();
		} catch(Exception e) {
			throw new NerdResourceException("Error instanciating the (N)ERD knowledge base. ", e);
		}

		wikipedias = UpperKnowledgeBase.getInstance().getWikipediaConfs();
		try {
			relatedness = Relatedness.getInstance();
			rankers = new HashMap<>();
			selectors = new HashMap<>();
			wikipediaDomainMaps = UpperKnowledgeBase.getInstance().getWikipediaDomainMaps();
		} catch(Exception e) {
			throw new NerdResourceException("Error when opening the relatedness model", e);
		}

		pruningService = new PruningService();
	}

	/**
	 * Disambiguate a structured query and return the corresponding normalised
     * enriched and disambiguated query object.
	 *
	 * @param nerdQuery the POJO query object
	 * @return a response query object containing the structured representation of
	 *         the enriched and disambiguated query
	 */
	public List<NerdEntity> disambiguate(NerdQuery nerdQuery) {
		// Validation //TODO we should find a way to move this out of here.
		String text = nerdQuery.getText();
		String shortText = nerdQuery.getShortText();
		boolean shortTextVal = false;

		if (isEmpty(text) && isNotEmpty(shortText) ) {
			shortTextVal = true;
			text = shortText;
		}

		List<LayoutToken> tokens = nerdQuery.getTokens();
		if (isEmpty(text) && tokens != null) {
			// we might have an input as a list of LayoutToken
			text = LayoutTokensUtil.toText(tokens);
			shortTextVal = false;
		}

		//if the text is null, then better return the same entities provided in input,
		List<NerdEntity> entities = nerdQuery.getEntities();
		if (isEmpty(text) ) {
			LOGGER.info("The length of the text to be parsed is 0.");
			return entities;
		}

		// source language
		String lang = null;
		Language language = nerdQuery.getLanguage();
		if (language != null)
			lang = language.getLang();

		if (lang == null) {
			// the language recognition has not been done upstream of the call to this method, so
			// let's do it now
			LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
			try {
				language = languageUtilities.runLanguageId(text);
				nerdQuery.setLanguage(language);
				lang = language.getLang();
				LOGGER.debug(">> identified language: " + lang);
			}
			catch(Exception e) {
				LOGGER.debug("exception language identifier for: " + text,e);
			}
		}

		if (lang == null) {
			// default - it might be better to raise an exception?
			lang = Language.EN;
			LOGGER.warn("No language specified, defaulting to EN. ");
		}

		// get the LayoutToken ready if not already the case
		if (tokens == null) {
			tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, new Language(lang, 1.0));
		}

		NerdContext context = nerdQuery.getContext();
		if (context == null) {
			context = new NerdContext();
			nerdQuery.setContext(context);
		}

		/*for(NerdEntity entity : entities) {
		System.out.println("Surface: " + entity.getRawName() + " / normalised: " + entity.getNormalisedName());
		}*/

		Map<NerdEntity, List<NerdCandidate>> candidates = generateCandidatesSimple(entities, lang);
		//Map<NerdEntity, List<NerdCandidate>> candidates = generateCandidatesMultiple(entities, lang);

		/*for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			System.out.println("Surface: " + entity.getRawName() + " / normalised: " + entity.getNormalisedName());
			for(NerdCandidate cand : cands) {
				System.out.println("generated candidates: " + cand.toString());
			}
			System.out.println("--");
		}*/

		int nbEntities = 0;
		int nbCandidates = 0;
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			nbEntities += 1;
			if (cands != null)
				nbCandidates += cands.size();
		}
		LOGGER.debug("Total number of entities: " + nbEntities);
		LOGGER.debug("Total number of candidates: " + nbCandidates);
		LowerKnowledgeBase wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf(lang);

		// if needed, segment long text into either natural paragraph (if present) or arbitary ones
		/*List<List<LayoutToken>> subTokens = null;
		if (tokens.size() < ProcessText.MAXIMAL_PARAGRAPH_LENGTH) {
			subTokens = new ArrayList<>();
			subTokens.add(tokens);
		}
		else {
			// try to find "natural" paragraph segmentations
			subTokens = ProcessText.segmentInParagraphs(tokens);
		}

		for(List<LayoutToken> subToken : subTokens) {
			NerdContext localContext = rank(candidates, lang, context, shortTextVal, subToken);*/
		NerdContext localContext = rank(candidates, lang, context, shortTextVal, tokens);

		/*for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
		System.out.println("Surface: " + entity.getRawName());
		for(NerdCandidate cand : cands) {
			System.out.println("rank: " + cand.toString());
		}
		System.out.println("--");
		}*/

		// reimforce with document-level context if available
		/*if (context instanceof DocumentContext) {
			reimforce(candidates, (DocumentContext)context);
		}*/

		double minSelectorScore = wikipedia.getConfig().getMinSelectorScore();
		if (nerdQuery.getMinSelectorScore() != 0.0)
			minSelectorScore = nerdQuery.getMinSelectorScore();
		pruneWithSelector(candidates, lang, nerdQuery.getNbest(), shortTextVal, minSelectorScore, localContext, text);
		//}
		/*for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
		System.out.println("Surface: " + entity.getRawName());
		for(NerdCandidate cand : cands) {
			System.out.println("select: " + cand.toString());
		}
		System.out.println("--");
		}*/
				//prune(candidates, nerdQuery.getNbest(), shortTextVal, minRankerScore, lang);
		/*for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
		for(NerdCandidate cand : cands) {
			System.out.println(cand.toString());
		}
		}*/
				//impactOverlap(candidates);
		/*for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
		for(NerdCandidate cand : cands) {
			System.out.println(cand.toString());
		}
		}*/
				//if (!shortText && !nerdQuery.getNbest())
		//			prune(candidates, nerdQuery.getNbest(), shortTextVal, minRankerScore, lang);


		// reconciliate acronyms, i.e. ensure consistency of acronyms and expended forms in the complete
		// document
		if (context.getAcronyms() != null) {
			reconciliateAcronyms(nerdQuery);
		}

		WikipediaDomainMap wikipediaDomainMap = wikipediaDomainMaps.get(lang);
		List<NerdEntity> result = new ArrayList<>();
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();

			if (entity.getSource() == ProcessText.MentionMethod.user) {
				result.add(entity);
			} else if ( CollectionUtils.isEmpty(cands) ) {
				// default for class entity only
				if (entity.getType() != null) {
					entity.setNerdScore(entity.getNer_conf());
					result.add(entity);
				}
			} else {
				for(NerdCandidate candidate : cands) {
					NerdEntity nerdEntity = new NerdEntity(entity);
					nerdEntity.populateFromCandidate(candidate, lang);
					//nerdEntity.setWikipediaMultilingualRef(
					//	candidate.getWikiSense().getTranslations(), targetLanguages, wikipedias);

					// note: for the moment we use English categories via translingual information
					if (lang.equals(Language.EN)) {
						if (wikipediaDomainMap == null)
							LOGGER.warn("wikipediaDomainMap is null for en");
						else
							nerdEntity.setDomains(wikipediaDomainMap.getDomains(nerdEntity.getWikipediaExternalRef()));
					} else {
						// we get the English page id if available via the translations and then calculate the domain
						Map<String,String> translations = candidate.getWikiSense().getTranslations();
						String translationEN = translations.get(Language.EN);
						Article article = wikipedias.get(Language.EN).getArticleByTitle(translationEN);
						if (article != null) {
							nerdEntity.setDomains(wikipediaDomainMap.getDomains(article.getId()));
						}
					}
					result.add(nerdEntity);
					if (!nerdQuery.getNbest())
						break;
				}
			}
		}
		Collections.sort(result);

		/*for (NerdEntity entity : result) {
		System.out.println("Surface: " + entity.getRawName() + " - "+  entity.toString());
		System.out.println("--");
		}*/

		if(shortTextVal) {
            return result;
        }


        if (nerdQuery.getNbest()) {
            result = pruningService.pruneOverlapNBest(result, shortTextVal);
        } else {
            result = pruningService.pruneOverlap(result, shortTextVal);
        }

		// final pruning
		double minRankerScore = wikipedia.getConfig().getMinRankerScore();
		if (nerdQuery.getMinRankerScore() != 0.0)
			minRankerScore = nerdQuery.getMinRankerScore();

		if (!nerdQuery.getNbest())
			pruningService.prune(result, minRankerScore);

		return result;
	}


	public Map<NerdEntity, List<NerdCandidate>> generateCandidatesSimple(List<NerdEntity> entities, String lang) {
		Map<NerdEntity, List<NerdCandidate>> result = new TreeMap<>();
		LowerKnowledgeBase wikipedia = wikipedias.get(lang);

		if (wikipedia == null) {
			throw new NerdException("Wikipedia environment is not loaded for language " + lang);
		}
		if (entities == null)
			return result;

		NerdConfig conf = wikipedia.getConfig();

		for(NerdEntity entity : entities) {
			// if the entity is already input in the query (i.e. by the "user"), we do not generate candidates
			// for it if they are disambiguated

			if (entity.getSource() == ProcessText.MentionMethod.user
					&& entity.getNer_conf() == 1.0) {

				// do we have disambiguated entity information for the entity?
				// the assumption is that the validation has been already done upstream or we will have to maintain the
				// same logic everywhere.

				//Maybe a validation flag on the entity?

				//if (entity.getWikipediaExternalRef() != -1) {
				result.put(entity, null);
				continue;
				//}
			}

			List<NerdCandidate> candidates = new ArrayList<>();

			// if the mention is originally recognized as NE class MEASURE, we don't try to disambiguate it
			if (isNERClassExcludedFromDisambiguation(entity)) {
				result.put(entity, candidates);
				continue;
			}

			String normalisedString = entity.getNormalisedName();
			if (entity.getSource() == ProcessText.MentionMethod.species) {
				normalisedString = entity.getRawName();
			}

			if (isEmpty(normalisedString))
				continue;

			Label bestLabel = this.bestLabel(normalisedString, wikipedia);
			if (bestLabel !=null && !bestLabel.exists()) {
				//if (entity.getIsAcronym())
				//System.out.println("No concepts found for '" + normalisedString + "' " + " / " + entity.getRawName() );
				if (entity.getType() != null) {
					result.put(entity, candidates);
					continue;
				}
			}
			else {
//if (entity.getIsAcronym())
//System.out.println("Concept(s) found for '" + normalisedString + "' " + " / " + entity.getRawName() +
//" - " + bestLabel.getSenses().length + " senses");
				entity.setLinkProbability(bestLabel.getLinkProbability());
				boolean bestCaseContext = true;
				Label localBestLabel = new Label(wikipedia.getEnvironment(), normalisedString);
				if (!localBestLabel.exists()) {
					bestCaseContext = false;
				}
//System.out.println("LinkProbability for the string '" + normalisedString + "': " + entity.getLinkProbability());
				Label.Sense[] senses = bestLabel.getSenses();
				if ((senses != null) && (senses.length > 0)) {
					int s = 0;
					for(int i=0; i<senses.length; i++) {
						Label.Sense sense = senses[i];

						PageType pageType = sense.getType();
						if (pageType != PageType.article)
							continue;

						if (sense.getPriorProbability() < conf.getMinSenseProbability()
								&& sense.getPriorProbability() != 0.0 ) {
							// senses are sorted by prior prob.
							//continue;
							break;
						}

						// not a valid sense if title is a list of ...
						String title = sense.getTitle();
						if ((title == null) || title.startsWith("List of") || title.startsWith("Liste des"))
							continue;

						NerdCandidate candidate = new NerdCandidate(entity);

						boolean invalid = false;
//System.out.println("check categories for " + sense.getId());							
						com.scienceminer.nerd.kb.model.Category[] parentCategories = sense.getParentCategories();
						if ( (parentCategories != null) && (parentCategories.length > 0) ) {
							for(com.scienceminer.nerd.kb.model.Category theCategory : parentCategories) {
								// not a valid sense if a category of the sense contains "disambiguation" -> this is then a disambiguation page
								if (theCategory == null) {
									LOGGER.warn("Invalid category page for sense: " + title);
									continue;
								}
								if (theCategory.getTitle() == null) {
									LOGGER.warn("Invalid category content for sense: " + title);
									continue;
								}

								if (!NerdCategories.categoryToBefiltered(theCategory.getTitle()))
									candidate.addWikipediaCategories(new com.scienceminer.nerd.kb.Category(theCategory));
								if (theCategory.getTitle().toLowerCase().contains("disambiguation")) {
									invalid = true;
									break;
								}
							}
						}
						if (invalid)
							continue;

						candidate.setWikiSense(sense);
						candidate.setWikipediaExternalRef(sense.getId());
						if (sense.getPriorProbability() == 0.0)
							candidate.setProb_c(1.0);
						else
							candidate.setProb_c(sense.getPriorProbability());
						candidate.setPreferredTerm(sense.getTitle());
						candidate.setLang(lang);
						candidate.setLabel(bestLabel);
						candidate.setWikidataId(sense.getWikidataId());
						candidate.setBestCaseContext(bestCaseContext);
						candidates.add(candidate);
						//System.out.println(candidate.toString());
						s++;
						if (s == MAX_SENSES) {
							// max. sense alternative has been reach
							break;
						}
					}
				}

				if (candidates.size() > 0) {
					List<Label> bestLabels = this.bestLabels(normalisedString, wikipedia, lang);
					// check in alternative labels if we get for the same entity sense better statistical
					// information
					//System.out.println((bestLabels.size()-1) + " alternative labels...");
					for(int p=0; p<bestLabels.size(); p++) {
						Label altBestLabel = bestLabels.get(p);
						if (altBestLabel.getText().equals(bestLabel.getText()))
							continue;
						long countOcc = altBestLabel.getOccCount();
						long countLinkOcc = altBestLabel.getLinkOccCount();
						Label.Sense[] altSenses = altBestLabel.getSenses();
						if ((altSenses != null) && (altSenses.length > 0)) {
							for(int i=0; i<altSenses.length; i++) {
								Label.Sense sense = altSenses[i];
								long senseCountOcc = sense.getLinkOccCount();
								for(NerdCandidate candid : candidates) {
									if (sense.getId() == candid.getWikipediaExternalRef()) {
										// check statistics
										long candCountOcc = candid.getLabel().getOccCount();
										long candLinkCountOcc = candid.getLabel().getLinkOccCount();
										long candSenseCountOcc = candid.getWikiSense().getLinkOccCount();

										if (countOcc > candCountOcc) {
											//System.out.println("better label for same sense is: " + altBestLabel.getText() +
											//	", " + countOcc + " countOcc vs " + candCountOcc + " candCountOcc");

											// update candidate sense
											candid.setWikiSense(sense);
											candid.setLabel(altBestLabel);

											// update entity
											entity.setLinkProbability(altBestLabel.getLinkProbability());
										}
									}
								}
							}
						}
					}
				}

				if ( (candidates.size() > 0) || (entity.getType() != null) ) {
					Collections.sort(candidates);
					result.put(entity, candidates);
				} /*else
					System.out.println("No concepts found for '" + normalisedString + "' " + " / " + entity.getRawName() );*/
			}

		}

		//result = expendCoReference(entities, result);

		return result;
	}

	public Map<NerdEntity, List<NerdCandidate>> generateCandidatesMultiple(List<NerdEntity> entities, String lang) {
		Map<NerdEntity, List<NerdCandidate>> result = new TreeMap<>();
		LowerKnowledgeBase wikipedia = wikipedias.get(lang);
		if (wikipedia == null) {
			throw new NerdException("Wikipedia environment is not loaded for language " + lang);
		}
		if (entities == null)
			return result;

		NerdConfig conf = wikipedia.getConfig();

		for(NerdEntity entity : entities) {
			// if the entity is already inputted in the query (i.e. by the "user"), we do not generate candidates
			// for it if they are disambiguated
			if (entity.getSource() == ProcessText.MentionMethod.user) {
				// do we have disambiguated entity information for the entity?
				if (entity.getWikipediaExternalRef() != -1) {
					result.put(entity, null);
					continue;
				}
			}

			List<NerdCandidate> candidates = new ArrayList<NerdCandidate>();

			if (isNERClassExcludedFromDisambiguation(entity)) {
				result.put(entity, candidates);
				continue;
			}

			String normalisedString = entity.getNormalisedName();
			if (isEmpty(normalisedString))
				continue;

			List<Label> bestLabels = this.bestLabels(normalisedString, wikipedia, lang);
			//if (!bestLabel.exists()) {
			if (bestLabels == null || bestLabels.size() == 0) {
				//if (entity.getIsAcronym())
				//System.out.println("No concepts found for '" + normalisedString + "' " + " / " + entity.getRawName() );
				//if (strict)
				if (entity.getType() != null) {
					result.put(entity, candidates);
					continue;
				}
			}
			else {
				int s = 0;
				for(int p=0; p<bestLabels.size() && s==0; p++) {
					Label bestLabel = bestLabels.get(p);

					entity.setLinkProbability(bestLabel.getLinkProbability());
					//System.out.println("LinkProbability for the string '" + normalisedString + "': " + entity.getLinkProbability());
					Label.Sense[] senses = bestLabel.getSenses();
					if ((senses != null) && (senses.length > 0)) {
						for(int i=0; i<senses.length; i++) {
							Label.Sense sense = senses[i];

							PageType pageType = sense.getType();
							//System.out.println("pageType:" + pageType);
							if (pageType != PageType.article)
								continue;
							//System.out.println("prior prob:" + sense.getPriorProbability());
							if (sense.getPriorProbability() < conf.getMinSenseProbability()
									&& sense.getPriorProbability() != 0.0) {
								// senses are sorted by prior prob.
								//continue;
								break;
							}

							// not a valid sense if title is a list of ...
							String title = sense.getTitle();
							if ((title == null) || title.startsWith("List of") || title.startsWith("Liste des"))
								continue;

							NerdCandidate candidate = new NerdCandidate(entity);

							boolean invalid = false;
							//System.out.println("check categories for " + sense.getId());
							com.scienceminer.nerd.kb.model.Category[] parentCategories = sense.getParentCategories();
							if ( (parentCategories != null) && (parentCategories.length > 0) ) {
								for(com.scienceminer.nerd.kb.model.Category theCategory : parentCategories) {
									// not a valid sense if a category of the sense contains "disambiguation" -> this is then a disambiguation page
									if (theCategory == null) {
										LOGGER.warn("Invalid category page for sense: " + title);
										continue;
									}
									if (theCategory.getTitle() == null) {
										LOGGER.warn("Invalid category content for sense: " + title);
										continue;
									}

									if (!NerdCategories.categoryToBefiltered(theCategory.getTitle()))
										candidate.addWikipediaCategories(new com.scienceminer.nerd.kb.Category(theCategory));
									if (theCategory.getTitle().toLowerCase().indexOf("disambiguation") != -1) {
										invalid = true;
										break;
									}
								}
							}
							if (invalid)
								continue;

							candidate.setWikiSense(sense);
							candidate.setWikipediaExternalRef(sense.getId());
							if (sense.getPriorProbability() == 0.0)
								candidate.setProb_c(1.0/senses.length);
							else
								candidate.setProb_c(sense.getPriorProbability());
							candidate.setPreferredTerm(sense.getTitle());
							candidate.setLang(lang);
							candidate.setLabel(bestLabel);
							candidate.setWikidataId(sense.getWikidataId());

							// check if the entity is already present with another candidate (coming from an alternative label)
							// if yes, check if the current sense has better statistical information than the already present one
							long countOcc = bestLabel.getOccCount();
							long countLinkOcc = bestLabel.getLinkOccCount();
							boolean updateExistingCandidate = false;
							for(NerdCandidate candid : candidates) {
								if (sense.getId() == candid.getWikipediaExternalRef()) {
									// check statistics
									long candCountOcc = candid.getLabel().getOccCount();
									long candLinkCountOcc = candid.getLabel().getLinkOccCount();
									long candSenseCountOcc = candid.getWikiSense().getLinkOccCount();

									if (countOcc > candCountOcc) {
										//System.out.println("better label for same sense is: " + altBestLabel.getText() +
										//	", " + countOcc + " countOcc vs " + candCountOcc + " candCountOcc");

										// update candidate sense
										candid.setWikiSense(sense);
										candid.setLabel(bestLabel);

										// update entity
										entity.setLinkProbability(bestLabel.getLinkProbability());
									}
									updateExistingCandidate = true;
									break;
								}
							}

							// if the new candidate's entity is not already present, we add the candidate
							if (!updateExistingCandidate) {
								candidates.add(candidate);
								s++;
								if (s == MAX_SENSES) {
									// max. sense alternative has been reach
									break;
								}
							}
						}
					}
				}

				if (candidates.size() > 0 || entity.getType() != null) {
					Collections.sort(candidates);
					// we just take the best candidates following the conf. MAX_SENSES
					//int upperLimit = Math.min(candidates.size(), MAX_SENSES);
					//candidates = candidates.subList(0, upperLimit);
					result.put(entity, candidates);
				} /*else
					System.out.println("No concepts found for '" + normalisedString + "' " + " / " + entity.getRawName() );*/
			}

		}

		result = this.expendCoReference(entities, result);

		return result;
	}

	/**
	 * Heuristics for covering person name co-reference within a same candidate set.
	 *
	 * If a mention is a substring of another mentions which are either identified as a PERSON
	 * by the NER or whose most-likely candidate is an entity instance of (P31) human (Q5) or
	 * if identified as a species by the species mention identifier, then the candidates of the
	 * mention is merged with those of these another mentions.
	 *
	 * Note: TBD global context entities (e.g. from customization) must also be considered.
	 * TBD other named entity type might be relevant, e.g. company
	 *
	 */
	private Map<NerdEntity, List<NerdCandidate>> expendCoReference(List<NerdEntity> entities,
		Map<NerdEntity, List<NerdCandidate>> candidates) {
		if (entities == null)
			return candidates;

		Map<NerdEntity, List<NerdCandidate>> newCandidates = new TreeMap<>();
		Map<String, List<NerdCandidate>> cacheSubSequences = new HashMap<>();
		Map<String, Double> cacheLinkProbability = new HashMap<>();
		List<String> failures = new ArrayList<>();

		for(NerdEntity entity : entities) {
			// if the entity is already inputted in the query (i.e. by the "user"), we do not generate candidates
			// for it if they are disambiguated
			if (entity.getSource() == ProcessText.MentionMethod.user && entity.getNer_conf() == 1.0) {
				// do we have disambiguated entity information for the entity?
//				if (entity.getWikipediaExternalRef() != -1) {
//					continue;
//				}

				continue;
			}

			String entityString = entity.getRawName().toLowerCase();
			String entityNormalisedString = entity.getNormalisedName().toLowerCase();

			if ((entityString != null) && entityString.length() < 3)
				continue;
			if ((entityNormalisedString != null) && entityNormalisedString.length() < 3)
				continue;
			// avoiding the case "iii" subsequence of "Valentinian III" which appears from time to time
			if (entityNormalisedString.equals("iii"))
				continue;

			// if ner type is present and contradicting a "human" type (basically person)
			if ( (entity.getType() != null) &&
				 (entity.getType() != NER_Type.PERSON) &&
				 (entity.getType() != NER_Type.PERSON_TYPE) &&
				 (entity.getSource() != ProcessText.MentionMethod.species))
				continue;

			if (failures.contains(entityString))
				continue;

			List<NerdCandidate> cands = candidates.get(entity);

			if (cacheSubSequences.get(entityString) != null) {
				entity.setLinkProbability(cacheLinkProbability.get(entityString).doubleValue());
				List<NerdCandidate> otherCands = cacheSubSequences.get(entityString);
				if (cands == null) {
					// we deep copy the candidates of otherEntity for entity
					cands = new ArrayList<NerdCandidate>();
					for(NerdCandidate otherCand : otherCands) {
						NerdCandidate newCand = otherCand.copy(entity);
						newCand.setCoReference(true);
						cands.add(newCand);
					}
				} else {
					// merging of candidates
					List<Integer> listOfCandsId = new ArrayList<Integer>();
					for (NerdCandidate cand : cands) {
						listOfCandsId.add(new Integer(cand.getWikipediaExternalRef()));
					}
					for(NerdCandidate otherCand : otherCands) {
						if (!listOfCandsId.contains(new Integer(otherCand.getWikipediaExternalRef()))) {
							NerdCandidate newCand = otherCand.copy(entity);
							newCand.setCoReference(true);
							cands.add(newCand);
						}
					}
				}
				// add in temporary map
				newCandidates.put(entity, cands);
				continue;
			}

			int start = entity.getOffsetStart();
			int end = entity.getOffsetEnd();
			boolean success = false;

			// check if the mention is a (continous) subsequence of another entities
			for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
				List<NerdCandidate> otherCands = entry.getValue();
				NerdEntity otherEntity = entry.getKey();

				if (otherEntity.getRawName().length() <= entityString.length())
					continue;

				if (otherEntity.getRawName().toLowerCase().equals(entityString) ||
					otherEntity.getNormalisedName().toLowerCase().equals(entityNormalisedString) )
					continue;

				boolean isHuman = false;
				if (otherCands != null && otherCands.size() > 0) {
					NerdCandidate topCandidate = otherCands.get(0);
					String instanceOf = topCandidate.getWikidataP31Id();
					if (instanceOf != null && instanceOf.equals("Q5"))
						isHuman = true;
				}

				// entities cannot overlap...
				if ( (otherEntity.getOffsetStart()<=end && otherEntity.getOffsetEnd()>=end) ||
 					 (otherEntity.getOffsetStart()<=start && otherEntity.getOffsetEnd()>=start) )
					continue;

				// check NER type / top prior P31 property
				if ( (otherEntity.getType() != NERLexicon.NER_Type.PERSON) && !isHuman) {
					continue;
				}

				if (NerdEntity.subSequence(entity, otherEntity, false)) {
//System.out.println(entityString + " is subsequence of " + otherEntity.getRawName() + " -> merging candidates...");
					if (otherEntity.getLinkProbability() > entity.getLinkProbability())
						entity.setLinkProbability(otherEntity.getLinkProbability());
					// we merge the candidates of otherEntity in those of entity
					if (cands == null) {
						// we deep copy the candidates of otherEntity for entity
						cands = new ArrayList<NerdCandidate>();
						for(NerdCandidate otherCand : otherCands) {
							NerdCandidate newCand = otherCand.copy(entity);
							newCand.setCoReference(true);
							cands.add(newCand);
						}
					} else {
						// merging of candidates
						List<Integer> listOfCandsId = new ArrayList<Integer>();
						for (NerdCandidate cand : cands) {
							listOfCandsId.add(new Integer(cand.getWikipediaExternalRef()));
						}
						for(NerdCandidate otherCand : otherCands) {
							if (!listOfCandsId.contains(new Integer(otherCand.getWikipediaExternalRef()))) {
								NerdCandidate newCand = otherCand.copy(entity);
								newCand.setCoReference(true);
								cands.add(newCand);
							}
						}
					}
					// add in temporary map
					newCandidates.put(entity, cands);
					cacheSubSequences.put(entityString, otherCands);
					cacheLinkProbability.put(entityString, new Double(otherEntity.getLinkProbability()));
					success= true;
					break;
				}
			}

			if (!success) {
				failures.add(entityString);
			}
		}

		// update of candidates
		for(Map.Entry<NerdEntity, List<NerdCandidate>> entry : newCandidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			if (candidates.get(entity) == null)
				candidates.put(entity, cands);
			else
				candidates.replace(entity, cands);
		}

		// final default sort candidates against priors (prob_c)
		for(Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			Collections.sort(cands);
		}

		return candidates;
	}

	/**
	 * Ranking of the candidates passed in a map as parameter for a set of mentions based on an external context.
	 *
	 * @return the context enriched with the internal context considered when ranking.
	 *
	 */
	public NerdContext rank(Map<NerdEntity, List<NerdCandidate>> candidates, String lang,
		NerdContext context, boolean shortText, List<LayoutToken> tokens) {
		// we rank candidates for each entity mention
		//relatedness.resetCache(lang);

		// first pass to get the "certain" entities
		List<NerdEntity> userEntities = new ArrayList<>();
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			NerdEntity entity = entry.getKey();
			if (entity.getSource() == ProcessText.MentionMethod.user) {
				userEntities.add(entity);
			}
		}

		// we create/augment the context for the disambiguation
		NerdContext localContext = null;
		try {
			 localContext = relatedness.getContext(candidates, userEntities, lang, shortText);
//System.out.println("size of local context: " + localContext.getSenseNumber());
//System.out.println(localContext.toString());
			 // merge context
			 if (context != null) {
			 	context.merge(localContext);
			 }
		} catch(Exception e) {
			LOGGER.error("Error while merging context for the disambiguation.", e);
		}

		NerdRanker ranker = rankers.get(lang);
		ranker = instantiateRankerIfNull(lang, ranker);

		GenericRankerFeatureVector feature = ranker.getNewFeature();

		double quality = 0.0;
		if (localContext != null) {
			if (feature.Add_context_quality) {
				// computed only if required
				quality = localContext.getQuality();
			}
		}

		// second pass for producing the ranking score
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();

			if (cands == null)
				continue;

			// get a window of layout tokens around without target tokens
			List<LayoutToken> subTokens = com.scienceminer.nerd.utilities.Utilities.getWindow(entity, tokens,
				NerdRanker.EMBEDDINGS_WINDOW_SIZE, lang);

//			LOGGER.debug("Mention: " + entry.toString());
			for(NerdCandidate candidate : cands) {
//			    LOGGER.debug("Candidate: " +  candidate.toString());
				double score = 0.0;
				try {
					double commonness = candidate.getProb_c();

					double related = 0.0;
					// computed only if needed
					if (feature.Add_relatedness) {
						related = relatedness.getRelatednessTo(candidate, localContext, lang);
						candidate.setRelatednessScore(related);
					}

					boolean bestCaseContext = candidate.getBestCaseContext();
					/*if (!entity.getNormalisedName().equals(bestLabel.getText())) {
						bestCaseContext = false;
					}*/

					float embeddingsSimilarity = 0.0F;
					// computed only if needed
					if (feature.Add_embeddings_centroid_similarity) {
						embeddingsSimilarity = SimilarityScorer.getInstance().getCentroidScore(candidate, subTokens, lang);
					}

					if (ranker == null) {
						LOGGER.error("Cannot rank candidates: disambiguator for the language " +
							lang + " is invalid");
					}

					String wikidataId = "Q0"; // undefined entity
					//if (candidate.getWikidataId() != null)
					//	wikidataId = candidate.getWikidataId();

					String wikidataP31Id = "Q0"; // undefined entity
					//if (candidate.getWikidataP31Id() != null)
					//	wikidataP31Id = candidate.getWikidataP31Id();

					score = ranker.getProbability(commonness, related, quality,
						bestCaseContext, embeddingsSimilarity, wikidataId, wikidataP31Id);

					/*System.out.println("RANKER - " + candidate.getWikidataId() + " = " + entity.getRawName() + " -> commonness: " + commonness + 
						", related: " + related + ", quality: " + quality + 
						", bestCaseContext: " + bestCaseContext + ", embeddingsSimilarity: " + embeddingsSimilarity + " = " + score);*/

					//System.out.println(entity.getRawName() + " -> " + candidate.getWikiSense().getTitle() + "(candidate) " + score + "(ranker/nerd score) " +  " " + entity.toString());
					//System.out.println("\t\t" + "commonness: " + commonness + ", relatedness: " + related + ", embeddingsSimilarity: " + embeddingsSimilarity);
				}
				catch(Exception e) {
					LOGGER.debug("Fail to compute ranker score.", e);
				}

				candidate.setNerdScore(score);
			}
			Collections.sort(cands);
		}

		//System.out.println("relatedness - Comparisons requested: " + relatedness.getComparisonsRequested());
		LOGGER.debug("relatedness - comparisons: " + relatedness.getComparisonsCalculated()
		+ " - cache proportion: " + relatedness.getCachedProportion());

		return localContext;
	}

	private NerdRanker instantiateRankerIfNull(String lang, NerdRanker ranker) {
		if (ranker == null) {
			LowerKnowledgeBase wikipedia = wikipedias.get(lang);
			try {
				ranker = new NerdRanker(wikipedia);
				rankers.put(lang, ranker);
			}
			catch(Exception e) {
				LOGGER.error("Cannot load ranker for language " + lang, e);
			}
		}

		return ranker;
	}

	/**
	 * Ranking of candidates for a term rawTerm in a vector of weighted terms.
	 * Optionally a contextual text is given, where the terms of the vector might occur (or not).
	 */
	private void rank(List<NerdCandidate> candidates, String rawTerm, List<WeightedTerm> terms,
					  String text, String lang, NerdContext context, List<NerdEntity> userEntities) {
	    if ( (candidates == null) || (candidates.size() == 0) )
			return;

		// get the disambiguator for this language
		NerdRanker disambiguator = rankers.get(lang);
		disambiguator = instantiateRankerIfNull(lang, disambiguator);

		// for the embeddings similarity we need a textual context as a list of LayoutToken
		List<LayoutToken> tokens = new ArrayList<LayoutToken>();
		for(WeightedTerm term : terms) {
			tokens.add(new LayoutToken(term.getTerm()));
		}

		// if we have extra textual information, we can try to get the different local contexts
		List<NerdContext> localContexts = null;
		if ( (text != null) && (text.length() > 0) ) {
			List<String> localContextStrings = buildLocalContexts(rawTerm, text);
			// build the corresponding contexts
			for(String localContextString : localContextStrings) {
                NerdContext contextObject = relatedness.getContextFromText(localContextString, userEntities, lang);
                if (localContexts == null)
                    localContexts = new ArrayList<NerdContext>();
                localContexts.add(contextObject);

				List<LayoutToken> subTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(localContextString);
				for(LayoutToken token : subTokens)
					tokens.add(token);
			}
		}

		GenericRankerFeatureVector feature = disambiguator.getNewFeature();
		double quality = 0.0;
		if (feature.Add_context_quality) {
			// calculated if needed
			quality = context.getQuality();
		}
		// second pass for producing the ranking score
		for(NerdCandidate candidate : candidates) {
			double score = 0.0;
			try {
				double commonness = candidate.getProb_c();

				boolean bestCaseContext = candidate.getBestCaseContext();
				// actual label used
				/*Label bestLabel = candidate.getLabel();
				if (!rawTerm.equals(bestLabel.getText())) {
					bestCaseContext = false;
				}*/

				float embeddingsSimilarity = 0.0F;
				// computed only of needed

				if (feature.Add_embeddings_centroid_similarity) {
					//embeddingsSimilarity = SimilarityScorer.getInstance().getLRScore(candidate, tokens, lang);
					embeddingsSimilarity = SimilarityScorer.getInstance().getCentroidScore(candidate, tokens, lang);
				}

				// for the candidate
				double related = 0.0;
				// computed only if needed
				if (feature.Add_relatedness)
					related = relatedness.getRelatednessTo(candidate, context, lang);

				String wikidataId = "Q0"; // undefined entity
				if (candidate.getWikidataId() != null)
					wikidataId = candidate.getWikidataId();

				String wikidataP31Id = "Q0"; // undefined entity
				if (candidate.getWikidataP31Id() != null)
					wikidataP31Id = candidate.getWikidataP31Id();


				if (localContexts == null) {
					score = disambiguator.getProbability(commonness, related, quality,
						bestCaseContext, embeddingsSimilarity, wikidataId, wikidataP31Id);
				}
				else {
					// we disambiguate for each local context
					score = disambiguator.getProbability(commonness, related, quality,
						bestCaseContext, embeddingsSimilarity, wikidataId, wikidataP31Id);
					for(NerdContext localContext : localContexts) {
						if (feature.Add_relatedness)
							related = relatedness.getRelatednessTo(candidate, localContext, lang);
						double localQuality = 0.0;
						if (feature.Add_context_quality) {
							localQuality = localContext.getQuality();
						}
						score += disambiguator.getProbability(commonness, related, localQuality,
							bestCaseContext, embeddingsSimilarity, wikidataId, wikidataP31Id);
						//double localScore = disambiguator.getProbability(commonness, related, localContext);
						//if (localScore > score)
						//	score = localScore;
					}
					score = score / (localContexts.size() + 1);
				}

				//System.out.println(candidate.getWikiSense().getTitle() + " " + candidate.getNerdScore() +  " " + entity.toString());
				//System.out.println("\t\t" + "commonness: " + commonness + ", relatedness: " + related);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			candidate.setNerdScore(score);
		}
		Collections.sort(candidates);
	}

	private List<String> buildLocalContexts(String rawTerm, String text) {
		List<String> localContexts = new ArrayList<String>();
		int ind = text.indexOf(rawTerm);
//System.out.println("\n" + rawTerm + " - ");		
		while( (ind != -1) && (localContexts.size() < maxContextSize) ) {
			// we extract the local context from the text
			int start = ind - 200;
			if (start < 0)
				start = 0;
			else {
				// adjust the start to the beginning of a token
				while( (start>0) && (text.charAt(start) != ' ') ) {
					start--;
				}
			}
			int end = ind + 200;
			if (end >= text.length())
				end = text.length() - 1;
			else {
				// adjust the end to the end of a token
				while( (end<=text.length()) && (text.charAt(end) != ' ') ) {
					end++;
				}
			}
			String localContext = text.substring(start, end);
			localContexts.add(localContext);
//System.out.println(localContext);
			ind = text.indexOf(rawTerm, ind+201);
		}
		return localContexts;
	}

	/**
	 * 	We prioritize the longest term match from the KB : the term coming from the KB shorter than
     *  the longest match from the KB is pruned. For equal mention arity, nerd confidence score is used.
	 *  Note that the longest match heuristics is debatable and should be further experimentally
	 *  validated...
	 *
	 *  @Deprecated: please use PruningService.pruneOverlap()
	 */
	@Deprecated
	public List<NerdEntity> pruneOverlap(List<NerdEntity> entities, boolean shortText) {
//System.out.println("pruning overlaps - we have " + entities.size() + " entities");
		List<Integer> toRemove = new ArrayList<Integer>();
		for (int pos1=0; pos1<entities.size(); pos1++) {
			if (toRemove.contains(new Integer(pos1)))
				continue;
			NerdEntity entity1 = entities.get(pos1);

			if (entity1.getRawName() == null)  {
				if (!toRemove.contains(new Integer(pos1))) {
					toRemove.add(new Integer(pos1));
				}
//System.out.println("Removing " + pos1 + " - " + entity1.getNormalisedName());
				continue;
			}

			// the arity measure below does not need to be precise
			int arity1 = entity1.getNormalisedName().length() - entity1.getNormalisedName().replaceAll("\\s", "").length() + 1;
//System.out.println("Position1 " + pos1 + " / arity1 : " + entity1.getNormalisedName() + ": " + arity1);

			// find all sub term of this entity and entirely or partially overlapping entities
			for (int pos2=0; pos2<entities.size(); pos2++) {
				if (pos1 == pos2)
					continue;

				NerdEntity entity2 = entities.get(pos2);
				if (entity2.getOffsetEnd() < entity1.getOffsetStart())
					continue;

				if (entity1.getOffsetEnd() < entity2.getOffsetStart())
					continue;

				if (toRemove.contains(new Integer(pos2)))
					continue;
				
				/*if ( 
					( (entity2.getOffsetStart() >= entity1.getOffsetStart()) &&
						(entity2.getOffsetStart() < entity1.getOffsetEnd()) ) 
					||  
					( (entity1.getOffsetStart() >= entity2.getOffsetStart()) &&
						(entity1.getOffsetStart() < entity2.getOffsetEnd()) ) 
				   )*/
				{

//System.out.println("Position2 " + pos2 + " / overlap: " + entity1.toJson() + " /////////////// " + entity2.toJson());
					// overlap
					//int arity2 = entity2.getOffsetEnd() - entity2.getOffsetStart();
					if (entity2.getRawName() == null) {
						if (!toRemove.contains(new Integer(pos2))) {
							toRemove.add(new Integer(pos2));
						}
//System.out.println("Removing " + pos2 + " - " + entity2.getRawNormalisedName());
						continue;
					}

					if ((entity2.getType() != null) && (entity2.getWikipediaExternalRef() == -1)) {
						// we have a NER not disambiguated
						// check if the other entity has been disambiguated
						if ( (entity1.getWikipediaExternalRef() != -1) && (entity1.getNerdScore() > 0.2) ) {
							if (!toRemove.contains(new Integer(pos2))) {
								toRemove.add(new Integer(pos2));
							}
//System.out.println("Removing " + pos2 + " - " + entity2.getNormalisedName());
							continue;
						}
					}

					if ((entity1.getType() != null) && (entity1.getWikipediaExternalRef() == -1)) {
						// we have a NER not disambiguated
						// check if the other entity has been disambiguated
						if ( (entity2.getWikipediaExternalRef() != -1) && (entity2.getNerdScore() > 0.2) ) {
							if (!toRemove.contains(new Integer(pos1))) {
								toRemove.add(new Integer(pos1));
							}
//System.out.println("Removing " + pos1 + " - " + entity1.getNormalisedName());
							break;
						}
					}

					/*if ((entity1.getType() != null) && (entity1.getWikipediaExternalRef() == -1) &&
						 (entity2.getWikipediaExternalRef() != -1)) {
						// we don't apply arity based pruning
						continue;
					}*/

					if (entity1.getWikipediaExternalRef() == entity2.getWikipediaExternalRef()) {
						if ( (entity1.getType() != null) && (entity2.getType() == null) ) {
							if (!toRemove.contains(new Integer(pos2)))
								toRemove.add(new Integer(pos2));
//System.out.println("Removing " + pos2 + " - " + entity2.getNormalisedName());
							continue;
						}
					}

					int arity2 = entity2.getNormalisedName().length() - entity2.getNormalisedName().replaceAll("\\s", "").length() + 1;
//System.out.println("arity2 : " + entity2.getNormalisedName() + ": " + arity2);
					if (arity2 < arity1) {
						// longest match wins
						if (!toRemove.contains(new Integer(pos2)))
							toRemove.add(new Integer(pos2));
//System.out.println("Removing " + pos2 + " - " + entity2.getNormalisedName());
						continue;
					}
					else if (arity2 == arity1) {
						// we check the nerd scores of the top candiate for the two entities
						double conf1 = entity1.getNerdScore();
						double conf2 = entity2.getNerdScore();
						//double conf1 = entity1.getSelectionScore();
						//double conf2 = entity2.getSelectionScore();
						if (conf2 < conf1) {
							if (!toRemove.contains(new Integer(pos2))) {
								toRemove.add(new Integer(pos2));
							}
//System.out.println("Removing " + pos2 + " - " + entity2.getNormalisedName());
							continue;
						} /*else {
							// if equal we check the selection scores of the top candiate for the two entities
							conf1 = entity1.getSelectionScore();
							conf2 = entity2.getSelectionScore();
							if (conf2 < conf1) {
								if (!toRemove.contains(new Integer(pos2))) {
									toRemove.add(new Integer(pos2));
								}
							} else {
								// if still equal we check the prob_c 
								conf1 = entity1.getProb_c();
								conf2 = entity2.getProb_c();
								if (conf2 < conf1) {
									if (!toRemove.contains(new Integer(pos2))) {
										toRemove.add(new Integer(pos2));
									}
								} else {
									// too uncertain we remove all
									if (!toRemove.contains(new Integer(pos2))) {
										toRemove.add(new Integer(pos2));
									}
									if (!toRemove.contains(new Integer(pos1))) {
										toRemove.add(new Integer(pos1));
									}
								}
							}
						}*/
					}
				}
			 }
		}

		List<NerdEntity> newEntities = new ArrayList<NerdEntity>();
		for(int i=0; i<entities.size(); i++) {
			if (!toRemove.contains(new Integer(i))) {
				newEntities.add(entities.get(i));
			} else {
				if (shortText) {
					// in case of short text we simply reduce the score of the entity but we don't remove it
					entities.get(i).setNerdScore(entities.get(i).getNerdScore() / 2);
					newEntities.add(entities.get(i));
				}
			}
		}

		return newEntities;
	}

	public List<NerdCandidate> annotateShort(String runID, String textID, String text) {
		//System.out.println("runID=" + runID + " textID="+textID+ " text="+text);

		long start = System.currentTimeMillis();
		LOGGER.debug(">> set ERD short text for stateless service: " + text);

		//List<ErdAnnotationShort> annotations = new ArrayList<ErdAnnotationShort>();
		List<NerdCandidate> concept_terms = null;//generateCandidates(text, true);

		if (CollectionUtils.isNotEmpty(concept_terms) ) {
			try {
				// we rank the entity candidates
				// second parameter is the method
				//concept_terms = rank(concept_terms, 0);

				// finally we apply a decision on the validity of entities
				// second parameter is the n-best value, third parameter is the method
				//concept_terms = select(concept_terms, 1, 1, text);

				// final prune
				//concept_terms = prune(concept_terms, true); // strict

				if ( (concept_terms != null) && (concept_terms.size() != 0) ) {
					// create annotations in the ERD 2014 expected format
					int interpretation = 0;
					int begin = text.length();
					boolean first = true;
					int end = 0;
					for(NerdCandidate term : concept_terms) {
						int local_begin = -1;
						int local_end = -1;
						if (term.getEntity() != null) {
							// normally the entity associated to a disambiguation candidate NerdCandidate
							// is never null
							local_begin = term.getEntity().getOffsetStart();
							local_end = term.getEntity().getOffsetEnd();

							if ( (local_begin == 0) && (local_end == text.length())) {
								if (!first) {
									interpretation++;
								}
								else
									first = false;
							}
							else if ( (local_begin < end) && (local_end > begin) ) {
								if (!first) {
									interpretation++;
								}
								else
									first = false;
							}
						}
						
						/*ErdAnnotationShort a = new ErdAnnotationShort();
						a.setQid(textID);
						a.setInterpretationSet(interpretation);
						a.setPrimaryId(term.getFreeBaseExternalRef());
						a.setMentionText(term.getRawString());
						a.setScore(term.getSelectionScore());
						//a.setScore(term.getRelatednessScore());
						annotations.add(a);
						*/
						if (term.getEntity() != null) {
							if (local_end > end) {
								end = local_end;
							}
							if (local_begin < begin) {
								begin = local_begin;
							}
						}
						first = false;
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
			}
		}

		long end = System.currentTimeMillis();
		System.out.println((end - start) + " milliseconds");

		return concept_terms;
	}

	/**
	 * 	We merge candidates corresponding to the same chunks and referring to the same concept (because of
	 *  redirection) by taking the highest probability and summing the number of occurences. We merge NER
	 *  type and entity candidate when compatible.
	 */
	/*public List<NerdCandidate> merge(List<NerdCandidate> terms, 
									List<NerdCandidate> senses, 
									List<NerdCandidate> mentions) {
System.out.println("Merging...");
//System.out.println(terms.toString());		
		if ( (terms == null) || (terms.size() == 0) ) 
			return null;

		List<Integer> toRemove = new ArrayList<Integer>();
		for(int i=0; i<terms.size(); i++) {
			if (toRemove.contains(new Integer(i))) {
				// already merged
				continue;
			}
			NerdCandidate term1 = terms.get(i);
			if (term1.getMethod() == NerdCandidate.NERD) {
				String surface1 = term1.getEntity().getNormalisedName();
				
				if (term1.getFreeBaseExternalRef() == null) {
					continue;
				}
				
				// we check other candidates on the same text chunk 
				for(int j=0; j<terms.size(); j++) {
					if (j == i)
						continue;
					
					if (toRemove.contains(new Integer(j))) {
						// already merged
						continue;
					}
										
					NerdCandidate term2 = terms.get(j);
					if (term2.getMethod() == NerdCandidate.NERD) {
						String surface2 = term2.getEntity().getNormalisedName();
						if (term2.getFreeBaseExternalRef() != null) {
							if (surface2.equals(surface1) && 
								(term2.getFreeBaseExternalRef().equals(term1.getFreeBaseExternalRef())) ) {
								if (term2.getProb_c() > term1.getProb_c())
									term1.setProb_c(term2.getProb_c()); 
								term1.setFreq(term1.getFreq() + term2.getFreq());
								if (term2.getProb_i() > term1.getProb_i())
									term1.setProb_i(term2.getProb_i());
								if (term2.getFreebaseTypes() != null) {
									for(String type : term2.getFreebaseTypes()) {
										term1.addFreebaseType(type);
									}
								}	
								toRemove.add(new Integer(j));
							}
						}
					}
				}
			}
		}
		
		List<NerdCandidate> result = new ArrayList<NerdCandidate>();
		for(int i=0; i<terms.size(); i++) {
			if (toRemove.contains(new Integer(i))) {
				continue;
			}
			else {
				result.add(terms.get(i));
			}
		}
		
		return result;
	}*/

	/**
	 *  Pruning for vector of terms.
	 * 	We prune following the longest term match from the KB : the term coming from the KB shorter than
     *  the longest match from the KB and which have not been merged, are removed.
	 */
	public List<NerdCandidate> prune(List<NerdCandidate> candidates, boolean strict, double threshold) {
//System.out.println("Prunning...");
//System.out.println(terms.toString());

		if ( (candidates == null) || (candidates.size() == 0) )
			return null;

		List<Integer> toRemove = new ArrayList<Integer>();
		for(int i=0; i<candidates.size(); i++) {
			NerdCandidate term1 = candidates.get(i);

			if (term1.isSubTerm) {
				continue;
			}

			if (term1.getMethod() == NerdCandidate.NERD) {
				String surface1 = term1.getEntity().getNormalisedName();

				// we check if the raw string is a substring of another NerdCandidate from the ERD method
				for(int j=0; j<candidates.size(); j++) {
					if (j == i)
						continue;

					/*if (toRemove.contains(new Integer(j))) {
						// already pruned
						continue;
					}*/

					NerdCandidate term2 = candidates.get(j);

					if (term2.getFreeBaseExternalRef() == null) {
						//continue;
						// should the entity not covered by the NER FreeBase snapshot be used for pruning?
					}

					// if we are not pruning strictly,
					// if the probability and frequency of the term to be used for prunning are too low
					// as compared to the term to be possibly pruned, we ignore it
					if (!strict) {
						double diff_prob = 0.0;
						int diff_freq = 0;
						if (term2.getProb_i() < term1.getProb_i())
							diff_prob = term1.getProb_i() - term2.getProb_i();
						if (term2.getFreq() < term1.getFreq())
							diff_freq = term1.getFreq() - term2.getFreq();

					/*	if (term1.getFreq() < 50000) {
							if ((term1.getEntityType() == null) ||
								!term1.getEntityType().equals(term2.getEntityType())) {*/

								if (diff_prob > 0.99)
								 	continue;

								if (diff_freq > 5000)
									continue;

							//}
						//}
					}
					String surface2 = term2.getEntity().getNormalisedName();
					if ((surface2.length() > surface1.length()) && (surface2.contains(surface1))) {
						toRemove.add(i);
						break;
					}
				}
			}
		}

		List<NerdCandidate> result = new ArrayList<>();
		for(int i=0; i<candidates.size(); i++) {
			if (toRemove.contains(i)) {
				continue;
			} else if (candidates.get(i).getNerdScore() > threshold) {
				result.add(candidates.get(i));
			}
		}

		return result;
	}

	/**
	 * 	Pruning using a the selector model scores.
	 */
	public void pruneWithSelector(Map<NerdEntity, List<NerdCandidate>> cands,
			String lang,
			boolean nbest,
			boolean shortText,
			double threshold,
			NerdContext context,
			String text) {
		LowerKnowledgeBase wikipedia = wikipedias.get(lang);
		if(wikipedia == null) {
			LOGGER.warn("Cannot find a selector for the language " + lang + ". Skipping the pruning.");
			return;
		}

		NerdSelector selector = selectors.get(lang);
		if (selector == null) {
			selector = new NerdSelector(wikipedia);
			selectors.put(lang, selector);
		}

		NerdConfig conf = wikipedia.getConfig();

		List<NerdEntity> toRemove = new ArrayList<>();
		GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();

		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : cands.entrySet()) {
			List<NerdCandidate> candidates = entry.getValue();
			if ( (candidates == null) || (candidates.size() == 0) )
				continue;
			NerdEntity entity = entry.getKey();

			if (entity.getSource() == ProcessText.MentionMethod.species) {
				// don't prune anything
				continue;
			}
			List<String> words = analyzer.tokenize(entity.getRawName(),
					new Language(wikipedia.getConfig().getLangCode(), 1.0));

			double dice = ProcessText.getDICECoefficient(entity.getNormalisedName(), lang);

			boolean isNe = entity.getType() != null;
			for(NerdCandidate candidate : candidates) {
				//if (candidate.getMethod() == NerdCandidate.NERD)
				//{
				try {
					double tf = Utilities.getOccCount(candidate.getLabel().getText(), text);
					double idf = ((double)wikipedia.getArticleCount()) / candidate.getLabel().getDocCount();
					double prob = selector.getProbability(candidate.getNerdScore(),
						candidate.getLabel().getLinkProbability(),
						candidate.getWikiSense().getPriorProbability(),
						words.size(),
						candidate.getRelatednessScore(),
						context.contains(candidate),
						isNe,
						tf*idf,
						dice);

					/*System.out.println("SELECTOR - " + candidate.getWikidataId() + " = " + entity.getRawName() + " -> nerdScore: " + candidate.getNerdScore() + 
						", linkProbability: " + candidate.getLabel().getLinkProbability() + 
						", priorProbability(): " + candidate.getWikiSense().getPriorProbability() + 
						", size: " + words.size() + ", relatedness: " + candidate.getRelatednessScore() + 
						", context: " + context.contains(candidate) + 
						", isNe: " + isNe + ", tf*idf: " + tf*idf + ", dice: " + dice + " = " + prob);*/

					candidate.setSelectionScore(prob);
				} catch(Exception e) {
					e.printStackTrace();
				}
				//}
			}

/*System.out.println("Surface: " + entity.getRawName());	
for(NerdCandidate cand : candidates) {
	System.out.println("select: " + cand.toString());
}
System.out.println("--");*/


			List<NerdCandidate> newCandidates = new ArrayList<>();
			for(NerdCandidate candidate : candidates) {
				if (candidate.getSelectionScore() < conf.getMinSenseProbability() && shortText) {
					continue;
				} else if (candidate.getSelectionScore() < threshold && !shortText) {
					continue;
				} else {
					newCandidates.add(candidate);
				}

				// variant: we don't prune named entities the same way
				/*if ( (candidate.getSelectionScore() < threshold) && (entity.getType() == null) ) {
					continue;
				} else if ( (candidate.getSelectionScore() < (threshold/2)) && (entity.getType() != null) ) {
					continue;
				} else {
					newCandidates.add(candidate);
				}*/
			}

			// variant: we don't prune top-relatedness candidate if a lower candidate for the same
			// mention remains - the goal is to avoid the selector reversing a ranker decision
			// because ranker is more accurate than selector
			/*NerdCandidate topCandidate = candidates.get(0);
			if ( (newCandidates.size() > 0) && (!newCandidates.contains(topCandidate)) )
				newCandidates.add(0, topCandidate);
			*/

			if (newCandidates.size() > 0)
				cands.replace(entity, newCandidates);
			else {
				if (entity.getType() == null)
					toRemove.add(entity);
				else {
					// this should be useless...
					cands.replace(entity, new ArrayList<>());
				}
			}
		}

		for(NerdEntity entity : toRemove) {
			cands.remove(entity);
		}
	}


	/**
	 * Disambiguate a provided vector of weighted terms.
	 */
	public void disambiguateWeightedTerms(NerdQuery nerdQuery) {
		List<WeightedTerm> terms = nerdQuery.getTermVector();
		String lang = null;
		Language language = nerdQuery.getLanguage();
		if (language != null)
			lang = language.getLang();

		if (lang == null) {
			// the language recognition has not been done upstream of the call to this method, so
			// let's do it

			// reformat text content
			StringBuilder textContent = new StringBuilder();
			for(WeightedTerm wt : nerdQuery.getTermVector()) {
				textContent.append(" " + wt.getTerm());
			}
			String text = textContent.toString();

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
			lang = Language.EN;
		}

		// additional target languages for translations (source language is always the default target
		// language for the results!)
		List<String> targetLanguages = nerdQuery.getResultLanguages();

		// get the optional additional contextual text to control the term disambiguation
		String text = nerdQuery.getText();
		if ((text != null) && (text.length() > 0))
			text = text.toLowerCase().trim();
		else {
			text = nerdQuery.getAbstract_();
			text += nerdQuery.getClaims();
			text += nerdQuery.getDescription();
			if ((text != null) && (text.length() > 0))
				text = text.toLowerCase().trim();
		}

		// get the "certain" entities
		List<NerdEntity> userEntities = new ArrayList<NerdEntity>();
		for (WeightedTerm term : terms) {
			List<NerdEntity> entities = term.getNerdEntities();
			if (entities != null) {
				for(NerdEntity entity : entities) {
					if (entity.getSource() == ProcessText.MentionMethod.user) {
						userEntities.add(entity);
					}
				}
			}
		}
		// this is a stable context for the whole vector
		NerdContext stableContext = relatedness.getContext(terms, userEntities, lang);

		List<List<NerdCandidate>> candidates = generateCandidatesTerms(terms, lang);
		int n = 0;
		for(WeightedTerm term : terms) {
			if (term.getNerdEntities() == null) {
				List<NerdCandidate> candidateList = candidates.get(n);

				rank(candidateList, term.getTerm().toLowerCase(), terms, text, lang, stableContext, userEntities);
				prune(candidateList, nerdQuery.getNbest(), 0.1);

				List<NerdEntity> result = new ArrayList<NerdEntity>();

				/*if (entity.getOrigin() == NerdEntity.USER) {
					result.add(entity);
				}
				else */
				{
					for(NerdCandidate candidate : candidateList) {
						if (candidate.getNerdScore() < 0.1)
							continue;

						NerdEntity nerdEntity = new NerdEntity();
						nerdEntity.setRawName(term.getTerm());
						nerdEntity.populateFromCandidate(candidate, lang);
						nerdEntity.setWikipediaMultilingualRef(
							candidate.getWikiSense().getTranslations(), targetLanguages, wikipedias);
						result.add(nerdEntity);
						if (!nerdQuery.getNbest())
							break;
					}
				}
				term.setNerdEntities(result);
			}

			n++;
		}
		// calculate the global categories
		nerdQuery = NerdCategories.addCategoryDistributionWeightedTermVector(nerdQuery);
	}

	private List<List<NerdCandidate>> generateCandidatesTerms(List<WeightedTerm> terms, String lang) {
		List<List<NerdCandidate>> result = new ArrayList<>();
		int n = 0;
		LowerKnowledgeBase wikipedia = wikipedias.get(lang);
		for(WeightedTerm term : terms) {
			List<NerdCandidate> candidates = null;
			List<NerdEntity> entities = term.getNerdEntities();
			if (entities == null)
				candidates = new ArrayList<>();
			else {
				result.add(null);
				n++;
				continue;
			}

			NerdEntity entity = new NerdEntity();
			entity.setRawName(term.getTerm());

			// we go only with Wikipedia for the moment
			Label lbl = new Label(wikipedia.getEnvironment(), term.getTerm()) ;
			if (!lbl.exists()) {
				//System.out.println("No concepts found for '" + entity.getRawName() + "'");
				//if (strict)
				//	continue;
			}
			else {
				Label.Sense[] senses = lbl.getSenses();
				if (senses.length == 0)
					continue;

				int s = 0;
				boolean invalid = false;
				for(int i=0; i<senses.length; i++) {
					Label.Sense sense = senses[i];
					NerdCandidate candidate = new NerdCandidate(entity);
					candidate.setWikiSense(sense);
					candidate.setWikipediaExternalRef(sense.getId());
					candidate.setWikidataId(sense.getWikidataId());
					candidate.setProb_c(sense.getPriorProbability());
					candidate.setPreferredTerm(sense.getTitle());
					candidate.setLang(lang);
					candidate.setLabel(lbl);
					candidate.setProb_c(sense.getPriorProbability());
					for(com.scienceminer.nerd.kb.model.Category theCategory : sense.getParentCategories()) {
						if (!NerdCategories.categoryToBefiltered(theCategory.getTitle()))
							candidate.addWikipediaCategories(new com.scienceminer.nerd.kb.Category(theCategory));
						if (theCategory.getTitle().toLowerCase().contains("disambiguation")) {
							invalid = true;
							break;
						}

					}

					if (invalid)
						continue;
					candidates.add(candidate);
					s++;
					if (s == MAX_SENSES-1) {
						// max. sense alternative has been reach
						break;
					}
				}
			}
			//if (candidates.size() > 0)
			result.add(candidates);
			n++;
		}
		return result;
	}

	/**
	 *  Reconciliate acronyms, i.e. ensure consistency of acronyms and extended forms in the complete
	 *  sequence / document.
	 */
	public void reconciliateAcronyms(NerdQuery nerdQuery) {
		if (nerdQuery.getContext() == null)
			return;

		Map<Mention, Mention> acronyms = nerdQuery.getContext().getAcronyms();
		if ( (acronyms == null) || (acronyms.size() == 0) )
			return;

		List<NerdEntity> entities = nerdQuery.getEntities();
		if ( (entities == null) || (entities.size() == 0) )
			return;

/*for (Map.Entry<Entity, Entity> entry : acronyms.entrySet()) {
Entity acronym = entry.getKey();
Entity base = entry.getValue();
System.out.println(acronym.getRawName() + " / " + base.getRawName());
}*/
		// prepare access to the acronym entities
		Map<String, List<NerdEntity>> entityPositions = indexEntityPositions(entities);
		if ( (entityPositions == null) || (entityPositions.size() ==0) )
			return;

		// gives for a given base all the acronyms mentions
		Map<String, List<Mention>> reverseAcronyms = new HashMap<String, List<Mention>>();
		for (Map.Entry<Mention, Mention> entry : acronyms.entrySet()) {
            Mention acronym = entry.getKey();
            Mention base = entry.getValue();
            String basePos = "" + base.getOffsetStart() + "/" + base.getOffsetEnd();
            if (reverseAcronyms.get(basePos) == null) {
            	List<Mention> acros = new ArrayList<Mention>();
            	acros.add(acronym);
            	reverseAcronyms.put(basePos, acros);
            } else {
            	List<Mention> acros = reverseAcronyms.get(basePos);
            	acros.add(acronym);
            	reverseAcronyms.replace(basePos, acros);
            }
        }

        // work with complete nerd entities now
        for(NerdEntity entity : entities) {
        	// is it a base?
        	String checkBase = "" + entity.getOffsetStart() + "/" + entity.getOffsetEnd();
			if (reverseAcronyms.get(checkBase) != null) {
				List<Mention> localAcronyms = reverseAcronyms.get(checkBase);
				// gather the disambiguations
				NerdEntity best = entity;
				List<NerdEntity> acronymSubset = null;
				for(Mention acroEntity : localAcronyms) {
					int localStartPos = acroEntity.getOffsetStart();
					int localEndPos = acroEntity.getOffsetEnd();
					acronymSubset = getEntitiesAtPos(entityPositions, localStartPos, localEndPos);
					if (acronymSubset != null) {
						for(NerdEntity localAcronym : acronymSubset) {
							if (localAcronym.getNerdScore() > best.getNerdScore()) {
								best = localAcronym;
							}
						}
					}
				}
				// get the best one and propagate it to all base/acronyms NerdEntity
				updateEntity(entity, best);
				for(Mention acroEntity : localAcronyms) {
					int localStartPos = acroEntity.getOffsetStart();
					int localEndPos = acroEntity.getOffsetEnd();
					acronymSubset = getEntitiesAtPos(entityPositions, localStartPos, localEndPos);
					if (acronymSubset != null) {
						for(NerdEntity localAcronym : acronymSubset) {
							updateEntity(localAcronym, best);
						}
					}
				}

			}
        }
	}

	private Map<String, List<NerdEntity>> indexEntityPositions(List<NerdEntity> entities) {
		Map<String, List<NerdEntity>> result = new HashMap<String, List<NerdEntity>>();
		for(NerdEntity entity : entities) {
			String pos = "" + entity.getOffsetStart() + "/" + entity.getOffsetEnd();
			if (result.get(pos) == null) {
				List<NerdEntity> localList = new ArrayList<NerdEntity>();
				localList.add(entity);
				result.put(pos, localList);
			} else {
				List<NerdEntity> localList = result.get(pos);
				localList.add(entity);
				result.replace(pos, localList);
			}
		}
		return result;
	}

	private List<NerdEntity> getEntitiesAtPos(Map<String, List<NerdEntity>> entityPositions,
		int localStartPos, int localEndPos) {
		String pos = "" + localStartPos + "/" + localEndPos;
		return entityPositions.get(pos);
	}

	/**
	 * Update disambiguation information of a NerdEntity based on a given NerdEntity
	 */
	private void updateEntity(NerdEntity toBeUpDated, NerdEntity best) {
		toBeUpDated.setSense(best.getSense());
		toBeUpDated.setWikipediaExternalRef(best.getWikipediaExternalRef());
		toBeUpDated.setProb_c(best.getProb_c());
		toBeUpDated.setPreferredTerm(best.getPreferredTerm());
		toBeUpDated.setLang(best.getLang());
		toBeUpDated.setWikidataId(best.getWikidataId());
		toBeUpDated.setDefinitions(best.getDefinitions());
		toBeUpDated.setDomains(best.getDomains());
		toBeUpDated.setNerdScore(best.getNerdScore());
		toBeUpDated.setSelectionScore(best.getSelectionScore());
		toBeUpDated.setCategories(best.getCategories());
		toBeUpDated.setStatements(best.getStatements());
		toBeUpDated.setType(best.getType());
		toBeUpDated.setSubTypes(best.getSubTypes());
	}

	/**
	 * Try to find the best KB Label from a normalized string. In practice, this method has
	 * a huge impact on performance, as it can maximize the chance to have the right entity
	 * in the list of candidates.
	 */
	public static Label bestLabel(String normalisedString, LowerKnowledgeBase wikipedia) {
		Label label = null;
		//String normalisedString = entity.getNormalisedName();
		if (isEmpty(normalisedString))
			return null;

		// normalised mention following case as it appears
		Label bestLabel = new Label(wikipedia.getEnvironment(), normalisedString);

		// try case variants
		if (!bestLabel.exists()) {

			// full upper or lower case
			if (StringProcessor.isAllUpperCase(normalisedString)) {
				label = new Label(wikipedia.getEnvironment(), normalisedString.toLowerCase());
			}
			else if (StringProcessor.isAllLowerCase(normalisedString)) {
				label = new Label(wikipedia.getEnvironment(), normalisedString.toUpperCase());
			}
			else {
				label = new Label(wikipedia.getEnvironment(), normalisedString.toLowerCase());
				Label label2 = new Label(wikipedia.getEnvironment(), normalisedString.toUpperCase());
				if (label2.exists() && (!label.exists() || label2.getLinkOccCount() > label.getLinkOccCount())) {
					label = label2;
				}
			}

			// first letter upper case
			Label label2 = new Label(wikipedia.getEnvironment(), WordUtils.capitalize(normalisedString.toLowerCase()));
			if (label2.exists() && (!label.exists() || label2.getLinkOccCount() > label.getLinkOccCount())) {
				label = label2;
			} else {
				// try variant cases
				/*if (ProcessText.isAllUpperCase(normalisedString)) {
					// a usual pattern in all upper case that is missed above is a combination of 
					// acronym + normal term, e.g. NY RANGERS -> NY Rangers
					List<String> tentativeLabels = new ArrayList<String>();
					List<LayoutToken> localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken();
					for(int i=0; i<localTokens.size(); i++) {
						LayoutToken token = localTokens.get(i);
						if (token.getText().length() == 2) {


							label2 = new Label(wikipedia.getEnvironment(), WordUtils.capitalize(normalisedString.toLowerCase()));

							tentativeLabels.add();
						}
					}
				}*/

				label2 = new Label(wikipedia.getEnvironment(), WordUtils.capitalizeFully(normalisedString.toLowerCase()));
				if (!label2.exists()) {
					// more aggressive
					label2 = new Label(wikipedia.getEnvironment(),
						WordUtils.capitalizeFully(normalisedString.toLowerCase(), ProcessText.delimiters.toCharArray()));
				}
				if (label2.exists() && (!label.exists() || label2.getLinkOccCount() > label.getLinkOccCount())) {
					label = label2;
				}
			}
			if (label.exists() && (!bestLabel.exists() || label.getLinkOccCount() > bestLabel.getLinkOccCount()*2)) {
				bestLabel = label;
			}
		}

		return bestLabel;
	}

	/**
	 * Try to find the best KB Labels from a normalized string. In practice, this method has
	 * a huge impact on performance, as it can maximize the chance to have the right entity
	 * in the list of candidates.
	 */
	public static List<Label> bestLabels(String normalisedString, LowerKnowledgeBase wikipedia, String lang) {
		List<Label> labels = new ArrayList<Label>();
		//String normalisedString = entity.getNormalisedName();
		if (isEmpty(normalisedString))
			return null;

		// normalised mention following case as it appears
		Label bestLabel = new Label(wikipedia.getEnvironment(), normalisedString);
		labels.add(bestLabel);

		// try case variants
		//if (!bestLabel.exists())
		{
			// first letter upper case
			Label label = new Label(wikipedia.getEnvironment(), WordUtils.capitalize(normalisedString.toLowerCase()));
			if (label.exists())
					labels.add(label);

			// full upper or lower case
			if (StringProcessor.isAllUpperCase(normalisedString)) {
				label = new Label(wikipedia.getEnvironment(), normalisedString.toLowerCase());
				if (label.exists())
					labels.add(label);
			}
			else if (StringProcessor.isAllLowerCase(normalisedString)) {
				label = new Label(wikipedia.getEnvironment(), normalisedString.toUpperCase());
				if (label.exists())
					labels.add(label);
			} else {
				label = new Label(wikipedia.getEnvironment(), normalisedString.toLowerCase());
				if (label.exists())
					labels.add(label);

				label = new Label(wikipedia.getEnvironment(), normalisedString.toUpperCase());
				if (label.exists())
					labels.add(label);
			}

			label = new Label(wikipedia.getEnvironment(), WordUtils.capitalizeFully(normalisedString.toLowerCase()));
			if (label.exists())
				labels.add(label);

			// more aggressive
			label = new Label(wikipedia.getEnvironment(),
					WordUtils.capitalizeFully(normalisedString.toLowerCase(), ProcessText.delimiters.toCharArray()));
			if (label.exists())
				labels.add(label);

			// only first word capitalize
			if (normalisedString.length()>1) {
				label = new Label(wikipedia.getEnvironment(), normalisedString.toLowerCase().substring(0, 1).toUpperCase() +
					normalisedString.toLowerCase().substring(1));
				if (label.exists())
					labels.add(label);
			}

			// try variant cases
			if (StringProcessor.isAllUpperCase(normalisedString)) {
				// a usual pattern in all upper case that is missed above is a combination of
				// acronym + normal term, e.g. NY RANGERS -> NY Rangers
				List<LayoutToken> localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(normalisedString, new Language(lang, 1.0));
				for(int i=0; i<localTokens.size(); i++) {
					LayoutToken token = localTokens.get(i);
					if (token.getText().length() == 2) {
						StringBuilder newLabel = new StringBuilder();
						for(int j=0; j<localTokens.size(); j++) {
							if ( j!= i)
								newLabel.append(WordUtils.capitalize(localTokens.get(j).getText().toLowerCase()));
							else
								newLabel.append(token);
						}
						label = new Label(wikipedia.getEnvironment(), newLabel.toString());
						if (label.exists())
							labels.add(label);
					}
				}
			}
		}

		return labels;
	}

	/**
	 * Exploit a document-level context to reimforce candidates based on previous
	 * disambiguation
	 */
	private void reinforce(Map<NerdEntity, List<NerdCandidate>> candidates, DocumentContext context) {
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			int pageId = -1;
			Pair<NerdEntity, Integer> count = context.getEntityCount(entity.getRawName());
				if (count != null) {
					pageId = count.getA().getWikipediaExternalRef();
				}
			if (pageId != -1) {
				for(NerdCandidate cand : cands) {
					if (cand.getWikipediaExternalRef() == pageId) {
						cand.setNerdScore(cand.getNerdScore() + 0.02*count.getB());
						//System.out.println(cand.toString());
					}
				}
			}

		}
	}

	public String solveCitation(BiblioItem citation) {

        final String originalDOI= citation.getDOI();
        String wikidataID = "";
        if(isEmpty(originalDOI)) {
            LOGGER.warn("Cannot fetch Wikidata ID without DOI. ");
        }else {
            wikidataID = UpperKnowledgeBase.getInstance().getEntityIdPerDoi(originalDOI);

            if (isEmpty(wikidataID)) {
                return "";
            }
        }

        return wikidataID;
	}

	public List<NerdEntity> solveCitations(List<BibDataSet> resCitations) {
		List<NerdEntity> results = null;

		for(BibDataSet bds : resCitations) {
			BiblioItem biblio = bds.getResBib();
			System.out.println(biblio.getDOI());
			if (biblio.getDOI() != null) {
				System.out.println(UpperKnowledgeBase.getInstance().getEntityIdPerDoi(biblio.getDOI()));
			}
		}
		return results;
	}

	/**
	 * Return true when the entity is originally recognised as NE class which we don't want to have
	 * disambiguated. E.g. PERIOD, MEASURE
	 */
	public static boolean isNERClassExcludedFromDisambiguation(NerdEntity entity) {
		//return entity.getType() == NER_Type.MEASURE || entity.getType() == NER_Type.PERIOD;
		return entity.getType() == NER_Type.MEASURE;
	}

}