package com.scienceminer.nerd.mention;

import com.johnsnowlabs.nlp.DocumentAssembler;
import com.johnsnowlabs.nlp.Finisher;
import com.johnsnowlabs.nlp.annotators.LemmatizerModel;
import com.johnsnowlabs.nlp.annotators.Tokenizer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

import scala.collection.Iterator;
import scala.collection.mutable.WrappedArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class providing lemmatization on text or pre-tokenized text. 
 * Lemmatization is only used for some languages with rich morphology, for which the Wikipedia
 * labels do not provide sufficient coverage.
 * 
 * By default Spark NLP lemmatizers are used as implementation with lazy loading of the 
 * language-specific models. 
 * 
 **/
class Lemmatizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Lemmatizer.class);

    private static volatile Lemmatizer instance;

    private Map<String,LemmatizerModel> models = null;

    private static SparkSession spark;
    private static String modelFolder = "data/lemmatizers";

    public static Lemmatizer getInstance() {
        if (instance == null) {
            synchronized (Lemmatizer.class) {
                if (instance == null) {
                    getNewInstance();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        LOGGER.debug("Get new instance of Lemmatizer");
        instance = new Lemmatizer();
    }

    /**
     * Hidden constructor
     */
    private Lemmatizer() {
        spark = SparkSession.builder()
                .appName("entity-fishing lemmatizers")
                .config("spark.master", "local")
                .config("spark.jsl.settings.pretrained.cache_folder", modelFolder)
                .getOrCreate();
        models = new TreeMap<>();
    }


    /**
     *  Lemmatization of a list of sentences
     **/
    public List<List<String>> lemmatize(List<String> inputs, String lang) {
        if (inputs == null || inputs.size() == 0 || lang == null)
            return null;

        LemmatizerModel lemmatizer = models.get(lang);
        if (lemmatizer == null) {
            try {
                lemmatizer = this.loadModel(lang);
            } catch(IOException e) {
                LOGGER.warn("Could not load lemmatizer model for " + lang);
            }
        }

        if (lemmatizer == null) {
            LOGGER.info("Lemmatizer model for " + lang + " not found, so no lemmatization applied for this language");
            return null;
        }

        DocumentAssembler documentAssembler = new DocumentAssembler();
        documentAssembler.setInputCol("text");
        documentAssembler.setOutputCol("sentence");
        documentAssembler.setCleanupMode("disabled");

        Tokenizer tokenizer = new Tokenizer();
        tokenizer.setInputCols(new String[]{"sentence"});
        tokenizer.setOutputCol("token");
        
        Finisher finisher = new Finisher();
        finisher.setInputCols(new String[]{"lemma"});

        Pipeline pipeline = new Pipeline();
        pipeline.setStages(new PipelineStage[]{documentAssembler, tokenizer, lemmatizer, finisher});

        Dataset<Row> data = spark.createDataset(inputs, Encoders.STRING()).toDF("text");
        PipelineModel pipelineModel = pipeline.fit(data);
        Dataset<Row> transformed = pipelineModel.transform(data);

        transformed.selectExpr("explode(finished_lemma)");
        List<Row> rows = transformed.selectExpr("finished_lemma").collectAsList();
        List<List<String>> results = new ArrayList<>();
        for (Row row : rows) {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < row.length(); i++) {
                WrappedArray array = (WrappedArray) row.get(i);
                Iterator iterator = array.iterator();
                while (iterator.hasNext()) {
                    String next = (String) iterator.next();
                    result.add(next);
                }
            }
            results.add(result);
        }

        return results;
    }

    /**
     * Lemmatize a pre-tokenized list of sentences 
     **/
    public List<List<String>> lemmatizeTokens(List<List<String>> tokens, String lang) throws IOException {
        if (tokens == null || tokens.size() == 0) {
            return tokens;
        }

        // perform a lemmatization on text, then re-aligned to tokens


        return tokens;
    }

    private LemmatizerModel loadModel(String lang) throws IOException  {
        LemmatizerModel lemmatizer = null;

        String modelPathString = modelFolder + File.separator + lang;
        Path modelPath = Paths.get(modelPathString);
        if (Files.exists(modelPath)) {
            LOGGER.info("loading local lemmatizer model for " + lang);
            lemmatizer = (LemmatizerModel) LemmatizerModel.load(modelFolder + File.separator + lang);

            // all the Spark NLP lemmatizer considered in entity-fishing have the same input/output profile
            lemmatizer.setInputCols(new String[]{"sentence", "token"});
            lemmatizer.setOutputCol("lemma");
            models.put(lang, lemmatizer);
        }

        return lemmatizer;
    }

}