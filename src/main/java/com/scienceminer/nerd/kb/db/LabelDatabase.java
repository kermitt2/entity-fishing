package com.scienceminer.nerd.kb.db;

import java.io.*;
import java.util.*;

import org.apache.hadoop.record.CsvRecordInput;
import org.apache.hadoop.record.CsvRecordOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.model.hadoop.DbLabel;
import com.scienceminer.nerd.kb.model.hadoop.DbSenseForLabel;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.utilities.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * A {@link KBDatabase} for associating Strings with a record. 
 */
public class LabelDatabase extends StringRecordDatabase<DbLabel> {

	public LabelDatabase(KBEnvironment env) {
		super(env, DatabaseType.label);
	}

	/*@Override
	public DbLabel retrieve(String key) {
		return super.retrieve(key);
	}*/

	@Override
	public KBEntry<String,DbLabel> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
		String text = record.readString(null);
		DbLabel l = new DbLabel();
		l.deserialize(record);
		
		return new KBEntry<String,DbLabel>(text, l);
	}

	/**
	 * This method enrich the label database with the page titles (otherwise absent), updating 
	 * statistics and if necessary create new label/senses entries
	 */
	/*public void enrich(KBDatabase<String,Integer> dbArticlesByTitle) {
		// iterate on title pages
		KBIterator iterator = new KBIterator(dbArticlesByTitle);
		int nbToAdd = 0;
		Transaction tx = environment.createWriteTransaction();
		while(iterator.hasNext()) {
			// we have to maintain a map for these updates to ensure consistency 
			// of changes (except if nbToAdd is 1, but it will be slow as hell)
			if (nbToAdd == 1) {
				tx.commit();
				tx.close();
				nbToAdd = 0;
				tx = environment.createWriteTransaction();
			}

			Entry entry = iterator.next();
			String title = null;
			Integer pageId = null;

			byte[] keyData = entry.getKey();
	        byte[] valueData = entry.getValue();
	        try {
	            pageId = (Integer)KBEnvironment.deserialize(valueData);
	            title = (String)KBEnvironment.deserialize(keyData);
	        } catch(Exception e) {
	            e.printStackTrace();
	        }
			
			// does the title present as label in the LabelDB?
	        DbLabel label = retrieve(title);
	        if (label != null) {
	        	// update statistics for label and sense
	        	label.setLinkOccCount(label.getLinkOccCount()+1);
	        	label.setLinkDocCount(label.getLinkDocCount()+1); 
	        	label.setTextOccCount(label.getTextOccCount()+1);
	        	label.setTextDocCount(label.getTextDocCount()+1);  // unsure
	        	// do we have the pageId in the sense list for this label?
	        	boolean found = false;
	        	ArrayList<DbSenseForLabel> senses = label.getSenses();
	        	if (senses != null && senses.size() >0) {
		        	for(DbSenseForLabel sense : senses) {
		        		if (sense.getId() == pageId.intValue()) {
	    	    			// update sense info



	        				found = true;
	        				break;
	        			}
		        	}
		        }
	        	if (!found) {
	        		// we add a new sense
	        		DbSenseForLabel sense = new DbSenseForLabel(pageId.intValue(), 1, 1, true, false); 
	        		senses.add(sense);
	        		// update the senses
	        		label.setSenses(senses);
	        	}

	        } else {
	        	// we add a new label with associated sense
	        	label = new DbLabel();
	        	

	        	DbSenseForLabel sense = new DbSenseForLabel();


	        	ArrayList<DbSenseForLabel> senses = new ArrayList<DbSenseForLabel>();
	        	senses.add(sense);
	        	label.setSenses(senses);
	        }

	        // update/add label db, put will replace the key value if already present
	        //db.put(tx, KBEnvironment.serialize(title), KBEnvironment.serialize(label));
			nbToAdd++;
		}
		tx.commit();
		tx.close();
	}*/

}
