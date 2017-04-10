package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.utilities.*;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.*;
import java.math.BigInteger;

import org.nustaq.serialization.*;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.compress.compressors.CompressorException;

import org.apache.log4j.Logger;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import org.wikipedia.miner.db.struct.*; 
import com.scienceminer.nerd.kb.model.Wikipedia;

import org.apache.hadoop.record.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A KB corresponding to a Wikipedia instance, which is concretely stored as a set of LMDB databases.
 * 
 */
public class KBEnvironment  {
	
	// this is the singleton FST configuration for all serialization operations with the KB
	private static FSTConfiguration singletonConf = FSTConfiguration.createDefaultConfiguration();
    //private static FSTConfiguration singletonConf = FSTConfiguration.createUnsafeBinaryConfiguration(); 

    public static FSTConfiguration getFSTConfigurationInstance() {
        return singletonConf;
    }
	
	/**
	 * Serialization in the KBEnvironment with FST
	 */
    public static byte[] serialize(Object obj) throws IOException {
    	byte data[] = getFSTConfigurationInstance().asByteArray(obj);
		return data;
	}

	/**
	 * Deserialization in the KBEnvironment with FST. The returned Object needs to be casted
	 * in the expected actual object. 
	 */
	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		return getFSTConfigurationInstance().asObject(data);
	}

	// NERD configuration for the KB instance
	private NerdConfig conf = null;

	// the different databases of the KB
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
	private RelationDatabase dbRelations = null;
	private PropertyDatabase dbProperties = null;

	// database registry for the environment
	//@SuppressWarnings("unchecked")
	private HashMap<DatabaseType, KBDatabase> databasesByType = null;
	
	/**
	 * Constructor
	 */	
	public KBEnvironment(NerdConfig conf) {
		this.conf = conf;
		// register classes to be serialized
		singletonConf.registerClass(DbPage.class, DbIntList.class, DbTranslations.class);
		initDatabases();
	}

	/**
	 * Returns the configuration of this environment
	 */
	public NerdConfig getConfiguration() {
		return conf;
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
	 * Returns the {@link DatabaseType#pageLabel} database
	 */
	/*public KBDatabase<Integer, DbLabelForPageList> getDbLabelsForPage() {
		return dbLabelsForPage;
	}*/
	
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
	 * Returns the {@link DatabaseType#pageLinksIn} database
	 */
	/*public KBDatabase<Integer, DbLinkLocationList> getDbPageLinkIn() {
		return dbPageLinkIn;
	}*/
	
	/**
	 * Returns the {@link DatabaseType#pageLinksInNoSentences} database
	 */
	public KBDatabase<Integer, DbIntList> getDbPageLinkInNoSentences() {
		return dbPageLinkInNoSentences;
	}
	

	/**
	 * Returns the {@link DatabaseType#pageLinksOut} database
	 */
	/*public KBDatabase<Integer, DbLinkLocationList> getDbPageLinkOut() {
		return dbPageLinkOut;
	}*/
	
	
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
	 * Returns the {@link DatabaseType#sentenceSplits} database
	 */
	/*public KBDatabase<Integer, DbIntList> getDbSentenceSplits() {
		return dbSentenceSplits;
	}*/
	
	/**
	 * Returns the {@link DatabaseType#translations} database
	 */
	public KBDatabase<Integer, DbTranslations> getDbTranslations() {
		return dbTranslations;
	}
	
	//@SuppressWarnings("unchecked")
	private void initDatabases() {
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
		
		dbRelations = dbFactory.buildInfoBoxRelationDatabase();
		databasesByType.put(DatabaseType.relations, dbRelations);

		dbProperties = dbFactory.buildInfoBoxPropertyDatabase();
		databasesByType.put(DatabaseType.properties, dbProperties);

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

	/*public ConcurrentMap getValidArticleIds(int minLinkCount) {
		ConcurrentMap pageIds = new ConcurrentHashMap();

		System.out.println("gathering valid page ids");
		KBIterator iter = dbPageLinkInNoSentences.getIterator();
		
		while (iter.hasNext()) {
			Entry entry = iter.next();
			byte[] keyData = entry.getKey();
			byte[] valueData = entry.getValue();
			try {
				KBEntry<Integer, DbLinkLocationList> e = 
					new KBEntry<Integer, DbLinkLocationList>(new BigInteger(keyData).intValue(), 
															(DbLinkLocationList)Utilities.deserialize(valueData));
				if (e.getValue().getLinkLocations().size() > minLinkCount) 
					pageIds.put(e.getKey(), e.getKey()); // PL: why a map ??
			} catch(Exception e) {
				Logger.getLogger(KBEnvironment.class).error("Failed deserialize");
			}
		}
		
		iter.close();
		return pageIds;
	}*/

	//@SuppressWarnings("unchecked")
	private KBDatabase getDatabase(DatabaseType dbType) {
		return databasesByType.get(dbType);
	}
	
	//@SuppressWarnings("unchecked")
	public void close() {
		/*for (KBDatabase<String, DbLabel> dbProcessedLabel: processedLabelDbs.values()) {
			dbProcessedLabel.close();
		}*/
		
		for (KBDatabase db:this.databasesByType.values()) {
			db.close();
		}
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
	public void buildEnvironment(NerdConfig conf, boolean overwrite) throws IOException, XMLStreamException, CompressorException {
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
		
		File translations = getDataFile(dataDirectory, "translations.csv");
		File infoboxes = getDataFile(dataDirectory, "infoboxes.csv");

		File markup = getMarkupDataFile(dataDirectory);
		
		//now load databases
		File dbDirectory = new File(conf.getDbDirectory());
		if (!dbDirectory.exists())
			dbDirectory.mkdirs();

		//System.out.println("Building statistics db");
		dbStatistics.loadFromCsvFile(statistics, overwrite);

		//System.out.println("Building Page db");
		dbPage.loadFromCsvFile(page, overwrite);

		//System.out.println("Building Label db");
		dbLabel.loadFromCsvFile(label, overwrite);

		//System.out.println("Building LabelsForPage db");
		//dbLabelsForPage.loadFromCsvFile(pageLabel, overwrite);
		
		//System.out.println("Building ArticlesByTitle db");
		dbArticlesByTitle.loadFromCsvFile(page, overwrite);

		//System.out.println("Building CategoriesByTitle db");
		dbCategoriesByTitle.loadFromCsvFile(page, overwrite);

		//System.out.println("Building TemplatesByTitle db");
		dbTemplatesByTitle.loadFromCsvFile(page, overwrite);
		
		//System.out.println("Building RedirectTargetBySource db");
		dbRedirectTargetBySource.loadFromCsvFile(redirectTargetBySource, overwrite);

		//System.out.println("Building RedirectSourcesByTarget db");
		dbRedirectSourcesByTarget.loadFromCsvFile(redirectSourcesByTarget, overwrite);
		
		//System.out.println("Building PageLinkIn db");
		//dbPageLinkIn.loadFromCsvFile(pageLinksIn, overwrite);

		//System.out.println("Building PageLinkInNoSentences db");
		dbPageLinkInNoSentences.loadFromCsvFile(pageLinksIn, overwrite);

		//System.out.println("Building PageLinkOut db");
		//dbPageLinkOut.loadFromCsvFile(pageLinksOut, overwrite);
		
		//System.out.println("Building PageLinkOutNoSentences db");
		dbPageLinkOutNoSentences.loadFromCsvFile(pageLinksOut, overwrite);
		
		//System.out.println("Building PageLinkCounts db");
		dbPageLinkCounts.loadFromCsvFiles(pageLinksIn, pageLinksOut, overwrite);
		
		//System.out.println("Building CategoryParents db");
		dbCategoryParents.loadFromCsvFile(categoryParents, overwrite);
		
		//System.out.println("Building ArticleParents db");
		dbArticleParents.loadFromCsvFile(articleParents, overwrite);
		
		//System.out.println("Building ChildCategories db");
		dbChildCategories.loadFromCsvFile(childCategories, overwrite);
		
		//System.out.println("Building ChildArticles db");
		dbChildArticles.loadFromCsvFile(childArticles, overwrite);
		
		//System.out.println("Building SentenceSplits db");
		//dbSentenceSplits.loadFromCsvFile(sentenceSplits, overwrite);
		
		//System.out.println("Building Translations db");
		dbTranslations.loadFromCsvFile(translations, overwrite);
		
		//System.out.println("Building Relations db");
		dbRelations.loadFromCsvFile(infoboxes, overwrite);

		//System.out.println("Building Properties db");
		dbProperties.loadFromCsvFile(infoboxes, overwrite);

		//System.out.println("Building Markup db");
		dbMarkup.loadFromXmlFile(markup, overwrite);

		System.out.println("Environment built - " + dbPage.getDatabaseSize() + " pages.");
	}
	
	/**
	 * The full markup database is built separately because it is only required for training
	 * purposes. 
	 */
	public void buildFullMarkup(NerdConfig conf, boolean overwrite) throws IOException, XMLStreamException, CompressorException {
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

	private static File getDataFile(File dataDirectory, String fileName) throws IOException {
		File file = new File(dataDirectory + File.separator + fileName);
		if (!file.canRead()) {
			Logger.getLogger(KBEnvironment.class).info(file + " is not readable");
			return null;
		} else
			return file;
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

	/**
	 * Statistics available about a wikipedia dump
	 */
	public enum StatisticName {
		/**
		 * The number of articles (not disambiguations or redirects) available
		 */
		articleCount,
		
		/**
		 * The number of categories available
		 */
		categoryCount,
		
		/**
		 * The number of disambiguation pages available
		 */
		disambiguationCount,
		
		/**
		 * The number of redirects available
		 */
		redirectCount,
		
		/**
		 * A long value representation of the date and time this dump was last edited -- use new Date(long) to get to parse
		 */
		lastEdit,
		
		/**
		 * The maximum path length between articles and the root category 
		 */
		maxCategoryDepth,
		
		/**
		 * The id of root category, below which all articles should be organized 
		 */
		rootCategoryId 
	}

}
