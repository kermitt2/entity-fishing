package com.scienceminer.nerd.utilities.mediaWiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.parser.nodes.WtUrl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.output.HtmlRenderer;
import org.sweble.wikitext.engine.output.HtmlRendererCallback;
import org.sweble.wikitext.engine.output.MediaInfo;
import org.sweble.wikitext.engine.config.Interwiki;

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

        config = DefaultConfigArWp.generate();
        configs.put("ar", config);
        engine = new WtEngineImpl(config);        
        engines.put("ar", engine);

        config = DefaultConfigZhWp.generate();
        configs.put("zh", config);
        engine = new WtEngineImpl(config);        
        engines.put("zh", engine);

        config = DefaultConfigJaWp.generate();
        configs.put("ja", config);
        engine = new WtEngineImpl(config);        
        engines.put("ja", engine);

        config = DefaultConfigRuWp.generate();
        configs.put("ru", config);
        engine = new WtEngineImpl(config);        
        engines.put("ru", engine);

        config = DefaultConfigPtWp.generate();
        configs.put("pt", config);
        engine = new WtEngineImpl(config);        
        engines.put("pt", engine);

        config = DefaultConfigFaWp.generate();
        configs.put("fa", config);
        engine = new WtEngineImpl(config);        
        engines.put("fa", engine);

        config = DefaultConfigUkWp.generate();
        configs.put("uk", config);
        engine = new WtEngineImpl(config);        
        engines.put("uk", engine);

        config = DefaultConfigSvWp.generate();
        configs.put("sv", config);
        engine = new WtEngineImpl(config);        
        engines.put("sv", engine);

        config = DefaultConfigHiWp.generate();
        configs.put("hi", config);
        engine = new WtEngineImpl(config);        
        engines.put("hi", engine);

        config = DefaultConfigBnWp.generate();
        configs.put("bn", config);
        engine = new WtEngineImpl(config);        
        engines.put("bn", engine);
    }

    /**
     * @return the content of the wiki text fragment with all markup removed
     */
    public String toTextOnly(String wikitext, String lang) {
        String result = "";

        // get a compiler for wiki pages
        WtEngineImpl engine = engines.get(lang);

        try {
            // Retrieve a fake page (I didn't find how to avoid this) 
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            result = (String)converter.go(cp.getPage());
            result = formatFragment(result);
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
        WtEngineImpl engine = engines.get(lang);

        try {
            // Retrieve a fake page (I didn't find how to avoid this) 
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            converter.addToKeep(WikiTextConverter.INTERNAL_LINKS);
            result = (String)converter.go(cp.getPage());
            result = formatFragment(result);
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
        WtEngineImpl engine = engines.get(lang);

        try {
            // Retrieve a fake page (I didn't find how to avoid this) 
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            converter.addToKeep(WikiTextConverter.INTERNAL_LINKS);
            converter.addToKeep(WikiTextConverter.CATEGORY_LINKS);
            result = (String)converter.go(cp.getPage());
            result = formatFragment(result);
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
        WtEngineImpl engine = engines.get(lang);

        try {
            // Retrieve a fake page (I didn't find how to avoid this) 
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            converter.addToKeep(WikiTextConverter.INTERNAL_LINKS_ARTICLES);
            result = (String)converter.go(cp.getPage());
            result = formatFragment(result);
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
        WtEngineImpl engine = engines.get(lang);
        try {
            // Retrieve a fake page (I didn't find how to avoid this) 
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // remove simple templates outside paragraphs
            wikitext = removeNonInlineTemplates(wikitext);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            WikiTextConverter converter = new WikiTextConverter(configs.get(lang));
            converter.addToKeep(WikiTextConverter.INTERNAL_LINKS);
            converter.addToKeep(WikiTextConverter.BOLD);
            converter.addToKeep(WikiTextConverter.ITALICS);
            result = (String)converter.go(cp.getPage());
            result = formatFragment(result);
        } catch(Exception e) {
            LOGGER.warn("Fail to parse MediaWiki text in toTextWithInternalLinksEmphasisOnly, lang is " + lang, e);
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

        wikitext = wikitext.replaceAll("<!--.*?-->", " ");
        // remove html comment in media wiki

        try {
            wikitext = toTextWithInternalLinksEmphasisOnly(wikitext, lang);
        } catch(Exception e) {
            LOGGER.warn("Fail to parse MediaWiki text in formatFirstParagraphWikiText, lang is " + lang, e);
        }
        
        String firstParagraph = "";
        int pos = wikitext.indexOf("\n\n");
        while (pos >= 0) {
            firstParagraph = wikitext.substring(0, pos);
            if (pos > 150)
                break;
            pos = wikitext.indexOf("\n\n", pos+2);
        }
        firstParagraph= formatFragment(firstParagraph);
        return trim(firstParagraph);
    }

    /**
     * @return the content of the wiki text fragment converted into HTML
     */
    public String toHTML(String wikitext, String lang) {
        // get a compiler for wiki pages
        WtEngineImpl engine = engines.get(lang);
        String htmlString = null;
        
        try {
            // still no idea why we need a fake page title
            PageTitle pageTitle = PageTitle.make(configs.get(lang), "crap");
            PageId pageId = new PageId(pageTitle, -1);
        
            // here we also need a callback to inject link information in the html
            HtmlCallback htmlCallback = new HtmlCallback();
            htmlCallback.setLang(lang);

            EngProcessedPage ast = engine.postprocess(pageId, wikitext, null);
            htmlString = HtmlRenderer.print(htmlCallback, configs.get(lang), null, ast);

            if (htmlString != null) {
                // we need to strip out the starting/trailing new line and tabulation characters
                // for the paragraphs
                htmlString = htmlString.replaceAll("<p>[\\n\\t]+", "<p>");
                htmlString = htmlString.replaceAll("[\\n\\t]+</p>", "<p>");
            }

        } catch(Exception e) {
            LOGGER.error("Fail to convertMediaWiki text into HTML, lang is " + lang, e);
            // fallback to text only, which is always a string, possibly empty
            htmlString = "<p>" + toTextOnly(wikitext, lang) + "</p>";
        }

        return htmlString;
    }

    /**
     * @return the full content of the wiki text fragment after basic cleaning (template, etc.)
     * preserving all links and style markup
     */
    public String formatAllWikiText(String wikitext, String lang) {
        wikitext = wikitext.replaceAll("={2,}(.+)={2,}", "\n"); 
        // clear section headings completely

        wikitext = wikitext.replaceAll("<!--.*?-->", " ");
        // remove html comment in media wiki

        wikitext = toTextWithInternalLinksEmphasisOnly(wikitext, lang);
        wikitext= formatFragment(wikitext);

        return trim(wikitext);
    }

    static private String formatFragment(String wikitext) {
        wikitext = wikitext.replace("( )", " ");
        wikitext = wikitext.replaceAll("\n", " ");
        wikitext = wikitext.replaceAll("\\[\\]", ""); 
        wikitext = wikitext.replaceAll("\\s+", " ");  
        wikitext = wikitext.replaceAll("\\s,", ",");
        return trim(wikitext);
    }

    /**
     * @return the content of the wiki text fragment after removing all templates outside
     * paragraphs (templates that are not appearing inline in the text)
     */
    public String removeNonInlineTemplates(String wikitext) {
        StringBuilder wikitextSlim = new StringBuilder();        
        String[] lines = wikitext.split("\n");
        for(int i=0; i<lines.length;i++) {
            String line = lines[i];
            line = line.trim();
            // note we don't remove {{redirect...}}
            if (line.startsWith("{{") && line.endsWith("}}"))
                continue;

            wikitextSlim.append(line).append("\n");
        }
        return wikitextSlim.toString();
    }

    public String removeNonInlineTemplatesOld(String wikitext) {
        StringBuilder wikitextSlim = new StringBuilder();        
        String[] lines = wikitext.split("\n");
        int ignore = 0;
        for(int i=0; i<lines.length;i++) {
            String line = lines[i];
            line = line.trim();
            // note we don't remove {{redirect...}}
            if (line.startsWith("{{"))
                ignore++;

            if (ignore>0 && line.indexOf("}}") != -1) {
                ignore--;
                if (ignore == 0)
                    line = line.substring(line.indexOf("}}")+2, line.length());            
            }

            if (ignore == 0) {
                // conservative checks
                if (!line.trim().startsWith("|") && !line.trim().startsWith("{|") && 
                    !line.trim().startsWith("|}") && !line.trim().startsWith("}}") && 
                    !line.trim().startsWith("{{"))
                    wikitextSlim.append(line).append("\n");
            }
        }

        return wikitextSlim.toString();
    }

    private static final class HtmlCallback implements HtmlRendererCallback {

        // didn't find  way to get the lang code, so adding the info here
        private String lang = null;

        public void setLang(String lang) {
            this.lang = lang;
        }

        public String getLang() {
            return this.lang;
        }

        @Override
        public boolean resourceExists(PageTitle target) {
            // we could check if page title in KB, but just following input is also good strategy and easier
            return true;
        }
        
        @Override
        public MediaInfo getMediaInfo(String title, int width, int height) {
            // we don't do images
            return null;
        }

        @Override
        public String makeUrlMissingTarget(String path) {
            // we assume always present
            return path;
        }

        @Override
        public String makeUrl(PageTitle linkTarget) {            
            // getTitle() here return normally the wikipedia "normalized" title (" " replaced by "_")
            return "https://"+this.lang+".wikipedia.org/wiki/" + linkTarget.getTitle();
        }

        @Override
        public String makeUrl(WtUrl target) {
            return "";
        }
    }
}



