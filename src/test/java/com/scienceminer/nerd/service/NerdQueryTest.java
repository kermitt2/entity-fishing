package com.scienceminer.nerd.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scienceminer.nerd.exceptions.QueryException;
import org.apache.commons.io.IOUtils;
import org.grobid.core.lang.Language;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
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
        assertThat(target.fromJson("{\"text\":\"bao\"}"), isA(NerdQuery.class));
    }

    @Test
    public void testSerialiseQueryAndBack() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        NerdQuery aQuery = new NerdQuery();
        aQuery.setText("bla bla");

        String json = aQuery.toJSON();

        MatcherAssert.assertThat(json, is("{\"text\":\"bla bla\",\"format\":\"JSON\",\"customisation\":\"generic\"}"));

        aQuery = mapper.readValue(json, NerdQuery.class);
        MatcherAssert.assertThat(aQuery.getText(), is("bla bla"));
    }


    @Test
    public void testDeserialiseQueryAndBack() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        InputStream is = this.getClass().getResourceAsStream("/query.json");
        String theQuery = IOUtils.toString(is, UTF_8);
        NerdQuery nerdQuery = mapper.readValue(theQuery, NerdQuery.class);

        MatcherAssert.assertThat(nerdQuery.toJSON(), is("{\"text\":\"John\",\"format\":\"JSON\",\"customisation\":\"generic\"}"));


    }

    @Test(expected = JsonParseException.class)
    public void testDeserialiseQueryAndBack_unknownField() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        NerdQuery nerdQuery = mapper.readValue("{'languages':{'lang':'fr'}}", NerdQuery.class);

    }
}