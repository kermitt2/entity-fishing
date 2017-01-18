package org.wikipedia.miner.db;

import com.scienceminer.nerd.utilities.*;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.*;
import java.math.BigInteger;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.compress.compressors.CompressorException;

import org.apache.log4j.Logger;

import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.db.struct.*; 
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.ProgressTracker;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.util.text.TextProcessor;

import org.apache.hadoop.record.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A wrapper for {@link Environment}, that keeps track of all of the databases required for a single dump of Wikipedia.
 * 
 */
public class WEnvironment  {
	
	/**
	 * Statistics available about a wikipedia dump
	 */
	public enum StatisticName 
	{
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
	

	private WikipediaConfiguration conf ;
	private PreparationThread prepThread ;
	
	private WDatabase<Integer, DbPage> dbPage ;
	private LabelDatabase dbLabel ;
	private HashMap<String, LabelDatabase> processedLabelDbs ;
	
	private WDatabase<Integer, DbLabelForPageList> dbLabelsForPage ; 
	
	private WDatabase<String,Integer> dbArticlesByTitle ;
	private WDatabase<String,Integer> dbCategoriesByTitle ;
	private WDatabase<String,Integer> dbTemplatesByTitle ;
	
	private WDatabase<Integer,Integer> dbRedirectTargetBySource ;
	private WDatabase<Integer,DbIntList> dbRedirectSourcesByTarget ;
	
	private WDatabase<Integer, DbLinkLocationList> dbPageLinkIn ;
	private WDatabase<Integer, DbIntList> dbPageLinkInNoSentences ;
	private WDatabase<Integer, DbLinkLocationList> dbPageLinkOut ;
	private WDatabase<Integer, DbIntList> dbPageLinkOutNoSentences ;
	private PageLinkCountDatabase dbPageLinkCounts ;
	
	private WDatabase<Integer, DbIntList> dbCategoryParents ;
	private WDatabase<Integer, DbIntList> dbArticleParents ;
	private WDatabase<Integer, DbIntList> dbChildCategories ;
	private WDatabase<Integer, DbIntList> dbChildArticles ;
	
	private MarkupDatabase dbMarkup ;
	private WDatabase<Integer, DbIntList> dbSentenceSplits ;
	
	private WDatabase<Integer, DbTranslations> dbTranslations ;
	
	private WDatabase<Integer, Long> dbStatistics ;
	
	
	@SuppressWarnings("unchecked")
	private HashMap<DatabaseType, WDatabase> databasesByType ;
	
	
	/**
	 * Returns the configuration of this environment
	 * 
	 * @return the configuration of this environment
	 */
	public WikipediaConfiguration getConfiguration() {
		return conf ;
	}
	
	/**
	 * Returns the {@link DatabaseType#page} database
	 * 
	 * @return see {@link DatabaseType#page} 
	 */
	public WDatabase<Integer, DbPage> getDbPage() {
		return dbPage;
	}

	/**
	 * Returns the {@link DatabaseType#label} database for the given text processor
	 * 
	 * @param textProcessor the text processor that should be applied to labels before indexing or searching (or null if the original label database is required)
	 * @return see {@link DatabaseType#label} 
	 */
	public LabelDatabase getDbLabel(TextProcessor textProcessor) {
		
		if (textProcessor == null)
			return dbLabel;
		else {
			LabelDatabase db = processedLabelDbs.get(textProcessor.getName()) ;
			
			if (db == null) {
				db = new LabelDatabase(this, textProcessor) ;
				
				processedLabelDbs.put(textProcessor.getName(), db) ;
			}
			return db ;
		}
	}
	
	/**
	 * Returns the {@link DatabaseType#pageLabel} database
	 * 
	 * @return see {@link DatabaseType#pageLabel} 
	 */
	public WDatabase<Integer, DbLabelForPageList> getDbLabelsForPage() {
		return dbLabelsForPage ;
	}
	
