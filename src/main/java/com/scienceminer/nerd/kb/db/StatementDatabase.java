package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.compress.compressors.*;
import org.apache.hadoop.record.CsvRecordInput;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.*;
//import com.scienceminer.nerd.kb.Property.ValueType;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class StatementDatabase extends StringRecordDatabase<List<Statement>> {
	private static final Logger logger = LoggerFactory.getLogger(StatementDatabase.class);	

	public StatementDatabase(KBUpperEnvironment env) {
		super(env, DatabaseType.statements);
	}

	@Override
	public KBEntry<String, List<Statement>> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	private KBEntry<String, List<Statement>> deserializePageLinkCsvRecord(CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override 
	public void loadFromFile(File dataFile, boolean overwrite) throws Exception {
//System.out.println("input file: " + dataFile.getPath());
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + name + " database");

		// open file
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(dataFile));
		CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		String line = null;
		int nbToAdd = 0;
		int nbTotalAdded = 0;
		int currentPageId = -1;
		ObjectMapper mapper = new ObjectMapper();
		Transaction tx = environment.createWriteTransaction();
		while ((line=reader.readLine()) != null) {
			if (line.length() == 0) continue;
			if (line.startsWith("[")) continue;
			if (line.startsWith("]")) break;

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

			JsonNode rootNode = mapper.readTree(line);
			JsonNode idNode = rootNode.findPath("id");
			String itemId = null;
			if ((idNode != null) && (!idNode.isMissingNode())) {
				itemId = idNode.textValue();
			}
            
            if (itemId == null)
            	continue;

            List<Statement> statements = new ArrayList<Statement>();
			JsonNode claimsNode = rootNode.findPath("claims");
			if ((claimsNode != null) && (!claimsNode.isMissingNode())) {
				Iterator<JsonNode> ite = claimsNode.elements();
	            while (ite.hasNext()) {
	            	JsonNode propertyNode = ite.next();
					
	            	String propertytId = null;
	            	String value = null;

	            	Iterator<JsonNode> ite2 = propertyNode.elements();
	            	if (ite2.hasNext()) {
	            		JsonNode mainsnakNode = ite2.next();
	            		JsonNode propNameNode = mainsnakNode.findPath("property");
	            		if ((propNameNode != null) && (!propNameNode.isMissingNode())) {
		            		propertytId = propNameNode.textValue();
		            	}

		            	JsonNode dataValueNode = mainsnakNode.findPath("datavalue");
		            	if ((dataValueNode != null) && (!dataValueNode.isMissingNode())) {
		            		JsonNode valueNode = dataValueNode.findPath("value");
		            		if ((valueNode != null) && (!valueNode.isMissingNode())) {
		            			// for "entity-type":"item", we just take the wikidata id
		            			JsonNode entityTypeNode = valueNode.findPath("entity-type");
		         		   		if ((entityTypeNode != null) && (!entityTypeNode.isMissingNode()) && (entityTypeNode.textValue().equals("item"))) {
		         		   			JsonNode localIdNode = valueNode.findPath("id");
		            				if ((localIdNode != null) && (!localIdNode.isMissingNode())) {
		            					value = localIdNode.textValue();
		            				}
		         		   		} 
		         		   		if (value == null) {
			            			// default, store the json value
			            			value = valueNode.toString();
			            		}
		            		}
		            	}

		            	if ((propertytId != null) && (value != null)) {
							Statement statement = new Statement(itemId, propertytId, value);
//System.out.println("Adding: " + statement.toString());
							statements.add(statement);
						}
					}

				}
			}

			if (statements.size() > 0) {
				try {
					db.put(tx, KBEnvironment.serialize(itemId), KBEnvironment.serialize(statements));
					nbToAdd++;
					nbTotalAdded++;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}			

		// last commit
		tx.commit();
		tx.close();
		reader.close();
		isLoaded = true;
		System.out.println("Total of " + nbTotalAdded + " statements indexed");
	}

}
