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
        assertThat(nerTypePredictor.predict("Q152099").getPredictedClass(), is("PERSON"));
        assertThat(nerTypePredictor.predict("Q34389").getPredictedClass(), is("PERSON"));
        assertThat(nerTypePredictor.predict("Q629").getPredictedClass(), is("SUBSTANCE"));
        assertThat(nerTypePredictor.predict("Q19939").getPredictedClass(), is("ANIMAL"));
        assertThat(nerTypePredictor.predict("Q26463").getPredictedClass(), is("BUSINESS"));
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