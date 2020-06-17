package com.scienceminer.nerd.disambiguation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.LowerKnowledgeBase.Direction;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Article;
import com.scienceminer.nerd.kb.model.Label;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.utilities.NerdConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.scienceminer.nerd.kb.UpperKnowledgeBase.TARGET_LANGUAGES;

/**
 * Provide semantic relatedness measures, which is an adaptation of the original Relateness measure from
 * Milne and Witten.
 *
 */
public class Relatedness {
	private static final Logger LOGGER = LoggerFactory.getLogger(Relatedness.class);

	private static volatile Relatedness instance = null;

	// all the maps use the language code as a key
	private Map<String, LowerKnowledgeBase> wikipedias = null;
	private Map<String, LoadingCache<ArticlePair, Double>> caches = null;

	private long comparisonsRequested = 0;
	private long comparisonsCalculated = 0;
	private final static int MAX_CACHE_SIZE = 5000000;


	public static Relatedness getInstance() {
	    if (instance == null) {
			getNewInstance();
	    }
	    return instance;
	}

	/**
	 * Creates a new instance.
	 */
	private static synchronized void getNewInstance() {
		LOGGER.debug("Get new instance of Relatedness");
		instance = new Relatedness();
	}

	/**
	 * Hidden constructor
	 */
	private Relatedness() {
		wikipedias = UpperKnowledgeBase.getInstance().getWikipediaConfs();
		caches = new HashMap<>();
		for (String lang : TARGET_LANGUAGES) {
			 caches.put(lang, CacheBuilder.newBuilder()
					.maximumSize(MAX_CACHE_SIZE)  // if cache reach the max, then remove the older elements
					.build(
							new CacheLoader<ArticlePair, Double>() {
								@Override
								public Double load(ArticlePair articlePair) throws Exception {
									return getRelatednessWithoutCache(articlePair.getArticleA(),articlePair.getArtticleB() ,lang);
								}
							}
					)
			 );
		}

	}

	/**
	 * Calculate the relatedness of a candidate with a context
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
		 	article = (Article) sense;
		else
			return 0.0;

		if ( (contextArticles == null) || (contextArticles.size() == 0) ) {
			// if there is no context, we can set an arbitrary score
			return 0.1;
		}

		for (Article contextArticle : contextArticles) {
			//if (article.getId() != contextArticle.getId()){
			currentRelatedness = 0.0;
			try {
				currentRelatedness = getRelatedness(article, contextArticle, lang);
			}
			catch (Exception e) {
				LOGGER.error("Error computing semantic relatedness for "
						+ article + " and " + contextArticle, e);
			}
			totalRelatedness += currentRelatedness;
			totalComparisons++;
			//}
		}
		if (totalComparisons == 0) {
			return 0.0;
		}
		return totalRelatedness / totalComparisons;
	}

	/**
	 * Calculate the relatedness between two articles
	 */
	public double getRelatedness(Article art1, Article art2, String lang) throws ExecutionException{
		comparisonsRequested++;

		LoadingCache<ArticlePair, Double> relatednessCache = caches.get(lang);
		return relatednessCache.get(new ArticlePair(art1,art2));
	}


	public double getRelatednessWithoutCache(Article artA, Article artB, String lang) {
		comparisonsCalculated++;
		if (artA.getId() == artB.getId())
			return 1.0;

		LowerKnowledgeBase wikipedia = wikipedias.get(lang);
		NerdConfig conf = wikipedia.getConfig();

		EntityPairRelatedness epr = getEntityPairRelatedness(artA, artB, wikipedia);
		if (epr == null)
			return 0.0;

		if ( (epr.getInLinkIntersectionProportion() == 0.0) && (epr.getOutLinkIntersectionProportion() == 0.0) )
			return 0.0;

		//System.out.println("gi " + epr.getInLinkmilneWittenMeasure());
		//System.out.println("go " + epr.getOutLinkmilneWittenMeasure());
		int count = 0;
		double total = 0.0;

		count++;
		total = total + epr.getInLinkMilneWittenMeasure();

		if (conf.getUseLinkOut()) {
			count++;
			total = total + epr.getOutLinkMilneWittenMeasure();
		}

		if (count == 0)
			return 0.0;
		else
			return total/count;
	}

