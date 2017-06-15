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

/**
 * A {@link KBDatabase} for associating concept identifier with list of identifier associated to a language. 
 */
public class ConceptDatabase extends StringRecordDatabase<Map<String,Integer>> {

	/**
	 * Creates or connects to a database, whose name and type will be {@link KBDatabase.DatabaseType#concepts}. 
	 * This will index label statistics according to their raw, unprocessed texts. 
	 * 
	 * @param env the KBEnvironment surrounding this database
	 */
	public ConceptDatabase(KBEnvironment env) {
		super(env, DatabaseType.concepts);
	}

	/**
	 * Retrieves the label statistics associated with the given text key. 
	 * 
	 * @return true if the database has been prepared for use, otherwise false
	 */
	@Override
	public Map<String,Integer> retrieve(String key) {
		return super.retrieve(key);
	}

	/**
	 * Builds the persistent database from a file.
	 * 
	 * @param dataFile the file (here a text file with fields separated by a tabulation) containing data to be loaded
	 * @param overwrite true if the existing database should be overwritten, otherwise false
	 * @throws IOException if there is a problem reading or deserialising the given data file.
	 */
	public void loadFromFile(File dataFile, boolean overwrite) throws IOException  {
//System.out.println("input file: " + dataFile.getPath());
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

			String[] pieces = line.split(",");
			if (pieces.length < 3)
				continue;
			int pos = 0;
			String keyVal = pieces[pos];
			if ( (keyVal == null) || (keyVal.trim().length() == 0) )
				continue;
			pos++;
			Map<String,Integer> conceptMap = new HashMap<String,Integer>();
			while(pos < pieces.length) {
				// pieces[1]
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