	/**
	 * Returns the {@link DatabaseType#articlesByTitle} database
	 * 
	 * @return see {@link DatabaseType#articlesByTitle} 
	 */
	public WDatabase<String, Integer> getDbArticlesByTitle() {
		return dbArticlesByTitle ;
	}
	
	/**
	 * Returns the {@link DatabaseType#categoriesByTitle} database
	 * 
	 * @return see {@link DatabaseType#categoriesByTitle} 
	 */
	public WDatabase<String, Integer> getDbCategoriesByTitle() {
		return dbCategoriesByTitle ;
	}
	
	/**
	 * Returns the {@link DatabaseType#templatesByTitle} database
	 * 
	 * @return see {@link DatabaseType#templatesByTitle} 
	 */
	public WDatabase<String, Integer> getDbTemplatesByTitle() {
		return dbTemplatesByTitle ;
	}

	
	/**
	 * Returns the {@link DatabaseType#redirectTargetBySource} database
	 * 
	 * @return see {@link DatabaseType#redirectTargetBySource} 
	 */
	public WDatabase<Integer, Integer> getDbRedirectTargetBySource() {
		return dbRedirectTargetBySource ;
	}
	
	/**
	 * Returns the {@link DatabaseType#redirectSourcesByTarget} database
	 * 
	 * @return see {@link DatabaseType#redirectSourcesByTarget} 
	 */
	public WDatabase<Integer, DbIntList> getDbRedirectSourcesByTarget() {
		return dbRedirectSourcesByTarget ;
	}

	/**
	 * Returns the {@link DatabaseType#pageLinksIn} database
	 * 
	 * @return see {@link DatabaseType#pageLinksIn} 
	 */
	public WDatabase<Integer, DbLinkLocationList> getDbPageLinkIn() {
		return dbPageLinkIn;
	}
	
	/**
	 * Returns the {@link DatabaseType#pageLinksInNoSentences} database
	 * 
	 * @return see {@link DatabaseType#pageLinksInNoSentences} 
	 */
	public WDatabase<Integer, DbIntList> getDbPageLinkInNoSentences() {
		return dbPageLinkInNoSentences;
	}
	

	/**
	 * Returns the {@link DatabaseType#pageLinksOut} database
	 * 
	 * @return see {@link DatabaseType#pageLinksOut} 
	 */
	public WDatabase<Integer, DbLinkLocationList> getDbPageLinkOut() {
		return dbPageLinkOut;
	}
	
	
	/**
	 * Returns the {@link DatabaseType#pageLinksOutNoSentences} database
	 * 
	 * @return see {@link DatabaseType#pageLinksOutNoSentences} 
	 */
	public WDatabase<Integer, DbIntList> getDbPageLinkOutNoSentences() {
		return dbPageLinkOutNoSentences;
	}
	
	/**
	 * Returns the {@link DatabaseType#pageLinkCounts} database
	 * 
	 * @return see {@link DatabaseType#pageLinkCounts} 
	 */
	public WDatabase<Integer, DbPageLinkCounts> getDbPageLinkCounts() {
		return dbPageLinkCounts;
	}
	
	/**
	 * Returns the {@link DatabaseType#categoryParents} database
	 * 
	 * @return see {@link DatabaseType#categoryParents} 
	 */
	public WDatabase<Integer, DbIntList> getDbCategoryParents() {
		return dbCategoryParents;
	}

	/**
	 * Returns the {@link DatabaseType#articleParents} database
	 * 
	 * @return see {@link DatabaseType#articleParents} 
	 */
	public WDatabase<Integer, DbIntList> getDbArticleParents() {
		return dbArticleParents;
	}

	/**
	 * Returns the {@link DatabaseType#childCategories} database
	 * 
	 * @return see {@link DatabaseType#childCategories} 
	 */
	public WDatabase<Integer, DbIntList> getDbChildCategories() {
		return dbChildCategories;
	}

