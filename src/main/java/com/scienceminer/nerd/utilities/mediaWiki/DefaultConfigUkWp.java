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
 * Default configuration for the Ukrainian Wikipedia.
 */

/*
 For reference, Dec 2022:

 <siteinfo>
    <sitename>Вікіпедія</sitename>
    <dbname>ukwiki</dbname>
    <base>https://uk.wikipedia.org/wiki/%D0%93%D0%BE%D0%BB%D0%BE%D0%B2%D0%BD%D0%B0_%D1%81%D1%82%D0%BE%D1%80%D1%96%D0%BD%D0%BA%D0%B0</base>
    <generator>MediaWiki 1.40.0-wmf.10</generator>
    <case>first-letter</case>
    <namespaces>
      <namespace key="-2" case="first-letter">Медіа</namespace>
      <namespace key="-1" case="first-letter">Спеціальна</namespace>
      <namespace key="0" case="first-letter" />
      <namespace key="1" case="first-letter">Обговорення</namespace>
      <namespace key="2" case="first-letter">Користувач</namespace>
      <namespace key="3" case="first-letter">Обговорення користувача</namespace>
      <namespace key="4" case="first-letter">Вікіпедія</namespace>
      <namespace key="5" case="first-letter">Обговорення Вікіпедії</namespace>
      <namespace key="6" case="first-letter">Файл</namespace>
      <namespace key="7" case="first-letter">Обговорення файлу</namespace>
      <namespace key="8" case="first-letter">MediaWiki</namespace>
      <namespace key="9" case="first-letter">Обговорення MediaWiki</namespace>
      <namespace key="10" case="first-letter">Шаблон</namespace>
      <namespace key="11" case="first-letter">Обговорення шаблону</namespace>
      <namespace key="12" case="first-letter">Довідка</namespace>
      <namespace key="13" case="first-letter">Обговорення довідки</namespace>
      <namespace key="14" case="first-letter">Категорія</namespace>
      <namespace key="15" case="first-letter">Обговорення категорії</namespace>
      <namespace key="100" case="first-letter">Портал</namespace>
      <namespace key="101" case="first-letter">Обговорення порталу</namespace>
      <namespace key="446" case="first-letter">Education Program</namespace>
      <namespace key="447" case="first-letter">Education Program talk</namespace>
      <namespace key="710" case="first-letter">TimedText</namespace>
      <namespace key="711" case="first-letter">TimedText talk</namespace>
      <namespace key="828" case="first-letter">Модуль</namespace>
      <namespace key="829" case="first-letter">Обговорення модуля</namespace>
      <namespace key="2300" case="case-sensitive">Gadget</namespace>
      <namespace key="2301" case="case-sensitive">Gadget talk</namespace>
      <namespace key="2302" case="case-sensitive">Gadget definition</namespace>
      <namespace key="2303" case="case-sensitive">Gadget definition talk</namespace>
    </namespaces>
  </siteinfo>
*/

public class DefaultConfigUkWp extends DefaultConfigWp {

    public static WikiConfigImpl generate()
    {
        WikiConfigImpl c = new WikiConfigImpl();
        new DefaultConfigUkWp().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c)
    {
        c.setSiteName("My Ukrainian Wiki");

        c.setWikiUrl("http://localhost/");

        c.setContentLang("uk");

        c.setIwPrefix("uk");
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
                "Медіа",
                "Media",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                -1,
                "Спеціальна",
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
                "Обговорення",
                "Talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2,
                "Користувач",
                "User",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                3,
                "Обговорення користувача",
                "User talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                4,
                "Вікіпедія",
                "Wikipedia",
                false,
                false,
                Arrays.asList("WP")));

        c.addNamespace(new NamespaceImpl(
                5,
                "Обговорення Вікіпедії",
                "Wikipedia talk",
                false,
                false,
                Arrays.asList("WT")));

        c.addNamespace(new NamespaceImpl(
                6,
                "Файл",
                "File",
                false,
                true,
                Arrays.asList("Image")));

        c.addNamespace(new NamespaceImpl(
                7,
                "Обговорення файлу",
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
                "Обговорення MediaWiki",
                "MediaWiki talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                10,
                "Шаблон",
                "Template",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                11,
                "Обговорення шаблону",
                "Template talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                12,
                "Довідка",
                "Help",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                13,
                "Обговорення довідки",
                "Help talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                14,
                "Категорія",
                "Category",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                15,
                "Обговорення категорії",
                "Category talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                100,
                "Портал",
                "Portal",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                101,
                "Обговорення порталу",
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
                "Модуль",
                "Module",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                829,
                "Обговорення модуля",
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

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }
}
