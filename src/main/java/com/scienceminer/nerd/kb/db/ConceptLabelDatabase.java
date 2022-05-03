package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import org.apache.hadoop.record.CsvRecordInput;
import org.fusesource.lmdbjni.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ConceptLabelDatabase extends StringRecordDatabase<Map<String,String>> {
    private static final Logger logger = LoggerFactory.getLogger(ConceptLabelDatabase.class);

    private int nbTotalAdded = 0;
    private int nbToAdd = 0;
    private Transaction tx = null;

    public ConceptLabelDatabase(KBEnvironment env) {
        super(env, DatabaseType.concepts);
    }

    public ConceptLabelDatabase(KBEnvironment env, DatabaseType type) {
        super(env, type);
    }

    public void startFillDatabase() {
        nbToAdd = 0;
        nbTotalAdded = 0;
        tx = environment.createWriteTransaction();
    }

    public void fillDatabase(String itemId, Map<String,String> labelsPerLang) {
        if (nbToAdd >= 10000) {
            try {
                tx.commit();
                tx.close();
                nbToAdd = 0;
                tx = environment.createWriteTransaction();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        try {
            db.put(tx, KBEnvironment.serialize(itemId), KBEnvironment.serialize(labelsPerLang));
            nbToAdd++;
            nbTotalAdded++;
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void completeDatabase() {
        // last commit
        tx.commit();
        tx.close();
        isLoaded = true;
        System.out.println("Concept labels database: Total of " + nbTotalAdded + " label mapping indexed");
    } 

    @Override
    public KBEntry<String,Map<String,String>> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
        throw new UnsupportedOperationException();
    }

}
 