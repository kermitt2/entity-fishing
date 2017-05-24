package com.scienceminer.nerd.service;

import java.util.*;
import java.io.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.HttpHeaders; 

import com.scienceminer.nerd.utilities.NerdRestUtils;
import com.scienceminer.nerd.utilities.NerdServiceProperties;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.grobid.core.lang.Language;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.*;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.lang.Language;
import org.grobid.core.document.*;
import org.grobid.core.utilities.*;
import org.grobid.core.data.*;

import com.scienceminer.nerd.disambiguation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

import static com.scienceminer.nerd.service.NerdRestProcessQuery.parseQuery;

/**
 * 
 * @author Patrice
 * 
 */
public class NerdRestProcessFile {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestProcessFile.class);

	/**
	 * Parse a structured query in combination with a PDF file and return the corresponding 
	 * normalized enriched and disambiguated query object, where resulting entities include
	 * position coordinates in the PDF.
	 * 
	 * @param theQuery 
	 *            the POJO query object
	 * @param inputStream
	 *            the PDF file as InputStream
	 * @return a response query object containing the structured representation of
	 *         the enriched and disambiguated query.
	 */
	public static Response processQueryPDFFile(String theQuery, final InputStream inputStream) {
		LOGGER.debug(methodLogIn());
		Response response = null;		
		File originFile = null;
        Engine engine = null;
        LOGGER.debug(">> received query to process: " + theQuery);
        try {
            LibraryLoader.load();
            engine = GrobidFactory.getInstance().getEngine();
            originFile = IOUtilities.writeInputFile(inputStream);
            LOGGER.debug(">> input PDF file saved locally...");

            GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().build();
            if (originFile == null) {
                response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } else {
                long start = System.currentTimeMillis();
				NerdQuery nerdQuery = parseQuery(theQuery);
				
				if ( (nerdQuery == null) || 
				  	 ( (nerdQuery.getText() != null) && (nerdQuery.getText().trim().length() > 1) ) ||
				  	 ( (nerdQuery.getShortText() != null) && (nerdQuery.getShortText().trim().length() > 1) ) ) {
					return Response.status(Status.BAD_REQUEST).build();	 
				}
				LOGGER.debug(">> set query object...");
								
				// language identification
				// test first if the language is already indicated in the query structure	
				Language lang = nerdQuery.getLanguage();
				if ( (nerdQuery.getLanguage() == null) || (nerdQuery.getLanguage().getLang() == null) ) {
					LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
					lang = languageUtilities.runLanguageId(nerdQuery.getText());
					nerdQuery.setLanguage(lang);
					LOGGER.debug(">> identified language: " + lang.toString());
				} else {
					System.out.println("lang is already defined");
					lang.setConf(1.0);
					LOGGER.debug(">> language already identified: " + nerdQuery.getLanguage().getLang().toString());
				}
				
				if ( (lang == null) || (lang.getLang() == null) ) {
					response = Response.status(Status.NOT_ACCEPTABLE).build();
					LOGGER.debug(methodLogOut());  
					return response;
				} else {
					String theLang = lang.getLang();
					if ( !theLang.equals("en") && !theLang.equals("de") && !theLang.equals("fr") ) {
						response = Response.status(Status.NOT_ACCEPTABLE).build();
						LOGGER.debug(methodLogOut());  
						return response;
					}
				}
				
				// entities originally from the query are marked as such
				/*List<NerdEntity> originalEntities = null;
				if  ( (nerdQuery.getEntities() != null) && (nerdQuery.getEntities().size() > 0) ) {
					for(NerdEntity entity : nerdQuery.getEntities()) {
						entity.setNer_conf(1.0);
						
						// do we have disambiguated entity information for the entity?
						if (entity.getWikipediaExternalRef() != -1) {
							entity.setOrigin(NerdEntity.Origin.USER);
							entity.setNerdScore(1.0);
						}
					}
					originalEntities = nerdQuery.getEntities();
				}*/
				
				//List<NerdEntity> entities = originalEntities;
		        Document doc = null;
		        DocumentContext documentContext = new DocumentContext();
		        NerdQuery workingQuery = new NerdQuery(nerdQuery);
		        //NerdQuery workingQuery = nerdQuery;
		        try {
					DocumentSource documentSource = 
						DocumentSource.fromPdf(originFile, config.getStartPage(), config.getEndPage());
					doc = engine.getParsers().getSegmentationParser().processing(documentSource, config);
					
		            // here we process the relevant textual content of the document

		            // for refining the process based on structures, we need to filter
		            // segment of interest (e.g. header, body, annex) and possibly apply 
		            // the corresponding model to further filter by structure types 

		            // from the header, we are interested in title, abstract and keywords
		            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(SegmentationLabel.HEADER);
		            if (documentParts != null) {
		                String header = engine.getParsers().getHeaderParser().getSectionHeaderFeatured(doc, documentParts, true);
		                List<LayoutToken> tokenizationHeader = 
		                	doc.getTokenizationParts(documentParts, doc.getTokenizations());
		                String labeledResult = null;

		                // alternative
		                String header2 = doc.getHeaderFeatured(true, true);
		                // we choose the longest header
		                if ((header == null) || (header.trim().length() == 0)) {
		                	header = header2;
		                	tokenizationHeader = doc.getTokenizationsHeader();
		                } else if ( (header2!= null) && (header2.length() > header.length()) ) {
			            	header = header2;
			                tokenizationHeader = doc.getTokenizationsHeader();
						}
		                
		                if ((header != null) && (header.trim().length() > 0)) {
		                    labeledResult = engine.getParsers().getHeaderParser().label(header);

		                    BiblioItem resHeader = new BiblioItem();
		                    resHeader.generalResultMapping(doc, labeledResult, tokenizationHeader);

		                    // title
		                    List<LayoutToken> titleTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_TITLE);
		                    if (titleTokens != null) {
System.out.println("Process title... ");// + LayoutTokensUtil.toText(titleTokens));
		                    	workingQuery.setEntities(null);
		                        List<NerdEntity> newEntities = processLayoutTokenSequence(titleTokens, documentContext, workingQuery);
if (newEntities != null)
System.out.println(newEntities.size() + " nerd entities");		                        
		                        nerdQuery.addNerdEntities(newEntities);
		                    }

		                    // abstract
		                    List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
		                    if (abstractTokens != null) {
System.out.println("Process abstract...");
		                    	workingQuery.setEntities(null);
		                        List<NerdEntity> newEntities = processLayoutTokenSequence(abstractTokens, documentContext, workingQuery);
if (newEntities != null)
System.out.println(newEntities.size() + " nerd entities");	
		                        nerdQuery.addNerdEntities(newEntities);
		                    }

		                    // keywords
		                    List<LayoutToken> keywordTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_KEYWORD);
		                    if (keywordTokens != null) {
System.out.println("Process keywords...");
		                    	workingQuery.setEntities(null);
		                        List<NerdEntity> newEntities = processLayoutTokenSequence(keywordTokens, documentContext, workingQuery);
if (newEntities != null)
System.out.println(newEntities.size() + " nerd entities");	
		                        nerdQuery.addNerdEntities(newEntities);
		                    }
		                }
		            }

		            // we can process all the body, in the future figure and table could be the 
		            // object of more refined processing
		            /*documentParts = doc.getDocumentPart(SegmentationLabel.BODY);
		            if (documentParts != null) {
System.out.println("Process body...");
		            	workingQuery.setEntities(null);
		                List<NerdEntity> newEntities = processDocumentPart(documentParts, doc, documentContext, workingQuery);
if (newEntities != null)
System.out.println(newEntities.size() + " nerd entities");	
		                nerdQuery.addNerdEntities(newEntities);
		            }

		            // we don't process references (although reference titles could be relevant)
		            
		            // acknowledgement
		            documentParts = doc.getDocumentPart(SegmentationLabel.ACKNOWLEDGEMENT);
		            if (documentParts != null) {
System.out.println("Process acknowledgement...");
		            	workingQuery.setEntities(null);
		                List<NerdEntity> newEntities = processDocumentPart(documentParts, doc, documentContext, workingQuery);
if (newEntities != null)
System.out.println(newEntities.size() + " nerd entities");	
		                nerdQuery.addNerdEntities(newEntities);
		            }

		            // we can process annexes
		            documentParts = doc.getDocumentPart(SegmentationLabel.ANNEX);
		            if (documentParts != null) {
System.out.println("Process annex...");  
		            	workingQuery.setEntities(null);
		                List<NerdEntity> newEntities = processDocumentPart(documentParts, doc, documentContext, workingQuery);
if (newEntities != null)
System.out.println(newEntities.size() + " nerd entities");	
		                nerdQuery.addNerdEntities(newEntities);
		            }

		            // footnotes are also relevant
		            documentParts = doc.getDocumentPart(SegmentationLabel.FOOTNOTE);
		            if (documentParts != null) {
System.out.println("Process footnotes...");  
		            	workingQuery.setEntities(null);
		                List<NerdEntity> newEntities = processDocumentPart(documentParts, doc, documentContext, workingQuery);
if (newEntities != null)
System.out.println(newEntities.size() + " nerd entities");	
		                nerdQuery.addNerdEntities(newEntities);
		            }*/

		        } catch (Exception e) {
		        	LOGGER.error("Cannot process input pdf file. ", e);
		            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		        }

		        //List<NerdEntity> entities = workingQuery.getEntities();
		        /*if (entities != null) {
					// we keep only entities not conflicting with the ones already present in the query
					int offsetPos = 0;
					int ind = 0;
					
					if (originalEntities == null)
						workingQuery.setAllEntities(entities);
					else {
						for(Entity entity : entities) {
							int begin = entity.getOffsetStart();
							int end = entity.getOffsetEnd();
							
							if (ind >= originalEntities.size()) {
								NerdEntity theEntity = new NerdEntity(entity);
								newEntities.add(theEntity);
							}
							else if (end < originalEntities.get(ind).getOffsetStart()) {
								NerdEntity theEntity = new NerdEntity(entity);
								newEntities.add(theEntity);
							}
							else if ( (begin > originalEntities.get(ind).getOffsetStart()) &&
								(begin < originalEntities.get(ind).getOffsetEnd()) ) {
								continue;
							}
							else if ( (end > originalEntities.get(ind).getOffsetStart()) &&
							(end < originalEntities.get(ind).getOffsetEnd()) ) {
								continue;
							}
							else if (begin > originalEntities.get(ind).getOffsetEnd()) {
								while(ind < originalEntities.size()) {
									ind++;
									if (ind >= originalEntities.size()) {
										NerdEntity theEntity = new NerdEntity(entity);
										newEntities.add(theEntity);
										break;
									}
									if (begin < originalEntities.get(ind).getOffsetEnd()) {
										if (end < originalEntities.get(ind).getOffsetStart()) {
											NerdEntity theEntity = new NerdEntity(entity);
											newEntities.add(theEntity);
										}
										break;
									}
								}
							}
						}
						for(NerdEntity entity : originalEntities) {
							newEntities.add(entity);
						}
						workingQuery.setEntities(newEntities);
					}
				} else {
					workingQuery.setEntities(originalEntities);
				}*/

		        nerdQuery.setText(null);
		        nerdQuery.setShortText(null);
		        nerdQuery.setTokens(null);

				long end = System.currentTimeMillis();
				nerdQuery.setRuntime(end - start);
				System.out.println("runtime: " + (end - start));
				Collections.sort(nerdQuery.getEntities());
				String json = nerdQuery.toJSONCompactClean(doc);
				
				// TBD: output in the resulting json also page info from the doc object as in GROBID

				if (json == null) {
					response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
				}
				else {
					response = Response.status(Status.OK).entity(json)
						.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON+"; charset=UTF-8" )
						.build();
				}
			}
		} 
		catch (NoSuchElementException nseExp) {
			LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		}
		catch (Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
		}
		LOGGER.debug(methodLogOut());
		return response;
	}

	/**
	 * Generate a global context for a document
	 */
	public static NerdContext getGlobalContext(NerdQuery query) {
		return null;
	}

	private static List<NerdEntity> processLayoutTokenSequence(List<LayoutToken> layoutTokens, 
														NerdContext documentContext,
                                                  		NerdQuery workingQuery) {
		// text of the selected segment
        //String text = LayoutTokensUtil.toText(layoutTokens);
        //List<NerdEntity> entities = workingQuery.getEntities();
/*for(LayoutToken token : layoutTokens) {
	System.out.print(token.getText());
}
System.out.println("\n");*/
        workingQuery.setText(null);
        workingQuery.setShortText(null);
        workingQuery.setTokens(layoutTokens);
        try {
	        // ner
			ProcessText processText = ProcessText.getInstance();
			List<Entity> nerEntities = processText.process(workingQuery);
if (nerEntities != null)
System.out.println(nerEntities.size() + " ner entities");
			if (!workingQuery.getOnlyNER()) {
				List<Entity> entities2 = processText.processBrutal(workingQuery);
				if (entities2 != null) {
System.out.println(entities2.size() + " non-ner entities");
					for(Entity entity : entities2) {
						// we add entities only if the mention is not already present
						if (!nerEntities.contains(entity))
							nerEntities.add(entity);
					}
				}
			}

			/*if (nerEntities != null) {
				// we keep only entities not conflicting with the ones already present in the query
				if (entities == null) {*/
					workingQuery.setAllEntities(nerEntities);
				/*} else {
					// overlapping are based on the coordinates of the bounding boxes of entities
					for(Entity entity : nerEntities) {
						// based on PDF coordinates?
					}
				}
			}*/

			if (workingQuery.getEntities() != null) {
/*for (NerdEntity entity : workingQuery.getEntities()) {
	if (entity.getBoundingBoxes() == null)
		System.out.println("Empty bounding box for " + entity.toString());
}*/

				// sort the entities
				Collections.sort(workingQuery.getEntities());
				// disambiguate and solve entity mentions
				if (!workingQuery.getOnlyNER()) {
					NerdEngine disambiguator = NerdEngine.getInstance();
					List<NerdEntity> disambiguatedEntities = 
						disambiguator.disambiguate(workingQuery);
					workingQuery.setEntities(disambiguatedEntities);
/*for (NerdEntity entity : workingQuery.getEntities()) {
	if (entity.getBoundingBoxes() == null)
		System.out.println("Empty bounding box for " + entity.toString());
}*/					
				} else {
					for (NerdEntity entity : workingQuery.getEntities()) {
						entity.setNerdScore(entity.getNer_conf());
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
		}
		return workingQuery.getEntities();
	}

	private static List<NerdEntity> processDocumentPart(SortedSet<DocumentPiece> documentParts, 
												Document doc,
												NerdContext documentContext,
                                                NerdQuery workingQuery) {
		List<LayoutToken> tokenizationParts = doc.getTokenizationParts(documentParts, doc.getTokenizations());
		return processLayoutTokenSequence(tokenizationParts, documentContext, workingQuery);
	}

	/**
	 * @return
	 */
	public static String methodLogIn() {
		return ">> " + NerdRestProcessFile.class.getName() + "." + 
			Thread.currentThread().getStackTrace()[1].getMethodName();
	}

	/**
	 * @return
	 */
	public static String methodLogOut() {
		return "<< " + NerdRestProcessFile.class.getName() + "." + 
			Thread.currentThread().getStackTrace()[1].getMethodName();
	}

}
