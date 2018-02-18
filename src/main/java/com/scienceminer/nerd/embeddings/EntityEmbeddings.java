/**
 * Originally from https://github.com/yahoo/FEL
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package com.scienceminer.nerd.embeddings;

import it.unimi.dsi.logging.ProgressLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Random;
import java.util.Map;
import java.util.TreeMap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import com.scienceminer.nerd.main.MainArgs;

/**
 * Learns entity embeddings using regularized logistic regression and negative
 * sampling (in the FEL paper rho=20)
 *
 * From the origianl FEL version, addition of standalonemainMultithread for exploiting multhreading 
 * during creation of embeddings.
 * This gives ~7 hours for producing embeddings for 4.6 millions Wikidata entities based on entity
 * descriptions corresponding to first paragraphs of English Wikipedia with 8 threads. 
 *
 * From the origianl FEL version, remove hadoop processing for simplification and replace command 
 * argument processing which was relying on a library not compatible with Apache 2 license.
 *
 * Example command:
 *
 * mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.EntityEmbeddings 
 * -Dexec.args="-i /home/lopez/nerd/data/wikipedia/training/description.en 
 * -v /mnt/data/wikipedia/embeddings/wiki.en.quantized -o /mnt/data/wikipedia/embeddings/entity.en.embeddings -n 10"
 *
 * @author roi blanco (original), with modifications patrice lopez
 */
public class EntityEmbeddings {   
    private Random r;
    private int embeddingsSize = -1;

    /**
     * Arguments of the command
     */
    private static MainArgs gbdArgs;

    static enum MyCounters {
        NUM_RECORDS, ERR
    };

    public EntityEmbeddings() {
        this.r = new Random(1234);
    }

    public int getEmbeddingsSize() {
        return embeddingsSize;
    }

