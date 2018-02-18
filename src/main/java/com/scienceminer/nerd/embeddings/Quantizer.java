/**
 * Originally from https://github.com/yahoo/FEL
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package com.scienceminer.nerd.embeddings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import com.scienceminer.nerd.main.MainArgs;

/**
 * Quantizes a word2vec-like vector file given a quantization factor, or 
 * it will binary search for the optimum one (given a reconstruction error target)
 * 
 * From the original FEL version, replace command argument processing which was
 * relying on a library not compatible with Apache 2 license.
 * 
 * Example command:
 * mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.Quantizer 
 * -Dexec.args="-i /mnt/data/wikipedia/embeddings/wiki.en.vec -o /mnt/data/wikipedia/embeddings/wiki.en.quantized -hashheader"
 *
 * note: for instance binary search gives q = 5 for fastText English wikipedia word vectors, we can pass it directly as follow
 * mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.Quantizer 
 * -Dexec.args="-i /mnt/data/wikipedia/embeddings/wiki.en.vec -o /mnt/data/wikipedia/embeddings/wiki.en.quantized -hashheader -quantizer 5"
 *
 * The quantized embeddings will always be saved in the traditional word2vec .vec format.
 *
 * @author roi blanco (original), with modifications patrice lopez
 */
public class Quantizer {
    private static Logger LOGGER = LoggerFactory.getLogger(Quantizer.class);

    /**
     * Arguments of the command
     */
    private static MainArgs gbdArgs;

    /**
     * Inner class for holding the error and number of words
     */
    public class ErrorHolder {
        public int words;
        public double error;

        public ErrorHolder(int words, double error) {
            this.words = words;
            this.error = error;
        }
    }

    /**
     * Computes the number of bits needed to store a vector
     * @param v vector
     * @return number of bits of the compressed representation
     */
    public double golombBits(float[] v) {
        double[] nat_v = new double[v.length];
        float abs_sum = 0;
        for(int i = 0; i < v.length; i++) {
            nat_v[i] += v[i] >= 0 ? 2 * v[i] : -(2 * v[i] + 1);
            abs_sum += nat_v[i];
        }
        if(abs_sum == 0) return 0F;
        double f = ((float) v.length) / abs_sum;
        double m = Math.ceil(Math.log(2 - f) / -Math.log(1 - f));
        double b = Math.ceil(Math.log(m));
        double acc = 0;
        double ex = Math.pow(2, b);
        for(int i = 0; i < v.length; i++) {
            double vv = nat_v[i] < ex - m ? b - 1 : b;
            acc += nat_v[i] / m + 1 + vv;
        }
        return acc;
    }

