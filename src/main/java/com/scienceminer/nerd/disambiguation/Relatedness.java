package com.scienceminer.nerd.disambiguation;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.utilities.TextUtilities;
import com.scienceminer.nerd.kb.Lexicon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.annotation.*;
import org.wikipedia.miner.util.text.*;
import org.grobid.core.utilities.OffsetPosition;

/**
 * Provide semantic relatedness measures, which is an adaptation of the original Relateness measure from 
 * Milne and Witten.
 * 
 * @author Patrice Lopez
 * 
 */
public class Relatedness {
	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(Relatedness.class);

	private static volatile Relatedness instance = null;
		
	// all the maps use the language code as a key
	private Map<String, Wikipedia> wikipedias = null;
	//private Map<String, NerdRanker> disambiguators = null;
	private Map<String, ArticleComparer> articleComparers = null;
	private Map<String, ConcurrentMap<Long,Double>> caches = null;

	private long comparisonsRequested = 0;
	private long comparisonsCalculated = 0;
		
	public static Relatedness getInstance() throws Exception {
	    if (instance == null) {
			getNewInstance();	        
	    }
	    return instance;
	}

	/**
	 * Creates a new instance.
	 */
	private static synchronized void getNewInstance() throws Exception {
		LOGGER.debug("Get new instance of Relatedness");		
		instance = new Relatedness();
	}

	/**
	 * Hidden constructor
	 */
	private Relatedness() throws Exception {	
		wikipedias = Lexicon.getInstance().getWikipediaConfs();
		caches = new HashMap<String, ConcurrentMap<Long,Double>>();
		//disambiguators = new HashMap<String, NerdRanker>();
		articleComparers = new HashMap<String, ArticleComparer>();
	}

	/*public Map<String, NerdRanker> getRankers() {
		return disambiguators;
	}*/

	/**
	 * Given a Wikipedia article and a set of context article collected from the
	 * same text, this method computes the article's average semantic
	 * relatedness to the context
	 * 
	 * @param article
	 * @param contextArticles
	 * @param lang the language to be considered
	 * @return double value between 0 and 1 measuring the semantic relatedness
	 */
	public double getRelatednessTo(NerdCandidate candidate, NerdContext context, String lang) {
		double totalRelatedness = 0.0;
		double currentRelatedness = 0.0;
		int totalComparisons = 0;

		if (context == null) {
			return 0.0;
		}

		List<Article> contextArticles = context.getArticles();

		Label.Sense sense = candidate.getWikiSense();
		Article article = null;
		if (sense.getType() == Page.PageType.article)
		 	article = (Article)sense;
		else 
			return 0.0;

		if ( (contextArticles == null) || (contextArticles.size() == 0) ) {
			return 0.0;
		} 
		
		if (article == null) {
			return 0.0;
		}
		for (Article contextArticle : contextArticles) {
			if (article.getId() != contextArticle.getId()) {
				currentRelatedness = 0.0;
				try {
					currentRelatedness = getRelatedness(article, contextArticle, lang);
				} 
				catch (Exception e) {
					System.out.println("Error computing semantic relatedness for "
							+ article + " and " + contextArticle);
					e.printStackTrace();
				}
				totalRelatedness += currentRelatedness;
				totalComparisons++;
			}
		}
		if (totalComparisons == 0) {
			return 0.0;
		}
		return totalRelatedness / totalComparisons;
	}

	public double getRelatedness(Article art1, Article art2, String lang) throws Exception {
		comparisonsRequested++;
		
		//generate unique key for the pair of articles
		int min = Math.min(art1.getId(), art2.getId());
		int max = Math.max(art1.getId(), art2.getId());
		//long key = min + (max << 30);
//System.out.println(min + " / " + max);
		long key = (((long)min) << 32) | (max & 0xffffffffL);
//System.out.println(key);
		double relatedness = 0.0;
		ConcurrentMap<Long,Double> cache = caches.get(lang);
		if (cache == null) {
			cache = new ConcurrentHashMap<Long,Double>();
			caches.put(lang, cache);
		}
		if (!cache.containsKey(new Long(key))) {
			ArticleComparer articleComparer = articleComparers.get(lang);
			if (articleComparer == null) {
				Wikipedia wikipedia = wikipedias.get(lang);
				articleComparer = new ArticleComparer(wikipedia);
				articleComparers.put(lang, articleComparer);
			}
			relatedness = articleComparer.getRelatedness(art1, art2);		
			cache.put(new Long(key), new Double(relatedness));
			
			comparisonsCalculated++;
		} else {
			relatedness = cache.get(new Long(key)).doubleValue();
		}
//System.out.println("obtained relatedness: " + relatedness);
		return relatedness;
	}

