package com.scienceminer.nerd.kb.model;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.kb.model.hadoop.DbIntList;
import com.scienceminer.nerd.kb.model.hadoop.DbPage;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.utilities.MediaWikiParser;

/**
 * Represents pages of any type in Wikipedia
 */
public class Page implements Comparable<Page> {

	/**
	 * Types that wikipedia pages can be.
	 */
	public enum PageType {

		/**
		 * A page that provides informative text about a topic.
		 */
		article, 

		/**
		 * A page that hierarchically organises other pages
		 */
		category, 

		/**
		 * A page that exists only to connect an alternative title to an article
		 */
		redirect, 

		/**
		 * A page that lists possible senses of an ambiguous word
		 */
		disambiguation, 

		/**
		 * A page that can be transcluded into other pages
		 */
		template,

		/**
		 * A type of page that we don't currently deal with (e.g templates)
		 */
		invalid
	};

	protected int id;
	protected String wikidataId = null;
	protected String title = null;
	protected PageType type;
	protected int depth;
	protected Double weight = null;

	protected KBLowerEnvironment env = null;
	protected boolean detailsSet = false;

	protected Page(KBLowerEnvironment env, int id, DbPage pd)  {
		this.env = env;
		this.id = id;
		setDetails(pd);
	}

	public Page(KBLowerEnvironment env, int id) {
		this.env = env;
		this.id = id;
		this.detailsSet = false;
	}

	public String getWikidataId() {
		if (!detailsSet) 
			setDetails();
		return wikidataId;
	}

	/**
	 *  @return true if a page with this id is defined in Wikipedia, otherwise false.
	 */
	public boolean exists() {
		if (!detailsSet) 
			setDetails();
		return (type != PageType.invalid);
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getWeight() {
		return weight;
	}

	public boolean equals(Page p) {
		return p.id == id;
	}

	/**
	 * Compares this page to another page based on their weights. 
	 * 
	 */
    @Override
	public int compareTo(Page p) {
		if (p.id == id)
			return 0;
		int cmp = 0;
		if (p.weight != null && weight != null && p.weight != weight)
			cmp =  p.weight.compareTo(weight); 
		if (cmp == 0)
			cmp = new Integer(id).compareTo(p.id);
		return cmp;
	}

    @Override
	public String toString()  {
		String s = getId() + ": " + getTitle();
		return s;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		if (!detailsSet) 
			setDetails();

		return title;
	}

	public PageType getType() {
		if (!detailsSet) 
			setDetails();

		return type;
	}

	/**
	 * @return the full content of this page, in mediawiki markup format
	 */
	public String getFullMarkup() {
		return env.getDbMarkupFull().retrieve(id);
	}

	/**
	 * Returns the first paragraph from the content of this page, cleaned of all markup except links and 
	 * basic formating. 
	 * 
	 */
	public String getFirstParagraphMarkup() {
		return env.getDbMarkup().retrieve(id);
	}

	public static String formatFirstParagraphMarkup(String markup) {
		MediaWikiParser stripper = new MediaWikiParser();

		markup = markup.replaceAll("={2,}(.+)={2,}", "\n"); //clear section headings completely - not just formating, but content as well.			
		markup = stripper.stripAllButInternalLinksAndEmphasis(markup, null);
		markup = stripper.stripNonArticleInternalLinks(markup, null);
		markup = stripper.stripExcessNewlines(markup);

		String fp = "";
		int pos = markup.indexOf("\n\n");

		while (pos>=0) {
			fp = markup.substring(0, pos);

			if (pos > 150) 
				break;

			pos = markup.indexOf("\n\n", pos+2);
		}

		fp = fp.replaceAll("\n", " ");
		fp = fp.replaceAll("\\s+", " ");  //turn all whitespace into spaces, and collapse them.
		fp = fp.trim();

		return fp;
	}

	public static String formatAllMarkup(String markup) {
		MediaWikiParser stripper = new MediaWikiParser();

		//markup = markup.replaceAll("={2,}(.+)={2,}", "\n"); //clear section headings completely - not just formating, but content as well.			
		markup = stripper.stripAllButInternalLinksAndEmphasis(markup, null);
		markup = stripper.stripNonArticleInternalLinks(markup, null);
		markup = stripper.stripExcessNewlines(markup);

		markup = markup.replaceAll("\n", " ");
		markup = markup.replaceAll("\\s+", " ");  //turn all whitespace into spaces, and collapse them.
		markup = markup.trim();

		return markup;
	}

	/**
	 * Instantiates the appropriate subclass of Page given the supplied parameters
	 */
	public static Page createPage(KBLowerEnvironment env, int id)  {
		DbPage pd = env.getDbPage().retrieve(id); 
		if (pd != null)
			return createPage(env, id, pd);
		else {
			pd = new DbPage("Invalid id or excluded via caching", PageType.invalid.ordinal(), -1);
			return new Page(env, id, pd);
			//return null;
		}
	}

	/**
	 * Instantiates the appropriate subclass of Page given the supplied parameters
	 * 
	 */
	public static Page createPage(KBLowerEnvironment env, int id, DbPage pd) {
		Page p = null;
		PageType type = PageType.values()[pd.getType()];
		switch (type) {
			case article:
				p = new Article(env, id, pd);
				break;
			case redirect:
				p = new Redirect(env, id, pd);
				break;
			case disambiguation:
				p = new Disambiguation(env, id, pd);
				break;
			case category:
				p = new Category(env, id, pd);
				break;
			case template:
				p = new Template(env, id, pd);
				break;
			default:
				p = new Page(env, id, pd);
		}

		return p;
	}

	private void setDetails()  {
		DbPage pd = env.getDbPage().retrieve(id);
		if (pd == null) {
			title = null;
			type = PageType.invalid;
		} else {
			setDetails(pd);
		}
	}

	private void setDetails(DbPage pd)  {
		title = pd.getTitle();
		type = PageType.values()[pd.getType()];
		depth = pd.getDepth();
		wikidataId = env.getDbConceptByPageId().retrieve(id);
		detailsSet = true;
	}

}
