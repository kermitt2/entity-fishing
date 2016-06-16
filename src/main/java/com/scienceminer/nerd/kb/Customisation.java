package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.utilities.TextUtilities;
import org.grobid.core.utilities.OffsetPosition;
import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.disambiguation.NerdCandidate;

import java.util.*;
import java.io.*;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;
import org.codehaus.jackson.map.ObjectMapper;*/

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

import org.mapdb.*;

import org.wikipedia.miner.model.*;

/**
 * Class for managing the NERD customisation which are contexts for particular domains.
 *
 * @author Patrice Lopez
 */
public final class Customisation {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(Customisation.class);
	private static volatile Customisation instance;
	private ObjectMapper mapper = new ObjectMapper();
	
	// mongoDB
	/*private DB db = null;
	private MongoClient mongo = null;
	private DBCollection collectionCustomisation = null;*/

	// mapdb
	private DB db = null;
	private String database_path = null;
    private String database_name = "customisations";

    // map a customisation id to the json definition
    private ConcurrentMap<String, String> cDB = null;

	public static Customisation getInstance() throws Exception {
        if (instance == null) {
			getNewInstance();
        }
        return instance;
    }

	/**
     * Creates a new instance.
     */
	private static synchronized void getNewInstance() throws Exception {
		LOGGER.debug("Get new instance of Customisation");		
		instance = new Customisation();
	}
	
	/**
     * Hidden constructor
     */
    public Customisation() {
		//connectMongoDB();
		
	}

	/**
     * Open index for customisations
     */
    public void open() {
        File home = null;      
        try {
            home = new File(NerdProperties.getInstance().getMapDBPath());
        } catch (Exception e) {
            throw new NerdException(e);
        }
        try {
            // open MapDB
            db = DBMaker
                    .fileDB(NerdProperties.getInstance().getMapDBPath() + "/" + database_name+ ".db")
                    .fileMmapEnable()            // always enable mmap
                    .make();

            cDB = db.hashMap(database_name)
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .makeOrGet();
        } catch (Exception dbe) {
            LOGGER.debug("Error when opening MapDB.");
            throw new NerdException(dbe);
        }
    }
	
    /**
     * Close index for customisations
     */
    public void close() {
    	db.close();
    }

	public List<String> getCustomisations() {
		//connectMongoDB();
		List<String> results = null;
		try {
			open();
			results = new ArrayList<String>();
			for(String key : cDB.keySet()) {
				results.add(key);
			}
		} catch(Exception e) {
			LOGGER.debug("Error when opening MapDB.");
            throw new NerdException(e);
		} finally {
        	close();
        }
		return results;
	}
	
	public String getCustomisation(String name) {
		String message = null;
		try {
			open();
			String doc = cDB.get(name);
			if (doc == null) {
				message = "Resource was not found";
			}
			else {
				message = doc;
			}
		}
		catch(Exception e) {
			LOGGER.debug("error, invalid retrieved DBObject.");
			e.printStackTrace();
			message = "Server error";
		} finally {
        	close();
        }
		return message;
	}
	
	public String createCustomisation(String name, String profile) {
		String message = null;
		
		try {
			open();
			if (cDB.get(name) == null) {
                cDB.put(name, profile);
                message = "OK";
            } else {
				message = "Customisation already created.";
            }
		} catch (Exception e) {
            LOGGER.debug("Cannot create customisation.");
			e.printStackTrace();
			message = "Server error";
        } finally {
        	close();
        }

		/*connectMongoDB();
		String message = null;
		if (collectionCustomisation == null) {
			collectionCustomisation = db.getCollection("nerd-customisation");	
			collectionCustomisation.ensureIndex(new BasicDBObject("name", 1)); 
		}
		// test if the customisation does not exist
		BasicDBObject where = new BasicDBObject();
		where.put("name", name);
		DBObject doc = collectionCustomisation.findOne(where);
		if (doc != null) {		   
			 message = "Customisation already created.";
		}
		
		JsonNode resJsonStruct = null;
		try {
			// parse the profile which should be a JSON structure with specific attributes
			resJsonStruct = mapper.readTree(profile);
		}
		catch(Exception e) {
			LOGGER.debug("Cannot read customisation profile.");
			e.printStackTrace();
			message = "Invalid request, cannot read customisation profile.";
		}
		try {
			// we add the name in the json structure
			((ObjectNode)resJsonStruct).put("name",name);
			DBObject dbObject = (DBObject)JSON.parse(resJsonStruct.toString());
			// clean the json
			cleanJsonDoc(dbObject);
			// add the json
			collectionCustomisation.insert(dbObject);
			message = "OK";
		}
		catch(Exception e) {
			LOGGER.debug("Cannot create customisation.");
			e.printStackTrace();
			message = "Server error";
		}*/

		return message;
	}