	public EntityPairRelatedness getEntityPairRelatedness(Article artA, Article artB, LowerKnowledgeBase wikipedia) {
		EntityPairRelatedness epr = new EntityPairRelatedness(artA, artB);
		NerdConfig conf = wikipedia.getConfig();

		epr = setPageLinkFeatures(epr, Direction.In, wikipedia);
		if (conf.getUseLinkOut())
			epr = setPageLinkFeatures(epr, Direction.Out, wikipedia);

		if (!epr.inLinkFeaturesSet() && !epr.outLinkFeaturesSet())
			return null;

		return epr;
	}

	/**
	 *  Following Milne anf Witten relatedness measurement as implemented in WikipediaMiner
	 */
	private EntityPairRelatedness setPageLinkFeatures(EntityPairRelatedness epr, Direction dir, LowerKnowledgeBase wikipedia) {
		if (epr.getArticleA().getId() == epr.getArticleB().getId()) {
			// nothing to do
			return epr;
		}

		List<Integer> linksA = wikipedia.getLinks(epr.getArticleA().getId(), dir);
		List<Integer> linksB = wikipedia.getLinks(epr.getArticleB().getId(), dir);

		//we can't do anything if there are no links
		if (linksA.isEmpty() || linksB.isEmpty())
			return epr;

		NerdConfig conf = wikipedia.getConfig();
//System.out.println("#linksA: " + linksA.size() + " / " + "#linksB: " + linksB.size());

		int intersection = 0;
		//int sentenceIntersection = 0;
		int union = 0;

		int indexA = 0;
		int indexB = 0;

		List<Double> vectA = new ArrayList<>();
		List<Double> vectB = new ArrayList<>();

		while (indexA < linksA.size() || indexB < linksB.size()) {
			//identify which links to use (A, B, or both)
			boolean useA = false;
			boolean useB = false;
			boolean mutual = false;

			Integer linkA = null;
			Integer linkB = null;

			if (indexA < linksA.size())
				linkA = linksA.get(indexA);

			if (indexB < linksB.size())
				linkB = linksB.get(indexB);

			if ( (linkA != null) && (linkB != null) && (linkA.equals(linkB)) ) {
				useA = true;
				useB = true;
				intersection ++;
			} else {
				if (linkA != null && (linkB == null || linkA < linkB)) {
					useA = true;
					if (linkA.equals(epr.getArticleB().getId())) {
						intersection++;
						mutual = true;
					}

				} else {
					useB = true;
					if (linkB.equals(epr.getArticleA().getId())) {
						intersection++;
						mutual = true;
					}
				}
			}
			union++;

			if (useA)
				indexA++;
			if (useB)
				indexB++;
		}

		// this is the famous Milne & Witten relatedness measure
		double milneWittenMeasure = 1.0;
		if (intersection == 0) {
			milneWittenMeasure = 1.0;
		} else {
			double a = Math.log(linksA.size());
			double b = Math.log(linksB.size());
			double ab = Math.log(intersection);

			double m = Math.log(wikipedia.getArticleCount());

			milneWittenMeasure = (Math.max(a, b) - ab) / (m - Math.min(a, b));
		}

		// normalization
		if (milneWittenMeasure >= 1)
				milneWittenMeasure = 0.0;
		else
			milneWittenMeasure = 1 - milneWittenMeasure;

		double intersectionProportion;
		if (union == 0)
			intersectionProportion = 0;
		else
			intersectionProportion = (double)intersection/union;

		if (dir == Direction.Out)
			epr.setOutLinkFeatures(milneWittenMeasure, intersectionProportion);
		else
			epr.setInLinkFeatures(milneWittenMeasure, intersectionProportion);

		return epr;
	}


