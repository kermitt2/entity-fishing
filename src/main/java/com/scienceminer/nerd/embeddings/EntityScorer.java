package com.scienceminer.nerd.embeddings;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import com.scienceminer.nerd.kb.LowerKnowledgeBase;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

// derived from https://github.com/ot/entity2vec
public abstract class EntityScorer {

    public static float DEFAULT_SCORE = -Float.MAX_VALUE;
    protected LowerKnowledgeBase kb = null;

    public EntityScorer(LowerKnowledgeBase kb) {
        this.kb = kb;
    }

    public abstract class ScorerContext {
        protected float[] word_vecs;
        protected int[] word_counts;
        protected float[] entity_vec;

        public ScorerContext(float[] word_vecs, int[] word_counts) {
            this.word_vecs = word_vecs;
            this.word_counts = word_counts;
        }

        public float score(String entity_id) {
            if (entity_id == null || word_counts.length == 0) {
                return DEFAULT_SCORE;
            }
            short[] entity_vec_tmp = kb.getEntityEmbeddings(entity_id);
            if(entity_vec_tmp != null) {
                entity_vec = new float[entity_vec_tmp.length];
                for (int i = 0; i < entity_vec_tmp.length; i++) {
                    this.entity_vec[i] = entity_vec_tmp[i];
                }
            }
//if (this.entity_vec == null)
//    System.out.println("warning: null vector for " + entity_id);
//else
//    System.out.println("getEntityEmbeddings for " + entity_id + " -> " + this.entity_vec.length);
            return compute_score();
        }

        public abstract float compute_score();
    }

    public abstract ScorerContext create_context(float[] word_vecs, int[] word_counts);

    public ScorerContext context(List<String> words) {
        Multiset<String> counter = TreeMultiset.create();
        counter.addAll(words);

        int word_dim = kb.getEmbeddingsSize();
        // word_vecs is the concatenation of all word vectors of the word list
        float[] word_vecs = new float[counter.size() * word_dim];
        IntArrayList word_counts = new IntArrayList();
        int n_words = 0;

        for(Multiset.Entry<String> entry : counter.entrySet()) {
            short[] vector = kb.getWordEmbeddings(entry.getElement());
            if (vector != null) {
                word_counts.add(entry.getCount());
                for (int i=0; i<kb.getEmbeddingsSize(); i++) {
                    word_vecs[n_words * word_dim + i] = vector[i];
                }
                n_words += 1;
            }
        }
        word_counts.trim();

        return create_context(word_vecs, word_counts.elements());
    }

    public float[] score(List<String> entities, List<String> words) {
        float[] scores = new float[entities.size()];
        ScorerContext ctx = context(words);
        for(int i = 0; i < entities.size(); ++i) {
            scores[i] = ctx.score(entities.get(i));
        }
        return scores;
    }

    public float score(String entity, List<String> words) {
        return score(Arrays.asList(entity), words)[0];
    }
}
