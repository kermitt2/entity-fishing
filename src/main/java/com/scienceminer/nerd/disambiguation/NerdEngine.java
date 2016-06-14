package com.scienceminer.nerd.disambiguation;

import java.util.*;
import java.io.*;

import org.grobid.core.data.Entity;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.disambiguation.NerdCandidate;
import com.scienceminer.nerd.utilities.NerdProperties;
import org.grobid.core.utilities.OffsetPosition;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.service.NerdQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.model.*;
//import org.wikipedia.miner.annotation.Disambiguator;
import weka.core.*;
import weka.classifiers.*;

import com.scienceminer.nerd.lang.Language;
import com.scienceminer.nerd.utilities.LanguageUtilities;

/**
 * @author Patrice Lopez
 * 
 */
public class NerdEngine {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(NerdEngine.class);

	private static volatile NerdEngine instance = null;
	
	private Lexicon lexicon = null;

	private Classifier ranker_short = null;
	private Classifier ranker_long = null;

	private Classifier selector_short = null;
	private Classifier selector_long = null;

	// ranker model files
	private static final String rankerShortFile = "data/models/ranker-short.model"; 
	private static final String rankerLongFile = "data/models/ranker-long.model";
	
	// selector model files
	private static final String selectorShortFile = "data/models/selector-short.model"; 
	private static final String selectorLongFile = "data/models/selector-long.model";
	
	private Map<String, Wikipedia> wikipedias = null;
	private Map<String, NerdDisambiguator> disambiguators = null;
	private Relatedness relatedness = null;
	private Map<String, WikipediaDomainMap> wikipediaDomainMaps = null;
	private Map<String, FreeBaseTypeMap> freeBaseTypeMaps = null;
	
	static public int maxContextSize = 15;	
	static public int maxLabelLength = 50;
	static public double minLinkProbability = 0.005;
	static public double minSenseProbability = 0.01;
	static public int MAX_SENSES = 5; // maximum level of ambiguity for an entity
	static public double minEntityScore = 0.15; // threshold for final entity pruning
	// disambiguating a term from a weighted term vector
	
	public static NerdEngine getInstance() throws Exception {
	    if (instance == null) {
			getNewInstance();	        
	    }
	    return instance;
	}

	/**
	 * Creates a new instance.
	 */
	private static synchronized void getNewInstance() throws Exception {
		LOGGER.debug("Get new instance of Engine");		
		instance = new NerdEngine();
	}

	/**
	 * Hidden constructor
	 */
	private NerdEngine() throws Exception {			
		// lexicon
		try {
			lexicon = Lexicon.getInstance();
		}
		catch(Exception e) {
			throw new NerdResourceException("Error instanciating the lexicon. ", e);
		}
		
		// rankers 
		/*ObjectInputStream in = null;
		try {
			BufferedInputStream inStream =
				new BufferedInputStream(new FileInputStream(rankerShortFile));
			in = new ObjectInputStream(inStream);
			ranker_short = (Classifier)in.readObject();
			
			if (in != null)
				in.close();
			
			inStream =
				new BufferedInputStream(new FileInputStream(selectorShortFile));
			in = new ObjectInputStream(inStream);
			selector_short = (Classifier)in.readObject();
		}
		catch(Exception e) {
			//throw new NerdResourceException("Error when opening the ranker model for short text: " 
			//	+ rankerShortFile, e);
		}
		finally { 
			if (in != null)
				in.close();
		}*/

		wikipedias = Lexicon.getInstance().getWikipediaConfs();
		try {
			relatedness = Relatedness.getInstance();
			disambiguators = relatedness.getDisambiguators();
			wikipediaDomainMaps = Lexicon.getInstance().getWikipediaDomainMaps();
			//freeBaseTypeMaps = Lexicon.getInstance().getFreeBaseTypeMaps();
		}
		catch(Exception e) {
			throw new NerdResourceException("Error when opening the relatedness model", e);
		}
		
		//kbAccessNERD = KBAccessNERD.getInstance();
		//kbAccessERD = KBAccessERD.getInstance();
	}
	
	/**
	 * Disambiguate a structured query and return the corresponding normalized 
     * enriched and disambiguated query object.
	 * 
	 * @param nerdQuery the
	 *            POJO query object
	 * @param strict boolean indicating if the mentions not resolved should be kept
	 * 			or not in the resulting entities. 
	 * @param shortText boolean indicating if the text to be disambiguated is short,
	 *  		e.g. a text query. 		 
	 * @return a response query object containing the structured representation of
	 *         the enriched and disambiguated query.
	 */
	public List<NerdEntity> disambiguate(NerdQuery nerdQuery, boolean shortText) {
		String text = nerdQuery.getText();
		
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
				language = languageUtilities.runLanguageIdRestricted(text);
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
		
		// additional target languages for translations (source language is always the default target 
		// language for the results!)
		List<String> targetLanguages = nerdQuery.getResultLanguages();
		
		List<NerdEntity> entities = nerdQuery.getEntities();
		Integer[] processSentence = nerdQuery.getProcessSentence();
		
		Map<NerdEntity, List<NerdCandidate>> candidates = generateCandidates(text, entities, lang);
int nbEntities = 0;
int nbCandidates = 0;
for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
	List<NerdCandidate> cands = entry.getValue();
	NerdEntity entity = entry.getKey();
	nbEntities += 1;
	nbCandidates += cands.size();
	//System.out.println(entity.toString());
/*for(NerdCandidate cand : cands) {
	System.out.println(cand.toString());
}*/
}
System.out.println("total number of entities: " + nbEntities);
System.out.println("total number of candidates: " + nbCandidates);
		rank(candidates, lang);
/*for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
	List<NerdCandidate> cands = entry.getValue();
	NerdEntity entity = entry.getKey();
for(NerdCandidate cand : cands) {
	System.out.println(cand.toString());
}
}*/
		prune(candidates, nerdQuery.getNbest(), shortText, minEntityScore);
		//impactOverlap(candidates);
		//if (!shortText && !nerdQuery.getNbest())
		//	pruneOverlap(candidates);
			
