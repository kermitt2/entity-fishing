package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.service.NerdRestService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class NerdKidDatabaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestService.class);

    UpperKnowledgeBase target = UpperKnowledgeBase.getInstance();

    @Test
    public void testNerdKidPredictedClass(){
        try {
            List<String> wikidataId = new ArrayList<>();
            wikidataId.add("Q18498"); // Gray wolf-Canis lupus,ANIMAL
            wikidataId.add("Q1098"); // Uranium,SUBSTANCE
            wikidataId.add("Q362"); //  World War II,EVENT
            wikidataId.add("Q3487910"); // Swiss Bank Corporation,BUSINESS
            wikidataId.add("Q27470"); // Germains swiftlet,ANIMAL
            wikidataId.add("Q27471"); // Common firecrest,ANIMAL
            wikidataId.add("Q27549"); // Jacob Markström,PERSON
            wikidataId.add("Q29416"); // Alfons Kontarsky,PERSON
            wikidataId.add("Q29417"); // Tretinoin,SUBSTANCE
            wikidataId.add("Q29418"); // John Ashbery,PERSON
            wikidataId.add("Q29423"); // English articles,UNKNOWN
            wikidataId.add("Q29424"); // Fathers and Sons (novel),CREATION
            wikidataId.add("Q35315"); // Ghomara language,CONCEPT
            wikidataId.add("Q412546"); // Wikimedia disambiguation page, UNKNOWN

            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(0)), is("ANIMAL"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(1)), is("SUBSTANCE"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(2)), is("EVENT"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(3)), is("BUSINESS"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(4)), is("ANIMAL"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(5)), is("ANIMAL"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(6)), is("PERSON"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(7)), is("PERSON"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(8)), is("SUBSTANCE"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(9)), is("PERSON"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(10)), is("UNKNOWN"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(11)), is("CREATION"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(12)), is("CONCEPT"));
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(13)), is("UNKNOWN"));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}