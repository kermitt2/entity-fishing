package com.scienceminer.nerd.kb;

import java.io.Serializable;

/**
 * Class for representing and exchanging a semantic relation.
 */
public class Relation extends org.apache.hadoop.record.Record implements Serializable { 

    // Orign of the entity definition
    public enum RelationType {
        RELATED   ("related"),
        PART_OF   ("part_of"),
        IS_A      ("is_a"),
        HYPERNYM  ("hypernym"),
        HYPONYM   ("hyponym"),
        CUSTOM    ("custom");

        private String name;

        private RelationType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    };

    private RelationType type = RelationType.RELATED; // default
    private String relationName = null;
    private String templateName = null;
    private Integer concept1ID = -1;
    private Integer concept2ID = -1;

    public Relation() {}

    public Relation(RelationType relationType, String relationName, Integer concept1ID, Integer concept2ID, String templateName) {
        this.type = relationType;
        this.concept1ID = concept1ID;
        this.concept2ID = concept2ID;
        this.relationName = relationName;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public RelationType getRelationType() {
        return type;
    }

    public void setRelationType(RelationType relationType) {
        this.type = relationType;
    }

    public Integer getConcept1ID() {
        return concept1ID;
    }

    public void setConcept1ID(Integer concept1ID) {
        this.concept1ID = concept1ID;
    }

    public Integer getConcept2ID() {
        return concept2ID;
    }

    public void setConcept2ID(Integer concept2ID) {
        this.concept2ID = concept2ID;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(concept1ID).append("\t").append(" -> ").append(type.getName()).append(" -> ").append(concept2ID).append("\n");

        return sb.toString();
    }

    @Override
    public int compareTo(Object theRel) {
        Relation theRelation = (Relation)theRel;
        if ( (concept1ID == theRelation.getConcept1ID()) &&
             (concept2ID == theRelation.getConcept2ID()) &&
             (type == theRelation.getRelationType()) ) {
            if (type == RelationType.CUSTOM) {
                if (relationName.equals(theRelation.getRelationName())) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                return 0;
            }
        } else {
            return -1;
        }
    }

    @Override
    public void deserialize(final org.apache.hadoop.record.RecordInput _rio_a, final String _rio_tag) throws java.io.IOException {
        throw new UnsupportedOperationException();
    }

    @Override
     public void serialize(final org.apache.hadoop.record.RecordOutput _rio_a, final String _rio_tag)
        throws java.io.IOException {
        throw new UnsupportedOperationException();
    }
}