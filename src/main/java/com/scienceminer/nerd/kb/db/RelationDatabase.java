package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.record.CsvRecordInput;

import org.wikipedia.miner.util.WikipediaConfiguration;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;

import org.wikipedia.miner.util.*;

import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.Relation.RelationType;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class RelationDatabase extends IntRecordDatabase<Relation> {
	private static final Logger logger = LoggerFactory.getLogger(RelationDatabase.class);	

	public RelationDatabase(KBEnvironment env) {
		super(env, DatabaseType.relations);
	}

	@Override
	public KBEntry<Integer, Relation> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	private KBEntry<Integer, Relation> deserializePageLinkCsvRecord(CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override 
	public void loadFromCsvFile(File dataFile, boolean overwrite) throws IOException  {
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + name + " database");

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
		long bytesRead = 0;

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

			String[] pieces = line.split("|");
			int pageId = -1;
			for(int n=0; n<pieces.length; n++) {
				// each line gives the page id then a sequence of triplets
				if (n== 0) {
					String page = pieces[n];
					try {
						pageId = Integer.parseInt(page);
					} catch(Exception e) {
						logger.warn("Invalid page id: " + page);
						break;
					}
					n++;
				} 
				
				String rel = null;
				if (n<pieces.length) {
					n++;
					rel = pieces[n];
				}
				String value = null;
				int valueId = -1;
				if (n<pieces.length) {
					n++;
					value = pieces[n];
					// basically value can be another page id (it's a relation) or a string (it's then a property)
					try {
						valueId = Integer.parseInt(value);
					} catch(Exception e) {
						// nothing
					}
				}
				String template = null;
				if (n<pieces.length) {
					n++;
					template = pieces[n];
				}

				if (valueId != -1) {
					Relation relation = 
						new Relation(RelationType.CUSTOM, rel, new Integer(pageId), new Integer(valueId), template);
					try {
						db.put(tx, BigInteger.valueOf(relation.getConcept1ID()).toByteArray(), Utilities.serialize(relation));
						nbToAdd++;
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		tx.commit();
		tx.close();
		input.close();
		isLoaded = true;
	}

}
