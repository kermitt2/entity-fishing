/*
 *    Context.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.wikipedia.miner.annotation;

import java.util.* ;
import java.text.* ;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;

/**
 * A selection of unambiguous terms and their corresponding articles, which are used to resolve ambiguous terms.
 * 
 * @author David Milne 
 */
public class Context {
		
	private Vector<Article> contextArticles ;
	private float totalWeight ;
	private RelatednessCache relatednessCache ;
	
	/**
	 * Initialises a collection of context articles from the given set of unambiguous anchors. 
	 * 
	 * @param unambigAnchors a set of unambiguous anchors, the most useful of which will be used to disambiguate other terms
	 * @param relatednessCache a cache in which relatedness measures will be saved so they aren't repeatedly calculated.
	 * @param maxSize the maximum number of anchors that will be used (the more there are, the longer disambiguation takes, but the more accurate it is likely to be).
	 * @throws Exception 
	 */ /*
	public Context(Collection<Label> unambigLabels, RelatednessCache relatednessCache, int maxSize) throws Exception {
		
		this.relatednessCache = relatednessCache ;
		
		HashSet<Integer> doneIds = new HashSet<Integer>() ;		
		Vector<Label.Sense> senses = new Vector<Label.Sense>() ;
		for (Label label: unambigLabels) {
			
			Label.Sense sense = label.getSenses()[0] ;	
			if (!isDate(sense) && !doneIds.contains(sense.getId())) {
				sense.setWeight(label.getLinkProbability()) ;
				senses.add(sense) ;
				doneIds.add(sense.getId()) ;
			}
		}
		
		TreeSet<Article> sortedContextArticles = new TreeSet<Article>() ;
		for (Label.Sense s:senses) {
			double linkProb = s.getWeight() ;
			
			double avgRelatedness = 0 ;
			
			for (Label.Sense s2: senses) 
				avgRelatedness += this.relatednessCache.getRelatedness(s, s2) ; 
				
			avgRelatedness = avgRelatedness / (senses.size()) ;
			
			double weight = (linkProb + avgRelatedness + avgRelatedness)/3 ;
			
			s.setWeight(weight) ;
			sortedContextArticles.add(s) ;
		}
		
		contextArticles = new Vector<Article>() ; 
		int c = 0 ;
		for (Article art: sortedContextArticles) {
			if (c++ > maxSize)
				break ;
			
			//System.out.println(" - cntxt art:" + art + ", w: " + art.getWeight()) ;
			
			totalWeight += art.getWeight() ;
			contextArticles.add(art) ;			
		}		
	}*/
	
	
	/**
	 * Initialises a collection of context articles from the given set of ambiguous anchors,  
	 * 
	 * @param ambigAnchors a set of ambiguous anchors, the most useful of which will be used to disambiguate other terms
	 * @param relatednessCache a cache in which relatedness measures will be saved so they aren't repeatedly calculated. 
	 * @param maxSize the maximum number of anchors that will be used (the more there are, the longer disambiguation takes, but the more accurate it is likely to be).
	 * @param minSenseLimit the minimum prior probability of an anchors sense that will be used as context.  
	 * @throws Exception 
	 */
	public Context(Collection<Label> labels, RelatednessCache relatednessCache, int maxSize, double minSenseLimit) throws Exception {
		
		this.relatednessCache = relatednessCache ;
				
		int maxCandidates = maxSize*5 ;
		
		//first gather senses and sort them according to (label.linkProb * sense.priorProb) 
		//only maintain a set of maxSize*5 so we don't bother adding all candidates
		ArrayList<Article> articles = new ArrayList<Article>() ;
		for (Label label:labels) {
			
			double lp = label.getLinkProbability() ;
			
			for (Label.Sense sense:label.getSenses()) {
			
				double sp = sense.getPriorProbability() ;
				
				//if below sp threshold, skip
				if (sp < minSenseLimit) break ;
				
				//if this is a date, skip
				if (isDate(sense)) continue ;
				
				sense.setWeight((lp + sp)/2) ;	
				//sense.setWeight(sp) ;
				int index = Collections.binarySearch(articles, sense) ;
					
				//if already in list, skip
				if (index >= 0) continue ;
				
				index = (-1*index) -1 ;
				
				//if belongs at end of too large a set, skip
				if (index >= maxCandidates) continue ;
				
				articles.add(index, sense) ;
				
				if (articles.size() > maxCandidates)
					articles.remove(maxCandidates-1) ;
			}	
		}
		
		//now weight candidates by their relatedness to each other
		for (Article art:articles) {
			
			double avgRelatedness = 0 ;
			
			for (Article art2:articles) {
				if (art.getId() != art2.getId()) {
					avgRelatedness += relatednessCache.getRelatedness(art, art2) ;
				}
			}
			
			avgRelatedness = avgRelatedness / (articles.size() - 1) ;
			
			art.setWeight((art.getWeight() + (4*avgRelatedness)) /5) ;
		}
		
		Collections.sort(articles) ;
		
		contextArticles = new Vector<Article>() ; 
		int c = 0 ;
		for (Article art: articles) {
			if (c++ > maxSize)
				break ;
			
			//System.out.println("context: " + art + " " + art.getWeight()) ;
			
			totalWeight += art.getWeight() ;
			contextArticles.add(art) ;			
		}
	}

	/**
	 * @return the quality (size and homogeneity) of the available context. 
	 */
	public float getQuality() {
		return totalWeight ;		
	}	

	/**
	 * Compares the given article to all context anchors.
	 * 
	 * @param art the article to be compared
	 * @return the average relatedness between the article and context anchors
	 * @throws Exception 
	 */
	public double getRelatednessTo(Article art) throws Exception {
		
		if (contextArticles.size() == 0 || totalWeight == 0)
			return 0 ;

		double relatedness = 0 ;
		
		for (Article contextArt: contextArticles) { 
			
			double r = relatednessCache.getRelatedness(art, contextArt) ;
			r = r * contextArt.getWeight() ;
			relatedness = relatedness + r ;
		}
		
		return relatedness / totalWeight ;
	}
	
	private boolean isDate(Article art) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM d") ;
		Date date = null ;
		
		try {
			date = sdf.parse(art.getTitle()) ;
		} catch (ParseException e) {
			return false ;
		}

		return (date != null) ;		
	}
}

