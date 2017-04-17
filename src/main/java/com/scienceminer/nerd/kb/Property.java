package com.scienceminer.nerd.kb;

import java.io.Serializable;

import com.fasterxml.jackson.core.io.*;

/**
 * Class for representing and exchanging a property associated to a concept.
 */
//public class Property extends org.apache.hadoop.record.Record implements Serializable { 

public class Property implements Serializable { 

    private String attribute = null;
    private String value = null;
    private String templateName = null;
    private Integer conceptId = -1;
    private Integer valueConcept = null; // this is a concept associated to the value, 
                                         // for instance a unit in case of measurement
    public Property() {}

    public Property(Integer conceptId, String attribute, String value, String template, Integer valueConcept) {
        this.attribute = attribute;
        this.conceptId = conceptId;
        this.value = value;
        this.templateName = template;
        this.valueConcept = valueConcept;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Integer getConceptId() {
        return conceptId;
    }

    public void setConceptId(Integer conceptId) {
        this.conceptId = conceptId;
    }

    public Integer getValueConcept() {
        return valueConcept;
    }

    public void setValueConcept(Integer valueConcept) {
        this.valueConcept = valueConcept;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(conceptId).append("\t").append(" -> ").append(attribute).append(" -> ").append(value).append("\n");
        return sb.toString();
    }

    /*@Override
    public int compareTo(Object theProp) {
        Property theProperty = (Property)theProp;
        if ( (conceptId == theProperty.getConceptId()) &&
             (attribute.equals(theProperty.getAttribute())) &&
             (value.equals(theProperty.getValue())) ) {
                return 0;
        } else {
            return -1;
        }
    }*/

    /*@Override
    public void deserialize(final org.apache.hadoop.record.RecordInput _rio_a, final String _rio_tag) throws java.io.IOException {
        throw new UnsupportedOperationException();
    }

    @Override
     public void serialize(final org.apache.hadoop.record.RecordOutput _rio_a, final String _rio_tag)
        throws java.io.IOException {
        throw new UnsupportedOperationException();
    }*/

    public String toJson() {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder sb = new StringBuilder();

        byte[] encodedAttribute = encoder.quoteAsUTF8(attribute);
        String outputAttribute = new String(encodedAttribute); 
        sb.append("{ \"attribute\" : \"" + outputAttribute + "\"");

        byte[] encodedValue = encoder.quoteAsUTF8(value);
        String outputValue = new String(encodedValue); 
        sb.append(", \"value\" : \"" + outputValue + "\"");

        if (templateName != null) {
            byte[] encodedTemplate = encoder.quoteAsUTF8(templateName);
            String outputTemplate = new String(encodedTemplate); 
            sb.append(", \"template\" : \"" + outputTemplate + "\" }");        
        }

        //Integer valueConcept

        return sb.toString();
    }
}