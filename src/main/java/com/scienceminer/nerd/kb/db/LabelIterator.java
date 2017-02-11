package com.scienceminer.nerd.kb.db;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.math.BigInteger;

import org.wikipedia.miner.db.struct.DbLabel;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.util.text.TextProcessor;

import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class LabelIterator implements Iterator<Label> {

    private KBEnvironment env = null;
    private TextProcessor tp = null;
    private KBIterator iter = null;

    /**
     * Creates an iterator that will loop through all pages in Wikipedia.
     *
     * @param database an active (connected) Wikipedia database.
     */
    public LabelIterator(KBEnvironment env, TextProcessor tp) {
        this.env = env;
        this.tp = tp;
        iter = env.getDbLabel(tp).getIterator();
    }

    public boolean hasNext() {
        return iter.hasNext();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Label next() {
        Entry entry = iter.next();
        byte[] keyData = entry.getKey();
        byte[] valueData = entry.getValue();
        Label l = null;
        try {
            DbLabel la = (DbLabel)Utilities.deserialize(valueData);
            String keyId = string(keyData);
            l = toLabel(new KBEntry<String, DbLabel>(keyId, la));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return l;
    }

    private Label toLabel(KBEntry<String, DbLabel> e) {
        if (e == null) {
            return null;
        } else {
            return Label.createLabel(env, e.getKey(), e.getValue(), tp);
        }
    }

    public void close() {
        iter.close();
    }
}
