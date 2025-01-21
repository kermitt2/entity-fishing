package com.scienceminer.nerd.utilities.mediaWiki;

import org.sweble.wikitext.engine.config.NamespaceImpl;
import org.sweble.wikitext.engine.config.ParserConfigImpl;
import org.sweble.wikitext.engine.config.WikiConfigImpl;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Default configuration for the Dutch Wikipedia.
 *
 * <siteinfo>
 *     <sitename>Wikipedia</sitename>
 *     <dbname>nlwiki</dbname>
 *     <base>https://nl.wikipedia.org/wiki/Hoofdpagina</base>
 *     <generator>MediaWiki 1.44.0-wmf.8</generator>
 *     <case>first-letter</case>
 *     <namespaces>
 *       <namespace key="-2" case="first-letter">Media</namespace>
 *       <namespace key="-1" case="first-letter">Speciaal</namespace>
 *       <namespace key="0" case="first-letter" />
 *       <namespace key="1" case="first-letter">Overleg</namespace>
 *       <namespace key="2" case="first-letter">Gebruiker</namespace>
 *       <namespace key="3" case="first-letter">Overleg gebruiker</namespace>
 *       <namespace key="4" case="first-letter">Wikipedia</namespace>
 *       <namespace key="5" case="first-letter">Overleg Wikipedia</namespace>
 *       <namespace key="6" case="first-letter">Bestand</namespace>
 *       <namespace key="7" case="first-letter">Overleg bestand</namespace>
 *       <namespace key="8" case="first-letter">MediaWiki</namespace>
 *       <namespace key="9" case="first-letter">Overleg MediaWiki</namespace>
 *       <namespace key="10" case="first-letter">Sjabloon</namespace>
 *       <namespace key="11" case="first-letter">Overleg sjabloon</namespace>
 *       <namespace key="12" case="first-letter">Help</namespace>
 *       <namespace key="13" case="first-letter">Overleg help</namespace>
 *       <namespace key="14" case="first-letter">Categorie</namespace>
 *       <namespace key="15" case="first-letter">Overleg categorie</namespace>
 *       <namespace key="100" case="first-letter">Portaal</namespace>
 *       <namespace key="101" case="first-letter">Overleg portaal</namespace>
 *       <namespace key="710" case="first-letter">TimedText</namespace>
 *       <namespace key="711" case="first-letter">TimedText talk</namespace>
 *       <namespace key="828" case="first-letter">Module</namespace>
 *       <namespace key="829" case="first-letter">Overleg module</namespace>
 *     </namespaces>
 *   </siteinfo>
 */
public class DefaultConfigNlWp extends DefaultConfigWp {

	public static WikiConfigImpl generate()
	{
		WikiConfigImpl c = new WikiConfigImpl();
		new DefaultConfigNlWp().configureWiki(c);
		return c;
	}

	protected void configureSiteProperties(WikiConfigImpl c)
	{
		c.setSiteName("My Dutch Wiki");

		c.setWikiUrl("http://localhost/");

		c.setContentLang("nl");

		c.setIwPrefix("nl");
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
				"Speciaal",
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
				"Overleg",
				"Talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2,
				"Gebruiker",
				"User",
				false,
				false,
				Arrays.asList()));

		c.addNamespace(new NamespaceImpl(
				3,
				"Overleg gebruiker",
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
				"Overleg Wikipedia",
				"Wikipedia talk",
				false,
				false,
				Arrays.asList("WT")));

		c.addNamespace(new NamespaceImpl(
				6,
				"Bestand",
				"File",
				false,
				true,
				Arrays.asList("Image")));

		c.addNamespace(new NamespaceImpl(
				7,
				"Overleg bestand",
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
				"Overleg MediaWiki",
				"MediaWiki talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				10,
				"Sjabloon",
				"Template",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				11,
				"Overleg sjabloon",
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
				"Overleg help",
				"Help talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				14,
				"Categorie",
				"Category",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				15,
				"Overleg categorie",
				"Category talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				100,
				"Portaal",
				"Portal",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				101,
				"Overleg portaal",
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
				"Module",
				"Module",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				829,
				"Overleg module",
				"Module talk",
				false,
				false,
				new ArrayList<String>()));

		c.setDefaultNamespace(c.getNamespace(0));
		c.setTemplateNamespace(c.getNamespace(10));
	}

}
