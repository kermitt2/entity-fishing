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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

	public static List<ArticleTrainingSample> buildExclusiveSamples(ArticleTrainingSampleCriterias trainingConstraints, 
																ArticleTrainingSampleCriterias evaluationConstraints, 
																List<Integer> sizes, 
																LowerKnowledgeBase wikipedia) {
		List<ArticleTrainingSample> samples = new ArrayList<>();
		List<Integer> exclude = trainingConstraints.getExclude();

		for (int i=0; i<sizes.size(); i++) {
			// rank over i: training ranker, training selector, eval ranker, eval selector, eval end-to-end
			if ( i <2 )
				samples.add(new ArticleTrainingSample(wikipedia, sizes.get(i), trainingConstraints, exclude));
			else
				samples.add(new ArticleTrainingSample(wikipedia, sizes.get(i), evaluationConstraints, exclude));
			if (samples.get(i).getSample() == null) {
				System.out.println("Article sample is empty for set " + i);
			} else {
				for(Article article : samples.get(i).getSample()) {
					if (exclude == null)
						exclude = new ArrayList<>();
					exclude.add(article.getId());
				}
				trainingConstraints.setExclude(exclude);
			}
		}
		
		return samples;
	}

	public static List<ArticleTrainingSample> buildExclusiveCorpusSets(String corpus, double ratio, LowerKnowledgeBase wikipedia) {
		List<ArticleTrainingSample> sets = new ArrayList<ArticleTrainingSample>();

		ArticleTrainingSample rankerSet = new ArticleTrainingSample(wikipedia);
		ArticleTrainingSample selectorSet = new ArticleTrainingSample(wikipedia);

		String corpusPath = "data/corpus/corpus-long/" + corpus + "/";
		String corpusRefPath = corpusPath + corpus + ".xml";

		// first we parse the result to get the documents and mentions
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		org.w3c.dom.Document dom = null;
		try {
			//Using factory get an instance of document builder
			db = dbf.newDocumentBuilder();
			//parse using builder to get DOM representation
			dom = db.parse(corpusRefPath);
		} catch(Exception e) {
			e.printStackTrace();
		}

		Random rand = new Random();

		//get the root element and the document node
		Element root = dom.getDocumentElement();
		NodeList docs = root.getElementsByTagName("document");
		if (docs == null || docs.getLength() <= 0) {
			LOGGER.error("the list documents for this corpus is empty");
			return null;
		}

		for (int i = 0; i < docs.getLength(); i++) {
			//get each document name
			Element docElement = (Element)docs.item(i);
			String docName = docElement.getAttribute("docName");
			String docPath = corpusPath + "RawText/" + docName;

			File docFile = new File(docPath);
			if (!docFile.exists()) {
				System.out.println("The document file " + docPath + " for corpus " + corpus + " is not found: ");
				continue;
			}
			CorpusArticle article = new CorpusArticle(corpus, wikipedia);
			article.setPath(docPath);
			// file ratio filtering
			double random = rand.nextDouble();

			if (random < ratio) {
				if (rankerSet.sample == null)
					rankerSet.sample = new ArrayList<>();
				rankerSet.sample.add(article);
			} else {
				if (selectorSet.sample == null)
					selectorSet.sample = new ArrayList<>();
				selectorSet.sample.add(article);
			}
		}

		sets.add(rankerSet);
		sets.add(selectorSet);

		return sets;
	}
}
