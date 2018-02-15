package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Article;
import com.scienceminer.nerd.kb.model.Label;
import com.scienceminer.nerd.mention.Mention;
import com.scienceminer.nerd.utilities.NerdConfig;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class represents a context to be exploited for performing a disambiguation. 
 * 
 *
 */
public class NerdContext {
	
	// the selected entities modeling the context
	protected List<Article> contextArticles = null;
	protected List<Integer> contextArticlesIds = null;
	
	// working acronyms for this context
	protected Map<Mention, Mention> acronyms = null;

	// the list of mentions encountered so far in the current "session"
	// this is useful for future co-reference resolutions
	protected List<Mention> mentionRepository = null;

	// the list of disambiguated entities produced so far in the current "session"
	// this can be used for adding features, reimforcement or various post-processing
	protected List<Mention> entityRepository = null;

	protected double totalWeight = 0.0;
	protected Relatedness relatedness = Relatedness.getInstance();
	protected String lang = null;
	private Map<String, LowerKnowledgeBase> wikipediaConfs;

	public NerdContext() {
		wikipediaConfs = UpperKnowledgeBase.getInstance().getWikipediaConfs();
	}

	public NerdContext(String lang) {
		this();
		this.lang = lang;
	}

	public NerdContext(List<Label.Sense> unambig, 
					List<Article> certainPages,
					String lang) {
		this(lang);

		final NerdConfig config = wikipediaConfs.get(lang).getConfig();

		List<Article> articles = new ArrayList<>();
		for(Label.Sense sense : unambig) {
			double sp = sense.getPriorProbability();
			
			if (sp < config.getMinSenseProbability())
				continue; 
			
			if (isDate(sense) || isNumber(sense)) 
				continue;
			
			sense.setWeight(sp);
			if (!articles.contains(sense)) {
				articles.add(sense);
			}
		}
		// add the "certain" pages
		if (certainPages != null) {
			for(Article page : certainPages) {
				page.setWeight(new Double(1.0));
				articles.add(page);
			}
		}
		
		Collections.sort(articles);		
		contextArticles = new ArrayList<>();
		contextArticlesIds = new ArrayList<>();
		int c = 0;
		for (Article art: articles) {
			if (c >= NerdEngine.maxContextSize)
				break;
			if (!contextArticlesIds.contains(new Integer(art.getId()))) {
				contextArticles.add(art);
				c++;
				contextArticlesIds.add(new Integer(art.getId()));
			}
		}
	}
	
	public void addSense(Label.Sense sense) {
		if (sense == null)
			return;
		if (contextArticles == null) {
			contextArticles = new ArrayList<>();
			contextArticlesIds = new ArrayList<>();
		}

		final NerdConfig config = wikipediaConfs.get(lang).getConfig();
		
		double sp = sense.getPriorProbability();

		if (sp < config.getMinSenseProbability())
			return; 
		
		if (isDate(sense) || isNumber(sense)) 
			return;

		sense.setWeight(sp);	
		if (!contextArticlesIds.contains(new Integer(sense.getId()))) {
			contextArticles.add(sense);
			contextArticlesIds.add(new Integer(sense.getId()));
		}
	}
	
	protected void addArticle(Article article) {
		if (article == null)
			return;
		if (contextArticles == null) {
			contextArticles = new ArrayList<>();
			contextArticlesIds = new ArrayList<>();
		}

		final NerdConfig config = wikipediaConfs.get(lang).getConfig();
		
		double sp = article.getWeight();

		if (sp < config.getMinSenseProbability())
			return; 
		
		if (isDate(article) || isNumber(article)) 
			return;
		
		if (!contextArticlesIds.contains(new Integer(article.getId()))) {
			contextArticles.add(article);
			contextArticlesIds.add(new Integer(article.getId()));
		}
	}
	
	public List<Article> getArticles() {
		return contextArticles;
	}
	
	public int getSenseNumber() {
		if (contextArticles == null)
			return 0;
		return 
			contextArticles.size();
	}

	public double getQuality() {
		if ((contextArticles == null) || (contextArticles.size() == 0)) 
			return 0.0;

		if (totalWeight != 0.0)
			return totalWeight;
//System.out.println("size of context: " + contextArticles.size());
		for (Article art : contextArticles) {
			double avgRelatedness = 0.0;
			for (Article art2 : contextArticles) {
				if (art.getId() != art2.getId()) {
					try {
						avgRelatedness += relatedness.getRelatedness(art, art2, lang);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			avgRelatedness = avgRelatedness / (contextArticles.size() - 1);
			if (art.getWeight() != null)
				art.setWeight((art.getWeight().doubleValue() + (4*avgRelatedness)) / 5);
			else
				art.setWeight(((4*avgRelatedness)) / 5);
			totalWeight += art.getWeight();
		}

		return totalWeight;		
	}	

	public double getRelatednessTo(Article art) throws Exception {
		totalWeight = getQuality();
		if ((contextArticles == null) || (contextArticles.size() == 0) || (totalWeight == 0.0))
			return 0.0;

		double relatednessScore = 0.0;
		for (Article contextArt: contextArticles) { 
			double r = relatedness.getRelatedness(art, contextArt, lang);
			r = r * contextArt.getWeight();
			relatednessScore = relatednessScore + r;
		}
		
		return relatednessScore / totalWeight;
	}
	
	public static boolean isDate(Article art) {
		String title = art.getTitle();
		if (title == null)
			return false;
		
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM d"); 
		// to be reviewed with actual wikipedia dates in title
		Date date = null;
		try {
			date = sdf.parse(title);
		} catch (ParseException e) {
		}

		if (date != null)
			return true;	
		else 
			return false;		
	}

	public static boolean isNumber(Article art) {
		String title = art.getTitle();
		if (title == null)
			return false;

		Integer number = null;
		try { 	
			number = Integer.parseInt(title);
		} catch (Exception e) {
		}

		if (number != null)
			return true;

		Double doub = null;
		try { 	
			doub = Double.parseDouble(title);
		} catch (Exception e) {
		}

		if (doub != null)
			return true;
		else 
			return false;	
	}

	/**
	 * Merge the current context articles with the NerdContext given as parameter
	 */
	public void merge(NerdContext context) {
		if (contextArticles != null) {
			for(Article article : contextArticles) {
				context.addArticle(article);
			}
		}
	}
	
	/**
	 * @return true if the candidate sense is present in the relatedness context
	 */
	public boolean contains(NerdCandidate candidate) {
		Integer entityId = candidate.getWikipediaExternalRef();
		return contextArticlesIds.contains(new Integer(entityId));
	}

	public Map<Mention, Mention> getAcronyms() {
		return this.acronyms;
	} 

	public void setAcronyms(Map<Mention, Mention> acronyms) {
		this.acronyms = acronyms;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (contextArticles != null) {
			for(Article article : contextArticles) {
				builder.append(article.getTitle() + "\t" + article.getId() + "\t" + article.getWeight() + "\n");
			}
		}
		return builder.toString();
	}
}