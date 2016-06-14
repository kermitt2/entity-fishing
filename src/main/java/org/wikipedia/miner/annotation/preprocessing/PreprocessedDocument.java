/*
 *    PreprocessedDocument.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


package org.wikipedia.miner.annotation.preprocessing;

import java.util.* ;

/**
 * This class stores a document that is ready to be processed by linkDetector, disambiguator, documentTagger, etc.
 * 
 * @author David Milne
 */
public class PreprocessedDocument {

	private final String originalText ;
	private final String preprocessedText ;
	private final String contextText ;
	private HashSet<Integer> bannedTopics ;
	private final ArrayList<RegionTag> regionTags ;

	//region tracking
	private List<HashSet<Integer>> doneIdsStack ;
	private HashSet<Integer> doneIds ;
	private int nextTagIndex ;

	/**
	 * Initializes a preprocessed document. You should not use this yourself, instead let the relevant documentPreprocessor create the document.
	 * 
	 * @param originalText the unmodified, original markup.
	 * @param preprocessedText the modified text, stripped of all markup.
	 * @param contextText any additional text that can help disambiguate terms or judge their importance (e.g. metadata)
	 * @param regionTags the region tags detected in the document.
	 * @param bannedTopics a set of ids for topics that you don't want to be detected in the document.
	 */
	public PreprocessedDocument(String originalText, String preprocessedText, String contextText, ArrayList<RegionTag> regionTags, HashSet<Integer>bannedTopics) {
		this.originalText = originalText ;
		this.preprocessedText = preprocessedText ;
		this.contextText = contextText ;
		this.bannedTopics = bannedTopics ;	
		this.regionTags = regionTags ;	
		
		if (this.bannedTopics == null)
			this.bannedTopics = new HashSet<Integer>() ;

		resetRegionTracking() ;
	}

	/**
	 * Resets the information that has been recorded about which regions have been seen (and topics seen within them) so far.
	 * 
	 * This should only be used by the document tagger. 
	 */
	public void resetRegionTracking() {
		doneIdsStack = new ArrayList<HashSet<Integer>>() ;
		doneIds = new HashSet<Integer>() ;
		nextTagIndex = 0 ;
	}

	/**
	 * @return the original markup of the document.
	 */
	public String getOriginalText(){
		return originalText ;		
	}

	/**
	 * @return the content of the document, stripped of all markup
	 */
	public String getPreprocessedText(){
		return preprocessedText ;		
	}

	/**
	 * @return any additional text (metadata, etc) that may be helpful for disambiguating terms or judging their importance
	 */
	public String getContextText() {
		return contextText ;
	}

	/**
	 * bans a topic so that it will not be detected in the document
	 * 
	 * @param topicId the id of the topic to be banned.
	 */
	public void banTopic(int topicId) {
		bannedTopics.add(topicId) ;
	}
	
	/**
	 * @return the set of all ids that have been banned from being detected in the document
	 */
	public HashSet<Integer> getBannedTopics() {
		return bannedTopics ;
	}
	
	/**
	 * @param topicId the id of the topic to check
	 * @return true if the given topic is banned, otherwise false
	 */
	public boolean isTopicBanned(int topicId) {
		return bannedTopics.contains(topicId) ;
	}
	
	/**
	 * @return the index of the region we are currently looking at (should only be used by documentTagger)
	 */
	public int getCurrRegionIndex() {
		return nextTagIndex ;
	}
	
	/**
	 * @param pos the character position of the document where we are currently looking.
	 * @return the set of ids for all topics that we have seen already in the region surrounding the given pos.
	 */
	public HashSet<Integer> getDoneIdsInCurrentRegion(int pos) {

		//System.out.println(" - currPos=" + pos + "nextIndex=" + nextTagIndex + ", maxIndex=" + regionTags.size()) ;

		if (nextTagIndex >= regionTags.size()) {
			// no more tags, so just return last set we looked at
			return doneIds ;
		}

		while (nextTagIndex < regionTags.size()) {

			RegionTag nextTag = regionTags.get(nextTagIndex) ;
			//System.out.println(" - nextTag=" + nextTag) ;

			if (nextTag.getPosition() < pos) {
				// we have passed this tag
				nextTagIndex ++ ;

				//System.out.println(" - passed " + nextTag) ;

				if (nextTag.getType() == RegionTag.REGION_SPLIT) 
					doneIds = new HashSet<Integer>() ;

				if (nextTag.getType() == RegionTag.REGION_CLOSE) {
					//pop previous doneIds
					if (doneIdsStack.isEmpty()) 
						doneIds = new HashSet<Integer>() ;	
					else {
						doneIds = doneIdsStack.get(doneIdsStack.size()-1);
						doneIdsStack.remove(doneIdsStack.size()-1) ;
					}
				}

				if (nextTag.getType() == RegionTag.REGION_OPEN) {
					//push new doneIds ;
					doneIdsStack.add(doneIds) ;
					doneIds = new HashSet<Integer>() ;					
				}
			} else {
				//this tag is the next we will encounter 
				break ;
			}
		}

		return doneIds ;
	}


	protected static class RegionTag implements Comparable<RegionTag> {

		/**
		 * specifies an opening tag of a region
		 */
		public static final int REGION_OPEN = 1 ;
		
		/**
		 * specifies a closing tag of a region 
		 */
		public static final int REGION_CLOSE = 2 ;
		
		/**
		 * specifies a tag that splits a region 
		 */
		public static final int REGION_SPLIT = 3 ;

		private final int pos ;
		private final int type ;

		/**
		 * Initializes a region tag with the given type and location
		 * 
		 * @param pos the character position where this tag starts
		 * @param type (REGION_OPEN, REGION_CLOSE or REGION_SPLIT)
		 */
		public RegionTag(int pos, int type) {
			this.pos = pos ;
			this.type = type ;
		}

		/**
		 * @return the character position where this tag starts
		 */
		public int getPosition() {
			return pos ;
		}

		/**
		 * @return the type of this tag (REGION_OPEN, REGION_CLOSE or REGION_SPLIT)
		 */
		public int getType() {
			return type ;
		}

                @Override
		public int compareTo(RegionTag rt) {
			return new Integer(pos).compareTo(rt.getPosition()) ;
		}

                @Override
		public String toString() {
			switch(type) {
			case REGION_OPEN: return "(" + pos + ",OPEN)" ;
			case REGION_CLOSE: return "(" + pos + ",CLOSE)" ;
			case REGION_SPLIT: return "(" + pos + ",SPLIT)" ;
			}

			return "(" + pos + ",UNKOWN)" ;
		}
	}

}