		WikipediaDomainMap wikipediaDomainMap = wikipediaDomainMaps.get(lang);
		//FreeBaseTypeMap freeBaseTypeMap = freeBaseTypeMaps.get(lang);

		List<NerdEntity> result = new ArrayList<NerdEntity>();
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			
			if (entity.getOrigin() == NerdEntity.USER) {
				result.add(entity);
			}
			else if ( (cands == null) || (cands.size() == 0) ) {
				// default for class entity only
				if (entity.getType() != null) {
					entity.setNerd_score(entity.getNer_conf()); 
					result.add(entity);
				}
			}
			else {
				for(NerdCandidate candidate : cands) {
					NerdEntity nerdEntity = new NerdEntity(entity);
					nerdEntity.populateFromCandidate(candidate, lang);
					nerdEntity.setWikipediaMultilingualRef(
						candidate.getWikiSense().getTranslations(), targetLanguages, wikipedias);
					//nerdEntity.setDomains(freeBaseTypeMap.getTypes(nerdEntity.getWikipediaExternalRef()));
					// note: for the moment we use English categories via translingual informations
					if (lang.equals("en"))
						nerdEntity.setDomains(wikipediaDomainMap.getDomains(nerdEntity.getWikipediaExternalRef()));
					else {
						// we get the English page id if available
						int pageId = nerdEntity.getWikipediaExternalRef();
						Map<String,String> translations = candidate.getWikiSense().getTranslations();
						String translationEN = translations.get("en");
						Article article = wikipedias.get("en").getArticleByTitle(translationEN);
						if (article != null) {
							nerdEntity.setDomains(wikipediaDomainMap.getDomains(article.getId()));
						}
					}
					result.add(nerdEntity);
					//if (!nerdQuery.getNbest())
					//	break;
				}
			}
		}

		Collections.sort(result);
		//if (!shortText && !nerdQuery.getNbest())
		if (!nerdQuery.getNbest())	
			result = pruneOverlap(result, shortText);

		return result;
	}


	public Map<NerdEntity, List<NerdCandidate>> generateCandidates(String text, 
															List<NerdEntity> entities,
															String lang) {
		Map<NerdEntity, List<NerdCandidate>> result = new TreeMap<NerdEntity, List<NerdCandidate>>();
		Wikipedia wikipedia = wikipedias.get(lang);
		if (wikipedia == null) {
			throw new NerdException("Wikipedia environment is not loaded for language " + lang);
		}
		if (entities != null) {
			for(NerdEntity entity : entities) {
				// if the entity is already inputed in the query (i.e. by the "user"), we do not generate candidates
				// for it if they are disambiguated
				if (entity.getOrigin() == Entity.USER) {
					// do we have disambiguated entity information for the entity?
					if (entity.getWikipediaExternalRef() != -1) {
						result.put(entity, null);
						continue;
					}
				}

				// we go only with Wikipedia for the moment
//System.out.println(entity.getRawName());
				Label lbl = new Label(wikipedia.getEnvironment(), entity.getRawName(), null);
				List<NerdCandidate> candidates = new ArrayList<NerdCandidate>();

				if (!lbl.exists()) {
					//System.out.println("No concepts found for '" + entity.getRawName() + "'");
					//if (strict)
					if (entity.getType() != null) {
						result.put(entity, candidates);
						continue;
					}
				}
				else {
					entity.setLinkProbability(lbl.getLinkProbability());
					Label.Sense[] senses = lbl.getSenses();
					if (senses.length > 0) {
						int s = 0;
						for(int i=0; i<senses.length; i++) {
							Label.Sense sense = senses[i];
							if (sense.getPriorProbability() < minSenseProbability)
								continue;
							// not a valid sense if title is a list of ...
							String title = sense.getTitle();
							if ((title == null) || title.startsWith("List of ")) 
								continue;
							NerdCandidate candidate = new NerdCandidate(entity);
							candidate.setWikiSense(sense);
							candidate.setWikipediaExternalRef(sense.getId());
							candidate.setProb_c(sense.getPriorProbability());
							candidate.setPreferredTerm(sense.getTitle());
							candidate.setLang(lang);
							boolean invalid = false;
							for(org.wikipedia.miner.model.Category theCategory : sense.getParentCategories()) {
								// not a valid sense if a category of the sense contains "disambiguation" -> this is then a disambiguation page
								if (theCategory.getTitle().indexOf("disambiguation") == -1)
									candidate.addWikipediaCategories(new com.scienceminer.nerd.kb.Category(theCategory));
								else {
									invalid = true;
									break;
								}
							}
							if (invalid)
								continue;
							// inject KB info (frequencies and FreeBase/Wiki mapping)
							// only english so far 
							// ideally these info should be injected in Berkeley DB with the other "sense" information
							/*KBAccess kbAccess = null;
							if (strict)
								kbAccess = kbAccessERD;
							else 
								kbAccess = kbAccessNERD;
							if (lang.equals("en")) {
								kbAccess.accessKB(candidate);
							}
							else {
								kbAccess.addFreeBaseId(candidate, wikipedias, candidate.getWikiSense().getTranslations());
							}*/
							candidates.add(candidate);
							s++;
							if (s == MAX_SENSES-1) {
								// max. sense alternative has been reach
								break;
							}
						}
					}
					if ( (candidates.size() > 0) || (entity.getType() != null) )
						result.put(entity, candidates);
				}
				
			}
		}
		return result;
	}

	/**
	 * Ranking of the candidates for a set of mentions from a contextual text
	 */
	private void rank(Map<NerdEntity, List<NerdCandidate>> candidates, String lang) {
		// we rank candidates for each entity mention
//relatedness.resetCache(lang);
		// first pass to get the "certain" entities 
		List<NerdEntity> userEntities = new ArrayList<NerdEntity>();
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			NerdEntity entity = entry.getKey();
			if (entity.getOrigin() == Entity.USER) {
				userEntities.add(entity);
			}
		}

		// we create a context for the disambiguation
		NerdContext context = null;
		try {
			 context = relatedness.getContext(candidates, userEntities, lang);
//System.out.println("size of context: " + context.getSenseNumber());
//System.out.println(context.toString());
		} catch(Exception e) {
			e.printStackTrace();
		}

		NerdDisambiguator disambiguator = disambiguators.get(lang);
		if (disambiguator == null) {
			Wikipedia wikipedia = wikipedias.get(lang);
			try {
				disambiguator = new NerdDisambiguator(wikipedia, 
													minSenseProbability, 
													maxLabelLength, 
													minLinkProbability, 
													maxContextSize);
				disambiguators.put(lang, disambiguator);
			}
			catch(Exception e) {
				e.printStackTrace();
			} 
		}

		// second pass for producing the ranking score
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			
			if (cands == null)
				continue;
			
			for(NerdCandidate candidate : cands) {			
				double score = 0.0;
				try {
					double commonness = candidate.getProb_c(); 
					double related = relatedness.getRelatednessTo(candidate, context, lang);
					if (disambiguator == null) {
						System.out.println("Cannot rank candidates: disambiguator for the language " + 
							lang + " is invalid");
					}
					score = disambiguator.getProbabilityOfSense(commonness, related, context);
					
					//System.out.println(candidate.getWikiSense().getTitle() + " " + candidate.getNerd_score() +  " " + entity.toString());
					//System.out.println("\t\t" + "commonness: " + commonness + ", relatedness: " + related);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				
				candidate.setNerd_score(score);
			}
			Collections.sort(cands);
		}