	/**
	 * Given a set of candidate entities extracted from the text, computes which
	 * one of these are the least ambiguous ones. From this, creates a vector of Wikipedia
	 * articles representing their senses. This list is computed for a given target candidate
	 * which has to be excluded.
	 * 
	 * @param candidates all the candidates entities
	 * @param targetCandidate the target candidate
	 * @param lang the language to be considered
	 * @return vector of context articles
	 */
	public Vector<Article> collectContextTerms(List<NerdCandidate> candidates, 
											NerdCandidate targetCandidate,
											String lang) {

		// vector to store unambiguous context articles
		Vector<Article> context = new Vector<Article>();

		int contextSize = 10;

		// vector to store senses of ambiguos candidates and sort them by
		// probability
		List<Article> bestCandidateSenses = new ArrayList<Article>();

		Wikipedia wikipedia = wikipedias.get(lang);
		for (NerdCandidate candidate : candidates) {
			if (candidate != targetCandidate) {
				int bestSense = candidate.getWikipediaExternalRef();
				Label label = wikipedia.getLabel(candidate.getPreferredTerm());
				if (label == null) {
					label = wikipedia.getLabel(candidate.getEntity().getRawName());
				}
				Article bestArticle = null;
				if (bestSense == -1) {
					bestArticle = wikipedia.getArticleByTitle(candidate.getEntity().getRawName());
					if (bestArticle == null) {
						TextProcessor tp = new org.wikipedia.miner.util.text.CaseFolder();
						bestArticle = wikipedia.getMostLikelyArticle(candidate.getEntity().getRawName(), tp);
					}
						
					if (bestArticle != null) {
						//bestSense = bestArticle.getTitle().replace(" ", "_");
						bestSense = bestArticle.getId();
					}
				}
				if (bestSense == -1)
					continue;
				if (bestArticle == null) {
					//bestArticle = wikipedia.getArticleByTitle(bestSense.replace("_", " "));	
					bestArticle = (Article)wikipedia.getPageById(bestSense);
				}
				if (bestArticle == null) 
					continue;
				try {

					// if required number of context articles
					// is reached, break
					/*if (context.size() >= contextSize) {
						break;
					}*/
					// what is the most likely sense for the given candidate
					//Sense bestSense = anchor.getSenses().first();

			//		double comonness = bestSense.getProbability();
			//		if (comonness == 0)
					double commonness = candidate.getProb_c();
					//double comonness = candidate.getProb_i();
			
					String termText = candidate.getPreferredTerm();
					if (termText == null) {
						termText = candidate.getEntity().getRawName();
					}
					
					double keyphraseness = label.getLinkProbability();
			
					// add to the context all articles that map
					// from ngrams with one possible meaning
					// and high keyphrasenesss
					//if ((anchor.getSenses().size() == 1) && (keyphraseness >= 0.5)) {
					if (keyphraseness >= 0.4) 
					{	
						if (context.contains(bestArticle)) {
							continue;
						}
						context.add(bestArticle);
					}

					// in case if not enough non-ambigious terms were collected
					// additionally collect other mappings based on
					// sense probability and keyphraseness 
					if ((commonness >= 0.5) && (keyphraseness > 0.1)) {
						bestArticle.setWeight(commonness);
						bestCandidateSenses.add(bestArticle);
					}

				} 
				catch (Exception e) {
					System.out.println("Error computing senses for " + candidate.toString());
					e.printStackTrace();
				}
			}
		}

		// if not enough context was collected
		//if (context.size() < contextSize) {
		if (context.size() == 0) { 	
			// fill up context anchors with most likely mappings
			for (int i = 0; (i < bestCandidateSenses.size()) && (context.size() < contextSize); i++) {
				Article sense = bestCandidateSenses.get(i);
		//		System.out.println("Adding best from ambiguous " + sense);
				context.add(sense);
				break;
			}
		}
		return context;
	}
	
