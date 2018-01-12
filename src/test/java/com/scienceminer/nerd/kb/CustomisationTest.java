package com.scienceminer.nerd.kb;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class CustomisationTest {

    Customisations target;

    @Before
    public void setUp() throws Exception {

        target = new Customisations();
        final File tempFile = File.createTempFile("customisation", "output");
        tempFile.deleteOnExit();
        target.setCustomisationFile(tempFile);
    }

    @Test
    public void testCreate_Retrieve() throws Exception {
        String createJson = "{ \"wikipedia\" : [4764461, 51499, 1014346], \"freebase\": [\"/m/0cm2xh\", \"/m/0dl4z\", \"/m/02kxg_\", \"/m/06v9th\"], \"language\" : {\"lang\":\"en\"}, \"texts\": [\"World War I (WWI or WW1 or World War One), also known as the First World War or the Great War, was a global war centred in Europe that began on 28 July 1914 and lasted until 11 November 1918.\", \"The war drew in all the world's economic great powers, which were assembled in two opposing alliances: the Allies (based on the Triple Entente of the United Kingdom, France and the Russian Empire) and the Central Powers of Germany and Austria-Hungary.\"] }";
        boolean result = target.createCustomisation("testWW1", createJson);

        assertThat(result, is(true));

        assertThat(target.getCustomisations(), hasSize(1));
        String retrievedCustomisation = target.getCustomisation("testWW1");

        assertThat(retrievedCustomisation, is(createJson));
    }


    @Test
    public void testCreate_Update_Delete() {
        String createJson = "{ \"wikipedia\" : [4764461, 51499], \"freebase\": [\"/m/0cm2xh\", \"/m/0dl4z\", \"/m/02kxg_\", \"/m/06v9th\"],\"language\" : {\"lang\":\"en\"}, \"texts\": [\"World War I (WWI or WW1 or World War One), also known as the First World War or the Great War, was a global war centred in Europe that began on 28 July 1914 and lasted until 11 November 1918.\", \"The war drew in all the world's economic great powers, which were assembled in two opposing alliances: the Allies (based on the Triple Entente of the United Kingdom, France and the Russian Empire) and the Central Powers of Germany and Austria-Hungary.\"] }";

        target.createCustomisation("test2WW1", createJson);

        List<String> theCustomisations = target.getCustomisations();

        assertThat(theCustomisations, hasSize(1));
        assertThat(target.getCustomisation("test2WW1"), is(createJson));

        String updateJson = "{ \"wikipedia\" : [4764461, 1014346], \"freebase\": [],\"language\" : {\"lang\":\"en\"}, \"texts\": [\"No, no\"], \"description\" : \"This the war\" }";
        boolean result = target.updateCustomisation("testWW1", updateJson);

        assertThat(result, is(false));

        assertThat(target.getCustomisation("testWW1"), is(nullValue()));

        target.deleteCustomisation("test2WW1");

        assertThat(target.getCustomisations(), hasSize(0));
    }

    @Test
    public void testUpdateCustomisation_doesntExists_shouldReturnFalse() throws Exception {
        assertThat(target.updateCustomisation("bao", "{ \"wikipedia\" : [], \"language\" : {\"lang\":\"en\"} }"), is(false));
    }

    @Test
    public void testUpdateCustomisation_Exists_shouldReturnTrue() throws Exception {
        target.createCustomisation("bao", "{ \"wikipedia\" : [], \"language\" : {\"lang\":\"en\"} }");
        assertThat(target.updateCustomisation("bao", "{\"wikipedia\" : [], \"language\" : {\"lang\":\"en\"}, \"aaa\": \"bbb\"}"), is(true));
    }

    @Test
    public void testDelete_customisationDoesntExists_shouldbeignored() {
        target.deleteCustomisation("somethingRandom");
    }

    @Test
    public void testDelete_customisationExists_shouldbeDeleted() {
        target.createCustomisation("test", "{\"wikipedia\" : [], \"language\" : {\"lang\":\"en\"}}");
        assertThat(target.getCustomisations(), hasSize(1));
        target.deleteCustomisation("test");
        assertThat(target.getCustomisations(), hasSize(0));
    }
}