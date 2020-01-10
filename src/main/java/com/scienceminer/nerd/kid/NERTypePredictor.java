package com.scienceminer.nerd.kid;

import com.scienceminer.nerd.kb.Statement;
import org.nerd.kid.data.WikidataElement;
import org.nerd.kid.data.WikidataElementInfos;
import org.nerd.kid.extractor.wikidata.WikidataFetcherWrapper;
import org.nerd.kid.model.WikidataNERPredictor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*  A class for predicting NER type for Wikidata elements.
    The statements for eace element are collected directly from the local LMDB (db/db-kb)
    through the class NerdKBDBFetcherWrapper.
* */

public class NERTypePredictor {
    private WikidataNERPredictor wikidataNERPredictor;

    public NERTypePredictor(){
        WikidataFetcherWrapper wrapper = new NerdKBDBFetcherWrapper();
        wikidataNERPredictor = new WikidataNERPredictor(wrapper);
    }

    // get the input of Wikidata Id and return the prediction result, complete with other information (Wiki Id, label, statements, prediction result
    public WikidataElementInfos predict(String wikidataId) {
        return wikidataNERPredictor.predict(wikidataId);
    }

    // method for predicting Wikidata Id having statements
    public WikidataElementInfos predict(String wikidataId, List<Statement> statements) {
        // grouping the collected statements based by its Wikidata Ids
        final Map<String, List<String>> collect = statements.stream().collect(
                Collectors.groupingBy(Statement::getPropertyId, Collectors.mapping(Statement::getValue, Collectors.toList())
                )
        );

        WikidataElement element = new WikidataElement();
        element.setId(wikidataId);
        element.setProperties(collect);
        element.setPropertiesNoValue(new ArrayList<>(collect.keySet()));
        return wikidataNERPredictor.predict(element);
    }
}
