package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.kb.Category;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*; 

import com.scienceminer.nerd.kb.model.*;

/**
 * Exploit disambiguation for processing categories, e.g. compute a category distribution 
 * over a set of entities. 
 * 
 *
 */
public class NerdCategories {
	
	/**
	 * The class Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(NerdCategories.class);	
	
	// this is the threshold to keep a category at document level
	static private double DOC_CATEGORY_THRESHOLD = 0.05;
	
	/**
	 * Use category information present in the disambiguated entities of a weighted vector 
	 * to produce a global category distribution.
	 */
	static public NerdQuery addCategoryDistributionWeightedTermVector(NerdQuery query) {

		// map wikipediaIds to the catogory objects
		Map<Integer, Category> categoryMap = new HashMap<Integer, Category>();

		if (query.getTermVector() == null) {
			logger.debug("Category distribution for Weighted term vector: " + 
				"NerdQuery does not contain a termVector object ");
			return query;
		}

		for(WeightedTerm term : query.getTermVector()) {
			List<NerdEntity> entities = term.getNerdEntities();
			if (entities != null) {
				Double weight = term.getScore();
				if (weight == 0.0)
					continue;
				for(NerdEntity entity : entities) {
					List<com.scienceminer.nerd.kb.Category> categories = entity.getCategories();
					if (categories != null) {				
						Double nerd_score = entity.getNerdScore();
						if (nerd_score == 0.0)
							continue;
						for(Category category : categories) {
							int wikipediaID = category.getWikiPageID();
							Integer wikipediaInteger = new Integer(wikipediaID);

							if (categoryMap.get(wikipediaInteger) == null) {
								Category categoryBis = new Category(category.getName(), 
									category.getWikiCategory(), 
									category.getWikiPageID());
								categoryBis.setWeight(nerd_score*weight);
								categoryMap.put(wikipediaInteger, categoryBis);
							}
							else {
								Category categoryBis = categoryMap.get(wikipediaInteger);
								double localWeight = categoryBis.getWeight();
								categoryBis.setWeight(localWeight + (nerd_score*weight));
								categoryMap.put(wikipediaInteger, categoryBis);
							}
						}
					}
				}
			}
		}
		// build the category list based on the map 
		// first path to get the weight range 
		double accumulatedWeight = 0.0;
		for (Map.Entry<Integer, Category> entry : categoryMap.entrySet()) {
			// entry.getKey();
            Category categ = entry.getValue();
			accumulatedWeight += categ.getWeight();
		}

		if (accumulatedWeight == 0.0)
			return query;

		// normalize, apply a threashold and select the categories
        for (Map.Entry<Integer, Category> entry : categoryMap.entrySet()) {
            Category categ = entry.getValue();
			categ.setWeight(categ.getWeight() / accumulatedWeight);
			if ( (categ.getWeight() >= DOC_CATEGORY_THRESHOLD) || 
				 (query.getGlobalCategories() == null) ||
				 (query.getGlobalCategories().size() < 3) ) {
				query.addGlobalCategory(categ);
			}
		}
		
		return query;
	}
	
	/**
	 * Use category information present in the disambiguated entities of a text 
	 * to produce a global category distribution.
	 */
	static public NerdQuery addCategoryDistribution(NerdQuery query) {
		// map wikipediaIds to the catogory objects
		Map<Integer, Category> categoryMap = new HashMap<Integer, Category>();

		if (query.getEntities() == null) {
			logger.debug("Category distribution for Weighted term vector: " + 
				"NerdQuery does not contain a termVector object ");
			return query;
		}

		List<NerdEntity> entities = query.getEntities();
		if (entities != null) {
			for(NerdEntity entity : entities) {
				List<com.scienceminer.nerd.kb.Category> categories = entity.getCategories();
				if (categories != null) {				
					Double nerd_score = entity.getNerdScore();
					if (nerd_score == 0.0)
							continue;
					for(Category category : categories) {
						int wikipediaID = category.getWikiPageID();
						Integer wikipediaInteger = new Integer(wikipediaID);

						if (categoryMap.get(wikipediaInteger) == null) {
							Category categoryBis = new Category(category.getName(), 
								category.getWikiCategory(), 
								category.getWikiPageID());
							categoryBis.setWeight(nerd_score);
							categoryMap.put(wikipediaInteger, categoryBis);
						}
						else {
							Category categoryBis = categoryMap.get(wikipediaInteger);
							double localWeight = categoryBis.getWeight();
							categoryBis.setWeight(localWeight + nerd_score);
							categoryMap.put(wikipediaInteger, categoryBis);
						}
					}
				}
			}
		}
		
		// build the category list based on the map 
		// first path to get the weight range 
		double accumulatedWeight = 0.0;
		for (Map.Entry<Integer, Category> entry : categoryMap.entrySet()) {
			// entry.getKey();
            Category categ = entry.getValue();
			accumulatedWeight += categ.getWeight();
		}

		if (accumulatedWeight == 0.0)
			return query;

		// normalize, apply a threashold and select the categories
        for (Map.Entry<Integer, Category> entry : categoryMap.entrySet()) {
            Category categ = entry.getValue();
			categ.setWeight(categ.getWeight() / accumulatedWeight);
			if ( (categ.getWeight() >= DOC_CATEGORY_THRESHOLD) || 
				 (query.getGlobalCategories() == null) ||
				 (query.getGlobalCategories().size() < 3) ) {
				query.addGlobalCategory(categ);
			}
		}
		
		return query;
	}

	private static List<String> categoryFilter = Arrays.asList("article", "disambiguation", "pilot", "list of", "beadwork", "births");

	public static boolean categoryToBefiltered(String category) {
		if (category != null) {
			String cat = category.toLowerCase();
			for (int i=0; i < categoryFilter.size(); i++) {
				if (cat.indexOf(categoryFilter.get(i)) != -1)
					return true;
			}
			return false;
		}
		return true;
	}
	
}