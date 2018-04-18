package com.scienceminer.nerd.kb.db;

import org.lmdbjava.CursorIterator;
import org.lmdbjava.Dbi;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;

import java.io.Closeable;
import java.nio.ByteBuffer;

/**
 * Iterator for entries of an LMDB database.
 */
public class KBIterator implements Closeable {

    private KBDatabase database;
    private Dbi<ByteBuffer> db = null;
    private CursorIterator<ByteBuffer> iterator = null;
    private Env<ByteBuffer> environment = null;
    private Txn<ByteBuffer> tx = null;

    public KBIterator(KBDatabase database) {
        this.database = database;
        environment = database.getEnvironment();
        db = database.getDatabase();
        tx = environment.txnRead();
        iterator = db.iterate(tx);
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void close() {
        if (iterator != null)
            iterator.close();
        if (tx != null)
            tx.close();
    }

    public CursorIterator.KeyVal<ByteBuffer> next() {
        return iterator.next();
    }
}
