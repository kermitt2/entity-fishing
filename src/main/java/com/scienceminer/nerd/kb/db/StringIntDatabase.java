package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import org.apache.hadoop.record.CsvRecordInput;
import org.lmdbjava.Cursor;
import org.lmdbjava.SeekOp;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;

import static com.scienceminer.nerd.kb.db.KBEnvironment.deserialize;
import static java.nio.ByteBuffer.allocateDirect;


public abstract class StringIntDatabase extends KBDatabase<String, Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringIntDatabase.class);

    public StringIntDatabase(KBEnvironment envi, DatabaseType type) {
        super(envi, type);
    }

    public StringIntDatabase(KBEnvironment envi, DatabaseType type, String name) {
        super(envi, type, name);
    }

    @Override
    public Integer retrieve(String key) {
        final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
        ByteBuffer cachedData = null;
        Integer record = null;
        try (Txn<ByteBuffer> tx = environment.txnRead()) {
            keyBuffer.put(KBEnvironment.serialize(key));
            cachedData = db.get(tx, keyBuffer);
            if (cachedData != null) {
                record = (Integer) KBEnvironment.deserialize(cachedData);
            }
        } catch (Exception e) {
            LOGGER.error("cannot retrieve " + key, e);
        }
        return record;
    }

    // using LMDB zero copy mode
    //@Override
    public Integer retrieve2(String key) {
        final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
        Integer record = null;
        try (Txn<ByteBuffer> tx = environment.txnRead();
             final Cursor<ByteBuffer> cursor = db.openCursor(tx)) {

            keyBuffer.put(KBEnvironment.serialize(key)).flip();
            if (cursor.seek(SeekOp.MDB_FIRST)) {
                record = (Integer) deserialize(cursor.val());
            }
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve key " + key, e);
        }
        return record;
    }

    public void loadFromFile(File dataFile, boolean overwrite) throws Exception {
        if (isLoaded && !overwrite)
            return;
        System.out.println("Loading " + name + " database");

        if (dataFile == null)
            throw new NerdResourceException("Resource file not found");

        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
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

            CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8")));
            KBEntry<String, Integer> entry = deserialiseCsvRecord(cri);

            if (entry != null) {
                try {
                    final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
                    keyBuffer.put(KBEnvironment.serialize(entry.getKey())).flip();
                    final byte[] serializedValue = KBEnvironment.serialize(entry.getValue());
                    final ByteBuffer valBuffer = allocateDirect(serializedValue.length);
                    valBuffer.put(serializedValue).flip();
                    db.put(tx, keyBuffer, valBuffer);
                    nbToAdd++;
                } catch (Exception e) {
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
