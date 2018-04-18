package com.scienceminer.nerd.kb.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.Property;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.hadoop.record.CsvRecordInput;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.nio.ByteBuffer.allocateDirect;

public class PropertyDatabase extends StringRecordDatabase<Property> {
    private static final Logger logger = LoggerFactory.getLogger(PropertyDatabase.class);

    public PropertyDatabase(KBEnvironment env) {
        super(env, DatabaseType.properties);
    }

    @Override
    public KBEntry<String, Property> deserialiseCsvRecord(CsvRecordInput record) {
        throw new UnsupportedOperationException();
    }

    /**
     * Property descriptions are expressed in JSON format
     */
    @Override
    public void loadFromFile(File dataFile, boolean overwrite) throws Exception {
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
        ObjectMapper mapper = new ObjectMapper();
        List<Property> properties = new ArrayList<Property>();
        while ((line = reader.readLine()) != null) {
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
                } catch (Exception e) {
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
        Txn<ByteBuffer> tx = environment.txnWrite();
        for (Property property : properties) {
            try {
                final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
                keyBuffer.put(KBEnvironment.serialize(property.getId()));
                final byte[] serializedValue = KBEnvironment.serialize(property);
                final ByteBuffer valBuffer = allocateDirect(serializedValue.length);
                valBuffer.put(serializedValue);
                db.put(tx, keyBuffer, valBuffer);
                nbTotalAdded++;
            } catch (Exception e) {
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
}
