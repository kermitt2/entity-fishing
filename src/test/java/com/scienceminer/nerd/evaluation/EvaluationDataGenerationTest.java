package com.scienceminer.nerd.evaluation;

import org.grobid.core.utilities.Pair;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
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

        final Pair<String, List<String>> output = target.extractPDFContent(input);

        assertThat(output.a, is("fr"));

        assertThat(output.b, hasSize(4));

        output.b.stream().forEach(
                f -> System.out.println(f)
        );

        assertThat(output.b.get(0).split("\n").length, is(25));
        assertThat(output.b.get(0).split("\n")[0], startsWith("Sur quoi reposent nos"));
        assertThat(output.b.get(0).split("\n")[1], is(""));
        assertThat(output.b.get(0).split("\n")[4], startsWith("Comme nous l'avons "));
    }

    @Test
    public void testExtraction2PdfData() throws Exception {
        File input = new File(this.getClass().getResource("OB-oep-1827.pdf").getPath());

        final Pair<String, List<String>> output = target.extractPDFContent(input);

        assertThat(output.a, is("fr"));

        assertThat(output.b, hasSize(1));
        assertThat(output.b.get(0).split("\n").length, is(3));
        assertThat(output.b.get(0).split("\n")[0], startsWith("À PROPOS DE LA"));
        assertThat(output.b.get(0).split("\n")[1], is(""));
    }

}