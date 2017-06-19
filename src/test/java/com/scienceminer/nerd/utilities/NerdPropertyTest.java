package com.scienceminer.nerd.utilities;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by lfoppiano on 19/06/2017.
 */
public class NerdPropertyTest {

    @Test
    public void testParseJsonProperty() throws Exception {
        NerdProperty output = NerdProperty.fromJson("{\"key\":\"name\", \"value\":\"john doe\", \"type\": \"string\"}");

        assertThat(output.getKey(), is("name"));
        assertThat(output.getValue(), is("john doe"));
        assertThat(output.getType().toString(), is("string"));
    }


    @Test
    public void testToJson() throws Exception {
        NerdProperty property = new NerdProperty();

        property.setKey("name");
        property.setValue("john");
        property.setType(NerdProperty.TYPE.STRING);

        assertThat(property.toJson(), is("{\"key\":\"name\",\"value\":\"john\",\"type\":\"string\"}"));
    }

}