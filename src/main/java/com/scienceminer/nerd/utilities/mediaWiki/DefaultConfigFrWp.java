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
 * of the French Wikipedia.
 */
public class DefaultConfigFrWp extends DefaultConfigWp {

	public static WikiConfigImpl generate()
	{
		WikiConfigImpl c = new WikiConfigImpl();
		new DefaultConfigFrWp().configureWiki(c);
		return c;
	}

	protected void configureSiteProperties(WikiConfigImpl c)
	{
		c.setSiteName("My French Wiki");

		c.setWikiUrl("http://localhost/");

		c.setContentLang("fr");

		c.setIwPrefix("fr");
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
				"Discussion",
				"Talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2,
				"Utilisateur",
				"User",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				3,
				"Discussion utilisateur",
				"User talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				4,
				"Wikipédia",
				"Project",
				false,
				false,
				Arrays.asList("WP")));

		c.addNamespace(new NamespaceImpl(
				5,
				"Discussion Wikipédia",
				"Project talk",
				false,
				false,
				Arrays.asList("WT")));

		c.addNamespace(new NamespaceImpl(
				6,
				"Fichier",
				"File",
				false,
				true,
				Arrays.asList("Image")));

		c.addNamespace(new NamespaceImpl(
				7,
				"Discussion fichier",
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
				"Discussion MediaWiki",
				"MediaWiki talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				10,
				"Modèle",
				"Template",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				11,
				"Discussion Modèle",
				"Template talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				12,
				"Aide",
				"Help",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				13,
				"Discussion aide",
				"Help talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				14,
				"Catégorie",
				"Category",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				15,
				"Discussion catégorie",
				"Category talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				100,
				"Portail",
				"Portal",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				101,
				"Discussion Portail",
				"Portal talk",
				false,
				false,
				new ArrayList<String>()));

		c.setDefaultNamespace(c.getNamespace(0));
		c.setTemplateNamespace(c.getNamespace(10));
	}
}
