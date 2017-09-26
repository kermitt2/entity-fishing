/**
 * Copyright 2015 Giuseppe Ottaviano <giuott@gmail.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package it.cnr.isti.hpc;

/**
 * Compute similarity score baed on logistic regression
 * 
 * @author giuseppe ottaviano (orginal) with modification patrice lopez
 */

////from https://github.com/ot/entity2vec
public class LREntityScorer extends EntityScorer {

    public LREntityScorer( Word2VecCompress word_model, Word2VecCompress entity_model ) {
        super( word_model, entity_model );
        // PL: the +1 bellow does not make sense if the entity embeddings are computed from the word embeddings
        //if( entity_model.dimensions() != word_model.dimensions() + 1 ) {
        if( entity_model.dimensions() != word_model.dimensions() ) {    
            throw new IllegalArgumentException( "Word and entity models have incompatible vector dimensions" );
        }
    }

    public class LRScorerContext extends ScorerContext {
        public LRScorerContext( float[] word_vecs, int[] word_counts ) {
            super( word_vecs, word_counts );
        }

        @Override
        public float compute_score() {
            int n_words = word_counts.length;
            int word_size = word_model.dimensions();
            float s = 0;
            for( int i = 0; i < n_words; ++i ) {
                int word_count = word_counts[ i ];
                int word_offset = i * word_size;
                //double dotprod = entity_vec[ word_size ];
                // PL: size is word_size, not word_size+1, initialize at 0.0
                double dotprod = LinearAlgebra.inner( word_size, word_vecs, word_offset, entity_vec, 0 );
                s += word_count * Math.log( 1 + Math.exp( dotprod ) );
            }

            return -s;
        }
    }

    @Override
    public ScorerContext create_context( float[] word_vecs, int[] word_counts ) {
        return new LRScorerContext( word_vecs, word_counts );
    }

}