	/**
	 * Returns the {@link DatabaseType#childArticles} database
	 * 
	 * @return see {@link DatabaseType#childArticles} 
	 */
	public WDatabase<Integer, DbIntList> getDbChildArticles() {
		return dbChildArticles;
	}

	/**
	 * Returns the {@link DatabaseType#markup} database
	 * 
	 * @return see {@link DatabaseType#markup} 
	 */
	public MarkupDatabase getDbMarkup() {
		return dbMarkup;
	}
	
	/**
	 * Returns the {@link DatabaseType#sentenceSplits} database
	 * 
	 * @return see {@link DatabaseType#sentenceSplits} 
	 */
	public WDatabase<Integer, DbIntList> getDbSentenceSplits() {
		return dbSentenceSplits;
	}
	

	/**
	 * Returns the {@link DatabaseType#translations} database
	 * 
	 * @return see {@link DatabaseType#translations} 
	 */
	public WDatabase<Integer, DbTranslations> getDbTranslations() {
		return dbTranslations ;
	}

	/**
	 * Intitializes the environment defined in the given configuration, and immediately begins connecting to databases and caching them to memory.
	 * 
	 * This preparation can be done in a separate thread if required, in which case progress can be tracked using {@link #getProgress()}, {@link #getPreparationTracker()} and {@link #isReady()}.
	 * 
	 * @param conf configuration options
	 * @param threaded true if this should be prepared (e.g. cached to memory) in a separate thread, otherwise false
	 * @throws EnvironmentLockedException if the underlying {@link Environment} is unavailable
	 */
	//public WEnvironment(WikipediaConfiguration conf, boolean threaded) {

		//this.conf = conf ;
		
		/*EnvironmentConfig envConf = new EnvironmentConfig() ;
		envConf.setAllowCreate(false) ;
		envConf.setReadOnly(true) ;
		envConf.setCachePercent(10) ;
		env = new Environment(conf.getDatabaseDirectory(), envConf) ;*/
		
		//initDatabases();
		
		/*prepThread = new PreparationThread(conf) ;
		if (threaded)
			prepThread.start() ;
		else
			prepThread.doPreparation() ;*/
		/*try {
			buildEnvironment(this.conf, this.conf.getDataDirectory(), true);
		} catch(Exception e) {
			e.printStackTrace();
		}*/
	//}
	
	public WEnvironment(WikipediaConfiguration conf) {
		
		this.conf = conf ;
		
		initDatabases() ;
		//conf.setArticlesOfInterest(getValidArticleIds(conf.getMinLinksIn(), null)) ;
		
		/*EnvironmentConfig envConf = new EnvironmentConfig() ;
		envConf.setCachePercent(10) ;
		
		envConf.setAllowCreate(true) ;
		envConf.setReadOnly(false) ;
		
		env = new Environment(conf.getDatabaseDirectory(), envConf) ;*/
		/*try {
			buildEnvironment(this.conf, this.conf.getDataDirectory(), true);
		} catch(Exception e) {
			e.printStackTrace();
		}*/
	}
	
