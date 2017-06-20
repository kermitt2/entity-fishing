package com.scienceminer.nerd.service;

import org.grobid.core.lang.Language;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by lfoppiano on 16/06/2017.
 */
public class NerdQueryTest {
    NerdQuery target;

    @Before
    public void setUp() throws Exception {
        target = new NerdQuery();
    }


    @Test
    public void testHasValidLanguage_notSupportedLanguage_false() {
        target.setLanguage(new Language("it", 0.0));
        assertThat(target.hasValidLanguage(), is(false));
    }

    @Test
    public void testHasValidLanguage_null_false() {
        assertThat(target.hasValidLanguage(), is(false));
        target.setLanguage(new Language());
        assertThat(target.hasValidLanguage(), is(false));
    }

    @Test
    public void testHasValidLanguage_supportedLanguage_true() {
        target.setLanguage(new Language(Language.EN, 0.0));
        assertThat(target.hasValidLanguage(), is(true));

        target.setLanguage(new Language(Language.DE, 0.0));
        assertThat(target.hasValidLanguage(), is(true));

        target.setLanguage(new Language(Language.FR, 0.0));
        assertThat(target.hasValidLanguage(), is(true));
    }

}