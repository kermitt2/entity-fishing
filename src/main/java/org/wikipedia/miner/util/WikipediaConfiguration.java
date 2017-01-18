package org.wikipedia.miner.util;


//import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.comparison.ArticleComparer.DataDependency;
//import org.wikipedia.miner.db.WDatabase.CachePriority;
import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.util.text.TextProcessor;
import org.xml.sax.SAXException;

public class WikipediaConfiguration {
	
	private enum ParamName{langCode,databaseDirectory,dataDirectory,defaultTextProcessor,minLinksIn,minSenseProbability,minLinkProbability, articlesOfInterest, databaseToCache,stopwordFile,articleComparisonDependency,articleComparisonModel, labelDisambiguationModel, labelComparisonModel, comparisonSnippetModel, topicDisambiguationModel, linkDetectionModel, tokenModel, sentenceModel, unknown};
	
	private String langCode;

	private File dbDirectory;
	private File dataDirectory;
	private TextProcessor defaultTextProcessor = null;

	//private final HashMap<DatabaseType, CachePriority> databasesToCache = new HashMap<DatabaseType, CachePriority>();

	private HashSet<String> stopwords = new HashSet<String>();
	
	private EnumSet<DataDependency> articleComparisonDependencies;
	private File articleComparisonModel;
	private File labelDisambiguationModel;
	private File labelComparisonModel;
	
	private File comparisonSnippetModel;
	
	private File topicDisambiguationModel;
	private File linkDetectionModel;
	
	private Tokenizer tokenizer;
	private SentenceDetector sentenceDetector;
	
	private int minLinksIn = 0;
	private float minLinkProbability = 0;
	private float minSenseProbability = 0;
	
