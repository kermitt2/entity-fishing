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

public abstract class IntLongDatabase extends KBDatabase<Integer, Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntLongDatabase.class);

    public IntLongDatabase(KBEnvironment envi, DatabaseType type) {
        super(envi, type);
    }

    public IntLongDatabase(KBEnvironment envi, DatabaseType type, String name) {
        super(envi, type, name);
    }

    // using standard LMDB copy mode
    @Override
    public Long retrieve(Integer key) {
        final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
        ByteBuffer cachedData = null;
        Long record = null;
        try (Txn<ByteBuffer> tx = environment.txnRead()) {
            keyBuffer.put(KBEnvironment.serialize(key));
            cachedData = db.get(tx, keyBuffer);
            if (cachedData != null) {
                record = (Long) KBEnvironment.deserialize(cachedData);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve key " + key, e);
        }
        return record;
    }

    // using LMDB zero copy mode
    //@Override
    public Long retrieve2(Integer key) {
        final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
        Long record = null;
        try (Txn<ByteBuffer> tx = environment.txnRead();
             final Cursor<ByteBuffer> cursor = db.openCursor(tx)) {

            keyBuffer.put(KBEnvironment.serialize(key)).flip();
            if (cursor.seek(SeekOp.MDB_FIRST)) {
                record = (Long) deserialize(cursor.val());
            }
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve key " + key, e);
        }
        return record;
    }

    public void loadFromFile(File dataFile, boolean overwrite) throws Exception {
        if (isLoaded && !overwrite)
            return;

        if (dataFile == null)
            throw new NerdResourceException("Resource file not found");

        System.out.println("Loading " + name + " database");

        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
        long bytesRead = 0;

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
            bytesRead = bytesRead + line.length() + 1;

            CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream((line + "\n").getBytes("UTF-8")));
            KBEntry<Integer, Long> entry = deserialiseCsvRecord(cri);
            if (entry != null) {
                try {
                    final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
                    keyBuffer.put(KBEnvironment.serialize(entry.getKey()));
                    final byte[] serializedValue = KBEnvironment.serialize(entry.getValue());
                    final ByteBuffer valBuffer = allocateDirect(serializedValue.length);
                    valBuffer.put(serializedValue);
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
