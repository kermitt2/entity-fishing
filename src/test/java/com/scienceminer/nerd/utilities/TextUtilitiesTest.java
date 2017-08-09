package com.scienceminer.nerd.utilities;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TextUtilitiesTest {

    @Test
    public void testFormat4Digits() throws Exception {
        assertThat(TextUtilities.formatFourDecimals(0.0002), is("0.0002"));
        assertThat(TextUtilities.formatFourDecimals(20000), is("20000"));
        assertThat(TextUtilities.formatFourDecimals(2000.00234434), is("2000.0023"));
        assertThat(TextUtilities.formatFourDecimals(0.00234434), is("0.0023"));
    }

    @Test
    public void testFormat2Digits() throws Exception {
        assertThat(TextUtilities.formatTwoDecimals(0.0002), is("0"));
        assertThat(TextUtilities.formatTwoDecimals(20000), is("20000"));
        assertThat(TextUtilities.formatTwoDecimals(2000.00234434), is("2000"));
        assertThat(TextUtilities.formatTwoDecimals(0.01234434), is("0.01"));
    }


}