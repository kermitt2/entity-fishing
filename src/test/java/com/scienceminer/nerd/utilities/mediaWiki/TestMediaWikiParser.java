package com.scienceminer.nerd.utilities.mediaWiki;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Ignore; 

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 
 */
public class TestMediaWikiParser {

    private MediaWikiParser mediaWikiParser = null;

    @Before
    public void setUp() {
        mediaWikiParser = MediaWikiParser.getInstance();
    }

    @Test
    public void testWikiMedia2PureText() throws Exception {
        String testWikiMedia = "'''Nantier Beall Minoustchine Publishing Inc.''' (or '''NBM Publishing''') is an American [[graphic novel]] publisher. Founded by [[Terry Nantier]] in 1976 as Flying Buttress Publications, NBM is one of the oldest graphic novel publishers in North America. The company publishes English adaptations and translations of popular European comics, compilations of classic [[comic strip]]s, and original fiction and nonfiction graphic novels. In addition to NBM Graphic Novels, the company has several imprints including [[Papercutz (publisher)|Papercutz]] with comics geared towards younger audiences, ComicsLit for literary graphic fiction, and Eurotica and Amerotica for [[adult comics]].";
        String result = mediaWikiParser.toTextOnly(testWikiMedia);
        System.out.println(testWikiMedia);
        System.out.println(result);
    }

    @Test
    public void testWikiMedia2TextWithInternalLinks() throws Exception {
        String testWikiMedia = "The '''United Kingdom European Union membership referendum''', also known as the '''EU referendum''', took place in the [[United Kingdom]] and [[Gibraltar]] on 23 June 2016. [[Member state of the European Union|Membership of the European Union]] has been a topic of debate in the [[United Kingdom]] since the country joined the [[European Economic Community]] (the Common Market), as it was known then, in 1973.";
        String result = mediaWikiParser.toTextWithInternalLinksOnly(testWikiMedia);
        System.out.println(testWikiMedia);
        System.out.println(result);
    }

}