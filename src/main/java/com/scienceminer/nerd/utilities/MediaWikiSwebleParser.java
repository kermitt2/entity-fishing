package com.scienceminer.nerd.utilities;

import com.scienceminer.nerd.kb.model.Article;

/**
 * Created by lfoppiano on 20/06/2017.
 */
public class MediaWikiSwebleParser {

    /**
     * Convenience method which combines both of the above methods - i.e. returns a copy of the
     * given markup, where all markup has been removed except for section headers and list markers.
     * <p>
     * By default, unwanted markup is completely discarded. You can optionally specify
     * a character to replace the regions that are discared, so that the length of the
     * string and the locations of unstripped characters is not modified.
     */
    public String stripToPlainText(String markup, Character replacement) {
        String clearedMarkup = stripAllButInternalLinksAndEmphasis(markup, replacement);
        clearedMarkup = stripInternalLinks(clearedMarkup, replacement);

        return clearedMarkup;
    }

    /**
     * Returns a copy of the given markup, where all links to wikipedia pages
     * (categories, articles, etc) have been removed. Links to articles are
     * replaced with the appropriate anchor markup. All other links are removed completely.
     *
     * By default, unwanted markup is completely discarded. You can optionally specify
     * a character to replace the regions that are discarded, so that the length of the
     * string and the locations of unstripped characters is not modified.
     */
    public String stripInternalLinks(String markup, Character replacement) {
        return markup;   
    }

    /**
     * Returns a copy of the given markup, where all markup has been removed except for
     * internal links to other wikipedia pages (e.g. to articles or categories), section
     * headers, list markers, and bold/italic markers.
     * <p>
     * By default, unwanted markup is completely discarded. You can optionally specify
     * a character to replace the regions that are discared, so that the length of the
     * string and the locations of unstripped characters is not modified.
     */
    public String stripAllButInternalLinksAndEmphasis(String markup, Character replacement) {

        return markup;
    }

    public String stripExcessNewlines(String markup) {

        return markup;
    }

    public String stripNonArticleInternalLinks(String markup, Character replacement) {

        return markup;
    }

    public String getMarkupLinksOnly(Article article) {

        return null;
    }

    public String getCleanedContent(Article article) {
        return null;
    }
}
