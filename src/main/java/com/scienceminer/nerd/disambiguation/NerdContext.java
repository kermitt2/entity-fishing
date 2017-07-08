package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.data.Entity;
import org.grobid.core.data.Sense;

import java.util.*; 
import java.text.*;
import java.text.*;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.model.*;

import com.fasterxml.jackson.core.io.*;

/**
 * This class represents a context to be exploited for performing a disambiguation. 
 * 
 *
 */
public class NerdContext {
	
	protected List<Article> contextArticles = null;
	protected List<Integer> contextArticlesIds = null;
	
	protected double totalWeight = 0.0;
	protected Relatedness relatedness = Relatedness.getInstance();
	protected String lang = null;
	
	public NerdContext() {}

	public NerdContext(String lang) {
		this.lang = lang;
	}

	public NerdContext(List<Label.Sense> unambig, 
					List<Article> certainPages,
					String lang) throws Exception {
		this.lang = lang;
		
		List<Article> articles = new ArrayList<Article>();
		for(Label.Sense sense : unambig) {
			double sp = sense.getPriorProbability();
			
			if (sp < NerdEngine.minSenseProbability) 
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
		contextArticles = new ArrayList<Article>(); 
		contextArticlesIds = new ArrayList<Integer>(); 
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
			contextArticles = new ArrayList<Article>();
			contextArticlesIds = new ArrayList<Integer>(); 
		}
		
		double sp = sense.getPriorProbability();

		if (sp < NerdEngine.minSenseProbability) 
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
			contextArticles = new ArrayList<Article>();
			contextArticlesIds = new ArrayList<Integer>(); 
		}
		
		double sp = article.getWeight();

		if (sp < NerdEngine.minSenseProbability) 
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
		for(Article article : contextArticles) {
			context.addArticle(article);
		}
	}
	
	/**
	 * @return true if the candidate sense is present in the relatedness context
	 */
	public boolean contains(NerdCandidate candidate) {
		Integer entityId = candidate.getWikipediaExternalRef();
		return contextArticlesIds.contains(new Integer(entityId));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(Article article : contextArticles) {
			builder.append(article.getTitle() + "\t" + article.getId() + "\t" + article.getWeight() + "\n");
		}
		return builder.toString();
	}
}