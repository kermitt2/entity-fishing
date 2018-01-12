package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.exceptions.CustomisationException;
import com.scienceminer.nerd.exceptions.NerdException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.scienceminer.nerd.disambiguation.NerdCustomisation.parseAndValidate;

/**
 * Class for managing the NERD customisation which are contexts for particular domains.
 */
public final class Customisations {

    protected static final Logger LOGGER = LoggerFactory.getLogger(Customisations.class);
    private static volatile Customisations instance;

    private String databaseName = "customisations";
    private File customisationFile;

    // map a customisation id to the json definition
    private ConcurrentMap<String, String> customisationDatabase = null;

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
        customisationFile = new File("data/maps/" + databaseName + ".obj");
    }

    /**
     * Open index for customisations
     */
    public void open() {
        ObjectInputStream in = null;
        try {

            if (customisationDatabase != null) {
                return;
            }

            if (customisationFile.exists() && Files.size(Paths.get(customisationFile.getAbsolutePath())) > 0) {
                FileInputStream fileIn = new FileInputStream(customisationFile);
                in = new ObjectInputStream(fileIn);
                customisationDatabase = (ConcurrentMap<String, String>) in.readObject();
                LOGGER.debug("Opening customisation database:  " + customisationFile.getAbsolutePath());
            } else if (customisationDatabase == null) {
                customisationDatabase = new ConcurrentHashMap<>();
                LOGGER.debug("Cannot find customisation database, creating a new one:  " + customisationFile.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new CustomisationException("Error when opening the customization map.", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Close and save index for customisations
     */
    public void save() {
        if (customisationDatabase == null) {
            return;
        }

        ObjectOutputStream out = null;
        try {
            customisationFile = new File("data/maps/" + databaseName + ".obj");

            LOGGER.debug("Persisting customisation database on: " + customisationFile.getAbsolutePath());
            FileOutputStream fileOut = new FileOutputStream(customisationFile);
            out = new ObjectOutputStream(fileOut);
            out.writeObject(customisationDatabase);
        } catch (IOException e) {
            throw new CustomisationException("Error when saving the customization map.", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public List<String> getCustomisations() throws CustomisationException {
        List<String> results = new ArrayList<>();
        try {
            open();
            results.addAll(customisationDatabase.keySet());
        } catch (Exception e) {
            throw new CustomisationException("Error when opening the customisation database. ", e);
        }
        return results;
    }

    public String getCustomisation(String name) {
        try {
            open();
            return customisationDatabase.get(name);
        } catch (Exception e) {
            throw new CustomisationException("Error when fetching the customisation " + name);
        }
    }

    public boolean createCustomisation(String name, String content) {
        parseAndValidate(content);
        open();

        if (customisationDatabase.get(name) == null) {
            customisationDatabase.put(name, content);
        } else {
            throw new CustomisationException("The customisation " + name + " has been already created.");
        }

        save();

        return true;
    }

    /**
     * Update the customisation only if it's already existing
     **/
    public boolean updateCustomisation(String name, String content) {
        parseAndValidate(content);
        try {
            open();

            if (customisationDatabase.get(name) == null) {
                return false;
            }
            customisationDatabase.put(name, content);

        } catch (Exception e) {
            throw new CustomisationException("Cannot update customisation", e);
        } finally {
            save();
        }
        return true;
    }

    /**
     * delete customisation, if it doesn't exists it's ignoring the request
     **/
    public boolean deleteCustomisation(String name) {
        boolean ok = false;
        try {
            open();
            ok = customisationDatabase.remove(name) != null;
        } catch (Exception e) {
            throw new CustomisationException("Cannot delete customisation", e);
        } finally {
            save();
        }

        return ok;
    }

    public void setCustomisationFile(File customisationFile) {
        this.customisationFile = customisationFile;
    }
}