package com.scienceminer.nerd.embeddings;

import com.scienceminer.nerd.utilities.Utilities;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;

/**
 * Compute similarity score baed on logistic regression
 * 
 * @author giuseppe ottaviano (original) with modification patrice lopez
 */

// derived from https://github.com/ot/entity2vec
public class LREntityScorer extends EntityScorer {

    public LREntityScorer(LowerKnowledgeBase kb) {
        super(kb);
    }

    public class LRScorerContext extends ScorerContext {
        public LRScorerContext(float[] word_vecs, int[] word_counts) {
            super(word_vecs, word_counts);
        }

        @Override
        public float compute_score() {
            int n_words = word_counts.length;
            int word_size = kb.getEmbeddingsSize();
            float s = 0;
            for(int i = 0; i < n_words; ++i) {
                int word_count = word_counts[i];
                int word_offset = i * word_size;
                //double dotprod = entity_vec[ word_size ];
                // PL: size is word_size, not word_size+1, initialize at 0.0
                double dotprod = Utilities.inner(word_size, word_vecs, word_offset, entity_vec, 0);
                s += word_count * Math.log(1 + Math.exp(dotprod));
            }
//System.out.println("LR scorer: " + word_counts.length + " words context / " + (-s));
            return -s;
        }
    }

    @Override
    public ScorerContext create_context( float[] word_vecs, int[] word_counts ) {
        return new LRScorerContext( word_vecs, word_counts );
    }

}
