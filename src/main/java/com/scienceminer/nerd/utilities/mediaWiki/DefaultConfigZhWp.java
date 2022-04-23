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
 * Default configuration for the Mandarin Wikipedia.
 */

/* For reference:
  <siteinfo>
    <sitename>Wikipedia</sitename>
    <dbname>zhwiki</dbname>
    <base>https://zh.wikipedia.org/wiki/Wikipedia:%E9%A6%96%E9%A1%B5</base>
    <generator>MediaWiki 1.38.0-wmf.19</generator>
    <case>first-letter</case>
    <namespaces>
      <namespace key="-2" case="first-letter">Media</namespace>
      <namespace key="-1" case="first-letter">Special</namespace>
      <namespace key="0" case="first-letter" />
      <namespace key="1" case="first-letter">Talk</namespace>
      <namespace key="2" case="first-letter">User</namespace>
      <namespace key="3" case="first-letter">User talk</namespace>
      <namespace key="4" case="first-letter">Wikipedia</namespace>
      <namespace key="5" case="first-letter">Wikipedia talk</namespace>
      <namespace key="6" case="first-letter">File</namespace>
      <namespace key="7" case="first-letter">File talk</namespace>
      <namespace key="8" case="first-letter">MediaWiki</namespace>
      <namespace key="9" case="first-letter">MediaWiki talk</namespace>
      <namespace key="10" case="first-letter">Template</namespace>
      <namespace key="11" case="first-letter">Template talk</namespace>
      <namespace key="12" case="first-letter">Help</namespace>
      <namespace key="13" case="first-letter">Help talk</namespace>
      <namespace key="14" case="first-letter">Category</namespace>
      <namespace key="15" case="first-letter">Category talk</namespace>
      <namespace key="100" case="first-letter">Portal</namespace>
      <namespace key="101" case="first-letter">Portal talk</namespace>
      <namespace key="102" case="first-letter">WikiProject</namespace>
      <namespace key="103" case="first-letter">WikiProject talk</namespace>
      <namespace key="118" case="first-letter">Draft</namespace>
      <namespace key="119" case="first-letter">Draft talk</namespace>
      <namespace key="828" case="first-letter">Module</namespace>
      <namespace key="829" case="first-letter">Module talk</namespace>
      <namespace key="2300" case="first-letter">Gadget</namespace>
      <namespace key="2301" case="first-letter">Gadget talk</namespace>
      <namespace key="2302" case="case-sensitive">Gadget definition</namespace>
      <namespace key="2303" case="case-sensitive">Gadget definition talk</namespace>
      <namespace key="2600" case="first-letter">Topic</namespace>
    </namespaces>
  </siteinfo>
*/

public class DefaultConfigZhWp extends DefaultConfigWp {

    public static WikiConfigImpl generate()
    {
        WikiConfigImpl c = new WikiConfigImpl();
        new DefaultConfigZhWp().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c)
    {
        c.setSiteName("My Mandarin Wiki");

        c.setWikiUrl("http://localhost/");

        c.setContentLang("zh");

        c.setIwPrefix("zh");
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
                "Talk",
                "Talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2,
                "مستخدم",
                "User",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                3,
                "User talk",
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
                "Wikipedia talk",
                "Wikipedia talk",
                false,
                false,
                Arrays.asList("WT")));

        c.addNamespace(new NamespaceImpl(
                6,
                "File",
                "File",
                false,
                true,
                Arrays.asList("Image")));

        c.addNamespace(new NamespaceImpl(
                7,
                "File talk",
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
                "MediaWiki talk",
                "MediaWiki talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                10,
                "Template",
                "Template",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                11,
                "Template talk",
                "Template talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                12,
                "Help",
                "Help",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                13,
                "Help talk",
                "Help talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                14,
                "Category",
                "Category",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                15,
                "Category talk",
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
                "Portal talk",
                "Portal talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                102,
                "WikiProject",
                "WikiProject",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                103,
                "WikiProject talk",
                "WikiProject talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                118,
                "Draft",
                "Draft",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                119,
                "Draft talk",
                "Draft talk",
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
                "Topic",
                "Topic",
                false,
                false,
                new ArrayList<String>()));      

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }
}
