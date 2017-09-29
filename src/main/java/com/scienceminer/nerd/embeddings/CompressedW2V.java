/**
 * Originally from https://github.com/yahoo/FEL
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package com.scienceminer.nerd.embeddings;

import it.cnr.isti.hpc.LREntityScorer;
import it.cnr.isti.hpc.CentroidEntityScorer;
import it.cnr.isti.hpc.Word2VecCompress;
import it.unimi.dsi.fastutil.io.BinIO;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * This class is a wrapper forwarding everything to it.cnr.isti.hpc.Word2VecCompress. 
 * It also provides a command line tool for computing entity/word similarity.
 *
 * Example command for interactive word and entity similarities calculation:
 * mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.CompressedW2V 
 * -Dexec.args="/mnt/data/wikipedia/embeddings/entity.en.embeddings.quantized.compressed 
 * /mnt/data/wikipedia/embeddings/wiki.en.quantized.compressed"
 *
 *
 * @author roi blanco (original), with modification patrice lopez
 */
public class CompressedW2V implements Serializable {

    private static final long serialVersionUID = 1L;
    private Word2VecCompress vec;
    public int N;

    public CompressedW2V(String fileName) throws ClassNotFoundException, IOException {
        this.vec = (Word2VecCompress) BinIO.loadObject(fileName);
        N = vec.getInt(1).length;
    }

    public CompressedW2V(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        final Object result = ois.readObject();
        ois.close();
        this.vec = (Word2VecCompress) result;
        this.N = vec.getInt(1).length;
    }

    /**
     * Returns the vector for the word with the given id
     * @param word id of the word
     * @return word vector or null if not found
     */
    public float[] get(long word) {
        return vec.get(word);
    }

    /**
     * @return number of dimensions of the vectors
     */
    public int getSize() {
        return vec.size();
    }

    public float[] getVectorOf(String word) {
        return vec.get(word);
    }

    /**
     * Gets the id of a word string
     * @param word input word
     * @return long id of word or null if it doesn't exist in the vocabulary
     */
    public Long getWordId(String word) {
        return vec.word_id(word);
    }

    public int getVectorLength() {
        return N;
    }

    public Word2VecCompress getWord2VecCompress() {
        return vec;
    }

    /**
     * Command line tool to compute word and entity similarities
     * @param args arguments [entityVectors] [wordVectors]
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String args[]) throws IOException, ClassNotFoundException {
        //CentroidEntityScorer scorer = new CentroidEntityScorer((Word2VecCompress) BinIO.loadObject(args[0]), (Word2VecCompress) BinIO.loadObject(args[1]));
        Word2VecCompress wv = (Word2VecCompress) BinIO.loadObject(args[1]);
        System.out.println("word vectors loaded - dimention is " + wv.dimensions());

        CompressedW2V vectors = new CompressedW2V(args[0]);
        System.out.println("entity vectors loaded - dimention is " + vectors.getVectorLength());

        LREntityScorer scorer1 = new LREntityScorer(wv, vectors.getWord2VecCompress());
        CentroidEntityScorer scorer2 = new CentroidEntityScorer(wv, vectors.getWord2VecCompress());

        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String q;
        for(; ;) {
            System.out.print(">");
            q = br.readLine();
            if(q == null) {
                System.err.println();
                break; // CTRL-D
            }
            if(q.length() == 0) continue;
            String[] strings = q.split("/");
            List<String> w = Arrays.asList(strings[0].split(" "));
            float sim1 = scorer1.score(strings[1], w);
            float sim2 = scorer2.score(strings[1], w);
            float[] entity = vectors.getVectorOf(strings[1]);
            if(entity != null) {
                System.out.println(" sim LR ([ " + strings[ 0 ] + "] , [" + strings[ 1 ] + "]) = " + sim1);
                System.out.println(" sim Centroid ([ " + strings[ 0 ] + "] , [" + strings[ 1 ] + "]) = " + sim2);
            } else {
                System.out.println("entity " + strings[ 1 ] + " not found");
            }
        }
    }
}