	public Set<Article> collectAllContextTerms(List<NerdCandidate> candidates, String lang) {
		// vector to store unambiguous context articles
		Set<Article> context = new HashSet<Article>();

		// vector to store senses of ambiguos candidates and sort them by
		// probability
		//List<Article> bestCandidateSenses = new ArrayList<Article>();

		Wikipedia wikipedia = wikipedias.get(lang);
		for (NerdCandidate candidate : candidates) {
			
			int bestSense = candidate.getWikipediaExternalRef();
			if (bestSense == -1)
				continue;
			//Article bestArticle = wikipedia.getArticleByTitle(bestSense.replace("_", " "));	
			Article bestArticle = (Article)wikipedia.getPageById(bestSense);	
			if (bestArticle == null) 
				continue;
			try {

				// if required number of context articles
				// is reached, break

				// what is the most likely sense for the given candidate
				//Sense bestSense = anchor.getSenses().first();

				//double comonness = bestSense.getProbability();
				//double comonness = candidate.getProb_i();
				/*String termText = candidate.getPreferredTerm();
				if (termText == null) {
					termText = candidate.getRawName();
				}
				Label label = wikipedia.getLabel(termText);
				//double keyphraseness = label.getLinkProbability();
				*/
				if (context.contains(bestArticle)) {
					continue;
				}
				context.add(bestArticle);
			} 
			catch (Exception e) {
				System.out.println("Error computing senses for " + candidate.toString());
				e.printStackTrace();
			}
		}

		return context;
	}
	
	/**
     *  Get a Wikipedia-miner context from a text based on the unambiguous labels and the 
	 *  certain disambiguated entities. 
	 *  	 
	 */	
	public NerdContext getContext(Map<NerdEntity, List<NerdCandidate>> candidates, 
							List<NerdEntity> userEntities, 
							String lang) throws Exception {
		List<Label.Sense> unambig = new ArrayList<Label.Sense>();
		List<Integer> unambigIds = new ArrayList<Integer>();
		
		List<Label.Sense> extraSenses = new ArrayList<Label.Sense>();
		List<Integer> extraSensesIds = new ArrayList<Integer>();
		
		Wikipedia wikipedia = wikipedias.get(lang);

		// we add the "certain" senses
		List<Article> certainPages = new ArrayList<Article>();
		List<Integer> certainPagesIds = new ArrayList<Integer>();

		if ( (userEntities != null) && (userEntities.size() > 0) ){
			for(NerdEntity ent : userEntities) {
				if (ent.getWikipediaExternalRef() != -1) {
					//resultContext.addPage(wikipedia.getPageById(ent.getWikipediaExternalRef()));
					Page thePage = wikipedia.getPageById(ent.getWikipediaExternalRef());
					if (thePage.getType() == Page.PageType.article) {
						if (!certainPagesIds.contains(new Integer(thePage.getId()))) {
							certainPages.add((Article)thePage);
							certainPagesIds.add(new Integer(thePage.getId()));
						}
					}
				}
			}
		}
//System.out.println(certainPages.size()+ " certain entities; " + certainPages.size());		
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();

			if ( (cands == null) || (cands.size() == 0) )
				continue;
			else if (cands.size() == 1) { 
				if (!unambigIds.contains(new Integer(cands.get(0).getWikiSense().getId()))) {
					Label.Sense theSense = cands.get(0).getWikiSense();
					unambig.add(theSense);
					unambigIds.add(new Integer(theSense.getId()));
				}
			} else {
				for(NerdCandidate cand : cands) {
					if (cand.getProb_c() >= (1-NerdEngine.minSenseProbability)) {
						Label.Sense theSense = cands.get(0).getWikiSense();
						if (!unambigIds.contains(new Integer(theSense.getId()))) {
							unambig.add(theSense);
							unambigIds.add(new Integer(theSense.getId()));
						}
						//extraSenses.add(cands.get(0).getWikiSense());
						break;
					}
					else if (cand.getProb_c() >= 0.8) {
						Label.Sense theSense = cands.get(0).getWikiSense();
						if ( !extraSensesIds.contains(new Integer(theSense.getId())) && 
							!unambigIds.contains(new Integer(theSense.getId())) ) {
							extraSenses.add(theSense);
							extraSensesIds.add(new Integer(theSense.getId()));
						}
						break;
					}
				}
			}
			if (unambig.size()+certainPages.size() > NerdEngine.maxContextSize)
				break;
		}
//System.out.println(unambig.size()+ " unambiguous entities; " + unambig.size());		
		// if the context is still too small, we add the best senses of ambiguous labels
		if (unambig.size()+certainPages.size() < NerdEngine.maxContextSize) {
			if ((extraSenses != null) && (extraSenses.size() > 0)) {
				for(Label.Sense sense : extraSenses) {
					Integer theId = new Integer(sense.getId());
					if (!unambigIds.contains(theId)) {
						unambig.add(sense);
						unambigIds.add(theId);
					}
					if (unambig.size()+certainPages.size() > NerdEngine.maxContextSize)
						break;
				}
			}
		}

