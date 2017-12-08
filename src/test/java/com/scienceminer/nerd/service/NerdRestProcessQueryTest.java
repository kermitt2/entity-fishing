package com.scienceminer.nerd.service;

import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.mention.Mention;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class NerdRestProcessQueryTest {
    NerdRestProcessQuery target;

    @Before
    public void setUp() throws Exception {
        target = new NerdRestProcessQuery();
    }

    @Test
    public void selectEntities_noOriginalEntities_shouldUseNewEntities() throws Exception {
        List<Mention> newMentions = new ArrayList<>();
        newMentions.add(new Mention("Mucca"));
        newMentions.add(new Mention("Capra"));

        final List<NerdEntity> nerdEntities = target.selectEntities(new ArrayList<>(), newMentions);

        assertThat(nerdEntities, hasSize(2));
        assertThat(nerdEntities.get(0).getRawName(), is("Mucca"));
        assertThat(nerdEntities.get(1).getRawName(), is("Capra"));
    }

    @Test
    public void selectEntities_noNewMentions_shouldUseOnlyOriginalEntities() throws Exception {
        List<NerdEntity> originalEntities = new ArrayList<>();

        originalEntities.add(new NerdEntity("Austria", 0,7));
        originalEntities.add(new NerdEntity("Russia", 10,16));
        originalEntities.add(new NerdEntity("Abc", 22,25));

        final List<NerdEntity> nerdEntities = target.selectEntities(originalEntities, new ArrayList<>());

        assertThat(nerdEntities, hasSize(3));
        assertThat(nerdEntities.get(0).getRawName(), is("Austria"));
        assertThat(nerdEntities.get(1).getRawName(), is("Russia"));
        assertThat(nerdEntities.get(2).getRawName(), is("Abc"));
    }

    @Test
    public void selectEntities_noOverlapping_shouldMergeResults() throws Exception {
        List<NerdEntity> originalEntities = new ArrayList<>();

        originalEntities.add(new NerdEntity("Austria", 0,7));
        originalEntities.add(new NerdEntity("Russia", 10,16));
        originalEntities.add(new NerdEntity("Abc", 22,25));

        List<Mention> newMentions = new ArrayList<>();
        newMentions.add(new Mention("Mucca", 30, 35));
        newMentions.add(new Mention("Capra", 40, 45));

        final List<NerdEntity> nerdEntities = target.selectEntities(originalEntities, newMentions);

        assertThat(nerdEntities, hasSize(5));
        assertThat(nerdEntities.get(0).getRawName(), is("Mucca"));
        assertThat(nerdEntities.get(1).getRawName(), is("Capra"));
        assertThat(nerdEntities.get(2).getRawName(), is("Austria"));
        assertThat(nerdEntities.get(3).getRawName(), is("Russia"));
        assertThat(nerdEntities.get(4).getRawName(), is("Abc"));
    }

    @Test
    public void selectEntities_overlapping_shouldGivePriorityToOriginalEntities() throws Exception {
        List<NerdEntity> originalEntities = new ArrayList<>();

        originalEntities.add(new NerdEntity("Austria", 0,7));
        originalEntities.add(new NerdEntity("Russia", 10,16));
        originalEntities.add(new NerdEntity("Abc", 22,25));

        List<Mention> newMentions = new ArrayList<>();
        newMentions.add(new Mention("Mucca", 9, 14));
        newMentions.add(new Mention("Capra", 40, 45));

        final List<NerdEntity> nerdEntities = target.selectEntities(originalEntities, newMentions);

        assertThat(nerdEntities, hasSize(4));
        assertThat(nerdEntities.get(0).getRawName(), is("Capra"));
        assertThat(nerdEntities.get(1).getRawName(), is("Austria"));
        assertThat(nerdEntities.get(2).getRawName(), is("Russia"));
        assertThat(nerdEntities.get(3).getRawName(), is("Abc"));
    }

    @Test
    public void selectEntities_overlapping2_shouldGivePriorityToOriginalEntities() throws Exception {
        List<NerdEntity> originalEntities = new ArrayList<>();

        originalEntities.add(new NerdEntity("Austria", 0,7));
        originalEntities.add(new NerdEntity("Russia", 10,16));
        originalEntities.add(new NerdEntity("Abc", 22,25));

        List<Mention> newMentions = new ArrayList<>();
        newMentions.add(new Mention("Mucca", 9, 14));
        newMentions.add(new Mention("Capra", 16, 21));

        final List<NerdEntity> nerdEntities = target.selectEntities(originalEntities, newMentions);

        assertThat(nerdEntities, hasSize(3));
        assertThat(nerdEntities.get(0).getRawName(), is("Austria"));
        assertThat(nerdEntities.get(1).getRawName(), is("Russia"));
        assertThat(nerdEntities.get(2).getRawName(), is("Abc"));
    }

    @Test
    public void selectEntities_overlapping3_shouldGivePriorityToOriginalEntities() throws Exception {
        List<NerdEntity> originalEntities = new ArrayList<>();

        originalEntities.add(new NerdEntity("Austria", 0,7));
        originalEntities.add(new NerdEntity("Russia", 10,16));
        originalEntities.add(new NerdEntity("Abc", 22,25));

        List<Mention> newMentions = new ArrayList<>();
        newMentions.add(new Mention("Abd", 8, 10));

        final List<NerdEntity> nerdEntities = target.selectEntities(originalEntities, newMentions);

        assertThat(nerdEntities, hasSize(3));
        assertThat(nerdEntities.get(0).getRawName(), is("Austria"));
        assertThat(nerdEntities.get(1).getRawName(), is("Russia"));
        assertThat(nerdEntities.get(2).getRawName(), is("Abc"));
    }

}