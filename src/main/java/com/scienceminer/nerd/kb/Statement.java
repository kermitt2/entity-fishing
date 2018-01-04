package com.scienceminer.nerd.kb;

import java.io.Serializable;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.*;

import com.fasterxml.jackson.core.io.*;

/**
 * Class for representing and exchanging a semantic statement about an entity.
 */
public class Statement implements Serializable { 

    private String propertyId = null;
    private String conceptId = null;
    private String value = null;

    public Statement() {}

    public Statement(String wikidataId, String propertyId, String value) {
        this.conceptId = wikidataId;
        this.propertyId = propertyId;
        this.value = value;
    }

    public String getConceptId() {
        return this.conceptId;
    }

    public void setConceptId(String id) {
        this.conceptId = id;
    }

    public String getPropertyId() {
        return this.propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }
 
    public Property getProperty() {
        if (propertyId == null)
            return null;
        else
           return UpperKnowledgeBase.getInstance().getProperty(propertyId);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(conceptId).append("\t").append(" -> ")
            .append(propertyId)
            .append(" -> ").append(value);
        return sb.toString();
    }

    /**
     * Simple json serialization, keeping identifiers
     */
    public String toJsonOld() {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder sb = new StringBuilder();

        sb.append("{ \"conceptId\" : \"" + conceptId + "\"");

        if (propertyId != null) {
            sb.append(", \"propertyId\" : \"" + propertyId + "\"");

            Property property = UpperKnowledgeBase.getInstance().getProperty(propertyId);
            if (property != null) {
                sb.append(", \"propertyName\" : \"" + property.getName() + "\""); 
                if (property.getValueType() != null)
                    sb.append(", \"valueType\" : \"" + property.getValueType().getName() + "\"");   
            }
        }

        if (value != null) {
            byte[] encodedValue = encoder.quoteAsUTF8(value);
            String outputValue = new String(encodedValue); 
            if (value.startsWith("Q"))
                sb.append(", \"value\" : \"" + value + "\"");
            else
                sb.append(", \"value\" : " + value);   
        }

        sb.append("}");   

        return sb.toString();
    }

    /**
     * Json serialization replacing identifier by literal names
     */
    public String toJson() {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder sb = new StringBuilder();

        sb.append("{ \"conceptId\" : \"" + conceptId + "\"");

        if (propertyId != null) {
            sb.append(", \"propertyId\" : \"" + propertyId + "\"");
            Property property = UpperKnowledgeBase.getInstance().getProperty(propertyId);
            if (property != null) {
                sb.append(", \"propertyName\" : \"" + property.getName() + "\""); 
                if (property.getValueType() != null)
                    sb.append(", \"valueType\" : \"" + property.getValueType().getName() + "\"");   
            }
        }

        if (value != null) {
            byte[] encodedValue = encoder.quoteAsUTF8(value);
            String outputValue = new String(encodedValue); 
            boolean done = false;
            if (value.startsWith("Q") || value.startsWith("\"Q")) {
                Concept concept = null;
                if (value.startsWith("Q")) {
                    sb.append(", \"value\" : \"" + value + "\"");
                    concept = UpperKnowledgeBase.getInstance().getConcept(value);                    
                } else {
                    sb.append(", \"value\" : " + value);
                    concept = UpperKnowledgeBase.getInstance().getConcept(value.replace("\"", ""));
                }

                if (concept != null) {
                    Integer pageId = concept.getPageIdByLang("en");
                    if (pageId != null) {
                        LowerKnowledgeBase wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf("en");
                        Page page = wikipedia.getPageById(pageId);
                        if (page != null) {
                            byte[] encodedValueTitle = encoder.quoteAsUTF8(page.getTitle());
                            String outputValueTitle = new String(encodedValueTitle); 
                            sb.append(", \"valueName\" : \"" + outputValueTitle + "\"");
                            done = true;
                        }
                    }
                }
                /*if (!done)
                    sb.append(", \"value\" : \"" + value + "\"");*/  
            } else
                sb.append(", \"value\" : " + value);   
        }

        sb.append("}");   

        return sb.toString();
    }    
}