package com.scienceminer.nerd.disambiguation;

import org.grobid.core.lexicon.NERLexicon;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class PruningServiceTest {

    PruningService target;

    @Before
    public void setUp() throws Exception {
        target = new PruningService();
    }

    @Test
    public void testPruneOverlap_sameEntity_shouldRemoveOne() throws Exception {
        NerdEntity entity1 = new NerdEntity("Austria", 0, 7);
        entity1.setNormalisedName("Austria");
        entity1.setWikipediaExternalRef(1234);
        entity1.setNerdScore(0.6);
        NerdEntity entity2 = new NerdEntity("Austria", 0, 7);
        entity2.setNormalisedName("Austria");
        entity2.setWikipediaExternalRef(1234);
        entity2.setNerdScore(0.5);

        List<NerdEntity> result = target.pruneOverlap(Arrays.asList(entity1, entity2), false);

        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(entity2));
    }


    @Test
    public void testPruneOverlap_sameEntityButDifferentNERType_shouldRemoveOne() throws Exception {
        NerdEntity entity1 = new NerdEntity("Austria", 0, 7);
        entity1.setNormalisedName("Austria");
        entity1.setWikipediaExternalRef(1234);
        entity1.setNerdScore(0.6);
        NerdEntity entity2 = new NerdEntity("Austria", 0, 7);
        entity2.setNormalisedName("Austria");
        entity2.setWikipediaExternalRef(1234);
        entity2.setNerdScore(0.5);
        entity2.setType(NERLexicon.NER_Type.LOCATION);

        List<NerdEntity> result = target.pruneOverlap(Arrays.asList(entity1, entity2), false);

        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(entity2));
    }

    @Test
    public void testPruneOverlap_nullRawName_shouldRemoveIt() throws Exception {
        NerdEntity entity1 = new NerdEntity(null, 0, 10);
        NerdEntity entity2 = new NerdEntity("test1", 0, 10);
        entity2.setNormalisedName("test1");

        List<NerdEntity> result = target.pruneOverlap(Arrays.asList(entity1, entity2), false);

        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(entity2));
    }


    @Test
    public void testPruneOverlap_differentEntitiesOverlapping_sameProb_shouldRemoveShorter() throws Exception {
        NerdEntity entity1 = new NerdEntity("German", 0, 6);
        entity1.setNormalisedName("German");
        entity1.setWikipediaExternalRef(1234);
        entity1.setNerdScore(0.5);
        NerdEntity entity2 = new NerdEntity("German Army", 0, 11);
        entity2.setNormalisedName("German Army");
        entity2.setWikipediaExternalRef(5678);
        entity2.setNerdScore(0.4);

        List<NerdEntity> result = target.pruneOverlap(Arrays.asList(entity1, entity2), false);

        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(entity2));
    }

    @Test
    public void testPruneOverlap_differentEntitiesOverlapping_differentProb_shouldRemoveShorter() throws Exception {
        NerdEntity entity1 = new NerdEntity("German", 0, 6);
        entity1.setNormalisedName("German");
        entity1.setWikipediaExternalRef(1234);
        entity1.setNerdScore(0.9);
        NerdEntity entity2 = new NerdEntity("German Army", 0, 11);
        entity2.setNormalisedName("German Army");
        entity2.setWikipediaExternalRef(5678);
        entity2.setNerdScore(0.3);

        List<NerdEntity> result = target.pruneOverlap(Arrays.asList(entity1, entity2), false);

        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(entity2));
    }


    /**
     * {
         "rawName": "German-occupied territory",
         "offsetStart": 2413,
         "offsetEnd": 2438,
         "nerd_score": 1,
         "nerd_selection_score": 0.6295,
         "wikipediaExternalRef": 4900822,
         "wikidataId": "Q553157",
         "domains": [
            "Military"
         ]
     },
     {
         "rawName": "occupied territory",
         "offsetStart": 2420,
         "offsetEnd": 2438,
         "nerd_score": 1,
         "nerd_selection_score": 0.5973,
         "wikipediaExternalRef": 816214,
         "wikidataId": "Q188686",
         "domains": [
            "Military"
         ]
     },
     */

    @Test
    public void testPruneOverlap_realExample_shouldPrune() throws Exception {
        NerdEntity entity1 = new NerdEntity("German-occupied territory", 2413, 2438);
        entity1.setNormalisedName("German-occupied territory");
        entity1.setWikipediaExternalRef(4900822);
        entity1.setWikidataId("Q553157");
        entity1.setNerdScore(1);
        entity1.setSelectionScore(0.6295);

        NerdEntity entity2 = new NerdEntity("occupied territory", 2420, 2438);
        entity2.setNormalisedName("occupied territory");
        entity2.setWikipediaExternalRef(816214);
        entity2.setWikidataId("Q188686");
        entity2.setNerdScore(1);
        entity2.setSelectionScore(0.5973);

        final List<NerdEntity> result = target.pruneOverlap(Arrays.asList(entity1, entity2), false);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getRawName(), is("German-occupied territory"));
    }

    @Test
    public void testPruneOverlapNBest_differentEntity_overlapping_shouldNotRemove() throws Exception {
        NerdEntity entity1 = new NerdEntity("Austria", 0, 10);
        entity1.setNormalisedName("Austria");
        entity1.setWikipediaExternalRef(1);
        entity1.setNerdScore(0.9);
        NerdEntity entity2 = new NerdEntity("Austria", 0, 10);
        entity2.setNormalisedName("Austria");
        entity2.setWikipediaExternalRef(12);
        entity2.setNerdScore(0.7);

        List<NerdEntity> result = target.pruneOverlapNBest(Arrays.asList(entity1, entity2), false);

        assertThat(result, hasSize(2));
    }

    @Test
    public void testPruneOverlapNBest_overlapping_NE_shouldremoveOne() throws Exception {
        NerdEntity entity1 = new NerdEntity("Austria", 0, 10);
        entity1.setNormalisedName("Austria");
        entity1.setWikipediaExternalRef(1);
        entity1.setNerdScore(0.9);
        entity1.setType(NERLexicon.NER_Type.LOCATION);
        NerdEntity entity2 = new NerdEntity("Austria", 0, 10);
        entity2.setNormalisedName("Austria");
        entity2.setWikipediaExternalRef(1);
        entity2.setNerdScore(0.7);

        List<NerdEntity> result = target.pruneOverlapNBest(Arrays.asList(entity1, entity2), false);

        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(entity1));
    }

    @Test
    public void testPruneOverlapNBest_differentEntities_notOverlapping_shouldNotRemove() throws Exception {
        NerdEntity entity1 = new NerdEntity("Italy", 11, 20);
        entity1.setNormalisedName("Italy");
        entity1.setWikipediaExternalRef(1);
        entity1.setNerdScore(0.5);
        NerdEntity entity2 = new NerdEntity("Germany", 0, 10);
        entity2.setNormalisedName("Germany");
        entity2.setWikipediaExternalRef(12);
        entity2.setNerdScore(0.6);

        List<NerdEntity> result = target.pruneOverlapNBest(Arrays.asList(entity1, entity2), false);

        assertThat(result, hasSize(2));
    }

    @Test
    public void testPruneOverlapNBest_differentEntity_Overlapping_shouldRemove() throws Exception {
        NerdEntity entity1 = new NerdEntity("German", 0, 6);
        entity1.setNormalisedName("German");
        entity1.setWikipediaExternalRef(1);
        entity1.setNerdScore(0.5);
        NerdEntity entity2 = new NerdEntity("German Army", 0, 11);
        entity2.setNormalisedName("German Army");
        entity2.setWikipediaExternalRef(12);
        entity2.setNerdScore(0.9);

        List<NerdEntity> result = target.pruneOverlapNBest(Arrays.asList(entity1, entity2), false);

        assertThat(result, hasSize(2));
    }

    @Test
    public void testAreEntityOverlapping_notOverlapping_sholdReturnFalse() throws Exception {
        final NerdEntity entity1 = new NerdEntity("test", 0, 10);
        final NerdEntity entity2 = new NerdEntity("test", 11, 20);
        assertThat(target.areEntityOverlapping(entity1, entity2), is(false));
    }

    @Test
    public void testAreEntityOverlapping_notOverlapping2_sholdReturnFalse() throws Exception {
        final NerdEntity entity1 = new NerdEntity("test", 10, 20);
        final NerdEntity entity2 = new NerdEntity("test", 21, 40);
        assertThat(target.areEntityOverlapping(entity1, entity2), is(false));
    }

    @Test
    public void testAreEntityOverlapping_Overlapping_sholdReturnTrue() throws Exception {
        final NerdEntity entity1 = new NerdEntity("test", 0, 10);
        final NerdEntity entity2 = new NerdEntity("test", 0, 10);
        assertThat(target.areEntityOverlapping(entity1, entity2), is(true));
    }

    @Test
    public void testAreEntityOverlapping_Overlapping2_sholdReturnTrue() throws Exception {
        final NerdEntity entity1 = new NerdEntity("test", 5, 10);
        final NerdEntity entity2 = new NerdEntity("test", 0, 14);
        assertThat(target.areEntityOverlapping(entity1, entity2), is(true));
    }


}