	public Set<Article> collectAllContextTerms(List<NerdCandidate> candidates, String lang) {
		// unambiguous context articles
		Set<Article> context = new HashSet<Article>();
		LowerKnowledgeBase wikipedia = wikipedias.get(lang);
		for (NerdCandidate candidate : candidates) {
			int bestSense = candidate.getWikipediaExternalRef();
			if (bestSense == -1)
				continue;
			//Article bestArticle = wikipedia.getArticleByTitle(bestSense.replace("_", " "));
			Article bestArticle = (Article)wikipedia.getPageById(bestSense);
			if (bestArticle == null)
				continue;
			try {
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
     *  Get a context from a text based on the unambiguous labels and the
	 *  certain disambiguated entities.
	 *
	 */
	public NerdContext getContext(Map<NerdEntity, List<NerdCandidate>> candidates,
							List<NerdEntity> userEntities,
							String lang, boolean shortText) {
		List<Label.Sense> unambig = new ArrayList<>();
		List<Integer> unambigIds = new ArrayList<>();

		List<Label.Sense> extraSenses = new ArrayList<>();
		List<Integer> extraSensesIds = new ArrayList<>();

		LowerKnowledgeBase wikipedia = wikipedias.get(lang);
		double minSenseProbability = wikipedia.getConfig().getMinSenseProbability();

		// we add the "certain" senses
		List<Article> certainPages = new ArrayList<Article>();
		List<Integer> certainPagesIds = new ArrayList<Integer>();

		if ( (userEntities != null) && (userEntities.size() > 0) ){
			for(NerdEntity ent : userEntities) {

				if (ent.getWikipediaExternalRef() != -1) {
					Page thePage = wikipedia.getPageById(ent.getWikipediaExternalRef());
					if (thePage.getType() == Page.PageType.article) {
						if (!certainPagesIds.contains(thePage.getId())) {
							certainPages.add((Article)thePage);
							certainPagesIds.add(thePage.getId());
						}
					}
				}

				//TODO: wikidata id
			}
		}

		// first pass for non-ambiguous candidates
		for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
			List<NerdCandidate> cands = entry.getValue();
			NerdEntity entity = entry.getKey();

			if ( (cands == null) || (cands.size() == 0) )
				continue;
			else if (cands.size() == 1) {
				NerdCandidate cand = cands.get(0);
				if (cand.getProb_c() >= (1-minSenseProbability)) {
					// conditional prob of candidate sense must also be above the acceptable threshold
					if (!unambigIds.contains(cand.getWikiSense().getId())) {
						Label.Sense theSense = cands.get(0).getWikiSense();
						unambig.add(theSense);
						unambigIds.add(theSense.getId());
					}
				}
			}
			if (unambig.size()+certainPages.size() > NerdEngine.maxContextSize)
				break;
		}

		if (unambig.size()+certainPages.size() < NerdEngine.maxContextSize) {
		// second pass for not so ambiguous candidates
			for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
				List<NerdCandidate> cands = entry.getValue();
				NerdEntity entity = entry.getKey();

				if ( (cands == null) || (cands.size() == 0) )
					continue;
				else if (cands.size() == 1) {
					continue;
				} else {
					for(NerdCandidate cand : cands) {
						if (cand.getProb_c() >= (1-minSenseProbability)) {
							Label.Sense theSense = cands.get(0).getWikiSense();
							if (!unambigIds.contains(theSense.getId())) {
								unambig.add(theSense);
								unambigIds.add(theSense.getId());
							}
							//extraSenses.add(cands.get(0).getWikiSense());
							break;
						}
						/*else if (cand.getProb_c() >= 0.8) {
							// we store some extra "good" senses in case we need more of them
							Label.Sense theSense = cands.get(0).getWikiSense();
							if ( !extraSensesIds.contains(theSense.getId()) &&
								!unambigIds.contains(theSense.getId()) ) {
								extraSenses.add(theSense);
								extraSensesIds.add(theSense.getId());
							}
							break;
						}*/
					}
				}
				if (unambig.size()+certainPages.size() > NerdEngine.maxContextSize)
					break;
			}
		}

		// if the context is still too small, we add some of the top sense of ambiguous labels
		if (shortText) {
			if (unambig.size()+certainPages.size() < NerdEngine.maxContextSize) {
				if (CollectionUtils.isNotEmpty(extraSenses)) {
					for(Label.Sense sense : extraSenses) {
						Integer theId = sense.getId();
						if (!unambigIds.contains(theId)) {
							unambig.add(sense);
							unambigIds.add(theId);
						}
						if (unambig.size()+certainPages.size() > NerdEngine.maxContextSize)
							break;
					}
				}
			}
		}

		NerdContext resultContext = new NerdContext(unambig, certainPages, lang);

		return resultContext;
	}

	/**
     *  Get a context from a text based on the unambiguous labels and the certain disambiguated entities.
     *
     *  Note: To be removed !
	 *
	 */
	public NerdContext getContextFromText(String content,
							List<NerdEntity> userEntities,
							String lang) {
		List<Label.Sense> unambig = new ArrayList<>();

		//String targetString = content.substring(entity.getOffsetStart(), entity.getOffsetEnd());

		String s = "$ " + content + " $";

		Pattern p = Pattern.compile("[\\s\\{\\}\\(\\)\"\'\\.\\,\\;\\:\\-\\_]");
			//would just match all non-word chars, but we dont want to match utf chars
		Matcher m = p.matcher(s);

		List<Integer> matchIndexes = new ArrayList<>();
		List<Label.Sense> extraSenses = new ArrayList<>();
		LowerKnowledgeBase wikipedia = wikipedias.get(lang);
		double minSenseProbability = wikipedia.getConfig().getMinSenseProbability();
		double minLinkProbability = wikipedia.getConfig().getMinLinkProbability();

		while (m.find())
			matchIndexes.add(m.start());

		for (int i=0; i<matchIndexes.size(); i++) {

			int startIndex = matchIndexes.get(i) + 1;

			for (int j=Math.min(i + NerdEngine.maxLabelLength, matchIndexes.size()-1); j > i; j--) {
				int currIndex = matchIndexes.get(j);
				String ngram = s.substring(startIndex, currIndex);

				if (! (ngram.length()==1 && s.substring(startIndex-1, startIndex).equals("'")) &&
						!ngram.trim().equals("")) {
					Label label = new Label(wikipedia.getEnvironment(), ngram);
					if (label.getLinkProbability() > minLinkProbability) {
						Label.Sense[] senses = label.getSenses();
						if ( senses.length == 1 ||
							(senses[0].getPriorProbability() >= (1-minSenseProbability)) )
							unambig.add(senses[0]);

						// we store some extra senses in case the context is too small
						if ( (senses.length > 1) && (senses[0].getPriorProbability() >= 0.8 ) ) {
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
							String lang) {
		Vector<Label.Sense> unambig = new Vector<>();
		List<Label.Sense> extraSenses = new ArrayList<>();
		LowerKnowledgeBase wikipedia = wikipedias.get(lang);

		double minSenseProbability = wikipedia.getConfig().getMinSenseProbability();
		double minLinkProbability = wikipedia.getConfig().getMinLinkProbability();

		for (WeightedTerm term : terms) {

			String termString = term.getTerm();

			if ((termString.length()!=1) && (!termString.trim().equals(""))) {
				//Label label = new Label(wikipedia.getEnvironment(), ngram, tp);
				Label label = new Label(wikipedia.getEnvironment(), termString);

				if (label.getLinkProbability() > minLinkProbability) {

					Label.Sense[] senses = label.getSenses();

					if ( senses.length == 1 || (senses[0].getPriorProbability() >= (1-minSenseProbability)) )
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
		LowerKnowledgeBase wikipedia = wikipedias.get(lang);
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
		LoadingCache<ArticlePair,Double> cache = caches.get(lang);
		if (cache != null) {
			cache.invalidateAll();
		}
		comparisonsCalculated = 0;
		comparisonsRequested = 0;
	}

	public void close() {
		Iterator it = wikipedias.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        LowerKnowledgeBase wikipedia = (LowerKnowledgeBase)pair.getValue();
			wikipedia.close();
	        it.remove(); // avoids a ConcurrentModificationException
	   	}
	}

	/**
	 * A POJO for calculation of relatedness between two articles, not so useful maybe
	 */
	public class EntityPairRelatedness {

		private final Article articleA;
		private final Article articleB;

		private boolean inLinkFeaturesSet = false;
		private double inLinkMilneWittenMeasure = 0.0;
		private double inLinkIntersectionProportion = 0.0;

		private boolean outLinkFeaturesSet = false;
		private double outLinkMilneWittenMeasure = 0.0;
		private double outLinkIntersectionProportion = 0.0;

		public EntityPairRelatedness(Article artA, Article artB) {
			articleA = artA;
			articleB = artB;
		}

		public Article getArticleA() {
			return articleA;
		}

		public Article getArticleB() {
			return articleB;
		}

		public boolean inLinkFeaturesSet() {
			return inLinkFeaturesSet;
		}

		public double getInLinkMilneWittenMeasure() {
			return inLinkMilneWittenMeasure;
		}
		public double getInLinkIntersectionProportion() {
			return inLinkIntersectionProportion;
		}

		public boolean outLinkFeaturesSet() {
			return outLinkFeaturesSet;
		}

		public double getOutLinkMilneWittenMeasure() {
			return outLinkMilneWittenMeasure;
		}

		public double getOutLinkIntersectionProportion() {
			return outLinkIntersectionProportion;
		}

		public void setInLinkFeatures(double milneWittenMeasure, double intersectionProportion) {
			inLinkFeaturesSet = true;
			inLinkMilneWittenMeasure = milneWittenMeasure;
			inLinkIntersectionProportion = intersectionProportion;
		}

		public void setOutLinkFeatures(double milneWittenMeasure, double intersectionProportion) {
			outLinkFeaturesSet = true;
			outLinkMilneWittenMeasure = milneWittenMeasure;
			outLinkIntersectionProportion = intersectionProportion;
		}
	};

}