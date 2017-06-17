package com.scienceminer.nerd.kb;

import java.io.Serializable;
import java.util.List;

import com.scienceminer.nerd.kb.db.*;

import com.fasterxml.jackson.core.io.*;

/**
 * Class for representing and exchanging a property.
 */
public class Property implements Serializable { 

    // Value type of the property
    // see https://www.wikidata.org/wiki/Special:ListDatatypes
    public enum ValueType {
        STRING      ("string"),
        TEXT        ("monolingualtext"),
        ENTITY      ("wikibase-item"),
        GLOBE_COORDINATE      ("globe-coordinate"),
        GEO_SHAPE   ("geo-shape"),
        QUANTITY    ("quantity"),
        TIME        ("time"),
        WIKIMEDIA   ("commonsMedia"),
        TABULAR_DATA   ("tabular-data"),
        EXTERNAL_ID    ("external-id"),
        PROPERTY    ("property"),
        MATH        ("math"),
        URL         ("url"),
        UNKOWN      ("unknown");

        private String name;

        private ValueType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static ValueType fromString(String text) {
            for (ValueType b : ValueType.values()) {
                if (b.name.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    };

    private String id = null; // e.g. P37
    private String name = null; 
    private ValueType valueType = null;

    public Property() {}

    public Property(String id, String name, ValueType valueType) {
        this.id = id;
        this.name = name;
        this.valueType = valueType;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ValueType getValueType() {
        return this.valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    /**
     * Return the list of statements associated to the property
     */
    public List<Statement> getStatements(KBUpperEnvironment env) {
        return env.getDbStatements().retrieve(id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append("\t").append(" -> ").append(name).append(" -> ").append(valueType).append("\n");
        return sb.toString();
    }

    public String toJson() {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder sb = new StringBuilder();

        sb.append("{ \"id\" : \"" + id + "\"");

        byte[] encodedName = encoder.quoteAsUTF8(name);
        String outputName = new String(encodedName); 
        sb.append(", \"name\" : \"" + outputName + "\"");

        byte[] encodedValueType = encoder.quoteAsUTF8(valueType.getName());
        String outputValueType = new String(encodedValueType); 
        sb.append(", \"valueType\" : \"" + outputValueType + "\" }");

        return sb.toString();
    }
}