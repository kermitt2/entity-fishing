package com.scienceminer.nerd.evaluation.convertion;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IITBCorpusConverter {
    public static void main(String[] args) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            String corpusPath = "data/corpus/corpus-long/iitb/";

            File fileSource = new File(corpusPath + "iitbOriginal.xml");
            File fileTarget = new File(corpusPath+ "iitbNew.xml");


            Document documentSource = documentBuilder.parse(fileSource);

            String rootName = documentSource.getDocumentElement().getNodeName();

            NodeList annotationsSource = documentSource.getElementsByTagName("annotation");

            List<String> docNames = new ArrayList<>();
            List<String> userIds = new ArrayList<>();
            List<String> mentions = new ArrayList<>();
            List<String> wikiNames = new ArrayList<>();
            List<String> offsets = new ArrayList<>();
            List<String> lengths = new ArrayList<>();
            List<String> docNamesDistinct = new ArrayList<>();

            // get the list of documents
            for (int i = 0; i < annotationsSource.getLength(); i++) {
                Element element = (Element) annotationsSource.item(i);

                String docName = element.getElementsByTagName("docName").item(0).getTextContent();
                docNames.add(docName);

                userIds.add(element.getElementsByTagName("userId").item(0).getTextContent());

                wikiNames.add(element.getElementsByTagName("wikiName").item(0).getTextContent());

                String offset = element.getElementsByTagName("offset").item(0).getTextContent();
                offsets.add(offset);
                int offsetInt = Integer.parseInt(offset);

                String length = element.getElementsByTagName("length").item(0).getTextContent();
                lengths.add(length);
                int lengthInt = Integer.parseInt(length);


                // read documents from the folder /RawText
                String docPath = corpusPath + "RawText/" + docName;
                File docFile = new File(docPath);
                if (!docFile.exists()) {
                    System.out.println("The document file " + docPath + " for corpus iitb is not found: ");
                    continue;
                }
                String docContent = null;

                try {
                    docContent = FileUtils.readFileToString(docFile, "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (docContent == null || docContent.length() == 0) {
                    System.out.println("Document is empty: " + docPath);
                }

                // the end of mention
                int end = offsetInt + lengthInt;
                String mention = docContent.substring(offsetInt, end);
                mentions.add(mention);

                // to get the unique name of documents
                docNamesDistinct = docNames.stream().distinct().collect(Collectors.toList());
            }
            // create a new file
            Document documentTarget = documentBuilder.newDocument();
            Element rootElement = documentTarget.createElement(rootName);
            documentTarget.appendChild(rootElement);

            for (String docNameDistinct : docNamesDistinct) {

                // create tag of document in a new file of xml
                Element docElementTarget = documentTarget.createElement("document");
                rootElement.appendChild(docElementTarget);
                // set attribute for document
                docElementTarget.setAttribute("docName", docNameDistinct);
                for (int i = 0; i < docNames.size(); i++) {
                    if (docNames.get(i).equals(docNameDistinct)) {
                        System.out.println("docNames : " + docNames.get(i) + "; docNameDistinct : " + docNameDistinct);
                        System.out.println("wikiName : " + wikiNames.get(i) + " ; offset : " + offsets.get(i) + " ; length : " + lengths.get(i));

                        Element annotation = documentTarget.createElement("annotation");
                        docElementTarget.appendChild(annotation);

                        Element userIdToBePut = documentTarget.createElement("userId");
                        userIdToBePut.appendChild(documentTarget.createTextNode(userIds.get(i)));
                        annotation.appendChild(userIdToBePut);

                        Element mentionToBePut = documentTarget.createElement("mention");
                        mentionToBePut.appendChild(documentTarget.createTextNode(mentions.get(i)));
                        annotation.appendChild(mentionToBePut);

                        Element wikiNameToBePut = documentTarget.createElement("wikiName");
                        wikiNameToBePut.appendChild(documentTarget.createTextNode(wikiNames.get(i)));
                        annotation.appendChild(wikiNameToBePut);

                        Element offsetToBePut = documentTarget.createElement("offset");
                        offsetToBePut.appendChild(documentTarget.createTextNode(offsets.get(i)));
                        annotation.appendChild(offsetToBePut);

                        Element lengthToBePut = documentTarget.createElement("length");
                        lengthToBePut.appendChild(documentTarget.createTextNode(lengths.get(i)));
                        annotation.appendChild(lengthToBePut);
                    }
                }
            }
            // write the information into a new file
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource domSource = new DOMSource(documentTarget);
            StreamResult result = new StreamResult(fileTarget);
            transformer.transform(domSource, result);

            System.out.println("A new file has been built.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
