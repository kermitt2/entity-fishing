package com.scienceminer.nerd.utilities.mediaWiki;

import org.sweble.wikitext.engine.config.NamespaceImpl;
import org.sweble.wikitext.engine.config.ParserConfigImpl;
import org.sweble.wikitext.engine.config.WikiConfigImpl;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Programatically generate a default configuration for that is similar to that
 * of the English Wikipedia.
 */
public class DefaultConfigJaWp extends DefaultConfigWp {

	public static WikiConfigImpl generate()
	{
		WikiConfigImpl c = new WikiConfigImpl();
		new DefaultConfigJaWp().configureWiki(c);
		return c;
	}

	protected void configureSiteProperties(WikiConfigImpl c)
	{
		c.setSiteName("My Japanese Wiki");

		c.setWikiUrl("http://localhost/");

		c.setContentLang("ja");

		c.setIwPrefix("ja");
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
				"メディア",
				"メディア",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				-1,
				"特別",
				"特別",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				0,
				"標準",
				"標準",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				1,
				"ノート",
				"ノート",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				2,
				"利用者",
				"利用者",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				3,
				"利用者‐会話",
				"利用者‐会話",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				4,
				"Wikipedia",
				"Wikipedia",
				false,
				false,
				Arrays.asList("WP")));

		c.addNamespace(new NamespaceImpl(
				5,
				"Wikipedia‐ノート",
				"Wikipedia‐ノート",
				false,
				false,
				Arrays.asList("WT")));

		c.addNamespace(new NamespaceImpl(
				6,
				"ファイル",
				"ファイル",
				false,
				true,
				Arrays.asList("Image")));

		c.addNamespace(new NamespaceImpl(
				7,
				"ファイル‐ノート",
				"ファイル‐ノート",
				false,
				false,
				Arrays.asList("Image talk")));

		c.addNamespace(new NamespaceImpl(
				8,
				"MediaWiki",
				"MediaWiki",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				9,
				"MediaWiki‐ノート",
				"MediaWiki‐ノート",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				10,
				"Template",
				"Template",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				11,
				"Template‐ノート",
				"Template‐ノート",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				12,
				"Help",
				"Help",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				13,
				"Help‐ノート",
				"Help‐ノート",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				14,
				"Category",
				"Category",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				15,
				"Category‐ノート",
				"Category‐ノート",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				100,
				"Portal",
				"Portal",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				101,
				"Portal‐ノート",
				"Portal‐ノート",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				102,
				"プロジェクト",
				"プロジェクト",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				103,
				"プロジェクト‐ノート",
				"プロジェクト‐ノート",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				828,
				"モジュール",
				"モジュール",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				829,
				"モジュール‐ノート",
				"モジュール‐ノート",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				2300,
				"Gadget",
				"Gadget",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				2301,
				"Gadget talk",
				"Gadget talk",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				2302,
				"Gadget definition",
				"Gadget definition",
				false,
				false,
				new ArrayList<>()));

		c.addNamespace(new NamespaceImpl(
				2303,
				"Gadget definition talk",
				"Gadget definition talk",
				false,
				false,
				new ArrayList<>()));		

		c.setDefaultNamespace(c.getNamespace(0));
		c.setTemplateNamespace(c.getNamespace(10));
	}
}
