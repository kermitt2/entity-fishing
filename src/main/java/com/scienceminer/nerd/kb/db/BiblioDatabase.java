package com.scienceminer.nerd.kb.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.scienceminer.nerd.kb.Property;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.exceptions.NerdResourceException;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.hadoop.record.CsvRecordInput;

import org.fusesource.lmdbjni.Transaction;
import org.fusesource.lmdbjni.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BiblioDatabase extends StringRecordDatabase<String> {
	private static final Logger logger = LoggerFactory.getLogger(BiblioDatabase.class);	

	public BiblioDatabase(KBEnvironment env) {
		super(env, DatabaseType.biblio);
	}

	@Override
	public KBEntry<String, String> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Load the bilbiographical index 
	 */
	public void fillBiblioDb(ConceptDatabase conceptDb, StatementDatabase statementDb, boolean overwrite) throws Exception {
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + name + " database");

		if (conceptDb == null)
			throw new NerdResourceException("conceptDb not found");

		if (statementDb == null)
			throw new NerdResourceException("statementDb not found");

		// iterate through concepts
		KBIterator iter = new KBIterator(conceptDb);
		Transaction tx = environment.createWriteTransaction();
		try {
			int nbToAdd = 0;
			int n = 0; // total entities
			int nbDoi = 0; // total doi found
			while(iter.hasNext()) {
				if (nbToAdd > 10000) {
					tx.commit();
					tx.close();
					tx = environment.createWriteTransaction();
					nbToAdd = 0;
				}
				Entry entry = iter.next();
				byte[] keyData = entry.getKey();
				byte[] valueData = entry.getValue();
				//Page p = null;
				try {
					String entityId = (String)KBEnvironment.deserialize(keyData);
					// check the statements for a property P356 (DOI)
					String doi = null;

					List<Statement> statements = statementDb.retrieve(entityId);
					if ( (statements != null) && (statements.size() > 0) ) {
						for(Statement statement : statements) {
							if (statement.getPropertyId().equals("P356")) {
								doi = statement.getValue();
								//System.out.println("found DOI: " + doi);
								if (doi.startsWith("\""))
									doi = doi.substring(1);
								if (doi.endsWith("\""))
									doi = doi.substring(0, doi.length()-1);
							}
						}
					}

					if (doi != null) {
						KBEntry<String,String> theEntry = new KBEntry<>(doi, entityId);
                        try {
                            db.put(tx, KBEnvironment.serialize(theEntry.getKey()),
                                KBEnvironment.serialize(theEntry.getValue()));
                            nbToAdd++;
                            nbDoi++;
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
				} catch(Exception e) {
					logger.error("fail to write entity description", e);
				}
				n++;
			}
			System.out.println("total nb entities visited: " + n);
			System.out.println("total nb DOI found: " + nbDoi);
		} catch(Exception e) {
			logger.error("Error when creating biblioDb", e);
 		} finally {
			if (iter != null)
				iter.close();
			tx.commit();
			tx.close();
			isLoaded = true;
		}
	}
}
