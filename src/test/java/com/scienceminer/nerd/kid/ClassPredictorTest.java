package com.scienceminer.nerd.kid;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ClassPredictorTest {
    ClassPredictor classPredictor;

    @Before
    public void setUp() {
        try {
            classPredictor = new ClassPredictor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void predictTestMustValid() {
        assertThat(classPredictor.predict("Q76").getPredictedClass(), is("PERSON"));
        assertThat(classPredictor.predict("Q34389").getPredictedClass(), is("PERSON"));
        assertThat(classPredictor.predict("Q629").getPredictedClass(), is("SUBSTANCE"));
        assertThat(classPredictor.predict("Q19939").getPredictedClass(), is("ANIMAL"));
        assertThat(classPredictor.predict("Q26463").getPredictedClass(), is("BUSINESS"));
    }

    @Test
    @Ignore("predicted as other types")
    public void predictTestMightNotValid() {
        assertThat(classPredictor.predict("Q28119").getPredictedClass(), is("ACRONYM"));
        assertThat(classPredictor.predict("Q23644849").getPredictedClass(), is("TITLE"));
        assertThat(classPredictor.predict("Q246592").getPredictedClass(), is("AWARD"));
    }
}