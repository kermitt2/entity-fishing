package com.scienceminer.nerd.evaluation.convertion;

import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.engines.NERParsers;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*This class is built to add NE types into aida-conll corpus reference file
* Run with this command: mvn compile exec:java -Dexec.mainClass=com.scienceminer.nerd.evaluation.convertion.AidaConllCorpusWithNEType -Dexec.args="[corpus name] [ne type]"
* For example to add conll original NE type into aida-testb reference corpus file :
* $ mvn compile exec:java -Dexec.mainClass=com.scienceminer.nerd.evaluation.convertion.AidaConllCorpusWithNEType -Dexec.args="aida-testb conll"
*
* */

public class AidaConllCorpusWithNEType {
    private static final Logger LOGGER = LoggerFactory.getLogger(AidaConllCorpusWithNEType.class);
    public static List<String> corpora = Arrays.asList("aida-train", "aida-testa", "aida-testb");
    public static List<String> neType = Arrays.asList("grobid", "conll", "kid");
    private UpperKnowledgeBase upperKnowledgeBase = null;
    private String path = "data/corpus/corpus-long/";


    public void addNEType(String corpus, String neType) {
        if (neType.equals("conll")) {
            aidaConllWithNEConll2003Type(corpus);
        } //else if (neType.equals("grobid")) {
//            aidaConllWithNEGrobidType(corpus);
//        } else if (neType.equals("kid")) {
//            aidaConllWithNerdKidType(corpus);
//        }
    }

    public void aidaConllWithNEConll2003Type(String corpus) {
        String corpusPath = path + corpus;

        File fileSource = new File(corpusPath + "/" + corpus + ".xml");
        File fileTarget = new File(corpusPath + "-withNEConll2003Type/" + corpus + "-withNEConll2003Type.xml");

        // count the number of word max for mentions can be found in the conll corpus
        int maxNumberOfMention = maxWordNumber(corpus);
        //int maxNumberOfMention = 10;
        DocumentBuilderFactory documentBuilderFactory = null;
        DocumentBuilder documentBuilder = null;
        Document documentSource, documentTarget;

        if (!fileSource.exists()) {
            System.out.println("The reference file for corpus " + corpus + " is not found: " + fileSource.getPath());
        }

        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            documentSource = documentBuilder.parse(fileSource);

            Node root = documentSource.getDocumentElement();
            String rootName = documentSource.getDocumentElement().getNodeName();

            NodeList annotations = documentSource.getElementsByTagName("annotation");

            // call the conll parser
            ArrayList<ConllEntity> resultConllParser = conllParser(corpus, maxNumberOfMention);

            for (int i = 0; i < annotations.getLength(); i++) {
                // get the annotation
                Node annotation = documentSource.getElementsByTagName("annotation").item(i);

                // get the mention
                Node mention = documentSource.getElementsByTagName("mention").item(i);
                String mentionString = mention.getTextContent();
                System.out.println("Mention (" + i + ") : " + mentionString);

                // get the NE Conll mention
                String mentionConll = resultConllParser.get(i).mention;
                String neConllType = resultConllParser.get(i).ner;
                String neConllTypeString = null;

                //System.out.println("mentionConll : " + mentionConll);
                System.out.println("Type : " + neConllType);

                // ignore blank space between them and put all in lower case
                mentionString = mentionString.replaceAll("\\s", "");
                mentionConll = mentionConll.replaceAll("\\s", "");


                if (mentionString.toLowerCase().equals(mentionConll.toLowerCase())) {
                    neConllTypeString = neConllType;
                } else {
                    neConllTypeString = "UNDEFINED";
                    break;
                }

                // add the result to a new node
                // create a new element
                Element neCollType = documentSource.createElement("neConllType");
                neCollType.appendChild(documentSource.createTextNode(neConllTypeString));
                annotation.appendChild(neCollType);
            }

            // create a new file
            documentTarget = documentBuilder.newDocument();
            Element rootElement = documentTarget.createElement(rootName);
            documentTarget.appendChild(rootElement);

            // -- write the information into a new file --
            // this command will give a bug "DOMSource cannot be processed: check that saxon9-dom.jar is on the classpath"
            // Transformer transformer = TransformerFactory.newInstance().newTransformer();
            TransformerFactory transformerFactory = new com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource domSource = new DOMSource(documentSource);
            StreamResult result = new StreamResult(fileTarget);
            transformer.transform(domSource, result);

            System.out.println("A new file has been built.");

        } catch (Exception e) {
            LOGGER.error("Error adding NE Original Conll 2003 Type for Aida Conll Corpus", e);
        }
    }

