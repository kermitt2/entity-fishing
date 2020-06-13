package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.utilities.*;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;

import org.nustaq.serialization.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.kb.model.hadoop.*; 
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.*;

import org.apache.hadoop.record.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * The part of the KB corresponding to language-dependent resources, e.g. 
 * a Wikipedia instance, which is concretely stored as a set of LMDB databases.
 * 
 */
public class KBLowerEnvironment extends KBEnvironment {
	private static final Logger LOGGER = LoggerFactory.getLogger(KBLowerEnvironment.class);	

	private KBDatabase<Integer, DbPage> dbPage = null;
	private LabelDatabase dbLabel = null;
	private KBDatabase<String,Integer> dbArticlesByTitle = null;
	private KBDatabase<String,Integer> dbCategoriesByTitle = null;
	private KBDatabase<String,Integer> dbTemplatesByTitle = null;
	private KBDatabase<Integer,Integer> dbRedirectTargetBySource = null;
	private KBDatabase<Integer,DbIntList> dbRedirectSourcesByTarget = null;
	private KBDatabase<Integer, DbIntList> dbPageLinkInNoSentences = null;
	private KBDatabase<Integer, DbIntList> dbPageLinkOutNoSentences = null;
	private PageLinkCountDatabase dbPageLinkCounts = null;
	private KBDatabase<Integer, DbIntList> dbCategoryParents = null;
	private KBDatabase<Integer, DbIntList> dbArticleParents = null;
	private KBDatabase<Integer, DbIntList> dbChildCategories = null;
	private KBDatabase<Integer, DbIntList> dbChildArticles = null;
	private MarkupDatabase dbMarkup = null;
	private MarkupDatabase dbMarkupFull = null;
	private KBDatabase<Integer, DbTranslations> dbTranslations = null;
	private KBDatabase<Integer, Long> dbStatistics = null;
	private KBDatabase<Integer,String> dbConceptByPageId = null;
	private KBDatabase<String, short[]> dbWordEmbeddings = null;
	private KBDatabase<String, short[]> dbEntityEmbeddings = null;

	private int embeddingsSize = 300;

	public KBLowerEnvironment(NerdConfig conf) {
		super(conf);
		// register classes to be serialized
		//singletonConf.registerClass(DbPage.class, DbIntList.class, DbTranslations.class);
		initDatabases();
	}

	public KBDatabase<Integer, DbPage> getDbPage() {
		return dbPage;
	}

	public LabelDatabase getDbLabel() {
		return dbLabel;
	}
	
	public KBDatabase<String, Integer> getDbArticlesByTitle() {
		return dbArticlesByTitle;
	}
	
	public KBDatabase<String, Integer> getDbCategoriesByTitle() {
		return dbCategoriesByTitle;
	}

	public KBDatabase<String, Integer> getDbTemplatesByTitle() {
		return dbTemplatesByTitle;
	}

	public KBDatabase<Integer, Integer> getDbRedirectTargetBySource() {
		return dbRedirectTargetBySource;
	}
	
	public KBDatabase<Integer, DbIntList> getDbRedirectSourcesByTarget() {
		return dbRedirectSourcesByTarget;
	}

	public KBDatabase<Integer, DbIntList> getDbPageLinkInNoSentences() {
		return dbPageLinkInNoSentences;
	}
	
	public KBDatabase<Integer, DbIntList> getDbPageLinkOutNoSentences() {
		return dbPageLinkOutNoSentences;
	}
	
	public KBDatabase<Integer, DbPageLinkCounts> getDbPageLinkCounts() {
		return dbPageLinkCounts;
	}
	
	public KBDatabase<Integer, DbIntList> getDbCategoryParents() {
		return dbCategoryParents;
	}

	public KBDatabase<Integer, DbIntList> getDbArticleParents() {
		return dbArticleParents;
	}

	public KBDatabase<Integer, DbIntList> getDbChildCategories() {
		return dbChildCategories;
	}

	public KBDatabase<Integer, DbIntList> getDbChildArticles() {
		return dbChildArticles;
	}

	public MarkupDatabase getDbMarkup() {
		return dbMarkup;
	}

	public MarkupDatabase getDbMarkupFull() {
		return dbMarkupFull;
	}
	
	public KBDatabase<Integer, DbTranslations> getDbTranslations() {
		return dbTranslations;
	}

	public KBDatabase<Integer,String> getDbConceptByPageId() {
		return dbConceptByPageId;
	}

	public KBDatabase<String, short[]> getDbWordEmbeddings() {
		return dbWordEmbeddings;
	}

	public KBDatabase<String, short[]> getDbEntityEmbeddings() {
		return dbEntityEmbeddings;
	}

