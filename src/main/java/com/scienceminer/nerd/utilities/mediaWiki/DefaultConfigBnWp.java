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
 * Default configuration for the Bengali Wikipedia.
 */

/*
 For reference, Dec 2022:

  <siteinfo>
    <sitename>উইকিপিডিয়া</sitename>
    <dbname>bnwiki</dbname>
    <base>https://bn.wikipedia.org/wiki/%E0%A6%AA%E0%A7%8D%E0%A6%B0%E0%A6%A7%E0%A6%BE%E0%A6%A8_%E0%A6%AA%E0%A6%BE%E0%A6%A4%E0%A6%BE</base>
    <generator>MediaWiki 1.40.0-wmf.10</generator>
    <case>first-letter</case>
    <namespaces>
      <namespace key="-2" case="first-letter">মিডিয়া</namespace>
      <namespace key="-1" case="first-letter">বিশেষ</namespace>
      <namespace key="0" case="first-letter" />
      <namespace key="1" case="first-letter">আলাপ</namespace>
      <namespace key="2" case="first-letter">ব্যবহারকারী</namespace>
      <namespace key="3" case="first-letter">ব্যবহারকারী আলাপ</namespace>
      <namespace key="4" case="first-letter">উইকিপিডিয়া</namespace>
      <namespace key="5" case="first-letter">উইকিপিডিয়া আলোচনা</namespace>
      <namespace key="6" case="first-letter">চিত্র</namespace>
      <namespace key="7" case="first-letter">চিত্র আলোচনা</namespace>
      <namespace key="8" case="first-letter">মিডিয়াউইকি</namespace>
      <namespace key="9" case="first-letter">মিডিয়াউইকি আলোচনা</namespace>
      <namespace key="10" case="first-letter">টেমপ্লেট</namespace>
      <namespace key="11" case="first-letter">টেমপ্লেট আলোচনা</namespace>
      <namespace key="12" case="first-letter">সাহায্য</namespace>
      <namespace key="13" case="first-letter">সাহায্য আলোচনা</namespace>
      <namespace key="14" case="first-letter">বিষয়শ্রেণী</namespace>
      <namespace key="15" case="first-letter">বিষয়শ্রেণী আলোচনা</namespace>
      <namespace key="100" case="first-letter">প্রবেশদ্বার</namespace>
      <namespace key="101" case="first-letter">প্রবেশদ্বার আলোচনা</namespace>
      <namespace key="710" case="first-letter">TimedText</namespace>
      <namespace key="711" case="first-letter">TimedText talk</namespace>
      <namespace key="828" case="first-letter">মডিউল</namespace>
      <namespace key="829" case="first-letter">মডিউল আলাপ</namespace>
      <namespace key="2300" case="case-sensitive">গ্যাজেট</namespace>
      <namespace key="2301" case="case-sensitive">গ্যাজেট আলোচনা</namespace>
      <namespace key="2302" case="case-sensitive">গ্যাজেট সংজ্ঞা</namespace>
      <namespace key="2303" case="case-sensitive">গ্যাজেট সংজ্ঞার আলোচনা</namespace>
    </namespaces>
  </siteinfo>

*/

public class DefaultConfigBnWp extends DefaultConfigWp {

    public static WikiConfigImpl generate()
    {
        WikiConfigImpl c = new WikiConfigImpl();
        new DefaultConfigBnWp().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c)
    {
        c.setSiteName("My Bengali Wiki");

        c.setWikiUrl("http://localhost/");

        c.setContentLang("bn");

        c.setIwPrefix("bn");
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
                "মিডিয়া",
                "Media",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                -1,
                "বিশেষ",
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
                "আলাপ",
                "Talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2,
                "ব্যবহারকারী",
                "User",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                3,
                "ব্যবহারকারী আলাপ",
                "User talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                4,
                "উইকিপিডিয়া",
                "Wikipedia",
                false,
                false,
                Arrays.asList("WP")));

        c.addNamespace(new NamespaceImpl(
                5,
                "উইকিপিডিয়া আলোচনা",
                "Wikipedia talk",
                false,
                false,
                Arrays.asList("WT")));

        c.addNamespace(new NamespaceImpl(
                6,
                "চিত্র",
                "File",
                false,
                true,
                Arrays.asList("Image")));

        c.addNamespace(new NamespaceImpl(
                7,
                "চিত্র আলোচনা",
                "File talk",
                false,
                false,
                Arrays.asList("Image talk")));

        c.addNamespace(new NamespaceImpl(
                8,
                "মিডিয়াউইকি",
                "MediaWiki",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                9,
                "মিডিয়াউইকি আলোচনা",
                "MediaWiki talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                10,
                "টেমপ্লেট",
                "Template",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                11,
                "টেমপ্লেট আলোচনা",
                "Template talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                12,
                "সাহায্য",
                "Help",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                13,
                "সাহায্য আলোচনা",
                "Help talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                14,
                "বিষয়শ্রেণী",
                "Category",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                15,
                "বিষয়শ্রেণী আলোচনা",
                "Category talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                100,
                "প্রবেশদ্বার",
                "Portal",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                101,
                "প্রবেশদ্বার আলোচনা",
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
                "মডিউল",
                "Module",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                829,
                "মডিউল আলাপ",
                "Module talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2300,
                "গ্যাজেট",
                "Gadget",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2301,
                "গ্যাজেট আলোচনা",
                "Gadget talk",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2302,
                "গ্যাজেট সংজ্ঞা",
                "Gadget definition",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                2303,
                "গ্যাজেট সংজ্ঞার আলোচনা",
                "Gadget definition talk",
                false,
                false,
                new ArrayList<String>()));

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }
}
