package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import org.apache.hadoop.record.CsvRecordInput;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.nio.ByteBuffer.allocateDirect;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.lmdbjava.PutFlags.MDB_APPEND;

public class ConceptDatabase extends StringRecordDatabase<Map<String, Integer>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConceptDatabase.class);

    public ConceptDatabase(KBEnvironment env) {
        super(env, DatabaseType.concepts);
    }

    public ConceptDatabase(KBEnvironment env, DatabaseType type) {
        super(env, type);
    }

    public void loadFromFile(File dataFile, boolean overwrite) throws IOException {
        if (isLoaded && !overwrite)
            return;
        if (dataFile == null)
            throw new NerdResourceException("Concept dump file not found. ");
        System.out.println("Loading " + name + " database");


        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
        //long bytesRead = 0;

        String line = null;
        int nbToAdd = 0;
        Txn<ByteBuffer> tx = environment.txnWrite();
        while ((line = input.readLine()) != null) {
            if (nbToAdd == 10000) {
                tx.commit();
                tx.close();
                nbToAdd = 0;
                tx = environment.txnWrite();
            }

            String[] pieces = line.split(",");
            if (pieces.length <= 1)
                continue;

            int pos = 0;
            String keyVal = pieces[pos];
            if (!startsWith(keyVal, "Q"))
                continue;
            pos++;
            Map<String, Integer> conceptMap = new HashMap<>();
            while (pos < pieces.length) {
                if (pieces[pos].equals("m{}")) {
                    pos++;
                    continue;
                }
                String lang = pieces[pos].replace("m{'", "").replace("'", "");
                pos++;
                if (pos == pieces.length)
                    break;
                String pageidString = pieces[pos].replace("'", "").replace("}", "");
                
                pos++;
                Integer pageid = null;
                try {
                    pageid = Integer.parseInt(pageidString);
                } catch (Exception e) {
                    LOGGER.warn("Cannot parse page id: " + pageidString);
                }
                if ((lang.trim().length() > 0) && (pageid != null))
                    conceptMap.put(lang, pageid);
            }
            KBEntry<String, Map<String, Integer>> entry = new KBEntry<>(keyVal, conceptMap);
            try {
                final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
                keyBuffer.put(KBEnvironment.serialize(entry.getKey())).flip();
                final byte[] serializedValue = KBEnvironment.serialize(entry.getValue());
                final ByteBuffer valBuffer = allocateDirect(serializedValue.length);
                valBuffer.put(serializedValue).flip();
                db.put(tx, keyBuffer, valBuffer);
                nbToAdd++;
            } catch (Exception e) {
                LOGGER.warn("While loading the concept database, there was a problem with the record  " + line, e);
            }
        }
        tx.commit();
        tx.close();
        input.close();
        isLoaded = true;
    }

    @Override
    public KBEntry<String, Map<String, Integer>> deserialiseCsvRecord(CsvRecordInput record) {
        throw new UnsupportedOperationException();
    }

}