	@SuppressWarnings("unchecked")
	private void initDatabases() {
		System.out.println("init Environment for language " + conf.getLangCode());
		
		WDatabaseFactory dbFactory = new WDatabaseFactory(this) ;
		
		databasesByType = new HashMap<DatabaseType, WDatabase>() ;
		
		dbPage = dbFactory.buildPageDatabase();
		databasesByType.put(DatabaseType.page, dbPage) ;
		
		dbLabel = dbFactory.buildLabelDatabase() ;
		databasesByType.put(DatabaseType.label, dbLabel) ;
		
		processedLabelDbs = new HashMap<String, LabelDatabase>() ;
		
		dbLabelsForPage = dbFactory.buildPageLabelDatabase() ;
		databasesByType.put(DatabaseType.pageLabel, dbLabelsForPage) ;
		
		dbArticlesByTitle = dbFactory.buildTitleDatabase(DatabaseType.articlesByTitle) ;
		databasesByType.put(DatabaseType.articlesByTitle, dbArticlesByTitle) ;
		dbCategoriesByTitle = dbFactory.buildTitleDatabase(DatabaseType.categoriesByTitle) ;
		databasesByType.put(DatabaseType.categoriesByTitle, dbCategoriesByTitle) ;
		dbTemplatesByTitle = dbFactory.buildTitleDatabase(DatabaseType.templatesByTitle) ;
		databasesByType.put(DatabaseType.templatesByTitle, dbTemplatesByTitle) ;
		
		dbPageLinkIn = dbFactory.buildPageLinkDatabase(DatabaseType.pageLinksIn) ; 
		databasesByType.put(DatabaseType.pageLinksIn, dbPageLinkIn) ;
		dbPageLinkInNoSentences = dbFactory.buildPageLinkNoSentencesDatabase(DatabaseType.pageLinksInNoSentences) ; 
		databasesByType.put(DatabaseType.pageLinksInNoSentences, dbPageLinkInNoSentences) ;
		
		dbPageLinkOut = dbFactory.buildPageLinkDatabase(DatabaseType.pageLinksOut) ; 
		databasesByType.put(DatabaseType.pageLinksOut, dbPageLinkOut) ;
		dbPageLinkOutNoSentences = dbFactory.buildPageLinkNoSentencesDatabase(DatabaseType.pageLinksOutNoSentences) ; 
		databasesByType.put(DatabaseType.pageLinksOutNoSentences, dbPageLinkOutNoSentences) ;
		
		dbPageLinkCounts = dbFactory.buildPageLinkCountDatabase() ;
		databasesByType.put(DatabaseType.pageLinkCounts, dbPageLinkCounts) ;
		
		dbCategoryParents = dbFactory.buildIntIntListDatabase(DatabaseType.categoryParents) ;
		databasesByType.put(DatabaseType.categoryParents, dbCategoryParents) ;
		dbArticleParents = dbFactory.buildIntIntListDatabase(DatabaseType.articleParents) ;
		databasesByType.put(DatabaseType.articleParents, dbArticleParents) ;
		dbChildCategories = dbFactory.buildIntIntListDatabase(DatabaseType.childCategories) ;
		databasesByType.put(DatabaseType.childCategories, dbChildCategories) ;
		dbChildArticles = dbFactory.buildIntIntListDatabase(DatabaseType.childArticles) ;
		databasesByType.put(DatabaseType.childArticles, dbChildArticles) ;
		
		dbRedirectSourcesByTarget = dbFactory.buildIntIntListDatabase(DatabaseType.redirectSourcesByTarget) ;
		databasesByType.put(DatabaseType.redirectSourcesByTarget, dbRedirectSourcesByTarget) ;
		dbRedirectTargetBySource = dbFactory.buildRedirectTargetBySourceDatabase() ;
		databasesByType.put(DatabaseType.redirectTargetBySource, dbRedirectTargetBySource) ;
		
		dbMarkup = new MarkupDatabase(this) ;
		databasesByType.put(DatabaseType.markup, dbMarkup) ;
		
		dbSentenceSplits = dbFactory.buildIntIntListDatabase(DatabaseType.sentenceSplits) ;
		databasesByType.put(DatabaseType.sentenceSplits, dbSentenceSplits) ;
		
		dbTranslations = dbFactory.buildTranslationsDatabase() ;
		databasesByType.put(DatabaseType.translations, dbTranslations) ;
		
		dbStatistics = dbFactory.buildStatisticsDatabase() ;
		databasesByType.put(DatabaseType.statistics, dbStatistics) ;
	}
	
	
	/**
	 * @return true if the preparation work has been completed, otherwise false
	 */
	public boolean isReady() {
		return prepThread.isCompleted() ;
		
	}
	
	/**
	 * @return a number between 0 (just started) and 1 (completed) indicating progress of the preparation work. 
	 */
	public double getProgress() {
		return prepThread.getProgress() ;
	}
	
	/**
	 * @return a tracker for progress of the preparation work. 
	 */
	public ProgressTracker getPreparationTracker() {
		return prepThread.getTracker() ;
	}
	

