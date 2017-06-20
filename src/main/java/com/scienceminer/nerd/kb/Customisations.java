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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

import com.scienceminer.nerd.kb.model.*;

/**
 * Class for managing the NERD customisation which are contexts for particular domains.
 *
 */
public final class Customisations {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(Customisations.class);
	private static volatile Customisations instance;
	private ObjectMapper mapper = new ObjectMapper();

	private String database_path = null;
    private String database_name = "customisations";

    // map a customisation id to the json definition
    private ConcurrentMap<String, String> cDB = null;

	public static Customisations getInstance() throws Exception {
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
		instance = new Customisations();
	}
	
	/**
     * Hidden constructor
     */
    public Customisations() {		
	}

	/**
     * Open index for customisations
     */
    public void open() {
        File home = null;
        ObjectInputStream in = null;
        try {
            home = new File(NerdProperties.getInstance().getMapsPath() + "/" + database_name + ".obj");
        } catch (Exception e) {
            throw new NerdException(e);
        }
        try {
        	if (home.exists()) {
        		FileInputStream fileIn = new FileInputStream(home);
         	 	in = new ObjectInputStream(fileIn);
	 			cDB = (ConcurrentMap<String, String>)in.readObject();
	 		} else if (cDB == null) {
 				cDB = new ConcurrentHashMap<String, String>();
 			}
        } catch (Exception dbe) {
            LOGGER.debug("Error when opening the customization map.");
            throw new NerdException(dbe);
        } finally {
        	try {
	        	if (in != null)
		        	in.close();
		    } catch(IOException e) {
		    	LOGGER.debug("Error when closing the customization map.");
            	throw new NerdException(e);
		    }
        }
    }
	
    /**
     * Close index for customisations
     */
    public void save() {
    	if (cDB == null)
    		return;
    	File home = null;
        ObjectOutputStream out = null;
        try {
            home = new File(NerdProperties.getInstance().getMapsPath() + "/" + database_name + ".obj");
        } catch (Exception e) {
            throw new NerdException(e);
        }
    	try {
	    	if (home != null) {
        		FileOutputStream fileOut = new FileOutputStream(home);
		        out = new ObjectOutputStream(fileOut);
		    	out.writeObject(cDB);
		    }
		} catch(IOException e) {
			LOGGER.debug("Error when saving the customization map.");
            throw new NerdException(e);
		} finally {
			try {
				if (out != null)
	        		out.close();
	        } catch(IOException e) {
		    	LOGGER.debug("Error when closing the customization map.");
            	throw new NerdException(e);
		    }
		}
    }

	public List<String> getCustomisations() {
		List<String> results = null;
		try {
			if (cDB == null)
				open();
			results = new ArrayList<String>();
			for(String key : cDB.keySet()) {
				results.add(key);
			}
		} catch(Exception e) {
			LOGGER.debug("Error when opening MapDB.");
            throw new NerdException(e);
		} 
		return results;
	}
	
	public String getCustomisation(String name) {
		String message = null;
		try {
			if (cDB == null)
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
		} 
		return message;
	}
	
	public String createCustomisation(String name, String profile) {
		String message = null;
		
		try {
			if (cDB == null)
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
        	save();
        }
		return message;
	}

	public String extendCustomisation(String name, String profile) {
		String message = null;
		try {
			if (cDB == null)
				open();
		}
		catch(Exception e) {
			LOGGER.debug("Cannot extend customisation.");
			e.printStackTrace();
			message = "Server error";
		} finally {
			//save();
		}
		return message;
	}
	
	public String deleteCustomisation(String name) {
		String message = null;
		try {
			if (cDB == null)
				open();
            // Delete the record(s) for the given key
            cDB.remove(name);
            message = "OK";
        } catch (Exception e) {
            throw new NerdException(e);
        } finally {
			save();
		}

		return message;
	}
	
}