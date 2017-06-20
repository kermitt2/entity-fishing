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
	
	/*public ArticleSet getRandomSubset(int size) {
		if (size > this.size())
			throw new IllegalArgumentException("requested size " + size + " is larger than " + size());
		
		Random r = new Random();
		HashSet<Integer> usedIds = new HashSet<Integer>();
		
		ArticleSet subset = new ArticleSet();
		while (subset.size() < size) {
			int index = r.nextInt(size());
			
			Article art = get(index);
			
			if (!usedIds.contains(art.getId())) {
				subset.add(art);
				usedIds.add(art.getId());
			}
		}
		
		Collections.sort(subset);
		
		return subset;
	}*/
	
	/*private void build(Wikipedia wikipedia, int size, ArticleTrainingSampleCriterias criterias, ArticleTrainingSample exclude) {
		DecimalFormat df = new DecimalFormat("#0.00 %");
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
				if ((criterias.getMinOutLinks() != null) && (article.getLinksOut().length < criterias.getMinOutLinks()))
					continue;
				if ((criteria.getMinInLinks() != null) && (article.getLinksIn().length < criterias.getMinInLinks()))
					continue;
				if (isArticleValid(article, criterias, exclude)) 
					sample.add(article);
			}
		} finally {
			i.close();
		}
		
		if (sample.size() < size)
			System.out.println("Warning: we could only find " + size() + " suitable articles, the set contains " + size + " articles.");
		Collections.sort(sample);
	}*/

	/*private void buildFromCandidates(Wikipedia wikipedia, Vector<Article> roughCandidates, int size, 
		Integer minInLinks, Integer minOutLinks, Double minLinkProportion, Double maxLinkProportion, 
		Integer minWordCount, Integer maxWordCount, Double maxListProportion, Pattern mustMatch, 
		Pattern mustNotMatch, ArticleSet exclude) {
		
		DecimalFormat df = new DecimalFormat("#0.00 %");
		int totalRoughCandidates = roughCandidates.size();
		double lastWarningProgress = 0;
		while (roughCandidates.size() > 0) {
			if (size() == size)
				break; //we have enough ids
			
			//pop a random id
			Integer index = (int)Math.floor(Math.random() * roughCandidates.size());
			Article art = roughCandidates.elementAt(index);
			roughCandidates.removeElementAt(index);
			if (isArticleValid(art, minLinkProportion, maxLinkProportion, minWordCount, maxWordCount, 
				maxListProportion, mustMatch, mustNotMatch, exclude)) 
				sample.add(art);
			
			// warn user if it looks like we wont find enough valid articles
			double roughProgress = 1-((double) roughCandidates.size()/totalRoughCandidates);
			if (roughProgress >= lastWarningProgress + 0.01) {
				double fineProgress = (double)size()/size;
			
				if (roughProgress > fineProgress) {
					System.err.println("Warning : we have exhausted " + df.format(roughProgress) + 
						" of the available pages and only gathered " + df.format(fineProgress*100) 
						+ " of the articles needed.");
					lastWarningProgress = roughProgress;
				}
			}
		}
		
		if (size() < size)
			System.err.println("Warning: we could only find " + size() + " suitable articles.");
		
		Collections.sort(this);
	}*/

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
		
	/*protected static Vector<Article> getRoughCandidates(Wikipedia wikipedia, Integer minInLinks, 
		Integer minOutLinks)  {
		Vector<Article> articles = new Vector<Article>();
		PageIterator i = wikipedia.getPageIterator(PageType.article);
		while (i.hasNext()) {
			Article art = (Article)i.next();
			if (minOutLinks != null && art.getLinksOut().length < minOutLinks)
				continue;
			
			if (minInLinks != null && art.getLinksIn().length < minInLinks)
				continue;
			
			sample.add(art);
		}
		i.close();
		
		return sample;
	}*/
	
	/*@Override
	public boolean contains(Object obj) {
		Article art = (Article)obj;
		int index = Collections.binarySearch(this, art);
		
		return (index >= 0);
	}*/
	
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
		
		/*if (mustMatch != null) {
			Matcher m = mustMatch.matcher(markup);
			
			if (!m.find()) {
				Logger.getLogger(ArticleSet.class).debug(" - rejected due to mustMatch pattern");
				return false;	
			}
		}
		
		if (mustNotMatch != null) {
			Matcher m = mustNotMatch.matcher(markup);
			
			if (m.find()) {
				Logger.getLogger(ArticleSet.class).debug(" - rejected due to mustNotMatch pattern");
				return false;	
			}
		}*/
		MediaWikiParser stripper = new MediaWikiParser();
		markup = stripper.stripToPlainText(markup, null); 
		markup = stripper.stripExcessNewlines(markup);
		
		/*if (criterias.getMaxListProportion() != null) {
			//we need to count lines and list items
			String[] lines = markup.split("\n");
			
			int lineCount = 0;
			int listCount = 0;
			for (String line: lines) {
				line = line.replace(':', ' ');
				line = line.replace(';', ' ');
				
				line = line.trim();
				
				if (line.length() > 5) {
					lineCount++;

					if (line.startsWith("*") || line.startsWith("#")) 
						listCount++;			
				}
			}
			
			float listProportion = ((float)listCount) / lineCount;
			if (listProportion > maxListProportion) {
				Logger.getLogger(ArticleSet.class).debug(" - rejected for max list proportion " + (listProportion));
				return false;
			}
		}*/
				
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

	/*public ArticleTrainingSample build(int size, Wikipedia wikipedia) {
		return new ArticleTrainingSample(wikipedia, size, minInLinks, minOutLinks, minLinkProportion, 
			maxLinkProportion, minWordCount, maxWordCount, maxListProportion, mustMatch, 
			mustNotMatch, null, exclude);
	}*/

	public static ArticleTrainingSample[] buildExclusiveSamples(ArticleTrainingSampleCriterias constraints, 
																List<Integer> sizes, Wikipedia wikipedia) {
		ArticleTrainingSample samples[] = new ArticleTrainingSample[sizes.size()];
		List<Integer> exclude = constraints.getExclude();
		
		/*if (this.exclude != null)
			exclude.addAll(this.exclude);*/
		
		//List<Article> candidates = null;//ArticleSet.getRoughCandidates(wikipedia, minInLinks, minOutLinks);
		for (int i=0; i<sizes.size(); i++) {
			samples[i] = new ArticleTrainingSample(wikipedia, sizes.get(i), constraints, exclude);
			for(Article article : samples[i].getSample())
				exclude.add(article.getId());
			constraints.setExclude(exclude);
		}
		
		return samples;
	}
}