    public Map<String,float[]> loadEmbeddings(String path) {
        Map<String,float[]> vectors = new TreeMap<String,float[]>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
            String line = null;
            while((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    // skip header
                    continue;
                }
                String word = parts[0];
                if (embeddingsSize != -1) {
                    if (parts.length-1 != embeddingsSize) {
                        System.out.println("Warning - vector for word '" + word + "' is not of expected size " + embeddingsSize);
                        continue;
                    }
                } else {
                    embeddingsSize = parts.length-1;
                }
                float[] vector = new float[parts.length-1];
                for(int i = 1; i < parts.length; i++) {
                    float ff = 0.0f;
                    try {
                        ff = Float.parseFloat(parts[i]);
                    } catch(Exception e) {
                        System.out.println("Warning - float cannot be parsed: " + parts[i]);
                    }
                    vector[i-1] = ff;
                }
                vectors.put(word, vector);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return vectors;
    }

    /**
     * Use this method to compute entity embeddings on a single machine. See --help
     * @param args command line arguments
     * @throws JSAPException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    static public void standalonemainMultithread(String args[]) throws ClassNotFoundException, IOException {
        gbdArgs = new MainArgs();
        if (processArgs(args)) {
            EntityEmbeddings eb = new EntityEmbeddings();
            //CompressedW2V vectors = new CompressedW2V(gbdArgs.getVectorFile());
            Map<String,float[]> vectors = eb.loadEmbeddings(gbdArgs.getVectorFile());
            final int nbThreads = Integer.parseInt(gbdArgs.getNbThreads());
            final int rho = Integer.parseInt(gbdArgs.getRho());
            final int nwords = vectors.size();

            final int maxWords = Integer.parseInt(gbdArgs.getMax()) > 0? Integer.parseInt(gbdArgs.getMax()):Integer.MAX_VALUE;
            final BufferedReader br = 
                new BufferedReader(new InputStreamReader(new FileInputStream(new File(gbdArgs.getInput()))));
            int count = 0;

            while(br.readLine() != null) count++; 
            br.close();

            ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
            for(int i=0; i<nbThreads; i++) {
                Runnable worker = new EntityCruncher(i, gbdArgs.getInput(), gbdArgs.getOutput(), vectors, rho, count, 
                    maxWords, nbThreads, eb.getEmbeddingsSize());
                executor.execute(worker);
            }
            try {
                System.out.println("wait for thread completion");
                executor.shutdown();
                executor.awaitTermination(48, TimeUnit.HOURS);
            } catch(InterruptedException e) {
                System.err.println("worker interrupted");
            } finally {
                if (!executor.isTerminated()) {
                    System.err.println("cancel all non-finished workers");
                }
                executor.shutdownNow();
            }

            // recompose result files into one
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(gbdArgs.getOutput())));
            for(int i=0; i<nbThreads; i++) {
                try (BufferedReader brr = new BufferedReader(new FileReader(gbdArgs.getOutput() +"."+i))) {
                    String text = null;
                    while ((text = brr.readLine()) != null) {
                        bw.write(text);
                        bw.newLine();
                    }
                } finally {
                    // delete the part file
                    try {
                        Path path = FileSystems.getDefault().getPath(gbdArgs.getOutput()+"."+i);
                        Files.delete(path);
                    } catch (Exception e) {
                        System.out.println("Fail to delete part file: " + gbdArgs.getOutput()+"."+i);
                    }
                }

            }
        } else {
            System.out.println("Command line error, usage: \n" + EntityEmbeddings.getHelp());
        }
    }

    public static class EntityCruncher implements Runnable {
        private final int rank;
        private final String input;
        private final String output;
        private final Map<String,float[]> vectors;
        private final int rho;
        private final int count; 
        private final int maxWords;
        private final int nbThreads;
        private final int embeddingsSize;

        EntityCruncher(int rank, String input, String output, Map<String,float[]> vectors, int rho, int count, int maxWords, 
            int nbThreads, int embeddingsSize) {
            this.rank = rank;
            this.input = input;
            this.output = output;
            this.vectors = vectors;
            this.rho = rho;
            this.count = count;
            this.maxWords = maxWords;
            this.nbThreads = nbThreads;
            this.embeddingsSize = embeddingsSize;
        }

        @Override
        public void run() {
            ProgressLogger pl = new ProgressLogger();
            final int nwords = vectors.size();
            final int d = embeddingsSize;
            try {
                pl.count = count / nbThreads;
                pl.itemsName = "entities";
                final PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output+"."+rank, false)));
                /*if (rank == 0)
                    pw.println(count + " " + d);*/

                float alpha = 10;
                EntityEmbeddings eb = new EntityEmbeddings();
                final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input)) , "UTF-8"));
                pl.start();
                String line;
                int nb = 0;
                int nbWritten = 0;
                while((line = br.readLine()) != null) {
                    if ((nb < (count/nbThreads)*rank)) {
                        nb++; 
                        continue;
                    }

                    if ((rank != nbThreads-1) && (nb > (count/nbThreads)*(rank+1)))
                        break;

                    String[] parts = line.split("\t");
                    if(parts.length > 1) {
                        TrainingExamples ex = eb.getVectors(parts[1], vectors, rho, nwords, maxWords);
                        if (ex.y.length == 0) {
                            nb++;
                            continue;
                        }
                        float[] w = eb.trainLR2(ex.x, d, ex.y, alpha);
                        pw.print(parts[0] + " ");
                        for(int i = 0; i < d; i++) {
                            pw.print(w[i] + " ");
                        }
                        pw.println();
                        pl.lightUpdate();
                        nbWritten++;
                        if (nbWritten == 1000) {
                            pw.flush();
                            nbWritten = 0;
                        }

                        for(int i = 0; i < ex.y.length; i++) {
                            if(ex.y[i] > 0) {
                                eb.scoreLR(ex.x[i], w);
                                //double v = eb.scoreLR(ex.x[i], w);
                            }
                        }
                    }
                    nb++;
                }
                br.close();
                pw.close();
                pl.stop();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Holder for ({x},y) sets
     */
    public class TrainingExamples {
        public TrainingExamples(float[][] x, int[] y) {
            this.x = x;
            this.y = y;
        }
        public float[][] x;
        public int[] y;
    }

    /**
     * Gets a set of training examples out of a chunk of text using its word embeddings as features and negative sampling
     * @param input text to extract features from
     * @param vectors word embeddings
     * @param rho number of words to sample negatively
     * @param nwords total words in the vocabulary
     * @return training examples
     */
    public TrainingExamples getVectors(String input, Map<String,float[]> vectors, int rho, int nwords, int maxWordsPerEntity) {
        String[] parts = input.split("\\s+");
        List<float[]> positive = new ArrayList<float[]>();
        List<float[]> negative = new ArrayList<float[]>();

        HashSet<String> positiveWords = new HashSet<String>();
        int tmp = 0;
        for(String s : parts) {
            float[] vectorOf = vectors.get(s);
            if (vectorOf != null) {
                positive.add(vectorOf);
                //positiveWords.add(vectors.getWordId(s));
                positiveWords.add(s);
                tmp++;
            }
            if (tmp > maxWordsPerEntity) 
                break;
        }

        int total = 0;
        if (rho < 0) 
            rho = positive.size();
        List<String> keysAsArray = new ArrayList<String>(vectors.keySet());
        while(total < rho) {
            int xx = r.nextInt(nwords);
            String xxw = keysAsArray.get(xx);
            while(positiveWords.contains(xxw)) {
                xx = r.nextInt(nwords);
                xxw = keysAsArray.get(xx);
            }
            negative.add(vectors.get(xxw));
            total++;
        }

        float[][] x = new float[positive.size() + negative.size()][embeddingsSize];
        int[] y = new int[positive.size() + negative.size()];

        for(int i = 0; i < positive.size(); i++) {
            x[i] = positive.get(i);
            y[i] = 1;
        }
        final int j = positive.size();
        for(int i = 0; i < negative.size(); i++) {
            x[i + j] = negative.get(i);
            y[i + j] = 0;
        }
        return new TrainingExamples(x, y);
    }

    /**
     * Initializes randomly the weights
     * @param N number of dimensions
     * @return a vector of N dimensions with random weights
     */
    public float[] initWeights(int N) {
        float[] w = new float[N];
        for(int i = 0; i < N; i++) {
            w[i] = r.nextFloat();
        }
        return w;
    }

    /**
     * Sigmoid score
     * @param w weights
     * @param x input
     * @return 1/(1+e^(- w * x))
     */
    public double scoreLR(float[] w, float[] x) {
        float inner = 0;
        for(int i = 0; i < w.length; i++) {
            inner += w[i] * x[i];
        }
        return 1d / (1 + Math.exp(-inner));
    }

    /**
     * Learns the weights of a L2 regularized LR algorithm
     * @param x input data
     * @param d number of dimensions
     * @param y labels
     * @param C loss-regularizer tradeoff parameter
     * @return learned weights
     */
    public float[] trainLR2(float[][] x, int d, int[] y, float C) { //m examples. dim = N
        C = C / 2;
        final int maxIter = 50000;
        double alpha = 1D;
        final int N = y.length;
        final double tolerance = 0.00001;
        float[] w = initWeights(d);
        double preLik = 100;
        boolean convergence = false;
        int iter = 0;
        while(!convergence) {
            double likelihood = 0;
            double[] currentScores = new double[N];
            float acumBias = 0;
            for(int i = 0; i < N; i++) {
                currentScores[i] = scoreLR(w, x[i]) - y[i];
                acumBias += currentScores[i] * x[i][0];
            }
            w[0] = (float) (w[0] - alpha * (1D / N) * acumBias); //bias doesn't regularize
            for(int j = 1; j < d; j++) {
                float acum = 0;
                for(int i = 0; i < N; i++) {
                    acum += currentScores[i] * x[i][j];
                }
                w[j] = (float) (w[j] - alpha * ((1D / N) * (acum + C * w[j])));

            }

            double norm = 0;
            for(int j = 0; j < d; j++) {
                norm += w[j] * w[j];
            }
            norm = (C / N) * norm;
            for(int i = 0; i < N; i++) {
                double nS = scoreLR(w, x[i]);
                if(nS > 0) {
                    double s = y[i] * Math.log(nS) + (1 - y[i]) * Math.log(1 - nS);
                    if(!Double.isNaN(s)) likelihood += s;
                }
            }
            likelihood = norm - (1 / N) * likelihood;
            iter++;
            if(iter > maxIter) convergence = true;
            else if(Math.abs(likelihood - preLik) < tolerance) convergence = true;
            if(likelihood > preLik) alpha /= 2;

            preLik = likelihood;

        }
        return w;
    }

    /**
     * @return String to display for help.
     */
    protected static String getHelp() {
        final StringBuilder help = new StringBuilder();
        help.append("Learns entity embeddings for entity-fishing\n");
        help.append("-h: displays help\n");
        help.append("-in: path to an entity description data file.\n");
        help.append("-out: path to the result entity embeddings file (not quantized nor compressed)\n");
        help.append("-n: number of threads to be used, default is 1\n");
        help.append("-rho: rho negative sampling parameters, if it's < 0 use even sampling, default is -1 (must be an integer)\n");
        help.append("-max: maximum words per entity, if < 0 use all the words, default is -1 (must be an integer)\n");
        help.append("-v: the path to the word embedding file in compressed format (e.g. one originally of word2vec, faster, lexvec, etc.)\n");
        return help.toString();
    }

    /**
     * Process command given the args.
     *
     * @param pArgs The arguments given to the batch.
     */
    protected static boolean processArgs(final String[] pArgs) {
        boolean result = true;
        if (pArgs.length == 0) {
            System.out.println(getHelp());
            result = false;
        } 
        else {
            String currArg;
            for (int i = 0; i < pArgs.length; i++) {
                currArg = pArgs[i];
                if (currArg.equals("-h") || currArg.equals("-help")) {
                    System.out.println(getHelp());
                    result = false;
                    break;
                }
                if (currArg.equals("-v") || currArg.equals("-vector") || currArg.equals("-vectors")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setVectorFile(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-i") || currArg.equals("-in") || currArg.equals("-input")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setInput(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-o") || currArg.equals("-out") || currArg.equals("-output")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setOutput(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-rho")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setRho(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-max")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setMax(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-n")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setNbThreads(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        standalonemainMultithread(args);
        System.exit(0);
    }
}
