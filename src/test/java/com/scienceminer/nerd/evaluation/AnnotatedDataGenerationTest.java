package com.scienceminer.nerd.evaluation;

import org.grobid.core.utilities.Pair;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class AnnotatedDataGenerationTest {

    AnnotatedDataGeneration target;

    @Before
    public void setUp() {
        target = new AnnotatedDataGeneration();
    }

    //@Test
    public void testExtractionPdfData_1() throws Exception {
        File input = new File(this.getClass().getResource("OB-oep-1830.pdf").getPath());

        final Pair<String, List<String>> output = target.extractPDFContent(input);

        assertThat(output.a, is("fr"));

        assertThat(output.b, hasSize(3));

        output.b.stream().forEach(s -> {
            //System.out.println(s);
            assertThat(s, not(startsWith("\n")));
        });

        assertThat(output.b.get(0).split("\n").length, is(35));
        assertThat(output.b.get(0).split("\n")[0], startsWith("Sur quoi reposent nos"));
        assertThat(output.b.get(0).split("\n")[1], is(""));
        assertThat(output.b.get(0).split("\n")[4], startsWith("Comme nous l'avons "));
    }

    //@Test
    public void testExtractionPdfData_2() throws Exception {
        File input = new File(this.getClass().getResource("OB-oep-1827.pdf").getPath());

        final Pair<String, List<String>> output = target.extractPDFContent(input);

        assertThat(output.a, is("fr"));

        assertThat(output.b, hasSize(1));
        //output.b.stream().forEach(System.out::println);
        output.b.stream().forEach(s -> assertThat(s, not(startsWith("\n"))));

        assertThat(output.b.get(0).split("\n").length, is(3));
        assertThat(output.b.get(0).split("\n")[0], startsWith("À PROPOS DE LA"));
        assertThat(output.b.get(0).split("\n")[1], is(""));
    }

    //@Test
    public void testExtractionPdfData_3() throws Exception {
        File input = new File(this.getClass().getResource("OB-obp-1523.pdf").getPath());

        final Pair<String, List<String>> output = target.extractPDFContent(input);

        assertThat(output.a, is("en"));

//        assertThat(output.b, hasSize(7));

        output.b.stream().forEach(s -> assertThat(s, not(startsWith("\n"))));
    }

    @Test
    public void testPostProcess_normalString() throws Exception {
        String string= "a normal string";

        assertThat(target.postProcess(string), is(string));
    }

    @Test
    public void testPostProcess_stringStartingWithDot() throws Exception {
        String string= ".a normal string";

        assertThat(target.postProcess(string), is("a normal string"));
    }

    @Test
    public void testPostProcess_stringStartingWithDotAndSpace() throws Exception {
        String string= ". a normal string";

        assertThat(target.postProcess(string), is("a normal string"));
    }

}