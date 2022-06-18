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
 * Default configuration for the Farsi Wikipedia.
 */

/*

For reference:

<siteinfo>
    <sitename>ویکی‌پدیا</sitename>
    <dbname>fawiki</dbname>
    <base>https://fa.wikipedia.org/wiki/%D8%B5%D9%81%D8%AD%D9%87%D9%94_%D8%A7%D8%B5%D9%84%DB%8C</base>
    <generator>MediaWiki 1.39.0-wmf.13</generator>
    <case>first-letter</case>
    <namespaces>
      <namespace key="-2" case="first-letter">مدیا</namespace>
      <namespace key="-1" case="first-letter">ویژه</namespace>
      <namespace key="0" case="first-letter" />
      <namespace key="1" case="first-letter">بحث</namespace>
      <namespace key="2" case="first-letter">کاربر</namespace>
      <namespace key="3" case="first-letter">بحث کاربر</namespace>
      <namespace key="4" case="first-letter">ویکی‌پدیا</namespace>
      <namespace key="5" case="first-letter">بحث ویکی‌پدیا</namespace>
      <namespace key="6" case="first-letter">پرونده</namespace>
      <namespace key="7" case="first-letter">بحث پرونده</namespace>
      <namespace key="8" case="first-letter">مدیاویکی</namespace>
      <namespace key="9" case="first-letter">بحث مدیاویکی</namespace>
      <namespace key="10" case="first-letter">الگو</namespace>
      <namespace key="11" case="first-letter">بحث الگو</namespace>
      <namespace key="12" case="first-letter">راهنما</namespace>
      <namespace key="13" case="first-letter">بحث راهنما</namespace>
      <namespace key="14" case="first-letter">رده</namespace>
      <namespace key="15" case="first-letter">بحث رده</namespace>
      <namespace key="100" case="first-letter">درگاه</namespace>
      <namespace key="101" case="first-letter">بحث درگاه</namespace>
      <namespace key="118" case="first-letter">پیش‌نویس</namespace>
      <namespace key="119" case="first-letter">بحث پیش‌نویس</namespace>
      <namespace key="446" case="first-letter">Education Program</namespace>
      <namespace key="447" case="first-letter">Education Program talk</namespace>
      <namespace key="828" case="first-letter">پودمان</namespace>
      <namespace key="829" case="first-letter">بحث پودمان</namespace>
      <namespace key="2300" case="first-letter">ابزار</namespace>
      <namespace key="2301" case="first-letter">بحث ابزار</namespace>
      <namespace key="2302" case="case-sensitive">توضیحات ابزار</namespace>
      <namespace key="2303" case="case-sensitive">بحث توضیحات ابزار</namespace>
      <namespace key="2600" case="first-letter">مبحث</namespace>
    </namespaces>
  </siteinfo>
*/

public class DefaultConfigFaWp extends DefaultConfigWp {

    public static WikiConfigImpl generate()
    {
        WikiConfigImpl c = new WikiConfigImpl();
        new DefaultConfigFaWp().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c)
    {
        c.setSiteName("My Farsi Wiki");

        c.setWikiUrl("http://localhost/");

        c.setContentLang("fa");

        c.setIwPrefix("fa");
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
                "مدیا",
                "Media",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                -1,
                "ویژه",
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
                "بحث",
                "Talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2,
                "کاربر",
                "User",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                3,
                "بحث کاربر",
                "User talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                4,
                //"ویکی‌پدیا",
                "ویکی\u200Cپدیا",
                "Wikipedia",
                false,
                false,
                Arrays.asList("WP")));

        c.addNamespace(new NamespaceImpl(
                5,
                "بحث ویکی\u200Cپدیا",
                "Wikipedia talk",
                false,
                false,
                Arrays.asList("WT")));

        c.addNamespace(new NamespaceImpl(
                6,
                "پرونده",
                "File",
                false,
                true,
                Arrays.asList("Image")));

        c.addNamespace(new NamespaceImpl(
                7,
                "بحث پرونده",
                "File talk",
                false,
                false,
                Arrays.asList("Image talk")));

        c.addNamespace(new NamespaceImpl(
                8,
                "مدیاویکی",
                "MediaWiki",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                9,
                "بحث مدیاویکی",
                "MediaWiki talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                10,
                "الگو",
                "Template",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                11,
                "بحث الگو",
                "Template talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                12,
                "راهنما",
                "Help",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                13,
                "بحث راهنما",
                "Help talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                14,
                "رده",
                "Category",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                15,
                "بحث رده",
                "Category talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                100,
                "درگاه",
                "Portal",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                101,
                "بحث درگاه",
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
                118,
                "پیش\u200Cنویس",
                "Draft",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                119,
                "بحث پیش\u200Cنویس",
                "Draft talk",
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
                828,
                "پودمان",
                "Module",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                829,
                "بحث پودمان",
                "Module talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2300,
                "ابزار",
                "Gadget",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2301,
                "بحث ابزار",
                "Gadget talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2302,
                "توضیحات ابزار",
                "Gadget definition",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2303,
                "بحث توضیحات ابزار",
                "Gadget definition talk",
                false,
                false,
                new ArrayList<String>()));      

        c.addNamespace(new NamespaceImpl(
                2600,
                "مبحث",
                "Topic",
                false,
                false,
                new ArrayList<String>()));

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }
}
