package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;  

import org.mapdb.*;

import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;

/*import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;
import org.codehaus.jackson.map.ObjectMapper;*/

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

/**
 * Persistent mapping between Wikipedia page and GRISP domain taxonomy based on FreeBase types.
 * 
 * @author Patrice Lopez
 * 
 */
public class FreeBaseTypeMap { 
    /**
     * The class Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeBaseTypeMap.class);

    // mapdb
    private org.mapdb.DB mapdb = null;
    private String database_path = null;
    private String database_name = "types";

    // map a Wikipedia page id to a list of types derived from FreeBase (see FreeBase mappings to GRISP)
    private ConcurrentMap<Integer, String> types = null;

    // domain id map
    private Map<Integer,String> id2domain = null;
  
    // domain label map  
    private Map<String,Integer> domain2id = null;

    // wikipedia main categories (pageId of the category) to grisp domains
    private Map<Integer,List<Integer>> wikiCat2domains = null;

    private Wikipedia wikipedia = null;
    private String lang = null;

    private static String grispDomains = "data/grisp/domains.txt";
    private static String wikiGrispMapping = "data/wikipedia/mapping.txt";

    // mongoDB
    private DB db = null;
    private MongoClient mongo = null;
    private DBCollection collectionNerd = null;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Hidden constructor
     */
    public FreeBaseTypeMap() {
    }

    public void setWikipedia(Wikipedia wikipedia) {
        this.wikipedia = wikipedia;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * Open index for wikipedia domain map creation
     */
    public void openCreate() {
        File home = null;      
        try {
            home = new File(NerdProperties.getInstance().getMapDBPath());
        } catch (Exception e) {
            throw new NerdException(e);
        }
        try {
            // open MapDB
            mapdb = DBMaker
                    .fileDB(NerdProperties.getInstance().getMapDBPath() + "/" + database_name + "-" + lang + ".db")
                    .fileMmapEnable()            // always enable mmap
                    .make();

            types = mapdb.hashMap(database_name)
                    .keySerializer(Serializer.INTEGER)
                    .valueSerializer(Serializer.STRING)
                    .make();
        } catch (Exception dbe) {
            LOGGER.debug("Error when opening MapDB.");
            throw new NerdException(dbe);
        }
    }

    /**
     * Open index for wikipedia domain map usage, non transactional
     */
    public void openUse() {
        File home = null;      
        try {
            home = new File(NerdProperties.getInstance().getMapDBPath());
        } catch (Exception e) {
            throw new NerdException(e);
        }
        try {
            // open MapDB
            mapdb = DBMaker
                    .fileDB(NerdProperties.getInstance().getMapDBPath() + "/" + database_name + "-" + lang + ".db")
                    .fileMmapEnable()            // always enable mmap
                    .readOnly()
                    .make();

            types = mapdb.hashMap(database_name)
                    .keySerializer(Serializer.INTEGER)
                    .valueSerializer(Serializer.STRING)
                    .makeOrGet();
        } catch (Exception dbe) {
            LOGGER.debug("Error when opening MapDB.");
            throw new NerdException(dbe);
        }
    }

    /**
     * Close index wikipedia domain map
     */
    public void close() {
        mapdb.close();
    }

    private void connectMongoDB() {
        try {
            mongo = new MongoClient(NerdProperties.getInstance().getMongoDBHost(), 
                                    NerdProperties.getInstance().getMongoDBPort() );
        }
        catch(Exception e) {
            LOGGER.debug("Cannot open a client to MongoDB.");
            throw new NerdResourceException(e);
        }

        try {
            // database
            boolean dbFound = false;
            List<String> dbs = mongo.getDatabaseNames();
            for(String db : dbs){
                if (db.equals("nerd4science")) {
                    dbFound = true;
                }
            }
            if (!dbFound) {
                LOGGER.debug("MongoDB database nerd4science does not exist and will be created");
            }
        }
        catch(Exception e) {
            LOGGER.debug("Cannot retrieve existing MongoDB database list.");
            throw new NerdResourceException(e);
        }

        try {
            db = mongo.getDB("nerd4science");
        }
        catch(Exception e) {
            LOGGER.debug("Cannot retrieve MongoDB nerd4science database.");
            throw new NerdResourceException(e);
        }

        try {
            // collection
            boolean collectionFound = false;
            Set<String> collections = db.getCollectionNames();
            for(String collection : collections) {
                if (collection.equals("nerd-entry")) {
                    collectionFound = true;
                }
            }
            if (!collectionFound) {
                LOGGER.debug("MongoDB collection nerd-entry does not exist and will be created");
            }
            collectionNerd = db.getCollection("nerd-entry");

            // index on PageID
            collectionNerd.ensureIndex(new BasicDBObject("PageID", 1));  
        }
        catch(Exception e) {
            LOGGER.debug("Cannot retrieve MongoDB nerd-entry collection.");
            throw new NerdResourceException(e);
        }
    }

    public void closeMongoDB() {
        mongo.close();
    }

    private String createMapping(Article page) {
        String result = null;

        if (collectionNerd == null) {
            collectionNerd = db.getCollection("nerd-entry");    
            collectionNerd.ensureIndex(new BasicDBObject("PageID", 1)); 
        }
        BasicDBObject query = new BasicDBObject();
        query.put("PageID", page.getId());
        DBObject doc = collectionNerd.findOne(query);

        if (doc == null) {
            //System.out.println("missing PageID: " + query);
            return null;
        }
        
        JsonNode resJsonStruct = null;
        String id = null;
        try { 
            resJsonStruct = mapper.readTree(doc.toString());

            JsonNode subjectNode = resJsonStruct.path("subjectField");
            if (!subjectNode.isMissingNode()) {
                java.util.Iterator<JsonNode> elements = ((ArrayNode) subjectNode).elements();
                while (elements.hasNext()) {
                    JsonNode element = elements.next();
                    String type = element.textValue();
                    if (result == null)
                        result = type;
                    else 
                        result += ";"+type;
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public void createAllMappings() {
        connectMongoDB();
        // for each page id in wikipedia we get the list of domain id
        PageIterator iterator = wikipedia.getPageIterator(Page.PageType.article);
        int p = 0;
        while(iterator.hasNext()) {
            if ((p%10000) == 0)
                System.out.println(p);
            // add to the persistent map
            Page page = iterator.next();
            int pageId = page.getId();
            String theTypes = createMapping((Article) page);
            if (theTypes != null)
                types.put(new Integer(pageId), theTypes);
            p++;
        }
        iterator.close();
        closeMongoDB();
    }

    public List<String> getTypes(int pageId) {
        String list = types.get(new Integer(pageId));
        List<String> result = null;
        if (list != null) {
            result = new ArrayList<String>();
            String[] theList = list.split(";");
            for(int i=0; i<theList.length; i++) {
                result.add(theList[i]);
            }
        }
        return result;
    }

}