		NerdContext resultContext = new NerdContext(unambig, certainPages, lang);
		
		return resultContext;
	}

	/**
     *  Get a context from a text based on the unambiguous labels and the certain disambiguated entities. 
	 *  	 
	 */	
	public NerdContext getContextFromText(String content, 
							List<NerdEntity> userEntities, 
							String lang) throws Exception{
		List<Label.Sense> unambig = new ArrayList<Label.Sense>();

		//String targetString = content.substring(entity.getOffsetStart(), entity.getOffsetEnd());

		String s = "$ " + content + " $";

		Pattern p = Pattern.compile("[\\s\\{\\}\\(\\)\"\'\\.\\,\\;\\:\\-\\_]");  
			//would just match all non-word chars, but we dont want to match utf chars
		Matcher m = p.matcher(s);

		List<Integer> matchIndexes = new ArrayList<Integer>();
		List<Label.Sense> extraSenses = new ArrayList<Label.Sense>();
		Wikipedia wikipedia = wikipedias.get(lang);

		while (m.find()) 
			matchIndexes.add(m.start());

		for (int i=0; i<matchIndexes.size(); i++) {

			int startIndex = matchIndexes.get(i) + 1;

			for (int j=Math.min(i + NerdEngine.maxLabelLength, matchIndexes.size()-1); j > i; j--) {
				int currIndex = matchIndexes.get(j);	
				String ngram = s.substring(startIndex, currIndex);

				/*if (ngram.indexOf(targetString) != -1)
					continue;
				
				if (targetString.indexOf(ngram) != -1)
					continue;
				*/
				if (! (ngram.length()==1 && s.substring(startIndex-1, startIndex).equals("'")) && 
						!ngram.trim().equals("")) {
					//Label label = new Label(wikipedia.getEnvironment(), ngram, tp);
					Label label = new Label(wikipedia.getEnvironment(), ngram);

					if (label.getLinkProbability() > NerdEngine.minLinkProbability) {
						
						Label.Sense[] senses = label.getSenses();
						
						if ( senses.length == 1 || 
							(senses[0].getPriorProbability() >= (1-NerdEngine.minSenseProbability)) ) 
							unambig.add(senses[0]);
						
						// we store some extra senses if needed
						if ( (senses.length > 1) && (senses[0].getPriorProbability() >= 0.8 ) ) {
							//if ( senses.length > 1 )	
							extraSenses.add(senses[0]);
						}
					}
				}
			}
		}
		
		// we add the "certain" senses
		List<Article> certainPages = new ArrayList<Article>();
		for(NerdEntity ent : userEntities) {
			if (ent.getWikipediaExternalRef() != -1) {
				//resultContext.addPage(wikipedia.getPageById(ent.getWikipediaExternalRef()));
				Page thePage = wikipedia.getPageById(ent.getWikipediaExternalRef());
				if (thePage.getType() == Page.PageType.article) {
					certainPages.add((Article)thePage);
				}
			}
		}

		NerdContext resultContext = new NerdContext(unambig, certainPages, lang);

		// if the context is still too small, we had the best senses of ambiguous labels
		if (resultContext.getSenseNumber() < (NerdEngine.maxContextSize/2)) {
			for(Label.Sense sense : extraSenses) {
				resultContext.addSense(sense);
				if (resultContext.getSenseNumber() > (NerdEngine.maxContextSize/2))
					break;
			}
		}
		return resultContext;
	}
	
	/**
     *  Get a context from a vector of terms based on the unambiguous labels and the 
	 *  certain disambiguated entities. 
	 *  	 
	 */	
	public NerdContext getContext(List<WeightedTerm> terms, 
							List<NerdEntity> userEntities,
							String lang) throws Exception{
		Vector<Label.Sense> unambig = new Vector<Label.Sense>();
		List<Label.Sense> extraSenses = new ArrayList<Label.Sense>();
		Wikipedia wikipedia = wikipedias.get(lang);
		for (WeightedTerm term : terms) {

			String termString = term.getTerm();

			if ((termString.length()!=1) && (!termString.trim().equals(""))) {
				//Label label = new Label(wikipedia.getEnvironment(), ngram, tp);
				Label label = new Label(wikipedia.getEnvironment(), termString);

				if (label.getLinkProbability() > NerdEngine.minLinkProbability) {
					
					Label.Sense[] senses = label.getSenses();
					
					if ( senses.length == 1 || (senses[0].getPriorProbability() >= (1-NerdEngine.minSenseProbability)) ) 
						unambig.add(senses[0]);
					
					// we store some extra senses if needed
					if ( senses.length > 1 && (senses[0].getPriorProbability() >= 0.8 ) ) {
						//if ( senses.length > 1 )	
						extraSenses.add(senses[0]);
					}
				}
			}
		}

		// we add the "certain" senses
		List<Article> certainPages = new ArrayList<Article>();
		for(NerdEntity ent : userEntities) {
			if (ent.getWikipediaExternalRef() != -1) {
				//resultContext.addPage(wikipedia.getPageById(ent.getWikipediaExternalRef()));
				Page thePage = wikipedia.getPageById(ent.getWikipediaExternalRef());
				if (thePage.getType() == Page.PageType.article) {
					certainPages.add((Article)thePage);
				}
			}
		}

		NerdContext resultContext = new NerdContext(unambig, certainPages, lang);

		// if the context is still too small, we had the best senses of ambiguous labels
		if (((NerdContext)resultContext).getSenseNumber() < NerdEngine.maxContextSize) {
			for(Label.Sense sense : extraSenses) {
				((NerdContext)resultContext).addSense(sense);
				if (((NerdContext)resultContext).getSenseNumber() == NerdEngine.maxContextSize)
					break;
			}
		}
		return resultContext;
	}
	
	public long getTermOccurrence(String text, String lang) {
		Wikipedia wikipedia = wikipedias.get(lang);
		Label label = wikipedia.getLabel(text);
		if (label != null)
			return label.getOccCount();
		else
			return 0;
	}
	
	public long getComparisonsCalculated() {
		return comparisonsCalculated;
	}
	
	public long getComparisonsRequested() {
		return comparisonsRequested;
	}
	
	public double getCachedProportion() {
		double p = (double)comparisonsCalculated/comparisonsRequested;
		return 1-p;
	}

	public void resetCache(String lang) {
		ConcurrentMap<Long,Double> cache = caches.get(lang);
		if (cache != null) {
			cache.clear();
		}
		comparisonsCalculated = 0;
		comparisonsRequested = 0;
	}

	public void close() {
		Iterator it = wikipedias.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        Wikipedia wikipedia = (Wikipedia)pair.getValue();
			wikipedia.close();
	        it.remove(); // avoids a ConcurrentModificationException
	   	}
	}

}