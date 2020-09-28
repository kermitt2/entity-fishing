package com.scienceminer.nerd.training;

import org.grobid.core.engines.EngineParsers;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.trainer.evaluation.EvaluationUtilities;
import org.grobid.core.engines.tagging.GenericTagger;
import org.grobid.trainer.*;
import org.apache.commons.io.FileUtils;

import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.training.*;
import com.scienceminer.nerd.disambiguation.*;
import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.features.FeaturesVectorDeepType;
import com.scienceminer.nerd.utilities.mediaWiki.MediaWikiParser;

import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lang.Language;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.engines.AbstractParser;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.impl.CntManagerFactory;
import org.grobid.core.lexicon.FastMatcher;
import org.grobid.core.utilities.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import static org.apache.commons.lang3.StringUtils.*;
//import org.apache.commons.lang3.tuple.Pair;

import com.scienceminer.nerd.utilities.NerdConfig;

/**
 * Training of the software entity recognition model
 *
 * @author Patrice
 */
public class TypeSequenceLabelingTrainer extends AbstractTrainer {
    private static final Logger logger = LoggerFactory.getLogger(TypeSequenceLabelingTrainer.class);

    private LowerKnowledgeBase wikipedia = null;

    private String lang = null;

    // the articles used for training
    private ArticleTrainingSample articles = null; 

    public TypeSequenceLabelingTrainer() {
        this(0.00001, 20, 0);

        epsilon = 0.00001;
        window = 30;
        nbMaxIterations = 1500;
    }

    public void setArticles(ArticleTrainingSample theArticles) {
        this.articles = theArticles;
    }