    /**
     * Writes out to disk the quantized vectors
     * @param modelFile input file
     * @param outputFile output file
     * @param q quantized factor
     * @param numberOfWords number of entries in the file
     * @param hasheader whether the file has a header or not
     * @throws IOException
     */
    public void serializeW2VFormat(String modelFile, String outputFile, int q, int numberOfWords, boolean hasheader) throws
            IOException {
        LOGGER.info("Serializing quantized model to " + outputFile + " using q = " + q);
        
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(modelFile))));
        String line = null;
        if(hasheader) 
            br.readLine();

        line = br.readLine();
        int len = line.split("\\s+").length - 1;
        bw.write(numberOfWords + "\t" + len + "\t" + q);
        bw.write("\n");
        br.close();

        br = new BufferedReader(new InputStreamReader(new FileInputStream(modelFile), "UTF-8"));
        if(hasheader) br.readLine(); //skip the header
        while((line = br.readLine()) != null) {
            String[] parts = line.split("\\s+");
            bw.write(parts[0] + " ");
            for(int i = 1; i < parts.length; i++) {
                Double ff = new Double(parts[i]);
                int qa = (int) ((int) (Math.abs(ff) * q) * Math.signum(ff));
                bw.write(qa + " ");
            }
            bw.write("\n");
        }
        br.close();
        bw.close();
    }

    /**
     * To avoid storing everything in memory, we read the input file each time
     *
     * @param modelFile original vector file
     * @param q quantization factor
     * @throws IOException
     * @return number of words and reconstruction error
     */
    public ErrorHolder quantizeArray(String modelFile, int q) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(modelFile ))));
        String line = null;
        double error = 0;
        int items = 0;
        while((line = br.readLine()) != null) {
            String[] parts = line.split("\\s+");
            double norm = 0;
            items++;
            double wordError = 0;
            double v[] = new double[parts.length];
            for(int i = 1; i < parts.length; i++) {
                v[i] = new Double(parts[i]);
                norm += v[i] * v[i];
            }
            norm = Math.sqrt(norm);
            for(int i = 1; i < parts.length; i++) {
                int qa = (int) ((int) (Math.abs(v[i]) * q) * Math.signum(v[i]));
                double dqa = (qa + 0.5 * Math.signum(qa)) / q;
                wordError += (v[i] - dqa) * (v[i] - dqa);
            }
            error += Math.sqrt(wordError) / norm;
        }
        br.close();
        return new ErrorHolder(items - 1, error / items);
    }

    /**
     * Quantizes the word vectors without attempting to look for the optimal error-rate quantizer
     *
     * @param inputFile initial file to quantize
     * @param outputFile output file name
     * @param q quantization factor
     * @param hasheader whether the original file has a header or not
     * @throws IOException
     */
    public void quantizeSinglePass(String inputFile, String outputFile, int q, boolean hasheader) throws IOException {
        serializeW2VFormat(inputFile, outputFile, q, countWords(inputFile, hasheader), hasheader);
    }

    /**
     * @param inputfile vector file
     * @param hasHeader whether the input file has a header or not
     * @return number of words in the original vector file
     * @throws IOException
     */
    public int countWords(String inputfile, boolean hasHeader) throws IOException {
        int items = 0;
        String line = null;
        final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputfile))));
        if(hasHeader) br.readLine();
        while((line = br.readLine()) != null) {
            items++;
        }
        System.out.println("Found ["+items+"] words ");
        return items;
    }

    /**
     * You have two losses - number of bits used to compress - reconstruction
     * error (after quantizing) The standard mode is to specify a target
     * reconstruction error and stop there. The other option is to select a
     * tradeoff between the two losses and optimize
     *
     * @param inputFile original vector file
     * @param outputFile output file
     * @param hasheader whether the input file has a header or not
     * @throws IOException
     */
    public void quantize(String inputFile, String outputFile, double targetError, boolean hasheader) throws IOException {
        int low = 1;
        int high = 128;
        int bestQ = 0;
        int nWords = 0;

        //if you want rice coding you could start from 1 to 128 and increment by *2 each time (and stop when the
        // condition is met)
        while(high - low > 1) {
            bestQ = (high + low) / 2;
            ErrorHolder err = quantizeArray(inputFile, bestQ);
            nWords = err.words;
            LOGGER.info("Binary search: q=" + bestQ + " err= " + err.error);
            if(err.error > targetError) {
                low = bestQ;
            } else {
                high = bestQ;
            }
        }
        serializeW2VFormat(inputFile,outputFile,bestQ, nWords, hasheader);
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
                if (currArg.equals("-q") | currArg.equals("-quantizer")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setQuantizer(pArgs[i + 1]);
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
                if (currArg.equals("-error")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setError(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-hashheader") || currArg.equals("-header")) {
                    gbdArgs.setHashheader(true);
                    continue;
                }
            }
        }
        return result;
    }

    /**
     * @return String to display for help.
     */
    public static String getHelp() {
        final StringBuilder help = new StringBuilder();
        help.append("Quantize embeddings for entity-fishing\n");
        help.append("-h: displays help\n");
        help.append("-in: path to an input vector data file.\n");
        help.append("-out: path to the result quantized data files\n");
        help.append("-error: error rate for quantizing, default is 0.01 (must be a double)\n");
        help.append("-quantizer: quantizer value to avoid binary search (must be an integer)\n");
        help.append("-hashheader: flag to indicate if the data file include a header (that will be skipped)\n");
        return help.toString();
    }

    /**
     * Main class for quantizing word vectors
     * @param args command line arguments (see --help)
     * @throws IOException
     */
    public static void main(String args[]) throws IOException {
        gbdArgs = new MainArgs();
        if (processArgs(args)) {
            Quantizer q = new Quantizer();
            if(gbdArgs.getQuantizer() != null) {
                System.out.println("Using as a quantizer " + gbdArgs.getQuantizer() + " (won't attempt to search for a better one) ");
                q.quantizeSinglePass(gbdArgs.getInput(), gbdArgs.getOutput(), Integer.parseInt(gbdArgs.getQuantizer()), 
                    gbdArgs.getHashheader());
            } else {
                q.quantize(gbdArgs.getInput(), gbdArgs.getOutput(), 
                    Double.parseDouble(gbdArgs.getError()), gbdArgs.getHashheader());
            }
        } else {
            System.out.println("Command line error, usage: \n" + Quantizer.getHelp());
        }
    }

}