package com.scienceminer.nerd.kb;

import org.codehaus.jackson.io.JsonStringEncoder;

import java.io.Serializable;

/**
 * Class for getting information about wikidata Ids and their predicted classes
 */

public class NerdKid implements Serializable {
    public String getWikidataId() {
        return wikidataId;
    }

    public void setWikidataId(String wikidataId) {
        this.wikidataId = wikidataId;
    }


    private String wikidataId = null;

    public String getPredictedClass() {
        return predictedClass;
    }

    public void setPredictedClass(String predictedClass) {
        this.predictedClass = predictedClass;
    }

    private String predictedClass = null;

    public NerdKid() {}

    public NerdKid(String wikidataId, String claz){
        this.wikidataId = wikidataId;
        this.predictedClass = claz;
    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(wikidataId).append("\t").append(" -> ").append(predictedClass);
        return stringBuilder.toString();
    }

    // serialization
    public String toJson(){
        JsonStringEncoder jsonStringEncoder = JsonStringEncoder.getInstance();
        StringBuilder stringBuilder = new StringBuilder();

        // add wikidata Ids
        if (wikidataId.startsWith("Q")){
            stringBuilder.append("{ \"wikidataId\" : \"" + jsonStringEncoder.encodeAsUTF8(wikidataId) + "\"");
        }

        // add predicted class by Nerd-Kid
        if (predictedClass != null){
            stringBuilder.append(", \"predictedClass\" : \"" + jsonStringEncoder.encodeAsUTF8(predictedClass) + "\"");
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