	/**
	 * @param sn the name of the desired statistic
	 * @return the value of the desired statistic
	 */
	public Long retrieveStatistic(StatisticName sn) {
		return dbStatistics.retrieve(sn.ordinal()) ;
	}
	
	/**
	 * @param tp a text processor
	 * @return true if the environment is ready to be searched for labels using the given text processor, otherwise false 
	 */
	public boolean isPreparedFor(TextProcessor tp) {
		//LabelDatabase db = getDbLabel(tp) ;
		if (dbLabel == null)
			return false;
		else
			return dbLabel.isLoaded() ;	
	}
	
	
	/**
	 * Identifies the set of valid article ids which fit the given constrains. Useful for specifying a subset of 
	 * articles that we are interested in caching.
	 * 
	 * @param minLinkCount the minimum number of links that an article must receive from other articles to be included
	 * @param tracker an optional progress notifier
	 * @return the set of valid ids which fit the given constrains. 
	 */
	/*public TIntHashSet getValidArticleIds(int minLinkCount, ProgressTracker tracker) {
		
		//TODO: ideally this should advance a page iterator at the same time, to check page type
		//TODO: ideally this should advance a pageLinkOut iterator at the same time, to check minimum outlinks
		
		TIntHashSet pageIds = new TIntHashSet() ;

		if (tracker == null) tracker = new ProgressTracker(1, WEnvironment.class) ;
		tracker.startTask(dbPageLinkIn.getDatabaseSize(), "gathering valid page ids") ;

		WIterator<Integer, DbLinkLocationList> iter = dbPageLinkIn.getIterator() ;
		
		while (iter.hasNext()) {
			
			WEntry<Integer, DbLinkLocationList> e = iter.next() ;
						
			if (e.getValue().getLinkLocations().size() > minLinkCount) 
				pageIds.add(e.getKey()) ;
			
			tracker.update();
		}
		
		iter.close() ;
					
		return pageIds ;
	}*/

	public ConcurrentMap getValidArticleIds(int minLinkCount, ProgressTracker tracker) {
		
		//TODO: ideally this should advance a page iterator at the same time, to check page type
		//TODO: ideally this should advance a pageLinkOut iterator at the same time, to check minimum outlinks
		
		ConcurrentMap pageIds = new ConcurrentHashMap() ;

		if (tracker == null) tracker = new ProgressTracker(1, WEnvironment.class) ;
		tracker.startTask(dbPageLinkIn.getDatabaseSize(), "gathering valid page ids") ;

		//WIterator<Integer, DbLinkLocationList> iter = dbPageLinkIn.getIterator();
		WIterator iter = dbPageLinkIn.getIterator();
		
		while (iter.hasNext()) {
			//WEntry<Integer, DbLinkLocationList> e = iter.next() ;
			Entry entry = iter.next() ;
			byte[] keyData = entry.getKey();
			byte[] valueData = entry.getValue();
			try {
				WEntry<Integer, DbLinkLocationList> e = 
					new WEntry<Integer, DbLinkLocationList>(new BigInteger(keyData).intValue(), 
															(DbLinkLocationList)Utilities.deserialize(valueData));
				if (e.getValue().getLinkLocations().size() > minLinkCount) 
					pageIds.put(e.getKey(), e.getKey()); // PL: why a map ??
			} catch(Exception e) {
				Logger.getLogger(WEnvironment.class).error("Failed deserialize");
			}

			tracker.update();
		}
		
		iter.close() ;
		return pageIds ;
	}

	
	/*protected void cleanAndCheckpoint() throws DatabaseException{
		
		Logger.getLogger(WEnvironment.class).info("Starting cleaning") ;
		boolean anyCleaned = false;
		while (env.cleanLog() > 0) {
			System.out.println("cleaning") ;
			anyCleaned = true;
		}
		Logger.getLogger(WEnvironment.class).info("Finished cleaning") ;
		
		if (anyCleaned) {
			Logger.getLogger(WEnvironment.class).info("Starting checkpoint") ;
		
			CheckpointConfig force = new CheckpointConfig();
			force.setForce(true);
			env.checkpoint(force);
			
			Logger.getLogger(WEnvironment.class).info("Finished checkpoint") ;
		}
	}*/



	
	@SuppressWarnings("unchecked")
	private WDatabase getDatabase(DatabaseType dbType) {
		return databasesByType.get(dbType) ;
	}
	