//    public void aidaConllWithNEGrobidType(String corpus) {
//        String corpusPath = path + corpus;
//
//        File fileSource = new File(corpusPath + "/" + corpus + ".xml");
//        File fileTarget = new File(corpusPath + "-withNEGrobidType/" + corpus + "-withNEGrobidType.xml");
//
//        DocumentBuilderFactory documentBuilderFactory = null;
//        DocumentBuilder documentBuilder = null;
//        UpperKnowledgeBase upperKnowledgeBase = null;
//        Document documentSource, documentTarget;
//        NERParsers nerParsers = null;
//
//        if (!fileSource.exists()) {
//            System.out.println("The reference file for corpus " + corpus + " is not found: " + fileSource.getPath());
//        }
//
//        try {
//
//            // need to prepare the upper knowledge first before using the grobid NER Parser
//            upperKnowledgeBase = UpperKnowledgeBase.getInstance();
//            nerParsers = new NERParsers();
//
//            documentBuilderFactory = DocumentBuilderFactory.newInstance();
//            documentBuilder = documentBuilderFactory.newDocumentBuilder();
//            documentSource = documentBuilder.parse(fileSource);
//
//            Node root = documentSource.getDocumentElement();
//            String rootName = documentSource.getDocumentElement().getNodeName();
//
//            NodeList annotations = documentSource.getElementsByTagName("annotation");
//
//            for (int i = 0; i < annotations.getLength(); i++) {
//                // get the annotation
//                Node annotation = documentSource.getElementsByTagName("annotation").item(i);
//
//                // get the mention
//                Node mention = documentSource.getElementsByTagName("mention").item(i);
//                String mentionString = mention.getTextContent();
//                System.out.println("Mention (" + i + ") : " + mentionString);
//
//                String neGrobidTypeString = null;
//
//
//                // get the NE Grobid type
//                List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(mentionString, new Language("en", 1.0));
//                List<org.grobid.core.data.Entity> disambiguatedEntities = nerParsers.extractNE(tokens, new Language("en", 1.0));
//                if (disambiguatedEntities.size() != 0) {
//                    neGrobidTypeString = disambiguatedEntities.get(0).getType().getName();
//                } else {
//                    neGrobidTypeString = "UNKNOWN";
//                }
//
//                System.out.println("Type : " + neGrobidTypeString);
//
//                // add the result to a new node
//                // create a new element
//                Element neGrobidType = documentSource.createElement("neGrobidType");
//                neGrobidType.appendChild(documentSource.createTextNode(neGrobidTypeString));
//                annotation.appendChild(neGrobidType);
//            }
//
//            // create a new file
//            documentTarget = documentBuilder.newDocument();
//            Element rootElement = documentTarget.createElement(rootName);
//            documentTarget.appendChild(rootElement);
//
//            // -- write the information into a new file --
//            // this command will give a bug "DOMSource cannot be processed: check that saxon9-dom.jar is on the classpath"
//            // Transformer transformer = TransformerFactory.newInstance().newTransformer();
//            TransformerFactory transformerFactory = new com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl();
//            Transformer transformer = transformerFactory.newTransformer();
//            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//            DOMSource domSource = new DOMSource(documentSource);
//            StreamResult result = new StreamResult(fileTarget);
//            transformer.transform(domSource, result);
//
//            System.out.println("A new file has been built.");
//
//        } catch (Exception e) {
//            LOGGER.error("Error adding NE Grobid Type for Aida Conll Corpus", e);
//        }
//    }
//
//    public void aidaConllWithNerdKidType(String corpus) {
//        String corpusPath = path + corpus;
//
//        File fileSource = new File(corpusPath + "/" + corpus + ".xml");
//        File fileTarget = new File(corpusPath + "-withNerdKidType/" + corpus + "-withNerdKidType.xml");
//
//        DocumentBuilderFactory documentBuilderFactory = null;
//        DocumentBuilder documentBuilder = null;
//        UpperKnowledgeBase upperKnowledgeBase = null;
//        Document documentSource, documentTarget;
//        NERParsers nerParsers = null;
//
//        if (!fileSource.exists()) {
//            System.out.println("The reference file for corpus " + corpus + " is not found: " + fileSource.getPath());
//        }
//
//        try {
//
//            // need to prepare the upper knowledge first before using the grobid NER Parser
//            upperKnowledgeBase = UpperKnowledgeBase.getInstance();
//            nerParsers = new NERParsers();
//
//            documentBuilderFactory = DocumentBuilderFactory.newInstance();
//            documentBuilder = documentBuilderFactory.newDocumentBuilder();
//            documentSource = documentBuilder.parse(fileSource);
//
//            Node root = documentSource.getDocumentElement();
//            String rootName = documentSource.getDocumentElement().getNodeName();
//
//            NodeList annotations = documentSource.getElementsByTagName("annotation");
//
//            for (int i = 0; i < annotations.getLength(); i++) {
//                // get the annotation
//                Node annotation = documentSource.getElementsByTagName("annotation").item(i);
//
//                // get the mention
//                Node mention = documentSource.getElementsByTagName("mention").item(i);
//                String mentionString = mention.getTextContent();
//                System.out.println("Mention (" + i + ") : " + mentionString);
//
//                String neGrobidTypeString = null;
//
//
//                // get the NE Grobid type
//                List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(mentionString, new Language("en", 1.0));
//                List<org.grobid.core.data.Entity> disambiguatedEntities = nerParsers.extractNE(tokens, new Language("en", 1.0));
//                if (disambiguatedEntities.size() != 0) {
//                    neGrobidTypeString = disambiguatedEntities.get(0).getType().getName();
//                } else {
//                    neGrobidTypeString = "UNKNOWN";
//                }
//
//                System.out.println("Type : " + neGrobidTypeString);
//
//                // add the result to a new node
//                // create a new element
//                Element neGrobidType = documentSource.createElement("nerdKidType");
//                neGrobidType.appendChild(documentSource.createTextNode(neGrobidTypeString));
//                annotation.appendChild(neGrobidType);
//            }
//
//            // create a new file
//            documentTarget = documentBuilder.newDocument();
//            Element rootElement = documentTarget.createElement(rootName);
//            documentTarget.appendChild(rootElement);
//
//            // -- write the information into a new file --
//            // this command will give a bug "DOMSource cannot be processed: check that saxon9-dom.jar is on the classpath"
//            // Transformer transformer = TransformerFactory.newInstance().newTransformer();
//            TransformerFactory transformerFactory = new com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl();
//            Transformer transformer = transformerFactory.newTransformer();
//            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//            DOMSource domSource = new DOMSource(documentSource);
//            StreamResult result = new StreamResult(fileTarget);
//            transformer.transform(domSource, result);
//
//            System.out.println("A new file has been built.");
//
//        } catch (Exception e) {
//            LOGGER.error("Error adding NE Grobid Type for Aida Conll Corpus", e);
//        }
//    }

    public ArrayList<ConllEntity> conllParser(String corpus, int maxNumbeOfMention) {
        File fileConll = new File("data/conll2003/en/" + corpus + ".txt");
        String line;
        ArrayList<String> mentionSourceList = new ArrayList<String>();
        ArrayList<String> tagSourceList = new ArrayList<String>();
        ArrayList<ConllEntity> conllResults = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileConll));

            while ((line = reader.readLine()) != null) {

                if (isIgnored(line)) {
                    continue;
                }

                // split every line to get mention and ner type
                String[] tokens = line.split(" ");

                // firstly, collect all the mentions and their ner tags
                if (tokens[3].startsWith("B-") || (tokens[3].startsWith("I-"))) {
                    String mention = tokens[0];
                    String ner = tokens[3];

                    // put the result in the hash map
                    mentionSourceList.add(mention);
                    tagSourceList.add(ner);
                }
            }

            reader.close();

            // read the result
            int size = mentionSourceList.size();

            for (int i = 0; i < size; i++) {
                String currentNerTag = tagSourceList.get(i);
                String nextNerTag = tagSourceList.get((i + 1) % size);
                String nextNextNerTag = tagSourceList.get((i + 2) % size);
                String currentMention = mentionSourceList.get(i);
                String nextMention = mentionSourceList.get((i + 1) % size);
                String mention = null;
                String ner = null;

                ConllEntity conllEntity = new ConllEntity();

                // if the  B- tag follows by the  I- tag
                if (currentNerTag.startsWith("B-")) {
                    if (nextNerTag.startsWith("I-")) {
                        if (nextNextNerTag.startsWith("I-")) {
                            String iMention = null;
                            StringBuilder sb = new StringBuilder();
                            String currentITag = null, nextITag = null, currentIMention = null, nextIMention = null;
                            for (int j = 0; j < maxNumbeOfMention; j++) {
                                currentITag = tagSourceList.get((i + j + 1) % size);
                                nextITag = tagSourceList.get((i + j + 2) % size);
                                currentIMention = mentionSourceList.get((i + j + 1) % size);
                                nextIMention = mentionSourceList.get((i + j + 2) % size);

                                if (currentITag.equals(nextITag)) {
                                    // mention is concatenated with a blank space if it's a word and not containing ' or .
                                    if (nextIMention.contains("'") || currentIMention.endsWith(".")) {
                                        iMention = sb.append(currentIMention).toString();
                                    } else {
                                        iMention = sb.append(" ").append(currentIMention).toString();
                                    }
                                } else {
                                    iMention = iMention + " " + currentIMention;
                                    System.out.println("Total mention has been given NE Type : " + i);
                                    break;
                                }
                            }
                            mention = currentMention + iMention;
                        } else {
                            currentNerTag = currentNerTag.replace("B-", "");
                            nextNerTag = nextNerTag.replace("I-", "");
                            if (currentNerTag.equals(nextNerTag)) {
                                if (nextMention.startsWith("'") || currentMention.endsWith(".")) {
                                    mention = currentMention + nextMention;
                                } else {
                                    mention = currentMention + " " + nextMention;
                                }
                            }
                        }
                    } else {
                        mention = currentMention;
                    }
                    ner = currentNerTag.replace("B-", "");
                    conllEntity.mention = mention;
                    conllEntity.ner = ner;
                    conllResults.add(conllEntity);
                }

                System.out.println("Total mention has been given NE Type : " + i);
            }

        } catch (Exception e) {
            LOGGER.error("Error occurs when extracting conll file.", e);
        }
        return conllResults;
    }

    public int maxWordNumber(String corpus) {
        String corpusPath = path + corpus;
        int maxWordNumber = 0;

        File fileSource = new File(corpusPath + "/" + corpus + ".xml");

        DocumentBuilderFactory documentBuilderFactory = null;
        DocumentBuilder documentBuilder = null;
        Document documentSource, documentTarget;

        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            documentSource = documentBuilder.parse(fileSource);

            Node root = documentSource.getDocumentElement();
            String rootName = documentSource.getDocumentElement().getNodeName();

            NodeList annotations = documentSource.getElementsByTagName("annotation");

            for (int i = 0; i < annotations.getLength(); i++) {
                // get the annotation
                Node annotation = documentSource.getElementsByTagName("annotation").item(i);

                // get the mention
                Node mention = documentSource.getElementsByTagName("mention").item(i);
                String mentionString = mention.getTextContent();
                System.out.println("mentionString : " + mentionString);
                // get the number of words of mention
                int numberOfWords = mentionString.split("\\w+").length;
                // take the max number of mention word
                if (numberOfWords >= maxWordNumber) {
                    maxWordNumber = numberOfWords;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error when counting number of words in corpus " + corpus, e);
        }
        return maxWordNumber;
    }

    public boolean isIgnored(String line) {
        boolean ignored = false;

        if (line.length() == 0) {
            ignored = true;
        }

        if (line == " ") {
            ignored = true;
        }

        if ((line == "\\r\\n") || (line == "\\n")) {
            ignored = true;
        }

        if (line.startsWith("-DOCSTART-")) {
            ignored = true;
        }

        if (line.startsWith(". . O O")) {
            ignored = true;
        }

        if (line.startsWith("\" \" O O")) {
            ignored = true;
        }

        if (line.startsWith(", , O O")) {
            ignored = true;
        }

        if (line.startsWith("( ( O O")) {
            ignored = true;
        }

        if (line.startsWith(") ) O O")) {
            ignored = true;
        }

        if (line.startsWith("-- : O O")) {
            ignored = true;
        }

        if (line.startsWith(": : O O")) {
            ignored = true;
        }

        if (line.startsWith("... : O O")) {
            ignored = true;
        }

        if (line.startsWith("- : O O")) {
            ignored = true;
        }

        return ignored;
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: command [name_of_corpus] [ne_type]");
            System.err.println("corpus must be one of: " + AidaConllCorpusWithNEType.corpora.toString());
            System.exit(-1);
        }

        String corpus = args[0].toLowerCase();
        if (!corpora.contains(corpus)) {
            System.err.println("corpus must be one of: " + AidaConllCorpusWithNEType.corpora.toString());
            System.exit(-1);
        }

        String neType = args[1].toLowerCase();
        if (!neType.contains(neType)) {
            System.err.println("NE Type must be one of: " + AidaConllCorpusWithNEType.neType.toString());
            System.exit(-1);
        }

        AidaConllCorpusWithNEType aidaConllCorpusWithNEType = new AidaConllCorpusWithNEType();
        aidaConllCorpusWithNEType.addNEType(corpus, neType);
    }
}
