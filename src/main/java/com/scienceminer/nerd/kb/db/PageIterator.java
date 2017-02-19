package com.scienceminer.nerd.kb.db;

import java.util.*;
import java.math.BigInteger;

import org.wikipedia.miner.db.struct.DbPage;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.model.Page.PageType;

import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class PageIterator implements Iterator<Page> {
	private KBEnvironment env = null;
	private KBIterator iter = null;

	private Page nextPage = null;
	private PageType type = null;

	/**
	 * Creates an iterator that will loop through all pages in Wikipedia.
	 * 
	 * @param database an active (connected) Wikipedia database.
	 */
	public PageIterator(KBEnvironment env) {
		this.env = env;
		iter = env.getDbPage().getIterator(); 
//System.out.println(env.getDbPage().getDatabaseSize());		
		queueNext();
	}

	/**
	 * Creates an iterator that will loop through all pages of the given type in Wikipedia.
	 * 
	 * @param database an active (connected) Wikipedia database.
	 * @param pageType the type of page to restrict the iterator to (ARTICLE, CATEGORY, REDIRECT or DISAMBIGUATION_PAGE)
	 * @throws SQLException if there is a problem with the Wikipedia database.
	 */
	public PageIterator(KBEnvironment env, PageType type)  {
		this.env = env;
		this.type = type;
		this.iter = this.env.getDbPage().getIterator(); 
//System.out.println(env.getDbPage().getDatabaseSize());	
		queueNext();
	}

	public boolean hasNext() {
		return (nextPage != null);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public Page next() {
		if (nextPage == null) 
			throw new NoSuchElementException();
		
		Page p = nextPage;
		queueNext();
		
		return p;

		/*Entry entry = iter.next();
		byte[] keyData = entry.getKey();
		byte[] valueData = entry.getValue();
		Page p = null;
		try {
			DbPage pa = (DbPage)Utilities.deserialize(valueData);
			Integer keyId = new BigInteger(keyData).intValue();
			p = toPage(new KBEntry<Integer,DbPage>(keyId, pa));
		} catch(Exception e) {
			//Logger.getLogger(PageIterator.class).error("Failed deserialize");
			e.printStackTrace();
		}
		return p;*/
	}

	private void queueNext() {
		try {
			nextPage = null;
			while(iter.hasNext()) {
				Entry entry = iter.next();
				byte[] keyData = entry.getKey();
				byte[] valueData = entry.getValue();
				//Page p = null;
				try {
					DbPage pa = (DbPage)Utilities.deserialize(valueData);
					
					Integer keyId = new BigInteger(keyData).intValue();
					nextPage = toPage(new KBEntry<Integer,DbPage>(keyId, pa));
					//PageType localType = PageType.values()[nextPage.getType()];
//System.out.println("Comparing : " + type + " / " + nextPage.getType());
					if ((type == null) || (nextPage.getType() == type)) {
//System.out.println("kept");
						break;
					} 
					/*else 
						System.out.println("skipped");*/
				} catch(Exception e) {
					//Logger.getLogger(PageIterator.class).error("Failed deserialize");
					e.printStackTrace();
				}
			}

			//nextPage = toPage(iter.next());

			/*if (type != null) {
				while (nextPage.getType() != type)
					nextPage = toPage(iter.next());
			}*/
		} catch (NoSuchElementException e) {
			nextPage = null;
		}
	}

	private Page toPage(KBEntry<Integer,DbPage> e) {
		if (e == null)
			return null;
		else
			return Page.createPage(env, e.getKey(), e.getValue());
	}
	
	public void close() {
		iter.close();
	}
}

