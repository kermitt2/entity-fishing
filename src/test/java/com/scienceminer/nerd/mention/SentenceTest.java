package com.scienceminer.nerd.mention;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by lfoppiano on 15/06/2017.
 */
public class SentenceTest {

    @Test
    public void testListToJSON() throws Exception {

        Sentence sentence1 = new Sentence();
        sentence1.setOffsetStart(1);
        sentence1.setOffsetEnd(10);

        Sentence sentence2 = new Sentence();
        sentence2.setOffsetStart(11);
        sentence2.setOffsetEnd(20);

        String result = Sentence.listToJSON(Arrays.asList(sentence1, sentence2));

        assertThat(result, is("\"sentences\" : [ { \"offsetStart\" : 1, \"offsetEnd\" : 10 }, { \"offsetStart\" : 11, \"offsetEnd\" : 20 } ]"));
    }
    
}