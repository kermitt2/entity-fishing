package com.scienceminer.nerd.kb.model;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;

import org.wikipedia.miner.db.struct.DbIntList;
import org.wikipedia.miner.db.struct.DbPage;

import com.scienceminer.nerd.kb.db.KBEnvironment;

import org.wikipedia.miner.util.MarkupStripper;

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
	} ;

	protected int id ;
	protected String title ;
	protected PageType type ;
	protected int depth ;
	protected Double weight = null ;

	protected KBEnvironment env ;
	protected boolean detailsSet ;

	//constructor =============================================================

	/**
	 * Initialises a newly created Page so that it represents the page given by <em>id</em> and <em>DbPage</em>.
	 * 
	 * This is the most efficient page constructor as no database lookup is required.
	 * 
	 * @param	env	an active WikipediaEnvironment
	 * @param	id	the unique identifier of the page
	 * @param	pd  details (title, type, etc) of the page	 
	 */
	protected Page(KBEnvironment env, int id, DbPage pd)  {
		this.env = env ;
		this.id = id ;
		setDetails(pd) ;
	}


	/**
	 * Initialises a newly created Page so that it represents the page given by <em>id</em>. This is also an efficient
	 * constructor, since details (page title, type, etc) are only retrieved when requested.
	 * 
	 * @param	env	an active WikipediaEnvironment
	 * @param	id	the unique identifier of the Wikipedia page
	 */
	public Page(KBEnvironment env, int id) {
		this.env = env ;
		this.id = id ;
		this.detailsSet = false ;
	}


	//public ==================================================================

	/**
	 * @return the database environment
	 */
	public KBEnvironment getEnvironment() {
		return env;
	}


	/**
	 *  @return true if a page with this id is defined in Wikipedia, otherwise false.
	 */
	public boolean exists() {
		if (!detailsSet) 
			setDetails() ;

		return (type != PageType.invalid) ;
	}

	/**
	 * Sets the weight by which this page will be compared to others.
	 * 
	 * @param weight  the weight by which this page will be compared to others.
	 */
	public void setWeight(Double weight) {
		this.weight = weight ;
	}

	/**
	 * @return the weight by which this page is compared to others. (may be null, in which case the page is compared only via id)
	 */	
	public Double getWeight() {
		return weight ;
	}


	/**
	 * @param p the page to compare to
	 * @return true if this page has the same id as the given one, otherwise false
	 */
	public boolean equals(Page p) {
		return p.id == id ;
	}

	/**
	 * Compares this page to another. If weights are defined for both pages, then the page with the larger 
	 * weight will be considered smaller (and thus appear earlier in sorted lists). Otherwise, the comparison is made based on their ids. 
	 * 
	 * @param	p	the Page to be compared
	 * @return	see above.
	 */
        @Override
	public int compareTo(Page p) {

		if (p.id == id)
			return 0 ;

		int cmp = 0 ;

		if (p.weight != null && weight != null && p.weight != weight)
			cmp =  p.weight.compareTo(weight) ; 

		if (cmp == 0)
			cmp = new Integer(id).compareTo(p.id) ;

		return cmp ;

	}

	/**
	 * Returns a string representation of this page, in the format "<em>id</em>: <em>title</em>".
	 * 
	 * @return a string representation of the page
	 */
        @Override
	public String toString()  {
		String s = getId() + ": " + getTitle() ;
		return s ;
	}



	/**
	 * @return the unique identifier
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		if (!detailsSet) setDetails() ;

		return title;
	}

	/**
	 * @return	the type of the page
	 */
	public PageType getType() {
		if (!detailsSet) setDetails() ;

		return type;
	}

	/**
	 * @return the length of the shortest path from this page to the root category, or null if no path exists.
	 */
	public Integer getDepth() {
		if (!detailsSet) setDetails() ;

		if (depth < 0)
			return null ;
		else
			return depth ;
	}

	/**
	 * @return a number representing the height of this page in the category hierarchy, between {@value 0} (as far from the root category as possible) and 1 {the root category}, or null if no path exists 
	 */
	public Float getGenerality() {

		Integer d = getDepth() ;

		if (d == null)
			return null ;


		int maxDepth = env.retrieveStatistic(StatisticName.maxCategoryDepth).intValue() ;

		return 1-((float)d/maxDepth) ;
	}

	/**
	 * @return the content of this page, in mediawiki markup format
	 */
	public String getMarkup() {

		String markup = env.getDbMarkup().retrieve(id) ; 
		return markup ;

	}

	/**
	 * @return the character positions of sentence breaks within this page's content
	 */
	public Integer[] getSentenceSplits() {

		DbIntList splits = env.getDbSentenceSplits().retrieve(id) ; 

		if (splits == null || splits.getValues() == null) 
			return new Integer[0] ;

		return splits.getValues().toArray(new Integer[splits.getValues().size()]) ;
	}

	/**
	 * @param index the index of the desired sentence
	 * @return the content of the desired sentence, in mediawiki markup format
	 */
	public String getSentenceMarkup(int index) {

		String markup = getMarkup() ;
		Integer[] splits = getSentenceSplits() ;

		MarkupStripper s = new MarkupStripper() ;
		markup = s.stripAllButInternalLinksAndEmphasis(markup, ' ') ;
		markup = s.stripNonArticleInternalLinks(markup, ' ') ;

		String sentence ;

		if (splits.length == 0)
			sentence = markup ;
		else if (index == 0)
			sentence = markup.substring(0, splits[0]) ;
		else if (index < splits.length) 
			sentence = markup.substring(splits[index-1], splits[index]) ;
		else if (index == splits.length)
			sentence = markup.substring(splits[index-1]) ;
		else
			sentence = "" ;

		sentence = sentence.replaceAll("\\s+", " ") ;

		return sentence.trim();
	}

	/**
	 * Returns the first paragraph from the content of this page, cleaned of all markup except links and 
	 * basic formating. 
	 * This generally serves as a more specific definition of the concept or concepts for which this 
	 * article, disambiguation page or category was written.
	 * 
	 * @return the first paragraph on this page.
	 */
	public String getFirstParagraphMarkup() {

		MarkupStripper stripper = new MarkupStripper() ;

		String markup = getMarkup() ;

		/*markup = markup.replaceAll("={2,}(.+)={2,}", "\n") ; //clear section headings completely - not just formating, but content as well.			
		markup = stripper.stripAllButInternalLinksAndEmphasis(markup, null) ;
		markup = stripper.stripNonArticleInternalLinks(markup, null) ;
		markup = stripper.stripExcessNewlines(markup) ;

		String fp = "";
		int pos = markup.indexOf("\n\n") ;

		while (pos>=0) {
			fp = markup.substring(0, pos) ;

			if (pos > 150) 
				break ;

			pos = markup.indexOf("\n\n", pos+2) ;
		}

		//fp = stripper.stripAllButInternalLinksAndEmphasis(fp, null) ;
		//fp = stripper.stripNonArticleInternalLinks(fp, null) ;

		fp = fp.replaceAll("\n", " ") ;
		fp = fp.replaceAll("\\s+", " ") ;  //turn all whitespace into spaces, and collapse them.
		fp = fp.trim();

		return fp ;*/
		return markup;
	}

	public static String formatFirstParagraphMarkup(String markup) {
		MarkupStripper stripper = new MarkupStripper() ;

		markup = markup.replaceAll("={2,}(.+)={2,}", "\n") ; //clear section headings completely - not just formating, but content as well.			
		markup = stripper.stripAllButInternalLinksAndEmphasis(markup, null) ;
		markup = stripper.stripNonArticleInternalLinks(markup, null) ;
		markup = stripper.stripExcessNewlines(markup) ;

		String fp = "";
		int pos = markup.indexOf("\n\n") ;

		while (pos>=0) {
			fp = markup.substring(0, pos) ;

			if (pos > 150) 
				break ;

			pos = markup.indexOf("\n\n", pos+2) ;
		}

		fp = fp.replaceAll("\n", " ") ;
		fp = fp.replaceAll("\\s+", " ") ;  //turn all whitespace into spaces, and collapse them.
		fp = fp.trim();

		return fp ;
	}

	//public static ============================================================


	/**
	 * Instantiates the appropriate subclass of Page given the supplied parameters
	 * 
	 * @param env an active Wikipedia environment
	 * @param id the id of the page
	 * @return the instantiated page, which can be safely cast as appropriate
	 */
	public static Page createPage(KBEnvironment env, int id)  {
		DbPage pd = env.getDbPage().retrieve(id); 

		if (pd != null)
			return createPage(env, id, pd);
		else {
			pd = new DbPage("Invalid id or excluded via caching", PageType.invalid.ordinal(), -1);

			return new Page(env, id, pd);
		}
	}

	/**
	 * Instantiates the appropriate subclass of Page given the supplied parameters
	 * 
	 * @param env an active Wikipedia environment
	 * @param id the id of the page
	 * @param pd the details of the page
	 * @return the instantiated page, which can be safely cast as appropriate
	 */
	public static Page createPage(KBEnvironment env, int id, DbPage pd) {

		Page p = null ;

		PageType type = PageType.values()[pd.getType()] ;

		switch (type) {
			case article:
				p = new Article(env, id, pd) ;
				break ;
			case redirect:
				p = new Redirect(env, id, pd) ;
				break ;
			case disambiguation:
				p = new Disambiguation(env, id, pd) ;
				break ;
			case category:
				p = new Category(env, id, pd) ;
				break ;
			case template:
				p = new Template(env, id, pd) ;
				break ;
			default:
				p = new Page(env, id, pd) ;
		}

		return p ;
	}


	//protected and private ====================================================

	private void setDetails()  {

		try {
			DbPage pd = env.getDbPage().retrieve(id) ;

			if (pd == null) {
				throw new Exception() ;
			} else {
				setDetails(pd) ;
			}
		} catch (Exception e) {
			title = null ;
			type = PageType.invalid ;
		}
	}

	private void setDetails(DbPage pd)  {

		title = pd.getTitle() ;
		type = PageType.values()[pd.getType()] ;
		depth = pd.getDepth() ;

		detailsSet = true ;
	}

}
