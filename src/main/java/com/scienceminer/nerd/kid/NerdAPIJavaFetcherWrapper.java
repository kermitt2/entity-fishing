package com.scienceminer.nerd.kid;

import com.scienceminer.nerd.kb.Concept;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import org.nerd.kid.data.WikidataElement;
import org.nerd.kid.extractor.wikidata.WikidataFetcherWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NerdAPIJavaFetcherWrapper implements WikidataFetcherWrapper {
    @Override
    public WikidataElement getElement(String wikidataId) {
        WikidataElement wikidataElement = new WikidataElement();
        Map<String, List<String>> propertiesCollected = new HashMap<>();
        List<String> propertiesNoValueCollected = new ArrayList<>();
        System.out.println("Fetch data through Nerd API JAVA... " + wikidataId);
        try {
            Concept concept = UpperKnowledgeBase.getInstance().getConcept(wikidataId);
            List<Statement> statements = concept.getStatements();
            if (statements != null) {
                for (Statement statement : statements) {
                    //wikidataId = statement.getConceptId(); // or concept.getId()
                    String propertyId = statement.getPropertyId();
                    String value = statement.getValue();
                    // property with values
                    if (value != null) {
                        // to make sure that the value is started by "Q"
                        if (value.startsWith("Q")) {
                            //if the property has not been added in the map, create a new key map from property
                            if (propertiesCollected.get(propertyId) == null) {
                                List<String> values = new ArrayList<>();
                                values.add(value);
                                propertiesCollected.put(propertyId, values);
                            } else {
                                //if the property have already exists in the map,just add the values
                                propertiesCollected.get(propertyId).add(value);
                            }
                        } else {
                            // property without any values
                            propertiesNoValueCollected.add(propertyId);
                        }
                    }
                }
            }
            // put the result in an object WikidataElement for the prediction objective
            wikidataElement.setId(wikidataId);
            wikidataElement.setProperties(propertiesCollected);
            wikidataElement.setPropertiesNoValue(propertiesNoValueCollected);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return wikidataElement;
    }
}
