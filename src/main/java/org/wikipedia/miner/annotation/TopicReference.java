/*
 *    TopicReference.java
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


package org.wikipedia.miner.annotation;

import org.wikipedia.miner.model.* ;
import org.wikipedia.miner.util.*;

import org.grobid.core.utilities.OffsetPosition;

/**
 * A term or phrase that is either unambiguous or has been disambiguated so that it refers to a particular wikipedia topic.
 * 
 *  @author David Milne
 */
public class TopicReference implements Comparable<TopicReference>{
	
	private Label label ;
	private int topicId ;
	private OffsetPosition position ;
	//private double disambigConfidence ;
	
	/**
	 * Initializes a disambiguated topic reference.
	 * 
	 * @param label the label from which the reference was mined
	 * @param topicId the id of the topic it was disambiguated to
	 * @param position the location (start and end character indices) from which this reference was mined
	 */
	public TopicReference(Label label, int topicId, OffsetPosition position) {
		this.label = label ;
		this.topicId = topicId ;
		this.position = position ;
		//this.disambigConfidence = disambigConfidence ;
	}
	
	/**
	 * Initializes a topic reference that may or may not be ambiguous
	 * 
	 * @param label the label from which the reference was mined
	 * @param position the location (start and end character indices) from which this reference was mined
	 * @throws SQLException if there is a problem with the Wikipedia database that the label was obtained from
	 */
	public TopicReference(Label label, OffsetPosition position) {
		this.label = label ;
		this.position = position ;
		
		Label.Sense[] senses = label.getSenses() ;
		
		if (senses.length == 1) {
			topicId = senses[0].getId() ;
			//disambigConfidence = 1 ;
		} else {
			topicId = 0 ;
			//disambigConfidence = 0 ;
		}
	}
		
	/**
	 * @return true if the reference has been not been disambiguated yet, otherwise false. 
	 */
	public boolean isAmbiguous() {
		return topicId > 0 ;
	}
	
	/**
	 * @param tr the topic reference to check for overlap
	 * @return true if this overlaps the given reference, otherwise false.
	 */
	public boolean overlaps(TopicReference tr) {
		return position.overlaps(tr.getPosition()) ;
	}
	
	/**
	 * @return the label that reference was mined from
	 */
	public Label getLabel() {
		return label ;
	}
	
	/**
	 * @return the id that this reference has been disambiguated to, or 0 if it hasnt been disambiguated yet.
	 */
	public Integer getTopicId() {
		return topicId ;
	}

	/**
	 * @return the position (start and end character locations) in the document where this reference was found.
	 */
	public OffsetPosition getPosition() {
		return position ;
	}
	
	//public double getDisambigConfidence() {
	//	return disambigConfidence ;
	//}
	
	
	public int compareTo(TopicReference tr) {
		
		if (position != null) {
			//starts first, then goes first
			int c = new Integer(position.start).compareTo(tr.getPosition().start) ;
			if (c != 0) return c ;
			
			//starts at same time, so longest one goes first
			c = new Integer(tr.getPosition().end).compareTo(position.end) ;
			if (c != 0) return c ;
		}
		
		return new Integer(topicId).compareTo(new Integer(tr.getTopicId())) ;
	}
}
