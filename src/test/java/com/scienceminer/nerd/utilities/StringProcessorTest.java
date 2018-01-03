package com.scienceminer.nerd.utilities;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

public class StringProcessorTest {

    @Test
    public void testRemoveNonUtf8() throws Exception {
        final String input = "[…] Est-ce qu'on pourrait produire un logiciel de qualité en moins de temps que ça, en ne travaillant que sur du temps libre ? J'en doute.";
        assertThat(StringProcessor.removeInvalidUtf8Chars(input), startsWith("[] Est-ce"));
    }

}