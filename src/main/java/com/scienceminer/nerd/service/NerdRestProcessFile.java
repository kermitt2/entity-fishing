package com.scienceminer.nerd.service;

import com.scienceminer.nerd.disambiguation.DocumentContext;
import com.scienceminer.nerd.disambiguation.NerdContext;
import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.kb.Property;
import com.scienceminer.nerd.main.data.SoftwareInfo;
import com.scienceminer.nerd.mention.Mention;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.utilities.Filter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.FullTextParser;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import static com.scienceminer.nerd.utilities.StringProcessor.isAllUpperCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import org.apache.commons.lang3.tuple.Pair;

public class NerdRestProcessFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestProcessFile.class);
    NerdRestProcessQuery nerdRestProcessQuery = new NerdRestProcessQuery();
    SoftwareInfo softwareInfo = SoftwareInfo.getInstance();

    /**
     * Parse a structured query in combination with a PDF file and return the corresponding
     * normalized enriched and disambiguated query object, where resulting entities include
     * position coordinates in the PDF.
     *
     * @param theQuery    the POJO query object
     * @param inputStream the PDF file as InputStream
     * @return a response query object containing the structured representation of
     * the enriched and disambiguated query.
     */
    public String processQueryAndPdfFile(String theQuery, final InputStream inputStream) {
        LOGGER.debug(methodLogIn());
        File originFile = null;
        Engine engine = null;
        LOGGER.debug(">> received query to process: " + theQuery);

        LibraryLoader.load();
        engine = GrobidFactory.getInstance().getEngine();
        originFile = IOUtilities.writeInputFile(inputStream);
        LOGGER.debug(">> input PDF file saved locally...");

        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().consolidateHeader(1).build();
        if (originFile == null || FileUtils.sizeOf(originFile) == 0) {
            throw new QueryException("The PDF file is empty or null", QueryException.FILE_ISSUE);
        }
        long start = System.currentTimeMillis();
        NerdQuery nerdQuery = NerdQuery.fromJson(theQuery);

        if (nerdQuery == null || isNotBlank(nerdQuery.getText()) || isNotBlank(nerdQuery.getShortText())) {
            throw new QueryException("Query with PDF shall not have the field text or shortText filled in.");
        }
        LOGGER.debug(">> set query object...");

        Language lang = nerdQuery.getLanguage();
        if (nerdQuery.hasValidLanguage()) {
            lang.setConf(1.0);
            LOGGER.debug(">> language provided in query: " + lang);
        } 

        /* the code for validation has been moved in nerdRestProcessQuery.markUserEnteredEntities() */

        // we assume for the moment that there are no entities originally set in the query
        // however, it would be nice to have them specified with coordinates and map
        // them to their right layout tokens when processing the PDF (as done for
        // instance in the project grobid-astro)

//        List<NerdEntity> originalEntities = null;
//        if (CollectionUtils.isNotEmpty(nerdQuery.getEntities())) {
//            markUserEnteredEntities(nerdQuery, nerdQuery.getText().length());
//            originalEntities = nerdQuery.getEntities();
//        }

        // tuning the species only mention selection
        tuneSpeciesMentions(nerdQuery);

        //checking customisation
        NerdRestProcessQuery.processCustomisation(nerdQuery);

        //List<NerdEntity> entities = originalEntities;
        Document doc = null;
        DocumentContext documentContext = new DocumentContext();
        NerdQuery workingQuery = new NerdQuery(nerdQuery);

        DocumentSource documentSource =
                DocumentSource.fromPdf(originFile, config.getStartPage(), config.getEndPage());
        doc = engine.getParsers().getSegmentationParser().processing(documentSource, config);

        // test if we consider or not document structures when fishing entities
        if ((nerdQuery.getStructure() != null) && (nerdQuery.getStructure().equals("default") || nerdQuery.getStructure().equals("full"))) {
            List<LayoutToken> allTokens = doc.getTokenizations();
            if (allTokens != null) {
                if (lang == null) {
                    StringBuilder builder = new StringBuilder();
                    int nbTok = 0;
                    for(LayoutToken token : allTokens) {
                        if (nbTok == 1000)
                            break;
                        builder.append(token.getText());
                        nbTok++;
                    }

                    LanguageUtilities languageIdentifier = LanguageUtilities.getInstance();
                    synchronized (languageIdentifier) {
                        lang = languageIdentifier.runLanguageId(builder.toString(), 2000);
                    }

                    if (lang != null) {
                        workingQuery.setLanguage(lang);
                        nerdQuery.setLanguage(lang);
                    } else {
                        LOGGER.error("Language was not specified and there was not enough text to identify it. The process might fail. ");
                    }
                }

                List<NerdEntity> newEntities = processLayoutTokenSequence(allTokens, null, workingQuery);
                if (newEntities != null) {
                    LOGGER.debug(newEntities.size() + " nerd entities");
                }
                nerdQuery.addNerdEntities(newEntities);
            }
        } else if ( (nerdQuery.getStructure() == null) || (nerdQuery.getStructure() != null && nerdQuery.getStructure().equals("grobid"))) {
            // we applied entirely GROBID article structuring

            // we process the relevant textual content of the document
            // for refining the process based on structures, we need to filter
            // segment of interest (e.g. header, body, annex) and possibly apply
            // the corresponding model to further filter by structure types

            // from the header, we are interested in title, abstract and keywords
            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(SegmentationLabels.HEADER);
            if (documentParts != null) {
                Pair<String,List<LayoutToken>> headerFeatured = engine.getParsers().getHeaderParser().getSectionHeaderFeatured(doc, documentParts, true);
                String header = headerFeatured.getLeft();
                List<LayoutToken> tokenizationHeader =
                        Document.getTokenizationParts(documentParts, doc.getTokenizations());
                String labeledResult = null;

                // alternative
                String alternativeHeader = doc.getHeaderFeatured(true, true);
                // we choose the longest header
                if (StringUtils.isNotBlank(StringUtils.trim(header))) {
                    header = alternativeHeader;
                    tokenizationHeader = doc.getTokenizationsHeader();
                } else if (StringUtils.isNotBlank(StringUtils.trim(alternativeHeader)) && alternativeHeader.length() > header.length()) {
                    header = alternativeHeader;
                    tokenizationHeader = doc.getTokenizationsHeader();
                }

                if (StringUtils.isNotBlank(StringUtils.trim(header))) {
                    labeledResult = engine.getParsers().getHeaderParser().label(header);

                    BiblioItem resHeader = new BiblioItem();
                    resHeader.generalResultMapping(doc, labeledResult, tokenizationHeader);

                    if (lang == null) {
                        BiblioItem resHeaderLangIdentification = new BiblioItem();
                        engine.getParsers().getHeaderParser().resultExtraction(labeledResult, true,
                                tokenizationHeader, resHeaderLangIdentification, doc);

                        lang = identifyLanguage(resHeaderLangIdentification, doc);
                        if (lang != null) {
                            workingQuery.setLanguage(lang);
                            nerdQuery.setLanguage(lang);
                        } else {
                            LOGGER.error("Language was not specified and there was not enough text to identify it. The process might fail. ");
                        }
                    }

                    // title
                    List<LayoutToken> titleTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_TITLE);
                    if (titleTokens != null) {
                        LOGGER.debug("Process title... ");
                        //LOGGER.debug(LayoutTokensUtil.toText(titleTokens));

                        List<NerdEntity> newEntities = processLayoutTokenSequence(titleTokens, null, workingQuery);
                        if (newEntities != null) {
                            LOGGER.debug(newEntities.size() + " nerd entities");
                            /*for(NerdEntity entity : newEntities) {
                                LOGGER.debug(entity.toString());
                            }*/
                        }
                        nerdQuery.addNerdEntities(newEntities);
                    }
                    //LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
                    //LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");

                    // abstract
                    List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                    if (abstractTokens != null) {
                        LOGGER.debug("Process abstract...");
                        //workingQuery.setEntities(null);
                        List<NerdEntity> newEntities = processLayoutTokenSequence(abstractTokens, null, workingQuery);
                        if (newEntities != null) {
                            LOGGER.debug(newEntities.size() + " nerd entities");
                        }

                        nerdQuery.addNerdEntities(newEntities);
                    }
                    //LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
                    //LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");

                    // keywords
                    List<LayoutToken> keywordTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_KEYWORD);
                    if (keywordTokens != null) {
                        LOGGER.debug("Process keywords...");
                        //workingQuery.setEntities(null);
                        List<NerdEntity> newEntities = processLayoutTokenSequence(keywordTokens, null, workingQuery);
                        if (newEntities != null)
                            LOGGER.debug(newEntities.size() + " nerd entities");
                        nerdQuery.addNerdEntities(newEntities);
                    }

                    // create document context from this first pass
                    documentContext.seed(nerdQuery.getEntities(), lang);
                    nerdQuery.setEntities(null);

                    // as alternative, we should use key phrase extraction and disambiguation on the whole document
                    // to seed the document context

                    // reprocess header fields with document context
                    if (titleTokens != null) {
                        //workingQuery.setEntities(null);
                        List<NerdEntity> newEntities = processLayoutTokenSequence(titleTokens, documentContext, workingQuery);
                        if (newEntities != null) {
                            LOGGER.debug(newEntities.size() + " nerd entities");
                            for (NerdEntity entity : newEntities) {
                                LOGGER.debug(entity.toString());
                            }
                        }
                        nerdQuery.addNerdEntities(newEntities);
                    }
    //LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
    //LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");
                    if (abstractTokens != null) {
                        //workingQuery.setEntities(null);
                        List<NerdEntity> newEntities = processLayoutTokenSequence(abstractTokens, documentContext, workingQuery);
                        nerdQuery.addNerdEntities(newEntities);
                    }
    //LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
    //LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");
                    if (keywordTokens != null) {
                        //workingQuery.setEntities(null);
                        List<NerdEntity> newEntities = processLayoutTokenSequence(keywordTokens, documentContext, workingQuery);
                        nerdQuery.addNerdEntities(newEntities);
                    }
    //LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
    //LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");
                }
            }

            // we can process all the body, in the future figure and table could be the
            // object of more refined processing
            documentParts = doc.getDocumentPart(SegmentationLabels.BODY);
            if (documentParts != null) {
                LOGGER.debug("Process body...");
                // full text processing
                Pair<String, LayoutTokenization> featSeg = FullTextParser.getBodyTextFeatured(doc, documentParts);
                if (featSeg != null) {
                    // if featSeg is null, it usually means that no body segment is found in the
                    // document segmentation
                    String bodytext = featSeg.getLeft();

                    LayoutTokenization tokenizationBody = featSeg.getRight();
                    String rese = null;
                    if ((bodytext != null) && (bodytext.trim().length() > 0)) {
                        rese = engine.getParsers().getFullTextParser().label(bodytext);

                        // get out the reference, figure, table and formula markers, plus the formula
                        // the rest can be processed by NERD
                        List<TaggingLabel> toProcess = Arrays.asList(TaggingLabels.PARAGRAPH, TaggingLabels.ITEM,
                                TaggingLabels.SECTION, TaggingLabels.FIGURE, TaggingLabels.TABLE);
                        List<LayoutTokenization> documentBodyTokens =
                                FullTextParser.getDocumentFullTextTokens(toProcess, rese, tokenizationBody.getTokenization());

                        if (documentBodyTokens != null) {
                            List<NerdEntity> newEntities =
                                    processLayoutTokenSequences(documentBodyTokens, documentContext, workingQuery);
                            nerdQuery.addNerdEntities(newEntities);
                        } else
                            LOGGER.debug("no body part?!?");
                    } else {
                        LOGGER.debug("Fulltext model: The input to the CRF processing is empty");
                    }
                }
            }
            //LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
            //LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");
            // we process references if required
            if (nerdQuery.getMentions().contains(ProcessText.MentionMethod.grobid)) {
                List<BibDataSet> resCitations = engine.getParsers().getCitationParser().
                        processingReferenceSection(doc, engine.getParsers().getReferenceSegmenterParser(), 1);
                if ((resCitations != null) && (resCitations.size() > 0)) {
                    List<NerdEntity> newEntities = processCitations(resCitations, doc, workingQuery);
                    if (newEntities != null)
                        LOGGER.debug(newEntities.size() + " citation entities");
                    nerdQuery.addNerdEntities(newEntities);
                }
            }
            //LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
            //LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");
            // acknowledgement
            documentParts = doc.getDocumentPart(SegmentationLabels.ACKNOWLEDGEMENT);
            if (documentParts != null) {
                LOGGER.debug("Process acknowledgement...");
                workingQuery.setEntities(null);
                List<NerdEntity> newEntities = processDocumentPart(documentParts, doc, documentContext, workingQuery);
                if (newEntities != null)
                    LOGGER.debug(newEntities.size() + " nerd entities");
                nerdQuery.addNerdEntities(newEntities);
            }
            //LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
            //LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");
            // we can process annexes
            documentParts = doc.getDocumentPart(SegmentationLabels.ANNEX);
            if (documentParts != null) {
                LOGGER.debug("Process annex...");
                //workingQuery.setEntities(null);
                List<NerdEntity> newEntities = processDocumentPart(documentParts, doc, documentContext, workingQuery);
                if (newEntities != null)
                    LOGGER.debug(newEntities.size() + " nerd entities");
                nerdQuery.addNerdEntities(newEntities);
            }
            //LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
            //LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");
            // footnotes are also relevant
            documentParts = doc.getDocumentPart(SegmentationLabels.FOOTNOTE);
            if (documentParts != null) {
                LOGGER.debug("Process footnotes...");
                //workingQuery.setEntities(null);
                List<NerdEntity> newEntities = processDocumentPart(documentParts, doc, documentContext, workingQuery);
                if (newEntities != null)
                    LOGGER.debug(newEntities.size() + " nerd entities");
                nerdQuery.addNerdEntities(newEntities);
            }

            // we assume for the moment that there are no entities originally set in the query
            // but the following deals with that
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
        } else {
            // the value of the parameter structure is nto supported
            throw new QueryException("The value of the query parameter \"structure\" is not supported fot the PDF input: " + 
                nerdQuery.getStructure());
        }

        nerdQuery.setText(null);
        nerdQuery.setShortText(null);
        nerdQuery.setTokens(null);

        long end = System.currentTimeMillis();
        IOUtilities.removeTempFile(originFile);
        nerdQuery.setRuntime(end - start);
        // for metadata
        nerdQuery.setSoftware(softwareInfo.getName());
        nerdQuery.setVersion(softwareInfo.getVersion());
        nerdQuery.setDate(java.time.Clock.systemUTC().instant().toString());

        LOGGER.info("runtime: " + (end - start));
        if (CollectionUtils.isNotEmpty(nerdQuery.getEntities())) {
            Collections.sort(nerdQuery.getEntities());
            LOGGER.debug(nerdQuery.getEntities().size() + " nerd entities in NerdQuery");
//                LOGGER.debug(workingQuery.getEntities().size() + " nerd entities in workingQuery");
        }

        LOGGER.debug(methodLogOut());
        // TODO: output in the resulting json also page info from the doc object as in GROBID
        return nerdQuery.toJSONClean(doc);
    }


    //TODO: we should move it downstream
    public static void tuneSpeciesMentions(NerdQuery nerdQuery) {
        if (nerdQuery.getMentions().contains(ProcessText.MentionMethod.species) &&
                nerdQuery.getMentions().size() == 1) {
            nerdQuery.addMention(ProcessText.MentionMethod.wikipedia);
            Filter speciesFilter = new Filter();
            Property speciesProperty = new Property();
            speciesProperty.setId("P225");
            speciesFilter.setProperty(speciesProperty);
            nerdQuery.setFilter(speciesFilter);
        }
    }

    public static Language identifyLanguage(BiblioItem resHeader, Document doc) {
        StringBuilder contentSample = new StringBuilder();
        if (resHeader.getTitle() != null) {
            contentSample.append(resHeader.getTitle());
        }
        if (resHeader.getAbstract() != null) {
            contentSample.append("\n");
            contentSample.append(resHeader.getAbstract());
        }
        if (resHeader.getKeywords() != null) {
            contentSample.append("\n");
            contentSample.append(resHeader.getKeywords());
        }
        if (contentSample.length() < 200) {
            // we need more textual content to ensure that the language identification will be
            // correct
            // PL: the whole body, this is violent !
            contentSample.append(doc.getBody());
        }
        LanguageUtilities languageIdentifier = LanguageUtilities.getInstance();

        Language resultLang = null;
        synchronized (languageIdentifier) {
            resultLang = languageIdentifier.runLanguageId(contentSample.toString(), 2000);
        }

        return resultLang;
    }

    /**
     * Generate a global context for a document
     */
    public static NerdContext getGlobalContext(NerdQuery query) {
        // TODO
        return null;
    }


    /**
     * Mark (confidence 1.0) the user defined entities as long as:
     * - they have a valid coordinate set
     * - they have a valid wikipedia or wikidata ID
     **/
