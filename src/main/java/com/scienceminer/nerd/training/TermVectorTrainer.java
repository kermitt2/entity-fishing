package com.scienceminer.nerd.training;

import com.scienceminer.nerd.exceptions.*;
import org.grobid.core.lang.Language;
import org.grobid.core.lang.LanguageDetectorFactory;
import com.scienceminer.nerd.disambiguation.*;
import com.scienceminer.nerd.service.*;
import com.scienceminer.nerd.kb.Category;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.io.FileUtils;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.io.*;
import org.codehaus.jackson.node.*;

/**
 * Method for training and generating training data for the disambiguation of a vector of weighted terms
 *
 */
public class TermVectorTrainer {
	public static final Logger LOGGER = LoggerFactory.getLogger(TermVectorTrainer.class);
	
	private NerdEngine engine = null;
	
	public TermVectorTrainer() {
		try {
			engine = NerdEngine.getInstance();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void generateTrainingFromJSON(File jsonFile, String outpath) {
        if (!jsonFile.exists()) {
            throw new NerdResourceException("File does not exist: " + jsonFile.getAbsolutePath() );
        }
        if (!jsonFile.canRead()) {
            throw new NerdResourceException("Cannot read file: " + jsonFile.getAbsolutePath());
        }
        InputStream ist = null;
        InputStreamReader isr = null;
        BufferedReader dis = null;
		try {
			//String content = FileUtils.readFileToString(jsonFile, "UTF-8");
			ist = new FileInputStream(jsonFile);
            isr = new InputStreamReader(ist, "UTF8");
            dis = new BufferedReader(isr);

            String l = null;
			StringBuilder segment = null;
            while ((l = dis.readLine()) != null) {
                if (l.length() == 0) continue;
				if (l.startsWith("{")) {
					segment = new StringBuilder();
					segment.append(l);
					segment.append("\n");
				}
				else if (l.startsWith("}")) {
					segment.append(l);
					generateTrainingFromJSON(segment.toString(), outpath);
					segment = new StringBuilder();
				}
				else {
					if (l.indexOf("ObjectId(") != -1) {
						l = l.replace("ObjectId(", "").replace("),",",");
					}
					segment.append(l);
					segment.append("\n");
				}
			}
        } 
		catch (FileNotFoundException e) {
//	    	e.printStackTrace();
            throw new NerdException("An exception occured while running Nerd.", e);
        } 
		catch (IOException e) {
//	    	e.printStackTrace();
            throw new NerdException("An exception occured while running Nerd.", e);
        } 
		finally {
            try {
                if (ist != null)
                    ist.close();
                if (isr != null)
                    isr.close();
                if (dis != null)
                    dis.close();
            } 
			catch (Exception e) {
                throw new NerdResourceException("Cannot close all streams.", e);
            }
        }
	}
	
	public void generateTrainingFromJSON(String jsonChunk, String path) {
		try {		
			ObjectMapper mapper = new ObjectMapper();
			JsonStringEncoder encoder = JsonStringEncoder.getInstance();
			JsonNode jsonRoot = mapper.readTree(jsonChunk);
			List<WeightedTerm> terms = new ArrayList<WeightedTerm>();
			
			JsonNode idNode = jsonRoot.findPath("publication_id");
			if ((idNode != null) && (!idNode.isMissingNode())) {
    			String docid = idNode.getTextValue();
				String outputPath = path + File.separator + docid + ".json";
				
				JsonNode id = jsonRoot.findPath("_id");
				JsonNode inventionTitle = jsonRoot.findPath("invention_title");
				JsonNode keywordsNode = jsonRoot.findPath("keywords");
				
				if (keywordsNode.isArray()) {
				    for (final JsonNode termNode : keywordsNode) {
						String keyword = null;
						double weight = 0.0;

		                JsonNode idNode2 = termNode.findPath("keyword");
						if ((idNode2 != null) && (!idNode2.isMissingNode())) {
		        			keyword = idNode2.getTextValue();
							keyword = keyword.replace("_", " ");
						}

		                idNode2 = termNode.findPath("weight");
						if ((idNode2 != null) && (!idNode2.isMissingNode())) {
		        			weight = idNode2.getDoubleValue();
						}

						if ( (keyword != null) && (weight != 0.0) ) {
							WeightedTerm wTerm = new WeightedTerm();
							wTerm.setTerm(keyword);
							wTerm.setScore(weight);
							terms.add(wTerm);
						}
				    }
				}
				NerdQuery query = new NerdQuery();
				Language lang = new Language("en", 1.0);
				query.setLanguage(lang);
				query.setTermVector(terms);
				query.setNbest(true);
				// we disambiguate the term vector
				engine.disambiguateWeightedTerms(query);
					
				// write back the disambiguated terms
				StringBuilder builder = new StringBuilder();
				builder.append("{\n");
			    builder.append("\t\"_id\" : \"" + id.getTextValue() + "\",\n"); 
			    builder.append("\t\"publication_id\" : \""+docid+"\",\n"); 
			    builder
					.append("\t\"invention_title\" : \"" + encoder.quoteAsUTF8(inventionTitle.getTextValue()) + "\",\n"); 
				
				// if available, document level distribution of categories
				List<Category> globalCategories = query.getGlobalCategories();
				if ( (globalCategories != null) && (globalCategories.size() > 0) ) {
					builder.append("\t\"global_categories\": [\n");
					boolean first = true;
					for(com.scienceminer.nerd.kb.Category category : globalCategories) {				
						byte[] encoded = encoder.quoteAsUTF8(category.getName());
						String output = new String(encoded);
						if (first) {
							first = false;
						}
						else
							builder.append(", \n");
						builder.append("\t\t{\n\t\t\t\"weight\" : " + category.getWeight() + ", \n\t\t\t\"source\" : \"wikipedia-" 
							+ lang.getLang()
							+ "\", \n\t\t\t\"category\" : \"" + output + "\", ");
						builder.append("\n\t\t\t\"page_id\" : " + category.getWikiPageID() + " }");
					}
					builder.append("],\n");
				}
				
				builder.append("\t\"keywords\" : [\n");
				List<WeightedTerm> resultTerms = query.getTermVector();
				if ( (resultTerms != null) && (resultTerms.size() > 0) ) {
					boolean start = true;
					for(WeightedTerm term : resultTerms) {
						builder.append("\t\t{\n");
						builder.append("\t\t\t\"keyword\" : \"" + term.getTerm() + "\",\n");
						builder.append("\t\t\t\"weight\" : " + term.getScore() + ",\n");
						builder.append("\t\t\t\"entities\" : [ \n");
						List<NerdEntity> entities = term.getNerdEntities();
						if ( (entities != null) && (entities.size() > 0) ) {
							boolean first = true;
							for(NerdEntity entity : entities) {
								if (first) 
									first = false;
								else 
									builder.append(", \n");
								builder.append(entity.toJsonFull()); 
							}
						}
						if (start) {
							builder.append("\n\t\t\t]\n\t\t}\n");
							start = false;
						}
						else
							builder.append("\n\t\t\t]\n\t\t},\n");
					}
				}

				builder.append("\t]\n");	
				builder.append("}\n");
				FileUtils.writeStringToFile(new File(outputPath), builder.toString(), StandardCharsets.UTF_8);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
}