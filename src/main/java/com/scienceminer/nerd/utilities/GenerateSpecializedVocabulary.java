package com.scienceminer.nerd.utilities;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.grobid.core.utilities.UnicodeUtil;

import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.model.*;
import com.scienceminer.nerd.kb.model.Page.PageType;
import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.disambiguation.NerdCategories;

/**
 * Utility for generating a vocabulary for a constrained set of Wikidata entity in a 
 * given target language. The constraint is expressed simply as an entity identifier
 * and a set of properties that will be recursively followed via P279 (subclass of) 
 * and P31 (instance of) properties. The Wikipedia for the target language can be 
 * exploited with the redirection and anchors. 
 *
 * Example: Command for creating the vocabulary of all software (Q7397) names in 
 * English but excluding video games (Q7889):
 * mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.utilities.GenerateSpecializedVocabulary 
 * -Dexec.args="en +Q7397 -Q7889 /home/lopez/test/softwareVoc.txt"
 */
public class GenerateSpecializedVocabulary {

    public String lang = null;
    public File outputPath = null;

    public GenerateSpecializedVocabulary(String lang, File outputFile) {
        this.lang = lang;
        this.outputPath = outputFile;
    }

    public void process(List<String> positiveEntityIds, List<String> negativeEntityIds) throws NerdResourceException {
        BufferedWriter writerVocab = null;
        BufferedWriter writerSuperType = null;
        BufferedWriter writerCategories = null;
        BufferedWriter writerEntities = null;
        List<String> properties = Arrays.asList(new String[]{"P279", "P31"});
        try {
            writerVocab = new BufferedWriter(new FileWriter(outputPath));
            File outputPathTypes = new File(outputPath.getPath()+".types");
            writerSuperType = new BufferedWriter(new FileWriter(outputPathTypes));
            File outputPathCategories = new File(outputPath.getPath()+".categories");
            writerCategories = new BufferedWriter(new FileWriter(outputPathCategories));
            File outputPathEntities = new File(outputPath.getPath()+".entities");
            writerEntities = new BufferedWriter(new FileWriter(outputPathEntities));
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

        Set<String> selection = new TreeSet<String>();
        for (String wikidataId : positiveEntityIds) {
            select(wikidataId, negativeEntityIds, upperKB, selection);
        }

        System.out.println("Selection of " + selection.size() + " entities");

        // gather vocabulary, as Wikipedia page titles/redirections/anchors (ok wikidata entity labels are missing!)
        try {
            List<String> categories = new ArrayList<String>();
            List<String> types = new ArrayList<String>();
            for (String entityId : selection) {
                //System.out.println(entityId);
                Integer pageId = upperKB.getPageIdByLang(entityId, lang); 
                //System.out.println(pageId);
                if (pageId == null) {
                    // no wikipedia page for this entity and the target language
                    continue;
                }
                Page page = wikipedia.getPageById(pageId);
                String title = cleanTitle(page.getTitle());
                if (title.length() > 0) {
                    writerVocab.write(title);
                    writerVocab.write("\n");
                }
                if (page.getType() == Page.PageType.article) {
                    Redirect[] redirects = ((Article)page).getRedirects();
                    for(Redirect redirect : redirects) {
                        title = cleanTitle(redirect.getTitle());
                        if (title.length() > 0) {
                            writerVocab.write(title);
                            writerVocab.write("\n");
                        }
                    }
                }

                // we also generate an additional file with the values of the P279 and P31 properties of 
                // all the selected entities
                List<String> localTypes = getPropertyValues(entityId, properties, upperKB);
                if (localTypes != null) {
                    for(String localType : localTypes) {
                        if (!types.contains(localType))
                            types.add(localType);
                    }
                }

                // we also generate an additional file with the Wikipedia categories of all the selected entities
                if (page.getType() == Page.PageType.article) {
                    List<String> localCategories = getCategoryTitles(page.getTitle(), wikipedia);
                    if (localCategories != null) {
                        for(String localCategory : localCategories) {
                            if (!categories.contains(localCategory))
                                categories.add(localCategory);
                        }
                    }
                }

                // and finally all the entity in a last file
                writerEntities.write(entityId);
                writerEntities.write("\n");
            }

            // write list of values of the P279 and P31 properties
            for(String type : types) {
                if (type.length() > 0) {
                    writerSuperType.write(type);
                    writerSuperType.write("\n");
                }
            }

            // write list of categories
            Collections.sort(categories);
            for(String category : categories) {
                if (category.length() > 0) {
                    writerCategories.write(category);
                    writerCategories.write("\n");
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                writerVocab.close();
                writerSuperType.close();
                writerCategories.close();
                writerEntities.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Recursive selection of wikidata entities via P279 and P31 properties
     */
    private void select(String wikidataId, List<String> negativeEntityIds, UpperKnowledgeBase upperKB, Set<String> selection) {
        List<Statement> statements = upperKB.getReverseStatements(wikidataId);
        if (statements != null) {
            // filter by P279 and P31
            for(Statement statement : statements) {
                if (statement.getPropertyId().equals("P279") || statement.getPropertyId().equals("P31")) {
                    String newEntityId = statement.getConceptId();
                    String value = statement.getValue();
                    if (!selection.contains(newEntityId) && !negativeEntityIds.contains(newEntityId)) {
                        selection.add(newEntityId);
                        select(newEntityId, negativeEntityIds, upperKB, selection); 
                    }
                    
                    if ( (value!= null) && (value.length() > 2) && (value.startsWith("Q")) ) {
                        Boolean valueSuffixIsNumeric = StringUtils.isNumeric(value.substring(1,value.length()));
                        if (valueSuffixIsNumeric && !selection.contains(value) && !negativeEntityIds.contains(value)) {
                            selection.add(value);
                            select(value, negativeEntityIds, upperKB, selection); 
                        }
                    }
                }
            }
        }
    }

    public List<String> getPropertyValues(String wikidataId, List<String> properties, UpperKnowledgeBase upperKB) {
        List<String> result = new ArrayList<String>();
        List<Statement> statements = upperKB.getReverseStatements(wikidataId);
        if (statements != null) {
            for(Statement statement : statements) {
                if (properties.contains(statement.getPropertyId())) {
                    if (!result.contains(statement.getValue()))
                        result.add(statement.getValue());
                }
            }
        }
        return result;
    }

    public List<String> getCategoryTitles(String title, LowerKnowledgeBase wikipedia) {
        List<String> result = new ArrayList<String>();
        Article sense = wikipedia.getArticleByTitle(title);
        for(com.scienceminer.nerd.kb.model.Category theCategory : sense.getParentCategories()) {
            if (theCategory == null) 
                continue;
            if (theCategory.getTitle() == null) 
                continue;
            if (theCategory.getTitle().toLowerCase().contains("disambiguation")) 
                continue;
            if (!NerdCategories.categoryToBefiltered(theCategory.getTitle())) {
                if (!result.contains(theCategory.getTitle()))
                    result.add(theCategory.getTitle());
            }
        }
        return result;
    }

    /**
     * Remove the disambiguation suffix in a title
     */
    private String cleanTitle(String title) {
        int ind = title.lastIndexOf("(");
        if (ind != -1) {
            title = title.substring(0, ind);
        }
        return title.replace("\n", "").trim();
    }

    public static void main(String args[]) throws Exception {
        //System.out.println(args.length);
        if (args.length < 3) {
            System.out.println("usage: command lang entity_id outputPath");
            // e.g. all software names: 
            /** 
            mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.utilities.GenerateSpecializedVocabulary -Dexec.args="en +Q7397 /tmp/vocabulary-software.txt"
            */
            // we can put as many seed entities +Q??? and ones to ignore -Q??? as needed 
            System.exit(-1);
        }

        String lang = args[0];
        List<String> languages = Arrays.asList("en", "fr", "de", "es", "it");;
        if (!languages.contains(lang)) {
            System.out.println("unsupported language, must be one of " + languages.toString());
            System.exit(-1);
        }

        List<String> positiveEntityIds = new ArrayList<String>();
        List<String> negativeEntityIds = new ArrayList<String>();
        for (int i=1; i< args.length-1; i++) {
            String entityId = args[i];
            System.out.println(entityId);
            if (entityId.startsWith("+Q") || entityId.startsWith("Q")) {
                positiveEntityIds.add(entityId.substring(1, entityId.length()));
            } else if (entityId.startsWith("-Q")) {
                negativeEntityIds.add(entityId.substring(1, entityId.length()));
            } else {
                System.out.println("invalid entity identifier: " + entityId);
                System.exit(-1);
            }
        }

        File outputFile = new File(args[args.length-1]);
        File dataDir = outputFile.getParentFile();
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            System.err.println("Invalid output path directory: " + dataDir.getPath());
            System.exit(-1);
        }
        GenerateSpecializedVocabulary voc = new GenerateSpecializedVocabulary(lang, outputFile);
        voc.process(positiveEntityIds, negativeEntityIds);
    }
}


