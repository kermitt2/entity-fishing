package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.kb.model.Label;
import com.scienceminer.nerd.kb.model.hadoop.DbLabel;
import org.lmdbjava.CursorIterator;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class LabelIterator implements Iterator<Label> {

    private KBLowerEnvironment env = null;
    private KBIterator iter = null;

    public LabelIterator(KBLowerEnvironment env) {
        this.env = env;
        iter = env.getDbLabel().getIterator();
    }

    public boolean hasNext() {
        return iter.hasNext();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Label next() {
        CursorIterator.KeyVal<ByteBuffer> entry = iter.next();
        ByteBuffer keyData = entry.key();
        ByteBuffer valueData = entry.val();
        Label l = null;
        try {
            DbLabel la = (DbLabel) KBEnvironment.deserialize(valueData);
            String keyId = (String) KBEnvironment.deserialize(keyData);
            l = toLabel(new KBEntry<>(keyId, la));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return l;
    }

    private Label toLabel(KBEntry<String, DbLabel> e) {
        if (e == null) {
            return null;
        } else {
            return Label.createLabel(env, e.getKey(), e.getValue());
        }
    }

    public void close() {
        iter.close();
    }
}