	@Override
	protected void initDatabases() {
		System.out.println("init Environment for language " + conf.getLangCode());
		
		KBDatabaseFactory dbFactory = new KBDatabaseFactory(this);
		
		databasesByType = new HashMap<DatabaseType, KBDatabase>();
		
		dbPage = dbFactory.buildPageDatabase();
		databasesByType.put(DatabaseType.page, dbPage);
		
		dbArticlesByTitle = dbFactory.buildTitleDatabase(DatabaseType.articlesByTitle);
		databasesByType.put(DatabaseType.articlesByTitle, dbArticlesByTitle);
		dbCategoriesByTitle = dbFactory.buildTitleDatabase(DatabaseType.categoriesByTitle);
		databasesByType.put(DatabaseType.categoriesByTitle, dbCategoriesByTitle);
		dbTemplatesByTitle = dbFactory.buildTitleDatabase(DatabaseType.templatesByTitle);
		databasesByType.put(DatabaseType.templatesByTitle, dbTemplatesByTitle);
		
		dbLabel = dbFactory.buildLabelDatabase();
		databasesByType.put(DatabaseType.label, dbLabel);

		dbPageLinkInNoSentences = dbFactory.buildPageLinkNoSentencesDatabase(DatabaseType.pageLinksInNoSentences); 
		databasesByType.put(DatabaseType.pageLinksInNoSentences, dbPageLinkInNoSentences);
		
		dbPageLinkOutNoSentences = dbFactory.buildPageLinkNoSentencesDatabase(DatabaseType.pageLinksOutNoSentences); 
		databasesByType.put(DatabaseType.pageLinksOutNoSentences, dbPageLinkOutNoSentences);
		
		dbPageLinkCounts = dbFactory.buildPageLinkCountDatabase();
		databasesByType.put(DatabaseType.pageLinkCounts, dbPageLinkCounts);
		
		dbCategoryParents = dbFactory.buildIntIntListDatabase(DatabaseType.categoryParents);
		databasesByType.put(DatabaseType.categoryParents, dbCategoryParents);
		dbArticleParents = dbFactory.buildIntIntListDatabase(DatabaseType.articleParents);
		databasesByType.put(DatabaseType.articleParents, dbArticleParents);
		dbChildCategories = dbFactory.buildIntIntListDatabase(DatabaseType.childCategories);
		databasesByType.put(DatabaseType.childCategories, dbChildCategories);
		dbChildArticles = dbFactory.buildIntIntListDatabase(DatabaseType.childArticles);
		databasesByType.put(DatabaseType.childArticles, dbChildArticles);
		
		dbRedirectSourcesByTarget = dbFactory.buildIntIntListDatabase(DatabaseType.redirectSourcesByTarget);
		databasesByType.put(DatabaseType.redirectSourcesByTarget, dbRedirectSourcesByTarget);
		dbRedirectTargetBySource = dbFactory.buildRedirectTargetBySourceDatabase();
		databasesByType.put(DatabaseType.redirectTargetBySource, dbRedirectTargetBySource);
		
		dbMarkup = new MarkupDatabase(this, DatabaseType.markup);
		databasesByType.put(DatabaseType.markup, dbMarkup);
		
		dbTranslations = dbFactory.buildTranslationsDatabase();
		databasesByType.put(DatabaseType.translations, dbTranslations);

		dbConceptByPageId = dbFactory.buildDbConceptByPageIdDatabase();
		databasesByType.put(DatabaseType.conceptByPageId, dbConceptByPageId);

		dbStatistics = dbFactory.buildStatisticsDatabase();
		databasesByType.put(DatabaseType.statistics, dbStatistics);

		dbWordEmbeddings = dbFactory.buildWordEmbeddingsDatabase();
		databasesByType.put(DatabaseType.wordEmbeddings, dbWordEmbeddings);

		dbEntityEmbeddings = dbFactory.buildEntityEmbeddingsDatabase();
		databasesByType.put(DatabaseType.entityEmbeddings, dbEntityEmbeddings);
	}

	public Long retrieveStatistic(StatisticName sn) {
		return dbStatistics.retrieve(sn.ordinal());
	}
	
