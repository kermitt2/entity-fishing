package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
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
import com.scienceminer.nerd.kb.Property;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

public class PropertyDatabase extends StringRecordDatabase<List<Property>> {
	private static final Logger logger = LoggerFactory.getLogger(PropertyDatabase.class);	

	public PropertyDatabase(KBEnvironment env) {
		super(env, DatabaseType.properties);
	}

	@Override
	public KBEntry<String, List<Property>> deserialiseCsvRecord(
			CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}

	/*private KBEntry<String, List<Property>> deserializePageLinkCsvRecord(CsvRecordInput record) throws IOException {
		throw new UnsupportedOperationException();
	}*/

	/**
	 *  Property descriptions are expressed in JSON format
	 */
	@Override 
	public void loadFromFile(File dataFile, boolean overwrite) throws IOException  {
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
	}

}