	private class PreparationThread extends Thread {
		
		WikipediaConfiguration conf ;
		
		private ProgressTracker tracker ;		
		private boolean completed = false ;
		private Exception failureCause = null ;
		
		PreparationThread(WikipediaConfiguration conf) {
			this.conf = conf ;
		}
		
		public boolean isCompleted() {
			return completed ;
		}
		
		public boolean failed() {
			return (failureCause != null) ;
		}
		
		public double getProgress() {

			if (completed)
				return 1 ;

			if (tracker == null) 
				return 0 ;

			return tracker.getGlobalProgress() ;
		}
		
		public ProgressTracker getTracker() {
			return tracker ;
		}
		
		@Override
		public void run() {
			doPreparation() ;
		}

		public void doPreparation() {
			
			boolean mustGatherIds = conf.getMinLinksIn() > 0 && //!conf.getDatabasesToCache().isEmpty()) && 
				conf.getArticlesOfInterest() == null ;
			
			//int taskCount = conf.getDatabasesToCache().size() + 1;
			//if (mustGatherIds)
			//	taskCount++ ;
			
			//tracker = new ProgressTracker(taskCount, WEnvironment.class) ;
			
			try {
				//tracker.startTask(1, "Connecting to database") ;
				
				//dbStatistics.cache(conf, null) ;
				
				//tracker.update();
				
				if (mustGatherIds)
					conf.setArticlesOfInterest(getValidArticleIds(conf.getMinLinksIn(), null)) ;
				
				/*for(DatabaseType dbName : conf.getDatabasesToCache()) {
					if (dbName == DatabaseType.label)
						getDbLabel(conf.getDefaultTextProcessor()).cache(conf, tracker) ;
					else
						getDatabase(dbName).cache(conf, tracker) ;
				}*/
				
				//conf.setArticlesOfInterest(null) ;
				
				//System.gc() ;
				
			} catch (Exception e) {
				failureCause = e ;
			}

			completed = true ;
		}
	}
	
	public Exception getCachingFailureReason() {
		
		if (this.prepThread == null)
			return null ;
	
		return this.prepThread.failureCause ;
	}
	
	/**
	 * Tidily closes the environment, and all databases within it. This should always be called once you are finished with the environment.
	 */
	@SuppressWarnings("unchecked")
	public void close() {
		
		for (WDatabase<String, DbLabel> dbProcessedLabel: processedLabelDbs.values()) {
			dbProcessedLabel.close();
		}
		
		for (WDatabase db:this.databasesByType.values()) {
			db.close() ;
		}
	}
	
