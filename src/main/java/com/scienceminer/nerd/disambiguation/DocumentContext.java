package com.scienceminer.nerd.disambiguation;

import org.grobid.core.data.Entity;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.Pair;

import java.util.*;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.mention.Mention;

/**
 * This class represents a context in relation to a document. 
 * The document context is enriched with lexical and statistical information
 * applying to the document.
 *
 */
public class DocumentContext extends NerdContext {
	
	private static int MAX_SENSES = 10;

	// strictly equivalent mentions (e.g. acronyms) valid for the document only
	private Map<Entity, Entity> localMentions = null; 

	// local sense statistics for document-level reimforcement
	private Map<String, Pair<NerdEntity, Integer>> entityCount = null;

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
		Map<String, LowerKnowledgeBase> wikipedias = UpperKnowledgeBase.getInstance().getWikipediaConfs();
		LowerKnowledgeBase wikipedia = wikipedias.get(scopeLang.getLang());
		for(NerdEntity entity : entities) {
			if (nb == MAX_SENSES)
				break;
			if (contextArticlesIds.contains(entity.getWikipediaExternalRef()))
				continue;
			Page page = wikipedia.getPageById(entity.getWikipediaExternalRef());
			// conservative type checking
			PageType pageType = page.getType();
			if (pageType != PageType.article) {
				// something should be logged here...
				continue;
			}

			Article article = (Article)page;
			article.setWeight(entity.getSelectionScore());
			contextArticles.add(article);
			contextArticlesIds.add(entity.getWikipediaExternalRef());
			nb++;
		}
	}

	public void setLocalMentions(Map<Entity, Entity> mentions) {
		this.localMentions = mentions;
	}

	public Map<Entity, Entity> getLocalMentions() {
		return this.localMentions;
	}

	public void putLocalMention(Entity variant, Entity base) {
		if (this.localMentions == null)
			localMentions = new HashMap<Entity, Entity>();
		localMentions.put(variant, base);	
	}

	public void setEntityCount(Map<String, Pair<NerdEntity, Integer>> counts) {
		this.entityCount = counts;
	}

	public Map<String, Pair<NerdEntity, Integer>> getEntityCount() {
		return this.entityCount;
	}

	public Pair<NerdEntity, Integer> getEntityCount(String surface) {
		if (this.entityCount != null)
			return this.entityCount.get(surface);
		else 
			return null;
	}

	public void addEntityCount(String surface, NerdEntity entity) {
		if (this.entityCount == null)
			entityCount = new HashMap<String, Pair<NerdEntity, Integer>>();
		if (entityCount.get(surface) == null) {
			Pair<NerdEntity, Integer> thePair = new Pair<NerdEntity, Integer>(entity, 1);
			entityCount.put(surface, thePair);
		} else {
			Pair<NerdEntity, Integer> thePair = entityCount.get(surface);
			Pair<NerdEntity, Integer> newPair = new Pair<NerdEntity, Integer>(thePair.getA(), thePair.getB() + 1);
			entityCount.replace(surface, newPair);
		}
	}

	/**
	 * Update the document content with results present in a given NerdQuery. This NerdQuery must be
	 * fully disambiguated and processed.    
	 */
	public void update(NerdQuery nerdQuery) {
		// update entity counts
		List<NerdEntity> entities = nerdQuery.getEntities();
		for(NerdEntity entity : entities) {
			addEntityCount(entity.getRawName(), entity);
		}

		// update acronyms
		if (nerdQuery.getContext() != null) {
			Map<Mention,Mention> localAcronyms = nerdQuery.getContext().getAcronyms();
			if (localAcronyms != null) {
				for (Map.Entry<Mention, Mention> entry : localAcronyms.entrySet()) {
	        		Mention base = entry.getValue();
	        		Mention acronym = entry.getKey();

	        		if (acronyms == null)
	        			acronyms = new HashMap<Mention, Mention>();

	        		if (acronyms.get(acronym) == null) {
	        			acronyms.put(acronym, base);
	        		}
	        	}
	        }
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