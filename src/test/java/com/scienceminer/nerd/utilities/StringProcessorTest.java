package com.scienceminer.nerd.utilities;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

public class StringProcessorTest {

    @Test
    public void testRemoveNonUtf8() throws Exception {
        final String input = "[…] Est-ce qu'on pourrait produire un logiciel de qualité en moins de temps que ça, en ne travaillant que sur du temps libre ? J'en doute.";
        assertThat(StringProcessor.removeInvalidUtf8Chars(input), startsWith("[] Est-ce"));
    }

    @Test
    public void adjustLetterCase_fullCase_shouldLowerCase() {

        String input = "THIS IS A TITLE";

        final List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        final List<LayoutToken> result = StringProcessor.adjustLetterCase(layoutTokens);

        assertThat(LayoutTokensUtil.toText(result), is(lowerCase(input)));
        
    }

    @Test
    public void adjustLetterCase_fullCase_2_shouldLowerCase() {

        String input = "THIS IS A TITLE.";

        final List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        final List<LayoutToken> result = StringProcessor.adjustLetterCase(layoutTokens);

        assertThat(LayoutTokensUtil.toText(result), is(lowerCase(input)));

    }

    @Test
    public void adjustLetterCase_initialUpperCase_shouldLowerCase() {

        String input = "This Is A Title";

        final List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        final List<LayoutToken> result = StringProcessor.adjustLetterCase(layoutTokens);

        assertThat(LayoutTokensUtil.toText(result), is(input.charAt(0) + lowerCase(input.substring(1, input.length()))));
    }

    @Test
    public void adjustLetterCase_initialUpperCase_2_shouldLowerCase() {

        String input = "This Is A, Title";

        final List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        final List<LayoutToken> result = StringProcessor.adjustLetterCase(layoutTokens);

        assertThat(LayoutTokensUtil.toText(result), is(input.charAt(0) + lowerCase(input.substring(1, input.length()))));

    }
}