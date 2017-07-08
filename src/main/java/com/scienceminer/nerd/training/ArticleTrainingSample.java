package com.scienceminer.nerd.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.utilities.mediaWiki.MediaWikiParser;
import com.scienceminer.nerd.disambiguation.NerdContext;

/**
 *
 *	A training sample corresponding to a subset of Wikipedia articles. 
 *
 *  The building of the sample is very similar to the one of WikipediaMiner (by David Milne), but with 
 *  a slightly more restricted list of selection criterias.
 */
public class ArticleTrainingSample extends TrainingSample<Article> {
	public static final Logger LOGGER = LoggerFactory.getLogger(TermVectorTrainer.class);	
	
	private LowerKnowledgeBase wikipedia = null;

	public ArticleTrainingSample(LowerKnowledgeBase wikipedia) {
		super();
		this.wikipedia = wikipedia;
	}
	
	/**
	 * Create a random sample of articles from Wikipedia given some constraints
	 */
	public ArticleTrainingSample(LowerKnowledgeBase wikipedia, 
								int size, 
								ArticleTrainingSampleCriterias criterias, 
								List<Integer> exclude) {
		super();
		this.wikipedia = wikipedia;
		double lastWarningProgress = 0;
		PageIterator ite = wikipedia.getPageIterator(PageType.article);
		try {
			while (ite.hasNext()) {
				if ( (sample != null) && (sample.size() >= size))
					break; //we have enough ids
				Page page = ite.next();
				if (page.getType() != PageType.article)
					continue;
				Article article = (Article)page;

				String title = article.getTitle();
				if ((title == null) || title.startsWith("List of") || title.startsWith("Liste des")) 
					continue;
				
				if ((criterias.getMinOutLinks() != null) && 
					(article.getLinksOut().length < criterias.getMinOutLinks()))
					continue;
				if ((criterias.getMinInLinks() != null) && 
					(article.getLinksIn().length < criterias.getMinInLinks()))
					continue;
				if (isArticleValid(article, criterias, exclude)) {
					if (sample == null)
						sample = new ArrayList<Article>();
					sample.add(article);
				}
			}
		} finally {
			ite.close();
		}
		
		if ((sample == null) || (sample.size() < size)) {
			int sampleSize = 0;
			if (sample != null)
				sampleSize = sample.size();
			LOGGER.warn("Only " + sampleSize + " suitable articles found out of " + size + " articles.");
		}
		if (sample != null)
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

	private boolean isArticleValid(Article article, ArticleTrainingSampleCriterias criterias, List<Integer> exclude) {	
		if (article.getType() == PageType.disambiguation) {
			return false;	
		}
		
		if ((exclude != null) && exclude.contains(article.getId())) {
			return false;	
		}
	
		// avoid - as a general principle date and numerical articles
		if (NerdContext.isDate(article) || NerdContext.isNumber(article))
			return false;

		// no other constraints?
		if ((criterias.getMinLinkProportion() == null) && (criterias.getMaxLinkProportion() == null) && 
			(criterias.getMinWordCount() == null) && (criterias.getMaxWordCount() == null))
			return true;

		// get and prepare markup
		String wikiText = article.getFullWikiText();
		if (wikiText == null)
			return false;
		
		String content = MediaWikiParser.getInstance().toTextOnly(wikiText, wikipedia.getConfig().getLangCode());

		if ((criterias.getMinWordCount() != null) || (criterias.getMaxWordCount() != null) || 
			(criterias.getMinLinkProportion() != null) || (criterias.getMaxLinkProportion() != null) ) {
			//we need to count words
					
			StringTokenizer st = new StringTokenizer(content);
	    	int tokenCount = st.countTokens();

			if ((criterias.getMinWordCount() != null) && (tokenCount < criterias.getMinWordCount())) {
				return false;
			}
			
			if ((criterias.getMaxWordCount() != null) && (tokenCount > criterias.getMaxWordCount())) {
				return false;
			}
			
			int linkCount = article.getTotalLinksOutCount();
			float linkProportion = (float)linkCount/tokenCount;
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
																LowerKnowledgeBase wikipedia) {
		List<ArticleTrainingSample> samples = new ArrayList<ArticleTrainingSample>();
		List<Integer> exclude = constraints.getExclude();

		for (int i=0; i<sizes.size(); i++) {
			samples.add(new ArticleTrainingSample(wikipedia, sizes.get(i), constraints, exclude));
			if (samples.get(i).getSample() == null) {
				System.out.println("Article sample is empty for set " + i);
			} else {
				for(Article article : samples.get(i).getSample()) {
					if (exclude == null)
						exclude = new ArrayList<Integer>();
					exclude.add(article.getId());
				}
				constraints.setExclude(exclude);
			}
		}
		
		return samples;
	}
}
