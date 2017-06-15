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
import com.scienceminer.nerd.kb.model.Wikipedia;

import org.apache.hadoop.record.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A KB corresponding to a Wikipedia instance, which is concretely stored as a set of LMDB databases.
 * 
 */
public class KBLowerEnvironment extends KBEnvironment {

	// the different language-specific databases of the KB
	private KBDatabase<Integer, DbPage> dbPage = null;
	private LabelDatabase dbLabel = null;
	//private HashMap<String, LabelDatabase> processedLabelDbs = null;
	//private KBDatabase<Integer, DbLabelForPageList> dbLabelsForPage = null; 
	private KBDatabase<String,Integer> dbArticlesByTitle = null;
	private KBDatabase<String,Integer> dbCategoriesByTitle = null;
	private KBDatabase<String,Integer> dbTemplatesByTitle = null;
	private KBDatabase<Integer,Integer> dbRedirectTargetBySource = null;
	private KBDatabase<Integer,DbIntList> dbRedirectSourcesByTarget = null;
	//private KBDatabase<Integer, DbLinkLocationList> dbPageLinkIn = null;
	private KBDatabase<Integer, DbIntList> dbPageLinkInNoSentences = null;
	//private KBDatabase<Integer, DbLinkLocationList> dbPageLinkOut = null;
	private KBDatabase<Integer, DbIntList> dbPageLinkOutNoSentences = null;
	private PageLinkCountDatabase dbPageLinkCounts = null;
	private KBDatabase<Integer, DbIntList> dbCategoryParents = null;
	private KBDatabase<Integer, DbIntList> dbArticleParents = null;
	private KBDatabase<Integer, DbIntList> dbChildCategories = null;
	private KBDatabase<Integer, DbIntList> dbChildArticles = null;
	private MarkupDatabase dbMarkup = null;
	private MarkupDatabase dbMarkupFull = null;
	//private KBDatabase<Integer, DbIntList> dbSentenceSplits = null;
	private KBDatabase<Integer, DbTranslations> dbTranslations = null;
	private KBDatabase<Integer, Long> dbStatistics = null;
	private KBDatabase<Integer,String> dbConceptByPageId = null;
	
	/**
	 * Constructor
	 */	
	public KBLowerEnvironment(NerdConfig conf) {
		super(conf);
		// register classes to be serialized
		singletonConf.registerClass(DbPage.class, DbIntList.class, DbTranslations.class);
		initDatabases();
	}

	
	/**
	 * Returns the {@link DatabaseType#page} database
	 */
	public KBDatabase<Integer, DbPage> getDbPage() {
		return dbPage;
	}

	/**
	 * Returns the {@link DatabaseType#label} database for the given text processor
	 * 
	 * @return see {@link DatabaseType#label} 
	 */
	public LabelDatabase getDbLabel() {
		return dbLabel;
	}
	
	/**
	 * Returns the {@link DatabaseType#articlesByTitle} database
	 */
	public KBDatabase<String, Integer> getDbArticlesByTitle() {
		return dbArticlesByTitle;
	}
	
	/**
	 * Returns the {@link DatabaseType#categoriesByTitle} database
	 */
	public KBDatabase<String, Integer> getDbCategoriesByTitle() {
		return dbCategoriesByTitle;
	}
	
	/**
	 * Returns the {@link DatabaseType#templatesByTitle} database
	 */
	public KBDatabase<String, Integer> getDbTemplatesByTitle() {
		return dbTemplatesByTitle;
	}
	
	/**
	 * Returns the {@link DatabaseType#redirectTargetBySource} database
	 */
	public KBDatabase<Integer, Integer> getDbRedirectTargetBySource() {
		return dbRedirectTargetBySource;
	}
	
	/**
	 * Returns the {@link DatabaseType#redirectSourcesByTarget} database
	 */
	public KBDatabase<Integer, DbIntList> getDbRedirectSourcesByTarget() {
		return dbRedirectSourcesByTarget;
	}

	/**
	 * Returns the {@link DatabaseType#pageLinksInNoSentences} database
	 */
	public KBDatabase<Integer, DbIntList> getDbPageLinkInNoSentences() {
		return dbPageLinkInNoSentences;
	}
	
	/**
	 * Returns the {@link DatabaseType#pageLinksOutNoSentences} database
	 */
	public KBDatabase<Integer, DbIntList> getDbPageLinkOutNoSentences() {
		return dbPageLinkOutNoSentences;
	}
	
