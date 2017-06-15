package com.scienceminer.nerd.evaluation;

import com.scienceminer.nerd.kb.model.*;

import org.grobid.core.utilities.OffsetPosition;

/**
 * A term or phrase that is either unambiguous or has been disambiguated so that it refers to a particular wikipedia topic.
 * 
 */
public class TopicReference implements Comparable<TopicReference>{
	
	private Label label;
	private int topicId;
	private OffsetPosition position;
	
	/**
	 * Initializes a disambiguated topic reference.
	 * 
	 * @param label the label from which the reference was mined
	 * @param topicId the id of the topic it was disambiguated to
	 * @param position the location (start and end character indices) from which this reference was mined
	 */
	public TopicReference(Label label, int topicId, OffsetPosition position) {
		this.label = label;
		this.topicId = topicId;
		this.position = position;
	}
	
	/**
	 * Initializes a topic reference that may or may not be ambiguous
	 * 
	 * @param label the label from which the reference was mined
	 * @param position the location (start and end character indices) from which this reference was mined
	 * @throws SQLException if there is a problem with the Wikipedia database that the label was obtained from
	 */
	public TopicReference(Label label, OffsetPosition position) {
		this.label = label;
		this.position = position;
		
		Label.Sense[] senses = label.getSenses();
		
		if (senses.length == 1) {
			topicId = senses[0].getId();
		} else {
			topicId = 0;
		}
	}
		
	/**
	 * @return true if the reference has been not been disambiguated yet, otherwise false. 
	 */
	public boolean isAmbiguous() {
		return topicId > 0;
	}
	
	/**
	 * @param tr the topic reference to check for overlap
	 * @return true if this overlaps the given reference, otherwise false.
	 */
	public boolean overlaps(TopicReference tr) {
		return position.overlaps(tr.getPosition());
	}
	
	/**
	 * @return the label that reference was mined from
	 */
	public Label getLabel() {
		return label;
	}
	
	/**
	 * @return the id that this reference has been disambiguated to, or 0 if it hasnt been disambiguated yet.
	 */
	public Integer getTopicId() {
		return topicId;
	}

	/**
	 * @return the position (start and end character locations) in the document where this reference was found.
	 */
	public OffsetPosition getPosition() {
		return position;
	}
	
	public int getOffsetStart() {
		if (position == null)
			return -1;
		return position.start;
	}

	public int getOffsetEnd() {
		if (position == null)
			return -1;
		return position.end;
	}
	
	public int compareTo(TopicReference tr) {
		
		if (position != null) {
			//starts first, then goes first
			int c = new Integer(position.start).compareTo(tr.getPosition().start);
			if (c != 0) return c;
			
			//starts at same time, so longest one goes first
			c = new Integer(tr.getPosition().end).compareTo(position.end);
			if (c != 0) return c;
		}
		
		return new Integer(topicId).compareTo(new Integer(tr.getTopicId()));
	}
}
