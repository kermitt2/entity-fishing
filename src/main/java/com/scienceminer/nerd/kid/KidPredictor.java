package com.scienceminer.nerd.kid;

import org.nerd.kid.data.WikidataElementInfos;
import org.nerd.kid.extractor.wikidata.WikidataFetcherWrapper;
import org.nerd.kid.model.WikidataNERPredictor;

public class KidPredictor {
    // get the input of Wikidata Id and return the prediction result
    public WikidataElementInfos predict(String wikidataId) {
        WikidataElementInfos wikidataElementInfos = new WikidataElementInfos();
        try {
            // prediction by Nerd-Kid
            WikidataFetcherWrapper wrapper = new NerdAPIJavaFetcherWrapper();
            WikidataNERPredictor wikidataNERPredictor = new WikidataNERPredictor(wrapper);
            String predictionResult = wikidataNERPredictor.predict(wikidataId).getPredictedClass();

            // put the result in an object WikidataElementInfos
            wikidataElementInfos.setWikidataId(wikidataId);
            wikidataElementInfos.setPredictedClass(predictionResult);

        }catch (Exception e){
            e.printStackTrace();
        }
        return wikidataElementInfos;
    }
}