	/**
	 * Returns the {@link DatabaseType#pageLinkCounts} database
	 */
	public KBDatabase<Integer, DbPageLinkCounts> getDbPageLinkCounts() {
		return dbPageLinkCounts;
	}
	
	/**
	 * Returns the {@link DatabaseType#categoryParents} database
	 */
	public KBDatabase<Integer, DbIntList> getDbCategoryParents() {
		return dbCategoryParents;
	}

	/**
	 * Returns the {@link DatabaseType#articleParents} database
	 */
	public KBDatabase<Integer, DbIntList> getDbArticleParents() {
		return dbArticleParents;
	}

	/**
	 * Returns the {@link DatabaseType#childCategories} database
	 */
	public KBDatabase<Integer, DbIntList> getDbChildCategories() {
		return dbChildCategories;
	}

	/**
	 * Returns the {@link DatabaseType#childArticles} database
	 */
	public KBDatabase<Integer, DbIntList> getDbChildArticles() {
		return dbChildArticles;
	}

	/**
	 * Returns the {@link DatabaseType#markup} database
	 */
	public MarkupDatabase getDbMarkup() {
		return dbMarkup;
	}

	/**
	 * Returns the {@link DatabaseType#markupFull} database
	 */
	public MarkupDatabase getDbMarkupFull() {
		return dbMarkupFull;
	}
	
	/**
	 * Returns the {@link DatabaseType#translations} database
	 */
	public KBDatabase<Integer, DbTranslations> getDbTranslations() {
		return dbTranslations;
	}

	/**
	 * Returns the {@link DatabaseType#conceptByPageId} database
	 */
	public KBDatabase<Integer,String> getDbConceptByPageId() {
		return dbConceptByPageId;
	}

	@Override
	protected void initDatabases() {
		System.out.println("init Environment for language " + conf.getLangCode());
		
		KBDatabaseFactory dbFactory = new KBDatabaseFactory(this);
		
		databasesByType = new HashMap<DatabaseType, KBDatabase>();
		
		dbPage = dbFactory.buildPageDatabase();
		databasesByType.put(DatabaseType.page, dbPage);
		
		dbLabel = dbFactory.buildLabelDatabase();
		databasesByType.put(DatabaseType.label, dbLabel);
		
		//processedLabelDbs = new HashMap<String, LabelDatabase>();
		
		//dbLabelsForPage = dbFactory.buildPageLabelDatabase();
		//databasesByType.put(DatabaseType.pageLabel, dbLabelsForPage);
		
		dbArticlesByTitle = dbFactory.buildTitleDatabase(DatabaseType.articlesByTitle);
		databasesByType.put(DatabaseType.articlesByTitle, dbArticlesByTitle);
		dbCategoriesByTitle = dbFactory.buildTitleDatabase(DatabaseType.categoriesByTitle);
		databasesByType.put(DatabaseType.categoriesByTitle, dbCategoriesByTitle);
		dbTemplatesByTitle = dbFactory.buildTitleDatabase(DatabaseType.templatesByTitle);
		databasesByType.put(DatabaseType.templatesByTitle, dbTemplatesByTitle);
		
		//dbPageLinkIn = dbFactory.buildPageLinkDatabase(DatabaseType.pageLinksIn); 
		//databasesByType.put(DatabaseType.pageLinksIn, dbPageLinkIn);
		dbPageLinkInNoSentences = dbFactory.buildPageLinkNoSentencesDatabase(DatabaseType.pageLinksInNoSentences); 
		databasesByType.put(DatabaseType.pageLinksInNoSentences, dbPageLinkInNoSentences);
		
		//dbPageLinkOut = dbFactory.buildPageLinkDatabase(DatabaseType.pageLinksOut); 
		//databasesByType.put(DatabaseType.pageLinksOut, dbPageLinkOut);
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
		
		//dbSentenceSplits = dbFactory.buildIntIntListDatabase(DatabaseType.sentenceSplits);
		//databasesByType.put(DatabaseType.sentenceSplits, dbSentenceSplits);
		
		dbTranslations = dbFactory.buildTranslationsDatabase();
		databasesByType.put(DatabaseType.translations, dbTranslations);
		
		//dbRelations = dbFactory.buildInfoBoxRelationDatabase();
		//databasesByType.put(DatabaseType.relations, dbRelations);

		dbConceptByPageId = dbFactory.buildDbConceptByPageIdDatabase();
		databasesByType.put(DatabaseType.conceptByPageId, dbConceptByPageId);

		//dbProperties = dbFactory.buildInfoBoxPropertyDatabase();
		//databasesByType.put(DatabaseType.properties, dbProperties);

		dbStatistics = dbFactory.buildStatisticsDatabase();
		databasesByType.put(DatabaseType.statistics, dbStatistics);
	}

