package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.*;

import java.util.zip.GZIPInputStream;
import java.util.List;

import org.apache.hadoop.record.CsvRecordInput;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;

import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.exceptions.NerdResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;


public class TypeDatabase extends StringIntDatabase {

    public TypeDatabase(KBEnvironment env, DatabaseType type) {
        super(env, type);
    }

    @Override
    public KBEntry<String, Integer> deserialiseCsvRecord(CsvRecordInput record)
        throws IOException {
        return null;
    }

    public void loadTypesFromFile(File dataFile, List<String> types, boolean overwrite) throws Exception  {
        if (isLoaded && !overwrite)
            return;
        System.out.println("Loading " + name + " database");

        if (dataFile == null)
            throw new NerdResourceException("Resource file not found");

        BufferedReader input;
        if (dataFile.getName().endsWith(".gz")) {
            InputStream fis = new FileInputStream(dataFile);
            InputStream gzipStream = new GZIPInputStream(fis);
            input = new BufferedReader(new InputStreamReader(gzipStream, "UTF-8"));
        } else {
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

            String[] pieces = line.split(" ");
            Integer typeIndex = null;
            int ind = types.indexOf(pieces[1]);
            if (ind != -1) 
                typeIndex = new Integer(ind);

            if (typeIndex == null)
                continue;
            
            KBEntry<String,Integer> entry = new KBEntry<String,Integer>(pieces[0],typeIndex);
            if (entry != null) {
                try {
                    db.put(tx, KBEnvironment.serialize(entry.getKey()), KBEnvironment.serialize(entry.getValue()));
                    nbToAdd++;
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        tx.commit();
        tx.close();
        input.close();
        isLoaded = true;
    }

}
