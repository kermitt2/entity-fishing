package com.scienceminer.nerd.kb.db;

import java.util.*;
import java.math.BigInteger;

import com.scienceminer.nerd.kb.model.hadoop.DbPage;

import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

public class PageIterator implements Iterator<Page> {
	private KBLowerEnvironment env = null;
	private KBIterator iter = null;
	private Page nextPage = null;
	private PageType type = null;

	public PageIterator(KBLowerEnvironment env) {
		this.env = env;
		iter = env.getDbPage().getIterator(); 
//System.out.println(env.getDbPage().getDatabaseSize());		
		queueNext();
	}

	public PageIterator(KBLowerEnvironment env, PageType type)  {
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
					DbPage pa = (DbPage)KBEnvironment.deserialize(valueData);
					
					//Integer keyId = new BigInteger(keyData).intValue();
					Integer keyId = (Integer)KBEnvironment.deserialize(keyData);
					
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
					e.printStackTrace();
				}
			}
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

