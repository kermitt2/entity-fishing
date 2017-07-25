package com.scienceminer.nerd.service;

import com.scienceminer.nerd.exceptions.QueryException;
import org.grobid.core.lang.Language;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
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

    @Test(expected = QueryException.class)
    public void testFromJson_nullValue_ShouldThrowException() throws Exception {
        target.fromJson(null);
    }

    @Test(expected = QueryException.class)
    public void testFromJson_emptyValue_ShouldThrowException() throws Exception {
        target.fromJson("");
    }

    @Test(expected = QueryException.class)
    public void testFromJson_invalidJsonValue_ShouldThrowException() throws Exception {
        target.fromJson("bao:miao");
    }

    @Test
    public void testFromJson_validJsonValue_ShouldWork() throws Exception {
        assertThat(target.fromJson("{\"name\":\"bao\"}"), isA(NerdQuery.class));
    }

}