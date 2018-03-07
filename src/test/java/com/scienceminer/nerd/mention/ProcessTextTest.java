package com.scienceminer.nerd.mention;

import com.scienceminer.nerd.utilities.StringPos;
import com.scienceminer.nerd.utilities.Utilities;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.prefs.PreferenceChangeEvent;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class ProcessTextTest {
    private String testPath = null;
    private ProcessText processText = null;

    static final String testText = "Other factors were also at play, said Felix Boni, head of research at " +
            "James Capel in Mexico City, such as positive technicals and economic uncertainty in Argentina, " +
            "which has put it and neighbouring Brazil's markets at risk.";

    @Before
    public void setUp() throws Exception {
        processText = new ProcessText(true);

    }

    @Test
    public void testAcronymsStringAllLower() {
        String input = "A graphical model or probabilistic graphical model (PGM) is a probabilistic model.";

        Map<Mention, Mention> acronyms = ProcessText.acronymCandidates(input, new Language("en", 1.0));
        assertNotNull(acronyms);
        for (Map.Entry<Mention, Mention> entry : acronyms.entrySet()) {
            Mention base = entry.getValue();
            Mention acronym = entry.getKey();
            assertEquals(input.substring(acronym.getOffsetStart(), acronym.getOffsetEnd()).trim(), "PGM");
            assertEquals(base.getRawName(), "probabilistic graphical model");
        }
    }

    @Test
    public void testAcronymsTokensAllLower() {
        String input = "A graphical model or probabilistic graphical model (PGM) is a probabilistic model.";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        Map<Mention, Mention> acronyms = ProcessText.acronymCandidates(tokens);
        assertThat(acronyms.entrySet(), hasSize(1));

        final ArrayList<Mention> keys = new ArrayList<>(acronyms.keySet());
        final Mention shortAcronym = keys.get(0);
        final Mention extendedAcronym = acronyms.get(shortAcronym);

        assertThat(extendedAcronym.getRawName(), is("probabilistic graphical model"));
        assertThat(input.substring(shortAcronym.getOffsetStart(), shortAcronym.getOffsetEnd()), is("PGM"));
    }

    @Test
    public void testAcronymsTokens() {
        String input = "Figure 4. \n" +
                "Canonical Correspondence Analysis (CCA) diagram showing the ordination of anopheline species along the\n" +
                "first two axes and their correlation with environmental variables. The first axis is horizontal, second vertical. Direction\n" +
                "and length of arrows shows the degree of correlation between mosquito larvae and the variables.";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        Map<Mention, Mention> acronyms = ProcessText.acronymCandidates(tokens);

        assertNotNull(acronyms);
        for (Map.Entry<Mention, Mention> entry : acronyms.entrySet()) {
            Mention base = entry.getValue();
            Mention acronym = entry.getKey();

            assertEquals(input.substring(acronym.getOffsetStart(), acronym.getOffsetEnd()).trim(), "CCA");
            assertEquals(base.getRawName(), "Canonical Correspondence Analysis");

            assertThat(acronym.getOffsetStart(), is(46));
            assertThat(acronym.getOffsetEnd(), is(49));
        }
    }


    @Test
    public void testAcronymsStringMixedCase() {
        String input = "Cigarette smoke (CS)-induced airway epithelial senescence has been implicated in " +
                "the pathogenesis of chronic obstructive pulmonary disease (COPD).";

        Map<Mention, Mention> acronyms = ProcessText.acronymCandidates(input, new Language("en", 1.0));
        assertNotNull(acronyms);
        for (Map.Entry<Mention, Mention> entry : acronyms.entrySet()) {
            Mention base = entry.getValue();
            Mention acronym = entry.getKey();
//System.out.println("acronym: " + input.substring(acronym.start, acronym.end) + " / base: " + base.getRawName());
            if (input.substring(acronym.getOffsetStart(), acronym.getOffsetEnd()).trim().equals("CS")) {
                assertEquals(base.getRawName(), "Cigarette smoke");
            } else {
                assertEquals(input.substring(acronym.getOffsetStart(), acronym.getOffsetEnd()).trim(), "COPD");
                assertEquals(base.getRawName(), "chronic obstructive pulmonary disease");
            }
        }
    }

    @Test
    public void testAcronymsTokensMixedCase() {
        String input = "Cigarette smoke (CS)-induced airway epithelial senescence has been implicated in " +
                "the pathogenesis of chronic obstructive pulmonary disease (COPD).";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        Map<Mention, Mention> acronyms = ProcessText.acronymCandidates(tokens);
        assertNotNull(acronyms);
        for (Map.Entry<Mention, Mention> entry : acronyms.entrySet()) {
            Mention base = entry.getValue();
            Mention acronym = entry.getKey();
//System.out.println("acronym: " + input.substring(acronym.start, acronym.end) + " / base: " + base.getRawName());
            if (input.substring(acronym.getOffsetStart(), acronym.getOffsetEnd()).trim().equals("CS")) {
                assertEquals(base.getRawName(), "Cigarette smoke");
            } else {
                assertEquals(input.substring(acronym.getOffsetStart(), acronym.getOffsetEnd()).trim(), "COPD");
                assertEquals(base.getRawName(), "chronic obstructive pulmonary disease");
            }
        }
    }

    @Test
    public void testDICECoefficient() throws Exception {
        String mention = "Setophaga ruticilla";
        Double dice = ProcessText.getDICECoefficient(mention, "en");
        System.out.println(mention + ": " + dice);

        mention = "Setophaga";
        dice = ProcessText.getDICECoefficient(mention, "en");
        System.out.println(mention + ": " + dice);

        mention = "ruticilla";
        dice = ProcessText.getDICECoefficient(mention, "en");
        System.out.println(mention + ": " + dice);

        mention = "bird";
        dice = ProcessText.getDICECoefficient(mention, "en");
        System.out.println(mention + ": " + dice);

        mention = "washing machine";
        dice = ProcessText.getDICECoefficient(mention, "en");
        System.out.println(mention + ": " + dice);

        mention = "washing";
        dice = ProcessText.getDICECoefficient(mention, "en");
        System.out.println(mention + ": " + dice);

        mention = "machine";
        dice = ProcessText.getDICECoefficient(mention, "en");
        System.out.println(mention + ": " + dice);
    }

    /*@Test
    public void testProcessSpecies() throws Exception {
        List<Mention> entities = processText.processSpecies("The mouse is here with us, beware not to be too aggressive.",
                new Language("en"));

        assertThat(entities, hasSize(1));
    }*/

    /*@Test
    public void testProcessSpecies2() {
        if (processText == null) {
            System.err.println("text processor was not properly initialised!");
        }
        try {
            List<Mention> entities = processText.processSpecies("Morphological variation in hybrids between Salmo marmoratus and alien Salmo species in the Volarja stream, Soca River basin, Slovenia", 
                new Language("en"));

            assertThat(entities, hasSize(1));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }*/


    @Test
    public void testParagraphSegmentation() {
        // create a dummy super long text to be segmented
        List<LayoutToken> tokens = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            if (i == 250) {
                tokens.add(new LayoutToken("\n"));
            }
            if (i == 500) {
                tokens.add(new LayoutToken("\n"));
                tokens.add(new LayoutToken("\n"));
            }
            tokens.add(new LayoutToken("blabla"));
            tokens.add(new LayoutToken(" "));
        }

        List<List<LayoutToken>> segments = ProcessText.segmentInParagraphs(tokens);
        assertThat(segments, hasSize(5));
    }

    @Test
    public void testParagraphSegmentationMonolithic() {
        // create a dummy super long text to be segmented
        List<LayoutToken> tokens = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(new LayoutToken("blabla"));
            tokens.add(new LayoutToken(" "));
        }

        List<List<LayoutToken>> segments = ProcessText.segmentInParagraphs(tokens);
        assertThat(segments, hasSize(4));
    }

    @Test
    @Ignore("This test is failing")
    public void testNGram_old_oneGram_shouldWork() throws Exception {
        final String input = "this is it.";

        final List<StringPos> result = ProcessText.ngrams(input, 1, new Language("en"));
        System.out.println(result);

        assertThat(result, hasSize(6));
        assertThat(result.get(0), is(new StringPos("this", 0)));
        assertThat(result.get(1), is(new StringPos(" ", 4)));
        assertThat(result.get(2), is(new StringPos("is", 5)));
        assertThat(result.get(3), is(new StringPos(" ", 7)));
    }

    @Test
    @Ignore("This test is failing too")
    public void testNGram_old_biGram_shouldWork() throws Exception {
        final String input = "this is it.";

        final List<StringPos> result = ProcessText.ngrams(input, 2, new Language("en"));
        System.out.println(result);

        assertThat(result, hasSize(6));
        assertThat(result.get(0), is(new StringPos("this", 0)));
        assertThat(result.get(1), is(new StringPos("this ", 4)));
        assertThat(result.get(2), is(new StringPos(" ", 5)));
        assertThat(result.get(3), is(new StringPos(" is", 7)));
    }

    @Test
    public void testNGram_new_oneGram_shouldWork() throws Exception {
        final String input = "this is it.";

        final List<LayoutToken> inputLayoutTokens = GrobidAnalyzer.getInstance()
                .tokenizeWithLayoutToken(input, new Language("en"));

        final List<StringPos> result = processText.ngrams(inputLayoutTokens, 1);
        System.out.println(result);

        assertThat(result, hasSize(6));
        assertThat(result.get(0), is(new StringPos("this", 0)));
        assertThat(result.get(1), is(new StringPos(" ", 4)));
        assertThat(result.get(2), is(new StringPos("is", 5)));
        assertThat(result.get(3), is(new StringPos(" ", 7)));
    }

    @Test
    @Ignore("This test is not testing anything")
    public void testNGram_twoGram_shouldWork() throws Exception {
        final String input = "this is it.";

        final List<StringPos> old = processText.ngrams(input, 2, new Language("en"));
        Collections.sort(old);
        old.remove(4);
        System.out.println(old);

        final List<StringPos> newd = processText.ngrams(GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input), 2);
        Collections.sort(newd);
        System.out.println(newd);
    }

    @Test
    @Ignore("This test is not testing anything")
    public void extractMentionsWikipedia() throws Exception {
        final String input = "this is it.";

        final Language language = new Language("en");
        final List<LayoutToken> inputLayoutTokens = GrobidAnalyzer.getInstance()
                .tokenizeWithLayoutToken(input, language);

        System.out.println(processText.extractMentionsWikipedia(inputLayoutTokens, language));

        System.out.println(processText.extractMentionsWikipedia(input, language));

    }
}