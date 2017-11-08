package com.scienceminer.nerd.mention;

import com.scienceminer.nerd.exceptions.NerdException;

import org.grobid.core.utilities.OffsetPosition;

import com.fasterxml.jackson.core.io.*;

import java.util.List;

/**
 * This class represents a sentence with stand-off position to mark its boundaries in a text. 
 * 
 *
 */
public class Sentence {
	
	// relative offset positions in context
	private OffsetPosition offsets = null;
	
	public Sentence() {
		this.offsets = new OffsetPosition();
    }
	
	public OffsetPosition getOffsets() {
		return this.offsets;
	}
	
	public void setOffsets(OffsetPosition offsets) {
		this.offsets = offsets;
	}
	
	public void setOffsetStart(int start) {
        offsets.start = start;
    }

    public int getOffsetStart() {
        return offsets.start;
    }

    public void setOffsetEnd(int end) {
        offsets.end = end;
    }

    public int getOffsetEnd() {
        return offsets.end;
    }
	
	public String toJSON() {
		return "{ \"offsetStart\" : " + offsets.start + ", \"offsetEnd\" : " + offsets.end + " }";
	}

	/**
	 * Utility method to serialise to JSON a list of sentences 
	 */
	public static String listToJSON(List<Sentence> sentences) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("\"sentences\" : [ ");
		boolean start = true;
		for (Sentence sentence : sentences) {
			if (start) {
				buffer.append(sentence.toJSON());
				start = false;
			} else
				buffer.append(", " + sentence.toJSON());
		}
		buffer.append(" ]");

		return buffer.toString();
	}

}