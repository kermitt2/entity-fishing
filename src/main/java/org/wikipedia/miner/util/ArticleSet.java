package org.wikipedia.miner.util;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;

import org.apache.log4j.Logger;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.kb.db.*;

import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;


/**
 * @author David Milne
 *
 *	A set of Wikipedia articles that can be used to train and test disambiguators, linkDetectors, etc. 
 *  Can either be generated randomly from Wikipedia, or loaded from file.
 */
public class ArticleSet extends ArrayList<Article> {
	
	//private TreeSet<Integer> articleIds = new TreeSet<Integer>();
	private MarkupStripper stripper = new MarkupStripper();
	
	public ArticleSet() {
		super();
	}
	
	/**
	 * Loads this article set from file. The file must contain a list of article ids, separated by newlines. 
	 * If the file is comma separated, then only the first column is used.
	 * 
	 * @param file the file containing article ids.
	 * @throws IOException if the file cannot be read.
	 */
	public ArticleSet(File file, Wikipedia wikipedia) throws IOException{
		
		//articleIds = new TreeSet<Integer>();

		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line ;

		while ((line = reader.readLine()) != null) {
			String[] values = line.split("\t");
			int id = new Integer(values[0].trim());
			add((Article)wikipedia.getPageById(id));
		}

		reader.close();		
	}
	
	/**
	 * Generates a set of articles randomly from Wikipedia, given some constraints on what is an 
	 * acceptable article.
	 * <p>
	 * This first gathers all articles that satisfy the minInLink and minOutLink constraints, and then 
	 * randomly samples from
	 * these to produce the final set of articles which satisfy all constraints.
	 * <p>
	 * The length of time this takes is very variable. It will work fastest if the minInLink 
	 * and minOutLink constraints are strict, and 
	 * the other constraints are loose.
	 * <p>
	 * You can ignore any of the constraints by setting them to -1;
	 * 
	 * @param wikipedia	an instantiated instance of Wikipedia.
	 * @param size	the desired number of articles
	 * @param minInLinks	the minimum number of links that must be made to an article
	 * @param minOutLinks	the minimum number of links that an article must make
	 * @param minLinkProportion the minimum proportion of links (over total words) that articles must contain
	 * @param maxLinkProportion the maximum proportion of links (over total words) that articles must contain
	 * @param minWordCount  the minimum number of words allowed in an article
	 * @param maxWordCount the maximum number of words allowed in an article
	 * @param maxListProportion the maximum proportion of list items (over total line count) that an article may contain. 
	 */
	public ArticleSet(Wikipedia wikipedia, int size, Integer minInLinks, Integer minOutLinks, 
			Double minLinkProportion, Double maxLinkProportion, Integer minWordCount, 
			Integer maxWordCount, Double maxListProportion, Pattern mustMatch, Pattern mustNotMatch, 
			Vector<Article> candidates , ArticleSet exclude) {
		//if (candidates == null)
		//	candidates = getRoughCandidates(wikipedia, minInLinks, minOutLinks);
		
		//buildFromCandidates(wikipedia, candidates, size, minInLinks, minOutLinks, minLinkProportion, 
		//	maxLinkProportion, minWordCount, maxWordCount, maxListProportion, mustMatch, mustNotMatch, exclude);
		buildFromCandidatesVariant(wikipedia, size, minInLinks, minOutLinks, minLinkProportion, 
			maxLinkProportion, minWordCount, maxWordCount, maxListProportion, mustMatch, mustNotMatch, exclude);
	}
	
	public ArticleSet getRandomSubset(int size) {
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
	}
	
	private void buildFromCandidatesVariant(Wikipedia wikipedia, 
			int size, 
			Integer minInLinks, 
			Integer minOutLinks, 
			Double minLinkProportion, 
			Double maxLinkProportion, 
			Integer minWordCount, 
			Integer maxWordCount, 
			Double maxListProportion, 
			Pattern mustMatch, 
			Pattern mustNotMatch, 
			ArticleSet exclude) {
		DecimalFormat df = new DecimalFormat("#0.00 %");
		
		//int totalRoughCandidates = roughCandidates.size();
		double lastWarningProgress = 0;
		
		PageIterator i = wikipedia.getPageIterator(PageType.article);
		try {
			while (i.hasNext()) {
				if (size() >= size)
					break; //we have enough ids
				Page page = i.next();
				if (page.getType() != PageType.article)
					continue;
				Article art = (Article)page;
				if (minOutLinks != null && (art.getLinksOut().length < minOutLinks))
					continue;
				
				if (minInLinks != null && (art.getLinksIn().length < minInLinks))
					continue;
			//while (roughCandidates.size() > 0) {
				
				//pop a random id
				//Integer index = (int)Math.floor(Math.random() * roughCandidates.size());
				//Article art = roughCandidates.elementAt(index);
				//roughCandidates.removeElementAt(index);
										
				if (isArticleValid(art, minLinkProportion, maxLinkProportion, minWordCount, maxWordCount, 
					maxListProportion, mustMatch, mustNotMatch, exclude)) 
					add(art);
				
				// warn user if it looks like we wont find enough valid articles
				/*double roughProgress = 1-((double) roughCandidates.size()/totalRoughCandidates);
				if (roughProgress >= lastWarningProgress + 0.01) {
					double fineProgress = (double)size()/size;
				
					if (roughProgress > fineProgress) {
						System.err.println("ArticleSet | Warning : we have exhausted " + df.format(roughProgress) + 
							" of the available pages and only gathered " + df.format(fineProgress*100) + 
							" of the articles needed.");
						lastWarningProgress = roughProgress;
					}
				}*/
			}
		} finally {
			i.close();
		}
		
		if (size() < size)
			System.out.println("ArticleSet | Warning: we could only find " + size() + 
				" suitable articles, the set contains " + size + " articles.");
		
		
		Collections.sort(this);
	}

