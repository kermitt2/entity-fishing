package com.scienceminer.nerd.disambiguation;

import org.grobid.core.data.Entity;

import com.scienceminer.nerd.exceptions.NerdException;

import org.grobid.core.utilities.OffsetPosition;
import com.scienceminer.nerd.kb.Definition;
import com.scienceminer.nerd.kb.Domains;
import com.scienceminer.nerd.kb.Variant;
import com.scienceminer.nerd.kb.Category;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;

import java.util.List;    
import java.util.ArrayList;

import com.scienceminer.nerd.kb.model.*;

import com.fasterxml.jackson.core.io.*;

/**
 * This class represents a candidate for disambiguated entity, including conceptual and 
 * encyclopedic information, with all the information necessary for a disambiguation. 
 * 
 *
 */
public class NerdCandidate implements Comparable<NerdCandidate> {

	// entity associated to the present disambiguated entity candidate
	private NerdEntity entity = null;

	// true if the textual content corresponding to this element is not continuous
	// this should be rather very exceptional (e.g. particular biotech entities)
	private boolean isComplexSegment = false;
	
	// descriptions/definitions of the related sense/concept/entity
	private List<Definition> definitions = null;

	// tokenized lexical string of the element
	private List<String> tokens = null;
	
	// note: if the element corresponds to an entity, prob_coarse_confidence = prob_fine_confidence

	// language of the token
	private String lang = null;

	// Stems of the tokens
	private List<String> stems = null;

	// list of morphological variants for the term
	private List<String> morphoVariants = null;
	
	// list of conceptual variants for the term
	private List<Variant> variants = null;

	// Sense from Wikipedia Miner database corresponding to this disambiguation candidate 
	private Label.Sense wikiSense = null;
	
	// The Wikipedia label corresponding to the candidate
	private Label label = null;

	// Wikipedia page ID
	private int wikipediaExternalRef = -1;
	
	// Wiktionary page ID
	private int wiktionaryExternalRef = -1;
	
	// Wikidata identifier
	private String wikidataId = null;

	// preferred term for the corresponding sense
	private String preferredTerm = null;
	
	// link to FreeBase topic ID
	private String freeBaseExternalRef = null;

	// domain information
	private List<String> domains = null;

	// domain information extended following parent child relationship
	private List<String> expandedDomains = null;

	// freebase types
	private List<String> freebaseTypes = null; // full types
	private List<String> freebaseHighTypes = null; // first level types, e.g. /location

	// method used to produce the term candidate
	public static final int UNKNOWN = 0;
	public static final int WSD = 1;
	public static final int KB = 2;
	public static final int NER = 3;
	public static final int NERD = 4;
	public static final int COMBINED = 5;
	
	public boolean isSubTerm = false;
	
	private int method = UNKNOWN;

	// to carry statistics/priors
	// conditional probability of the concept given the string
	private double prob_c = 0.0;
	// conditional probability of the string given the concept (i.e. reverse prob_c)
	private double prob_i = 0.0;
	// frequency of usage of the string to refer to the concept in a reference corpus (usually wikipedia)
	// among all possible concepts it can realise
	private int freq = 0;
	// frequency of usage of the string to refer to the concept in a reference corpus (usually wikipedia)
	// among all the strings which can realise this particular concept
	private int freq_i = 0;
	// represent named entity disambiguation score in a NERD scenario
	private double nerdScore = 0.0;
	// represent named entity selection score in a NERD scenario
	private double selectionScore = 0.0;
	// relatedness score of the term with the context
	private double relatednessScore = 0.0;

	// list of wikipedia categories corresponding to the disambiguated term
	private List<com.scienceminer.nerd.kb.Category> wikipediaCategories = null;

	// if true, the candidate has been defined for a coreference mention from
	// the candidates originally from the first reference
	private boolean coReference = false;

	// if the actual string (with its case) matches exactly the selected label of the candidate
	private boolean bestCaseContext = true;

