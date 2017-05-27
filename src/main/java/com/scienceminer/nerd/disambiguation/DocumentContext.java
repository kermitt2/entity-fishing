package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.grobid.core.data.Entity;
import org.grobid.core.lang.Language;

import java.util.*; 
import java.text.*;
import java.text.*;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.model.*;

import com.fasterxml.jackson.core.io.*;

/**
 * This class represents a context in relation to a document. 
 * 
 * @author Patrice Lopez
 *
 */
public class DocumentContext extends NerdContext {
	
	private static int MAX_SENSES = 10;

	// equivalent mentions (e.g. acronyms) valid for the document only
	private TreeMap<String, String> localMentions = null;

	// local sense statistics
	// .. 

	public DocumentContext() {
		super();
	}

	public void seed(List<NerdEntity> entities, Language scopeLang) {
		if ( (entities == null) || (entities.size() == 0) )
			return;

		// sort entities by selection score
		Collections.sort(entities, new Comparator<NerdEntity>() {
    		public int compare(NerdEntity e1, NerdEntity e2) {
    			if (e1.getSelectionScore() > e2.getSelectionScore())
          			return -1;
			    else if (e1.getSelectionScore() < e2.getSelectionScore())
          			return 1;
    			return 0;
    		}
		}); 

		if (contextArticles == null) {
			contextArticles = new ArrayList<Article>();
			contextArticlesIds = new ArrayList<Integer>(); 
		}

		int nb = 0;
		Map<String, Wikipedia> wikipedias = Lexicon.getInstance().getWikipediaConfs();
		Wikipedia wikipedia = wikipedias.get(scopeLang.getLang());
		for(NerdEntity entity : entities) {
			if (nb == MAX_SENSES)
				break;
			if (contextArticlesIds.contains(entity.getWikipediaExternalRef()))
				continue;
			Article article = (Article)wikipedia.getPageById(entity.getWikipediaExternalRef());
			article.setWeight(entity.getSelectionScore());
			contextArticles.add(article);
			contextArticlesIds.add(entity.getWikipediaExternalRef());
			nb++;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(Article article : contextArticles) {
			builder.append(article.getTitle() + "\t" + article.getId() + "\t" + article.getWeight() + "\n");
		}
		return builder.toString();
	}
}