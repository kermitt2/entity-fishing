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
 * Default configuration for the Arabic Wikipedia.
 */

/* For reference:
<siteinfo>
    <sitename>ويكيبيديا</sitename>
    <dbname>arwiki</dbname>
    <base>https://ar.wikipedia.org/wiki/%D8%A7%D9%84%D8%B5%D9%81%D8%AD%D8%A9_%D8%A7%D9%84%D8%B1%D8%A6%D9%8A%D8%B3%D9%8A%D8%A9</base>
    <generator>MediaWiki 1.38.0-wmf.19</generator>
    <case>first-letter</case>
    <namespaces>
      <namespace key="-2" case="first-letter">ميديا</namespace>
      <namespace key="-1" case="first-letter">خاص</namespace>
      <namespace key="0" case="first-letter" />
      <namespace key="1" case="first-letter">نقاش</namespace>
      <namespace key="2" case="first-letter">مستخدم</namespace>
      <namespace key="3" case="first-letter">نقاش المستخدم</namespace>
      <namespace key="4" case="first-letter">ويكيبيديا</namespace>
      <namespace key="5" case="first-letter">نقاش ويكيبيديا</namespace>
      <namespace key="6" case="first-letter">ملف</namespace>
      <namespace key="7" case="first-letter">نقاش الملف</namespace>
      <namespace key="8" case="first-letter">ميدياويكي</namespace>
      <namespace key="9" case="first-letter">نقاش ميدياويكي</namespace>
      <namespace key="10" case="first-letter">قالب</namespace>
      <namespace key="11" case="first-letter">نقاش القالب</namespace>
      <namespace key="12" case="first-letter">مساعدة</namespace>
      <namespace key="13" case="first-letter">نقاش المساعدة</namespace>
      <namespace key="14" case="first-letter">تصنيف</namespace>
      <namespace key="15" case="first-letter">نقاش التصنيف</namespace>
      <namespace key="100" case="first-letter">بوابة</namespace>
      <namespace key="101" case="first-letter">نقاش البوابة</namespace>
      <namespace key="828" case="first-letter">وحدة</namespace>
      <namespace key="829" case="first-letter">نقاش الوحدة</namespace>
      <namespace key="2300" case="first-letter">إضافة</namespace>
      <namespace key="2301" case="first-letter">نقاش الإضافة</namespace>
      <namespace key="2302" case="case-sensitive">تعريف الإضافة</namespace>
      <namespace key="2303" case="case-sensitive">نقاش تعريف الإضافة</namespace>
      <namespace key="2600" case="first-letter">موضوع</namespace>
    </namespaces>
  </siteinfo>
*/

public class DefaultConfigArWp extends DefaultConfigWp {

    public static WikiConfigImpl generate()
    {
        WikiConfigImpl c = new WikiConfigImpl();
        new DefaultConfigArWp().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c)
    {
        c.setSiteName("My Arabic Wiki");

        c.setWikiUrl("http://localhost/");

        c.setContentLang("ar");

        c.setIwPrefix("ar");
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
                "ميديا",
                "Media",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                -1,
                "خاص",
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
                "نقاش",
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
                "نقاش المستخدم",
                "User talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                4,
                "ويكيبيديا",
                "Wikipedia",
                false,
                false,
                Arrays.asList("WP")));

        c.addNamespace(new NamespaceImpl(
                5,
                "نقاش ويكيبيديا",
                "Wikipedia talk",
                false,
                false,
                Arrays.asList("WT")));

        c.addNamespace(new NamespaceImpl(
                6,
                "ملف",
                "File",
                false,
                true,
                Arrays.asList("Image")));

        c.addNamespace(new NamespaceImpl(
                7,
                "نقاش الملف",
                "File talk",
                false,
                false,
                Arrays.asList("Image talk")));

        c.addNamespace(new NamespaceImpl(
                8,
                "ميدياويكي",
                "MediaWiki",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                9,
                "نقاش ميدياويكي",
                "MediaWiki talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                10,
                "قالب",
                "Template",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                11,
                "نقاش القالب",
                "Template talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                12,
                "مساعدة",
                "Help",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                13,
                "نقاش المساعدة",
                "Help talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                14,
                "تصنيف",
                "Category",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                15,
                "نقاش التصنيف",
                "Category talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                100,
                "بوابة",
                "Portal",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                101,
                "نقاش البوابة",
                "Portal talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                828,
                "وحدة",
                "Module",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                829,
                "نقاش الوحدة",
                "Module talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2300,
                "إضافة",
                "Gadget",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2301,
                "نقاش الإضافة",
                "Gadget talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2302,
                "تعريف الإضافة",
                "Gadget definition",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2303,
                "نقاش تعريف الإضافة",
                "Gadget definition talk",
                false,
                false,
                new ArrayList<String>()));      

        c.addNamespace(new NamespaceImpl(
                2600,
                "موضوع",
                "Thema",
                false,
                false,
                new ArrayList<String>()));      

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }
}
