package com.scienceminer.nerd.kb;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class KBUtilitiesTest {

    @Before
    public void setUp() {
        UpperKnowledgeBase.getInstance();
    }

    @Test
    public void testImmediateTaxonParents() {
        List<String> parents = UpperKnowledgeBase.getInstance().getParentTaxons("Q7377");

        System.out.println(parents);
    }

    @Test
    public void testFullTaxonParents() {
        List<String> parents = UpperKnowledgeBase.getInstance().getFullParentTaxons("Q18498");
        System.out.println("Q18498: " + parents);

        parents = UpperKnowledgeBase.getInstance().getFullParentTaxons("Q3200306");
        System.out.println("Q3200306: " + parents);
    }

    @Test
    public void testKingdomMethods() {
        boolean isAnimal = KBUtilities.isAnimal("Q18498");
        System.out.println("Q18498.isAnimal = " + isAnimal);

        isAnimal = KBUtilities.isAnimal("Q3200306");
        System.out.println("Q3200306.isAnimal = " + isAnimal);
    }

}