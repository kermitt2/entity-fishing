package com.scienceminer.nerd.evaluation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

public class EvaluationDataGenerationTest {

    EvaluationDataGeneration target;

    @Before
    public void setUp() {
        target = new EvaluationDataGeneration();
    }

    @Test
    public void testExtraction1PdfData() throws Exception {
        File input = new File(this.getClass().getResource("OB-oep-1830.pdf").getPath());

        final Map<String, String> output = target.extractPDFContent(input);

        assertThat(output.get("LANG"), is("fr"));

        assertThat(output.get("TEXT").split("\n").length, is(65));
        assertThat(output.get("TEXT").split("\n")[0], startsWith("Sur quoi reposent nos"));
        assertThat(output.get("TEXT").split("\n")[1], is(""));
        assertThat(output.get("TEXT").split("\n")[4], startsWith("Comme nous l'avons "));
    }

    @Test
    public void testExtraction2PdfData() throws Exception {
        File input = new File(this.getClass().getResource("OB-oep-1827.pdf").getPath());

        final Map<String, String> output = target.extractPDFContent(input);

        assertThat(output.get("LANG"), is("fr"));

        assertThat(output.get("TEXT").split("\n").length, is(3));
        assertThat(output.get("TEXT").split("\n")[0], startsWith("À PROPOS DE LA"));
        assertThat(output.get("TEXT").split("\n")[1], is(""));
    }
}