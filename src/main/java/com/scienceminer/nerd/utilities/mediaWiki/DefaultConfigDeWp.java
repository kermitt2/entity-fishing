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
 * of the German Wikipedia.
 */
public class DefaultConfigDeWp extends DefaultConfigWp {

	public static WikiConfigImpl generate()
	{
		WikiConfigImpl c = new WikiConfigImpl();
		new DefaultConfigDeWp().configureWiki(c);
		return c;
	}

	protected void configureSiteProperties(WikiConfigImpl c)
	{
		c.setSiteName("My German Wiki");

		c.setWikiUrl("http://localhost/");

		c.setContentLang("de");

		c.setIwPrefix("de");
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
				"Medium",
				"Media",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				-1,
				"Spezial",
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
				"Diskussion",
				"Talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2,
				"Benutzer",
				"User",
				false,
				false,
				Arrays.asList("Benutzerin")));

		c.addNamespace(new NamespaceImpl(
				3,
				"Benutzer Diskussion",
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
				"Wikipedia Diskussion",
				"Wikipedia talk",
				false,
				false,
				Arrays.asList("WT")));

		c.addNamespace(new NamespaceImpl(
				6,
				"Datei",
				"File",
				false,
				true,
				Arrays.asList("Image")));

		c.addNamespace(new NamespaceImpl(
				7,
				"Datei Diskussion",
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
				"MediaWiki Diskussion",
				"MediaWiki talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				10,
				"Vorlage",
				"Template",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				11,
				"Vorlage Diskussion",
				"Template talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				12,
				"Hilfe",
				"Help",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				13,
				"Hilfe Diskussion",
				"Help talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				14,
				"Kategorie",
				"Category",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				15,
				"Kategorie Diskussion",
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
				"Portal Diskussion",
				"Portal talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				828,
				"Modul",
				"Module",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				829,
				"Modul Diskussion",
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
				"Gadget Diskussion",
				"Gadget talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2302,
				"Gadget-Definition",
				"Gadget definition",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2303,
				"Gadget-Definition Diskussion",
				"Gadget definition talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2600,
				"Thema",
				"Theme",
				false,
				false,
				new ArrayList<String>()));

		c.setDefaultNamespace(c.getNamespace(0));
		c.setTemplateNamespace(c.getNamespace(10));
	}

}