	@Override
	public void buildEnvironment(NerdConfig conf, boolean overwrite) throws Exception {
		System.out.println("building Environment for language " + conf.getLangCode());	
		
		File dataDirectory = new File(conf.getDataDirectory());
		File statistics = getDataFile(dataDirectory, "stats.csv");
		File page = getDataFile(dataDirectory, "page.csv");
		File label = getDataFile(dataDirectory, "label.csv");
		File pageLabel = getDataFile(dataDirectory, "pageLabel.csv");
		File pageLinksIn = getDataFile(dataDirectory, "pageLinkIn.csv");
		File pageLinksOut = getDataFile(dataDirectory, "pageLinkOut.csv");
		File categoryParents = getDataFile(dataDirectory, "categoryParents.csv");
		File articleParents = getDataFile(dataDirectory, "articleParents.csv");
		File childCategories = getDataFile(dataDirectory, "childCategories.csv");
		File childArticles = getDataFile(dataDirectory, "childArticles.csv");
		File redirectTargetBySource = getDataFile(dataDirectory, "redirectTargetsBySource.csv");
		File redirectSourcesByTarget = getDataFile(dataDirectory, "redirectSourcesByTarget.csv");
		File wikidata = getDataFile(dataDirectory, "wikidata.txt");
		File translations = getDataFile(dataDirectory, "translations.csv");
		File markup = getMarkupDataFile(dataDirectory);
		File wordEmbeddingsFile = getDataFile(dataDirectory, "word.embeddings.quantized.gz");
		File entityEmbeddingsFile = getDataFile(dataDirectory, "entity.embeddings.quantized.gz");

		//now load databases
		File dbDirectory = new File(conf.getDbDirectory());
		if (!dbDirectory.exists())
			dbDirectory.mkdirs();

		//System.out.println("Building statistics db");
		dbStatistics.loadFromFile(statistics, overwrite);

		//System.out.println("Building Page db");
		dbPage.loadFromFile(page, overwrite);

		//System.out.println("Building ArticlesByTitle db");
		dbArticlesByTitle.loadFromFile(page, overwrite);

		//System.out.println("Building CategoriesByTitle db");
		dbCategoriesByTitle.loadFromFile(page, overwrite);

		//System.out.println("Building TemplatesByTitle db");
		dbTemplatesByTitle.loadFromFile(page, overwrite);
		
		//System.out.println("Building RedirectTargetBySource db");
		dbRedirectTargetBySource.loadFromFile(redirectTargetBySource, overwrite);

		//System.out.println("Building RedirectSourcesByTarget db");
		dbRedirectSourcesByTarget.loadFromFile(redirectSourcesByTarget, overwrite);
		
		//System.out.println("Building Label db");
		dbLabel.loadFromFile(label, overwrite);

		//System.out.println("Building PageLinkInNoSentences db");
		dbPageLinkInNoSentences.loadFromFile(pageLinksIn, overwrite);

		//System.out.println("Building PageLinkOutNoSentences db");
		dbPageLinkOutNoSentences.loadFromFile(pageLinksOut, overwrite);
		
		//System.out.println("Building PageLinkCounts db");
		dbPageLinkCounts.loadFromFiles(pageLinksIn, pageLinksOut, overwrite);
		
		//System.out.println("Building CategoryParents db");
		dbCategoryParents.loadFromFile(categoryParents, overwrite);
		
		//System.out.println("Building ArticleParents db");
		dbArticleParents.loadFromFile(articleParents, overwrite);
		
		//System.out.println("Building ChildCategories db");
		dbChildCategories.loadFromFile(childCategories, overwrite);
		
		//System.out.println("Building ChildArticles db");
		dbChildArticles.loadFromFile(childArticles, overwrite);
		
		//System.out.println("Building Translations db");
		dbTranslations.loadFromFile(translations, overwrite);
		
		//System.out.println("Building conceptbyPage db");
		dbConceptByPageId.loadFromFile(wikidata, overwrite);

		//System.out.println("Building Markup db");
		dbMarkup.loadFromXmlFile(markup, overwrite);

		//System.out.println("Building embeddings db");
		dbWordEmbeddings.loadFromFile(wordEmbeddingsFile, overwrite);

		//System.out.println("Building embeddings db");
		dbEntityEmbeddings.loadFromFile(entityEmbeddingsFile, overwrite);

		// we need to enrich the Label database with the article titles to ensure 
		// better mention resolution
		//dbLabel.enrich(dbArticlesByTitle);

		System.out.println("Environment built - " + dbPage.getDatabaseSize() + " pages.");
	}
	
	/**
	 * The full markup database is built separately because it is only required for training
	 * purposes. 
	 */
	public void buildFullMarkup(boolean overwrite) throws Exception {
		System.out.println("building full markup database for language " + conf.getLangCode());	

		KBDatabaseFactory dbFactory = new KBDatabaseFactory(this);

		dbMarkupFull = new MarkupDatabase(this, DatabaseType.markupFull);
		databasesByType.put(DatabaseType.markupFull, dbMarkupFull);

		File dataDirectory = new File(conf.getDataDirectory());

		File markup = getMarkupDataFile(dataDirectory);
		dbMarkupFull.loadFromXmlFile(markup, overwrite);

		System.out.println("Full markup database built - " + dbPage.getDatabaseSize() + " pages.");
	}
	
	private static File getMarkupDataFile(File dataDirectory) {
		File[] files = dataDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith("-pages-articles.xml") || name.endsWith("-pages-articles.xml.bz2") || name.endsWith("-pages-articles.xml.gz") || 
						name.endsWith("-pages-articles-multistream.xml");
			}
		});
		
		if ((files == null) || (files.length == 0)) {
			LOGGER.info("Could not locate markup file in " + dataDirectory);
			return null;
		}
		else { 
			if (files.length > 1)
				LOGGER.info("There are multiple markup files in " + dataDirectory);
		
			if (!files[0].canRead())
				LOGGER.info(files[0] + " is not readable");
		}
		return files[0];
	}

	public int getEmbeddingsSize() {
		return this.embeddingsSize;
	}

	public void setEmbeddingsSize(int size) {
		this.embeddingsSize = size;
	}

}
