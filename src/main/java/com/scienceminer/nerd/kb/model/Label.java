package com.scienceminer.nerd.kb.model;

import com.scienceminer.nerd.kb.db.KBEnvironment;

import org.wikipedia.miner.db.struct.DbLabel;
import org.wikipedia.miner.db.struct.DbSenseForLabel;

/**
 * A term or phrase that has been used to refer to one or more {@link Article Articles} in Wikipedia. 
 * 
 * These provide your best way of searching for articles relating to or describing a particular term.
 */
public class Label {
	
	private final String text;
	//private final TextProcessor textProcessor;

	private long linkDocCount = 0;
	private long linkOccCount = 0;
	private long textDocCount = 0;
	private long textOccCount = 0;
	
	private Sense[] senses = null;
	
	protected KBEnvironment env;
	private boolean detailsSet;

	/**
	 * Initialises a Label 
	 * 
	 * @param env an active KBEnvironment
	 * @param text the term or phrase of interest
	 */
	public Label(KBEnvironment env, String text) {
		this.env = env;
		this.text = text;
		this.detailsSet = false;
	}
	
	@Override
	public String toString() {
		return "\"" + text + "\""; 
	}
	
	/**
	 * @return the text used to refer to concepts. 
	 */
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
	 * @return the number of articles that contain links with this label used as an anchor.  
	 */
	public long getLinkDocCount() {
		if (!detailsSet) 
			setDetails();
		return linkDocCount;
	}

	/**
	 * @return the number of links that use this label as an anchor.  
	 */
	public long getLinkOccCount() {
		if (!detailsSet) 
			setDetails();
		return linkOccCount;
	}

	/**
	 * @return the number of articles that mention this label (either as links or in plain text).  
	 */
	public long getDocCount() {
		if (!detailsSet) 
			setDetails();
		return textDocCount;
	}

	/**
	 * @return the number of times this label is mentioned in articles (either as links or in plain text).  
	 */
	public long getOccCount() {
		if (!detailsSet) 
			setDetails();
		return textOccCount;
	}
	
	/**
	 * @return the probability that this label is used as a link in Wikipedia ({@link #getLinkDocCount()}/{@link #getDocCount()}.  
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
	 * @return	an array of {@link Sense Senses}, sorted by {@link Sense#getPriorProbability()}, that this label refers to.
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
		
		protected Sense(KBEnvironment env,  DbSenseForLabel s) {
			super(env, s.getId());

			this.sLinkDocCount = s.getLinkDocCount();
			this.sLinkOccCount = s.getLinkOccCount();
			this.fromTitle = s.getFromTitle();
			this.fromRedirect = s.getFromRedirect();
		}

		/**
		 * Returns the number of documents that contain links that use the surrounding label as anchor text, and point to this sense as the destination.
		 * 
		 * @return the number of documents that contain links that use the surrounding label as anchor text, and point to this sense as the destination.  
		 */
		public long getLinkDocCount() {
			return sLinkDocCount;
		}


		/**
		 * Returns the number of links that use the surrounding label as anchor text, and point to this sense as the destination.
		 * 
		 * @return the number of links that use the surrounding label as anchor text, and point to this sense as the destination.
		 */
		public long getLinkOccCount() {
			return sLinkOccCount;
		}


		/**
		 * Returns true if the surrounding label is used as a title for this sense article, otherwise false
		 * 
		 * @return true if the surrounding label is used as a title for this sense article, otherwise false
		 */
		public boolean isFromTitle() {
			return fromTitle;
		}

		/**
		 * Returns true if the surrounding label is used as a redirect for this sense article, otherwise false
		 * 
		 * @return true if the surrounding label is used as a redirect for this sense article, otherwise false
		 */
		public boolean isFromRedirect() {
			return fromRedirect;
		}
		
		
		/**
		 * Returns the probability that the surrounding label goes to this destination 
		 * 
		 * @return the probability that the surrounding label goes to this destination 
		 */
		public double getPriorProbability() {

			if (getSenses().length == 1)
				return 1;

			if (linkOccCount == 0)
				return 0;
			else 			
				return ((double)sLinkOccCount) / linkOccCount;
		}
		
		/**
		 * Returns true if this is the most likely sense for the surrounding label, otherwise false
		 * 
		 * @return true if this is the most likely sense for the surrounding label, otherwise false
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
		
		this.senses = new Sense[lbl.getSenses().size()];
		
		int i = 0;
		for (DbSenseForLabel dbs:lbl.getSenses()) {
			this.senses[i] = new Sense(env, dbs);
			i++;
		}
		
		this.detailsSet = true;
	}
	
	public static Label createLabel(KBEnvironment env, String text, DbLabel dbLabel) {
		Label l = new Label(env, text);
		l.setDetails(dbLabel);
		
		return l;
	}
	
}
