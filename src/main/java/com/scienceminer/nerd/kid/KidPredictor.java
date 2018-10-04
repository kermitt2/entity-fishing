package com.scienceminer.nerd.kid;

import org.nerd.kid.data.WikidataElementInfos;
import org.nerd.kid.extractor.wikidata.WikidataFetcherWrapper;
import org.nerd.kid.model.WikidataNERPredictor;

public class KidPredictor {

    private WikidataNERPredictor wikidataNERPredictor;

    public KidPredictor() {
        WikidataFetcherWrapper wrapper = new NerdAPIJavaFetcherWrapper();
        wikidataNERPredictor = new WikidataNERPredictor(wrapper);
    }

    // get the input of Wikidata Id and return the prediction result
    public WikidataElementInfos predict(String wikidataId) {
        return wikidataNERPredictor.predict(wikidataId);
    }
}