	//private TIntHashSet articlesOfInterest;
	private ConcurrentMap articlesOfInterest;
	
	
	public WikipediaConfiguration(Element xml) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		initFromXml(xml);
	}

	public WikipediaConfiguration(File configFile) throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(configFile);
		doc.getDocumentElement().normalize();
		
		initFromXml(doc.getDocumentElement());
	}

	public WikipediaConfiguration(String langCode, File dbDirectory) {
		this.langCode = langCode;
		this.dbDirectory = dbDirectory;
	}

	public String getLangCode() {
		return langCode;
	}
		
	public File getDatabaseDirectory() {
		return dbDirectory;
	}
	
	public File getDataDirectory() {
		return dataDirectory;
	}
	
	public void setDataDirectory(File f) {
		dataDirectory = f;
	}

	public void setDefaultTextProcessor(TextProcessor tp) {
		defaultTextProcessor = tp;
	}

	public TextProcessor getDefaultTextProcessor() {
		return defaultTextProcessor;
	}

	/*public void addDatabaseToCache(DatabaseType type) {
		databasesToCache.put(type, CachePriority.space);
	}
	
	public void addDatabaseToCache(DatabaseType type, CachePriority priority) {
		
		//System.out.println("Will cache " + type + " for " + priority);
		databasesToCache.put(type, priority);
	}
	
	public void clearDatabasesToCache() {
		databasesToCache.clear();
	}
	
	public Set<DatabaseType> getDatabasesToCache() {
		return databasesToCache.keySet();
	}
	
	public CachePriority getCachePriority(DatabaseType databaseType) {
		return databasesToCache.get(databaseType);
	}*/

	public int getMinLinksIn() {
		return minLinksIn;
	}

	public void setMinLinksIn(int minLinksIn) {
		this.minLinksIn = minLinksIn;
	}

	public float getMinLinkProbability() {
		return minLinkProbability;
	}

	public void setMinLinkProbability(float minLinkProbability) {
		this.minLinkProbability = minLinkProbability;
	}

	public float getMinSenseProbability() {
		return minSenseProbability;
	}

	public void setMinSenseProbability(float minSenseProbability) {
		this.minSenseProbability = minSenseProbability;
	}

	/*public TIntHashSet getArticlesOfInterest() {
		return this.articlesOfInterest;
	}*/

	public ConcurrentMap getArticlesOfInterest() {
		return this.articlesOfInterest;
	}
	
	/*public void setArticlesOfInterest(TIntHashSet articlesOfInterest) {
		this.articlesOfInterest = articlesOfInterest;
	}*/

	public void setArticlesOfInterest(ConcurrentMap articlesOfInterest) {
		this.articlesOfInterest = articlesOfInterest;
	}
	
	public boolean isStopword(String stopword) {

		return stopwords.contains(stopword.trim());
	}

	public void setStopwords(HashSet<String> stopwords) {
		this.stopwords = stopwords;
	}

	public void setStopwords(File stopwordFile) throws IOException {

		stopwords = new HashSet<String>();

		BufferedReader input = new BufferedReader(new FileReader(stopwordFile));
		String line;
		while ((line=input.readLine()) != null) 
			stopwords.add(line.trim());
	}
	
	public EnumSet<DataDependency> getArticleComparisonDependancies() {
		return articleComparisonDependencies;
	}
	
	public void setArticleComparisonDependancies(EnumSet<DataDependency> dependancies) {
		articleComparisonDependencies = dependancies;
	}
	
	public File getArticleComparisonModel() {
		return articleComparisonModel;
	}

	public void setArticleComparisonModel(File model) {
		articleComparisonModel = model;
	}
	
	public File getLabelDisambiguationModel() {
		return labelDisambiguationModel;
	}

	public void setLabelDisambiguationModel(File model) {
		labelDisambiguationModel = model;
	}
	
	public File getLabelComparisonModel() {
		return labelComparisonModel;
	}

	public void setLabelComparisonModel(File model) {
		labelComparisonModel = model;
	}
	
	public File getComparisonSnippetModel() {
		return comparisonSnippetModel;
	}

	public void setComparisonSnippetModel(File model) {
		comparisonSnippetModel = model;
	}
	
	public File getLinkDetectionModel() {
		return linkDetectionModel;
	}

	public void setLinkDetectionModel(File model) {
		linkDetectionModel = model;
	}

	public File getTopicDisambiguationModel() {
		return topicDisambiguationModel;
	}

	public void setTopicDisambiguationModel(File model) {
		topicDisambiguationModel = model;
	}
	
	public Tokenizer getTokenizer() {
		
		if (tokenizer == null)
			tokenizer = SimpleTokenizer.INSTANCE;
		
		return tokenizer;
	}
	
	public void setTokenizer(Tokenizer t) {
		tokenizer = t;
	}
	
	public void setTokenizer(File modelFile) throws IOException{
			
		InputStream modelStream = new FileInputStream(modelFile);
		TokenizerModel model = null;
		
		model = new TokenizerModel(modelStream);
		
		tokenizer = new TokenizerME(model);
	}
	
	public SentenceDetector getSentenceDetector() {		
		return sentenceDetector;
	}
	
	public void setSentenceDetector(SentenceDetector sd) {
		sentenceDetector = sd;
	}
	
	public void setSentenceDetector(File modelFile) throws IOException{
			
		InputStream modelStream = new FileInputStream(modelFile);
		SentenceModel model = null;
		
		model = new SentenceModel(modelStream);
		
		sentenceDetector = new SentenceDetectorME(model);
	}
	
	public EnumSet<DataDependency> getReccommendedRelatednessDependancies() {
		
		ArrayList<DataDependency> dependancies = new ArrayList<DataDependency>();
		
		boolean valid = false;
		
		//if (this.databasesToCache.containsKey(DatabaseType.pageLinksIn)) 
		{
			dependancies.add(DataDependency.pageLinksIn);
			valid = true;
		}
		
		//if (this.databasesToCache.containsKey(DatabaseType.pageLinksOut)) 
		{
			dependancies.add(DataDependency.pageLinksOut);
			valid = true;
		}
		
		//if (this.databasesToCache.containsKey(DatabaseType.pageLinkCounts)) 
		{
			dependancies.add(DataDependency.linkCounts);	
		}
		
		//if (!valid)
		//	dependancies.add(DataDependency.pageLinksIn);
		
		return EnumSet.copyOf(dependancies);
	}
	
	@SuppressWarnings("rawtypes")
	private void initFromXml(Element xml) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		ArrayList<ArticleComparer.DataDependency> artCompDependencies = new ArrayList<ArticleComparer.DataDependency>();
		
		NodeList children = xml.getChildNodes();
		
		for (int i=0; i<children.getLength(); i++) {
			
			Node xmlChild = children.item(i);
			
			if (xmlChild.getNodeType() == Node.ELEMENT_NODE) {
				
				Element xmlParam = (Element)xmlChild;
				
				String paramName = xmlParam.getNodeName();
				String paramValue = getParamValue(xmlParam);
				
				if (paramValue == null)
					continue;
				
				switch(resolveParamName(xmlParam.getNodeName())) {
				
				case langCode:
					this.langCode = paramValue;
					break;
				case databaseDirectory:
					this.dbDirectory = new File(paramValue);
					break;
				case dataDirectory:
					this.dataDirectory = new File(paramValue);
					break;
				case defaultTextProcessor:
					Class tpClass = Class.forName(paramValue);
					this.defaultTextProcessor = (TextProcessor)tpClass.newInstance();
					break;
				case minLinksIn:
					this.minLinksIn = Integer.valueOf(paramValue);
					break;
				case minSenseProbability:
					this.minSenseProbability = Float.valueOf(paramValue);
					break;
				case minLinkProbability:
					this.minLinkProbability = Float.valueOf(paramValue);
					break;
				case articlesOfInterest:
					this.articlesOfInterest = gatherArticles(new File(paramValue));
					break;
				case databaseToCache: 
					/*if (xmlParam.hasAttribute("priority"))
						addDatabaseToCache(DatabaseType.valueOf(paramValue), CachePriority.valueOf(xmlParam.getAttribute("priority")));
					else
						addDatabaseToCache(DatabaseType.valueOf(paramValue));*/
					break;
				case stopwordFile:
					this.setStopwords(new File(paramValue));
					break;
				case articleComparisonDependency: 
					artCompDependencies.add(ArticleComparer.DataDependency.valueOf(paramValue));
					break;
				case articleComparisonModel:
					articleComparisonModel = new File(paramValue);
					break;
				case labelDisambiguationModel:
					labelDisambiguationModel = new File(paramValue);
					break;
				case labelComparisonModel:
					labelComparisonModel = new File(paramValue);
					break;
				case comparisonSnippetModel:
					comparisonSnippetModel = new File(paramValue);
					break;
				case topicDisambiguationModel:
					topicDisambiguationModel = new File(paramValue);
					break;
				case linkDetectionModel:
					this.linkDetectionModel = new File(paramValue);
					break;
				case tokenModel:
					this.setTokenizer(new File(paramValue));
					break;
				case sentenceModel:
					this.setSentenceDetector(new File(paramValue));
					break;
				default:
					Logger.getLogger(WikipediaConfiguration.class).warn("Ignoring unknown parameter: '" + paramName + "'");
				};
			}
			
			if (!artCompDependencies.isEmpty())
				articleComparisonDependencies = EnumSet.copyOf(artCompDependencies);;
			
		
			//TODO: throw fit if mandatory params (langCode, dbDirectory) are missing. 	
		}
	}
	
	private String getParamValue(Element xmlParam) {
		
		Node nodeContent = xmlParam.getChildNodes().item(0);
		
		if (nodeContent == null)
			return null;
		
		if (nodeContent.getNodeType() != Node.TEXT_NODE)
			return null;
		
		String content = nodeContent.getTextContent().trim();
		
		if (content.length() == 0)
			return null;
		
		return content;
	}
		
	private ParamName resolveParamName(String name) {
		try {
			return ParamName.valueOf(name.trim());
		} catch (Exception e) {
			return ParamName.unknown;
		}
	}
	
	/*private TIntHashSet gatherArticles(File file) throws NumberFormatException, IOException {
		
		TIntHashSet artIds = new TIntHashSet();
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line ;

		while ((line = reader.readLine()) != null) {
			String[] values = line.split("\t");
			int id = new Integer(values[0].trim());
			artIds.add(id);
		}

		reader.close();		
		
		return artIds;
	}*/

	private ConcurrentMap gatherArticles(File file) throws NumberFormatException, IOException {
		
		ConcurrentMap artIds = new ConcurrentHashMap();
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line ;

		while ((line = reader.readLine()) != null) {
			String[] values = line.split("\t");
			int id = new Integer(values[0].trim());
			//artIds.add(id);
			artIds.put(id, id);
		}

		reader.close();
		
		return artIds;
	}
	
}
