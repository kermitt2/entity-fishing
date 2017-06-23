package com.scienceminer.nerd.utilities.mediaWiki;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.output.HtmlRenderer;
import org.sweble.wikitext.engine.output.HtmlRendererCallback;
import org.sweble.wikitext.engine.output.MediaInfo;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.engine.utils.UrlEncoding;
import org.sweble.wikitext.parser.nodes.WtUrl;
import org.sweble.wikitext.parser.parser.LinkTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for the parser instance for document in mediawiki format able to modify
 * the mediawiki articles into simpler text formats. 
 */
public class MediaWikiParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaWikiParser.class);

    private static volatile MediaWikiParser instance;
    private WikiConfig config = null;

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
        // Set-up a simple wiki configuration
        config = DefaultConfigEnWp.generate();
    }

    public String toTextOnly(String wikitext) {
        String result = "";

        // Instantiate a compiler for wiki pages
        WtEngineImpl engine = new WtEngineImpl(config);        

        try {
            // Retrieve a page 
            // PL: no clue what is this??
            PageTitle pageTitle = PageTitle.make(config, "crap");
            PageId pageId = new PageId(pageTitle, -1);

            // Compile the retrieved page
            EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);
            TextConverter converter = new TextConverter(config);
            result = (String)converter.go(cp.getPage());
        } catch(Exception e) {
            LOGGER.warn("Fail to parse MediaWiki text");
            e.printStackTrace();
        }

        return result;
    }
}



