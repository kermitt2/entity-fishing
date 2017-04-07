package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

import com.scienceminer.nerd.utilities.*;

import org.apache.hadoop.record.*;
import org.apache.log4j.Logger;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;

import org.wikipedia.miner.db.struct.*;

import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.kb.model.Page;

//import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A factory for creating KBDatabases of various types
 */
public class KBDatabaseFactory {
	
	private KBEnvironment env = null;

	/**
	 * Creates a new KBDatabaseFactory for the given KBEnvironment
	 * 
	 * @param env a KBEnvironment
	 */
	public KBDatabaseFactory(KBEnvironment env) {

		this.env = env;
	}

	/**
	 * Returns a database associating page ids with the title, type and generality of the page. 
	 * 
	 * @return a database associating page ids with the title, type and generality of the page. 
	 */
	public KBDatabase<Integer, DbPage> buildPageDatabase() {

		return new IntRecordDatabase<DbPage>(
				env, 
				DatabaseType.page
		) {
			@Override
			public KBEntry<Integer,DbPage> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				Integer id = record.readInt(null);

				DbPage p = new DbPage();
				p.deserialize(record);

				return new KBEntry<Integer,DbPage>(id, p);
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
					KBEntry<Integer, DbPage> e) {
				// we want to index only articles
				PageType pageType = PageType.values()[e.getValue().getType()];
				
				//if (validIds == null || validIds.contains(e.getKey()) || pageType == PageType.category || pageType==PageType.redirect) 
				if ( (pageType == PageType.article) || (pageType == PageType.category) || (pageType == PageType.redirect))
					return e.getValue();
				else
					return null;
			}

			/*@Override
			public DbPage filterCacheEntry(
					KBEntry<Integer, DbPage> e, 
					NerdConf conf
			) {
				PageType pageType = PageType.values()[e.getValue().getType()];
				//int conf.getMinLinksIn() = conf.getMinLinksIn();
				boolean valid = false;
				try {
					//KBEntry<Integer, DbLinkLocationList> e2 = 
					//	new KBEntry<Integer, DbLinkLocationList>(e.getKey(), 
					//											(DbLinkLocationList)Utilities.deserialize(e.getValue()));
					KBDatabase<Integer,DbLinkLocationList> dbLinkLocationList = env.getDbPageLinkIn();
					DbLinkLocationList list = (DbLinkLocationList)dbLinkLocationList.retrieve(e.getKey());
					//if (list == null)
					//	System.out.println("warning DbLinkLocationList null for id " + e.getKey());
					//if (list != null)
					//	System.out.println("DbLinkLocationList null for id " + e.getKey() + " has " + list.getLinkLocations().size() + " links in");
					if ((list != null) && (list.getLinkLocations().size() > conf.getMinLinksIn()))
						valid = true;
				} catch(Exception exp) {
					Logger.getLogger(KBDatabaseFactory.class).error("filterCacheEntry: Failed deserialize");
					exp.printStackTrace();
				}

				//if (validIds == null || validIds.contains(e.getKey()) || pageType == PageType.category || pageType==PageType.redirect) 
				//if (validIds == null || (validIds.get(e.getKey()) != null) || pageType == PageType.category || pageType==PageType.redirect) 
				if ((valid && (pageType == PageType.article)) || pageType == PageType.category || pageType == PageType.redirect) 	
					return e.getValue();
				else
					return null;
			}*/

			/*@Override
			public void caching(WikipediaConfiguration conf) {
				System.out.println("Checking cache for page db");
				// check if the cache is already present in the data files
				String cachePath = env.getConfiguration().getDatabaseDirectory() + "/" + type.toString() + "/cache.obj";

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
			            Logger.getLogger(KBDatabaseFactory.class).debug("Error when opening the cache for page db.");
			            //throw new NerdException(dbe);
			        } finally {
			            try {
			                if (in != null)
			                    in.close();
			            } catch(IOException e) {
			                Logger.getLogger(KBDatabaseFactory.class).debug("Error when closing the cache for page db.");
			                //throw new NerdException(e);
			            }
			        }
    			}
    			if (!isCached) {
					cache = new ConcurrentHashMap<Integer,DbPage>();
					System.out.println("Creating in-memory cache for page db");
					KBIterator iter = new KBIterator(this);//getIterator();
					try {
						while(iter.hasNext()) {
							Entry entry = iter.next();
							byte[] keyData = entry.getKey();
							byte[] valueData = entry.getValue();
							//Page p = null;
							try {
								Integer keyId = new BigInteger(keyData).intValue();
								DbPage pa = (DbPage)Utilities.deserialize(valueData);

								KBEntry<Integer,DbPage> KBEntry = new KBEntry<Integer,DbPage>(keyId, pa);

								DbPage filteredValue = filterCacheEntry(KBEntry, conf);
								if (filteredValue != null) {
									cache.put(keyId, filteredValue);
								}
							} catch(Exception exp) {
								Logger.getLogger(KBDatabaseFactory.class).error("Failed caching deserialize");
								exp.printStackTrace();
							}
						}
					} catch(Exception exp) {
						Logger.getLogger(KBDatabaseFactory.class).error("Page iterator failure");
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
					            Logger.getLogger(KBDatabaseFactory.class).debug("Error when saving the domain map.");
					            e.printStackTrace();
					            //throw new NerdException(e);
					        } finally {
					            try {
					                if (out != null)
					                    out.close();
					            } catch(IOException e) {
					                Logger.getLogger(KBDatabaseFactory.class).debug("Error when closing the domain map.");
					                e.printStackTrace();
					                //throw new NerdException(e);
					            }
					        }
					    }
					}
					isCached = true;
				}
				System.out.println("Cache for page db ready");
			}*/
		};
	}

	/**
	 * Returns a database associating article, category or template titles with their ids.
	 * 
	 * @param {@link DatabaseType#articlesByTitle}, {@link DatabaseType#templatesByTitle} or {@link DatabaseType#categoriesByTitle}.
	 * @return a database associating article, category or template titles with their ids.
	 */
	public KBDatabase<String,Integer> buildTitleDatabase(DatabaseType type) {
		return new StringIntDatabase(
				env, 
				type
		) {
			@Override
			public KBEntry<String,Integer> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				Integer id = record.readInt(null);

				DbPage p = new DbPage();
				p.deserialize(record);

				PageType pageType = PageType.values()[p.getType()];
				DatabaseType dbType = getType();

				if (dbType == DatabaseType.articlesByTitle && (pageType != PageType.article && pageType != PageType.disambiguation && pageType != PageType.redirect))
					return null;

				if (dbType == DatabaseType.categoriesByTitle && pageType != PageType.category)
					return null;

				if (dbType == DatabaseType.templatesByTitle && pageType != PageType.template)
					return null;

				return new KBEntry<String,Integer>(p.getTitle(), id);
			}
		};
	}

	/**
	 * Returns a database associating String labels with the statistics about the articles (senses) these labels could refer to.
	 * 
	 * @return a database associating String labels with the statistics about the articles (senses) these labels could refer to. 
	 */
	public LabelDatabase buildLabelDatabase() {
		return new LabelDatabase(env);
	}

	/**
	 * Returns a database associating Integer page ids with the labels used to refer to that page
	 * 
	 * @return a database associating Integer page ids with the labels used to refer to that page
	 */
	public KBDatabase<Integer,DbLabelForPageList> buildPageLabelDatabase() {

		return new IntRecordDatabase<DbLabelForPageList>(
				env, 
				DatabaseType.pageLabel
		) {

			@Override
			public KBEntry<Integer,DbLabelForPageList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {

				Integer id = record.readInt(null);

				DbLabelForPageList labels = new DbLabelForPageList();
				labels.deserialize(record);

				return new KBEntry<Integer,DbLabelForPageList>(id, labels);
			}
		};
	}

	/**
	 * Returns a database associating Integer ids with the ids of articles it links to or that link to it, and the sentence indexes where these links are found.
	 * 
	 * @param type either {@link DatabaseType#pageLinksIn} or {@link DatabaseType#pageLinksOut}.
	 * @return a database associating Integer ids with the ids of articles it links to or that link to it, and the sentence indexes where these links are found
	 */
	public KBDatabase<Integer, DbLinkLocationList> buildPageLinkDatabase(DatabaseType type) {

		if (type != DatabaseType.pageLinksIn && type != DatabaseType.pageLinksOut)
			throw new IllegalArgumentException("type must be either DatabaseType.pageLinksIn or DatabaseType.pageLinksOut");

		return new IntRecordDatabase<DbLinkLocationList>(
				env, 
				type
		) {
			@Override
			public KBEntry<Integer, DbLinkLocationList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {

				Integer id = record.readInt(null);

				DbLinkLocationList l = new DbLinkLocationList();
				l.deserialize(record);

				return new KBEntry<Integer, DbLinkLocationList>(id, l);
			}
		};
	}

	
	/**
	 * Returns a database associating Integer ids with the ids of articles it links to or that link to it.
	 * 
	 * @param type either {@link DatabaseType#pageLinksIn} or {@link DatabaseType#pageLinksOut}.
	 * @return a database associating Integer ids with the ids of articles it links to or that link to it.
	 */
	public KBDatabase<Integer, DbIntList> buildPageLinkNoSentencesDatabase(DatabaseType type) {

		if (type != DatabaseType.pageLinksInNoSentences && type != DatabaseType.pageLinksOutNoSentences)
			throw new IllegalArgumentException("type must be either DatabaseType.pageLinksInNoSentences or DatabaseType.pageLinksOutNoSentences");

		return new IntRecordDatabase<DbIntList>(
				env, 
				type
		) {

			@Override
			public KBEntry<Integer, DbIntList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				// this has to read from pagelinks file (with sentences
				
				Integer id = record.readInt(null);

				DbLinkLocationList l = new DbLinkLocationList();
				l.deserialize(record);
				
				ArrayList<Integer> linkIds = new ArrayList<Integer>();
				
				for (DbLinkLocation ll:l.getLinkLocations()) 
					linkIds.add(ll.getLinkId());
				
				return new KBEntry<Integer, DbIntList>(id, new DbIntList(linkIds));
			}
			
			@Override
			public void loadFromCsvFile(File dataFile, boolean overwrite) throws IOException  {
				if (isLoaded && !overwrite)
					return;
				System.out.println("Loading " + getName());

				BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));

				long bytesRead = 0;

				//Database db = getDatabase(false);

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
					bytesRead = bytesRead + line.length() + 1;
					CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8")));
					KBEntry<Integer,DbIntList> entry = deserialiseCsvRecord(cri);
					try {
						db.put(tx, BigInteger.valueOf(entry.getKey()).toByteArray(), Utilities.serialize(entry.getValue()));
						nbToAdd++;
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				tx.commit();
				tx.close();
				input.close();
			}

		};
	}

	/**
	 * Returns a database appropriate for the given {@link DatabaseType}
	 * 
	 * @param type {@link DatabaseType#categoryParents}, {@link DatabaseType#articleParents}, {@link DatabaseType#childCategories},{@link DatabaseType#childArticles}, {@link DatabaseType#redirectSourcesByTarget}, {@link DatabaseType#sentenceSplits}
	 * @return see the description of the appropriate DatabaseType
	 */
	public KBDatabase<Integer,DbIntList> buildIntIntListDatabase(final DatabaseType type) {

		switch (type) {
		case categoryParents:
		case articleParents:
		case childCategories:
		case childArticles:
		case redirectSourcesByTarget:
		case sentenceSplits:
			break;
		default: 
			throw new IllegalArgumentException(type.name() + " is not a valid DatabaseType for IntIntListDatabase");
		}

		return new IntRecordDatabase<DbIntList>(
				env, 
				type
		) {
			@Override
			public KBEntry<Integer, DbIntList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {

				Integer k = record.readInt(null);

				DbIntList v = new DbIntList();
				v.deserialize(record);

				return new KBEntry<Integer, DbIntList>(k,v);
			}
		};
	}

	/**
	 * Returns a database associating integer id of redirect with the id of its target
	 * 
	 * @return a database associating integer id of redirect with the id of its target
	 */
	public KBDatabase<Integer,Integer> buildRedirectTargetBySourceDatabase() {

		return new IntIntDatabase(
				env, 
				DatabaseType.redirectTargetBySource
		) {

			@Override
			public KBEntry<Integer, Integer> deserialiseCsvRecord(
					CsvRecordInput record) throws IOException {
				int k = record.readInt(null);
				int v = record.readInt(null);

				return new KBEntry<Integer, Integer>(k,v);
			}
		};
	}

	/**
	 * Returns a database associating integer {@link KBEnvironment.StatisticName#ordinal()} with the value relevant to this statistic.
	 * 
	 * @return a database associating integer {@link KBEnvironment.StatisticName#ordinal()} with the value relevant to this statistic.
	 */
	public IntLongDatabase buildStatisticsDatabase() {

		return new IntLongDatabase(
				env, 
				DatabaseType.statistics
		) {
			@Override
			public KBEntry<Integer, Long> deserialiseCsvRecord(
					CsvRecordInput record) throws IOException {

				String statName = record.readString(null);
				Long v = record.readLong(null);

				Integer k = null;

				try {
					k = StatisticName.valueOf(statName).ordinal();
				} catch (Exception e) {
					Logger.getLogger(KBDatabaseFactory.class).warn("Ignoring unknown statistic: " + statName);
					return null;
				}
				return new KBEntry<Integer, Long>(k,v);
			}
		};
	}

	/**
	 * Returns a database associating integer id of page with DbTranslations (language links)
	 * 
	 * @return a database associating integer id of page with DbTranslations (language links)
	 */
	public KBDatabase<Integer,DbTranslations> buildTranslationsDatabase() {

		return new IntRecordDatabase<DbTranslations>(
				env, 
				DatabaseType.translations
		) {
			@Override
			public KBEntry<Integer, DbTranslations> deserialiseCsvRecord(
					CsvRecordInput record) throws IOException {
				int k = record.readInt(null);

				DbTranslations v = new DbTranslations();
				v.deserialize(record);

				return new KBEntry<Integer, DbTranslations>(k,v);
			}
		};
	}

	/**
	 * Returns a database associating integer ids with counts of how many pages it links to or that link to it
	 * 
	 * @return a database associating integer ids with counts of how many pages it links to or that link to it
	 */
	public PageLinkCountDatabase buildPageLinkCountDatabase() {
		return new PageLinkCountDatabase(env);
	}

	/**
	 * Returns a database associating integer id of page with its content, in mediawiki markup format
	 * 
	 * @return a database associating integer id of page with its content, in mediawiki markup format
	 */
	public KBDatabase<Integer,String> buildMarkupDatabase() {
		return new MarkupDatabase(env);
	}
}



