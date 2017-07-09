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
 * Programatically generate a default configuration for that is similar to that
 * of the English Wikipedia.
 */
public class DefaultConfigEnWp extends DefaultConfigWp {

	public static WikiConfigImpl generate()
	{
		WikiConfigImpl c = new WikiConfigImpl();
		new DefaultConfigEnWp().configureWiki(c);
		return c;
	}

	protected void configureSiteProperties(WikiConfigImpl c)
	{
		c.setSiteName("My English Wiki");

		c.setWikiUrl("http://localhost/");

		c.setContentLang("en");

		c.setIwPrefix("en");
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
				"User",
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
				108,
				"Book",
				"Book",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				109,
				"Book talk",
				"Book talk",
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

		c.setDefaultNamespace(c.getNamespace(0));
		c.setTemplateNamespace(c.getNamespace(10));
	}
}
