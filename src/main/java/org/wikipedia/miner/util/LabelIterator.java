package org.wikipedia.miner.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.wikipedia.miner.db.WEntry;
import org.wikipedia.miner.db.WEnvironment;
import org.wikipedia.miner.db.WIterator;
import org.wikipedia.miner.db.struct.DbLabel;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.util.text.TextProcessor;

/**
 * @author David Milne
 *
 * Provides efficient iteration over the labels in Wikipedia
 */
public class LabelIterator implements Iterator<Label> {

    WEnvironment env;
    TextProcessor tp;
    WIterator<String, DbLabel> iter;

    Label nextLabel = null;

    /**
     * Creates an iterator that will loop through all pages in Wikipedia.
     *
     * @param database an active (connected) Wikipedia database.
     */
    public LabelIterator(WEnvironment env, TextProcessor tp) {

        this.env = env;
        this.tp = tp;
        iter = env.getDbLabel(tp).getIterator();

        queueNext();
    }

    public boolean hasNext() {
        return (nextLabel != null);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Label next() {

        if (nextLabel == null) {
            throw new NoSuchElementException();
        }

        Label l = nextLabel;
        queueNext();

        return l;
    }

    private void queueNext() {

        try {
            nextLabel = toLabel(iter.next());

        } catch (NoSuchElementException e) {
            nextLabel = null;
        }
    }

    private Label toLabel(WEntry<String, DbLabel> e) {
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
