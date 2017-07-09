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
 * of the Italian Wikipedia.
 */
public class DefaultConfigItWp extends DefaultConfigWp {

	public static WikiConfigImpl generate()
	{
		WikiConfigImpl c = new WikiConfigImpl();
		new DefaultConfigItWp().configureWiki(c);
		return c;
	}

	protected void configureSiteProperties(WikiConfigImpl c)
	{
		c.setSiteName("My Italian Wiki");

		c.setWikiUrl("http://localhost/");

		c.setContentLang("it");

		c.setIwPrefix("it");
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
				"Speciale",
				"Special",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				0,
				"Principale",
				"",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				1,
				"Discussione",
				"Talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2,
				"Utente",
				"User",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				3,
				"Discussioni utente",
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
				"Discussioni Wikipedia",
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
				"Discussioni file",
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
				"Discussioni MediaWiki",
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
				"Discussioni template",
				"Template talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				12,
				"Aiuto",
				"Help",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				13,
				"Discussioni aiuto",
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
				"Discussioni categoria",
				"Category talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				100,
				"Portale",
				"Portal",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				101,
				"Discussioni portale",
				"Portal talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				102,
				"Progetto",
				"Project",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				103,
				"Discussioni progetto",
				"Project talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				828,
				"Modulo",
				"Module",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				829,
				"Discussioni modulo",
				"Module talk",
				false,
				false,
				new ArrayList<String>()));

		c.setDefaultNamespace(c.getNamespace(0));
		c.setTemplateNamespace(c.getNamespace(10));
	}
}
