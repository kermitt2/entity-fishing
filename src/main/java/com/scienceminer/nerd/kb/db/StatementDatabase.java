package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.compress.compressors.*;
import org.apache.hadoop.record.CsvRecordInput;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.exceptions.NerdResourceException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class StatementDatabase extends StringRecordDatabase<List<Statement>> {
	private static final Logger logger = LoggerFactory.getLogger(StatementDatabase.class);	

	public StatementDatabase(KBUpperEnvironment env, DatabaseType type) {
		super(env, type);
	}

	@Override
	public KBEntry<String, List<Statement>> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	private KBEntry<String, List<Statement>> deserializePageLinkCsvRecord(CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void loadStatementsFromFile(File dataFile, 
							 		PropertyDatabase dbProperties, 
							 		ConceptLabelDatabase dbConceptLabels, 
							 		ConceptDatabase dbConcepts,
							 		boolean overwrite) 
		throws Exception {
//System.out.println("input file: " + dataFile.getPath());
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + name + " database");

		if (dataFile == null)
			throw new NerdResourceException("Wikidata dump file not found");

		// open file
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(dataFile));
		CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		String line = null;
		int nbToAdd = 0;
		int nbTotalAdded = 0;
		int currentPageId = -1;
		List<Property> properties = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		Transaction tx = environment.createWriteTransaction();
		if (dbConceptLabels != null)
			dbConceptLabels.startFillDatabase();
		while ((line=reader.readLine()) != null) {
			if (line.length() == 0) 
				continue;
			if (line.startsWith("[")) 
				continue;
			if (line.startsWith("]")) 
				break;

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

			JsonNode typeNode = rootNode.findPath("type");
			String type = null;
			if ((typeNode != null) && (!typeNode.isMissingNode())) {
				type = typeNode.textValue();
			}

			JsonNode idNode = rootNode.findPath("id");
			String itemId = null;
			if ((idNode != null) && (!idNode.isMissingNode())) {
				itemId = idNode.textValue();
			}
            
            if (itemId == null)
            	continue;

            List<Statement> statements = new ArrayList<Statement>();

			if (type != null && type.equals("property")) {
				if (dbProperties != null) {
					JsonNode datatypeNode = rootNode.findPath("datatype");
					String datatype = null;
					Property.ValueType valueType = null;
					if ((datatypeNode != null) && (!datatypeNode.isMissingNode())) {
						datatype = datatypeNode.textValue();
					}

					if (datatype != null) {
						try {
							valueType = Property.ValueType.fromString(datatype);
						} catch(Exception e) {
							logger.info("Invalid datatype value: " + datatype);
						}
					}

					String name = null;
					if (valueType != null) {
						JsonNode namesNode = rootNode.findPath("labels");
						if ((namesNode != null) && (!namesNode.isMissingNode())) {
							JsonNode enNameNode = namesNode.findPath("en");
							if ((enNameNode != null) && (!enNameNode.isMissingNode())) {
								JsonNode nameNode = enNameNode.findPath("value");
								name = nameNode.textValue();
							}
						}
					}

					if (name != null) {	
						Property property = new Property(itemId, name, valueType);
						properties.add(property);
					}
				}
			} else {
				// we have an entity, from which we can get the statements and labels
				// labels
				Map<String,String> labelsPerLang = new TreeMap<>();

				JsonNode labelsNode = rootNode.findPath("labels");
				if ((labelsNode != null) && (!labelsNode.isMissingNode())) {

					Iterator<Map.Entry<String,JsonNode>> ite = labelsNode.fields();
		            while (ite.hasNext()) {
		            	Map.Entry<String, JsonNode> field = ite.next();
		            	JsonNode baseLangNode = field.getValue();

		            	JsonNode labelNode = baseLangNode.findPath("value");
		            	JsonNode langNode = baseLangNode.findPath("language");
		            	String labelValue = labelNode.textValue();
		            	String langValue = langNode.textValue();

		            	labelsPerLang.put(langValue, labelValue);
		            }
				}

				dbConceptLabels.fillDatabase(itemId, labelsPerLang);
			
				// statements
				// optionally we load statements only for concepts that have at least one Wikipedia
				// page in the loaded languages
				if (dbConcepts != null) {
					if (dbConcepts.retrieve(itemId) != null) {
						Map<String,Integer> entryConcept = dbConcepts.retrieve(itemId);
						if (entryConcept.size() == 0)
							continue;
					}
				}

				JsonNode claimsNode = rootNode.findPath("claims");
				if ((claimsNode != null) && (!claimsNode.isMissingNode())) {
					Iterator<JsonNode> ite = claimsNode.elements();
		            while (ite.hasNext()) {
		            	JsonNode propertyNode = ite.next();
						
		            	String propertytId = null;
		            	String value = null;

		            	Iterator<JsonNode> ite2 = propertyNode.elements();
		            	while (ite2.hasNext()) {
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
								if (!statements.contains(statement))
									statements.add(statement);
							}
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

		if (dbConceptLabels != null)
			dbConceptLabels.completeDatabase();

		if (dbProperties != null)
			dbProperties.fillDatabase(properties, overwrite);
	}

	/**
	 * Reverse statement index (where the key is the tail entity) is created only when needed.
	 * Creation is based on the normal statement database (where key is the head entity).
	 */
	public void loadReverseStatements(boolean overwrite, StatementDatabase statementDb) {
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + name + " database");
		long totalStatements = statementDb.getDatabaseSize();
		System.out.println(totalStatements + " statements in statement db");

		KBIterator iter = new KBIterator(statementDb);
		Transaction tx = environment.createWriteTransaction();
		Map<String, List<Statement>> tmpMap = new TreeMap<String, List<Statement>>();
		long nbSeen = -1;
		int nbToAdd = 0;
		long nbTotalAdded = 0;
		while(iter.hasNext()) {
			nbSeen++;
			if (nbToAdd >= 1000) {
				try {
					// put what's in the tmp map into the db
					for (Map.Entry<String, List<Statement>> entry : tmpMap.entrySet()) {
						db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
					}
					tx.commit();
					tx.close();
					nbToAdd = 0;
					tx = environment.createWriteTransaction();
					// reset temporary map
					tmpMap = new TreeMap<String, List<Statement>>(); 
				} catch(Exception e) {
					e.printStackTrace();
				}
				//System.out.println(nbSeen + " / " + totalStatements);
			}

			Entry entry = iter.next();
			byte[] keyData = entry.getKey();
			byte[] valueData = entry.getValue();
			//Page p = null;
			
			try {
				String entityId = (String)KBEnvironment.deserialize(keyData);
				List<Statement> statements = (List<Statement>)KBEnvironment.deserialize(valueData);
				for (Statement statement : statements) {
					String value = statement.getValue();
					if ( (value != null) && value.startsWith("Q") ) {
						// the statement value is an entity

						// check temporary map first
						List<Statement> newStatements = tmpMap.get(value);
						if (newStatements == null) {
							// nothing in the tmp map, we look at the db
							newStatements = this.retrieve(value);
						}
						if (newStatements == null) {
							// nothing in db neither, we start a new fresh entry for this entity
							newStatements = new ArrayList<Statement>();
						}
						newStatements.add(statement);

						//db.put(tx, KBEnvironment.serialize(value), KBEnvironment.serialize(newStatements));
						tmpMap.put(value, newStatements);
						nbToAdd++;
					} 
				} 
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		// last commit
		tx.commit();
		tx.close();
		isLoaded = true;
		System.out.println("Total of " + nbTotalAdded + " statements indexed");
	}

}
