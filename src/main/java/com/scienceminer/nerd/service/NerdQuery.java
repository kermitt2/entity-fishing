package com.scienceminer.nerd.service;

import org.grobid.core.lang.Language;

import org.grobid.core.data.Entity;
import org.grobid.core.utilities.KeyGen;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.disambiguation.Sentence;
import com.scienceminer.nerd.disambiguation.WeightedTerm;
import com.scienceminer.nerd.utilities.NerdRestUtils;
import com.scienceminer.nerd.kb.Category;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONStringer;

/*import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.io.JsonStringEncoder;*/

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

/**
 * This is the POJO object for representing input and output "enriched" query. 
 * Having Jersey supporting JSON/object mapping, this permits to consume JSON post query.
 * 
 * @author Patrice
 *
 */
public class NerdQuery {
	
	// main text component
	private String text = null;
	
	// alternative text components for patent input
	private String abstract_ = null;
	private String claims = null;
	private String description = null;
	
	// language of the query
	private Language language = null;
	
	// the result of the query disambiguation and enrichment for each identified entities
	private List<NerdEntity> entities = null;
	
	// the sentence position if such segmentation is to be realized
	private List<Sentence> sentences = null;
	
	// a list of optional language codes for having multilingual Wikipedia sense correspondences 
	// note that the source language is by default the language of results, here ae additional
	// result correspondences in target languages for each entities 
	private List<String> resultLanguages = null;
	
	// runtime in ms of the last processing
	private long runtime = 0;
	
	// mode indicating if we disambiguate or not 
	private boolean onlyNER = false; 
	private boolean shortText = false;
	private boolean nbest = false;
	private boolean sentence = false;
	private NerdRestUtils.Format format = NerdRestUtils.Format.valueOf("JSON");
	private String customisation = "generic";
	
	// list of sentences to be processed
	private Integer[] processSentence = null; 
	
	// weighted vector to be disambiguated
	private List<WeightedTerm> termVector = null;
	
	// distribution of (Wikipedia) categories corresponding to the disambiguated object 
	// (text, term vector or search query)
	private List<Category> globalCategories = null;
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getAbstract_() {
		return abstract_;
	}

	public void setAbstract_(String abstract_) {
		this.abstract_ = abstract_;
	}

	public String getClaims() {
		return claims;
	}

