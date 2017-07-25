package com.scienceminer.nerd.kb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

	public static Customisations getInstance() {
        if (instance == null) {
			getNewInstance();
        }
        return instance;
    }

	private static synchronized void getNewInstance() {
		LOGGER.debug("Get new instance of Customisation");		
		instance = new Customisations();
	}
	
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
 				cDB = new ConcurrentHashMap<>();
 			}
        } catch (Exception dbe) {
            throw new NerdException("Error when opening the customization map.", dbe);
        } finally {
			IOUtils.closeQuietly(in);
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
            throw new NerdException("Error when saving the customization map.", e);
		} finally {
			IOUtils.closeQuietly(out);
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
			LOGGER.debug("error, invalid retrieved DBObject.", e );
			message = "Server error";
		} 
		return message;
	}
	
	public String createCustomisation(String name, String profile) {
		String message = null;
		
		try {
			if (cDB == null) {
				open();
			}

			if (cDB.get(name) == null) {
                cDB.put(name, profile);
                message = "OK";
            } else {
				message = "Customisation already created.";
            }
		} catch (Exception e) {
            LOGGER.debug("Cannot create customisation.", e);
			message = "Server error";
        } finally {
        	save();
        }
		return message;
	}

	public String updateCustomisation(String name, String profile) {
		String message = null;
		try {
			if (cDB == null)
				open();

			if(cDB.get(name) == null) {
				message = "The Customisation " + name + "doesn't exists.";
			} else {
				cDB.put(name, profile);
				message = "OK";
			}

		} catch(Exception e) {
			LOGGER.debug("Cannot extend customisation.");
			message = "Server error";
		} finally {
			save();
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