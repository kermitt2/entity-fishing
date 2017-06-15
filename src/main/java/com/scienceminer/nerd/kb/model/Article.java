package com.scienceminer.nerd.kb.model;

import java.util.*; 

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.Property;
import com.scienceminer.nerd.kb.Relation;

import com.scienceminer.nerd.kb.model.hadoop.*;

/**
 * Represents a page of type article in Wikipedia, e.g. en entity or a concept
 * 
 *  -> to be replaced by a Concept object
 */
public class Article extends Page {

	/**
	 * Initialises an article
	 * 
	 * @param env	KBEnvironment
	 * @param id 	unique identifier of the article
	 */
	public Article(KBLowerEnvironment env, int id) {
		super(env, id);
	}

	protected Article(KBLowerEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
	}

	/**
	 * Return the list of properties associated to the article
	 */
	/*public List<Property> getProperties() {
		// get the concept id first
		String conceptId = env.getDbConceptByPageId().retrieve(id);
		if (conceptId == null)
			return null;
		else
			return env.getDbProperties().retrieve(conceptId);
	}*/

	/**
	 * Return the list of relations associated to the article
	 */
	/*public List<Relation> getRelations() {
		// get the concept id first
		String conceptId = env.getDbConceptByPageId().retrieve(id);
		if (conceptId == null)
			return null;
		else
			return env.getDbRelations().retrieve(conceptId);
	}*/

	/**
	 * Returns a array of {@link Redirect Redirects}, sorted by id, that point to this article.
	 * 
	 * @return	an array of Redirects, sorted by id
	 */
	public Redirect[] getRedirects()  {
		DbIntList tmpRedirects = env.getDbRedirectSourcesByTarget().retrieve(id);
		if (tmpRedirects == null || tmpRedirects.getValues() == null) 
			return new Redirect[0];

		Redirect[] redirects = new Redirect[tmpRedirects.getValues().size()];
		for (int i=0; i<tmpRedirects.getValues().size(); i++)
			redirects[i] = new Redirect(env, tmpRedirects.getValues().get(i));	

		return redirects;	
	}

	/**
	 * Returns an array of {@link Category Categories} that this article belongs to. These are the categories 
	 * that are linked to at the bottom of any Wikipedia article. Note that one of these will be the article's
	 * equivalent category, if one exists.
	 * 
	 * @return	an array of Categories, sorted by id
	 */
	public Category[] getParentCategories() {
		DbIntList tmpParents = env.getDbArticleParents().retrieve(id);
		if (tmpParents == null || tmpParents.getValues() == null) 
			return new Category[0];

		Category[] parentCategories = new Category[tmpParents.getValues().size()];
		int index = 0;
		for (int id : tmpParents.getValues()) {
			parentCategories[index] = new Category(env, id);
			index++;
		}

		return parentCategories;	
	}

	public Article[] getLinksIn() {			
		DbIntList tmpLinks = env.getDbPageLinkInNoSentences().retrieve(id);
		if (tmpLinks == null || tmpLinks.getValues() == null) 
			return new Article[0];

		Article[] links = new Article[tmpLinks.getValues().size()];

		int index = 0;
		for (Integer id : tmpLinks.getValues()) {
			links[index] = new Article(env, id.intValue());
			index++;
		}

		return links;	
	}

	public Article[] getLinksOut()  {
		DbIntList tmpLinks = env.getDbPageLinkOutNoSentences().retrieve(id);
		if (tmpLinks == null || tmpLinks.getValues() == null) 
			return new Article[0];

		Article[] links = new Article[tmpLinks.getValues().size()];

		int index = 0;
		for (Integer id : tmpLinks.getValues()) {
			links[index] = new Article(env, id.intValue());
			index++;
		}

		return links;	
	}

	/**
	 * Returns the title of the article translated into the language given by <em>languageCode</em>
	 * (i.e. fn, jp, de, etc) or null if translation is not available. 
	 * 
	 * @param languageCode	the (generally 2 character) language code.
	 * @return the translated title if it is available; otherwise null.
	 */	
	public String getTranslation(String languageCode)  {		
		DbTranslations t = env.getDbTranslations().retrieve(id);
		if (t == null)
			return null;

		if (t.getTranslationsByLangCode() == null)
			return null;

		return t.getTranslationsByLangCode().get(languageCode.toLowerCase());
	}

	/**
	 * Returns a TreeMap associating language code with translated title for all available translations 
	 * 
	 * @return a TreeMap associating language code with translated title.
	 */	
	public TreeMap<String,String> getTranslations() {
		DbTranslations t = env.getDbTranslations().retrieve(id);
		if (t == null)
			return new TreeMap<String,String>();
		else
			return t.getTranslationsByLangCode();
	}

	/**
	 * @return the total number of links that are made to this article 
	 */
	public int getTotalLinksInCount()  {
		DbPageLinkCounts lc = env.getDbPageLinkCounts().retrieve(id);
		if (lc == null) 
			return 0;
		else
			return lc.getTotalLinksIn();
	}

	/**
	 * @return the number of distinct articles which contain a link to this article 
	 */
	public int getDistinctLinksInCount()  {
		DbPageLinkCounts lc = env.getDbPageLinkCounts().retrieve(id);
		if (lc == null) 
			return 0;
		else
			return lc.getDistinctLinksIn();
	}

	/**
	 * @return the total number links that this article makes to other articles 
	 */
	public int getTotalLinksOutCount() {
		DbPageLinkCounts lc = env.getDbPageLinkCounts().retrieve(id);

		if (lc == null) 
			return 0;
		else
			return lc.getTotalLinksOut();
	}