	private void buildFromCandidates(Wikipedia wikipedia, Vector<Article> roughCandidates, int size, 
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
				add(art);
			
			
			// warn user if it looks like we wont find enough valid articles
			double roughProgress = 1-((double) roughCandidates.size()/totalRoughCandidates);
			if (roughProgress >= lastWarningProgress + 0.01) {
				double fineProgress = (double)size()/size;
			
				if (roughProgress > fineProgress) {
					System.err.println("ArticleSet | Warning : we have exhausted " + df.format(roughProgress) + 
						" of the available pages and only gathered " + df.format(fineProgress*100) 
						+ " of the articles needed.");
					lastWarningProgress = roughProgress;
				}
			}
		}
		
		if (size() < size)
			System.err.println("ArticleSet | Warning: we could only find " + size() + " suitable articles.");
		
		
		Collections.sort(this);
	}

	/**
	 * @return the set of article ids, in ascending order.
	 *//*
	public ArrayList<Integer> getArticleIds() {
		return articleIds;
	}*/
	
	/**
	 * Saves this list of article ids in a text file, separated by newlines. 
	 * If the file exists already, it will be overwritten.
	 * 
	 * @param file the file in which this set is to be saved
	 * @throws IOException if the file cannot be written to.
	 */
	public void save(File file) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		for (Article art: this) 
			writer.write(art.getId() + "\n");
		
		writer.close();
	}
		
	protected static Vector<Article> getRoughCandidates(Wikipedia wikipedia, Integer minInLinks, 
		Integer minOutLinks)  {
		Vector<Article> articles = new Vector<Article>();
		//int totalArticles = wikipedia.getEnvironment().retrieveStatistic(StatisticName.articleCount).intValue();
		PageIterator i = wikipedia.getPageIterator(PageType.article);
		while (i.hasNext()) {
			Article art = (Article)i.next();
			if (minOutLinks != null && art.getLinksOut().length < minOutLinks)
				continue;
			
			if (minInLinks != null && art.getLinksIn().length < minInLinks)
				continue;
			
			articles.add(art);
		}
		i.close();
		
		return articles;
	}
	
	@Override
	public boolean contains(Object obj) {
		Article art = (Article)obj;
		int index = Collections.binarySearch(this, art);
		
		return (index >= 0);
	}
	
		
	private boolean isArticleValid(Article art, Double minLinkProportion, Double maxLinkProportion, 
		Integer minWordCount, Integer maxWordCount, Double maxListProportion, Pattern mustMatch, 
		Pattern mustNotMatch, ArticleSet exclude) {	
		Logger.getLogger(ArticleSet.class).debug("Evaluating " + art);

		
		//we don't want any disambiguations
		if (art.getType() == PageType.disambiguation) {
			Logger.getLogger(ArticleSet.class).debug(" - rejected due to disambiguation");
			return false;	
		}
		
		if (exclude != null && exclude.contains(art)) {
			Logger.getLogger(ArticleSet.class).debug(" - rejected due to exclusion list");
			return false;	
		}
		
		//TODO: check that list identification works
		//if (art.getType() == PageType.list) 
		//	return false;	
	
		//check if there are any other constraints
		if (minLinkProportion == null && maxLinkProportion == null && minWordCount == null && 
			maxWordCount == null && maxListProportion == null)
			return true;
		
		// get and prepare markup
		String markup = art.getMarkup();
		
		if (markup == null)
			return false;
		
		if (mustMatch != null) {
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
		}
		
		markup = stripper.stripToPlainText(markup, null); 
		
		markup = stripper.stripExcessNewlines(markup);
		
		
		if (maxListProportion != null) {
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
		}
		
				
		if (minWordCount != null || maxWordCount != null || minLinkProportion != null || maxLinkProportion != null ) {
			//we need to count words
					
			StringTokenizer t = new StringTokenizer(markup);
	    	int wordCount = t.countTokens();

			if (minWordCount != null && wordCount < minWordCount) {
				Logger.getLogger(ArticleSet.class).debug(" - rejected for min wordcount " + (wordCount));
				return false;
				
			}
			
			if (maxWordCount != null && wordCount > maxWordCount) {
				Logger.getLogger(ArticleSet.class).debug(" - rejected for max wordcount " + (wordCount));
				return false;
			}
			
			int linkCount = art.getTotalLinksOutCount();
			float linkProportion = (float)linkCount/wordCount;
			
			if (minLinkProportion != null && linkProportion < minLinkProportion) {
				Logger.getLogger(ArticleSet.class).debug(" - rejected for min link proportion " + (linkProportion));
				return false;
			}
			
			if (maxLinkProportion != null && linkProportion > maxLinkProportion) {
				Logger.getLogger(ArticleSet.class).debug(" - rejected for max link proportion " + (linkProportion));
				return false;
			}
		}
		
		return true;
	}
}
