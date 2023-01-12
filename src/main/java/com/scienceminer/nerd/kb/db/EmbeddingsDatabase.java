package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.compress.compressors.*;
import org.apache.hadoop.record.CsvRecordInput;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class EmbeddingsDatabase extends KBDatabase<String, short[]> {

    /**
     * Load an embedding database from an embeddings file in .vec format (text format).
     * It is typically either a word embedding database or an entity embeddings database. 
     * 
     * In case both word and entity embeddings are mixed in a single file, a boolean
     * mode indicates to distinguish word or entity objects - this is the case
     * for embeddings file produced by wikipedia2vec, where entity lines have a prefix
     * ENTITY/ 
     **/

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingsDatabase.class);  

    // a boolean mode must be indicated to distinguish word or entity objects
    // by default, we consider weord embeddings
    private boolean entityMode = false;

    public EmbeddingsDatabase(KBEnvironment env, DatabaseType type) {
        super(env, type);
        if (type == DatabaseType.entityEmbeddings) {
            this.entityMode = true;
        }
        else {
            this.entityMode = false;
        }
    }

    @Override
    public KBEntry<String, short[]> deserialiseCsvRecord(CsvRecordInput record) {
        throw new UnsupportedOperationException();
    }

    // using standard LMDB copy mode
    @Override
    public short[] retrieve(String key) {
        short[] record = null;
        try (Transaction tx = environment.createReadTransaction()) {
            byte[] cachedData = db.get(tx, KBEnvironment.serialize(key));
            if (cachedData != null) {
                record = (short[])KBEnvironment.deserialize(cachedData);
            }
        } catch(Exception e) {
            LOGGER.error("Word Embeddings Database: Cannot retrieve key " + key, e);
        }
        return record;
    }

    // using standard LMDB copy mode
    public float[] retrieveFloat(String key) {
        short[] record = null;
        try (Transaction tx = environment.createReadTransaction()) {
            byte[] cachedData = db.get(tx, KBEnvironment.serialize(key));
            if (cachedData != null) {
                record = (short[]) KBEnvironment.deserialize(cachedData);
            }
        } catch(Exception e) {
            LOGGER.error("Entity Embeddings Database: Cannot retrieve key " + key, e);
        }

        if (record == null)
            return null;

        // get the double typed vector
        float[] result = new float[record.length];
        for(int pos=0; pos<record.length; pos++) {
            result[pos] = (float)record[pos] / 10000.0f;
        }

        return result;
    }

    // using standard LMDB copy mode
    public double[] retrieveDouble(String key) {
        short[] record = null;
        try (Transaction tx = environment.createReadTransaction()) {
            byte[] cachedData = db.get(tx, KBEnvironment.serialize(key));
            if (cachedData != null) {
                record = (short[]) KBEnvironment.deserialize(cachedData);
            }
        } catch(Exception e) {
            LOGGER.error("Entity Embeddings Database: Cannot retrieve key " + key, e);
        }

        if (record == null)
            return null;
        
        // get the double typed vector
        double[] result = new double[record.length];
        for(int pos=0; pos<record.length; pos++) {
            result[pos] = (double)record[pos] / 10000.0;
        }

        return result;
    }

    @Override
    public void loadFromFile(File dataFile, boolean overwrite) throws Exception  {
        if (isLoaded && !overwrite)
            return;
        System.out.println("Loading " + name + " database");

        if (dataFile == null)
            throw new NerdResourceException("Embeddings file not found"); 

        BufferedReader input = null;
        if (dataFile.getName().endsWith(".bz2")) {
            FileInputStream fis = new FileInputStream(dataFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            CompressorInputStream reader = new CompressorStreamFactory().createCompressorInputStream(bis);
            input = new BufferedReader(new InputStreamReader(reader));
        } else if (dataFile.getName().endsWith(".gz")) {
            input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(dataFile)), "UTF-8"));
        } else {
            // assuming text file
            input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
        }

        String line = null;
        int nbToAdd = 0;
        Transaction tx = environment.createWriteTransaction();
        while ((line=input.readLine()) != null) {      
            if (nbToAdd == 10000) {
                tx.commit();
                tx.close();
                nbToAdd = 0;
                tx = environment.createWriteTransaction();
            }                  

            try {
                String[] pieces = line.split(" ");
                if (pieces.length == 2) {
                    // this is a header
                    continue;
                }

                if (entityMode && !line.startsWith("ENTITY/")) {
                    // word embeddings
                    continue;
                }

                if (!entityMode && line.startsWith("ENTITY/")) {
                    // entity embeddings
                    continue;
                }

                String keyVal = pieces[0];

                if (entityMode) {

                    if (line.startsWith("ENTITY/Catégorie:"))
                        continue;
                    // keyval should be the Wikidata entity Q identifier, so we need to retrieve it from 
                    // the wikipedia denormalized title page provided here in the format ENTITY/Page_title
                    String titlePage = keyVal.replace("ENTITY/", "");
                    titlePage = titlePage.replace("_" , " ");
                    titlePage = titlePage.replaceAll("( )+" , " ");
                    titlePage = titlePage.replace("ION [[" , "");
                    titlePage = titlePage.replace("ION[[" , "");
                    titlePage = titlePage.replace("Ion [[" , "");
                    titlePage = titlePage.replace("Ion[[" , "");
                    titlePage = titlePage.replace("]]" , "");
                    int pos = titlePage.indexOf(" [[");
                    if (pos != -1) {
                        titlePage = titlePage.substring(0,pos);
                    }
                    titlePage = titlePage.trim();
                    String qId = null;
                    
                    //Article article = this.env.getArticleByTitle(titlePage);
                    Article article = null;
                    Integer id = ((KBLowerEnvironment)this.env).getDbArticlesByTitle().retrieve(titlePage);
                    if (id == null) {
                        System.out.println("fail to lookup page title: " + titlePage);
                        continue;
                    }
                    Page page = Page.createPage(((KBLowerEnvironment)this.env), id);
                    if (page.exists()) {
                        if (page.getType() == PageType.redirect)
                            article = ((Redirect)page).getTarget();
                        else
                            article = (Article)page;
                    }

                    if (article != null) {
                        qId = article.getWikidataId();
                    }                    

                    if (qId == null) {
                        // the title page is not mapped to a valid Wikidata entity
                        LOGGER.warn("Fail to map the title page to a Wikidata entity: " + titlePage);
                        continue;
                    } else
                        keyVal = qId;
                }
                
                short[] vector = new short[pieces.length-1];
                for(int i=1; i<pieces.length; i++) {
                    try {
                        Double localValue = new Double(pieces[i]);
                        localValue = localValue * 10000;
                        short localShortValue = localValue.shortValue();
                        vector[i-1] = localShortValue;
                    } catch(Exception e) {
                        if (entityMode) 
                            LOGGER.warn("Entity embeddings: Cannot parse float value: " + pieces[i]);
                        else 
                            LOGGER.warn("Word embeddings: Cannot parse float value: " + pieces[i]);
                        vector[i-1] = 0;
                    }
                }
                KBEntry<String,short[]> entry = new KBEntry<>(keyVal, vector);
                db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
                nbToAdd++;

            } catch(Exception e) {
                LOGGER.error("Error parsing: " + line, e);
            }
        }
        tx.commit();
        tx.close();
        input.close();
        isLoaded = true;
    }
}
