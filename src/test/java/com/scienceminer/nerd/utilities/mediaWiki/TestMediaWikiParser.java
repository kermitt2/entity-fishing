package com.scienceminer.nerd.utilities.mediaWiki;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Ignore;

import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class TestMediaWikiParser {

    private MediaWikiParser mediaWikiParser;

    @Before
    public void setUp() {
        mediaWikiParser = MediaWikiParser.getInstance();
    }

    @Test
    public void testWikiMedia2PureText() throws Exception {
        String testWikiMedia = "'''Nantier Beall Minoustchine Publishing Inc.''' (or '''NBM Publishing''') is an American [[graphic novel]] publisher. Founded by [[Terry Nantier]] in 1976 as Flying Buttress Publications, NBM is one of the oldest graphic novel publishers in North America. The company publishes English adaptations and translations of popular European comics, compilations of classic [[comic strip]]s, and original fiction and nonfiction graphic novels. In addition to NBM Graphic Novels, the company has several imprints including [[Papercutz (publisher)|Papercutz]] with comics geared towards younger audiences, ComicsLit for literary graphic fiction, and Eurotica and Amerotica for [[adult comics]].";
        String result = mediaWikiParser.toTextOnly(testWikiMedia, "en");
        assertThat(result, startsWith("Nantier Beall Minoustchine Publishing Inc. (or NBM Publishing) is an American graphic novel publisher."));
        assertThat(result, not(containsString("[[")));
        assertThat(result, not(containsString("'''")));
    }

    @Test
    public void testWikiMedia2PureTextFr() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("cantal.fr.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextOnly(input, "fr");
        assertThat(result, startsWith("Le Cantal est un département français situé dans la région Auvergne-Rhône-Alpes"));
        assertThat(result, not(containsString("[[")));
        assertThat(result, not(containsString("'''")));
    }

    @Test
    public void testWikiMedia2PureTextFrBis() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("japan.fr.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextOnly(input, "fr");
        assertThat(result, startsWith("La culture japonaise a subi un apport considérable des cultures chinoise et coréenne"));
        assertThat(result, not(containsString("[[")));
        assertThat(result, not(containsString("'''")));
    }

    @Test
    public void testWikiMedia2PureTextDe() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("astana.de.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextOnly(input, "de");
        assertThat(result, startsWith("Astana [], deutsch auch [] (kasachisch und russisch ; ist auch das kasachische Wort für Hauptstadt) ist seit 1997 die Hauptstadt Kasachstan"));
        //assertThat(result, startsWith("Astana [], deutsch auch [] (kasachisch und russisch ; ist auch das kasachische Wort für Hauptstadt) ist seit 1997 die Hauptstadt Kasachstan"));
        assertThat(result, not(containsString("[[")));
        assertThat(result, not(containsString("'''")));
    }

    @Test
    public void testWikiMedia2TextWithInternalLinks() throws Exception {
        String testWikiMedia = "The '''United Kingdom European Union membership referendum''', also known as the '''EU referendum''', took place in the [[United Kingdom]] and [[Gibraltar]] on 23 June 2016. [[Member state of the European Union|Membership of the European Union]] has been a topic of debate in the [[United Kingdom]] since the country joined the [[European Economic Community]] (the Common Market), as it was known then, in 1973.";
        String result = mediaWikiParser.toTextWithInternalLinksOnly(testWikiMedia, "en");
        assertThat(result, startsWith("The United Kingdom European Union membership referendum, also known as the EU referendum, took place in"));
        assertThat(result, containsString("[["));
        assertThat(result, containsString("]]"));
        assertThat(result, not(containsString("'''")));
    }

    @Test
    public void testWikiMedia2PureText_complexArticle() throws Exception {

        InputStream is = this.getClass().getResourceAsStream("September2_articlePage.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextOnly(input, "en");

        assertThat(result, startsWith("1. Events"));
        assertThat(result, endsWith("5. External links"));

        assertThat(result, not(containsString("[[")));
        assertThat(result, not(containsString("]]")));
        assertThat(result, not(containsString("'''")));
    }

    @Test
    public void testWikiMedia2TextWithInternalLinks_complexArticle() throws Exception {

        InputStream is = this.getClass().getResourceAsStream("September2_articlePage.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextWithInternalLinksOnly(input, "en");

        assertThat(result, startsWith("1. Events"));
        assertThat(result, endsWith("5. External links"));

        assertThat(result, containsString("[["));
        assertThat(result, containsString("]]"));
        assertThat(result, not(containsString("'''")));
    }

    @Test
    public void testWikiMedia2TextWithInternalLinksEmphasisOnly_2september() throws Exception {

        InputStream is = this.getClass().getResourceAsStream("September2_articlePage.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextWithInternalLinksEmphasisOnly(input, "en");

        assertThat(result, containsString("[["));
        assertThat(StringUtils.countMatches(result, "[["), is(693));
        assertThat(result, containsString("]]"));
        assertThat(result, containsString("'''"));

        assertThat(result, startsWith("1. Events"));
        assertThat(result, endsWith("5. External links"));
    }

    @Test
    public void testWikiMedia2TextWithInternalLinksEmphasisOnly_cleopatra() throws Exception {

        InputStream is = this.getClass().getResourceAsStream("Cleopatra_articlePage.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextWithInternalLinksEmphasisOnly(input, "en");

        assertThat(result, containsString("[["));
        assertThat(StringUtils.countMatches(result, "[["), is(183));
        assertThat(result, containsString("]]"));
        assertThat(result, containsString("'''"));

        assertThat(result, startsWith("'''Cleopatra VII Philopator'''"));
        //TODO: this should be fixed
        assertThat(result, endsWith("at </small>"));
    }

    @Test
    public void testWikiMedia2TextWithInternalLinksArticlesOnly_2september() throws Exception {

        InputStream is = this.getClass().getResourceAsStream("September2_articlePage.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextWithInternalLinksArticlesOnly(input, "en");

        assertThat(result, containsString("[["));
        assertThat(StringUtils.countMatches(result, "[["), is(693));
        assertThat(result, containsString("]]"));
        assertThat(result, not(containsString("'''")));

        assertThat(result, startsWith("1. Events"));
        assertThat(result, endsWith("5. External links"));
    }

    @Test
    public void testWikiMedia2TextWithInternalLinksArticlesOnly_cleopatra() throws Exception {

        InputStream is = this.getClass().getResourceAsStream("Cleopatra_articlePage.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextWithInternalLinksArticlesOnly(input, "en");

        assertThat(result, containsString("[["));
        assertThat(StringUtils.countMatches(result, "[["), is(183));
        assertThat(result, containsString("]]"));
        assertThat(result, not(containsString("'''")));

        assertThat(result, startsWith("Cleopatra VII Philopator"));
        //TODO: this should be fixed
        assertThat(result, endsWith("at </small>"));
    }

    @Test
    public void testWikiMedia3TextWithInternalLinksArticlesOnly() throws Exception {

        InputStream is = this.getClass().getResourceAsStream("acropolis.txt");
        String input = IOUtils.toString(is, UTF_8);
        String result = mediaWikiParser.toTextWithInternalLinksArticlesOnly(input, "en");

        assertThat(result, containsString("[["));
        assertThat(result, containsString("]]"));
        assertThat(result, not(containsString("'''")));

        assertThat(result, startsWith("An acropolis"));
    }
    



}