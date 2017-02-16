package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.data.Entity;
import org.grobid.core.data.Sense;
import com.scienceminer.nerd.kb.*;

import java.util.List;    
import java.util.ArrayList;

import org.wikipedia.miner.model.*;

import com.fasterxml.jackson.core.io.*;

/**
 * This class represents a sentence with stand-off position to mark its boundaries in a text. 
 * 
 * @author Patrice Lopez
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
	
}