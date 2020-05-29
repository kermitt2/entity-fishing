package com.scienceminer.nerd.utilities;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import org.grobid.core.utilities.UnicodeUtil;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.exceptions.*;

/**
 * Utility for generating Inverse Document Frequency for terms based on Wikipedia articles
 * for a given language. The statistics are already present in the KB, so it's just a dump
 * and very fast. We can restrict to the N most frequent terms, to avoid super long 
 * useless tail. Term is here viewed as Label (text which can be an anchor or not). So we're 
 * not just generating idf for general vocabulary of single words, but also for phrases
 * which can be used as labels, titles or redirections. 
 *
 * Example: Command for creating the IDF for 1000 most frequent English terms:
 * mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.utilities.WikipediaLabelIDF 
 * -Dexec.args="en 1000 /home/lopez/test/idf.en.txt"
 */
public class WikipediaLabelIDF {
    public String lang = null;
    public File outputPath = null;
    public int nbTerms = -1;

    public WikipediaLabelIDF(String lang, int nbTerms, File outputFile) {
        this.lang = lang;
        this.outputPath = outputFile;
        this.nbTerms = nbTerms;
    }

    public void process() throws NerdResourceException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outputPath));
        } catch(Exception e) {
            throw new NerdException("Opening output file failed, path or right might be invalid.", e);
        }
        UpperKnowledgeBase upperKB = null;
        try {
            upperKB = UpperKnowledgeBase.getInstance();
            upperKB.loadReverseStatementDatabase(false);
        }
        catch(Exception e) {
            throw new NerdResourceException("Error instanciating the upper knowledge base. ", e);
        }
        LowerKnowledgeBase wikipedia = upperKB.getWikipediaConf(lang);
        if (wikipedia == null) {
            throw new NerdResourceException(
                "Error instanciating the lower knowledge base. Mostlikely the language is not supported or not loaded: " + lang);
        }

        Comparator<Pair<Label,Long>> comp = (Pair<Label,Long> a, Pair<Label,Long> b) -> {
            return b.getRight().compareTo(a.getRight());
        };

        // if nbTerms is -1, it is unspecified and we output all the term IDF
        List<Pair<Label,Long>> terms = new ArrayList<Pair<Label,Long>>();
        long lowestFrequency = 0;
        if (nbTerms > 0) {
            // first pass to gather the nbTerms most frequent terms
            try {
                LabelIterator ite = wikipedia.getLabelIterator();
                while(ite.hasNext()) {
                    Label label = ite.next();
                    long freq = label.getOccCount();

                    if ( (freq > lowestFrequency) || terms.size() < nbTerms) {
                        if (terms.size() == nbTerms) {
                            // remove the last one
                            terms.remove(terms.size() -1);
                        }
                        // add new element
                        terms.add(Pair.of(label, new Long(freq)));
                        
                        // sort
                        Collections.sort(terms, comp);

                        // update 
                        lowestFrequency = terms.get(terms.size()-1).getRight();
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("terms.size(): " + terms.size());
        // total number of articles/documents
        int nbArticles = wikipedia.getArticleCount();
        try {
            // if nbTerms is -1, it is unspecified and we output all the term IDF
            if (nbTerms == -1) {
                LabelIterator ite = wikipedia.getLabelIterator();
                while(ite.hasNext()) {
                    Label label = ite.next();
                    double idf = (double) label.getDocCount() / nbArticles;
                    writer.write(label.getText() + "\t" + idf + "\n");
                    //writer.flush();
                }
            } else {
                for(Pair<Label,Long> term : terms) {
                    Label label = term.getLeft();
                    double idf = (double) label.getDocCount() / nbArticles;

                    writer.write(label.getText() + "\t" + idf + "\n");
                    //writer.flush();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch(Exception e) {
                throw new NerdException("Something went wrong when closing the output file.", e);
            }
            wikipedia.close();
        }
    }


    public static void main(String args[]) throws Exception {
        //System.out.println(args.length);
        if (args.length < 1) {
            System.out.println("usage: command lang nb_terms outputPath");
            // e.g. all software names: 
            /** 
            mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.utilities.WikipediaLabelIDF -Dexec.args="en 1000 /home/lopez/test/idf.en.txt" 
            */
            System.exit(-1);
        }

        String lang = args[0];
        List<String> languages = Arrays.asList("en", "fr", "de", "es", "it");;
        if (!languages.contains(lang)) {
            System.out.println("unsupported language, must be one of " + languages.toString());
            System.exit(-1);
        }

        int nb_terms = -1;
        if (args.length > 2) {
            try {
                nb_terms = Integer.parseInt(args[1]);
            } catch(Exception e) {
                System.err.println("Invalid number of terms: " + args[1] + " - must be an integer");
                System.exit(-1);
            }
            if (nb_terms == 0) {
                System.out.println("Number of terms is 0, nothing to do...");
                System.exit(-1);
            }
        }

        File outputFile = new File(args[args.length-1]);
        File dataDir = outputFile.getParentFile();
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            System.err.println("Invalid output path directory: " + dataDir.getPath());
            System.exit(-1);
        }
        WikipediaLabelIDF voc = new WikipediaLabelIDF(lang, nb_terms, outputFile);
        voc.process();
    }
}