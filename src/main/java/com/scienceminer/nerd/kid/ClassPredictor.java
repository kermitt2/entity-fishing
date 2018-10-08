package com.scienceminer.nerd.kid;

import com.scienceminer.nerd.kb.Statement;
import org.nerd.kid.data.WikidataElement;
import org.nerd.kid.data.WikidataElementInfos;
import org.nerd.kid.extractor.FeatureDataExtractor;
import org.nerd.kid.extractor.wikidata.WikidataFetcherWrapper;
import org.nerd.kid.model.WikidataNERPredictor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassPredictor {

    private WikidataNERPredictor wikidataNERPredictor;

    public ClassPredictor() {
        WikidataFetcherWrapper wrapper = new NerdAPIJavaFetcherWrapper();
        wikidataNERPredictor = new WikidataNERPredictor(wrapper);
    }

    // get the input of Wikidata Id and return the prediction result
    public WikidataElementInfos predict(String wikidataId) {
        return wikidataNERPredictor.predict(wikidataId);
    }

    public WikidataElementInfos predict(String wikidataId, List<Statement> statements) {
//        FeatureDataExtractor featureDataExtractor = new FeatureDataExtractor();

        final Map<String, List<String>> collect = statements.stream().collect(
                Collectors.groupingBy(Statement::getPropertyId, Collectors.mapping(Statement::getValue, Collectors.toList())
                )
        );

//        final Double[] featureWikidata = featureDataExtractor.getFeatureWikidata(collect);
        WikidataElement element = new WikidataElement();
        element.setId(wikidataId);
        element.setProperties(collect);
        element.setPropertiesNoValue(new ArrayList<>(collect.keySet()));
        return wikidataNERPredictor.predict(element);
    }
}
