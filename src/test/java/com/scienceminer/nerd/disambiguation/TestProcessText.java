package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.utilities.StringPos;
import com.scienceminer.nerd.utilities.Utilities;
import org.grobid.core.data.Entity;
import org.grobid.core.lang.Language;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author Patrice Lopez
 */
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
    public void testNgram() {
        List<StringPos> ngrams = processText.ngrams("the house of card is here with us", 4);

        assertThat(ngrams, hasSize(26));
    }

}