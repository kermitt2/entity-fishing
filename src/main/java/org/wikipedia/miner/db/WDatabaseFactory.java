package org.wikipedia.miner.db;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

import com.scienceminer.nerd.utilities.*;

import org.apache.hadoop.record.*;
import org.apache.log4j.Logger;

import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.db.WEnvironment.StatisticName;
import org.wikipedia.miner.db.struct.*;
import org.wikipedia.miner.model.Page.PageType;
import org.wikipedia.miner.model.Page;
import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;


/**
 * A factory for creating WDatabases of various types
 */
public class WDatabaseFactory {
	
	WEnvironment env;

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

		/*RecordBinding<DbPage> keyBinding = new RecordBinding<DbPage>() {
			public DbPage createRecordInstance() {
				return new DbPage() ;
			}
		};*/

		return new IntRecordDatabase<DbPage>(
				env, 
				DatabaseType.page
		) {
			@Override
			public WEntry<Integer,DbPage> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				Integer id = record.readInt(null) ;

				DbPage p = new DbPage() ;
				p.deserialize(record) ;

				return new WEntry<Integer,DbPage>(id, p) ;
			}

			// using LMDB zero copy mode
			@Override
			public DbPage retrieve(Integer key) {
				if (isCached)
					return cache.get(key);

				byte[] cachedData = null;
				DbPage record = null;
				try (Transaction tx = environment.createReadTransaction();
					 BufferCursor cursor = db.bufferCursor(tx)) {
					cursor.keyWriteBytes(BigInteger.valueOf(key).toByteArray());
					if (cursor.seekKey()) {
						record = (DbPage)Utilities.deserialize(cursor.valBytes());
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				return record;
			}

			@Override
			public DbPage filterEntry(
					WEntry<Integer, DbPage> e) {
				// we want to index only articles
				PageType pageType = PageType.values()[e.getValue().getType()] ;
				
				//if (validIds == null || validIds.contains(e.getKey()) || pageType == PageType.category || pageType==PageType.redirect) 
				if ( (pageType == PageType.article) || (pageType == PageType.category) || (pageType == PageType.redirect))
					return e.getValue() ;
				else
					return null ;
			}

			@Override
			public DbPage filterCacheEntry(
					WEntry<Integer, DbPage> e, 
					WikipediaConfiguration conf
			) {
				PageType pageType = PageType.values()[e.getValue().getType()] ;
				//int conf.getMinLinksIn() = conf.getMinLinksIn();
				boolean valid = false;
				try {
					//WEntry<Integer, DbLinkLocationList> e2 = 
					//	new WEntry<Integer, DbLinkLocationList>(e.getKey(), 
					//											(DbLinkLocationList)Utilities.deserialize(e.getValue()));
					WDatabase<Integer,DbLinkLocationList> dbLinkLocationList = env.getDbPageLinkIn();
					DbLinkLocationList list = (DbLinkLocationList)dbLinkLocationList.retrieve(e.getKey());
					/*if (list == null)
						System.out.println("warning DbLinkLocationList null for id " + e.getKey());
					if (list != null)
						System.out.println("DbLinkLocationList null for id " + e.getKey() + " has " + list.getLinkLocations().size() + " links in");*/
					if ((list != null) && (list.getLinkLocations().size() > conf.getMinLinksIn()))
						valid = true;
				} catch(Exception exp) {
					Logger.getLogger(WDatabaseFactory.class).error("filterCacheEntry: Failed deserialize");
					exp.printStackTrace();
				}

				//if (validIds == null || validIds.contains(e.getKey()) || pageType == PageType.category || pageType==PageType.redirect) 
				//if (validIds == null || (validIds.get(e.getKey()) != null) || pageType == PageType.category || pageType==PageType.redirect) 
				if ((valid && (pageType == PageType.article)) || pageType == PageType.category || pageType == PageType.redirect) 	
					return e.getValue() ;
				else
					return null ;
			}

			@Override
			public void caching(WikipediaConfiguration conf) {
				System.out.println("Checking cache for page db");
				// check if the cache is already present in the data files
				String cachePath = env.getConfiguration().getDatabaseDirectory() + "/" + type.toString() + "/cache.obj" ;

		    	File theCacheFile = new File(cachePath);
    			if (theCacheFile.exists()) {
    				ObjectInputStream in = null;
    				try {
	    				FileInputStream fileIn = new FileInputStream(theCacheFile);
		                in = new ObjectInputStream(fileIn);
    		            cache = (ConcurrentMap<Integer, DbPage>)in.readObject();
    					isCached = true;
    				} catch (Exception dbe) {
    					isCached = false;
			            Logger.getLogger(WDatabaseFactory.class).debug("Error when opening the cache for page db.");
			            //throw new NerdException(dbe);
			        } finally {
			            try {
			                if (in != null)
			                    in.close();
			            } catch(IOException e) {
			                Logger.getLogger(WDatabaseFactory.class).debug("Error when closing the cache for page db.");
			                //throw new NerdException(e);
			            }
			        }
    			}
    			if (!isCached) {
					cache = new ConcurrentHashMap<Integer,DbPage>();
					System.out.println("Creating in-memory cache for page db");
					WIterator iter = new WIterator(this);//getIterator();
					try {
						while(iter.hasNext()) {
							Entry entry = iter.next();
							byte[] keyData = entry.getKey();
							byte[] valueData = entry.getValue();
							//Page p = null;
							try {
								Integer keyId = new BigInteger(keyData).intValue();
								DbPage pa = (DbPage)Utilities.deserialize(valueData);

								WEntry<Integer,DbPage> wEntry = new WEntry<Integer,DbPage>(keyId, pa);

								DbPage filteredValue = filterCacheEntry(wEntry, conf);
								if (filteredValue != null) {
									cache.put(keyId, filteredValue);
								}
							} catch(Exception exp) {
								Logger.getLogger(WDatabaseFactory.class).error("Failed caching deserialize");
								exp.printStackTrace();
							}
						}
					} catch(Exception exp) {
						Logger.getLogger(WDatabaseFactory.class).error("Page iterator failure");
						exp.printStackTrace();
						isCached = false;
					} finally {
						System.out.println("Closing iterator for cache page db");
						iter.close();
						// save cache
						if (cache != null) {
							ObjectOutputStream out = null;
							try {
					            if (theCacheFile != null) {
					                FileOutputStream fileOut = new FileOutputStream(theCacheFile);
					                out = new ObjectOutputStream(fileOut);
					                out.writeObject(cache);
					            }
					        } catch(IOException e) {
					            Logger.getLogger(WDatabaseFactory.class).debug("Error when saving the domain map.");
					            e.printStackTrace();
					            //throw new NerdException(e);
					        } finally {
					            try {
					                if (out != null)
					                    out.close();
					            } catch(IOException e) {
					                Logger.getLogger(WDatabaseFactory.class).debug("Error when closing the domain map.");
					                e.printStackTrace();
					                //throw new NerdException(e);
					            }
					        }
					    }
					}
					isCached = true;
				}
				System.out.println("Cache for page db ready");
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
		return new StringIntDatabase(
				env, 
				type
		) {
			@Override
			public WEntry<String,Integer> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				/*String title = record.readString(null) ;

				Integer id = record.readInt(null) ;

				return new WEntry<String,Integer>(title, id) ;*/


				Integer id = record.readInt(null);

				DbPage p = new DbPage();
				p.deserialize(record);

				PageType pageType = PageType.values()[p.getType()];
				DatabaseType dbType = getType() ;

				if (dbType == DatabaseType.articlesByTitle && (pageType != PageType.article && pageType != PageType.disambiguation && pageType != PageType.redirect))
					return null ;

				if (dbType == DatabaseType.categoriesByTitle && pageType != PageType.category)
					return null ;

				if (dbType == DatabaseType.templatesByTitle && pageType != PageType.template)
					return null ;

				return new WEntry<String,Integer>(p.getTitle(), id) ;
			}

			/*@Override
			public Integer filterCacheEntry(
					WEntry<String, Integer> e, 
					WikipediaConfiguration conf
			) {
				ConcurrentMap validIds = conf.getArticlesOfInterest();

				if (getType() == DatabaseType.articlesByTitle) {
					//if (validIds != null && !validIds.contains(e.getValue()))
					if (validIds != null && (validIds.get(e.getValue()) == null) )
						return null ;
				}

				return e.getValue();
			}*/
		};
		//return new TitleDatabase(env, type) ;
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

		/*RecordBinding<DbLabelForPageList> keyBinding = new RecordBinding<DbLabelForPageList>() {
			public DbLabelForPageList createRecordInstance() {
				return new DbLabelForPageList() ;
			}
		} ;*/

		return new IntRecordDatabase<DbLabelForPageList>(
				env, 
				DatabaseType.pageLabel
		) {

			@Override
			public WEntry<Integer,DbLabelForPageList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {

				Integer id = record.readInt(null) ;

				DbLabelForPageList labels = new DbLabelForPageList() ;
				labels.deserialize(record) ;

				return new WEntry<Integer,DbLabelForPageList>(id, labels) ;
			}

			/*@Override
			public DbLabelForPageList filterCacheEntry(WEntry<Integer,DbLabelForPageList> e, WikipediaConfiguration conf) {

				//TIntHashSet validIds = conf.getArticlesOfInterest() ;
				ConcurrentMap validIds = conf.getArticlesOfInterest() ;
				
				//if (validIds != null && !validIds.contains(e.getKey()))
				if (validIds != null && (validIds.get(e.getKey()) == null) )	
					return null ;

				return e.getValue();
			}*/
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

		/*RecordBinding<DbLinkLocationList> keyBinding = new RecordBinding<DbLinkLocationList>() {
			public DbLinkLocationList createRecordInstance() {
				return new DbLinkLocationList() ;
			}
		};*/

		return new IntRecordDatabase<DbLinkLocationList>(
				env, 
				type
		) {
			@Override
			public WEntry<Integer, DbLinkLocationList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {

				Integer id = record.readInt(null) ;

				DbLinkLocationList l = new DbLinkLocationList() ;
				l.deserialize(record) ;

				return new WEntry<Integer, DbLinkLocationList>(id, l) ;
			}

			/*@Override
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

				for (DbLinkLocation ll : links.getLinkLocations()) {
					//if (validIds != null && !validIds.contains(ll.getLinkId()))
					if (validIds != null && (validIds.get(ll.getLinkId()) == null) )	
						continue ;

					newLinks.add(ll) ;
				}

				if (newLinks.size() == 0)
					return null ;

				links.setLinkLocations(newLinks) ;

				return links ;
			}*/

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

		/*RecordBinding<DbIntList> keyBinding = new RecordBinding<DbIntList>() {
			public DbIntList createRecordInstance() {
				return new DbIntList() ;
			}
		};*/

		return new IntRecordDatabase<DbIntList>(
				env, 
				type
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

			/*@Override
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
			}*/
			
			@Override
			public void loadFromCsvFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException  {
				if (isLoaded && !overwrite)
					return ;

				if (tracker == null) tracker = new ProgressTracker(1, WDatabase.class) ;
				tracker.startTask(dataFile.length(), "Loading " + getName()) ;

				BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8")) ;

				long bytesRead = 0 ;

				//Database db = getDatabase(false) ;

				String line = null;
				int nbToAdd = 0;
				Transaction tx = environment.createWriteTransaction();
				while ((line=input.readLine()) != null) {
					if (nbToAdd == 10000) {
						tx.commit();
						tx.close();
						nbToAdd = 0;
						tx = environment.createWriteTransaction();
					}
					bytesRead = bytesRead + line.length() + 1 ;

					CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8"))) ;

					WEntry<Integer,DbIntList> entry = deserialiseCsvRecord(cri) ;

					try {
						db.put(tx, BigInteger.valueOf(entry.getKey()).toByteArray(), Utilities.serialize(entry.getValue()));
						nbToAdd++;
					} catch(Exception e) {
						e.printStackTrace();
					}

					/*if (entry != null) {				
						DatabaseEntry k = new DatabaseEntry() ;
						keyBinding.objectToEntry(entry.getKey(), k) ;

						DatabaseEntry v = new DatabaseEntry() ;
						valueBinding.objectToEntry(entry.getValue(), v) ;

						db.put(null, k, v) ;

					}*/
					tracker.update(bytesRead) ;
				}
				tx.commit();
				tx.close();
				input.close();

				//env.cleanAndCheckpoint() ;
				//getDatabase(true) ;
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

		/*RecordBinding<DbIntList> keyBinding = new RecordBinding<DbIntList>() {
			public DbIntList createRecordInstance() {
				return new DbIntList() ;
			}
		};*/

		return new IntRecordDatabase<DbIntList>(
				env, 
				type
		) {
			@Override
			public WEntry<Integer, DbIntList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {

				Integer k = record.readInt(null) ;

				DbIntList v = new DbIntList() ;
				v.deserialize(record) ;

				return new WEntry<Integer, DbIntList>(k,v) ;
			}

			/*@Override
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
			}*/
		};
	}

	/**
	 * Returns a database associating integer id of redirect with the id of its target
	 * 
	 * @return a database associating integer id of redirect with the id of its target
	 */
	public WDatabase<Integer,Integer> buildRedirectTargetBySourceDatabase() {

		return new IntIntDatabase(
				env, 
				DatabaseType.redirectTargetBySource
		) {

			@Override
			public WEntry<Integer, Integer> deserialiseCsvRecord(
					CsvRecordInput record) throws IOException {
				int k = record.readInt(null) ;
				int v = record.readInt(null) ;

				return new WEntry<Integer, Integer>(k,v) ;
			}

			/*@Override
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

			}*/
		};
	}

	/**
	 * Returns a database associating integer {@link WEnvironment.StatisticName#ordinal()} with the value relevant to this statistic.
	 * 
	 * @return a database associating integer {@link WEnvironment.StatisticName#ordinal()} with the value relevant to this statistic.
	 */
	public IntLongDatabase buildStatisticsDatabase() {

		return new IntLongDatabase(
				env, 
				DatabaseType.statistics
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

			/*@Override
			public Long filterCacheEntry(
					WEntry<Integer, Long> e, WikipediaConfiguration conf
			) {
				return e.getValue() ;
			}*/
		};
	}

	/**
	 * Returns a database associating integer id of page with DbTranslations (language links)
	 * 
	 * @return a database associating integer id of page with DbTranslations (language links)
	 */
	public WDatabase<Integer,DbTranslations> buildTranslationsDatabase() {

		return new IntRecordDatabase<DbTranslations>(
				env, 
				DatabaseType.translations/*, 
				new RecordBinding<DbTranslations>() {
					@Override
					public DbTranslations createRecordInstance() {
						return new DbTranslations() ;
					}
				}*/
		) {
			@Override
			public WEntry<Integer, DbTranslations> deserialiseCsvRecord(
					CsvRecordInput record) throws IOException {
				int k = record.readInt(null) ;

				DbTranslations v = new DbTranslations() ;
				v.deserialize(record) ;

				return new WEntry<Integer, DbTranslations>(k,v) ;
			}

			/*@Override
			public DbTranslations filterCacheEntry(
					WEntry<Integer, DbTranslations> e, WikipediaConfiguration conf
			) {
				//TIntHashSet validIds = conf.getArticlesOfInterest() ;
				ConcurrentMap validIds = conf.getArticlesOfInterest() ;
				
				//if (validIds != null && !validIds.contains(e.getKey()))
				if (validIds != null && (validIds.get(e.getKey()) == null) )
					return null ; 

				return e.getValue();

			}*/
		};
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