	/*@Override
	public void finalize() {
		if (env != null) {
			Logger.getLogger(WIterator.class).warn("Unclosed enviroment. You may be causing a memory leak.") ;
		}
	}*/
	
	
	
	
	/**
	 * Builds a WEnvironment, by loading all of the data files stored in the given directory into persistent databases.
	 * 
	 * It will not create the environment or any databases unless all of the required files are found in the given directory. 
	 * 
	 * It will not delete any existing databases, and will only overwrite them if explicitly specified (even if they are incomplete).
	 * 
	 * @param conf a configuration specifying where the databases are to be stored, etc.
	 * @param dataDirectory a directory containing the a single XML dump of wikipedia, and all of the CSV files produced by {@link DumpExtractor}
	 * @param overwrite true if existing databases should be overwritten, otherwise false
	 * @throws IOException if any of the required files cannot be read
	 * @throws XMLStreamException if the XML dump of wikipedia cannot be parsed
	 */
	public void buildEnvironment(WikipediaConfiguration conf, File dataDirectory, boolean overwrite) throws IOException, XMLStreamException, CompressorException {
System.out.println("building Environment for language " + conf.getLangCode());	
		//check all files exist and are readable before doing anything
		
		File statistics = getDataFile(dataDirectory, "stats.csv") ;
		File page = getDataFile(dataDirectory, "page.csv") ;
		File label = getDataFile(dataDirectory, "label.csv") ;
		File pageLabel = getDataFile(dataDirectory, "pageLabel.csv") ;
		
		File pageLinksIn = getDataFile(dataDirectory, "pageLinkIn.csv") ;
		File pageLinksOut = getDataFile(dataDirectory, "pageLinkOut.csv") ;
		
		File categoryParents = getDataFile(dataDirectory, "categoryParents.csv") ;
		File articleParents = getDataFile(dataDirectory, "articleParents.csv") ;
		File childCategories = getDataFile(dataDirectory, "childCategories.csv") ;
		File childArticles = getDataFile(dataDirectory, "childArticles.csv") ;
		
		File redirectTargetBySource = getDataFile(dataDirectory, "redirectTargetsBySource.csv") ;
		File redirectSourcesByTarget = getDataFile(dataDirectory, "redirectSourcesByTarget.csv") ;

		File sentenceSplits = getDataFile(dataDirectory, "sentenceSplits.csv") ;
		
		File translations = getDataFile(dataDirectory, "translations.csv") ;
		
		File markup = getMarkupDataFile(dataDirectory) ;
		
		//now load databases
		
		if (!conf.getDatabaseDirectory().exists())
			conf.getDatabaseDirectory().mkdirs() ;
		
		//WEnvironment env = new WEnvironment(conf) ;
		/*boolean mustGatherIds = conf.getMinLinksIn() > 0 && conf.getArticlesOfInterest() == null ;			
		try {
			if (mustGatherIds)
				conf.setArticlesOfInterest(env.getValidArticleIds(conf.getMinLinksIn(), null)) ;
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		//System.out.println("Building statistics db");
		dbStatistics.loadFromCsvFile(statistics, overwrite, null) ;

		//System.out.println("Building Page db");
		dbPage.loadFromCsvFile(page, overwrite, null) ;

		//System.out.println("Building Label db");
		dbLabel.loadFromCsvFile(label, overwrite, null) ;

		//System.out.println("Building LabelsForPage db");
		dbLabelsForPage.loadFromCsvFile(pageLabel, overwrite, null) ;
		
		//System.out.println("Building ArticlesByTitle db");
		dbArticlesByTitle.loadFromCsvFile(page, overwrite, null) ;

		//System.out.println("Building CategoriesByTitle db");
		dbCategoriesByTitle.loadFromCsvFile(page, overwrite, null) ;

		//System.out.println("Building TemplatesByTitle db");
		dbTemplatesByTitle.loadFromCsvFile(page, overwrite, null) ;
		
		//System.out.println("Building RedirectTargetBySource db");
		dbRedirectTargetBySource.loadFromCsvFile(redirectTargetBySource, overwrite, null) ;

		//System.out.println("Building RedirectSourcesByTarget db");
		dbRedirectSourcesByTarget.loadFromCsvFile(redirectSourcesByTarget, overwrite, null) ;
		
		//System.out.println("Building PageLinkIn db");
		dbPageLinkIn.loadFromCsvFile(pageLinksIn, overwrite, null) ;

		//System.out.println("Building PageLinkInNoSentences db");
		dbPageLinkInNoSentences.loadFromCsvFile(pageLinksIn, overwrite, null) ;

		//System.out.println("Building PageLinkOut db");
		dbPageLinkOut.loadFromCsvFile(pageLinksOut, overwrite, null) ;
		
		//System.out.println("Building PageLinkOutNoSentences db");
		dbPageLinkOutNoSentences.loadFromCsvFile(pageLinksOut, overwrite, null) ;
		
		//System.out.println("Building PageLinkCounts db");
		dbPageLinkCounts.loadFromCsvFiles(pageLinksIn, pageLinksOut, overwrite, null) ;
		
		//System.out.println("Building CategoryParents db");
		dbCategoryParents.loadFromCsvFile(categoryParents, overwrite, null) ;
		
		//System.out.println("Building ArticleParents db");
		dbArticleParents.loadFromCsvFile(articleParents, overwrite, null) ;
		
		//System.out.println("Building ChildCategories db");
		dbChildCategories.loadFromCsvFile(childCategories, overwrite, null) ;
		
		//System.out.println("Building ChildArticles db");
		dbChildArticles.loadFromCsvFile(childArticles, overwrite, null) ;
		
		//System.out.println("Building SentenceSplits db");
		dbSentenceSplits.loadFromCsvFile(sentenceSplits, overwrite, null) ;
		
		//System.out.println("Building Translations db");
		dbTranslations.loadFromCsvFile(translations, overwrite, null) ;
		
		//System.out.println("Building Markup db");
		dbMarkup.loadFromXmlFile(markup, overwrite, null) ;
		
		// create cache
		//if (!dbPage.isCached())
		//	dbPage.caching(conf);

		//env.close();
		
//		TextProcessor tp = conf.getDefaultTextProcessor() ;
//		if (tp != null) {
//			File tmpDir = new File(conf.getDataDirectory() + File.separator + "tmp" + tp.getName()) ;
//			tmpDir.mkdir() ;
//			tmpDir.deleteOnExit() ;
//			
//			prepareTextProcessor(tp, conf, tmpDir, overwrite, 5) ;
//		}
		System.out.println("Environment built - " + dbPage.getDatabaseSize() + " pages.");

	}
	
	/**
	 * Prepares the environment, so it can be searched efficiently for labels using the given text processor.
	 * 
	 * Note: you can use as many different text processors as you like
	 * 
	 * @see LabelDatabase#prepare(File, int)
	 * 
	 * @param tp a text processor
	 * @param conf a configuration specifying where the databases are to be stored, etc.
	 * @param tempDirectory a directory for writing temporary files
	 * @param overwrite true if the preparation should occur even if the environment has been prepared for this processor already
	 * @param passes the number of the number of passes to break the task into (more = slower, but less memory required)
	 * @throws IOException if the temporary directory is not writable
	 */
	public static void prepareTextProcessor(TextProcessor tp, WikipediaConfiguration conf, File tempDirectory, boolean overwrite, int passes) throws IOException {
		
		if (tp == null)
			return ;
		
		WEnvironment env = new WEnvironment(conf) ;
		
		if (!overwrite && env.isPreparedFor(tp))
			return ;
		
		LabelDatabase db = env.getDbLabel(tp) ;
		db.prepare(tempDirectory, passes) ;
		
		//env.cleanAndCheckpoint() ;
		env.close();
	}
	
	/*protected Environment getEnvironment() {
		return env ;
	}*/
	
	
	private static File getDataFile(File dataDirectory, String fileName) throws IOException {
		
		File file = new File(dataDirectory + File.separator + fileName) ;
		if (!file.canRead())
			throw new IOException(file + " is not readable") ;
		
		return file ;
	}
	
	private static File getMarkupDataFile(File dataDirectory) throws IOException {
		
		File[] files = dataDirectory.listFiles(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				return name.endsWith("-pages-articles.xml") || name.endsWith("-pages-articles.xml.bz2") ;
			}
		}) ;
		
		if (files.length == 0) 
			throw new IOException("Could not locate markup file in " + dataDirectory) ;
		
		if (files.length > 1) 
			throw new IOException("There are multiple markup files in " + dataDirectory) ;
		
		if (!files[0].canRead())
			throw new IOException(files[0] + " is not readable") ;
		
		return files[0] ;
	}
	
	

}
