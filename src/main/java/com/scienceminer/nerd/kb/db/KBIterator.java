package com.scienceminer.nerd.kb.db;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.Serializable;

import org.apache.log4j.Logger;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * Iterator for entries of an LMDB database.
 * 
 */
public class KBIterator {

	private KBDatabase database;
	private Database db = null;
	private EntryIterator iterator = null;
	private Env environment = null;
	private Transaction tx = null;

	public KBIterator(KBDatabase database) {	
		this.database = database;
		environment = database.getEnvironment();
		db = database.getDatabase();
		tx = environment.createReadTransaction();
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

	public Entry next()  {
		return iterator.next();
	}
}
