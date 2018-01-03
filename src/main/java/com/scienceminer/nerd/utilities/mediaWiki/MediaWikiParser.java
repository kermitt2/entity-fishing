package com.scienceminer.nerd.utilities.mediaWiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
//import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Handler for the parser of mediawiki format able, in particular, to modify
 * the mediawiki articles into simpler text formats. 
 * Based on the Sweble parsing framework.
 */
public class MediaWikiParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaWikiParser.class);

    private static volatile MediaWikiParser instance;
    
    // map giving the config following a language code
    private Map<String,WikiConfig> configs = null;

    // map giving a parser engine following a language code
    private Map<String,WtEngineImpl> engines = null;

    public static MediaWikiParser getInstance() {
        if (instance == null) {
			getNewInstance();
 		}
        return instance;
    }

    /**
     * Creates a new instance.
     */
	private static synchronized void getNewInstance() {
		LOGGER.debug("Get new instance of MediaWikiParser");
		instance = new MediaWikiParser();
	}

    /**
     * Hidden constructor
     */
    private MediaWikiParser() {
        // set-up simple wiki configurations
        configs = new HashMap<String,WikiConfig>();

        // set-up language specific parsers
        engines = new HashMap<String,WtEngineImpl>();

        WikiConfig config = DefaultConfigEnWp.generate();
        configs.put("en", config);
        WtEngineImpl engine = new WtEngineImpl(config);        
        engines.put("en", engine);

        config = DefaultConfigFrWp.generate();
        configs.put("fr", config);
        engine = new WtEngineImpl(config);        
        engines.put("fr", engine);

        config = DefaultConfigDeWp.generate();
        configs.put("de", config);
        engine = new WtEngineImpl(config);        
        engines.put("de", engine);

        config = DefaultConfigItWp.generate();
        configs.put("it", config);
        engine = new WtEngineImpl(config);        
        engines.put("it", engine);

        config = DefaultConfigEsWp.generate();
        configs.put("es", config);
        engine = new WtEngineImpl(config);        
        engines.put("es", engine);
    }

    /**
     * @return the content of the wiki text fragment with all markup removed
     */
    public String toTextOnly(String wikitext, String lang) {
        String result = "";

        // get a compiler for wiki pages
        //WtEngineImpl engine = new WtEngineImpl(config);        
        WtEngineImpl engine = engines.get(lang);

        try {
            // Retrieve a page 
            // PL: no clue what is this page title thing ?? not even documented
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            result = (String)converter.go(cp.getPage());
        } catch(Exception e) {
            LOGGER.warn("Fail to parse MediaWiki text, lang is " + lang, e);
        }

        return trim(result);
    }

    /**
     * @return the content of the wiki text fragment with all markup removed except links 
     * to internal wikipedia pages: external links to the internet are removed
     */
    public String toTextWithInternalLinksOnly(String wikitext, String lang) {
        String result = "";

        // Instantiate a compiler for wiki pages
        //WtEngineImpl engine = new WtEngineImpl(config);        
        WtEngineImpl engine = engines.get(lang);

        try {
            // Retrieve a page 
            // PL: no clue what is this??
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            converter.addToKeep(WikiTextConverter.INTERNAL_LINKS);
            result = (String)converter.go(cp.getPage());
        } catch(Exception e) {
            LOGGER.warn("Fail to parse MediaWiki text, lang is " + lang, e);
        }

        return trim(result);
    }   

    /**
     * @return the content of the wiki text fragment with all markup removed except links 
     * to internal wikipedia pages: external links to the internet are removed
     */ 
    public String toTextWithInternalLinksAndCategoriesOnly(String wikitext, String lang) {
        String result = "";

        // Instantiate a compiler for wiki pages
        //WtEngineImpl engine = new WtEngineImpl(config);        
        WtEngineImpl engine = engines.get(lang);

        try {
            // Retrieve a page 
            // PL: no clue what is this??
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            converter.addToKeep(WikiTextConverter.INTERNAL_LINKS);
            converter.addToKeep(WikiTextConverter.CATEGORY_LINKS);
            result = (String)converter.go(cp.getPage());
        } catch(Exception e) {
            LOGGER.warn("Fail to parse MediaWiki text, lang is " + lang, e);
        }

        return trim(result);
    }   

    /**
     * @return the content of the wiki text fragment with all markup removed except links 
     * to internal wikipedia articles : external links to the internet are removed, as well as
     * internal link not to an article (e.g. redirection, disambiguation page, category, ...)
     */
    public String toTextWithInternalLinksArticlesOnly(String wikitext, String lang) {
        String result = "";

        // Instantiate a compiler for wiki pages
        //WtEngineImpl engine = new WtEngineImpl(config);        
        WtEngineImpl engine = engines.get(lang);

        try {
            // Retrieve a page 
            // PL: no clue what is this??
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            converter.addToKeep(WikiTextConverter.INTERNAL_LINKS_ARTICLES);
            result = (String)converter.go(cp.getPage());
        } catch(Exception e) {
            LOGGER.warn("Fail to parse MediaWiki text, lang is " + lang, e);
        }

        return trim(result);
    }   

    /**
     * @return the content of the wiki text fragment with all markup removed except links 
     * to internal wikipedia (external links to the internet are removed) and except emphasis 
     * (bold and italics)
     */
    public String toTextWithInternalLinksEmphasisOnly(String wikitext, String lang) {
        String result = "";
        // Instantiate a compiler for wiki pages
        //WtEngineImpl engine = new WtEngineImpl(config);
        WtEngineImpl engine = engines.get(lang);    

        try {
            // Retrieve a page 
            // PL: no clue what is this??
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            converter.addToKeep(WikiTextConverter.INTERNAL_LINKS);
            converter.addToKeep(WikiTextConverter.BOLD);
            converter.addToKeep(WikiTextConverter.ITALICS);
            result = (String)converter.go(cp.getPage());
        } catch(Exception e) {
            LOGGER.warn("Fail to parse MediaWiki text, lang is " + lang, e);
        }

        return trim(result);
    }

    /**
     * @return the first paragraph of the wiki text fragment after basic cleaning (template, etc.)
     * preserving all links and style markup
     */
    public String formatFirstParagraphWikiText(String wikitext, String lang) {
        wikitext = wikitext.replaceAll("={2,}(.+)={2,}", "\n"); 
        // clear section headings completely          
        
        wikitext = toTextWithInternalLinksEmphasisOnly(wikitext, lang);

        String firstParagraph = "";
        int pos = wikitext.indexOf("\n\n");
        while (pos >= 0) {
            firstParagraph = wikitext.substring(0, pos);
            if (pos > 150)
                break;
            pos = wikitext.indexOf("\n\n", pos+2);
        }

        firstParagraph = firstParagraph.replaceAll("\n", " ");
        firstParagraph = firstParagraph.replaceAll("\\[\\]", "");  
        firstParagraph = firstParagraph.replaceAll("\\s+", " "); 
        firstParagraph = firstParagraph.replaceAll("\\s,", ",");

        return trim(firstParagraph);
    }

    /**
     * @return the full content of the wiki text fragment after basic cleaning (template, etc.)
     * preserving all links and style markup
     */
    public String formatAllWikiText(String wikitext, String lang) {
        wikitext = wikitext.replaceAll("={2,}(.+)={2,}", "\n"); 
        // clear section headings completely

        wikitext = toTextWithInternalLinksEmphasisOnly(wikitext, lang);
        wikitext = wikitext.replaceAll("\n", " ");
        wikitext = wikitext.replaceAll("\\[\\]", "");  
        wikitext = wikitext.replaceAll("\\s+", " ");  
        wikitext = wikitext.replaceAll("\\s,", ",");

        return trim(wikitext);
    }
}



