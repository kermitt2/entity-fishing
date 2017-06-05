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

import com.scienceminer.nerd.kb.Relation;
import com.scienceminer.nerd.kb.Property;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.kb.model.Page;

import org.wikipedia.miner.db.struct.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A factory for creating the LMDB databases used in (N)ERD 
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
	 * Create a database associating page ids with the title, type and generality of the page
	 */
	public KBDatabase<Integer, DbPage> buildPageDatabase() {
		return new IntRecordDatabase<DbPage>(env, DatabaseType.page) {
			@Override
			public KBEntry<Integer,DbPage> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				Integer id = record.readInt(null);
				DbPage p = new DbPage();
				p.deserialize(record);

				return new KBEntry<Integer,DbPage>(id, p);
			}

			// using LMDB zero copy mode
			//@Override
			public DbPage retrieve2(Integer key) {
				DbPage record = null;
				try (Transaction tx = environment.createReadTransaction();
					BufferCursor cursor = db.bufferCursor(tx)) {
					//cursor.keyWriteBytes(BigInteger.valueOf(key).toByteArray());
					cursor.keyWriteBytes(KBEnvironment.serialize(key));
					if (cursor.seekKey()) {
						record = (DbPage)KBEnvironment.deserialize(cursor.valBytes());
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				return record;
			}

			// using standard LMDB copy mode
			@Override
			public DbPage retrieve(Integer key) {
				byte[] cachedData = null;
				DbPage record = null;
				try (Transaction tx = environment.createReadTransaction()) {
					//cachedData = db.get(tx, BigInteger.valueOf(key).toByteArray());
					cachedData = db.get(tx, KBEnvironment.serialize(key));
					if (cachedData != null)
						record = (DbPage)KBEnvironment.deserialize(cachedData);
				} catch(Exception e) {
					e.printStackTrace();
				}
				return record;
			}

			@Override
			public DbPage filterEntry(KBEntry<Integer, DbPage> e) {
				// we want to index only articles
				PageType pageType = PageType.values()[e.getValue().getType()];
				
				//if (validIds == null || validIds.contains(e.getKey()) || pageType == PageType.category || pageType==PageType.redirect) 
				if ( (pageType == PageType.article) || (pageType == PageType.category) || (pageType == PageType.redirect))
					return e.getValue();
				else
					return null;
			}
		};
	}

	/**
	 * Create a database associating article, category or template titles with their page ids
	 */
	public KBDatabase<String,Integer> buildTitleDatabase(DatabaseType type) {
		return new StringIntDatabase(env, type) {
			@Override
			public KBEntry<String,Integer> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				Integer id = record.readInt(null);

				DbPage p = new DbPage();
				p.deserialize(record);

				PageType pageType = PageType.values()[p.getType()];
				DatabaseType dbType = getType();

				if ((dbType == DatabaseType.articlesByTitle) && 
					(pageType != PageType.article && pageType != PageType.disambiguation && pageType != PageType.redirect) )
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
	 * Create a database associating labels with the statistics about the articles these labels could realize
	 */
	public LabelDatabase buildLabelDatabase() {
		return new LabelDatabase(env);
	}

	/**
	 * Create a database associating ids with the ids of articles it links to or that link to it
	 */
	public KBDatabase<Integer, DbIntList> buildPageLinkNoSentencesDatabase(DatabaseType type) {
		if (type != DatabaseType.pageLinksInNoSentences && type != DatabaseType.pageLinksOutNoSentences)
			throw new IllegalArgumentException("type must be either DatabaseType.pageLinksInNoSentences or DatabaseType.pageLinksOutNoSentences");

		return new IntRecordDatabase<DbIntList>(env, type) {
			@Override
			public KBEntry<Integer, DbIntList> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				// this has to read from pagelinks file (with sentences)
				Integer id = record.readInt(null);

				DbLinkLocationList l = new DbLinkLocationList();
				l.deserialize(record);
				
				ArrayList<Integer> linkIds = new ArrayList<Integer>();
				
				for (DbLinkLocation ll : l.getLinkLocations()) {
					if (!linkIds.contains(ll.getLinkId()))
						linkIds.add(ll.getLinkId());
				}
				
				return new KBEntry<Integer, DbIntList>(id, new DbIntList(linkIds));
			}
			
			@Override
			public void loadFromFile(File dataFile, boolean overwrite) throws IOException  {
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
						//db.put(tx, BigInteger.valueOf(entry.getKey()).toByteArray(), KBEnvironment.serialize(entry.getValue()));
						db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
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
	 * Create a database appropriate for the given {@link DatabaseType}
	 * 
	 * @param type {@link DatabaseType#categoryParents}, {@link DatabaseType#articleParents}, {@link DatabaseType#childCategories},{@link DatabaseType#childArticles}, {@link DatabaseType#redirectSourcesByTarget}, {@link DatabaseType#sentenceSplits}
	 */
	public KBDatabase<Integer,DbIntList> buildIntIntListDatabase(final DatabaseType type) {
		switch (type) {
			case categoryParents:
			case articleParents:
			case childCategories:
			case childArticles:
			case redirectSourcesByTarget:
//			case sentenceSplits:
				break;
			default: 
				throw new IllegalArgumentException(type.name() + " is not a valid DatabaseType for IntIntListDatabase");
			}

		return new IntRecordDatabase<DbIntList>(env, type) {
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
	 * Create a database associating id of redirect with the id of its target
	 */
	public KBDatabase<Integer,Integer> buildRedirectTargetBySourceDatabase() {

		return new IntIntDatabase(env, DatabaseType.redirectTargetBySource) {
			@Override
			public KBEntry<Integer, Integer> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				int k = record.readInt(null);
				int v = record.readInt(null);

				return new KBEntry<Integer, Integer>(k,v);
			}
		};
	}

	/**
	 * Create a database storing KB statistics
	 */
	public IntLongDatabase buildStatisticsDatabase() {

		return new IntLongDatabase(env, DatabaseType.statistics) {
			@Override
			public KBEntry<Integer, Long> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
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
	 * Create a database associating the id of a page to its other wikipedia language mapping
	 */
	public KBDatabase<Integer,DbTranslations> buildTranslationsDatabase() {
		return new IntRecordDatabase<DbTranslations>(env, DatabaseType.translations) {
			@Override
			public KBEntry<Integer, DbTranslations> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				int k = record.readInt(null);
				DbTranslations v = new DbTranslations();
				v.deserialize(record);

				return new KBEntry<Integer, DbTranslations>(k,v);
			}
		};
	}

	/**
	 * Create a database associating ids with counts of how many pages it links to or that link to it
	 */
	public PageLinkCountDatabase buildPageLinkCountDatabase() {
		return new PageLinkCountDatabase(env);
	}

	/**
	 * Create a database associating id of page with its first paragraph/definition, in mediawiki markup format
	 */
	public KBDatabase<Integer,String> buildMarkupDatabase() {
		return new MarkupDatabase(env, DatabaseType.markup);
	}

	/**
	 * Create a database associating id of page with its full content/article, in original mediawiki markup format
	 */
	public KBDatabase<Integer,String> buildMarkupFullDatabase() {
		return new MarkupDatabase(env, DatabaseType.markupFull);
	}

	public KBDatabase<Integer,String> buildDbConceptByPageIdDatabase() {
		return new KBDatabase<Integer,String>(env, DatabaseType.conceptByPageId) {
			protected void add(KBEntry<Integer,String> entry) {
				try (Transaction tx = environment.createWriteTransaction()) {
					//db.put(tx, BigInteger.valueOf(entry.getKey()).toByteArray(), BigInteger.valueOf(entry.getValue()).toByteArray());
					db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
					tx.commit();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			// using standard LMDB copy mode
			@Override
			public String retrieve(Integer key) {
				byte[] cachedData = null;
				String record = null;
				try (Transaction tx = environment.createReadTransaction()) {
					//cachedData = db.get(tx, BigInteger.valueOf(key).toByteArray());
					cachedData = db.get(tx, KBEnvironment.serialize(key));
					if (cachedData != null) {
						//record = new BigInteger(cachedData).longValue();
						record = (String)KBEnvironment.deserialize(cachedData);
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				return record;
			}

			/**
			 * Builds the persistent database from a file.
			 * 
			 * @param dataFile the file (here a text file with fields separated by a tabulation) containing data to be loaded
			 * @param overwrite true if the existing database should be overwritten, otherwise false
			 * @throws IOException if there is a problem reading or deserialising the given data file.
			 */
			public void loadFromFile(File dataFile, boolean overwrite) throws IOException  {
System.out.println("input file: " + dataFile.getPath());
System.out.println("isLoaded: " + isLoaded);
				if (isLoaded && !overwrite)
					return;
				System.out.println("Loading " + name + " database");

				BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
				//long bytesRead = 0;

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
					//bytesRead = bytesRead + line.length() + 1;

					//CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8")));
					//KBEntry<Integer,Long> entry = deserialiseCsvRecord(cri);
					String[] pieces = line.split("\t");
					if (pieces.length != 2)
						continue;
					Integer keyVal = null;
					try {
						keyVal = Integer.parseInt(pieces[0]);
					} catch(Exception e) {
						e.printStackTrace();
					}
					if (keyVal == null)
						continue;
					KBEntry<Integer,String> entry = new KBEntry<Integer,String>(keyVal, pieces[1]);
					if (entry != null) {
						try {
							//db.put(tx, BigInteger.valueOf(entry.getKey()).toByteArray(), BigInteger.valueOf(entry.getValue()).toByteArray());
							db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
							nbToAdd++;
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
				tx.commit();
				tx.close();
				input.close();
				isLoaded = true;
			}

			@Override
			public KBEntry<Integer,String> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
				throw new UnsupportedOperationException();
			}
		};
	}
}