    public void setWikipedia(LowerKnowledgeBase wiki) {
        this.wikipedia = wiki;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public TypeSequenceLabelingTrainer(double epsilon, int window, int nbMaxIterations) {
        super(GrobidModels.DEEPTYPE);

        // adjusting CRF training parameters for this model
        this.epsilon = epsilon;
        this.window = window;
        //this.nbMaxIterations = nbMaxIterations;
        this.nbMaxIterations = 2000;
    }

    /**
     * Add the selected features to the model training for software entities. For grobid-software, 
     * we can have two types of training files: XML/TEI files where text content is annotated with
     * software entities, and PDF files where the entities are annotated with an additional
     * PDf layer. The two types of training files suppose two different process in order to
     * generate the CRF training file.
     */
    @Override
    public int createCRFPPData(File sourcePathLabel,
                               File outputPath) {
        return createCRFPPData(sourcePathLabel, outputPath, null, 1.0);
    }

    /**
     * Add the selected features to the model training for software entities. Split
     * automatically all available labeled data into training and evaluation data
     * according to a given split ratio.
     */
    @Override
    public int createCRFPPData(final File corpusDir,
                               final File trainingOutputPath,
                               final File evalOutputPath,
                               double splitRatio) {
        // create preliminary files (token and labels)
        File trainBaseFile = new File("data/wikipedia/training/"+lang+"/labeler.train");
        File evalBaseFile = new File("data/wikipedia/training/"+lang+"/labeler.eval");;

        int nb_articles = this.createBaseData(trainBaseFile, evalBaseFile, splitRatio);

        // now create usual training files by injecting features
        int totalExamples = 0;
        try {
            if (trainingOutputPath != null)
                System.out.println("outputPath for training data: " + trainingOutputPath);
            if (evalOutputPath != null)
                System.out.println("outputPath for evaluation data: " + evalOutputPath);

            // the file for writing the training data
            OutputStream os2 = null;
            Writer writer2 = null;
            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                writer2 = new OutputStreamWriter(os2, "UTF8");
            }

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(trainBaseFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() == 0)
                        writer2.write("\n");
                    else {
                        String[] pieces = line.split("\t");
                        String token = pieces[0];
                        String label = pieces[1];
                        writer2.write(TypeSequenceLabeling.addFeatures(token, label));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null)
                    reader.close();
                if (writer2 != null)
                    writer2.close();
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;
            Writer writer3 = null;
            if (evalOutputPath != null) {
                os3 = new FileOutputStream(evalOutputPath);
                writer3 = new OutputStreamWriter(os3, "UTF8");
            }

            reader = null;
            try {
                reader = new BufferedReader(new FileReader(evalBaseFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() == 0)
                        writer2.write("\n");
                    else {
                        String[] pieces = line.split("\t");
                        String token = pieces[0];
                        String label = pieces[1];
                        writer2.write(TypeSequenceLabeling.addFeatures(token, label));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null)
                    reader.close();
                if (writer3 != null)
                    writer3.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }

        return nb_articles;
    }

    /**
     * Add the selected features to the model training for software entities. Split
     * automatically all available labeled data into training and evaluation data
     * according to a given split ratio.
     */
    public int createBaseData(final File trainingOutputPath,
                              final File evalOutputPath,
                              double splitRatio) {
        if ( (articles.getSample() == null) || (articles.getSample().size() == 0) )
            return 0;

        int nbArticle = 0;
        try {
            // the file for writing the training data
            OutputStream os2 = null;
            Writer writer2 = null;
            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                writer2 = new OutputStreamWriter(os2, "UTF8");
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;
            Writer writer3 = null;
            if (evalOutputPath != null) {
                os3 = new FileOutputStream(evalOutputPath);
                writer3 = new OutputStreamWriter(os3, "UTF8");
            }
            
            for (Article article : articles.getSample()) {
                System.out.println("Training on " + (nbArticle+1) + " / " + articles.getSample().size());
                StringBuilder trainingBuilder = new StringBuilder();
                if (article instanceof CorpusArticle)
                    trainingBuilder = createTrainingCorpusArticle(article, trainingBuilder);
                else
                    trainingBuilder = createTrainingWikipediaArticle(article, trainingBuilder);  
                
                if ((writer2 == null) && (writer3 != null)) {
                    writer3.write(trainingBuilder.toString());
                    writer3.write("\n");
                } else if ((writer2 != null) && (writer3 == null)) {
                    writer2.write(trainingBuilder.toString());
                    writer2.write("\n");
                } else {
                    if (Math.random() <= splitRatio) {
                        writer2.write(trainingBuilder.toString());
                        writer2.write("\n");
                    } else {
                        writer3.write(trainingBuilder.toString());
                        writer3.write("\n");
                    }
                }
                nbArticle++;
            }

            if (writer2 != null) {
                writer2.close();
                os2.close();
            }

            if (writer3 != null) {
                writer3.close();
                os3.close();
            }
        } catch (Exception e) {
            throw new NerdException("An exception occured while compiling training data from Wikipedia.", e);
        }

        return nbArticle;
    }

    /**
     *  Ensure a ratio between positive and negative examples and shuffle
     */
    private List<Pair<String, String>> subSample(List<Pair<String, String>> labeled, double targetRatio) {
        int nbPositionTokens = 0;
        int nbNegativeTokens = 0;

        List<Pair<String, String>> reSampled = new ArrayList<Pair<String, String>>();
        List<Pair<String, String>> newSampled = new ArrayList<Pair<String, String>>();
        
        boolean hasLabels = false;
        for (Pair<String, String> tagPair : labeled) {
            if (tagPair.getB() == null) {
                // new sequence
                if (hasLabels) {
                    reSampled.addAll(newSampled);
                    reSampled.add(tagPair);
                } 
                newSampled = new ArrayList<Pair<String, String>>();
                hasLabels = false;
            } else {
                newSampled.add(tagPair);
                if (!tagPair.getB().equals("<other>") && !tagPair.getB().equals("other") && !tagPair.getB().equals("O")) 
                    hasLabels = true;
            }
        }
        return reSampled;
    }

    private StringBuilder createTrainingWikipediaArticle(Article article, StringBuilder trainingBuilder) throws Exception {
        List<NerdEntity> refs = new ArrayList<NerdEntity>();
        String lang = this.wikipedia.getConfig().getLangCode();

        String content = MediaWikiParser.getInstance().
            toTextWithInternalLinksArticlesOnly(article.getFullWikiText(), lang);
        content = content.replace("''", "");
        StringBuilder contentText = new StringBuilder(); 

        Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]"); 
        Matcher linkMatcher = linkPattern.matcher(content);

        SentenceUtilities sentenceUtilities = SentenceUtilities.getInstance();

        // gather reference gold values
        int head = 0;
        int nbInstance = 0;
        while (linkMatcher.find()) {            
            String link = content.substring(linkMatcher.start()+2, linkMatcher.end()-2);
            if (head != linkMatcher.start())
                contentText.append(content.substring(head, linkMatcher.start()));
            String labelText = link;
            String destText = link;

            int pos = link.lastIndexOf('|');
            if (pos > 0) {
                destText = link.substring(0, pos);
                // possible anchor #
                int pos2 = destText.indexOf('#');
                if (pos2 != -1) {
                    destText = destText.substring(0,pos2);
                }
                labelText = link.substring(pos+1);
            } else {
                // labelText and destText are the same, but we could have an anchor #
                int pos2 = link.indexOf('#');
                if (pos2 != -1) {
                    destText = link.substring(0,pos2);
                } else {
                    destText = link;
                }
                labelText = destText;
            }
            contentText.append(labelText);

            head = linkMatcher.end();
            
            Label label = new Label(wikipedia.getEnvironment(), labelText);
            Label.Sense[] senses = label.getSenses();
            if (destText.length() > 1)
                destText = Character.toUpperCase(destText.charAt(0)) + destText.substring(1);
            else {
                // no article considered as single letter
                continue;
            }
            Article dest = wikipedia.getArticleByTitle(destText);
            if ((dest != null) && (senses.length > 1)) {
                NerdEntity ref = new NerdEntity();
                ref.setRawName(labelText);
                ref.setWikipediaExternalRef(dest.getId());
                ref.setWikidataId(wikipedia.getWikidataId(dest.getId()));
                ref.setOffsetStart(contentText.length()-labelText.length());
                ref.setOffsetEnd(contentText.length());
                ref.setDeepType(UpperKnowledgeBase.getInstance().getDeepType(ref.getWikidataId()));
                refs.add(ref);
//System.out.println(link + ", " + labelText + ", " + destText + " / " + ref.getOffsetStart() + " " + ref.getOffsetEnd());
            }
        }

        System.out.println("number of entities: " + refs.size());        

        contentText.append(content.substring(head));
        String contentString = contentText.toString();

        // segment into sentences, we will keep only sentences with some labels
        List<OffsetPosition> sentencesPositions = sentenceUtilities.runSentenceDetection(contentString);

        for(OffsetPosition sentencePosition : sentencesPositions) {                        
            // do we have entity in this sentence?
            boolean hasEntity = false;
            for(int i=0; i<refs.size(); i++) {
                NerdEntity entity = refs.get(i);
                int entityStart = entity.getOffsetStart();
                int entityEnd = entity.getOffsetEnd();

                if (sentencePosition.start <= entityStart && entityEnd <= sentencePosition.end) {
                    String localType = entity.getDeepType();
                    if (localType == null || localType.equals("unknown"))
                        continue;
                    hasEntity = true;
//System.out.println(contentString.substring(sentencePosition.start, sentencePosition.end));
//System.out.println("entity found: " + contentString.substring(entityStart, entityEnd));
                    break;
                }
            }

            if (!hasEntity) 
                continue;

            String sentence = contentString.substring(sentencePosition.start, sentencePosition.end);
            List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(sentence, new Language(lang, 1.0));

            int offsetPos = 0;
            int entityIndex = 0;
            String previousLabel = "<other>";
            for(LayoutToken token : tokens) {
                String text = token.getText();
                if (text.trim().length() == 0 ||
                    text.equals("\n") ||
                    text.equals("\r") ||
                    text.equals("\t")) {
                    offsetPos += text.length();
                    continue;
                }

                int offsetStart = offsetPos;
                int offsetEnd = offsetPos+text.length();

                // default label
                String typeLabel = "<other>";

                // check entity index
                for(int i=0; i<refs.size(); i++) {
                    NerdEntity entity = refs.get(i);
                    if (sentencePosition.start <= entity.getOffsetStart() && entity.getOffsetEnd() <= sentencePosition.end) {
                        int entityStart = entity.getOffsetStart() - sentencePosition.start;
                        int entityEnd = entity.getOffsetEnd() - sentencePosition.start;

                        //if (entityStart > sentence.length())
                        //    continue;

    //                    if (entityStart<=offsetStart && offsetEnd<=entityEnd) {
                        if (sentence.substring(entityStart, entityEnd).equals(text)) {
//System.out.println("entity found: " + contentString.substring(entity.getOffsetStart(), entity.getOffsetEnd()) + " // " + entity.getDeepType());

                            String localType = entity.getDeepType();
                            if (localType == null || localType.equals("unknown"))
                                localType = "other";
                            typeLabel = "<" + localType + ">";
                            nbInstance++;
                            break;
                        }
                    }

                    /*if (offsetEnd < entityStart)
                        break;*/
                }

                trainingBuilder.append(text);
                trainingBuilder.append("\t");

                if (typeLabel.equals("<other>"))
                    trainingBuilder.append(typeLabel+"\n");
                else if (!typeLabel.equals(previousLabel))
                    trainingBuilder.append("I-"+typeLabel+"\n");
                else
                    trainingBuilder.append(typeLabel+"\n");
                previousLabel = typeLabel;
                offsetPos += text.length();
            }

            trainingBuilder.append("\n");
        }

        System.out.println("article contribution: " + nbInstance + " training instances");
        return trainingBuilder;
    }


    private StringBuilder createTrainingCorpusArticle(Article article, StringBuilder trainingBuilder) throws Exception {



        return trainingBuilder;
    }

    /**
     * Standard evaluation via the the usual Grobid evaluation framework.
     */
    @Override
    public String evaluate() {
        return evaluate(false);
    }

    @Override
    public String evaluate(boolean includeRawResults) {
        File evalDataF = GrobidProperties.getInstance().getEvalCorpusPath(
                new File(new File("resources").getAbsolutePath()), model);

        File tmpEvalPath = getTempEvaluationDataPath();
        createCRFPPData(evalDataF, tmpEvalPath);

        return EvaluationUtilities.evaluateStandard(tmpEvalPath.getAbsolutePath(), getTagger()).toString(includeRawResults);
    }

    @Override
    public String evaluate(GenericTagger tagger, boolean includeRawResults) {
        File evalDataF = GrobidProperties.getInstance().getEvalCorpusPath(
                new File(new File("resources").getAbsolutePath()), model);

        File tmpEvalPath = getTempEvaluationDataPath();
        createCRFPPData(evalDataF, tmpEvalPath);

        return EvaluationUtilities.evaluateStandard(tmpEvalPath.getAbsolutePath(), tagger).toString(includeRawResults);
    }

    @Override
    public String splitTrainEvaluate(Double split) {
        System.out.println("Paths :\n" + getCorpusPath() + "\n" + 
            GrobidProperties.getModelPath(model).getAbsolutePath() + "\n" + 
            getTempTrainingDataPath().getAbsolutePath() + "\n" + 
            getTempEvaluationDataPath().getAbsolutePath());// + " \nrand " + random);

        File trainDataPath = getTempTrainingDataPath();
        File evalDataPath = getTempEvaluationDataPath();

        final File dataPath = trainDataPath;
        createCRFPPData(getCorpusPath(), dataPath, evalDataPath, split);
        GenericTrainer trainer = TrainerFactory.getTrainer();

        if (epsilon != 0.0)
            trainer.setEpsilon(epsilon);
        if (window != 0)
            trainer.setWindow(window);
        if (nbMaxIterations != 0)
            trainer.setNbMaxIterations(nbMaxIterations);

        final File tempModelPath = new File(GrobidProperties.getModelPath(model).getAbsolutePath() + NEW_MODEL_EXT);
        final File oldModelPath = GrobidProperties.getModelPath(model);

        trainer.train(getTemplatePath(), dataPath, tempModelPath, GrobidProperties.getNBThreads(), model);

        // if we are here, that means that training succeeded
        renameModels(oldModelPath, tempModelPath);

        return EvaluationUtilities.evaluateStandard(evalDataPath.getAbsolutePath(), getTagger()).toString();
    }

    protected final File getTemplatePath() {
        return new File("data/deeptype/deeptype.template");
    }
}