	public NerdCandidate(NerdEntity entity) {
		this.entity = entity;
	}
	
	public NerdEntity getEntity() {
		return entity;
	}
	
	public void setEntity(NerdEntity entity) {
		this.entity = entity;
	}

	public boolean isComplexSegment() {
		return isComplexSegment;
	}

	public void setComplexSegment(boolean complex) {
		this.isComplexSegment = complex;
	}
	
	public List<String> getFreebaseTypes() {
		return freebaseTypes;
	}
	
	public void setFreebaseTypes(List<String> types) {
		freebaseTypes = types;
	}
	
	public List<String> getFreebaseHighTypes() {
		return freebaseHighTypes;
	}
	
	public void setFreebaseHighTypes(List<String> types) {
		freebaseHighTypes = types;
	}
	
	public void addFreebaseType(String type) {
		if (freebaseTypes == null)
			freebaseTypes = new ArrayList<String>();
		if (!freebaseTypes.contains(type))
			freebaseTypes.add(type);
	}
	
	public List<Variant> getVariants() {
		return variants;
	}
	
	public void setVariants(List<Variant> variants) {
		this.variants = variants;
	}
	
	public void addVariant(Variant variant) {
		if (variants == null) {
			variants = new ArrayList<Variant>();
		}
		if (!variants.contains(variant)) {
			variants.add(variant);
		}
	}
	
	public Label.Sense getWikiSense() {
		return wikiSense;
	}
	
	public void setWikiSense(Label.Sense sense) {
		this.wikiSense = sense;
	}

	public void setLabel(Label label) {
		this.label = label;
	}

	public Label getLabel() {
		return label;
	}
	
	public List<String> getDomains() {
		return domains;
	} 

	public void setDomains(List<String> domains) {
		this.domains = domains;
	}

	public void addDomain(String domain) {
		if (domains == null)
			domains = new ArrayList<String>();
		else {
			if (domains.contains(domain.toLowerCase())) {
				return;
			}
		}
		
		domains.add(domain);
	}
	
	public boolean getBestCaseContext() {
		return bestCaseContext;
	}

	public void setBestCaseContext(boolean bestCase) {
		this.bestCaseContext = bestCase;
	}

	public List<String> getExpandedDomains() {
		return expandedDomains;
	}

	public void setExpandedDomains(List<String> expandedDomains) {
		this.expandedDomains = expandedDomains;
	}

	public void addExpandedDomain(String expandedDomain) {
		if (expandedDomains == null)
			expandedDomains = new ArrayList<String>();
		else {
			if (expandedDomains.contains(expandedDomain.toLowerCase())) {
				return;
			}
		}
		expandedDomains.add(expandedDomain);
	}
	
	public int getWikipediaExternalRef() {
		return wikipediaExternalRef;
	}
	
	public void setWikipediaExternalRef(int ref) {
        this.wikipediaExternalRef = ref;
    }

    public String getWikidataId() {
		return wikidataId;
	}

	public void setWikidataId(String ref) {
        this.wikidataId = ref;
    }

	public List<com.scienceminer.nerd.kb.Category> getWikipediaCategories() {
		return wikipediaCategories;
	}
	
	public void setWikipediaCategories(List<com.scienceminer.nerd.kb.Category> categories) {
		wikipediaCategories = categories;
	}
	
