package com.scienceminer.nerd.utilities.mediaWiki;

import java.util.ArrayList;
import java.util.Arrays;

import org.sweble.wikitext.engine.config.I18nAliasImpl;
import org.sweble.wikitext.engine.config.InterwikiImpl;
import org.sweble.wikitext.engine.config.NamespaceImpl;
import org.sweble.wikitext.engine.config.ParserConfigImpl;
import org.sweble.wikitext.engine.config.WikiConfigImpl;
import org.sweble.wikitext.engine.utils.*;

/**
 * Default configuration for the Hindi Wikipedia.
 */

/*
 For reference, Dec 2022:

  <siteinfo>
    <sitename>विकिपीडिया</sitename>
    <dbname>hiwiki</dbname>
    <base>https://hi.wikipedia.org/wiki/%E0%A4%AE%E0%A5%81%E0%A4%96%E0%A4%AA%E0%A5%83%E0%A4%B7%E0%A5%8D%E0%A4%A0</base>
    <generator>MediaWiki 1.40.0-wmf.10</generator>
    <case>first-letter</case>
    <namespaces>
      <namespace key="-2" case="first-letter">मीडिया</namespace>
      <namespace key="-1" case="first-letter">विशेष</namespace>
      <namespace key="0" case="first-letter" />
      <namespace key="1" case="first-letter">वार्ता</namespace>
      <namespace key="2" case="first-letter">सदस्य</namespace>
      <namespace key="3" case="first-letter">सदस्य वार्ता</namespace>
      <namespace key="4" case="first-letter">विकिपीडिया</namespace>
      <namespace key="5" case="first-letter">विकिपीडिया वार्ता</namespace>
      <namespace key="6" case="first-letter">चित्र</namespace>
      <namespace key="7" case="first-letter">चित्र वार्ता</namespace>
      <namespace key="8" case="first-letter">मीडियाविकि</namespace>
      <namespace key="9" case="first-letter">मीडियाविकि वार्ता</namespace>
      <namespace key="10" case="first-letter">साँचा</namespace>
      <namespace key="11" case="first-letter">साँचा वार्ता</namespace>
      <namespace key="12" case="first-letter">सहायता</namespace>
      <namespace key="13" case="first-letter">सहायता वार्ता</namespace>
      <namespace key="14" case="first-letter">श्रेणी</namespace>
      <namespace key="15" case="first-letter">श्रेणी वार्ता</namespace>
      <namespace key="100" case="first-letter">प्रवेशद्वार</namespace>
      <namespace key="101" case="first-letter">प्रवेशद्वार वार्ता</namespace>
      <namespace key="710" case="first-letter">TimedText</namespace>
      <namespace key="711" case="first-letter">TimedText talk</namespace>
      <namespace key="828" case="first-letter">Module</namespace>
      <namespace key="829" case="first-letter">Module talk</namespace>
      <namespace key="2300" case="case-sensitive">गैजेट</namespace>
      <namespace key="2301" case="case-sensitive">गैजेट वार्ता</namespace>
      <namespace key="2302" case="case-sensitive">गैजेट परिभाषा</namespace>
      <namespace key="2303" case="case-sensitive">गैजेट परिभाषा वार्ता</namespace>
    </namespaces>
  </siteinfo>

*/

public class DefaultConfigHiWp extends DefaultConfigWp {

    public static WikiConfigImpl generate()
    {
        WikiConfigImpl c = new WikiConfigImpl();
        new DefaultConfigHiWp().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c)
    {
        c.setSiteName("My Hindi Wiki");

        c.setWikiUrl("http://localhost/");

        c.setContentLang("hi");

        c.setIwPrefix("hi");
    }

    protected ParserConfigImpl configureParser(WikiConfigImpl c)
    {
        ParserConfigImpl pc = super.configureParser(c);

        // --[ Link classification and parsing ]--

        pc.setInternalLinkPrefixPattern(null);
        pc.setInternalLinkPostfixPattern("[a-z]+");

        return pc;
    }

    protected void addNamespaces(WikiConfigImpl c)
    {
        c.addNamespace(new NamespaceImpl(
                -2,
                "मीडिया",
                "Media",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                -1,
                "विशेष",
                "Special",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                0,
                "",
                "",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                1,
                "वार्ता",
                "Talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2,
                "सदस्य",
                "User",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                3,
                "सदस्य वार्ता",
                "User talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                4,
                "विकिपीडिया",
                "Wikipedia",
                false,
                false,
                Arrays.asList("WP")));

        c.addNamespace(new NamespaceImpl(
                5,
                "विकिपीडिया वार्ता",
                "Wikipedia talk",
                false,
                false,
                Arrays.asList("WT")));

        c.addNamespace(new NamespaceImpl(
                6,
                "चित्र",
                "File",
                false,
                true,
                Arrays.asList("Image")));

        c.addNamespace(new NamespaceImpl(
                7,
                "चित्र वार्ता",
                "File talk",
                false,
                false,
                Arrays.asList("Image talk")));

        c.addNamespace(new NamespaceImpl(
                8,
                "मीडियाविकि",
                "MediaWiki",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                9,
                "मीडियाविकि वार्ता",
                "MediaWiki talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                10,
                "साँचा",
                "Template",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                11,
                "साँचा वार्ता",
                "Template talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                12,
                "सहायता",
                "Help",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                13,
                "सहायता वार्ता",
                "Help talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                14,
                "श्रेणी",
                "Category",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                15,
                "श्रेणी वार्ता",
                "Category talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                100,
                "प्रवेशद्वार",
                "Portal",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                101,
                "प्रवेशद्वार वार्ता",
                "Portal talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                710,
                "TimedText",
                "TimedText",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                711,
                "TimedText talk",
                "TimedText talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                828,
                "Module",
                "Module",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                829,
                "Module talk",
                "Module talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2300,
                "गैजेट",
                "Gadget",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2301,
                "गैजेट वार्ता",
                "Gadget talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2302,
                "गैजेट परिभाषा",
                "Gadget definition",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2303,
                "गैजेट परिभाषा वार्ता",
                "Gadget definition talk",
                false,
                false,
                new ArrayList<String>()));

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }
}