	public String extendCustomisation(String name, String profile) {
		String message = null;
		try {
			open();
		/*connectMongoDB();
		String message = null;
		if (collectionCustomisation == null) {
			collectionCustomisation = db.getCollection("nerd-customisation");	
			collectionCustomisation.ensureIndex(new BasicDBObject("name", 1)); 
		}
		try {
			DBObject dbObject = (DBObject)JSON.parse(profile);
			cleanJsonDoc(dbObject);
			// test if the customisation exists
			BasicDBObject where = new BasicDBObject();
			where.put("name", name);
			DBObject doc = collectionCustomisation.findOne(where);
			if (doc == null) {		   
				 message = "Resource was not found";
			}
			else {
				// merge the fields of doc and dbObject, 
				// converted into JSON jackson objects for merging
				//JsonNode resJsonStruct1 = mapper.readTree(doc.toString());
				//JsonNode resJsonStruct2 = mapper.readTree(dbObject.toString());
				//JsonNode mergedJson = merge(resJsonStruct1, resJsonStruct2);
				merge(doc, dbObject);
				BasicDBObject searchQuery = new BasicDBObject().append("name", name);
				collectionCustomisation.update(searchQuery, doc);
				
				//collectionCustomisation.remove(doc);
				//DBObject newDoc = (DBObject)JSON.parse(mergedJson.toString());
				//collectionCustomisation.insert(newDoc);
				message = "OK";
			}*/
		}
		catch(Exception e) {
			LOGGER.debug("Cannot extend customisation.");
			e.printStackTrace();
			message = "Server error";
		} finally {
			close();
		}
		return message;
	}
	
	public String deleteCustomisation(String name) {
		String message = null;
		try {
			open();
            // Delete the record(s) for the given key
            cDB.remove(name);
            message = "OK";
        } catch (Exception e) {
            throw new NerdException(e);
        } finally {
			close();
		}

		/*connectMongoDB();
		String message = null;
		if (collectionCustomisation == null) {
			collectionCustomisation = db.getCollection("nerd-customisation");	
			collectionCustomisation.ensureIndex(new BasicDBObject("name", 1)); 
		}

		// test if the customisation exists
		BasicDBObject where = new BasicDBObject();
		where.put("name", name);
		DBObject doc = collectionCustomisation.findOne(where);
		if (doc == null) {		   
			 message = "Resource was not found";
		}
		else {
			try {
				collectionCustomisation.remove(new BasicDBObject().append("name", name));
				message = "OK";
			}
			catch (MongoException e) {
				LOGGER.error(">> error deleteCustomisation: " + e.getMessage());
				e.printStackTrace();
				message = "Server error";
			}
		}*/
		return message;
	}
	
	/**
	 *  Remove all fields from the json object not corresponding to a customisation information.
	 */
	/*private void cleanJsonDoc(DBObject obj) {
		try {
			Set<String> fields = obj.keySet();
			List<String> toRemove = new ArrayList<String>();
			for(String field : fields) {
				if ( !( field.equals("name") || field.equals("description") || field.equals("wikipedia") ||
				field.equals("freebase") || field.equals("texts") || field.equals("_id") ) ) {
					toRemove.add(field);
				}
			}
			for(String field : toRemove) {
				obj.removeField(field);
			}
		}
		catch (MongoException e) {
			LOGGER.error(">> error cleaning Json oject cleanJsonDoc: " + e.getMessage());
			e.printStackTrace();
		}
	}*/
	
	/**
     *  A simple customisation JSON merge method - overwrite of the main document by the updating json 
	 *	if there is a conflict.
	 */
	/*private void merge(DBObject doc, DBObject updateDoc) {
		try {
			for (String field : updateDoc.keySet()) {
				if (field.equals("name")) {
					// ignore it
					continue;
				}
		
				if (!doc.containsField(field)) {
					doc.put(field, updateDoc.get(field));
					continue;
				}
				else if (field.equals("description")) {
					// overwrite
					doc.removeField(field);
					doc.put(field, updateDoc.get(field));
				}
				else if (field.equals("texts") || field.equals("wikipedia") || field.equals("freebase"))  {
					// we merge the lists
					BasicDBList oldArray = (BasicDBList)doc.get(field);
					BasicDBList newArray = (BasicDBList)updateDoc.get(field);
					Object[] newArr = newArray.toArray();
					for(int i=0; i<newArr.length; i++) {
						if (newArr[i] instanceof String) {
							if (!oldArray.contains((String)newArr[i]))
								oldArray.add((String)newArr[i]);
						}
						else if (newArr[i] instanceof Integer) {
							if (!oldArray.contains((Integer)newArr[i]))
								oldArray.add((Integer)newArr[i]);
						}
					}
				}
			}
		}
		catch (MongoException e) {
			e.printStackTrace();
		}
	}*/
	
}