package com.scienceminer.nerd.evaluation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.Utilities;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
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
import java.io.IOException;
import java.util.*;

import static com.scienceminer.nerd.service.NerdRestProcessFile.identifyLanguage;
import static java.nio.charset.StandardCharsets.UTF_8;

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
                String fileContent = extractPDFContent(evalFile).get("TEXT");
                String lang = extractPDFContent(evalFile).get("LANG");

                final String outputPath = corpusPathRawTexts + File.separator
                        + FilenameUtils.removeExtension(evalFile.getName()) + "." + lang + ".txt";
                File outputFile = new File(outputPath);
                if (outputFile.exists()) {
                    LOGGER.warn("The file " + outputPath + " exists. Skipping it.");
                    continue;
                }

                try {
                    LOGGER.info("Writing: " + outputFile.getAbsolutePath());
                    FileUtils.writeStringToFile(outputFile, fileContent, UTF_8);
                } catch (IOException e) {
                    throw new NerdException("Cannot write file " + outputFile.getAbsolutePath(), e);
                }
            }


        }
        // here we process starting from the TXT files.

    }

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

                        sb.append(LayoutTokensUtil.toText(titleTokens));
                        sb.append("\n");

                    }

                    // abstract
                    List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                    if (abstractTokens != null) {
                        LOGGER.info("Process abstract...");

                        sb.append(LayoutTokensUtil.toText(abstractTokens));
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
                            sb.append(LayoutTokensUtil.toText(layoutTokens));
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
