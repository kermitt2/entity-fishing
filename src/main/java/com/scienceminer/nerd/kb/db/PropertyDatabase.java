package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
import com.scienceminer.nerd.kb.Property;
import com.scienceminer.nerd.kb.Property.ValueType;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

public class PropertyDatabase extends StringRecordDatabase<Property> {
	private static final Logger logger = LoggerFactory.getLogger(PropertyDatabase.class);	

	public PropertyDatabase(KBEnvironment env) {
		super(env, DatabaseType.properties);
	}

	@Override
	public KBEntry<String, Property> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 *  Property descriptions are expressed in JSON format
	 */
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
		ObjectMapper mapper = new ObjectMapper();
		List<Property> properties = new ArrayList<Property>();
		while ((line=reader.readLine()) != null) {
			if (line.length() == 0) continue;
			if (line.startsWith("[")) continue;
			if (line.startsWith("]")) break;
			
			JsonNode rootNode = mapper.readTree(line);
			
			JsonNode typeNode = rootNode.findPath("type");
			String type = null;
			if ((typeNode != null) && (!typeNode.isMissingNode())) {
				type = typeNode.textValue();
			}
			
			if (type == null)
				continue;

			if (!type.equals("property"))
				continue;

			JsonNode idNode = rootNode.findPath("id");
			String itemId = null;
			if ((idNode != null) && (!idNode.isMissingNode())) {
				itemId = idNode.textValue();
			}
            
            if (itemId == null)
            	continue;

            JsonNode datatypeNode = rootNode.findPath("datatype");
			String datatype = null;
			Property.ValueType valueType = null;
			if ((datatypeNode != null) && (!datatypeNode.isMissingNode())) {
				datatype = datatypeNode.textValue();
			}

			if (datatype == null)
            	continue;
            else {
				try {
					valueType = Property.ValueType.fromString(datatype);
				} catch(Exception e) {
					System.out.println("Invalid datatype value: " + datatype);
				}
			}

			if (valueType == null) 
				continue;

			String name = null;
			JsonNode namesNode = rootNode.findPath("labels");
			if ((namesNode != null) && (!namesNode.isMissingNode())) {
				JsonNode enNameNode = namesNode.findPath("en");
				if ((enNameNode != null) && (!enNameNode.isMissingNode())) {
					JsonNode nameNode = enNameNode.findPath("value");
					name = nameNode.textValue();
				}
			}

			if (name == null)
				continue;

			Property property = new Property(itemId, name, valueType);
			properties.add(property);
		}			

		int nbTotalAdded = 0;
        Transaction tx = environment.createWriteTransaction();
        for(Property property : properties) {
        	try {
	        	db.put(tx, KBEnvironment.serialize(property.getId()), KBEnvironment.serialize(property));
				nbTotalAdded++;
			} catch(Exception e) {
				e.printStackTrace();
			}
        }

		// commit
		tx.commit();
		tx.close();
		reader.close();
		isLoaded = true;
		System.out.println("Total of " + nbTotalAdded + " properties indexed");
	}



	//@Override 
	/*public void loadFromFile(File dataFile, boolean overwrite) throws IOException  {
System.out.println("input file: " + dataFile.getPath());
		// ok it's not csv here, it's json but let's go on ;)

		// input example: 
		// {
      	// 		"property" : {
        // 		"type" : "uri",
        // 		"value" : "http://www.wikidata.org/entity/P6"
        // 	},
        // 	"propertyLabel" : {
        // 		"xml:lang" : "en",
        // 		"type" : "literal",
        // 		"value" : "head of government"
        // 	}
        // } 
		if (isLoaded && !overwrite)
			return;
		System.out.println("Loading " + name + " database");

		String json = FileUtils.readFileToString(dataFile, "UTF-8");
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readTree(json);

		Map<String,String> properties = new HashMap<String,String>();

        JsonNode resultsNode = jsonNode.findPath("results");
        if ((resultsNode != null) && (!resultsNode.isMissingNode())) {
	        JsonNode bindingsNode = resultsNode.findPath("bindings");
	        if ((bindingsNode != null) && (!bindingsNode.isMissingNode())) {
	        	// properties are children of claimsNode
	        	Iterator<JsonNode> ite = bindingsNode.elements();
	            while (ite.hasNext()) {
	            	JsonNode propertyNode = ite.next();

	            	String propertyId = null;
	            	String label = null;

                   	// get property id (e.g. P6)
                    JsonNode propNode = propertyNode.findPath("property");
                    if ((propNode != null) && (!propNode.isMissingNode())) {
                    	JsonNode valueNode = propNode.findPath("value");
                    	if ((valueNode != null) && (!valueNode.isMissingNode())) {
                    		propertyId = valueNode.textValue().replace("http://www.wikidata.org/entity/", "");
                    	}
                    }

                    // get the property label
                    JsonNode labelNode = propertyNode.findPath("propertyLabel");
                    if ((labelNode != null) && (!labelNode.isMissingNode())) {
	                    JsonNode valueNode = propNode.findPath("value");
	                    if ((valueNode != null) && (!valueNode.isMissingNode())) {
                    		label = valueNode.textValue();
                    	}
                    }

                    if ( (propertyId != null) && (label != null) )
	                    properties.put(propertyId, label);
                }
            }
        }

		int nbTotalAdded = 0;
        Transaction tx = environment.createWriteTransaction();
        for(Map.Entry<String, String> entry : properties.entrySet()) {
        	try {
	        	db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
				nbTotalAdded++;
			} catch(Exception e) {
				e.printStackTrace();
			}
        }

		// add last db commit
		tx.commit();
		tx.close();
		isLoaded = true;
		System.out.println("Total of " + nbTotalAdded + " properties indexed");
	}*/

}
