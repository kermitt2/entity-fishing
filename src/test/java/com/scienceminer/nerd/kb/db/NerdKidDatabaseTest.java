package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.service.NerdRestService;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NerdKidDatabaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestService.class);

    UpperKnowledgeBase target = UpperKnowledgeBase.getInstance();

    @Test
    @Ignore("Make sure the nerdKid LMDB database is updated")
    public void testNerdKidPredictedClass(){
        try {
            List<String> wikidataId = new ArrayList<>();
            wikidataId.add("Q18498"); // Gray wolf-Canis lupus,ANIMAL
            wikidataId.add("Q1098"); // Uranium,SUBSTANCE
            wikidataId.add("Q362"); //  World War II,EVENT
            wikidataId.add("Q27470"); // Germains swiftlet,ANIMAL
            wikidataId.add("Q27471"); // Common firecrest,ANIMAL
            wikidataId.add("Q27549"); // Jacob Markström,PERSON
            wikidataId.add("Q29416"); // Alfons Kontarsky,PERSON
            wikidataId.add("Q29417"); // Tretinoin,SUBSTANCE
            wikidataId.add("Q29418"); // John Ashbery,PERSON
            wikidataId.add("Q29423"); // English articles,OTHER
            wikidataId.add("Q29424"); // Fathers and Sons (novel),CREATION
            wikidataId.add("Q35315"); // Ghomara language,CONCEPT
            wikidataId.add("Q412546"); // Wikimedia disambiguation page, OTHER
            wikidataId.add("Q18543268"); // Anatoliy Arestov, PERSON
            wikidataId.add("Q1744"); // Madonna, PERSON
            wikidataId.add("Q13"); // Triskaidekaphobia, UNKNOWN

            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(0)), is("ANIMAL")); // Gray wolf-Canis lupus,ANIMAL
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(1)), is("SUBSTANCE")); // Uranium,SUBSTANCE
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(2)), is("EVENT")); // World War II,EVENT
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(3)), is("ANIMAL")); // Germains swiftlet,ANIMAL
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(4)), is("ANIMAL")); // Common firecrest,ANIMAL
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(5)), is("PERSON")); // Jacob Markström,PERSON
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(6)), is("PERSON")); // Alfons Kontarsky,PERSON
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(7)), is("SUBSTANCE")); // Tretinoin,SUBSTANCE
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(8)), is("PERSON")); // John Ashbery,PERSON
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(9)), is("OTHER")); // English articles,OTHER
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(10)), is("CREATION")); // Fathers and Sons (novel),CREATION
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(11)), is("CONCEPT")); // Ghomara language,CONCEPT
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(12)), is("OTHER")); // Wikimedia disambiguation page, OTHER
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(13)), is("PERSON")); // Anatoliy Arestov, PERSON
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(14)), is("PERSON")); // Madonna, PERSON
            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(15)), is("OTHER")); // Triskaidekaphobia, UNKNOWN
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore("predicted as other class")
    public void testNerdKidPredictedClassMightNotWork(){
        try {
            List<String> wikidataId = new ArrayList<>();
            wikidataId.add("Q3487910"); // Swiss Bank Corporation,BUSINESS

            assertThat(target.getPredictedClassByWikidataId(wikidataId.get(0)), is("BUSINESS"));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}