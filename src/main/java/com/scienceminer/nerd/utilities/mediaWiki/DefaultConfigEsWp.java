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
 * of the Spanish Wikipedia.
 */
public class DefaultConfigEsWp extends DefaultConfigWp {

	public static WikiConfigImpl generate()
	{
		WikiConfigImpl c = new WikiConfigImpl();
		new DefaultConfigEsWp().configureWiki(c);
		return c;
	}

	protected void configureSiteProperties(WikiConfigImpl c)
	{
		c.setSiteName("My Spanish Wiki");

		c.setWikiUrl("http://localhost/");

		c.setContentLang("es");

		c.setIwPrefix("es");
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
				"Medio",
				"Media",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				-1,
				"Especial",
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
				"Discusión",
				"Talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2,
				"Usuario",
				"User",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				3,
				"Usuario discusión",
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
				"Wikipedia discusión",
				"Wikipedia talk",
				false,
				false,
				Arrays.asList("WT")));

		c.addNamespace(new NamespaceImpl(
				6,
				"Archivo",
				"File",
				false,
				true,
				Arrays.asList("Image")));

		c.addNamespace(new NamespaceImpl(
				7,
				"Archivo discusión",
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
				"MediaWiki discusión",
				"MediaWiki talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				10,
				"Plantilla",
				"Template",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				11,
				"Plantilla discusión",
				"Template talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				12,
				"Ayuda",
				"Help",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				13,
				"Ayuda discusión",
				"Help talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				14,
				"Categoría",
				"Category",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				15,
				"Categoría discusión",
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
				"Portal Discusión",
				"Portal talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				102,
				"Wikiproyecto",
				"Project",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				103,
				"Wikiproyecto Discusión",
				"Project talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				104,
				"Anexo",
				"Annex",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				105,
				"Anexo Discusión",
				"Annex talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				446,
				"Programa educativo",
				"Education Program",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				447,
				"Programa educativo discusión",
				"Education Program talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				828,
				"Módulo",
				"Module",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				829,
				"Módulo discusión",
				"Module talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2300,
				"Accesorio",
				"Gadget",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2301,
				"Accesorio discusión",
				"Gadget talk",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2302,
				"Accesorio definición",
				"Gadget definition",
				false,
				false,
				new ArrayList<String>()));

		c.addNamespace(new NamespaceImpl(
				2303,
				"Accesorio definición discusión",
				"Gadget definition talk",
				false,
				false,
				new ArrayList<String>()));		

		c.setDefaultNamespace(c.getNamespace(0));
		c.setTemplateNamespace(c.getNamespace(10));
	}
}
