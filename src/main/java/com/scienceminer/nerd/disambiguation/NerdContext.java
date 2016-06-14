package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.data.Entity;
import org.grobid.core.data.Sense;
import com.scienceminer.nerd.kb.*;

import java.util.*; 
import java.text.*;
import java.text.*;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;

import org.codehaus.jackson.io.JsonStringEncoder;

/**
 * This class represents a context to be exploited for performing a disambiguation. 
 * 
 * @author Patrice Lopez
 *
 */
public class NerdContext {
	
	protected List<Article> contextArticles = null;
	protected List<Integer> contextArticlesIds = null;
	
	private double totalWeight = 0.0;
	private Relatedness relatedness = Relatedness.getInstance();
	private String lang = null;
	
	public NerdContext(List<Label.Sense> unambig, 
					List<Article> certainPages,
					String lang) throws Exception {
		this.lang = lang;
		
		List<Article> articles = new ArrayList<Article>();
		for(Label.Sense sense : unambig) {
			double sp = sense.getPriorProbability();
			
			//if below sp threshold, skip
			if (sp < NerdEngine.minSenseProbability) continue; 
			
			//if this is a date, skip
			if (isDate(sense)) continue;
			
			//sense.setWeight((lp + sp)/2);	
			sense.setWeight(sp);	
			
			if (!articles.contains(sense)) {
				articles.add(sense);
			}
		}
		// add the certain pages
		for(Article page : certainPages) {
			page.setWeight(new Double(1.0));
			articles.add(page);
		}
		
		//now weight candidates by their relatedness to each other
		/*for (Article art : articles) {
			double avgRelatedness = 0;
			for (Article art2:articles) {
				if (art.getId() != art2.getId()) {
					avgRelatedness += relatednessCache.getRelatedness(art, art2);
				}
			}
			
			avgRelatedness = avgRelatedness / (articles.size() - 1);
			art.setWeight((art.getWeight() + (4*avgRelatedness)) /5);
		}*/
		
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

		//if below sp threshold, skip
		if (sp < NerdEngine.minSenseProbability) return; 
		
		//if this is a date, skip
		if (isDate(sense)) return;

		sense.setWeight(sp);	
		if (!contextArticlesIds.contains(new Integer(sense.getId()))) {
			contextArticles.add(sense);
			contextArticlesIds.add(new Integer(sense.getId()));
		}
	}
	
	/*public void addPage(Page page) {
		if (page == null)
			return;
		if (contextArticles == null)
			contextArticles = new HashSet<Article>();
		if (page.getType() == Page.PageType.article) {
			contextArticles.add(((Article)page));
		}
	}*/
	
	public List<Article> getArticles() {
		return contextArticles;
	}
	
	public int getSenseNumber() {
		if (contextArticles == null)
			return 0;
		return 
			contextArticles.size();
	}

	/**
	 * @return the quality (size and homogeneity) of the available context. 
	 */
	/*public float getQuality() {
		return totalWeight ;		
	}*/

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
	
	private boolean isDate(Article art) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM d") ;
		Date date = null;
		try {
			date = sdf.parse(art.getTitle());
		} catch (ParseException e) {
			return false;
		}

		return (date != null);		
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