package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.kb.Definition;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class NerdEntityTest {

    NerdEntity target ;

    @Before
    public void setUp() throws Exception {
        target = new NerdEntity();
    }



    @Test
    public void testJsonCompact_simpleExample_shouldWork() throws Exception {
        final String name = "Name";
        final String preferredTerm = "A preferred term";

        target.setRawName(name);
        target.setPreferredTerm(preferredTerm);
        target.setOffsetStart(12);
        target.setOffsetEnd(15);

        String json = target.toJsonCompact();
//        System.out.println(json);

        JSONParser parser = new JSONParser();

        JSONObject object = (JSONObject) parser.parse(json);

        assertThat(object.get("rawName"), is(name));
        assertThat(object.get("preferredTerm"), is(nullValue()));
//        assertThat(object.get("nerd_selection_score"), is(0L));
//        assertThat(object.get("nerd_score"), is(0L));
        assertThat(object.get("confidence_score"), is(0L));
    }

    @Test
    public void testJsonCompact_missingName_shouldWork() throws Exception {
        final String preferredTerm = "A preferred term";

        target.setPreferredTerm(preferredTerm);
        target.setOffsetStart(12);
        target.setOffsetEnd(15);

        String json = target.toJsonCompact();
//        System.out.println(json);

        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(json);

        assertThat(object.get("rawName"), is(""));
        assertThat(object.get("preferredTerm"), is(nullValue()));
//        assertThat(object.get("nerd_selection_score"), is(0L));
//        assertThat(object.get("nerd_score"), is(0L));
        assertThat(object.get("confidence_score"), is(0L));
    }

    @Test
    public void testJsonFull_simpleExample_shouldWork() throws Exception {
        final String name = "Don't give me a name";
        final String preferredTerm = "A preferred term";

        target.setRawName(name);
        target.setPreferredTerm(preferredTerm);
        target.setOffsetStart(12);
        target.setOffsetEnd(15);

        String json = target.toJsonFull();

        JSONParser parser = new JSONParser();

        JSONObject object = (JSONObject) parser.parse(json);

        assertThat(object.get("rawName"), is(name));
        assertThat(object.get("preferredTerm"), is(preferredTerm));
//        assertThat(object.get("nerd_selection_score"), is(0L));
//        assertThat(object.get("nerd_score"), is(0L));
        assertThat(object.get("confidence_score"), is(0L));
    }

    @Test
    public void testJsonFull_missingName_shouldWork() throws Exception {
        final String preferredTerm = "A preferred term";

        target.setPreferredTerm(preferredTerm);
        target.setOffsetStart(12);
        target.setOffsetEnd(15);

        String json = target.toJsonFull();

        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(json);

        assertThat(object.get("rawName"), is(""));
        assertThat(object.get("preferredTerm"), is(preferredTerm));
//        assertThat(object.get("nerd_selection_score"), is(0L));
//        assertThat(object.get("nerd_score"), is(0L));
        assertThat(object.get("confidence_score"), is(0L));
    }

    @Test
    public void testJsonFull_nullDefinition_shouldWork() throws Exception {
        final String preferredTerm = "A preferred term";

        target.setPreferredTerm(preferredTerm);
        target.setOffsetStart(12);
        target.setOffsetEnd(15);

        final Definition definition = new Definition();
        definition.setDefinition("bao");
        target.setDefinitions(Arrays.asList(definition, new Definition()));

        String json = target.toJsonFull();

        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(json);

        JSONArray definitions = (JSONArray) object.get("definitions");
        assertThat(definitions.size(), is(1));
    }
}