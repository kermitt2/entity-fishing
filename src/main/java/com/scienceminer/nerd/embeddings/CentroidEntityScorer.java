package com.scienceminer.nerd.embeddings;

import com.scienceminer.nerd.utilities.Utilities;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;

/**
 * Compute similarity score using centroid 
 * 
 * @author giuseppe ottaviano (original) with modification patrice lopez
 */
// derived from https://github.com/ot/entity2vec
public class CentroidEntityScorer extends EntityScorer {

    public CentroidEntityScorer(LowerKnowledgeBase kb) {
        super(kb);
    }

    public class CentroidScorerContext extends ScorerContext {
        float[] centroid_vec;
        float norm;

        public CentroidScorerContext(float[] word_vecs, int[] word_counts) {
            super(word_vecs, word_counts);
            // compute context centroid
            int word_size = kb.getEmbeddingsSize();
            int n_words = word_counts.length;
            centroid_vec = new float[ word_size ];
            for(int i = 0; i < n_words; ++i) {
                int word_count = word_counts[ i ];
                for(int j = 0; j < word_size; ++j) {
                    centroid_vec[ j ] += word_count * word_vecs[ i * word_size + j ];
                }
            }

            norm = Utilities.inner(centroid_vec.length, centroid_vec, 0, centroid_vec, 0);
            norm = (float) Math.sqrt(norm);
        }

        @Override
        public float compute_score() {
            if (entity_vec == null)
                return 0.0f;
            int word_size = centroid_vec.length;
            float score = Utilities.inner(word_size, entity_vec, 0, centroid_vec, 0) / norm;
//System.out.println("centroid scorer: " + word_counts.length + " words context / " + score);
            return score;
        }
    }

    @Override
    public ScorerContext create_context(float[] word_vecs, int[] word_counts) {
        return new CentroidScorerContext(word_vecs, word_counts);
    }

}
