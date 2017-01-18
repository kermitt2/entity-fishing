package org.wikipedia.miner.db;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.record.CsvRecordInput;
import java.math.BigInteger;

import com.scienceminer.nerd.utilities.*;

import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.db.struct.DbPage;
import org.wikipedia.miner.model.Page.PageType;
import org.wikipedia.miner.util.ProgressTracker;
import org.wikipedia.miner.util.WikipediaConfiguration;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class TitleDatabase extends StringIntDatabase {

	public TitleDatabase(WEnvironment env, DatabaseType type) {
		super(env, type);

		if (type != DatabaseType.articlesByTitle && type != DatabaseType.categoriesByTitle && type != DatabaseType.templatesByTitle) 
			throw new IllegalArgumentException("type must be either DatabaseType.articlesByTitle, DatabaseType.categoriesByTitle or DatabaseType.templatesByTitle") ;
	}

	@Override
	public WEntry<String, Integer> deserialiseCsvRecord(CsvRecordInput record)
	throws IOException {
		Integer id = record.readInt(null) ;

		DbPage p = new DbPage() ;
		p.deserialize(record) ;

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

	@Override
	public void loadFromCsvFile(File dataFile, boolean overwrite, ProgressTracker tracker) throws IOException  {
		if (isLoaded && !overwrite)
			return ;

		if (tracker == null) tracker = new ProgressTracker(1, WDatabase.class) ;
		tracker.startTask(dataFile.length(), "Loading " + getName()) ;

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8")) ;

		long bytesRead = 0 ;

		TreeMap<String, Integer> tmp = new TreeMap<String, Integer>() ;

		String line = null;
		while ((line=input.readLine()) != null) {
			bytesRead = bytesRead + line.length() + 1 ;

			CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8"))) ;

			WEntry<String,Integer> entry = deserialiseCsvRecord(cri) ;

			if (entry != null) {				
				tmp.put(entry.getKey(), entry.getValue()) ;
				tracker.update(bytesRead) ;
			}
		}
		input.close();

		//Database db = getDatabase(false) ;
		int nbToAdd = 0;
		Transaction tx = environment.createWriteTransaction();
		for (Map.Entry<String, Integer> entry: tmp.entrySet()) {
			if (nbToAdd == 10000) {
				tx.commit();
				tx.close();
				nbToAdd = 0;
				tx = environment.createWriteTransaction();
			}
			if (entry != null) {
				try {
					db.put(tx, bytes(entry.getKey()), BigInteger.valueOf(entry.getValue()).toByteArray());
					nbToAdd++;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}

		tx.commit();
		tx.close();
		isLoaded = true;
	}

}
