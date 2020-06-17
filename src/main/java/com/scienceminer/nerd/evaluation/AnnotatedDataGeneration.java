package com.scienceminer.nerd.evaluation;

import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.mention.Mention;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.utilities.StringProcessor;
import com.scienceminer.nerd.utilities.Utilities;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.scienceminer.nerd.service.NerdRestProcessFile.identifyLanguage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.text.StringEscapeUtils.escapeXml11;
//import org.apache.commons.lang3.tuple.Pair;

/**
 * This class generate annotated text from raw text exploiting the current models to recognize and 
 * disambiguate entities. The annotated text can be used as training data or evaluation data
 * either in semi-supervised mode or in supervised mode after manual corrections. 
 * The format of the generated documents is the standard NERD corpus with offset annotations in a 
 * separate XML file. 
 */
public class AnnotatedDataGeneration {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotatedDataGeneration.class);

    LowerKnowledgeBase lowerKnowledgeBase = null;

    public AnnotatedDataGeneration() {
        Utilities.initGrobid();
        LibraryLoader.load();
    }

    public void generate(String corpus) {
        String corpusPath = "data/corpus/corpus-long/" + corpus + File.separator;
        String corpusPathRawTexts = "data/corpus/corpus-long/" + corpus + File.separator + "RawText";

        String corpusRefPath = corpusPath + corpus + ".xml";
        File corpusRefFile = new File(corpusRefPath);

        //If the pdf directory exists I take pdfs as source, so the RawText will be filled in with the text before
        // creating the xml file
        if (new File(corpusPath + "pdf").exists() || new File(corpusPath + "PDF").exists()) {
            Collection<File> evalFiles = FileUtils.listFiles(new File(corpusPath + "pdf"),
                    new SuffixFileFilter(".pdf", IOCase.INSENSITIVE), null);

            if (evalFiles.size() == 0) {
                evalFiles = FileUtils.listFiles(new File(corpusPath + "PDF"),
                        new SuffixFileFilter(".pdf", IOCase.INSENSITIVE), null);
            }

            for (File evalFile : evalFiles) {
                final Pair<String, List<String>> output = extractPDFContent(evalFile);
                List<String> fileContent = output.b;
                String lang = output.a;
                final String outputPathPrefix = corpusPathRawTexts + File.separator
                        + FilenameUtils.removeExtension(evalFile.getName());

                int idx = 0;
                for (String singleFile : fileContent) {
                    String outputPath = outputPathPrefix + "." + idx + "." + lang + ".txt";

                    File outputFile = new File(outputPath);
                    if (outputFile.exists()) {
                        LOGGER.warn("The fSile " + outputPath + " exists. Skipping it.");
                    } else {
                        try {
                            LOGGER.info("Writing: " + outputFile.getAbsolutePath());
                            FileUtils.writeStringToFile(outputFile, singleFile, UTF_8);
                        } catch (IOException e) {
                            throw new NerdException("Cannot write file " + outputFile.getAbsolutePath(), e);
                        }
                    }
                    idx++;
                }
            }

        }

        // read the txt files and create the XML file  with annotations
        Collection<File> evalTxtFiles = FileUtils.listFiles(new File(corpusPathRawTexts),
                new SuffixFileFilter(".txt", IOCase.INSENSITIVE), null);

        Collection<File> orderedEvalTxtFiles = evalTxtFiles.stream()
                .sorted(Comparator.comparing(o -> o.getName()))
                .collect(Collectors.toList());

        LOGGER.info("Writing: " + corpusRefFile);
        FileWriter corpusRefWriter = null;
        try {
            corpusRefWriter = new FileWriter(corpusRefFile);
            corpusRefWriter.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>").append("\n");
            corpusRefWriter.append("<" + corpus + ".entityAnnotation>").append("\n");

            for (File evalTxtFile : orderedEvalTxtFiles) {
                ProcessText textProcessor = ProcessText.getInstance();
                List<NerdEntity> entities = new ArrayList<>();

                // call the method to recognize language of every evalTxtFile
                String langId = NEDCorpusEvaluation.recognizeLanguage(evalTxtFile);
                Language language = new Language(langId);
                language.setLang(langId);

                lowerKnowledgeBase = UpperKnowledgeBase.getInstance().getWikipediaConf(language.getLang());

                String text = null;
                try {
                    text = FileUtils.readFileToString(evalTxtFile, UTF_8);
                } catch (IOException e) {
                    LOGGER.warn("cannot read file " + evalTxtFile + ". Skipping it.", e);
                    continue;
                }
                List<Mention> nerMentions = textProcessor.processNER(text, language);
                nerMentions.stream().forEach(m -> entities.add(new NerdEntity(m)));

                List<Mention> wikipediaMentions = textProcessor.processWikipedia(text, language);
                wikipediaMentions.stream().forEach(wm -> {
                    final NerdEntity nerdEntity = new NerdEntity(wm);
                    if (!entities.contains(nerdEntity)) {
                        entities.add(nerdEntity);
                    }
                });

                NerdQuery query = new NerdQuery();
                query.setText(text);
                query.setEntities(entities);
                query.setLanguage(language);
                query.setMinRankerScore(0.2);

                NerdEngine engine = NerdEngine.getInstance();
                final List<NerdEntity> processedEntities = engine.disambiguate(query);
                final String docName = evalTxtFile.getName().toString();

                try {
                    corpusRefWriter.append("\t").append("<document docName=\"" + docName + "\">").append("\n");
                    final StringBuilder sbDocument = new StringBuilder();
                    processedEntities.stream().forEach(e -> {
                        sbDocument.append("\t\t").append("<annotation>").append("\n");
                        sbDocument.append("\t\t\t").append("<mention>").append(escapeXml11(e.getRawName())).append("</mention>").append("\n");

                        final int wikipediaExternalRef = e.getWikipediaExternalRef();

                        String title = null;
                        final Page page = lowerKnowledgeBase.getPageById(wikipediaExternalRef);
                        if (page.getId() != -1) {
                            title = page.getTitle();
                        }

                        sbDocument.append("\t\t\t").append("<wikiName>").append(escapeXml11(title)).append("</wikiName>").append("\n");
                        sbDocument.append("\t\t\t").append("<wikidataId>").append(e.getWikidataId()).append("</wikidataId>").append("\n");
                        sbDocument.append("\t\t\t").append("<wikipediaId>").append(String.valueOf(wikipediaExternalRef)).append("</wikipediaId>").append("\n");
                        sbDocument.append("\t\t\t").append("<offset>").append(String.valueOf(e.getOffsetStart())).append("</offset>").append("\n");
                        sbDocument.append("\t\t\t").append("<length>").append(String.valueOf(e.getRawName().length())).append("</length>").append("\n");
                        sbDocument.append("\t\t").append("</annotation>").append("\n");
                    });

                    corpusRefWriter.append(sbDocument.toString());
                    corpusRefWriter.append("\t").append("</document>").append("\n");
                    corpusRefWriter.flush();
                } catch (IOException ioe) {
                    LOGGER.warn("Writing error. Skipping document: " + docName);
                }

            }

            corpusRefWriter.append("</" + corpus + ".entityAnnotation>").append("\n");

        } catch (IOException e) {
            throw new NerdException("Cannot write file " + corpusRefFile, e);
        } finally {
            IOUtils.closeQuietly(corpusRefWriter);
        }

    }

    /**
     * This method returns a map with two items inside,
     * Pair.a -> the language of the PDF
     * Pair.b -> the content of the PDF
     */
    public Pair<String, List<String>> extractPDFContent(File originFile) {

        LOGGER.info("Processing " + originFile.getAbsolutePath());
        List<String> resultingDocuments = new ArrayList<>();

        //It's crappy... I know but it's the quicker way to get the language and the text out
        String language = "en";

        StringBuilder sb = new StringBuilder();
        Engine engine = GrobidFactory.getInstance().getEngine();
        Document doc = null;
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().consolidateHeader(1).build();

        try {
            DocumentSource documentSource = DocumentSource.fromPdf(originFile, config.getStartPage(), config.getEndPage());
            doc = engine.getParsers().getSegmentationParser().processing(documentSource, config);

            // from the header, we are interested in title, abstract and keywords
            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(SegmentationLabels.HEADER);
            if (documentParts != null) {
                org.apache.commons.lang3.tuple.Pair<String,List<LayoutToken>> headerFeatured = engine.getParsers().getHeaderParser().getSectionHeaderFeatured(doc, documentParts, true);
                String header = headerFeatured.getLeft();

                List<LayoutToken> tokenizationHeader = doc.getTokenizationParts(documentParts, doc.getTokenizations());
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

                    BiblioItem resHeaderLangIdentification = new BiblioItem();
                    engine.getParsers().getHeaderParser().resultExtraction(labeledResult, true,
                            tokenizationHeader, resHeaderLangIdentification, doc);

                    Language lang = identifyLanguage(resHeaderLangIdentification, doc);
                    if (lang != null) {
                        language = lang.getLang();
                    } else {
                        LOGGER.error("Language was not specified and there was not enough text to identify it. The process might fail. ");
                    }

                    // title
                    List<LayoutToken> titleTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_TITLE);
                    if (titleTokens != null) {
                        LOGGER.info("Process title... ");

                        sb.append(StringProcessor.removeInvalidUtf8Chars(LayoutTokensUtil.normalizeDehyphenizeText(titleTokens)));
                        sb.append("\n");
                        sb.append("\n");
                    }

                    // abstract
                    List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                    if (abstractTokens != null) {
                        LOGGER.info("Process abstract...");

                        sb.append(StringProcessor.removeInvalidUtf8Chars(LayoutTokensUtil.normalizeDehyphenizeText(abstractTokens)));
                        sb.append("\n");
                        sb.append("\n");
                    }
                }
            }

            // we can process all the body
            documentParts = doc.getDocumentPart(SegmentationLabels.BODY);
            if (documentParts != null) {
                LOGGER.info("Process body...");
                // full text processing
                org.apache.commons.lang3.tuple.Pair<String, LayoutTokenization> featSeg = 
                    engine.getParsers().getFullTextParser().getBodyTextFeatured(doc, documentParts);
                if (featSeg != null) {
                    // if featSeg is null, it usually means that no body segment is found in the
                    // document segmentation
                    String bodytext = featSeg.getLeft();

                    LayoutTokenization tokenizationBody = featSeg.getRight();
                    String labeledResult = null;
                    if ((bodytext != null) && (bodytext.trim().length() > 0)) {
                        labeledResult = engine.getParsers().getFullTextParser().label(bodytext);
                    } else {
                        LOGGER.debug("Fulltext model: The input to the CRF processing is empty");
                    }

                    // limit to text only
                    List<TaggingLabel> toProcess = Arrays.asList(TaggingLabels.PARAGRAPH, TaggingLabels.ITEM,
                            TaggingLabels.SECTION);

                    List<TaggingLabel> ignoredLabels = Arrays.asList(TaggingLabels.EQUATION_LABEL, TaggingLabels.FIGURE,
                            TaggingLabels.TABLE);

                    //Labels to be ignored but their encountering doesn't means the end of the current label.
                    // e.g. a citation marker within a paragraph doesn't means there is a new paragraph
                    List<TaggingLabel> interruptingLabels = Arrays.asList(TaggingLabels.CITATION_MARKER,
                            TaggingLabels.FIGURE_MARKER, TaggingLabels.TABLE_MARKER,
                            TaggingLabels.EQUATION_MARKER, TaggingLabels.EQUATION_LABEL);

                    TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, labeledResult, tokenizationBody.getTokenization());
                    List<TaggingTokenCluster> clusters = clusteror.cluster();
                    TaggingLabel previousLabel = null;
                    int wordsCounter = 0;
                    boolean newData = false;

                    for (TaggingTokenCluster cluster : clusters) {
                        if (cluster == null) {
                            continue;
                        }

                        TaggingLabel clusterLabel = cluster.getTaggingLabel();
                        String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());

                        if (toProcess.contains(clusterLabel)) {
                            wordsCounter += cluster.getLabeledTokensContainers().size();

                            if (clusterContent.length() > 10) {
                                if (previousLabel != null && !interruptingLabels.contains(previousLabel)) {
                                    sb.append("\n");
                                    sb.append("\n");
                                }
                                sb.append(postProcess(clusterContent));
//                                sb.append(clusterContent);

                                newData = true;
                            }
                        }
                        previousLabel = clusterLabel;

                        if (wordsCounter > 1000) {
                            resultingDocuments.add(StringProcessor.removeInvalidUtf8Chars(StringUtils.trim(sb.toString())));
                            sb = new StringBuilder();
                            previousLabel = null;
                            wordsCounter = 0;
                            newData = false;
                        }
                    }
                    if (newData)
                        resultingDocuments.add(StringProcessor.removeInvalidUtf8Chars(StringUtils.trim(sb.toString())));
                }
            }
        } catch (Exception e) {
            throw new NerdException("PDF process failed", e);
        }

        Pair<String, List<String>> result = new Pair(language, resultingDocuments);

        return result;
    }

    protected String postProcess(String string) {
        return StringUtils.trim(StringUtils.replaceAll(string, "^\\.", ""));
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: command [name_of_corpus]");
            System.exit(-1);
        }
        String corpus = args[0].toLowerCase();

        AnnotatedDataGeneration nedEval = new AnnotatedDataGeneration();
        nedEval.generate(corpus);
    }
}
