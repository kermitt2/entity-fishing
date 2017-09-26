package com.scienceminer.nerd.training;

import org.apache.commons.lang3.StringUtils;

import org.grobid.core.utilities.UnicodeUtil;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.kb.db.KBIterator;
import com.scienceminer.nerd.kb.db.KBEnvironment;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.utilities.mediaWiki.MediaWikiParser;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Generate entity description to be used for producing entity embeddings. 
 */
public class EntityDescription {
	private static final Logger LOGGER = LoggerFactory.getLogger(EntityDescription.class);

	private UpperKnowledgeBase upperKB;

	public EntityDescription() {
		upperKB = UpperKnowledgeBase.getInstance();
	}

	public void generateDescription(String path, String lang) {
System.out.println("generateDescription " + lang);
		BufferedWriter writer = null;
		KBIterator iter = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(path+"/description."+lang)));
			iter = upperKB.getEntityIterator();
			int n = 0;
			while(iter.hasNext()) {
				Entry entry = iter.next();
				byte[] keyData = entry.getKey();
				byte[] valueData = entry.getValue();
				//Page p = null;
				try {
					String entityId = (String)KBEnvironment.deserialize(keyData);
					Map<String,Integer> pagesIds = (Map<String,Integer>)KBEnvironment.deserialize(valueData);
					Integer pageId = pagesIds.get(lang);
					if (pageId != null) {
						// get the description summary of the entity
						Page page = upperKB.getWikipediaConf(lang).getPageById(pageId);
						String text = page.getFirstParagraphWikiText();
						text = normaliseDescription(text, lang);
						if (text.length() > 10) {
							writer.write(entityId + "\t" + text + "\n");
							if (n > 10000) {
								writer.flush();
								n = 0;
							}
						}
					}
					n++;
				} catch(Exception e) {
					LOGGER.error("fail to write entity description", e);
				}
			}
		} catch(IOException e) {
			LOGGER.error("Error when writing entity description", e);
 		} finally {
			if (iter != null)
				iter.close();
			if (writer != null) {
				try {
					writer.close();
				} catch(Exception e) {
					LOGGER.error("fail to close the entity description file", e);
				}	
			}
		}
	}

	/**
	 * Normalise wikimedia texts according to embedding training requirements which
	 * is a simple sequence of words.
	 */
	private static String normaliseDescription(String wikitext, String lang) {
		String text = MediaWikiParser.getInstance().toTextOnly(wikitext, lang);
		text = text.replace("\t", " ");

		// following fastText string normalisation: 
		// 1. All punctuation-and-numeric. Things in this bucket get
        // 	  their numbers flattened, to prevent combinatorial explosions.
        //    They might be specific numbers, prices, etc.
        //    -> all numerical chars are actually all transformed to '0'
    	// 2. All letters: case-flattened.
   	    // 3. Mixed letters and numbers: a product ID? Flatten case and leave
    	//    numbers alone.

		// unicode normalization
		text = UnicodeUtil.normaliseText(text);

		// wikipedia unicode encoding to Java encoding
		// <U+00AD> -> \u00AD
		//text.replaceAll();

    	// remove all xml scories
		text = text.replaceAll("<[^>]+>", " ");

		// remove all punctuation
		text = text.replaceAll("\\p{P}", " ");

		// flatten numerical chars
		text = text.replaceAll("\\d", "0");

		// lower case everything (to be evaluated!)
		text = text.toLowerCase();

		// collapse spaces
		text = StringUtils.normalizeSpace(text);

		return text;
	}


	public static void main(String args[]) throws Exception {
		File dataDir = new File(args[0]);
		String lang = args[1];
		
        if (!dataDir.exists()) {
            System.err.println("Path to the output data directory is not valid");
            System.exit(-1);
        }
        EntityDescription entityDescription = new EntityDescription();
        entityDescription.generateDescription(args[0], lang);
	}
}