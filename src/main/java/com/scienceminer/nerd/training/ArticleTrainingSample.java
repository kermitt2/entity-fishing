package com.scienceminer.nerd.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.utilities.MediaWikiParser;

/**
 *
 *	A training sample corresponding to a subset of Wikipedia articles. 
 *
 *  The building of the sample is very similar to the one of WikipediaMiner (by David Milne), but with 
 *  a slightly more restricted list of selection criterias.
 */
public class ArticleTrainingSample extends TrainingSample<Article> {
	public static final Logger LOGGER = LoggerFactory.getLogger(TermVectorTrainer.class);	
	
	private Wikipedia wikipedia = null;

	public ArticleTrainingSample(Wikipedia wikipedia) {
		super();
		this.wikipedia = wikipedia;
	}
	
	/**
	 * Create a random sample of articles from Wikipedia given some constraints
	 */
	public ArticleTrainingSample(Wikipedia wikipedia, int size, ArticleTrainingSampleCriterias criterias, List<Integer> exclude) {
		super();
		this.wikipedia = wikipedia;
		double lastWarningProgress = 0;
		PageIterator ite = wikipedia.getPageIterator(PageType.article);
		try {
			while (ite.hasNext()) {
				if (sample.size() >= size)
					break; //we have enough ids
				Page page = ite.next();
				if (page.getType() != PageType.article)
					continue;
				Article article = (Article)page;
				if ((criterias.getMinOutLinks() != null) && 
					(article.getLinksOut().length < criterias.getMinOutLinks()))
					continue;
				if ((criterias.getMinInLinks() != null) && 
					(article.getLinksIn().length < criterias.getMinInLinks()))
					continue;
				if (isArticleValid(article, criterias, exclude)) 
					sample.add(article);
			}
		} finally {
			ite.close();
		}
		
		if (sample.size() < size)
			LOGGER.warn("Only " + sample.size() + " suitable articles found out of " + size + " articles.");
		Collections.sort(sample);
	}

	/**
	 * Loads the sample from a file, with one wikipedia page id per line.
	 * 
	 */
	public void load(File file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line =null;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split("\t");
				int id = new Integer(values[0].trim());
				sample.add((Article)wikipedia.getPageById(id));
			}
		} catch(IOException e) {
			LOGGER.error("Invalid Wikipedia article sample definition file");
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Save the sample of article ids in a file, one page id per line. 
	 * 
	 */
	public void save(File file) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (Article article : sample) 
				writer.write(article.getId() + "\n");
		} catch(IOException e) {
			LOGGER.error("Error when writing Wikipedia article sample definition file");
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch(Exception e) {
					e.printStackTrace();
				}	
			}
		}	
	}

	private static boolean isArticleValid(Article article, ArticleTrainingSampleCriterias criterias, List<Integer> exclude) {	
		if (article.getType() == PageType.disambiguation) {
			return false;	
		}
		
		if ((exclude != null) && exclude.contains(article.getId())) {
			return false;	
		}
	
		// no other constraints?
		if ((criterias.getMinLinkProportion() == null) && (criterias.getMaxLinkProportion() == null) && 
			(criterias.getMinWordCount() == null) && (criterias.getMaxWordCount() == null))
			return true;
		
		// get and prepare markup
		String markup = article.getFullMarkup();
		if (markup == null)
			return false;
		
		MediaWikiParser stripper = new MediaWikiParser();
		markup = stripper.stripToPlainText(markup, null); 
		markup = stripper.stripExcessNewlines(markup);
				
		if ((criterias.getMinWordCount() != null) || (criterias.getMaxWordCount() != null) || 
			(criterias.getMinLinkProportion() != null) || (criterias.getMaxLinkProportion() != null) ) {
			//we need to count words
					
			StringTokenizer t = new StringTokenizer(markup);
	    	int wordCount = t.countTokens();

			if ((criterias.getMinWordCount() != null) && (wordCount < criterias.getMinWordCount())) {
				return false;
			}
			
			if ((criterias.getMaxWordCount() != null) && (wordCount > criterias.getMaxWordCount())) {
				return false;
			}
			
			int linkCount = article.getTotalLinksOutCount();
			float linkProportion = (float)linkCount/wordCount;
			if ((criterias.getMinLinkProportion() != null) && (linkProportion < criterias.getMinLinkProportion())) {
				return false;
			}
			
			if ((criterias.getMaxLinkProportion() != null) && (linkProportion > criterias.getMaxLinkProportion())) {
				return false;
			}
		}
		
		return true;
	}

	public static List<ArticleTrainingSample> buildExclusiveSamples(ArticleTrainingSampleCriterias constraints, 
																List<Integer> sizes, 
																Wikipedia wikipedia) {
		List<ArticleTrainingSample> samples = new ArrayList<ArticleTrainingSample>();
		List<Integer> exclude = constraints.getExclude();

		for (int i=0; i<sizes.size(); i++) {
			samples.add(new ArticleTrainingSample(wikipedia, sizes.get(i), constraints, exclude));
			for(Article article : samples.get(i).getSample())
				exclude.add(article.getId());
			constraints.setExclude(exclude);
		}
		
		return samples;
	}
}