	/**
	 * @param sn the name of the desired statistic
	 * @return the value of the desired statistic
	 */
	public Long retrieveStatistic(StatisticName sn) {
		return dbStatistics.retrieve(sn.ordinal());
	}
	
	/**
	 * Builds a KBEnvironment, by loading all of the data files stored in the given directory into persistent databases.
	 * 
	 * It will not create the environment or any databases unless all of the required files are found in the given directory. 
	 * 
	 * It will not delete any existing databases, and will only overwrite them if explicitly specified (even if they are incomplete).
	 * 
	 * @param conf a configuration specifying where the databases are to be stored, etc.
	 * @param overwrite true if existing databases should be overwritten, otherwise false
	 * @throws IOException if any of the required files cannot be read
	 * @throws XMLStreamException if the XML dump of wikipedia cannot be parsed
	 */
	@Override
	public void buildEnvironment(NerdConfig conf, boolean overwrite) throws Exception {
		System.out.println("building Environment for language " + conf.getLangCode());	
		//check all files exist and are readable before doing anything
		
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

		//File sentenceSplits = getDataFile(dataDirectory, "sentenceSplits.csv");
		
		File wikidata = getDataFile(dataDirectory, "wikidata.txt");

		File translations = getDataFile(dataDirectory, "translations.csv");
		//File infoboxes = getDataFile(dataDirectory, "infoboxes.csv");

		File markup = getMarkupDataFile(dataDirectory);
		
		//now load databases
		File dbDirectory = new File(conf.getDbDirectory());
		if (!dbDirectory.exists())
			dbDirectory.mkdirs();

		//System.out.println("Building statistics db");
		dbStatistics.loadFromFile(statistics, overwrite);

		//System.out.println("Building Page db");
		dbPage.loadFromFile(page, overwrite);

		//System.out.println("Building Label db");
		dbLabel.loadFromFile(label, overwrite);

		//System.out.println("Building LabelsForPage db");
		//dbLabelsForPage.loadFromFile(pageLabel, overwrite);
		
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
		
		//System.out.println("Building PageLinkIn db");
		//dbPageLinkIn.loadFromFile(pageLinksIn, overwrite);

		//System.out.println("Building PageLinkInNoSentences db");
		dbPageLinkInNoSentences.loadFromFile(pageLinksIn, overwrite);

		//System.out.println("Building PageLinkOut db");
		//dbPageLinkOut.loadFromFile(pageLinksOut, overwrite);
		
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
		
		//System.out.println("Building SentenceSplits db");
		//dbSentenceSplits.loadFromFile(sentenceSplits, overwrite);
		
		//System.out.println("Building Translations db");
		dbTranslations.loadFromFile(translations, overwrite);
		
		//System.out.println("Building conceptbyPage db");
		dbConceptByPageId.loadFromFile(wikidata, overwrite);

		//System.out.println("Building Relations db");
		//dbRelations.loadFromFile(infoboxes, overwrite);

		//System.out.println("Building Properties db");
		//dbProperties.loadFromFile(infoboxes, overwrite);

		//System.out.println("Building Markup db");
		dbMarkup.loadFromXmlFile(markup, overwrite);

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
		//System.out.println("Building MarkupFull db");
		dbMarkupFull.loadFromXmlFile(markup, overwrite);

		System.out.println("Full markup database built - " + dbPage.getDatabaseSize() + " pages.");
	}
	
	private static File getMarkupDataFile(File dataDirectory) throws IOException {
		File[] files = dataDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith("-pages-articles.xml") || name.endsWith("-pages-articles.xml.bz2");
			}
		});
		
		if ((files == null) || (files.length == 0)) {
			Logger.getLogger(KBEnvironment.class).info("Could not locate markup file in " + dataDirectory);
			return null;
		}
		else { 
			if (files.length > 1)
				Logger.getLogger(KBEnvironment.class).info("There are multiple markup files in " + dataDirectory);
		
			if (!files[0].canRead())
				Logger.getLogger(KBEnvironment.class).info(files[0] + " is not readable");
		}
		return files[0];
	}

}
