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
 * Default configuration for the Swedish Wikipedia.
 */

/*
 For reference, Dec 2022:

 <siteinfo>
    <sitename>Wikipedia</sitename>
    <dbname>svwiki</dbname>
    <base>https://sv.wikipedia.org/wiki/Portal:Huvudsida</base>
    <generator>MediaWiki 1.40.0-wmf.10</generator>
    <case>first-letter</case>
    <namespaces>
      <namespace key="-2" case="first-letter">Media</namespace>
      <namespace key="-1" case="first-letter">Special</namespace>
      <namespace key="0" case="first-letter" />
      <namespace key="1" case="first-letter">Diskussion</namespace>
      <namespace key="2" case="first-letter">Användare</namespace>
      <namespace key="3" case="first-letter">Användardiskussion</namespace>
      <namespace key="4" case="first-letter">Wikipedia</namespace>
      <namespace key="5" case="first-letter">Wikipediadiskussion</namespace>
      <namespace key="6" case="first-letter">Fil</namespace>
      <namespace key="7" case="first-letter">Fildiskussion</namespace>
      <namespace key="8" case="first-letter">MediaWiki</namespace>
      <namespace key="9" case="first-letter">MediaWiki-diskussion</namespace>
      <namespace key="10" case="first-letter">Mall</namespace>
      <namespace key="11" case="first-letter">Malldiskussion</namespace>
      <namespace key="12" case="first-letter">Hjälp</namespace>
      <namespace key="13" case="first-letter">Hjälpdiskussion</namespace>
      <namespace key="14" case="first-letter">Kategori</namespace>
      <namespace key="15" case="first-letter">Kategoridiskussion</namespace>
      <namespace key="100" case="first-letter">Portal</namespace>
      <namespace key="101" case="first-letter">Portaldiskussion</namespace>
      <namespace key="446" case="first-letter">Education Program</namespace>
      <namespace key="447" case="first-letter">Education Program talk</namespace>
      <namespace key="710" case="first-letter">TimedText</namespace>
      <namespace key="711" case="first-letter">TimedText talk</namespace>
      <namespace key="828" case="first-letter">Modul</namespace>
      <namespace key="829" case="first-letter">Moduldiskussion</namespace>
      <namespace key="2300" case="case-sensitive">Gadget</namespace>
      <namespace key="2301" case="case-sensitive">Gadget talk</namespace>
      <namespace key="2302" case="case-sensitive">Gadget definition</namespace>
      <namespace key="2303" case="case-sensitive">Gadget definition talk</namespace>
      <namespace key="2600" case="first-letter">Ämne</namespace>
    </namespaces>
  </siteinfo>
*/

public class DefaultConfigSvWp extends DefaultConfigWp {

    public static WikiConfigImpl generate()
    {
        WikiConfigImpl c = new WikiConfigImpl();
        new DefaultConfigSvWp().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c)
    {
        c.setSiteName("My Swedish Wiki");

        c.setWikiUrl("http://localhost/");

        c.setContentLang("sv");

        c.setIwPrefix("sv");
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
                "Media",
                "Media",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                -1,
                "Special",
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
                "Diskussion",
                "Talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2,
                "Användare",
                "User",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                3,
                "Användardiskussion",
                "User talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                4,
                "Wikipedia",
                "Wikipedia",
                false,
                false,
                Arrays.asList("WP")));

        c.addNamespace(new NamespaceImpl(
                5,
                "Wikipediadiskussion",
                "Wikipedia talk",
                false,
                false,
                Arrays.asList("WT")));

        c.addNamespace(new NamespaceImpl(
                6,
                "Fil",
                "File",
                false,
                true,
                Arrays.asList("Image")));

        c.addNamespace(new NamespaceImpl(
                7,
                "Fildiskussion",
                "File talk",
                false,
                false,
                Arrays.asList("Image talk")));

        c.addNamespace(new NamespaceImpl(
                8,
                "MediaWiki",
                "MediaWiki",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                9,
                "MediaWiki-diskussion",
                "MediaWiki talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                10,
                "Mall",
                "Template",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                11,
                "Malldiskussion",
                "Template talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                12,
                "Hjälp",
                "Help",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                13,
                "Hjälpdiskussion",
                "Help talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                14,
                "Kategori",
                "Category",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                15,
                "Kategoridiskussion",
                "Category talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                100,
                "Portal",
                "Portal",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                101,
                "Portaldiskussion",
                "Portal talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                446,
                "Education Program",
                "Education Program",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                447,
                "Education Program talk",
                "Education Program talk",
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
                "Modul",
                "Module",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                829,
                "Moduldiskussion",
                "Module talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2300,
                "Gadget",
                "Gadget",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2301,
                "Gadget talk",
                "Gadget talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2302,
                "Gadget definition",
                "Gadget definition",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2303,
                "Gadget definition talk",
                "Gadget definition talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2600,
                "Ämne",
                "Topic",
                false,
                false,
                new ArrayList<String>()));

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }
}
