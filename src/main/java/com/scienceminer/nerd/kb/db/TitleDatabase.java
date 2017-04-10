package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.record.CsvRecordInput;
import java.math.BigInteger;

import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.kb.model.Page.PageType;

import org.wikipedia.miner.db.struct.DbPage;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class TitleDatabase extends StringIntDatabase {

	public TitleDatabase(KBEnvironment env, DatabaseType type) {
		super(env, type);

		if ((type != DatabaseType.articlesByTitle) && 
			(type != DatabaseType.categoriesByTitle) && 
			(type != DatabaseType.templatesByTitle) ) 
			throw new IllegalArgumentException("type must be either DatabaseType.articlesByTitle, DatabaseType.categoriesByTitle or DatabaseType.templatesByTitle");
	}

	@Override
	public KBEntry<String, Integer> deserialiseCsvRecord(CsvRecordInput record)
	throws IOException {
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

	@Override
	public void loadFromCsvFile(File dataFile, boolean overwrite) throws IOException  {
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + getName());

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));

		//TreeMap<String, Integer> tmp = new TreeMap<String, Integer>();
		int nbToAdd = 0;
		String line = null;
		Transaction tx = environment.createWriteTransaction();
		while ((line=input.readLine()) != null) {
			if (nbToAdd == 10000) {
				tx.commit();
				tx.close();
				nbToAdd = 0;
				tx = environment.createWriteTransaction();
			}
			CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8")));
			KBEntry<String,Integer> entry = deserialiseCsvRecord(cri);

			if (entry != null) {				
				//tmp.put(entry.getKey(), entry.getValue());
				//}
			//for (Map.Entry<String, Integer> entry: tmp.entrySet()) {
			//if (entry != null) {
				try {
					//db.put(tx, bytes(entry.getKey()), BigInteger.valueOf(entry.getValue()).toByteArray());
					db.put(tx, bytes(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
					nbToAdd++;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}

		tx.commit();
		tx.close();
		isLoaded = true;

		input.close();
	}

}
