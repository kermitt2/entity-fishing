package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.record.CsvRecordInput;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.Relation.RelationType;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class StatementDatabase extends StringRecordDatabase<List<Relation>> {
	private static final Logger logger = LoggerFactory.getLogger(StatementDatabase.class);	

	public StatementDatabase(KBEnvironment env) {
		super(env, DatabaseType.statements);
	}

	@Override
	public KBEntry<String, List<Relation>> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	private KBEntry<String, List<Relation>> deserializePageLinkCsvRecord(CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override 
	public void loadFromFile(File dataFile, boolean overwrite) throws IOException  {
System.out.println("input file: " + dataFile.getPath());
		// ok it's not csv here, rather piped but let's go on ;)
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + name + " database");

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));

		String line = null;
		int nbToAdd = 0;
		int nbTotalAdded = 0;
		int currentPageId = -1;
		List<Relation> relations = new ArrayList<Relation>();
		Transaction tx = environment.createWriteTransaction();
		while ((line=input.readLine()) != null) {
			if (nbToAdd >= 10000) {
				try {
					tx.commit();
					tx.close();
					nbToAdd = 0;
					tx = environment.createWriteTransaction();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			String[] pieces = line.split("\\|");
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

				if (pageId != currentPageId) {
					try {
						db.put(tx, KBEnvironment.serialize(currentPageId), KBEnvironment.serialize(relations));
						nbToAdd++;
					} catch(Exception e) {
						e.printStackTrace();
					}

					currentPageId = pageId;
					relations = new ArrayList<Relation>();
				}
				
				String rel = null;
				if (n<pieces.length) {
					rel = pieces[n];
					n++;
				}
				String value = null;
				int valueId = -1;
				if (n<pieces.length) {
					value = pieces[n];
					// basically value can be another page id (it's a relation) or a string (it's then a property)
					try {
						// this is a page id
						valueId = Integer.parseInt(value);
					} catch(Exception e) {
						// if not an page id (number), this is an attribute value
					}
					n++;
				}
				String template = null;
				if (n<pieces.length) {
					template = pieces[n];
				}

				if (valueId != -1) {
					Relation relation = 
						new Relation(RelationType.CUSTOM, rel, new Integer(pageId), new Integer(valueId), template);
//System.out.println(relation.toString());
					relations.add(relation);
					nbTotalAdded++;
				} 
			}
		}

		// add last property list
		try {
			db.put(tx, KBEnvironment.serialize(currentPageId), KBEnvironment.serialize(relations));
			nbToAdd++;
			nbTotalAdded++;
		} catch(Exception e) {
			e.printStackTrace();
		}

		// last commit
		tx.commit();
		tx.close();
		input.close();
		isLoaded = true;
		System.out.println("Total of " + nbTotalAdded + " relations indexed");
	}

}