	public void addWikipediaCategories(com.scienceminer.nerd.kb.Category category) {
		if (wikipediaCategories == null)
			wikipediaCategories = new ArrayList<com.scienceminer.nerd.kb.Category>();
		wikipediaCategories.add(category);
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getPreferredTerm() {
		return preferredTerm;
	}
	
	public void setPreferredTerm(String term) {
		preferredTerm = term;
	}

	public int getWiktionaryExternalRef() {
		return wiktionaryExternalRef;
	}
	
	public void setWiktionaryExternalRef(int ref) {
        this.wiktionaryExternalRef = ref;
    }

	public String getFreeBaseExternalRef() {
		return freeBaseExternalRef;
	}

	public void setFreeBaseExternalRef(String freebase) {
		freeBaseExternalRef = freebase;
	}

	public List<Definition> getDefinitions() {
		return definitions;
	}
	
	public void setDefinitions(List<Definition> desc) {
		definitions = desc;
	}
	
	public void addDefinition(Definition desc) {
		if (definitions == null)
		 	definitions = new ArrayList<Definition>();
		definitions.add(desc);
	}

	public void setMethod(int method) {
		this.method = method;
	}

	public int getMethod() {
		return method;
	}
	
	public double getProb_c() {
		return this.prob_c;
	}
	
	public void setProb_c(double p) {
		this.prob_c = p;
	}
	
	public double getProb_i() {
		return prob_i;
	}
	
	public void setProb_i(double p) {
		prob_i = p;
	}
	
	public int getFreq() {
		return freq;
	}
	
	public void setFreq(int f) {
		freq = f;
	}
	
	public int getFreq_i() {
		return freq_i;
	}
	
	public void setFreq_i(int f) {
		freq_i = f;
	}
	
	public double getNerdScore() {
		return nerdScore;
	}
	
	public void setNerdScore(double n) {
		nerdScore = n;
	}
	
	public double getSelectionScore() {
		return selectionScore;
	}
	
	public void setSelectionScore(double n) {
		selectionScore = n;
	}
	
	public double getRelatednessScore() {
		return relatednessScore;
	}
	
	public void setRelatednessScore(double score) {
		relatednessScore = score;
	}
	
	public boolean isCoReference() {
		return coReference;
	}

	public void setCoReference(boolean coReference) {
		this.coReference = coReference;
	}

	//@Override
	public String toString() {
        StringBuffer buffer = new StringBuffer();
		
		if (entity != null) {
			if (entity.getRawName() != null) {
				buffer.append(entity.getRawName() + "\t");
			}
		}
		
		//if (definitions != null)
		//	buffer.append("[" + definitions.toString() + "]\t");	
		if (wiktionaryExternalRef != -1) {
			buffer.append(wiktionaryExternalRef + "\t");	
		} 
		if (wikipediaExternalRef != -1) {
			buffer.append(wikipediaExternalRef + "\t");	
		}
		if (wikidataId != null) {
			buffer.append(wikidataId + "\t");	
		}
		
		if (domains != null) {
			buffer.append(domains.toString() + "\t");
		}
		
		if (variants != null) {
			for(Variant variant : variants)
				buffer.append(variant.toString() + "\t");
		}
		
		if (isSubTerm)
			buffer.append("isSubTerm\t");
		
		//if (nerdScore > 0.0) 
		{
			buffer.append(nerdScore + "(nerd)\t");
		}
		
		//if (selectionScore > 0.0) 
		{
			buffer.append(selectionScore + "(selection)\t");
		}
		
		{
			buffer.append(relatednessScore + "(relatedness)\t");
		}
		
		buffer.append(prob_c + "(prob_c)\t");
		buffer.append(freq + "(freq_c)\t");
		buffer.append(prob_i + "(prob_i)\t");
		buffer.append(freq_i + "(freq_i)\t");
		
		if (freebaseTypes != null)
			buffer.append(freebaseTypes.toString()+"\t");
		
		return buffer.toString();	
	}
	
	/**
	 *  This method combines and prune NerdCandidates. 
	 */
	static public List<NerdCandidate> prune(List<NerdCandidate> terms) {
		
		// expand the domains with the domain hierarchy information
		for(int i=0; i<terms.size(); i++) {
			NerdCandidate term = terms.get(i);
			
			List<String> ddomains = term.getDomains();
			
			if ( (ddomains != null) && (ddomains.size() != 0)) {
				try {
					term.setExpandedDomains(Domains.getInstance().expandDomains(ddomains));
				}
				catch(Exception e) {
					//("domain expansion failed");
				}
			}
		}
		
		boolean processing = true;
		while(processing) {
			int reduced = -1; 
			
			for(int i=0; i<terms.size(); i++) {
				NerdCandidate term = terms.get(i);
				String surface = term.getEntity().getRawName().toLowerCase(); 
				List<String> ddomains = term.getExpandedDomains();
								
				if ( (ddomains == null) || (ddomains.size() == 0)) {
					continue;
				}
							
				// do we have a term covering exactly the same string?
			
				for(int j=0; j<terms.size(); j++) {
					if (j == i) {
						continue;
					}
					NerdCandidate term2 = terms.get(j);
				 	String surface2 = term2.getEntity().getRawName().toLowerCase(); 
										
					if (surface.equals(surface2)) {						
						// the terms are covering exactly the same chunk
						// do we have matching domains?
						List<String> ddomains2 = term2.getExpandedDomains();
						if ( (ddomains2 != null) && (ddomains2.size() != 0) ) {
							boolean toMerge = false;		
							boolean stop = false;	
							for(String ddomain2 : ddomains2) {
								if (ddomains.contains(ddomain2) && !stop) {
									toMerge = true;
									stop = true;
								}
							}
							if (toMerge) {
								// we merge
								merge(term, term2);
								reduced = j;
								break;
							}
						}
					}
				}
			}
			
			if (reduced == -1) {
				processing = false;
			}
			else {
				// pruning pass
				System.out.println("remove " + terms.get(reduced).toString());
				terms.remove(reduced);								
			}

			
		}
		
		// we prune now following the longest term match from the KB : the term coming from the KB shorter than
		// the longest match from the KB and which have not been merged, are removed. 
		List<Integer> toRemove = new ArrayList<Integer>();
		for(int i=0; i<terms.size(); i++) {
			NerdCandidate term1 = terms.get(i);
			
			if (term1.isSubTerm) {
				continue;
			}
			
			if (term1.getMethod() == NerdCandidate.KB) {
				String surface1 = term1.getEntity().getRawName();
				
				// we check if the raw string is a substring of another NerdCandidate from KB or Combined methods
				for(int j=0; j<terms.size(); j++) {
					if (j == i)
						continue;
						
					NerdCandidate term2 = terms.get(j);
					if (term2.getMethod() == NerdCandidate.WSD) 
						continue;

					String surface2 = term2.getEntity().getRawName();
					if ((surface2.length() > surface1.length()) && (surface2.indexOf(surface1) != -1)) {
						toRemove.add(new Integer(i));
						break;
					}
				}
			}
		}
		
		List<NerdCandidate> result = new ArrayList<NerdCandidate>();
		for(int i=0; i<terms.size(); i++) {
			if (toRemove.contains(new Integer(i))) {
				continue;
			}
			else {
				result.add(terms.get(i));
			}
		}
		
		/*for(int i=terms.size()-1; i>=0; i--) {
			if (toRemove.contains(new Integer(i)))
				terms.remove(i);
		}*/
		
		return result;
	}

	/**
	 * Return the Wikidata identifier of the entity in relation to the candidate wikidata ID
	 * via P31 (instance of) if such relation exists in the list of statements
	 */
	public String getWikidataP31Id() {
		if (wikidataId == null) 
			return null;
		// check the statements
		List<Statement> statements = UpperKnowledgeBase.getInstance().getStatements(wikidataId);
		if (statements == null || statements.size() == 0)
			return null;
		for(Statement statement : statements) {
			if (statement.getPropertyId() != null && statement.getPropertyId().equals("P31"))
				return statement.getValue(); 
		}
		return null;
	}
	
	/**
	 * Merge two NerdCandidates.
	 */
	static public NerdCandidate merge(NerdCandidate term1, NerdCandidate term2) {
		// update domains
		List<String> domains1 = term1.getDomains();
		List<String> domains2 = term2.getDomains();
		
		if ( (domains1 == null) || (domains1.size() == 0) ) {
			term1.setDomains(domains2);
		}
		else if ( (domains2 != null) && (domains2.size() != 0) ) {			
			for(String domain2 : domains2) {
				term1.addDomain(domain2);
			}
		}
		
		// update entity information
		term1.setEntity(term2.getEntity());
		/*if (term1.getEntity().getNormalisedName() == null) {
			term1.getEntity().setNormalisedName(term2.getNormalisedName());
		}
		if (term1.getType() == null) {
			term1.setType(term2.getType());
		}
		if ( (term1.getSubTypes() == null) || (term1.getSubTypes().size() == 0)) {
			term1.setSubTypes(term2.getSubTypes());
		}
		else {
			if ( (term2.getSubTypes() != null) && (term2.getSubTypes().size() != 0)) {
				for(String subType2 : term2.getSubTypes()) {
					term1.getSubTypes().add(subType2);
				}
			}
		}*/
		
		// update definitions
		if (term2.getDefinitions() != null) {
			for(Definition definition : term2.getDefinitions()) {
				term1.addDefinition(definition);
			}
		}
		
		// update variant information
		if (term2.getVariants() != null) {
			for(Variant variant : term2.getVariants()) {
				term1.addVariant(variant);
			}
		}
		
		// subterm information
		if (term2.isSubTerm) {
			term1.isSubTerm = true;
		}
		
		if (term1.getMethod() != term2.getMethod()) {
			if ( (term1.getMethod() == NerdCandidate.COMBINED) || (term2.getMethod() == NerdCandidate.COMBINED) ) {
				term1.setMethod(NerdCandidate.COMBINED);
			}
			else if ( ( (term1.getMethod() == NerdCandidate.WSD) && (term2.getMethod() == NerdCandidate.KB) ) || 
					  ( (term1.getMethod() == NerdCandidate.KB) && (term2.getMethod() == NerdCandidate.WSD) ) ) {
				term1.setMethod(NerdCandidate.COMBINED);
			}
		}

		return term1;
	}
	
	/**
	 *  Comparable implementation. We sort against the nerdScore
	 */
	public int compareTo(NerdCandidate compareNerdCandidate) {
		
		//descending order
		int val = ((int)((compareNerdCandidate.getNerdScore() - this.nerdScore) * 1000));
		//return ((int)((compareQuantity - this.selectionScore) * 1000));

		if (val == 0) {
			val = ((int)((compareNerdCandidate.getSelectionScore() - this.selectionScore) * 1000));
		}

		if (val == 0) {
			val = ((int)((compareNerdCandidate.getRelatednessScore() - this.relatednessScore) * 1000));
		}

		if (val == 0) {
			val = ((int)((compareNerdCandidate.getProb_c() - this.prob_c) * 1000));	
		}

		if (val == 0) {
			if (compareNerdCandidate.getWikipediaExternalRef() != -1)
				val = compareNerdCandidate.getWikipediaExternalRef() - this.wikipediaExternalRef;
			else if (compareNerdCandidate.getWikidataId() != null) 
				val = compareNerdCandidate.getWikidataId().compareTo(this.wikidataId);
		}

		return val;
	}

	/**
	 * Copy of a candidate, with deep copy of relevant instance variable
	 */
	public NerdCandidate copy(NerdEntity entity) {
		NerdCandidate copy = new NerdCandidate(entity);
		copy.setWikiSense(this.getWikiSense());
		copy.setWikipediaExternalRef(this.getWikipediaExternalRef());
		copy.setProb_c(this.getProb_c());
		copy.setPreferredTerm(this.getPreferredTerm());
		copy.setLang(this.getLang());
		copy.setLabel(this.getLabel());
		copy.setWikidataId(this.getWikidataId());
		copy.setWikipediaCategories(this.getWikipediaCategories());
		return copy;
	}
}