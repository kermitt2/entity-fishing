package com.scienceminer.nerd.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scienceminer.nerd.exceptions.QueryException;
import org.apache.commons.io.IOUtils;
import org.grobid.core.lang.Language;
import org.hamcrest.MatcherAssert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
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
        target.setLanguage(new Language("jp", 0.0));
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

        target.setLanguage(new Language(Language.ES, 0.0));
        assertThat(target.hasValidLanguage(), is(true));

        target.setLanguage(new Language(Language.IT, 0.0));
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

    //@Test(expected = QueryException.class)
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

        MatcherAssert.assertThat(json, is("{\"text\":\"bla bla\",\"mentions\":[\"ner\",\"wikipedia\"],\"customisation\":\"generic\",\"structure\":\"grobid\"}"));

        aQuery = mapper.readValue(json, NerdQuery.class);
        MatcherAssert.assertThat(aQuery.getText(), is("bla bla"));
    }

    @Test
    public void testToJson_checkDefaultValues_shouldWork() throws Exception {
        target.setText("this is a test of a query");

        final String jsonQuery = target.toJSON();
        assertThat(jsonQuery, is("{\"text\":\"this is a test of a query\",\"mentions\":[\"ner\",\"wikipedia\"],\"customisation\":\"generic\",\"structure\":\"grobid\"}"));

        JSONParser anotherParser = new JSONParser();
        final JSONObject parsed = (JSONObject) anotherParser.parse(jsonQuery);
        assertThat(parsed.keySet().size(), is(4));
        assertThat(parsed.get("text"), is("this is a test of a query"));
        assertThat(parsed.get("customisation"), is("generic"));
        assertThat(((JSONArray) parsed.get("mentions")).size(), is(2));

        final String manualJson = target.toJSONClean();
        final JSONObject parsedManualJson = (JSONObject) anotherParser.parse(manualJson);
        assertThat(parsedManualJson.keySet().size(), is(3));
        assertThat(parsedManualJson.get("text"), is("this is a test of a query"));
        assertThat(parsedManualJson.get("nbest"), is(false));
        assertThat(parsedManualJson.get("runtime"), is(0L));
    }


    @Test
    public void testGetQueryType_withText_shouldReturnText() throws Exception {
        target.setText("this is a text");
        assertThat(target.getQueryType(), is(NerdQuery.QUERY_TYPE_TEXT));
    }

    @Test
    public void testGetQueryType_withShortText_shouldReturnText() throws Exception {
        target.setShortText("this is a short text");
        assertThat(target.getQueryType(), is(NerdQuery.QUERY_TYPE_SHORT_TEXT));
    }

    @Test
    public void testGetQueryType_EmptyText_shouldReturnInvalid() throws Exception {
        target.setText("");
        assertThat(target.getQueryType(), is(NerdQuery.QUERY_TYPE_INVALID));
    }

    @Test
    public void testGetQueryType_NOOrNullValues_ShouldReturnInvalid() throws Exception {
        assertThat(target.getQueryType(), is(NerdQuery.QUERY_TYPE_INVALID));

        target = new NerdQuery();
        target.setText("");
        target.setShortText("");
        assertThat(target.getQueryType(), is(NerdQuery.QUERY_TYPE_INVALID));

        target = new NerdQuery();
        target.setShortText("");
        assertThat(target.getQueryType(), is(NerdQuery.QUERY_TYPE_INVALID));
    }


    @Test
    public void getTextOrShortText_shouldReturnText() throws Exception {
        target.setText("This is a text");
        assertThat(target.getTextOrShortText(), is("This is a text"));
        assertThat(target.getQueryType(), is(NerdQuery.QUERY_TYPE_TEXT));
    }

    @Test
    public void getTextOrShortText_shouldReturnShortText() throws Exception {
        target.setShortText("This is a short text");
        assertThat(target.getTextOrShortText(), is("This is a short text"));
        assertThat(target.getQueryType(), is(NerdQuery.QUERY_TYPE_SHORT_TEXT));
    }

    @Test
    public void getTextOrShortText_setBothVariables_shouldReturnText() throws Exception {
        target.setText("this is a text");
        target.setShortText("this is a short text");
        assertThat(target.getTextOrShortText(), is("this is a text"));
        assertThat(target.getQueryType(), is(NerdQuery.QUERY_TYPE_TEXT));
    }

    @Test
    public void testDeserialiseQueryAndBack() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        InputStream is = this.getClass().getResourceAsStream("/query.json");
        String theQuery = IOUtils.toString(is, UTF_8);
        NerdQuery nerdQuery = mapper.readValue(theQuery, NerdQuery.class);

        MatcherAssert.assertThat(nerdQuery.toJSON(), is("{\"text\":\"John\",\"mentions\":[\"ner\",\"wikipedia\"],\"customisation\":\"generic\",\"structure\":\"grobid\"}"));


    }

    @Test(expected = JsonParseException.class)
    public void testDeserialiseQueryAndBack_unknownField() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        NerdQuery nerdQuery = mapper.readValue("{'languages':{'lang':'fr'}}", NerdQuery.class);

    }

    @Test
    public void testDeserializeQuery_singleQuote() throws Exception {
        final NerdQuery nerdQuery = target.fromJson("{'mentions': ['ner']}");
        assertThat(nerdQuery.getMentions(), hasSize(1));

        final NerdQuery nerdQuery2 = target.fromJson("{\"mentions\": [\"ner\"]}");
        assertThat(nerdQuery2.getMentions(), hasSize(1));
    }


    @Test
    public void testDeserializeQuery_minRankScore() throws Exception {
        NerdQuery nerdQuery = target.fromJson("{'minRankerScore': 0.03}");
        assertThat(nerdQuery.getMinRankerScore(), is(0.03));

        nerdQuery = target.fromJson("{'minRankerScore': 0.0123}");
        assertThat(nerdQuery.getMinRankerScore(), is(0.0123));
    }


}