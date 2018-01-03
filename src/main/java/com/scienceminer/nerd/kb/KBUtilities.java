package com.scienceminer.nerd.kb;

import java.io.*;
import java.util.*;
import org.apache.commons.collections4.CollectionUtils;

/**
 *  Various static methods for accessing efficiently bits of knowledge.
 */
public class KBUtilities {

	/** Taxon classification information are provided as present in Wikidata.
	 *  Various classification schemes exist and are still a matter of debate.
	 */

	/** Return true if the entity is part of the animal kingdom/regnum (Animalia) */
	public static boolean isAnimal(String wikidataId) {
//System.out.println(wikidataId);
		List<String> parents = UpperKnowledgeBase.getInstance().getFullParentTaxons(wikidataId);
//System.out.println(parents);
		if (CollectionUtils.isEmpty(parents))
			return false;
		return parents.contains("Q729");
	}

	/** Return true if the entity is part of the plant kingdom/regnum (Plantae) */
	public static boolean isPlant(String wikidataId) {
		List<String> parents = UpperKnowledgeBase.getInstance().getFullParentTaxons(wikidataId);
		if (CollectionUtils.isEmpty(parents))
			return false;
		return parents.contains("Q756");
	}

	/** Return true if the entity is part of the bacteria kingdom/regnum (Bacteria) */
	public static boolean isBacteria(String wikidataId) {
		List<String> parents = UpperKnowledgeBase.getInstance().getFullParentTaxons(wikidataId);
		if (CollectionUtils.isEmpty(parents))
			return false;
		return parents.contains("Q10876");
	}

	/** Return true if the entity is part of the single-celled microorganisms kingdom/regnum (Archaea) */
	public static boolean isArchaea(String wikidataId) {
		List<String> parents = UpperKnowledgeBase.getInstance().getFullParentTaxons(wikidataId);
		if (CollectionUtils.isEmpty(parents))
			return false;
		return parents.contains("Q10872");
	}

	/** Return true if the entity is part of the protozoa kingdom/regnum (Protozoa) */
	public static boolean isProtozoa(String wikidataId) {
		List<String> parents = UpperKnowledgeBase.getInstance().getFullParentTaxons(wikidataId);
		if (CollectionUtils.isEmpty(parents))
			return false;
		return parents.contains("Q101274");
	}

	/** Return true if the entity is part of the Fungus kingdom/regnum (Fungus) */
	public static boolean isFungus(String wikidataId) {
		List<String> parents = UpperKnowledgeBase.getInstance().getFullParentTaxons(wikidataId);
		if (CollectionUtils.isEmpty(parents))
			return false;
		return parents.contains("Q764");
	}

	/** Return true if the entity is part of the Chromista kingdom/regnum (Chromista) */
	public static boolean isChromista(String wikidataId) {
		List<String> parents = UpperKnowledgeBase.getInstance().getFullParentTaxons(wikidataId);
		if (CollectionUtils.isEmpty(parents))
			return false;
		return parents.contains("Q862296");
	}

}