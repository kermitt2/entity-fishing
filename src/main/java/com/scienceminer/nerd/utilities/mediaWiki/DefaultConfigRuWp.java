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
 * Default configuration for the Russian Wikipedia.
 */

/* For reference:
  <siteinfo>
    <sitename>Википедия</sitename>
    <dbname>ruwiki</dbname>
    <base>https://ru.wikipedia.org/wiki/%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F_%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0</base>
    <generator>MediaWiki 1.39.0-wmf.7</generator>
    <case>first-letter</case>
    <namespaces>
      <namespace key="-2" case="first-letter">Медиа</namespace>
      <namespace key="-1" case="first-letter">Служебная</namespace>
      <namespace key="0" case="first-letter" />
      <namespace key="1" case="first-letter">Обсуждение</namespace>
      <namespace key="2" case="first-letter">Участник</namespace>
      <namespace key="3" case="first-letter">Обсуждение участника</namespace>
      <namespace key="4" case="first-letter">Википедия</namespace>
      <namespace key="5" case="first-letter">Обсуждение Википедии</namespace>
      <namespace key="6" case="first-letter">Файл</namespace>
      <namespace key="7" case="first-letter">Обсуждение файла</namespace>
      <namespace key="8" case="first-letter">MediaWiki</namespace>
      <namespace key="9" case="first-letter">Обсуждение MediaWiki</namespace>
      <namespace key="10" case="first-letter">Шаблон</namespace>
      <namespace key="11" case="first-letter">Обсуждение шаблона</namespace>
      <namespace key="12" case="first-letter">Справка</namespace>
      <namespace key="13" case="first-letter">Обсуждение справки</namespace>
      <namespace key="14" case="first-letter">Категория</namespace>
      <namespace key="15" case="first-letter">Обсуждение категории</namespace>
      <namespace key="100" case="first-letter">Портал</namespace>
      <namespace key="101" case="first-letter">Обсуждение портала</namespace>
      <namespace key="102" case="first-letter">Инкубатор</namespace>
      <namespace key="103" case="first-letter">Обсуждение Инкубатора</namespace>
      <namespace key="104" case="first-letter">Проект</namespace>
      <namespace key="105" case="first-letter">Обсуждение проекта</namespace>
      <namespace key="106" case="first-letter">Арбитраж</namespace>
      <namespace key="107" case="first-letter">Обсуждение арбитража</namespace>
      <namespace key="828" case="first-letter">Модуль</namespace>
      <namespace key="829" case="first-letter">Обсуждение модуля</namespace>
      <namespace key="2300" case="first-letter">Гаджет</namespace>
      <namespace key="2301" case="first-letter">Обсуждение гаджета</namespace>
      <namespace key="2302" case="case-sensitive">Определение гаджета</namespace>
      <namespace key="2303" case="case-sensitive">Обсуждение определения гаджета</namespace>
    </namespaces>
  </siteinfo>

*/

public class DefaultConfigRuWp extends DefaultConfigWp {

    public static WikiConfigImpl generate()
    {
        WikiConfigImpl c = new WikiConfigImpl();
        new DefaultConfigRuWp().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c)
    {
        c.setSiteName("My Russian Wiki");

        c.setWikiUrl("http://localhost/");

        c.setContentLang("ru");

        c.setIwPrefix("ru");
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
                "Медиа",
                "Media",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                -1,
                "Служебная",
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
                "Обсуждение",
                "Talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2,
                "Участник",
                "User",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                3,
                "Обсуждение участника",
                "User talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                4,
                "Википедия",
                "Wikipedia",
                false,
                false,
                Arrays.asList("WP")));

        c.addNamespace(new NamespaceImpl(
                5,
                "Обсуждение Википедии",
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
                "Обсуждение файла",
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
                "Обсуждение MediaWiki",
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
                "Обсуждение шаблона",
                "Template talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                12,
                "Справка",
                "Help",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                13,
                "Обсуждение справки",
                "Help talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                14,
                "Категория",
                "Category",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                15,
                "Обсуждение категории",
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
                "Обсуждение портала",
                "Portal talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                102,
                "Инкубатор",
                "WikiProject",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                103,
                "Обсуждение Инкубатора",
                "WikiProject talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                104,
                "Проект",
                "Projet",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                105,
                "Обсуждение проекта",
                "Project talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                106,
                "Арбитраж",
                "Mediation",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                107,
                "Обсуждение арбитража",
                "Mediation talk",
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
                "Обсуждение модуля",
                "Module talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2300,
                "Гаджет",
                "Gadget",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2301,
                "Обсуждение гаджета",
                "Gadget talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2302,
                "Определение гаджета",
                "Gadget definition",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2303,
                "Обсуждение определения гаджета",
                "Gadget definition talk",
                false,
                false,
                new ArrayList<String>()));      

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }
}
