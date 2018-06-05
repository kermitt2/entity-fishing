package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.kb.db.KBUpperEnvironment;
import org.codehaus.jackson.io.JsonStringEncoder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Class for getting information about wikidata Ids and their predicted classes
 */

public class NerdKid implements Serializable {

    private KBUpperEnvironment env = null;
    private String wikidataId = null;

    public String getWikidataId() {
        return wikidataId;
    }

    public void setWikidataId(String wikidataId) {
        this.wikidataId = wikidataId;
    }

    public String getPredictedClass() {
        return predictedClass;
    }

    public void setPredictedClass(String predictedClass) {
        this.predictedClass = predictedClass;
    }

    private String predictedClass = null;

    public NerdKid() {}

    public NerdKid(KBUpperEnvironment env, String wikidataId) {
        this.env = env;
        this.wikidataId = wikidataId;
    }

    public String getPredictedClassById(String wikidataId) {
        String result = env.getDbNerdKid().retrieve(wikidataId);
        return result;
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