	/**
	 * @return the number of distinct articles that this article links to 
	 */
	public int getDistinctLinksOutCount() {
		DbPageLinkCounts lc = env.getDbPageLinkCounts().retrieve(id);
		if (lc == null) 
			return 0;
		else
			return lc.getDistinctLinksOut();
	}

	/**
	 * Returns an array of {@link Label Labels} that have been used to refer to this article.
	 * They are sorted by the number of times each label is used.
	 * 
	 * @return an array of {@link Label Labels} that have been used to refer to this article. 
	 */
	/*public Label[] getLabels() {

		DbLabelForPageList tmpLabels = env.getDbLabelsForPage().retrieve(id); 
		if (tmpLabels == null || tmpLabels.getLabels() == null) 
			return new Label[0];

		Label[] labels = new Label[tmpLabels.getLabels().size()];

		int index = 0;
		for (DbLabelForPage ll:tmpLabels.getLabels()) {
			labels[index] = new Label(ll);
			index++;
		}

		return labels;	
	}*/

	/**
	 * This efficiently identifies sentences within this article that contain links to the given target article. 
	 * The actual text of these sentences can be obtained using {@link Page#getSentenceMarkup(int)}
	 * 
	 * @param art the article of interest. 
	 * @return an array of sentence indexes that contain links to the given article.
	 */
	/*public Integer[] getSentenceIndexesMentioning(Article art) {
		DbLinkLocationList tmpLinks = env.getDbPageLinkIn().retrieve(art.getId());
		if (tmpLinks == null || tmpLinks.getLinkLocations() == null) 
			return new Integer[0];

		DbLinkLocation key = new DbLinkLocation(id, null);
		int index = Collections.binarySearch(tmpLinks.getLinkLocations(), key, new Comparator<DbLinkLocation>(){
                        @Override
			public int compare(DbLinkLocation a, DbLinkLocation b) {
				return new Integer(a.getLinkId()).compareTo(b.getLinkId());
			}
		});

		if (index < 0)
			return new Integer[0];
		ArrayList<Integer> sentenceIndexes = tmpLinks.getLinkLocations().get(index).getSentenceIndexes();

		return sentenceIndexes.toArray(new Integer[sentenceIndexes.size()]);
	}*/

	/**
	 * This efficiently identifies sentences within this article that contain links to all of the given target articles. 
	 * The actual text of these sentences can be obtained using {@link Page#getSentenceMarkup(int)}
	 * 
	 * @param arts the articles of interest. 
	 * @return an array of sentence indexes that contain links to the given article.
	 */
	/*public Integer[] getSentenceIndexesMentioning(ArrayList<Article> arts) {
		TreeMap<Integer, Integer> sentenceCounts = new TreeMap<Integer, Integer>();

		//associate sentence indexes with number of arts mentioned.
		for (Article art:arts) {

			//System.out.println(" - Checking art " + art);

			for (Integer sentenceIndex: getSentenceIndexesMentioning(art)) {

				//System.out.println(" - - Adding sentence " + sentenceIndex);

				Integer count = sentenceCounts.get(sentenceIndex);
				if (count == null)
					sentenceCounts.put(sentenceIndex, 1);
				else
					sentenceCounts.put(sentenceIndex, count + 1);	
			}
		}

		//gather all sentences that mention all arts
		ArrayList<Integer> validSentences = new ArrayList<Integer>();
		Iterator<Map.Entry<Integer, Integer>> iter = sentenceCounts.entrySet().iterator();

		while (iter.hasNext()) {
			Map.Entry<Integer, Integer> e = iter.next();

			//System.out.println(" - " + e.getKey() + ", " + e.getValue());

			if (e.getValue() == arts.size())
				validSentences.add(e.getKey());
		}

		return validSentences.toArray(new Integer[validSentences.size()]);
	}*/


	/**
	 * A label that has been used to refer to the enclosing {@link Article}. These are mined from the title of the article, the 
	 * titles of {@link Redirect redirects} that point to the article, and the anchors of links that point to the article.   
	 */
	/*public class Label {

		private final String text;

		private final long linkDocCount;
		private final long linkOccCount;

		private final boolean fromTitle;
		private final boolean fromRedirect;
		private final boolean isPrimary;

		protected Label(DbLabelForPage l) {

			this.text = l.getText();
			this.linkDocCount = l.getLinkDocCount();
			this.linkOccCount = l.getLinkOccCount();
			this.fromTitle = l.getFromTitle();
			this.fromRedirect = l.getFromRedirect();
			this.isPrimary = l.getIsPrimary();
		}

		// @return the text of this label (the title of the article or redirect, or the anchor of the link
		public String getText() {
			return text;
		}

		// @return the number of pages that contain links that associate this label with the enclosing {@link Article}.
		public long getLinkDocCount() {
			return linkDocCount;
		}

		// @return the number of times this label occurs as the anchor text in links that refer to the enclosing {@link Article}.
		public long getLinkOccCount() {
			return linkOccCount;
		}

		// @return true if this label matches the title of the enclosing {@link Article}, otherwise false.
		public boolean isFromTitle() {
			return fromTitle;
		}

		// @return true if there is a {@link Redirect} that associates this label with the enclosing {@link Article}, otherwise false.
		public boolean isFromRedirect() {
			return fromRedirect;
		}

		// @return true if the enclosing {@link Article} is the primary, most common sense for the given label, otherwise false.
		public boolean isPrimary() {
			return isPrimary;
		}
	}*/
}
