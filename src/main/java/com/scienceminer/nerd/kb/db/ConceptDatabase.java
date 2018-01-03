package com.scienceminer.nerd.kb.db;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import java.io.*;
import java.util.*;

import org.apache.hadoop.record.CsvRecordInput;
import org.apache.hadoop.record.CsvRecordOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.model.hadoop.DbLabel;
import com.scienceminer.nerd.kb.model.hadoop.DbSenseForLabel;

import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

import com.scienceminer.nerd.exceptions.NerdResourceException;

public class ConceptDatabase extends StringRecordDatabase<Map<String,Integer>> {

	public ConceptDatabase(KBEnvironment env) {
		super(env, DatabaseType.concepts);
	}

	public ConceptDatabase(KBEnvironment env, DatabaseType type) {
		super(env, type);
	}

	@Override
	public Map<String,Integer> retrieve(String key) {
		return super.retrieve(key);
	}

	public void loadFromFile(File dataFile, boolean overwrite) throws Exception  {
//System.out.println("input file: " + dataFile.getPath());
//System.out.println("isLoaded: " + isLoaded);
		if (isLoaded && !overwrite)
			return;
		if (dataFile == null)
			throw new NerdResourceException("Concept dump file not found: " + dataFile.getPath());
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

			String[] pieces = line.split(",");
			if (pieces.length <= 1) 
				continue;

			int pos = 0;
			String keyVal = pieces[pos];
			if ( (keyVal == null) || (keyVal.trim().length() == 0) || (!keyVal.startsWith("Q")) )
				continue;
			pos++;
			Map<String,Integer> conceptMap = new HashMap<String,Integer>();
			while(pos < pieces.length) {
				if (pieces[pos].equals("m{}")) {
					pos++;
					continue;
				}
				String lang = pieces[pos].replace("m{'", "").replace("'","");
				pos++;
				if (pos == pieces.length)
					break;
				String pageidString = pieces[pos].replace("'","").replace("}","");;
				pos++;
				Integer pageid = null;
				try {
					pageid = Integer.parseInt(pageidString);
				} catch(Exception e) {
					e.printStackTrace();
				}
				if ( (lang.trim().length() > 0) && (pageid != null) )
					conceptMap.put(lang, pageid);
			}
			KBEntry<String,Map<String,Integer>> entry = new KBEntry<String,Map<String,Integer>>(keyVal, conceptMap);
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
	public KBEntry<String,Map<String,Integer>> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

}
