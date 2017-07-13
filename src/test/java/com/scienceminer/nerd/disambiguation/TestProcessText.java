package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.utilities.StringPos;
import com.scienceminer.nerd.utilities.Utilities;
import org.grobid.core.data.Entity;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lang.Language;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TestProcessText {
    private String testPath = null;
    private ProcessText processText = null;

    static final String testText = "Other factors were also at play, said Felix Boni, head of research at " +
            "James Capel in Mexico City, such as positive technicals and economic uncertainty in Argentina, " +
            "which has put it and neighbouring Brazil's markets at risk.";

    @Before
    public void setUp() {
        NerdProperties.getInstance();
        testPath = NerdProperties.getTestPath();
        try {
            Utilities.initGrobid();
            processText = ProcessText.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testProcess() {
        if (processText == null) {
            System.err.println("text processor was not properly initialised!");
        }
        try {
            List<Entity> entities = processText.process(testText, new Language("en", 1.0));

            System.out.println("\n" + testText);
            if (entities != null) {
                for (Entity entity : entities) {
                    System.out.print(testText.substring(entity.getOffsetStart(), entity.getOffsetEnd()) + "\t");
                    System.out.println(entity.toString());
                }
            } else {
                System.out.println("No entity found.");
            }

			/*List<LayoutToken> tokens = new ArrayList<LayoutToken>();
            tokens.add(new LayoutToken("the"));
			tokens.add(new LayoutToken(" "));
			tokens.add(new LayoutToken("test"));
			List<List<LayoutToken>> pool = processText.ngrams(tokens, 2);*/
			/*for(List<LayoutToken> cand : pool) {
				System.out.println(LayoutTokensUtil.toText(cand));
			}*/
            //assertEquals(pool.size(), 3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAcronymsStringAllLower() {
        String input = "A graphical model or probabilistic graphical model (PGM) is a probabilistic model.";

        Map<OffsetPosition, Entity> acronyms = ProcessText.acronymCandidates(input, new Language("en", 1.0)); 
        assertNotNull(acronyms);
        for (Map.Entry<OffsetPosition, Entity> entry : acronyms.entrySet()) {
            Entity base = entry.getValue();
            OffsetPosition acronym = entry.getKey();
            assertEquals(input.substring(acronym.start, acronym.end).trim(), "PGM");
            assertEquals(base.getRawName(), "probabilistic graphical model");
        }
    }

    @Test
    public void testAcronymsTokensAllLower() {
        String input = "A graphical model or probabilistic graphical model (PGM) is a probabilistic model.";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        Map<OffsetPosition, Entity> acronyms = ProcessText.acronymCandidates(tokens); 
        assertNotNull(acronyms);
        for (Map.Entry<OffsetPosition, Entity> entry : acronyms.entrySet()) {
            Entity base = entry.getValue();
            OffsetPosition acronym = entry.getKey();
            assertEquals(input.substring(acronym.start, acronym.end).trim(), "PGM");
            assertEquals(base.getRawName(), "probabilistic graphical model");

            //System.out.println("acronym: " + input.substring(acronym.start, acronym.end) + " / base: " + base.getRawName());
        }
    }

    @Test
    public void testAcronymsStringMixedCase() {
        String input = "Cigarette smoke (CS)-induced airway epithelial senescence has been implicated in " + 
            "the pathogenesis of chronic obstructive pulmonary disease (COPD).";

        Map<OffsetPosition, Entity> acronyms = ProcessText.acronymCandidates(input, new Language("en", 1.0)); 
        assertNotNull(acronyms);
        for (Map.Entry<OffsetPosition, Entity> entry : acronyms.entrySet()) {
            Entity base = entry.getValue();
            OffsetPosition acronym = entry.getKey();
//System.out.println("acronym: " + input.substring(acronym.start, acronym.end) + " / base: " + base.getRawName());
            if (input.substring(acronym.start, acronym.end).trim().equals("CS")) {
                assertEquals(base.getRawName(), "Cigarette smoke");
            } else {
                assertEquals(input.substring(acronym.start, acronym.end).trim(), "COPD");
                assertEquals(base.getRawName(), "chronic obstructive pulmonary disease");
            }
        }
    } 

    @Test
    public void testAcronymsTokensMixedCase() {
        String input = "Cigarette smoke (CS)-induced airway epithelial senescence has been implicated in " + 
            "the pathogenesis of chronic obstructive pulmonary disease (COPD).";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        Map<OffsetPosition, Entity> acronyms = ProcessText.acronymCandidates(tokens); 
        assertNotNull(acronyms);
        for (Map.Entry<OffsetPosition, Entity> entry : acronyms.entrySet()) {
            Entity base = entry.getValue();
            OffsetPosition acronym = entry.getKey();
//System.out.println("acronym: " + input.substring(acronym.start, acronym.end) + " / base: " + base.getRawName());
            if (input.substring(acronym.start, acronym.end).trim().equals("CS")) {
                assertEquals(base.getRawName(), "Cigarette smoke");
            } else {
                assertEquals(input.substring(acronym.start, acronym.end).trim(), "COPD");
                assertEquals(base.getRawName(), "chronic obstructive pulmonary disease");
            }
        }
    }

    /*@Test
    public void testNgram() {
        if (processText == null) {
            System.err.println("text processor was not properly initialised!");
        }
        List<StringPos> ngrams = processText.ngrams("the house of card is here with us", 4);

        assertThat(ngrams, hasSize(26));
    }

    @Test
    public void testProcessBrutal() {
        if (processText == null) {
            System.err.println("text processor was not properly initialised!");
        }
        List<Entity> entities = processText.processBrutal("The Maven is here with us, beware not to be too aggressive.", "en");

        assertThat(entities, hasSize(5));
    }*/

}