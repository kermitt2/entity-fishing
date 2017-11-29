package com.scienceminer.nerd.evaluation;

import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.mention.Mention;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.utilities.Utilities;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
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
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

public class EvaluationDataGeneration {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationDataGeneration.class);

    public EvaluationDataGeneration() {
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
                final Map<String, String> output = extractPDFContent(evalFile);
                String fileContent = output.get("TEXT");
                String lang = output.get("LANG");

                final String outputPath = corpusPathRawTexts + File.separator
                        + FilenameUtils.removeExtension(evalFile.getName()) + "." + lang + ".txt";
                File outputFile = new File(outputPath);
                if (outputFile.exists()) {
                    LOGGER.warn("The file " + outputPath + " exists. Skipping it.");
                } else {
                    try {
                        LOGGER.info("Writing: " + outputFile.getAbsolutePath());
                        FileUtils.writeStringToFile(outputFile, fileContent, UTF_8);
                    } catch (IOException e) {
                        throw new NerdException("Cannot write file " + outputFile.getAbsolutePath(), e);
                    }
                }
            }

        }

        // Fetch the txt files and create the XML file  with annotations
        Collection<File> evalTxtFiles = FileUtils.listFiles(new File(corpusPathRawTexts),
                new SuffixFileFilter(".txt", IOCase.INSENSITIVE), null);

        Collection<File> orderedEvalTxtFiles = evalTxtFiles.stream()
                .sorted(Comparator.comparing(o -> o.getName()))
                .collect(Collectors.toList());

        LOGGER.info("Writing: " + corpusRefFile);
        FileWriter corpusRefWriter = null;
        try

        {
            corpusRefWriter = new FileWriter(corpusRefFile);
            corpusRefWriter.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>").append("\n");
            corpusRefWriter.append("<" + corpus + ".entityAnnotation>").append("\n");

            for (File evalTxtFile : orderedEvalTxtFiles) {
                ProcessText textProcessor = ProcessText.getInstance();
                List<NerdEntity> entities = new ArrayList<>();

                String filename = FilenameUtils.removeExtension(evalTxtFile.getName());

                Language language = new Language("en");
                final String[] split = filename.split("\\.");
                try {
                    final String langId = split[split.length - 1];
                    language.setLang(langId);
                } catch (ArrayIndexOutOfBoundsException aio) {
                    LOGGER.warn("No language specified in filename, defaulting to EN");
                }

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

                NerdEngine engine = NerdEngine.getInstance();
                final List<NerdEntity> processedEntities = engine.disambiguate(query);
                final String docName = evalTxtFile.getName().toString();

                try {
                    corpusRefWriter.append("\t").append("<document docName=\"" + docName + "\">").append("\n");
                    final StringBuilder sbDocument = new StringBuilder();
                    processedEntities.stream().forEach(e -> {
                        sbDocument.append("\t\t").append("<annotation>").append("\n");
                        sbDocument.append("\t\t\t").append("<mention>").append(escapeHtml4(e.getRawName())).append("</mention>").append("\n");
                        sbDocument.append("\t\t\t").append("<wikiName>").append(escapeHtml4(e.getNormalisedName())).append("</wikiName>").append("\n");
                        sbDocument.append("\t\t\t").append("<wikidataId>").append(e.getWikidataId()).append("</wikidataId>").append("\n");
                        sbDocument.append("\t\t\t").append("<wikipediaId>").append(String.valueOf(e.getWikipediaExternalRef())).append("</wikipediaId>").append("\n");
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
     * LANG -> the language of the PDF
     * TEXT -> the content of the PDF
     */
    public Map<String, String> extractPDFContent(File originFile) {

        LOGGER.info("Processing " + originFile.getAbsolutePath());

        //It's crappy... I know but it's the quicker way to get the language and the text out
        Map<String, String> result = new HashMap<>();
        result.put("LANG", "en"); // default language

        StringBuilder sb = new StringBuilder();
        Engine engine = GrobidFactory.getInstance().getEngine();
        Document doc = null;
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().consolidateHeader(true).build();

        try {
            DocumentSource documentSource = DocumentSource.fromPdf(originFile, config.getStartPage(), config.getEndPage());
            doc = engine.getParsers().getSegmentationParser().processing(documentSource, config);

            // from the header, we are interested in title, abstract and keywords
            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(SegmentationLabels.HEADER);
            if (documentParts != null) {
                String header = engine.getParsers().getHeaderParser().getSectionHeaderFeatured(doc, documentParts, true);


                List<LayoutToken> tokenizationHeader =
                        doc.getTokenizationParts(documentParts, doc.getTokenizations());
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
                            tokenizationHeader, resHeaderLangIdentification);

                    Language lang = identifyLanguage(resHeaderLangIdentification, doc);
                    if (lang != null) {
                        result.put("LANG", lang.getLang());
                    } else {
                        LOGGER.error("Language was not specified and there was not enough text to identify it. The process might fail. ");
                    }

                    // title
                    List<LayoutToken> titleTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_TITLE);
                    if (titleTokens != null) {
                        LOGGER.info("Process title... ");

                        sb.append(LayoutTokensUtil.normalizeDehyphenizeText(titleTokens));
                        sb.append("\n");

                    }

                    // abstract
                    List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                    if (abstractTokens != null) {
                        LOGGER.info("Process abstract...");

                        sb.append(LayoutTokensUtil.normalizeDehyphenizeText(abstractTokens));
                        sb.append("\n");
                    }
                }
            }

            // we can process all the body
            documentParts = doc.getDocumentPart(SegmentationLabels.BODY);
            if (documentParts != null) {
                LOGGER.info("Process body...");
                // full text processing
                Pair<String, LayoutTokenization> featSeg = engine.getParsers().getFullTextParser().getBodyTextFeatured(doc, documentParts);
                if (featSeg != null) {
                    // if featSeg is null, it usually means that no body segment is found in the
                    // document segmentation
                    String bodytext = featSeg.getA();

                    LayoutTokenization tokenizationBody = featSeg.getB();
                    String labeledResult = null;
                    if ((bodytext != null) && (bodytext.trim().length() > 0)) {
                        labeledResult = engine.getParsers().getFullTextParser().label(bodytext);
                    } else {
                        LOGGER.debug("Fulltext model: The input to the CRF processing is empty");
                    }

                    // limit to text only
                    List<TaggingLabel> toProcess = Arrays.asList(TaggingLabels.PARAGRAPH, TaggingLabels.ITEM,
                            TaggingLabels.SECTION);
                    List<LayoutTokenization> documentBodyTokens =
                            FullTextParser.getDocumentFullTextTokens(toProcess, labeledResult, tokenizationBody.getTokenization());

                    if (documentBodyTokens != null) {
                        for (LayoutTokenization layoutTokenization : documentBodyTokens) {
                            List<LayoutToken> layoutTokens = layoutTokenization.getTokenization();
                            sb.append(LayoutTokensUtil.normalizeDehyphenizeText(layoutTokens));
                        }
                        sb.append("\n");
                    }
                }
            }
        } catch (Exception e) {
            throw new NerdException("Something is wrong. ", e);
        }

        result.put("TEXT", sb.toString());

        return result;
    }


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: command [name_of_corpus]");
            System.err.println("corpus must be one of: " + NEDCorpusEvaluation.corpora.toString());
            System.exit(-1);
        }
        String corpus = args[0].toLowerCase();
        if (!NEDCorpusEvaluation.corpora.contains(corpus)) {
            System.err.println("corpus must be one of: " + NEDCorpusEvaluation.corpora.toString());
            System.exit(-1);
        }

        EvaluationDataGeneration nedEval = new EvaluationDataGeneration();
        nedEval.generate(corpus);
    }
}
