package org.wikipedia.miner.db;


//import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.record.CsvRecordInput;
import org.apache.log4j.Logger;
import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.db.WEnvironment.StatisticName;
import org.wikipedia.miner.db.struct.*;
import org.wikipedia.miner.model.Page.PageType;
import org.wikipedia.miner.util.ProgressTracker;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.util.text.TextProcessor;


import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;


/**
 * A factory for creating WDatabases of various types
 */
public class WDatabaseFactory {
	
	WEnvironment env ;

	/**
	 * Creates a new WDatabaseFactory for the given WEnvironment
	 * 
	 * @param env a WEnvironment
	 */
	public WDatabaseFactory(WEnvironment env) {

		this.env = env ;
	}

	/**
	 * Returns a database associating page ids with the title, type and generality of the page. 
	 * 
	 * @return a database associating page ids with the title, type and generality of the page. 
	 */
	public WDatabase<Integer, DbPage> buildPageDatabase() {

		RecordBinding<DbPage> keyBinding = new RecordBinding<DbPage>() {
			public DbPage createRecordInstance() {
				return new DbPage() ;
			}
		} ;

		return new IntObjectDatabase<DbPage>(
				env, 
				DatabaseType.page,
				keyBinding
		) {
			@Override
			public WEntry<Integer,DbPage> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				Integer id = record.readInt(null) ;

				DbPage p = new DbPage() ;
				p.deserialize(record) ;

				return new WEntry<Integer,DbPage>(id, p) ;
			}

			@Override
			public DbPage filterCacheEntry(
					WEntry<Integer, DbPage> e, 
					WikipediaConfiguration conf
			) {

				PageType pageType = PageType.values()[e.getValue().getType()] ;

				//TIntHashSet validIds = conf.getArticlesOfInterest() ;
				ConcurrentMap validIds = conf.getArticlesOfInterest();
				
				//if (validIds == null || validIds.contains(e.getKey()) || pageType == PageType.category || pageType==PageType.redirect) 
				if (validIds == null || (validIds.get(e.getKey()) != null) || pageType == PageType.category || pageType==PageType.redirect) 
					return e.getValue() ;
				else
					return null ;
			}
		};
	}

	/**
	 * Returns a database associating article, category or template titles with their ids.
	 * 
	 * @param {@link DatabaseType#articlesByTitle}, {@link DatabaseType#templatesByTitle} or {@link DatabaseType#categoriesByTitle}.
	 * @return a database associating article, category or template titles with their ids.
	 */
	public WDatabase<String,Integer> buildTitleDatabase(DatabaseType type) {

		return new TitleDatabase(env, type) ;
	}

	/**
	 * Returns a database associating String labels with the statistics about the articles (senses) these labels could refer to.
	 * 
	 * @return a database associating String labels with the statistics about the articles (senses) these labels could refer to. 
	 */
	public LabelDatabase buildLabelDatabase() {
		return new LabelDatabase(env) ;
	}

	/**
	 * Returns a database associating String labels with the statistics about the articles (senses) these labels could refer to.
	 * 
	 * @param tp a text processor that should be applied to string labels before indexing and searching
	 * @return a database associating String labels with the statistics about the articles (senses) these labels could refer to 
	 */
	public LabelDatabase buildLabelDatabase(TextProcessor tp) {

		if (tp == null) 
			throw new IllegalArgumentException("text processor must not be null") ;

		return new LabelDatabase(env, tp) ;
	}

	/**
	 * Returns a database associating Integer page ids with the labels used to refer to that page
	 * 
	 * @return a database associating Integer page ids with the labels used to refer to that page
	 */
	public WDatabase<Integer,DbLabelForPageList> buildPageLabelDatabase() {

		RecordBinding<DbLabelForPageList> keyBinding = new RecordBinding<DbLabelForPageList>() {
			public DbLabelForPageList createRecordInstance() {
				return new DbLabelForPageList() ;
			}
		} ;

		return new IntObjectDatabase<DbLabelForPageList>(
				env, 
				DatabaseType.pageLabel, 
				keyBinding
		) {

			@Override
			public WEntry<Integer,DbLabelForPageList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {

				Integer id = record.readInt(null) ;

				DbLabelForPageList labels = new DbLabelForPageList() ;
				labels.deserialize(record) ;

				return new WEntry<Integer,DbLabelForPageList>(id, labels) ;
			}

			@Override
			public DbLabelForPageList filterCacheEntry(WEntry<Integer,DbLabelForPageList> e, WikipediaConfiguration conf) {

				//TIntHashSet validIds = conf.getArticlesOfInterest() ;
				ConcurrentMap validIds = conf.getArticlesOfInterest() ;
				
				//if (validIds != null && !validIds.contains(e.getKey()))
				if (validIds != null && (validIds.get(e.getKey()) == null) )	
					return null ;

				return e.getValue();
			}
		} ;
	}

	/**
	 * Returns a database associating Integer ids with the ids of articles it links to or that link to it, and the sentence indexes where these links are found.
	 * 
	 * @param type either {@link DatabaseType#pageLinksIn} or {@link DatabaseType#pageLinksOut}.
	 * @return a database associating Integer ids with the ids of articles it links to or that link to it, and the sentence indexes where these links are found
	 */
	public WDatabase<Integer, DbLinkLocationList> buildPageLinkDatabase(DatabaseType type) {

		if (type != DatabaseType.pageLinksIn && type != DatabaseType.pageLinksOut)
			throw new IllegalArgumentException("type must be either DatabaseType.pageLinksIn or DatabaseType.pageLinksOut") ;

		RecordBinding<DbLinkLocationList> keyBinding = new RecordBinding<DbLinkLocationList>() {
			public DbLinkLocationList createRecordInstance() {
				return new DbLinkLocationList() ;
			}
		} ;

		return new IntObjectDatabase<DbLinkLocationList>(
				env, 
				type, 
				keyBinding
		) {

			@Override
			public WEntry<Integer, DbLinkLocationList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {

				Integer id = record.readInt(null) ;

				DbLinkLocationList l = new DbLinkLocationList() ;
				l.deserialize(record) ;

				return new WEntry<Integer, DbLinkLocationList>(id, l) ;
			}

			@Override
			public DbLinkLocationList filterCacheEntry(
					WEntry<Integer, DbLinkLocationList> e,
					WikipediaConfiguration conf) {

				int id = e.getKey() ;
				DbLinkLocationList links = e.getValue() ;
				
				//TIntHashSet validIds = conf.getArticlesOfInterest() ;
				ConcurrentMap validIds = conf.getArticlesOfInterest() ;

				//if (validIds != null && !validIds.contains(id))
				if (validIds != null && (validIds.get(id) == null) )
					return null ;

				ArrayList<DbLinkLocation> newLinks = new ArrayList<DbLinkLocation>() ;

				for (DbLinkLocation ll:links.getLinkLocations()) {
					//if (validIds != null && !validIds.contains(ll.getLinkId()))
					if (validIds != null && (validIds.get(ll.getLinkId()) == null) )	
						continue ;

					newLinks.add(ll) ;
				}

				if (newLinks.size() == 0)
					return null ;

				links.setLinkLocations(newLinks) ;

				return links ;
			}

		} ;
	}

	
	/**
	 * Returns a database associating Integer ids with the ids of articles it links to or that link to it.
	 * 
	 * @param type either {@link DatabaseType#pageLinksIn} or {@link DatabaseType#pageLinksOut}.
	 * @return a database associating Integer ids with the ids of articles it links to or that link to it.
	 */
	public WDatabase<Integer, DbIntList> buildPageLinkNoSentencesDatabase(DatabaseType type) {

		if (type != DatabaseType.pageLinksInNoSentences && type != DatabaseType.pageLinksOutNoSentences)
			throw new IllegalArgumentException("type must be either DatabaseType.pageLinksInNoSentences or DatabaseType.pageLinksOutNoSentences") ;

		RecordBinding<DbIntList> keyBinding = new RecordBinding<DbIntList>() {
			public DbIntList createRecordInstance() {
				return new DbIntList() ;
			}
		} ;

		return new IntObjectDatabase<DbIntList>(
				env, 
				type, 
				keyBinding
		) {

			@Override
			public WEntry<Integer, DbIntList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				// this has to read from pagelinks file (with sentences
				
				Integer id = record.readInt(null) ;

				DbLinkLocationList l = new DbLinkLocationList() ;
				l.deserialize(record) ;
				
				ArrayList<Integer> linkIds = new ArrayList<Integer>() ;
				
				for (DbLinkLocation ll:l.getLinkLocations()) 
					linkIds.add(ll.getLinkId()) ;
				
				return new WEntry<Integer, DbIntList>(id, new DbIntList(linkIds)) ;
			}

			@Override
			public DbIntList filterCacheEntry(
					WEntry<Integer, DbIntList> e,
					WikipediaConfiguration conf) {

				int id = e.getKey() ;
				DbIntList links = e.getValue() ;
				
				//TIntHashSet validIds = conf.getArticlesOfInterest();
				ConcurrentMap validIds = conf.getArticlesOfInterest();

				//if (validIds != null && !validIds.contains(id))
				if (validIds != null && (validIds.get(id) == null))	
					return null ;

				ArrayList<Integer> newLinks = new ArrayList<Integer>() ;

				for (Integer link:links.getValues()) {
					//if (validIds != null && !validIds.contains(link))
					if (validIds != null && (validIds.get(link) == null))
						continue ;

					newLinks.add(link) ;
				}

				if (newLinks.size() == 0)
					return null ;

				links.setValues(newLinks) ;

				return links ;
			}
			
			@Override
			public void loadFromCsvFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException  {

				if (exists() && !overwrite)
					return ;

				if (tracker == null) tracker = new ProgressTracker(1, WDatabase.class) ;
				tracker.startTask(dataFile.length(), "Loading " + getName()) ;

				BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8")) ;

				long bytesRead = 0 ;
				int lineNum = 0 ;

				Database db = getDatabase(false) ;

				String line ;
				while ((line=input.readLine()) != null) {
					bytesRead = bytesRead + line.length() + 1 ;
					lineNum++ ;

					CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8"))) ;

					WEntry<Integer,DbIntList> entry = deserialiseCsvRecord(cri) ;

					if (entry != null) {				
						DatabaseEntry k = new DatabaseEntry() ;
						keyBinding.objectToEntry(entry.getKey(), k) ;

						DatabaseEntry v = new DatabaseEntry() ;
						valueBinding.objectToEntry(entry.getValue(), v) ;

						db.put(null, k, v) ;
					}
					tracker.update(bytesRead) ;
				}
				input.close();

				env.cleanAndCheckpoint() ;
				getDatabase(true) ;
			}

		} ;
	}

	/**
	 * Returns a database appropriate for the given {@link DatabaseType}
	 * 
	 * @param type {@link DatabaseType#categoryParents}, {@link DatabaseType#articleParents}, {@link DatabaseType#childCategories},{@link DatabaseType#childArticles}, {@link DatabaseType#redirectSourcesByTarget}, {@link DatabaseType#sentenceSplits}
	 * @return see the description of the appropriate DatabaseType
	 */
	public WDatabase<Integer,DbIntList> buildIntIntListDatabase(final DatabaseType type) {

		switch (type) {
		case categoryParents:
		case articleParents:
		case childCategories:
		case childArticles:
		case redirectSourcesByTarget:
		case sentenceSplits:
			break ;
		default: 
			throw new IllegalArgumentException(type.name() + " is not a valid DatabaseType for IntIntListDatabase") ;
		}

		RecordBinding<DbIntList> keyBinding = new RecordBinding<DbIntList>() {
			public DbIntList createRecordInstance() {
				return new DbIntList() ;
			}
		} ;

		return new IntObjectDatabase<DbIntList>(
				env, 
				type, 
				keyBinding
		) {
			@Override
			public WEntry<Integer, DbIntList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {

				Integer k = record.readInt(null) ;

				DbIntList v = new DbIntList() ;
				v.deserialize(record) ;

				return new WEntry<Integer, DbIntList>(k,v) ;
			}

			@Override
			public DbIntList filterCacheEntry(WEntry<Integer,DbIntList> e, WikipediaConfiguration conf) {
				int key = e.getKey() ;
				ArrayList<Integer> values = e.getValue().getValues() ;

				//TIntHashSet validIds = conf.getArticlesOfInterest() ;
				ConcurrentMap validIds = conf.getArticlesOfInterest() ;
				
				ArrayList<Integer> newValues = null ;

				switch (type) {

				case articleParents :
				case sentenceSplits :
				case redirectSourcesByTarget :
					//only cache if key is valid article
					//if (validIds == null || validIds.contains(key))
					if (validIds == null || (validIds.get(key) != null) )
						newValues = values ;
					break ;
				case childArticles :
					//only cache values that are valid articles
					newValues = new ArrayList<Integer>() ;
					for (int v:values) {
						//if (validIds == null || validIds.contains(v))
						if (validIds == null || (validIds.get(v) != null) )
							newValues.add(v) ;
					}
				default :
					//cache everything
					newValues = values ;
				}

				if (newValues == null || newValues.size() == 0)
					return null ;

				return new DbIntList(newValues) ;
			}
		} ;
	}

	/**
	 * Returns a database associating integer id of redirect with the id of its target
	 * 
	 * @return a database associating integer id of redirect with the id of its target
	 */
	public WDatabase<Integer,Integer> buildRedirectTargetBySourceDatabase() {

		return new IntObjectDatabase<Integer>(
				env, 
				DatabaseType.redirectTargetBySource, 
				new IntegerBinding()
		) {

			@Override
			public WEntry<Integer, Integer> deserialiseCsvRecord(
					CsvRecordInput record) throws IOException {
				int k = record.readInt(null) ;
				int v = record.readInt(null) ;

				return new WEntry<Integer, Integer>(k,v) ;
			}

			@Override
			public Integer filterCacheEntry(
					WEntry<Integer, Integer> e, 
					WikipediaConfiguration conf
			) {
				//TIntHashSet validIds = conf.getArticlesOfInterest();
				ConcurrentMap validIds = conf.getArticlesOfInterest();

				//if (validIds != null && !validIds.contains(e.getValue()))
				if (validIds != null && (validIds.get(e.getValue()) == null))
					return null ; 

				return e.getValue();

			}
		} ;
	}

	/**
	 * Returns a database associating integer {@link WEnvironment.StatisticName#ordinal()} with the value relevant to this statistic.
	 * 
	 * @return a database associating integer {@link WEnvironment.StatisticName#ordinal()} with the value relevant to this statistic.
	 */
	public IntObjectDatabase<Long> buildStatisticsDatabase() {

		return new IntObjectDatabase<Long>(
				env, 
				DatabaseType.statistics, 
				new LongBinding()
		) {

			@Override
			public WEntry<Integer, Long> deserialiseCsvRecord(
					CsvRecordInput record) throws IOException {

				String statName = record.readString(null) ;
				Long v = record.readLong(null) ;

				Integer k = null;

				try {
					k = StatisticName.valueOf(statName).ordinal() ;
				} catch (Exception e) {
					Logger.getLogger(WDatabaseFactory.class).warn("Ignoring unknown statistic: " + statName) ;
					return null ;
				}
				return new WEntry<Integer, Long>(k,v) ;
			}

			@Override
			public Long filterCacheEntry(
					WEntry<Integer, Long> e, WikipediaConfiguration conf
			) {
				return e.getValue() ;
			}
		} ;
	}

	/**
	 * Returns a database associating integer id of page with DbTranslations (language links)
	 * 
	 * @return a database associating integer id of page with DbTranslations (language links)
	 */
	public WDatabase<Integer,DbTranslations> buildTranslationsDatabase() {

		return new IntObjectDatabase<DbTranslations>(
				env, 
				DatabaseType.translations, 
				new RecordBinding<DbTranslations>() {
					@Override
					public DbTranslations createRecordInstance() {
						return new DbTranslations() ;
					}
				}
		) {

			@Override
			public WEntry<Integer, DbTranslations> deserialiseCsvRecord(
					CsvRecordInput record) throws IOException {
				int k = record.readInt(null) ;

				DbTranslations v = new DbTranslations() ;
				v.deserialize(record) ;

				return new WEntry<Integer, DbTranslations>(k,v) ;
			}

			@Override
			public DbTranslations filterCacheEntry(
					WEntry<Integer, DbTranslations> e, WikipediaConfiguration conf
			) {
				//TIntHashSet validIds = conf.getArticlesOfInterest() ;
				ConcurrentMap validIds = conf.getArticlesOfInterest() ;
				
				//if (validIds != null && !validIds.contains(e.getKey()))
				if (validIds != null && (validIds.get(e.getKey()) == null) )
					return null ; 

				return e.getValue();

			}
		} ;
	}

	/**
	 * Returns a database associating integer ids with counts of how many pages it links to or that link to it
	 * 
	 * @return a database associating integer ids with counts of how many pages it links to or that link to it
	 */
	public PageLinkCountDatabase buildPageLinkCountDatabase() {

		return new PageLinkCountDatabase(env) ;
	}

	/**
	 * Returns a database associating integer id of page with its content, in mediawiki markup format
	 * 
	 * @return a database associating integer id of page with its content, in mediawiki markup format
	 */
	public WDatabase<Integer,String> buildMarkupDatabase() {
		return new MarkupDatabase(env) ;
	}
}



