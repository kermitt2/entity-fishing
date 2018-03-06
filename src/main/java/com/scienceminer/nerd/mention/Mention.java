package com.scienceminer.nerd.mention;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.lexicon.NERLexicon;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.data.Entity;
import org.grobid.core.data.Entity.Origin;
import org.grobid.core.data.Sense;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.utilities.StringPos;
import com.scienceminer.nerd.utilities.Utilities;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.model.Label;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Common representation of an unresolved mention in a textual document.
 * 
 * @author Patrice Lopez
 *
 */
public class Mention implements Comparable<Mention> {   

    protected String rawName = null;

    protected String normalisedName = null;

    private ProcessText.MentionMethod source = null;

    // relative offset positions in context, if defined
    protected OffsetPosition offsets = null;

    // optional bounding box in the source document
    protected List<BoundingBox> boundingBoxes = null;

    // optional layout tokens corresponding to the current mention
    private List<LayoutToken> layoutTokens = null;

    // if the mention is an acronym; if true, the normalisedName will give the found expended form
    private boolean isAcronym = false;

    private Entity entity = null;

    public Mention() {
        this.offsets = new OffsetPosition();
    }

    public Mention(String raw) {
        this();
        this.rawName = raw;
    }

    public Mention(String rawName, int start, int end) {
        this(rawName);
        this.setOffsetStart(start);
        this.setOffsetEnd(end);
    }

    public Mention(String rawText, ProcessText.MentionMethod source) {
        this(rawText);
        this.source = source;
    }

	public Mention(Entity ent) {
		rawName = ent.getRawName();
		normalisedName = ent.getNormalisedName();
		offsets = ent.getOffsets();
		boundingBoxes = ent.getBoundingBoxes();
		isAcronym = ent.getIsAcronym();
		entity = ent;
		layoutTokens = ent.getLayoutTokens();
		//startTokenPos = ent.startTokenPos;
		//endTokenPos = ent.startTokenPos;
	}

	public Mention(Mention ent) {
		rawName = ent.rawName;
		normalisedName = ent.normalisedName;
		offsets = ent.offsets;
		boundingBoxes = ent.boundingBoxes;
		isAcronym = ent.isAcronym;
		entity = ent.entity;
		source = ent.source;
		layoutTokens = ent.layoutTokens;
		//startTokenPos = ent.startTokenPos;
		//endTokenPos = ent.startTokenPos;
	}

    public String getRawName() {
        return rawName;
    }
	
	public void setRawName(String raw) {
        this.rawName = raw;
    }

	public String getNormalisedName() {
        return normalisedName;
    }
	
	public void setNormalisedName(String raw) {
        this.normalisedName = raw;
    }

    public ProcessText.MentionMethod getSource() {
        return this.source;
    }

    public void setSource(ProcessText.MentionMethod source) {
        this.source = source;
    }

    public OffsetPosition getOffsets() {
        return offsets;
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

    public double getProb() {
        if (entity != null)
            return entity.getProb();
        else
            return 0.0;
    }

    public void setProb(double prob) {
        if (entity == null)
            entity = new Entity();
        entity.setProb(prob);
    }

    public double getConf() {
        if (entity != null)
            return entity.getConf();
        else
            return 0.0;
    }

    public void setConf(double conf) {
        if (entity == null)
            entity = new Entity();
        entity.setConf(conf);
    }

    public Sense getSense() {
        if (entity != null)
            return entity.getSense();
        else
            return null;
    }

    public void setSense(Sense sense) {
        if (entity == null)
            entity = new Entity();
        entity.setSense(sense);
    }

    public Origin getOrigin() {
        if (entity != null)
            return entity.getOrigin();
        else
            return null;
    }

    public void setOrigin(Origin origin) {
        if (entity == null)
            entity = new Entity();
        entity.setOrigin(origin);
    }

    public NERLexicon.NER_Type getType() {
        if (entity != null)
            return entity.getType();
        else
            return null;
    }

    public void setType(NERLexicon.NER_Type theType) {
        if (entity == null)
            entity = new Entity();
        entity.setType(theType);
    }

    public List<String> getSubTypes() {
        if (entity != null)
            return entity.getSubTypes();
        else
            return null;
    }

    public void setSubTypes(List<String> theSubTypes) {
        if (entity == null)
            entity = new Entity();
        entity.setSubTypes(theSubTypes);
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }

    public List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void addBoundingBoxes(BoundingBox boundingBox) {
        if (this.boundingBoxes == null)
            this.boundingBoxes = new ArrayList<BoundingBox>();
        this.boundingBoxes.add(boundingBox);
    }

    public boolean getIsAcronym() {
        return this.isAcronym;
    }

    public void setIsAcronym(boolean acronym) {
        this.isAcronym = acronym;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public boolean equals(Object object) {
        boolean result = false;
        if ((object != null) && object instanceof Mention) {
            int start = ((Mention) object).getOffsetStart();
            int end = ((Mention) object).getOffsetEnd();
            if ((start != -1) && (end != -1)) {
                if ((start == offsets.start) && (end == offsets.end) && (source == ((Mention) object).getSource())) {
                    result = true;
                }
            } /*else {
				int startToken = ((Entity)object).getStartTokenPos();
				int endToken = ((Entity)object).getEndTokenPos();
				if ( (startToken != -1) && (endToken != -1) ) {
					if ( (startToken == startTokenPos) && (endToken == endTokenPos) ) {
						result = true;
					}
				}
			}*/
        }
        return result;
    }

    @Override
    public int compareTo(Mention theEntity) {
        int start = theEntity.getOffsetStart();
        int end = theEntity.getOffsetEnd();

        //if ((start != -1) && (end != -1)) {
        if (offsets.start != start)
            return offsets.start - start;
        else if (offsets.end != end)
            return offsets.end - end;
        else {
            return source.compareTo(theEntity.getSource());
        }
		/*} else {
			int startToken = theEntity.getStartTokenPos();
			int endToken =theEntity.getEndTokenPos();
			if ( (startToken != -1) && (endToken != -1) ) {
				if (startToken != startTokenPos) 
					return startTokenPos - startToken;
				else 
					return endTokenPos - endToken;
			} else {
				// it's too underspecified to be comparable, and for 
				// sure it's not equal
				// throw an exception ?
				return -1;
			}
		}*/
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (rawName != null) {
            buffer.append(rawName + "\t");
        }
        if (normalisedName != null) {
            buffer.append(normalisedName + "\t");
        }
        if (source != null) {
            buffer.append(source + "\t");
        }
        if (getType() != null) {
            buffer.append(getType() + "\t");
        }
        if (getSubTypes() != null) {
            for (String subType : getSubTypes())
                buffer.append(subType + "\t");
        }
        if (offsets != null) {
            buffer.append(offsets.toString() + "\t");
        }
        if (getSense() != null) {
            if (getSense().getFineSense() != null) {
                buffer.append(getSense().getFineSense() + "\t");
            }

            if (getSense().getCoarseSense() != null) {
                if ((getSense().getFineSense() == null) ||
                        ((getSense().getFineSense() != null) && !getSense().getCoarseSense().equals(getSense().getFineSense()))) {
                    buffer.append(getSense().getCoarseSense() + "\t");
                }
            }
        }

        return buffer.toString();
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }
}