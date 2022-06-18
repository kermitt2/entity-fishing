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
 * Default configuration for the Portuguese Wikipedia.
 */

/*

For reference:

<siteinfo>
    <sitename>Wikipédia</sitename>
    <dbname>ptwiki</dbname>
    <base>https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal</base>
    <generator>MediaWiki 1.39.0-wmf.13</generator>
    <case>first-letter</case>
    <namespaces>
      <namespace key="-2" case="first-letter">Multimédia</namespace>
      <namespace key="-1" case="first-letter">Especial</namespace>
      <namespace key="0" case="first-letter" />
      <namespace key="1" case="first-letter">Discussão</namespace>
      <namespace key="2" case="first-letter">Usuário(a)</namespace>
      <namespace key="3" case="first-letter">Usuário(a) Discussão</namespace>
      <namespace key="4" case="first-letter">Wikipédia</namespace>
      <namespace key="5" case="first-letter">Wikipédia Discussão</namespace>
      <namespace key="6" case="first-letter">Ficheiro</namespace>
      <namespace key="7" case="first-letter">Ficheiro Discussão</namespace>
      <namespace key="8" case="first-letter">MediaWiki</namespace>
      <namespace key="9" case="first-letter">MediaWiki Discussão</namespace>
      <namespace key="10" case="first-letter">Predefinição</namespace>
      <namespace key="11" case="first-letter">Predefinição Discussão</namespace>
      <namespace key="12" case="first-letter">Ajuda</namespace>
      <namespace key="13" case="first-letter">Ajuda Discussão</namespace>
      <namespace key="14" case="first-letter">Categoria</namespace>
      <namespace key="15" case="first-letter">Categoria Discussão</namespace>
      <namespace key="100" case="first-letter">Portal</namespace>
      <namespace key="101" case="first-letter">Portal Discussão</namespace>
      <namespace key="104" case="first-letter">Livro</namespace>
      <namespace key="105" case="first-letter">Livro Discussão</namespace>
      <namespace key="446" case="first-letter">Education Program</namespace>
      <namespace key="447" case="first-letter">Education Program talk</namespace>
      <namespace key="710" case="first-letter">TimedText</namespace>
      <namespace key="711" case="first-letter">TimedText talk</namespace>
      <namespace key="828" case="first-letter">Módulo</namespace>
      <namespace key="829" case="first-letter">Módulo Discussão</namespace>
      <namespace key="2300" case="first-letter">Gadget</namespace>
      <namespace key="2301" case="first-letter">Gadget talk</namespace>
      <namespace key="2302" case="case-sensitive">Gadget definition</namespace>
      <namespace key="2303" case="case-sensitive">Gadget definition talk</namespace>
      <namespace key="2600" case="first-letter">Tópico</namespace>
    </namespaces>
  </siteinfo>
*/

public class DefaultConfigPtWp extends DefaultConfigWp {

    public static WikiConfigImpl generate()
    {
        WikiConfigImpl c = new WikiConfigImpl();
        new DefaultConfigPtWp().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c)
    {
        c.setSiteName("My Portuguese Wiki");

        c.setWikiUrl("http://localhost/");

        c.setContentLang("pt");

        c.setIwPrefix("pt");
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
                "Multimédia",
                "Media",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                -1,
                "Especial",
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
                "Discussão",
                "Talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2,
                "Usuário(a)",
                "User",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                3,
                "Usuário(a) Discussão",
                "User talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                4,
                "Wikipédia",
                "Wikipedia",
                false,
                false,
                Arrays.asList("WP")));

        c.addNamespace(new NamespaceImpl(
                5,
                "Wikipédia Discussão",
                "Wikipedia talk",
                false,
                false,
                Arrays.asList("WT")));

        c.addNamespace(new NamespaceImpl(
                6,
                "Ficheiro",
                "File",
                false,
                true,
                Arrays.asList("Image")));

        c.addNamespace(new NamespaceImpl(
                7,
                "Ficheiro Discussão",
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
                "MediaWiki Discussão",
                "MediaWiki talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                10,
                "Predefinição",
                "Template",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                11,
                "Predefinição Discussão",
                "Template talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                12,
                "Ajuda",
                "Help",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                13,
                "Ajuda Discussão",
                "Help talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                14,
                "Categoria",
                "Category",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                15,
                "Categoria Discussão",
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
                "Portal Discussão",
                "Portal talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                104,
                "Livro",
                "Book",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                105,
                "Livro Discussão",
                "Book talk",
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
                "Módulo",
                "Module",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                829,
                "Módulo Discussão",
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
                "Tópico",
                "Topic",
                false,
                false,
                new ArrayList<String>()));

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }
}
