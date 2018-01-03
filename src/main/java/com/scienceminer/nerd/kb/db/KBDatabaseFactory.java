package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

import org.apache.hadoop.record.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.kb.Property;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.kb.model.hadoop.*;
import com.scienceminer.nerd.exceptions.NerdResourceException;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A factory for creating the LMDB databases used in (N)ERD Knowlegde Base for the 
 * lower environment (e.g. the language-dependent part of the KB).
 */
public class KBDatabaseFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(KBDatabaseFactory.class);	
	
	private KBEnvironment env = null;

	public KBDatabaseFactory(KBEnvironment env) {
		this.env = env;
	}

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
					cachedData = db.get(tx, KBEnvironment.serialize(key));
					if (cachedData != null)
						record = (DbPage)KBEnvironment.deserialize(cachedData);
				} catch(Exception e) {
					e.printStackTrace();
				}
				return record;
			}

			public DbPage filterEntry(KBEntry<Integer, DbPage> e) {
				// we want to index only articles
				PageType pageType = PageType.values()[e.getValue().getType()];
				if ( (pageType == PageType.article) || (pageType == PageType.category) || (pageType == PageType.redirect) ) {
					//|| (pageType == PageType.disambiguation))
					return e.getValue();
				}
				else
					return null;
			}

			public void loadFromFile(File dataFile, boolean overwrite) throws Exception  {
				if (isLoaded && !overwrite)
					return;
				System.out.println("Loading " + name + " database");

				if (dataFile == null)
					throw new NerdResourceException("Markup file not found");

				BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
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
					CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8")));
					try {
						KBEntry<Integer,DbPage> entry = deserialiseCsvRecord(cri);
						if ( (entry != null) && (filterEntry(entry) != null) ) {
							try {
								db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
								nbToAdd++;
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					} catch(Exception e) {
						System.out.println("Error deserialising: " + line);
						e.printStackTrace();
					}
				}
				tx.commit();
				tx.close();
				input.close();
				isLoaded = true;
			}
		};
	}

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

	public LabelDatabase buildLabelDatabase() {
		return new LabelDatabase(env);
	}

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
				return new KBEntry<Integer,DbIntList>(id, new DbIntList(linkIds));
			}
			
			@Override
			public void loadFromFile(File dataFile, boolean overwrite) throws IOException  {
				if (isLoaded && !overwrite)
					return;
				System.out.println("Loading " + getName());

				if (dataFile == null)
					throw new NerdResourceException("Markup file not found");

				BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
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
					CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8")));
					KBEntry<Integer,DbIntList> entry = deserialiseCsvRecord(cri);
					try {
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

	public KBDatabase<Integer,DbIntList> buildIntIntListDatabase(final DatabaseType type) {
		switch (type) {
			case categoryParents:
			case articleParents:
			case childCategories:
			case childArticles:
			case redirectSourcesByTarget:
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
					LOGGER.warn("Ignoring unknown statistic: " + statName);
					return null;
				}
				return new KBEntry<Integer, Long>(k,v);
			}
		};
	}

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

	public PageLinkCountDatabase buildPageLinkCountDatabase() {
		return new PageLinkCountDatabase(env);
	}

	public KBDatabase<Integer,String> buildMarkupDatabase() {
		return new MarkupDatabase(env, DatabaseType.markup);
	}

	public KBDatabase<Integer,String> buildMarkupFullDatabase() {
		return new MarkupDatabase(env, DatabaseType.markupFull);
	}

	public KBDatabase<Integer,String> buildDbConceptByPageIdDatabase() {
		return new KBDatabase<Integer,String>(env, DatabaseType.conceptByPageId) {
			// using standard LMDB copy mode
			@Override
			public String retrieve(Integer key) {
				byte[] cachedData = null;
				String record = null;
				try (Transaction tx = environment.createReadTransaction()) {
					cachedData = db.get(tx, KBEnvironment.serialize(key));
					if (cachedData != null) {
						record = (String)KBEnvironment.deserialize(cachedData);
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				return record;
			}

			public void loadFromFile(File dataFile, boolean overwrite) throws Exception  {
//System.out.println("input file: " + dataFile.getPath());
System.out.println("isLoaded: " + isLoaded);
				if (isLoaded && !overwrite)
					return;
				System.out.println("Loading " + name + " database");

				if (dataFile == null)
					throw new NerdResourceException("Markup file not found");

				BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
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