	public void setClaims(String claims) {
		this.claims = claims;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setLanguage(Language lang) {
		this.language = lang;
	}

	public Language getLanguage() {
		return language;
	}

	public void setResultLanguages(List<String> langs) {
		this.resultLanguages = langs;
	}

	public List<String> getResultLanguages() {
		return resultLanguages;
	}

	public void setRuntime(long tim) {
		runtime = tim;
	}
	
	public long getRuntime() {
		return runtime;
	}

	public List<WeightedTerm> getTermVector() {
		return termVector;
	}

	public void setTermVector(List<WeightedTerm> termVector) {
		this.termVector = termVector;
	}

	public List<NerdEntity> getEntities() {
		return entities;
	}
	
	public void setAllEntities(List<Entity> nerEntities) {
		this.entities = new ArrayList<NerdEntity>();
		for(Entity entity : nerEntities) {
			this.entities.add(new NerdEntity(entity));
		}
	}
	
	public void setEntities(List<NerdEntity> entities) {
		this.entities = entities;
	}
	
	public List<Sentence> getSentences() {
		return sentences;
	}
	
	public void setSentences(List<Sentence> sentences) {
		this.sentences = sentences;
	}
	
	public boolean getOnlyNER() {
		return onlyNER;
	}
	
	public void setOnlyNER(boolean onlyNER) {
		this.onlyNER = onlyNER;
	}
	
	public boolean getShortText() {
		return shortText;
	}
	
	public void setShortText(boolean shortText) {
		this.shortText = shortText;
	}
	
	public boolean getNbest() {
		return nbest;
	}
	
	public void setNbest(boolean nbest) {
		this.nbest = nbest;
	}
	
	public boolean getSentence() {
		return sentence;
	}

	public void setSentence(boolean sentence) {
		this.sentence = sentence;
	}

	public String getCustomisation() {
		return customisation;
	}

	public void setCustomisation(String customisation) {
		this.customisation = customisation;
	}

	public Integer[] getProcessSentence() {
		return processSentence;
	}
	
	public void setProcessSentence(Integer[] processSentence) {
		this.processSentence = processSentence;
	}

	public NerdRestUtils.Format getFormat() {
		return format;
	}

	public void setFormat(NerdRestUtils.Format format) {
		this.format = format;
	}

	public void addEntities(List<NerdEntity> newEntities) {
		if (entities == null) {
			entities = new ArrayList<NerdEntity>();
		}
		if (newEntities.size() == 0) {
			return;
		}
		for(NerdEntity entity : newEntities) {
			entities.add(entity);
		}
	}

	public void addEntity(NerdEntity entity) {
		if (entities == null) {
			entities = new ArrayList<NerdEntity>();
		}
		entities.add(entity);
	}
	
	public List<Category> getGlobalCategories() {
		return globalCategories;
	}
	
	public void setGlobalCategories(List<Category> globalCategories) {
		this.globalCategories = globalCategories;
	}

	public void addGlobalCategory(Category category) {
		if (globalCategories == null) {
			globalCategories = new ArrayList<Category>();
		}
		globalCategories.add(category);
	}

	public String toJSON() {
		ObjectMapper mapper = new ObjectMapper();
		String json = null;
		try {
			 json = mapper.writeValueAsString(this);

		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return json;
	}

	public String toJSONFullClean() {
		JsonStringEncoder encoder = JsonStringEncoder.getInstance();
		StringBuilder buffer = new StringBuilder();
		buffer.append("{");

		// server runtime is always present (even at 0.0)
		buffer.append("\"runtime\": " + runtime);

		// parameters
		buffer.append(", \"onlyNER\": " + onlyNER);
		buffer.append(", \"nbest\": " + nbest);

		// parameters
		if ( (processSentence != null) && (processSentence.length > 0) ) {
			buffer.append(", \"processSentence\": [");
			for(int i=0; i<processSentence.length; i++) {
				if (i != 0) {
					buffer.append(", ");
				}
				buffer.append(processSentence[i].intValue());
			}
			buffer.append("]");
		}

		// surface form
		if (text != null) {
			byte[] encoded = encoder.quoteAsUTF8(text);
			String output = new String(encoded); 
			buffer.append(", \"text\": \"" + output + "\"");
			if ( (sentences != null) && (sentences.size() > 0) ) {
				buffer.append(", \"sentences\": [ " );
				boolean theStart = true;
				for(Sentence sentence : sentences) {
					if (theStart) {
						buffer.append(sentence.toJSON());
						theStart = false;
					}
					else
						buffer.append(", " + sentence.toJSON());
				}
				buffer.append(" ]");
			}
		}
		
		if ( (termVector != null) && (termVector.size() > 0) ) {
			buffer.append(", \"termVector\": [ ");
			boolean begin = true;
			for(WeightedTerm term : termVector) {
				if (!begin)
					buffer.append(", ");
				else
					begin = false;
				buffer.append(term.toJson());
			}
			buffer.append(" ]");
		}

		String lang = "en"; // default language
		if (language != null) {
			buffer.append(", \"language\": " + language.toJSON());
			lang = language.getLang();
		}
		
		// if available, document level distribution of categories
		if ( (globalCategories != null) && (globalCategories.size() > 0) ) {
			buffer.append(", \"global_categories\": [");
			boolean first = true;
			for(com.scienceminer.nerd.kb.Category category : globalCategories) {				
				byte[] encoded = encoder.quoteAsUTF8(category.getName());
				String output = new String(encoded);
				if (first) {
					first = false;
				}
				else
					buffer.append(", ");
				buffer.append("{\"weight\" : " + category.getWeight() + ", \"source\" : \"wikipedia-" + lang
					+ "\", \"category\" : \"" + output + "\", ");
				buffer.append("\"page_id\" : " + category.getWikiPageID() + "}");
			}
			buffer.append("]");
		}

		if ( (entities != null) && (entities.size() > 0) ) {
			buffer.append(", \"entities\": [");
			boolean first = true;
			for(NerdEntity entity : entities) {
				if (first) 
					first = false;
				else 
					buffer.append(", ");
				buffer.append(entity.toJsonFull()); 
			}
			buffer.append("]");
		}
		buffer.append("}");

		return buffer.toString();
	}

	public String toJSONCompactClean() {
		JsonStringEncoder encoder = JsonStringEncoder.getInstance();
		StringBuilder buffer = new StringBuilder();
		buffer.append("{");

		// server runtime is always present (even at 0.0)
		buffer.append("\"runtime\": " + runtime);

		// parameters
		buffer.append(", \"onlyNER\": " + onlyNER);
		buffer.append(", \"nbest\": " + nbest);

		// parameters
		if ( (processSentence != null) && (processSentence.length > 0) ) {
			buffer.append(", \"processSentence\": [");
			for(int i=0; i<processSentence.length; i++) {
				if (i != 0) {
					buffer.append(", ");
				}
				buffer.append(processSentence[i].intValue());
			}
			buffer.append("]");
		}

		// surface form
		if (text != null) {
			byte[] encoded = encoder.quoteAsUTF8(text);
			String output = new String(encoded); 
			buffer.append(", \"text\": \"" + output + "\"");
			if ( (sentences != null) && (sentences.size() > 0) ) {
				buffer.append(", \"sentences\": [ " );
				boolean theStart = true;
				for(Sentence sentence : sentences) {
					if (theStart) {
						buffer.append(sentence.toJSON());
						theStart = false;
					}
					else
						buffer.append(", " + sentence.toJSON());
				}
				buffer.append(" ]");
			}
		}
		
		if ( (termVector != null) && (termVector.size() > 0) ) {
			buffer.append(", \"termVector\": [ ");
			boolean begin = true;
			for(WeightedTerm term : termVector) {
				if (!begin)
					buffer.append(", ");
				else
					begin = false;
				buffer.append(term.toJson());
			}
			buffer.append(" ]");
		}

		String lang = "en"; // default language
		if (language != null) {
			buffer.append(", \"language\": " + language.toJSON());
			lang = language.getLang();
		}
		
		// if available, document level distribution of categories
		if ( (globalCategories != null) && (globalCategories.size() > 0) ) {
			buffer.append(", \"global_categories\": [");
			boolean first = true;
			for(com.scienceminer.nerd.kb.Category category : globalCategories) {				
				byte[] encoded = encoder.quoteAsUTF8(category.getName());
				String output = new String(encoded);
				if (first) {
					first = false;
				}
				else
					buffer.append(", ");
				buffer.append("{\"weight\" : " + category.getWeight() + ", \"source\" : \"wikipedia-" + lang
					+ "\", \"category\" : \"" + output + "\", ");
				buffer.append("\"page_id\" : " + category.getWikiPageID() + "}");
			}
			buffer.append("]");
		}

		if ( (entities != null) && (entities.size() > 0) ) {
			buffer.append(", \"entities\": [");
			boolean first = true;
			for(NerdEntity entity : entities) {
				if (first) 
					first = false;
				else 
					buffer.append(", ");
				buffer.append(entity.toJsonCompact()); 
			}
			buffer.append("]");
		}
		buffer.append("}");

		return buffer.toString();
	}

	@Override
	public String toString() {
		return "Query [text=" + text + ", terms=" + "]";
	}
	
	/** 
	 * Export of standoff annotated text in TEI format 
	 */	 
	public String toTEI() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><tei xmlns:ng=\"http://relaxng.org/ns/structure/1.0\" xmlns:exch=\"http://www.epo.org/exchange\" xmlns=\"http://www.tei-c.org/ns/1.0\">");
		buffer.append("<teiHeader>");
		buffer.append(" </teiHeader>");
		String idP = KeyGen.getKey();
		buffer.append("<standoff>");
		int n = 0;
		for(NerdEntity entity : entities) {
			//buffer.append(entity.toTEI(idP, n));
			n++;
		}
		buffer.append("</standoff>");
		buffer.append("<text>");
		buffer.append(text);
		buffer.append("</text>");
		buffer.append("</tei>");
		
		return buffer.toString();
	}
}