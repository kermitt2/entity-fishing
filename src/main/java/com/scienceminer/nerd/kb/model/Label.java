package com.scienceminer.nerd.kb.model;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.kb.model.hadoop.DbLabel;
import com.scienceminer.nerd.kb.model.hadoop.DbSenseForLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A term (anchor, title or redirection) used to refer to articles in Wikipedia. 
 * 
 * -> to be replaced
 */
public class Label {
	private static final Logger LOGGER = LoggerFactory.getLogger(Label.class);	
	
	private final String text;
	private long linkDocCount = 0;
	private long linkOccCount = 0;
	private long textDocCount = 0;
	private long textOccCount = 0;
	private Sense[] senses = null;
	protected KBLowerEnvironment env = null;
	private boolean detailsSet = false;

	public Label(KBLowerEnvironment env, String text) {
		this.env = env;
		this.text = text;
		this.detailsSet = false;
	}
	
	@Override
	public String toString() {
		return "\"" + text + "\""; 
	}
	
	public String getText() {
		return text;
	}
	
	/**
	 * @return true if this label has ever been used to refer to an article, otherwise false
	 */
	public boolean exists() {
		if (!detailsSet) 
			setDetails();
		return (senses.length > 0);	
	}

	/**
	 * @return the number of articles that contain links with this label used as an anchor
	 */
	public long getLinkDocCount() {
		if (!detailsSet) 
			setDetails();
		return linkDocCount;
	}

	/**
	 * @return the number of links that use this label as an anchor
	 */
	public long getLinkOccCount() {
		if (!detailsSet) 
			setDetails();
		return linkOccCount;
	}

	/**
	 * @return the number of articles that mention this label (either as links or in plain text) 
	 */
	public long getDocCount() {
		if (!detailsSet) 
			setDetails();
		return textDocCount;
	}

	/**
	 * @return the number of times this label is mentioned in articles (either as links or in plain text) 
	 */
	public long getOccCount() {
		if (!detailsSet) 
			setDetails();
		return textOccCount;
	}
	
	/**
	 * @return the probability that this label is used as a link in Wikipedia ({@link #getLinkDocCount()}/{@link #getDocCount()}
	 */
	public double getLinkProbability() {
		if (!detailsSet) 
			setDetails();
		
		if (textDocCount == 0)
			return 0;
		
		double linkProb = (double) linkDocCount/textDocCount;
		if (linkProb >1)
			linkProb = 1;
			
		return linkProb;
	}

	/**
	 * @return	an array of {@link Sense Senses}, sorted by {@link Sense#getPriorProbability()}, that this label refers to
	 */
	public Sense[] getSenses() {
		if (!detailsSet) 
			setDetails();	
		return senses;
	}	
	
	/**
	 * A possible sense for a label
	 */
	public class Sense extends Article {
		private final long sLinkDocCount;
		private final long sLinkOccCount;

		private final boolean fromTitle;
		private final boolean fromRedirect;
		
		protected Sense(KBLowerEnvironment env,  DbSenseForLabel s) {
			super(env, s.getId());

			this.sLinkDocCount = s.getLinkDocCount();
			this.sLinkOccCount = s.getLinkOccCount();
			this.fromTitle = s.getFromTitle();
			this.fromRedirect = s.getFromRedirect();
		}

		/**
		 * Returns the number of documents that contain links that use the surrounding label as anchor text, and point to this sense as the destination.
		 * 
		 */
		public long getLinkDocCount() {
			return sLinkDocCount;
		}


		/**
		 * Returns the number of links that use the surrounding label as anchor text, and point to this sense as the destination.
		 * 
		 */
		public long getLinkOccCount() {
			return sLinkOccCount;
		}


		/**
		 * Returns true if the surrounding label is used as a title for this sense article, otherwise false
		 * 
		 */
		public boolean isFromTitle() {
			return fromTitle;
		}

		/**
		 * Returns true if the surrounding label is used as a redirect for this sense article, otherwise false
		 * 
		 */
		public boolean isFromRedirect() {
			return fromRedirect;
		}
		
		
		/**
		 * Returns the probability that the surrounding label goes to this destination 
		 * 
		 */
		public double getPriorProbability() {
			if (linkOccCount == 0)
				return 0.0;
			else {
				return ((double)sLinkOccCount) / linkOccCount;
			}
		}
		
		/**
		 * Returns true if this is the most likely sense for the surrounding label, otherwise false
		 * 
		 */
		public boolean isPrimary() {
			return (this == senses[0]);
		}
	}

	private void setDetails() {	
		try {
			DbLabel lbl = env.getDbLabel().retrieve(text);
			if (lbl == null) {
				throw new Exception();
			} else {
				setDetails(lbl);
			}
		} catch (Exception e) {
			this.senses = new Sense[0];
			detailsSet = true;
		}
	}
	
	private void setDetails(DbLabel lbl) {
		this.linkDocCount = lbl.getLinkDocCount();
		this.linkOccCount = lbl.getLinkOccCount();
		this.textDocCount = lbl.getTextDocCount();
		this.textOccCount = lbl.getTextOccCount();		
		Map<Integer, DbSenseForLabel> sensesCatalogue = new HashMap<Integer, DbSenseForLabel>();

		int i = 0;
		for (DbSenseForLabel dbs : lbl.getSenses()) {
			Page page = Page.createPage(env, dbs.getId());
			PageType pageType = page.getType();

			// solve possible redirect
			if (pageType == PageType.redirect) {
				Article article = ((Redirect)page).getTarget();
				if (article != null) {
					dbs.setId(article.getId());
					dbs.setFromRedirect(true);
					dbs.setFromTitle(false);
				} else {
					//LOGGER.warn("Page " + page.getId() + " is of type redirect but its target is null, it will be ignored");
					// this is cases like this one: 
					// https://en.wikipedia.org/w/api.php?action=query&prop=info&pageids=1487195&inprop=url
 					// quite frequent, redirect is empty, still we get redirected to category pages
					continue;
				} 
			} else {
				// no redirect
				dbs.setFromTitle(true);
				dbs.setFromRedirect(false);
			}

			DbSenseForLabel sfl = sensesCatalogue.get(dbs.getId());
			if (sfl == null) {
				sensesCatalogue.put(dbs.getId(), dbs);
			} else {
				// merging counts because a redirect directs to an already 
				// existing article in the sense list
				dbs.setLinkDocCount(dbs.getLinkDocCount() + sfl.getLinkDocCount());
				dbs.setLinkOccCount(dbs.getLinkOccCount() + sfl.getLinkOccCount());
				dbs.setFromTitle(true);
				dbs.setFromRedirect(false);
				sensesCatalogue.put(dbs.getId(), dbs);
			}
		}

		// create final sense list 
		this.senses = new Sense[sensesCatalogue.size()];
		Iterator it = sensesCatalogue.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			this.senses[i] = new Sense(env, (DbSenseForLabel)pair.getValue());
			i++;
		}

		// as the count have been changed, the sense list requires an additional sort
		if (this.senses.length > 0) {
	 		Arrays.sort(this.senses, new Comparator<Label.Sense>() {
			    public int compare(Label.Sense idx1, Label.Sense idx2) {
			        return Double.compare(idx2.getPriorProbability(), idx1.getPriorProbability());
		    	}
			});
	 	}
		this.detailsSet = true;
	}
	
	public static Label createLabel(KBLowerEnvironment env, String text, DbLabel dbLabel) {
		Label label = new Label(env, text);
		label.setDetails(dbLabel);
		
		return label;
	}
	
}