System.out.println("relatedness - Comparisons requested: " + relatedness.getComparisonsRequested());
System.out.println("relatedness - Comparisons calculated: " + relatedness.getComparisonsCalculated());
System.out.println("relatedness - cache proportion: " + relatedness.getCachedProportion());
	}

	/**
	 * Ranking of candidates for a term rawTerm in a vector of weighted terms.
	 * Optionally a contextual text is given, where the terms of the vector might occur (or not). 
	 */
	private void rank(List<NerdCandidate> candidates, String rawTerm, //List<WeightedTerm> terms, 
					  String text, String lang, NerdContext context, List<NerdEntity> userEntities) {
	    if ( (candidates == null) || (candidates.size() == 0) )
			return;

		// get the disambiguator for this language
		NerdDisambiguator disambiguator = disambiguators.get(lang);
		if (disambiguator == null) {
			Wikipedia wikipedia = wikipedias.get(lang);
			try {
				disambiguator = new NerdDisambiguator(wikipedia, 
													minSenseProbability, 
													maxLabelLength, 
													minLinkProbability, 
													maxContextSize);
				disambiguators.put(lang, disambiguator);
			}
			catch(Exception e) {
				e.printStackTrace();
			} 
		}
		
		// if we have extra textual information, we can try to get the different local contexts
		List<NerdContext> localContexts = null;
		if ( (text != null) && (text.length() > 0) ) {
			List<String> localContextStrings = buildLocalContexts(rawTerm, text);
			// build the corresponding contexts
			for(String localContextString : localContextStrings) {
				try {
					NerdContext contextObject = relatedness.getContextFromText(localContextString, userEntities, lang);
					if (localContexts == null)
						localContexts = new ArrayList<NerdContext>();
					localContexts.add(contextObject);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}

		// second pass for producing the ranking score
		for(NerdCandidate candidate : candidates) {			
			double score = 0.0;
			try {
				double commonness = candidate.getProb_c(); 

				// for the candidate
				double related = 0.0;
				if (localContexts == null) {
					related = relatedness.getRelatednessTo(candidate, context, lang);
					score = disambiguator.getProbabilityOfSense(commonness, related, context);
				}
				else {
					// we disambiguate for each local context
					related = relatedness.getRelatednessTo(candidate, context, lang);
					score = disambiguator.getProbabilityOfSense(commonness, related, context);

					for(NerdContext localContext : localContexts) {
						related = relatedness.getRelatednessTo(candidate, localContext, lang);
						score += disambiguator.getProbabilityOfSense(commonness, related, localContext);
						//double localScore = disambiguator.getProbabilityOfSense(commonness, related, localContext);
						//if (localScore > score)
						//	score = localScore;
					} 
					score = score / (localContexts.size() + 1);
				}
				
				//System.out.println(candidate.getWikiSense().getTitle() + " " + candidate.getNerd_score() +  " " + entity.toString());
				//System.out.println("\t\t" + "commonness: " + commonness + ", relatedness: " + related);
			}
			catch(Exception e) {
				e.printStackTrace();
			}

/*				if (candidate.getProb_c() > 0) {
				candidate.setNerd_score(candidate.getNerd_score() + candidate.getProb_c());
			}*/
			candidate.setNerd_score(score);
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

	public void prune(Map<NerdEntity, List<NerdCandidate>> candidates, boolean nbest, boolean shortText, double threshold) {
		List<NerdEntity> toRemove = new ArrayList<NerdEntity>();
		
		// we prune candidates for each entity mention
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			if ( (cands == null) || (cands.size() == 0) )
				continue;
			NerdEntity entity = entry.getKey();
			List<NerdCandidate> newCandidates = new ArrayList<NerdCandidate>();
			for(NerdCandidate candidate : cands) {	
				if (!nbest) {
					if (shortText && (candidate.getNerd_score() > 0.10)) {
						newCandidates.add(candidate);
						break;
					}
					else if (candidate.getNerd_score() > threshold) {
						newCandidates.add(candidate);
						break;
					}
				}
				else {
					if (shortText && (candidate.getNerd_score() > 0.10)) {
						newCandidates.add(candidate);
					}
					else if ( (newCandidates.size() == 0) && (candidate.getNerd_score() > threshold) ) {
						newCandidates.add(candidate);
					}
					else if (candidate.getNerd_score() > 0.6) {
						newCandidates.add(candidate);
					}
				}
			}
			if (newCandidates.size() > 0)
				candidates.replace(entity, newCandidates);
			else {
				if (entity.getType() == null)
					toRemove.add(entity);
				else
					candidates.replace(entity, new ArrayList<NerdCandidate>());
			}
		}
		
		for(NerdEntity entity : toRemove) {
			candidates.remove(entity);
		}
	}

	/**	 
	 * 	We prioritize the longest term match from the KB : the term coming from the KB shorter than
     *  the longest match from the KB and which have not been merged, are lowered.
	 */
	public void impactOverlap(Map<NerdEntity, List<NerdCandidate>> candidates) {
		List<Integer> toRemove = new ArrayList<Integer>();
		
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();
			if (cands == null)
				continue;
			int arity = entity.getRawName().split("[ ,-.]").length;
			for(NerdCandidate candidate : cands) {
				double score = candidate.getNerd_score();
				double new_score = score - ( (5-arity)*0.01);
				if ( (new_score > 0) && (new_score <= 1) )
					candidate.setNerd_score(new_score);

			}
			Collections.sort(cands);
		}
	}

	/**	 
	 * 	We prioritize the longest term match from the KB : the term coming from the KB shorter than
     *  the longest match from the KB is pruned. For equal mention arity, nerd confidence score is used.
	 *  Note that the longest match heuristics is debatable and should be further experimentally
	 *  validated... 
	 */
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
//System.out.println("Removing " + pos1 + " - " + entity1.getRawName());
				continue;
			}

			int arity1 = entity1.getRawName().length() - entity1.getRawName().replaceAll("\\s", "").length() + 1;
//System.out.println("Position1 " + pos1 + " / arity1 : " + entity1.getRawName() + ": " + arity1);
			
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
//System.out.println("Removing " + pos2 + " - " + entity2.getRawName());
						continue;
					}

					if ((entity2.getType() != null) && (entity2.getWikipediaExternalRef() == -1)) {
						// we have a NER not disambiguated
						// check if the other entity has been disambiguated
						if ( (entity1.getWikipediaExternalRef() != -1) && (entity1.getNerd_score() > 0.2) ) {
							if (!toRemove.contains(new Integer(pos2))) {
								toRemove.add(new Integer(pos2));
							}
//System.out.println("Removing " + pos2 + " - " + entity2.getRawName());
							continue;
						} 
					} 

					if ((entity1.getType() != null) && (entity1.getWikipediaExternalRef() == -1)) {
						// we have a NER not disambiguated
						// check if the other entity has been disambiguated
						if ( (entity2.getWikipediaExternalRef() != -1) && (entity2.getNerd_score() > 0.2) ) {
							if (!toRemove.contains(new Integer(pos1))) {
								toRemove.add(new Integer(pos1));
							}
//System.out.println("Removing " + pos1 + " - " + entity1.getRawName());
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
//System.out.println("Removing " + pos2 + " - " + entity2.getRawName());
							continue;
						}
					}

					int arity2 = entity2.getRawName().length() - entity2.getRawName().replaceAll("\\s", "").length() + 1;
