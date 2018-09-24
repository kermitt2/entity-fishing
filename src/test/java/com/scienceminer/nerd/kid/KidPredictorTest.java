package com.scienceminer.nerd.kid;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class KidPredictorTest {
    KidPredictor kidPredictor;

    @Before
    public void setUp() {
        try {
            kidPredictor = new KidPredictor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void predictTest() {
        KidPredictor kidPredictor = new KidPredictor();
        assertThat(kidPredictor.predict("Q76").getPredictedClass(), is("PERSON"));
        assertThat(kidPredictor.predict("Q34389").getPredictedClass(), is("PERSON"));
        assertThat(kidPredictor.predict("Q28119").getPredictedClass(), is("ACRONYM"));
        assertThat(kidPredictor.predict("Q34687").getPredictedClass(), is("PLANT"));
        assertThat(kidPredictor.predict("Q629").getPredictedClass(), is("SUBSTANCE"));
        assertThat(kidPredictor.predict("Q23644849").getPredictedClass(), is("TITLE"));
        assertThat(kidPredictor.predict("Q19939").getPredictedClass(), is("ANIMAL"));
        assertThat(kidPredictor.predict("Q246592").getPredictedClass(), is("AWARD"));
        assertThat(kidPredictor.predict("Q26463").getPredictedClass(), is("BUSINESS"));

    }
}