//    public void markUserEnteredEntities(NerdQuery nerdQuery) {
//
//        for (NerdEntity entity : nerdQuery.getEntities()) {
//
//            if (entity.getLayoutTokens().get(0).getX()== -1
//                    || entity.getOffsetEnd() == -1
//                    || entity.getOffsetEnd() < entity.getOffsetStart()
//                    || entity.getOffsetEnd() > maxOffsetValue) {
//                LOGGER.warn("The entity " + entity.toJsonCompact() + " doesn't have valid offset. Ignoring it.");
//            } else {
//                entity.setNer_conf(1.0);
//
//                // do we have disambiguated entity information for the entity?
//                if (entity.getWikipediaExternalRef() != -1 || StringUtils.isNotBlank(entity.getWikidataId())) {
//                    entity.setSource(ProcessText.MentionMethod.user);
//                    entity.setNerdScore(1.0);
//                }
//            }
//        }
//    }

    private List<NerdEntity> processLayoutTokenSequences(List<LayoutTokenization> layoutTokenizations,
                                                         NerdContext documentContext,
                                                         NerdQuery workingQuery) {
        // text of the selected segment
        List<NerdEntity> resultingEntities = new ArrayList<>();

        ProcessText processText = ProcessText.getInstance();
        NerdEngine disambiguator = NerdEngine.getInstance();

        for (LayoutTokenization layoutTokenization : layoutTokenizations) {
            List<LayoutToken> layoutTokens = layoutTokenization.getTokenization();

            workingQuery.setEntities(null);
            workingQuery.setText(null);
            workingQuery.setShortText(null);
            workingQuery.setTokens(layoutTokens);
            workingQuery.setContext(documentContext);

            //workingQuery.setMentions(mentions);
            try {
                // ner
                List<Mention> nerEntities = processText.process(workingQuery);

                // TODO: this should not be done at this place for all segments (this is quite costly), 
                // but before when dealing with explicit HEADER_TITLE, or by passing a parameter indicating 
                // that the token sequence is a title - or better a possibly full upper case sequence, 
                // because this is probably relevant to more than just title 
                if (isTitle(layoutTokens) && needToLowerCase(layoutTokens)) {
                    for (Mention nerEntity : nerEntities) {
                        nerEntity.setNormalisedName(lowerCase(nerEntity.getRawName()));
                    }
                }
				/*if (nerEntities != null)
					LOGGER.debug(nerEntities.size() + " ner entities");
				else
					nerEntities = new ArrayList<Mention>();*/

                //if (!workingQuery.getOnlyNER())
				/*{
					List<Mention> entities2 = processText.processWikipedia(workingQuery);
					if (entities2 != null) {
						LOGGER.debug(entities2.size() + " non-ner entities");
						for(Mention entity : entities2) {
							// we add entities only if the mention is not already present
							if (!nerEntities.contains(entity))
								nerEntities.add(entity);
						}
					}
				}*/

                // inject explicit acronyms
                nerEntities = processText.acronymCandidates(workingQuery, nerEntities);

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
                            LOGGER.debug("Empty bounding box for " + entity.toString());
                    }*/

                    // sort the entities
                    Collections.sort(workingQuery.getEntities());
                    // disambiguate and solve entity mentions
                    //if (!workingQuery.getOnlyNER())
                    //{
                    List<NerdEntity> disambiguatedEntities = disambiguator.disambiguate(workingQuery);
                    workingQuery.setEntities(disambiguatedEntities);
/*if (workingQuery.getEntities() != null)
LOGGER.debug(workingQuery.getEntities().size() + " nerd entities");	*/
	/*for (NerdEntity entity : workingQuery.getEntities()) {
		if (entity.getBoundingBoxes() == null)
			LOGGER.debug("Empty bounding box for " + entity.toString());
	}*/
                    //}
//						else {
                    for (NerdEntity entity : workingQuery.getEntities()) {
                        entity.setNerdScore(entity.getNer_conf());
                    }
//					}*/
                }

                resultingEntities.addAll(workingQuery.getEntities());
                // update document context
                if (documentContext != null)
                    ((DocumentContext) documentContext).update(workingQuery);

            } catch (Exception e) {
                LOGGER.error("An unexpected exception occurs when processing layout tokens. ", e);
            }
        }
        workingQuery.setEntities(resultingEntities);
        return workingQuery.getEntities();
    }

    protected boolean isTitle(List<LayoutToken> layoutTokens) {
        int count = 0;
        int total = 0;
        for (LayoutToken layoutToken : layoutTokens) {
            if (!TextUtilities.delimiters.contains(layoutToken.getText())) {
                if (layoutToken.getLabels().contains(TaggingLabels.HEADER_TITLE)) {
                    count++;
                }
                total++;
            }
        }

        return count == total;
    }

    private boolean needToLowerCase(List<LayoutToken> layoutTokens) {
        if (isAllUpperCase(LayoutTokensUtil.toText(layoutTokens))) {
            return true;
        } else {
            int count = 0;
            int total = 0;
            for (LayoutToken token : layoutTokens) {
                final String tokenText = token.getText();
                if (!TextUtilities.fullPunctuations.contains(tokenText)) {
                    total++;

                    if (tokenText.length() == 1) {
                        if (TextUtilities.isAllUpperCase(tokenText)) {
                            count++;
                        }
                    } else if (tokenText.length() > 1) {
                        if (Character.isUpperCase(tokenText.charAt(0))
                                && TextUtilities.isAllLowerCase(tokenText.substring(1, tokenText.length()))) {
                            count++;
                        }
                    }
                }
            }
            if (count == total) {
                return true;
            }
        }
        return false;
    }

    private List<NerdEntity> processLayoutTokenSequence(List<LayoutToken> layoutTokens,
                                                        NerdContext documentContext,
                                                        NerdQuery workingQuery) {
        List<LayoutTokenization> layoutTokenizations = new ArrayList<>();
        layoutTokenizations.add(new LayoutTokenization(layoutTokens));
        return processLayoutTokenSequences(layoutTokenizations, documentContext, workingQuery);
    }

    private List<NerdEntity> processDocumentPart(SortedSet<DocumentPiece> documentParts,
                                                 Document doc,
                                                 NerdContext documentContext,
                                                 NerdQuery workingQuery) {
        List<LayoutToken> tokenizationParts = Document.getTokenizationParts(documentParts, doc.getTokenizations());
        return processLayoutTokenSequence(tokenizationParts, documentContext, workingQuery);
    }

    private List<NerdEntity> processCitations(List<BibDataSet> resCitations,
                                              Document doc,
                                              NerdQuery workingQuery) {
        return NerdEngine.getInstance().solveCitations(resCitations);
    }

    public static String methodLogIn() {
        return ">> " + NerdRestProcessFile.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public static String methodLogOut() {
        return "<< " + NerdRestProcessFile.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }

}