//System.out.println("arity2 : " + entity2.getRawName() + ": " + arity2);
					if (arity2 < arity1) {
						// longest match wins
						if (!toRemove.contains(new Integer(pos2)))
							toRemove.add(new Integer(pos2));
//System.out.println("Removing " + pos2 + " - " + entity2.getRawName());
						continue;
					}
					else if (arity2 == arity1) {
						// we check the nerd scores of the top candiate for the two entities
						double conf1 = entity1.getNerd_score();
						double conf2 = entity2.getNerd_score();
						if (conf2 < conf1) {
							if (!toRemove.contains(new Integer(pos2))) {
								toRemove.add(new Integer(pos2));
							}
//System.out.println("Removing " + pos2 + " - " + entity2.getRawName());
							continue;
						}
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
					entities.get(i).setNerd_score(entities.get(i).getNerd_score() / 2);
					newEntities.add(entities.get(i));
				} 
			}
		}

		return newEntities;
	}

	public List<NerdCandidate> annotateShort(String runID, String textID, String text) {
		System.out.println("runID=" + runID + " textID="+textID+ " text="+text);
			
		long start = System.currentTimeMillis();
		LOGGER.debug(">> set ERD short text for stateless service: " + text);	
					
		//List<ErdAnnotationShort> annotations = new ArrayList<ErdAnnotationShort>();
		List<NerdCandidate> concept_terms = null;//generateCandidates(text, true);	
		
		if ( (concept_terms != null) && (concept_terms.size() > 0) ) {	
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
	public List<NerdCandidate> merge(List<NerdCandidate> terms, 
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
				String surface1 = term1.getEntity().getRawName();
				
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
						String surface2 = term2.getEntity().getRawName();
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
	}
	
	/**	 
	 * 	We prune following the longest term match from the KB : the term coming from the KB shorter than
     *  the longest match from the KB and which have not been merged, are removed.
	 */
	public List<NerdCandidate> prune(List<NerdCandidate> terms, boolean strict, double threshold) {
//System.out.println("Prunning...");
//System.out.println(terms.toString());

		if ( (terms == null) || (terms.size() == 0) ) 
			return null;

		List<Integer> toRemove = new ArrayList<Integer>();
		for(int i=0; i<terms.size(); i++) {
			NerdCandidate term1 = terms.get(i);
			
			if (term1.isSubTerm) {
				continue;
			}
			
			if (term1.getMethod() == NerdCandidate.NERD) {
				String surface1 = term1.getEntity().getRawName();
				
				// we check if the raw string is a substring of another NerdCandidate from the ERD method
				for(int j=0; j<terms.size(); j++) {
					if (j == i)
						continue;										
						
					/*if (toRemove.contains(new Integer(j))) {
						// already pruned
						continue;
					}*/
					
					NerdCandidate term2 = terms.get(j);

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
					String surface2 = term2.getEntity().getRawName();
					if ((surface2.length() > surface1.length()) && (surface2.indexOf(surface1) != -1)) {
						toRemove.add(new Integer(i));
						break;
					}
				}
			}
		}
		
		List<NerdCandidate> result = new ArrayList<NerdCandidate>();
		for(int i=0; i<terms.size(); i++) {
			if (toRemove.contains(new Integer(i))) {
				continue;
			}
			else if (terms.get(i).getNerd_score() > threshold) {
				result.add(terms.get(i));
			}
		}
		
		return result;
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
				language = languageUtilities.runLanguageIdRestricted(text);
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
					if (entity.getOrigin() == Entity.USER) {
						userEntities.add(entity);
					}
				}
			}
		}
		// this is a stable context for the whole vector
		NerdContext stableContext = null;
		try {
			 stableContext = relatedness.getContext(terms, userEntities, lang);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		List<List<NerdCandidate>> candidates = generateCandidatesTerms(terms, lang); 
		int n = 0;
		for(WeightedTerm term : terms) {
			if (term.getNerdEntities() == null) {
				List<NerdCandidate> candidateList = candidates.get(n);

				rank(candidateList, term.getTerm().toLowerCase(), text, lang, stableContext, userEntities);
				prune(candidateList, nerdQuery.getNbest(), 0.10);

				List<NerdEntity> result = new ArrayList<NerdEntity>();

				/*if (entity.getOrigin() == NerdEntity.USER) {
					result.add(entity);
				}
				else */
				{
					for(NerdCandidate candidate : candidateList) {						
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
		List<List<NerdCandidate>> result = new ArrayList<List<NerdCandidate>>();
		int n = 0;
		Wikipedia wikipedia = wikipedias.get(lang);
		for(WeightedTerm term : terms) {
			List<NerdCandidate> candidates = null;
			List<NerdEntity> entities = term.getNerdEntities();
			if (entities == null)
				candidates = new ArrayList<NerdCandidate>();
			else {
				result.add(null);
				n++;
				continue;
			}
			
			NerdEntity entity = new NerdEntity();
			entity.setRawName(term.getTerm());
			
			// we go only with Wikipedia for the moment
			Label lbl = new Label(wikipedia.getEnvironment(), term.getTerm(), null) ;
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
				for(int i=0; i<senses.length; i++) {
					Label.Sense sense = senses[i];
					NerdCandidate candidate = new NerdCandidate(entity);
					candidate.setWikiSense(sense);
					candidate.setWikipediaExternalRef(sense.getId());
					candidate.setProb_c(sense.getPriorProbability());
					candidate.setPreferredTerm(sense.getTitle());
					candidate.setLang(lang);
					for(org.wikipedia.miner.model.Category theCategory : sense.getParentCategories()) {
						candidate.addWikipediaCategories(new com.scienceminer.nerd.kb.Category(theCategory));
					}
					
					// inject KB info (frequencies and FreeBase/Wiki mapping)
					//if (strict) 
					{
						//kbAccessScience.accessMongoDB(candidate);
					}
					//else {
						//kbAccessERD.accessKB(candidate);
					//}
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
	 *  Return the Wikipedia categories of the specified article
	 */
	/*public List<Category> getParentCategories(String articleID, String lang) {
		// get the wikipedia article
		Wikipedia wikipedia = wikipedias.get(lang);
		Article article = wikipedia.getArticleByTitle(articleID.replace("_", " "));
		if (article == null) {
			System.out.println("article NOT found in wiki.miner: " + articleID);
			return null;
		}
		List<Category> categories = new ArrayList<Category>();
		Category[] cats = article.getParentCategories();
		
		// we keep only considered categories
		for(int i = 0; i<cats.length; i++) {
			Category cat = cats[i];
			//System.out.println(cat.toString());
			//System.out.println(cat.getTitle());
		}
		
		return categories;
	}*/
	
	
	/**
	 * Based on computed candidate scores, select n-best candidates.  
	 */
/*	public List<NerdCandidate> select(List<NerdCandidate> terms, int nbest, int method, String text) {
System.out.println("Selecting...");	
System.out.println(terms.toString());	

		if ( (terms == null) || (terms.size() == 0) ) 
			return null;

		if (method == 1) {
			// machine learning method - a selector produces a score corresponding to the probability of
			// valid candidate entity
			
			// Generate input format for classifier
			FeatureSelectorShortVector generalFeatures = new FeatureSelectorShortVector();
			FastVector atts = generalFeatures.setHeaderInstance();
			Instances classifierData = new Instances("ClassifierData", atts, terms.size());
			classifierData.setClassIndex(generalFeatures.getNumFeatures()-1);
			
			for (NerdCandidate candidate : terms) {
				Vector<Article> context = erdRelatedness.collectContextTerms(terms, candidate);
				
				// create the data instance corresponding to the candidate
				FeatureSelectorShortVector features = new FeatureSelectorShortVector();
				if (features.Add_prob_c) {
					features.prob_c = candidate.getProb_c();
				}
				if (features.Add_prob_i) {
					features.prob_i = candidate.getProb_i();
				}
				if (features.Add_frequencyStrict) {
					features.frequencyStrict = candidate.getFreq();
				}
				if (features.Add_frequencyConcept) {
					features.frequencyConcept = 0;
				}
				if (features.Add_termLength) {
					StringTokenizer st = new StringTokenizer(candidate.getRawString(), " -,");
					features.termLength = st.countTokens();
				}
				double relatedness = erdRelatedness.getRelatednessTo(candidate, context);
				candidate.setRelatednessScore(relatedness);
				if (features.Add_relatedness) {
					features.relatedness = relatedness;
				}
				if (features.Add_inDictionary) {
					boolean inDict = false;				
					if (lexicon.inDictionary(candidate.getRawString())) {
						inDict = true;
					}
					else {
						String[] toks = candidate.getRawString().split(" -,");
						boolean allDict = false;
						for (int i=0; i<toks.length; i++) {
							if (!lexicon.inDictionary(toks[i])) {
								allDict = false;
								break;
							}
							else {
								allDict = true;
							}
						}
						if (allDict) {
							inDict = true;
						}
					}
					
					if (inDict)
						features.inDictionary = true;
					else
						features.inDictionary = false;
				}
				if (features.Add_isSubTerm) { 
					boolean val = false;
					
					if (candidate.isSubTerm) {
						val = true;
					}
					else {
						String surface1 = candidate.getRawString();

						// we check if the raw string is a substring of another NerdCandidate from the ERD method
						for(int j=0; j<terms.size(); j++) {									
							NerdCandidate term2 = terms.get(j);

							String surface2 = term2.getRawString();
							if ((surface2.length() > surface1.length()) && (surface2.indexOf(surface1) != -1)) {
								val = true;
								break;
							}
						}
					}
					
					if (val)
						features.isSubTerm = true;
					else 
						features.isSubTerm = false;
				}
				if (features.Add_ner_st) {
					if (candidate.getMentionEntityType() != null) {
						features.ner_st = true;
					}
				}
				if (features.Add_ner_id) {
					if (candidate.getEntityType() != null) {
						features.ner_st = true;
					}
				}
				if (features.Add_ner_type) {
					if (candidate.getMentionEntityType() != null) {
						features.ner_type = candidate.getMentionEntityType();
					}
					else if (candidate.getEntityType() != null) {
						if (candidate.getEntityType().equals("person/N1"))
							features.ner_type = "PERSON";
						else if (candidate.getEntityType().equals("location/N1"))
							features.ner_type = "LOCATION";	
						else if (candidate.getEntityType().equals("organizational_unit/N1"))
							features.ner_type = "ORGANIZATION";	
						else
							features.ner_type = "NotNER";	
					}
				}
				if (features.Add_ner_subtype) {
					List<String> subTypes = candidate.getEntitySubTypes();
					if ( (subTypes != null) && (subTypes.size()>0) ) {
						features.ner_subtype = subTypes.get(0);
					}
					else
						features.ner_subtype = "NotNER";
				}				
				if (features.Add_NERType_relatedness) {
									
				}
				if (features.Add_NERSubType_relatedness) {
					
				}
				if (features.Add_occ_term) {
					long frequency = erdRelatedness.getTermOccurrence(candidate.getRawString());
					features.occ_term = frequency;
				}
				if (features.Add_rank_score) {
					features.rank_score = candidate.getNerd_score();
				}
				if (features.Add_preferred_term) {
					if (candidate.getPreferredTerm() != null) {
						if (candidate.getPreferredTerm().toLowerCase().equals(candidate.getRawString())) {
							features.preferred_term = true;
						}
					}
				}
				List<String> types = candidate.getFreebaseTypes();
				if (types != null) {
					List<String> shortTypes = new ArrayList<String>();
					for(String type : types) {
						int ind = type.indexOf("/", 2);
						String subType = type.substring(0,ind);
						if ((!shortTypes.contains(subType)) && ErdUtilities.erdHighCategories.contains(subType)) {
							shortTypes.add(subType);
						}
					}
					candidate.setFreebaseHighTypes(shortTypes);
				}
				if (features.Add_freebase_type) {
					if (types != null) {
						List<String> shortTypes = candidate.getFreebaseHighTypes();
						if (shortTypes.size() == 0) {
							features.freebase_type = "unk";
						}
						else if (shortTypes.size() == 1) {
							features.freebase_type = shortTypes.get(0);
						}
						else {
							// we need to select the most discriminant type
							// TBD...
							features.freebase_type = shortTypes.get(0);
						}
					}
				}	
				if (features.Add_freebase_fulltype) {
					if (types != null) {
						List<String> allTypes = new ArrayList<String>();
						for(String type : types) {
							if (ErdUtilities.erdCategories.contains(type)) {
								allTypes.add(type);
							}
						}
						
						if (allTypes.size() == 0) {
							features.freebase_fulltype = "unk";
						}
						else if (allTypes.size() == 1) {
							features.freebase_fulltype = allTypes.get(0);
						}
						else {
							// we need to select the most discriminant type
							// TBD...
							features.freebase_fulltype = allTypes.get(0);
						}
					}
				}
				
				//List<Category> categories = erdRelatedness.getParentCategories(candidate.getWikipediaExternalRef());
				
				// convert the feature vector into a WEKA data instance
				Instance inst = features.getFeatureValues(classifierData);
System.out.println(candidate.toString());
System.out.println("The instance: " + inst); 
				
				// Get the scores
				try {
					double[] probs = selector_short.distributionForInstance(inst);
					double prob = 1.00 - probs[0];
					//double prob = ranker_short.classifyInstance(inst);
					
System.out.println("The selector score: " + prob);
					candidate.setSelectionScore(prob);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			
		}
			
		//if (method == 0) 
		//{
			// we simply use the entity score without any other processing or heuristics
			
			// create a map with the surface string as key for candidates
			List<String> freebaseResults = new ArrayList<String>();
			Map<String, List<NerdCandidate>> candidates = new HashMap<String, List<NerdCandidate>>();
			for(NerdCandidate term : terms) {
				String surface = term.getRawString();
				List<NerdCandidate> theList = candidates.get(surface);
				if (theList == null) {
					theList = new ArrayList<NerdCandidate>();
				}
				theList.add(term);
				candidates.put(surface, theList);
			}
			
			List<NerdCandidate> result = new ArrayList<NerdCandidate>();
			for (String key : candidates.keySet()) {
			 	List<NerdCandidate> list = candidates.get(key);
				
				// sort according to the ranking score (aka nerd_score, it has been made for that!)
//System.out.println("BEFORE SORT: " + list.toString());
				//Collections.sort(list);
//System.out.println("AFTER SORT: " + list.toString());
				
				// if we have candidates that all cover the whole input text, there is no disambiguation
				// context, thus we can safely output all the corresponding candidate entities
				if (key.length() == text.length()) {
					int h = 0;
					for(NerdCandidate term : list) {
						if (term.getFreeBaseExternalRef() != null) {
							//if (term.getPreferredTerm() != null) {
							//	if (term.getPreferredTerm().toLowerCase().equals(text)) {
							//		result.add(term);
							//		continue;
							//	}
							//}
							
							//if ((term.getProb_c() > 0.001) || (term.getFreq() > 1)) 
							if (term.getSelectionScore() > 0.001)
							{
								if (!freebaseResults.contains(term.getFreeBaseExternalRef())) {	
									result.add(term);
									freebaseResults.add(term.getFreeBaseExternalRef());
									h++;
								}
							}
						}
						// but no more than 3
						if (h>2) {
							break;
						}
					}
				}
				else {
					// otherwise we select the highest score
					NerdCandidate theBest = null;
					if ( (list == null) || (list.size() == 0) )
						continue;
					for(NerdCandidate term : list) {
						if (theBest == null) {
							theBest = term;
						}
						else if (term.getNerd_score() > theBest.getNerd_score()) {
							theBest = term;
						}
					}
					if (theBest != null) {
						// the best candidate has also to be in the freebase snapshot
						if (theBest.getFreeBaseExternalRef() != null) {
							// and they have be identified as NE by the NER or is not in the dictionary
							boolean inDict = false;
						
							if (lexicon.inDictionary(key)) {
								inDict = true;
							}
							else {
								String[] toks = key.split(" -,");
								boolean allDict = false;
								for (int i=0; i<toks.length; i++) {
									if (!lexicon.inDictionary(toks[i])) {
										allDict = false;
										break;
									}
									else {
										allDict = true;
									}
								}
								if (allDict) {
									inDict = true;
								}
							}
							if ( (theBest.getEntityType() != null) || 
								 (theBest.getMentionEntityType() != null) || 
								 !inDict ||
								( (theBest.getProb_c() > 0.90) && (theBest.getFreq() > 1000) ) 
								|| (theBest.getSelectionScore() > 0.79) 
								|| (theBest.getEntitySubTypes() != null)  
								) {
								//if (!theBest.isSubTerm)
								if ((theBest.getProb_c() > 0.1) || (theBest.getFreq() > 1)) {
									if (!freebaseResults.contains(theBest.getFreeBaseExternalRef())) {	
										result.add(theBest);
										freebaseResults.add(theBest.getFreeBaseExternalRef());
									}
								}
							}
						}
					}
				}
	        }

			// score-based prunning... 
			List<NerdCandidate> finalResult = new ArrayList<NerdCandidate>();
			int p = 0;
			for(NerdCandidate term : result) {
				// if it is a substring of another candidate...
				boolean val = false;
				double maxSelect = 0.0;
				List<String> freebaseHighTypeGlobal = null;
				List<String> freebaseTypeGlobal = null;
				//if (term.isSubTerm) {
				//	val = true;
				//}
				//else 
				//
				{
					String surface1 = term.getRawString();

					// we check if the raw string is a substring of another NerdCandidate from the ERD method
					for(int j=0; j<result.size(); j++) {									
						NerdCandidate term2 = result.get(j);

						String surface2 = term2.getRawString();
						if ((surface2.length() > surface1.length()) && (surface2.indexOf(surface1) != -1)) {
							val = true;
							if (term2.getSelectionScore() > maxSelect)
								maxSelect = term2.getSelectionScore();
							if ( (term2.getFreebaseHighTypes() != null) && (term2.getFreebaseHighTypes().size() > 0) ) {	
								for (String typ : term2.getFreebaseHighTypes()) {
									if (freebaseHighTypeGlobal == null) {
										freebaseHighTypeGlobal = new ArrayList<String>();
									}
									if (!freebaseHighTypeGlobal.contains(typ))
										freebaseHighTypeGlobal.add(typ);
								}
							}
							if ( (term2.getFreebaseTypes() != null) && (term2.getFreebaseTypes().size() > 0) ) {	
								for (String typ : term2.getFreebaseTypes()) {
									if (freebaseTypeGlobal == null) {
										freebaseTypeGlobal = new ArrayList<String>();
									}
									if (!freebaseTypeGlobal.contains(typ))
										freebaseTypeGlobal.add(typ);
								}
							}
						}
					}
				}
				
				if ( ((term.getProb_c() > 0.001) && (term.getFreq() > 1)) || 
					 (term.getRawString().length() == text.length()) || 
					 (term.getSelectionScore() == 1) ||
					 (term.getEntitySubTypes() != null) ||
					 (term.isSpecialEntityType() )
				   ) 
				{
					List<NerdCandidate> contextTerms = new ArrayList<NerdCandidate>();
					for(int j=0; j<result.size(); j++) {
						if (j != p) {		
							contextTerms.add(result.get(j));
						}
					}
					Vector<Article> context = erdRelatedness.collectAllContextTerms(contextTerms);
//System.out.println(contextTerms.toString());	
//System.out.println(context.toString());

					// semantic relatedness between the current sub term and the other terms
					double localRelatedness = erdRelatedness.getRelatednessTo(term, context);
					System.out.println("relatedness " + term.getRawString() + " / " + localRelatedness);
					System.out.println("selection score " + term.getRawString() + " / " + term.getSelectionScore());
					
					if (!val) {
						boolean skip = false;
						if (term.getEntitySubTypes() != null) {
							if (term.getEntitySubTypes().contains("musical_composition/N1")
							 	&& (term.getCoarseConfidence() > 0.7)) {
								// /music/composition is not in the ERD set
								skip = true;
							}
						}
						//if ( (term.getSelectionScore() > 0.0001) ) {
						//if ( (term.getSelectionScore() > 0.01) ) {		
						//	 ((term.getEntityType()!= null) || (term.getMentionEntityType()!= null) ) )
						
							//if ( (term.getRawString().length() > 5) || (localRelatedness > 0.2) )
						if (!skip) {
							if (term.getNerd_score() > 1.9) {
								finalResult.add(term);
							}
							else if ( ( ( (term.getSelectionScore() > maxSelect*0.2) || (term.getSelectionScore() == 0.0) )
								&& (term.getSelectionScore() > 0.12) )  || (term.getEntitySubTypes() != null)
								) {
								finalResult.add(term);
							}
						}
					}
					else {	
						System.out.println("is subterm " + term.getRawString());
						boolean skip = false;
						// we remove sub-term in subterm with the same FreeBase type
						if ( (freebaseHighTypeGlobal != null) 
							 && (term.getFreebaseHighTypes() != null) 
							 && (term.getFreebaseHighTypes().size() > 0) ) {
											
							for (String typ : term.getFreebaseHighTypes()){							
								if (freebaseHighTypeGlobal.contains(typ)) {
									skip = true;
									break;
								}
							}
							if (term.getFreebaseHighTypes().contains("/organization") &&
							   (freebaseHighTypeGlobal.contains("/business") || 
								freebaseTypeGlobal.contains("/computer/software") ) ) {
								// remove organization as sub-term of /business/consumer_product and related
								skip = true;
							}
							
							// check consistency
							if (term.getEntityType() != null) {
								if (term.getEntityType().equals("institution/N2") && 
									!term.getFreebaseHighTypes().contains("/organization") ) {
									skip = true;	
								}
							}
							
							// do not consider typical modifiers
							if (term.getEntitySubTypes() != null) {
 								if (term.getEntitySubTypes().contains("state/N2") ||
									term.getEntitySubTypes().contains("municipality/N1")) {
									skip = true;
								}
								if (term.getEntitySubTypes().contains("musical_composition/N1")) {
									// /music/composition is not in the ERD set
									skip = true;
								}
								
							}
							
						}
						
						// semantic relatedness has to be higher than a given threashold
						if (!skip) {
							if ( (term.getSelectionScore() > 0.011) 
							    && (term.getSelectionScore() > maxSelect*0.9) 
							 	&& (localRelatedness > 0.50) )
							{
								finalResult.add(term);
							}
						}
					}
				}
				p++;
			}
			
System.out.println("final: " + finalResult.toString());
			// final pruning based on relatedness 
			terms = new ArrayList<NerdCandidate>();
			if (finalResult.size() > 1) {
				for(NerdCandidate term : finalResult) {
					String[] tokens = term.getRawString().split(" ");
					if (term.getRawString().length() == text.length()) {
						terms.add(term);
					}
					else if (term.getNerd_score() > 1.9) {
						terms.add(term);
					}
					else if ( ((term.getEntitySubTypes() == null) || (term.getEntitySubTypes().size() == 0)  || 
							   (term.getEntityType() == null)) 
						&& (term.getCoarseConfidence() > 0.9) ) {
				//		&& (term.getCoarseConfidence() > 0.7) ) {
						continue;
					}					
					else if ( (tokens.length > 2) && (term.getRelatednessScore() > 0.05) ) {
						terms.add(term);
					}
				//	else if ( (term.getProb_c() < 0.3) && (term.getProb_c() != 0) ) {
				//		continue;
				//	}
					else if ( (term.getRelatednessScore() > 0.12) || (term.getRelatednessScore() == 0.0) ) {
						terms.add(term);
					}
					
				}
			} 
			else if (finalResult.size() == 1) {
				NerdCandidate term = finalResult.get(0);
				if (term.getNerd_score() > 1.9) {
					terms.add(term);
				}
				else if ( ((term.getEntitySubTypes() == null) || (term.getEntitySubTypes().size() == 0)  || 
						   (term.getEntityType() == null)) 
					&& (term.getCoarseConfidence() > 0.9) ) {
					// nothing
				}
				else
					terms.add(term);
			}
				

			// we try to re-expand the surface string of the candidates given the prefered term
			// and the term variant, in order to have the best largest string fit
			for(NerdCandidate concept : result) {
				if (concept.getVariants() != null) {
					for (Variant variant : concept.getVariants()) {
						String variantString = variant.getTerm();
						System.out.println("variant: " + variantString);
						int ind = text.toLowerCase().indexOf(variantString.toLowerCase());
						if ( (ind != -1) && (variantString.length() > concept.getRawString().length()) ) {
							concept.setRawString(text.substring(ind, ind+variantString.length()));
							OffsetPosition pos2 = new OffsetPosition(); 
							pos2.start = ind;
							pos2.end = pos2.start + variantString.length();
							concept.setPosition(pos2);
						}
					}
				}
			}
			// last conservative possible prunning
			//result = prune(result);
			
			//return finalResult;
		
		
		return terms;
	}*/
	
}


