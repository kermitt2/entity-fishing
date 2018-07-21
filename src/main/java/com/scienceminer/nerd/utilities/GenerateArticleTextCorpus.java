package com.scienceminer.nerd.utilities;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.grobid.core.utilities.UnicodeUtil;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.utilities.mediaWiki.MediaWikiParser;
import com.scienceminer.nerd.exceptions.*;

/**
 * Utility for generating a text corpus corresponding to the complete set of 
 * Wikipedia articles available for a given language, in a clean and normalized 
 * format for training embeddings. 
 *
 * Command for creating description:
 * mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.utilities.GenerateArticleTextCorpus 
 * -Dexec.args="en /mnt/data/wikipedia/embeddings/wiki.en.text"
 *
 */
public class GenerateArticleTextCorpus {

    public String lang = null;
    public File outputPath = null;

    public GenerateArticleTextCorpus(String lang, File outputFile) {
        this.lang = lang;
        this.outputPath = outputFile;
    }

    public void process() throws NerdResourceException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outputPath));
        } catch(Exception e) {
            throw new NerdException("Opening output file failed, path or right might be invalid.", e);
        }
        UpperKnowledgeBase upperKB = null;
        try {
            upperKB = UpperKnowledgeBase.getInstance();
        }
        catch(Exception e) {
            throw new NerdResourceException("Error instanciating the upper knowledge base. ", e);
        }
        LowerKnowledgeBase wikipedia = upperKB.getWikipediaConf(lang);
        if (wikipedia == null) {
            throw new NerdResourceException(
                "Error instanciating the lower knowledge base. Mostlikely the language is not supported or not loaded: " + lang);
        }
        wikipedia.loadFullContentDB();
        PageIterator ite = wikipedia.getPageIterator(PageType.article);
        try {
            while (ite.hasNext()) {
                Page page = ite.next();
                if (page.getType() != PageType.article)
                    continue;
                Article article = (Article)page;
                String wikiText = article.getFullWikiText();
                if (wikiText == null)
                    continue;
                String content = normaliseDescription(wikiText, false, false, lang);
                writer.write(content);
                writer.write("\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch(Exception e) {
                throw new NerdException("Something went wrong when closing the output file.", e);
            }
        }
    }

    /**
     * Normalise wikimedia texts according to embedding training requirements which
     * is a simple sequence of words.
     */
    private static String normaliseDescription(String wikiText, boolean lowercase, boolean removePunctuation, String lang) {
        String text = MediaWikiParser.getInstance().toTextOnly(wikiText, lang);
        text = text.replace("\t", " ");

        /* following fastText-type string normalisation: 
           1. All punctuation-and-numeric. Things in this bucket get
              their numbers flattened, to prevent combinatorial explosions.
              They might be specific numbers, prices, etc.
              -> all numerical chars are actually all transformed to '0'
              punctuations are removed if parameter removePunctuation is true
           2. All letters: case-flattened if parameter lowercase is true.
           3. Mixed letters and numbers: e.g. a product ID? Flatten case if 
              parameter lowercase is true and transform numbers digit to 0.
        */

        // unicode normalization
        text = UnicodeUtil.normaliseText(text);

        // wikipedia unicode encoding to Java encoding
        // <U+00AD> -> \u00AD
        //text.replaceAll();

        // remove all xml scories
        text = text.replaceAll("<[^>]+>", " ");

        // remove all punctuation
        if (removePunctuation)
            text = text.replaceAll("\\p{P}", " ");

        // flatten numerical chars
        text = text.replaceAll("\\d", "0");

        text = text.replaceAll("\\|", " ");

        // lower case everything 
        if (lowercase)  
            text = text.toLowerCase();

        // collapse spaces amd clean
        text = StringUtils.normalizeSpace(text);
        text = text.replace("()", "");

        // remove stopword - this could be made optional, depending on the task
        // tokenize
        /*List<String> tokens = GrobidAnalyzer.getInstance().tokenize(text, new Language(lang, 1.0));
        StringBuilder textBuilder = new StringBuilder();
        for(String word : tokens) {
            try {
                if (!Stopwords.getInstance().isStopword(word, lang))
                    textBuilder.append(word).append(" ");
            } catch(Exception e) {
                LOGGER.warn("Problem getting Stopwords instance", e);
                textBuilder.append(word).append(" ");
            }
        }*/

        //return textBuilder.toString().replaceAll("( )*"," ").trim();
        return StringUtils.normalizeSpace(text);
    }

    public static void main(String args[]) throws Exception {
        if (args.length != 2) {
            System.out.println("usage: command lang outputPath");
            System.exit(-1);
        }

        String lang = args[0];
        List<String> languages = Arrays.asList("en", "fr", "de", "es", "it");;
        if (!languages.contains(lang)) {
            System.out.println("unsupported language, must be one of " + languages.toString());
            System.exit(-1);
        }

        File outputFile = new File(args[1]);
        File dataDir = outputFile.getParentFile();
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            System.err.println("Invalid output path directory: " + dataDir.getPath());
            System.exit(-1);
        }
        GenerateArticleTextCorpus corpus = new GenerateArticleTextCorpus(lang, outputFile);
        corpus.process();
    }
}