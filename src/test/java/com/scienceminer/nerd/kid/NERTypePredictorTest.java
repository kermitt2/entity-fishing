package com.scienceminer.nerd.kid;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nerd.kid.extractor.wikidata.NerdKBFetcherWrapper;
import org.nerd.kid.extractor.wikidata.WikidataFetcherWrapper;
import org.nerd.kid.model.WikidataNERPredictor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NERTypePredictorTest {

    NERTypePredictor nerTypePredictor;
    WikidataNERPredictor wikidataNERPredictor1;

    @Before
    public void setUp() {
        try {
            // statements collected from the local LMDB (db/db-db)
            nerTypePredictor = new NERTypePredictor();

            // statements collected from entity-fishing API Service (http://nerd.huma-num.fr/nerd/service/kb/concept)
            WikidataFetcherWrapper wrapperNerdAPIService = new NerdKBFetcherWrapper();
            wikidataNERPredictor1 = new WikidataNERPredictor(wrapperNerdAPIService);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    // predict the NER of Wikdiata element based on the statements collected from the local LMDB
    @Ignore("make sure the correct nerdKid model is copied")
    public void predictTestMustValid1() {
        assertThat(nerTypePredictor.predict("Q152099").getPredictedClass(), is("PERSON")); // Constantine I of Greece (Q152099)
        assertThat(nerTypePredictor.predict("Q34389").getPredictedClass(), is("PERSON")); //  Whitney Houston (Q34389)
        assertThat(nerTypePredictor.predict("Q629").getPredictedClass(), is("SUBSTANCE")); //  oxygen (Q629)
        assertThat(nerTypePredictor.predict("Q19939").getPredictedClass(), is("ANIMAL")); //  tiger (Q19939)
        assertThat(nerTypePredictor.predict("Q26463").getPredictedClass(), is("BUSINESS")); //  Industrial and Commercial Bank of China (Q26463)
        assertThat(nerTypePredictor.predict("Q18498").getPredictedClass(), is("ANIMAL")); // Gray wolf-Canis lupus,ANIMAL
        assertThat(nerTypePredictor.predict("Q1098").getPredictedClass(), is("SUBSTANCE")); // Uranium,SUBSTANCE
        assertThat(nerTypePredictor.predict("Q362").getPredictedClass(), is("OTHER")); // World War II,EVENT
        assertThat(nerTypePredictor.predict("Q27470").getPredictedClass(), is("ANIMAL")); // Germains swiftlet,ANIMAL
        assertThat(nerTypePredictor.predict("Q27471").getPredictedClass(), is("ANIMAL")); // Common firecrest,ANIMAL
        assertThat(nerTypePredictor.predict("Q27549").getPredictedClass(), is("PERSON")); // Jacob Markström,PERSON
        assertThat(nerTypePredictor.predict("Q29416").getPredictedClass(), is("PERSON")); // Alfons Kontarsky,PERSON
        assertThat(nerTypePredictor.predict("Q29417").getPredictedClass(), is("OTHER")); // Tretinoin,SUBSTANCE
        assertThat(nerTypePredictor.predict("Q29418").getPredictedClass(), is("PERSON")); // John Ashbery,PERSON
        assertThat(nerTypePredictor.predict("Q29423").getPredictedClass(), is("OTHER")); // English articles,OTHER
        assertThat(nerTypePredictor.predict("Q29424").getPredictedClass(), is("CREATION")); // Fathers and Sons (novel),CREATION
        assertThat(nerTypePredictor.predict("Q35315").getPredictedClass(), is("CONCEPT")); // Ghomara language,CONCEPT
        assertThat(nerTypePredictor.predict("Q412546").getPredictedClass(), is("OTHER")); // Wikimedia disambiguation page, OTHER
        assertThat(nerTypePredictor.predict("Q18543268").getPredictedClass(), is("PERSON")); // Anatoliy Arestov, PERSON
        assertThat(nerTypePredictor.predict("Q1744").getPredictedClass(), is("PERSON")); // Madonna, PERSON
    }

    @Test
    @Ignore("Need to make sure the entity-fishibg service on huma-num server is activated")
    // predict the NER of Wikdiata element based on the statements collected from entity-fishing API Service
    // in order to make these tests valid, the entity-fishing service of http://nerd.huma-num.fr/nerd/ must be ensured active
    public void predictTestMustValid2() {
        assertThat(wikidataNERPredictor1.predict("Q152099").getPredictedClass(), is("PERSON"));
        assertThat(wikidataNERPredictor1.predict("Q34389").getPredictedClass(), is("PERSON"));
        assertThat(wikidataNERPredictor1.predict("Q629").getPredictedClass(), is("SUBSTANCE"));
        assertThat(wikidataNERPredictor1.predict("Q19939").getPredictedClass(), is("ANIMAL"));
        assertThat(wikidataNERPredictor1.predict("Q26463").getPredictedClass(), is("BUSINESS"));
    }

}