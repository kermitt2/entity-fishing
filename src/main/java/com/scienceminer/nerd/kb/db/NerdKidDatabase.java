package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.Statement;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.hadoop.record.CsvRecordInput;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.Transaction;
import org.nerd.kid.extractor.FeatureFileExtractor;
import org.nerd.kid.model.WikidataNERPredictor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A factory for creating the LMDB databases used in (N)ERD Knowlegde Base for associating Wikidata Ids with their classes.
 * Wikidata Id is an entity Id for each Wikidata entity which is a number prefixed by a letter 'Q', e.g 'Q16502' for the entity 'World'
 * Class of Wikidata Ids might be one of these: NotNER, PERSON, LOCATION, ORGANIZATION, ACRONYM, ANIMAL, ARTIFACT, BUSINESS, INSTITUTION, MEASURE, AWARD, CONCEPT, CONCEPTUAL, CREATION, EVENT, LEGAL, IDENTIFIER, INSTALLATION, MEDIA, NATIONAL, SUBSTANCE, PLANT, PERIOD, TITLE, PERSON_TYPE, WEBSITE, SPORT_TEAM, UNKNOWN
 */

public class NerdKidDatabase extends StringRecordDatabase<List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdKidDatabase.class);

    public NerdKidDatabase(KBEnvironment env) {
        super(env, KBDatabase.DatabaseType.nerdKid);
    }

    @Override
    public KBEntry<String, List<String>> deserialiseCsvRecord(
            CsvRecordInput record) throws IOException {
        throw new UnsupportedOperationException();
    }

    /*
    * build Nerd_kid database
    * */

    public void buildNerdKidDatabase(StatementDatabase statementDatabase, boolean overwrite){
        if (isLoaded && !overwrite)
            return;
        System.out.println("Loading " + statementDatabase.getName() + " database");

        if (statementDatabase == null)
            throw new NerdResourceException("The database is not found.");

        // iterate through Statements database
        KBIterator kbIterator = new KBIterator(statementDatabase);
        Transaction transaction = environment.createWriteTransaction();
        try {
            int numberDataAdded = 0;

            while (kbIterator.hasNext()) {
                // for now on, add 1000 data into Nerd-Kid database
                if(numberDataAdded > 1000){
                    transaction.commit();
                    transaction.close();
                    transaction = environment.createWriteTransaction();
                    numberDataAdded = 0;
                }

                Entry entry = kbIterator.next();
                byte[] key = entry.getKey();
                byte[] value = entry.getValue();
                try {
                    String wikidataId = (String) KBEnvironment.deserialize(key);
                    // collect the content of the statement database into a list
                    List<Statement> statements = statementDatabase.retrieve(wikidataId);

                    // group the list by its wikidataIds
                    Map<String, List<Statement>> mapWikidataIdGrouped = new HashMap<>();
                    for (Statement statement : statements) {
                        String propertyId = statement.getPropertyId();
                        String valueProperty = statement.getValue();
                        if (!mapWikidataIdGrouped.containsKey(wikidataId)) { // for new wikidataId that doesn't exist yet in the map
                            List<Statement> listGroupedByWikidataId = new ArrayList<>();
                            listGroupedByWikidataId.add(statement);
                            mapWikidataIdGrouped.put(wikidataId, listGroupedByWikidataId);
                        } else { // if the wikidataId has already exist, add another statement
                            mapWikidataIdGrouped.get(wikidataId).add(statement);
                        }
                    }
                    // count the data read
                    numberDataAdded++;

                    // list of features of the mapper
                    Map<String, List<String>> resultFeatures = this.loadFeatures();
                    List<String> propertiesNoValuesMapper = this.loadFeaturesNoValue();

                    // size of features
                    int nbOfFeatures = resultFeatures.size() + propertiesNoValuesMapper.size();
                    double[] featureVector = new double[nbOfFeatures];
                    int idx = 0;

                    // concatenate features with value
                    List<String> propValMap = new ArrayList<>();
                    for (Map.Entry<String, List<String>> result : resultFeatures.entrySet()) {
                        String property = result.getKey();
                        List<String> values = result.getValue();
                        for (String val : values) {
                            String propVal = property + "_" + val;
                            propValMap.add(propVal);
                        }
                    }

                    // put 1 if featuresof entity match with the list of features in the map, otherwise put 0

                    // iterate inside the map which has been grouped by its WikidataId
                    for (Map.Entry<String, List<Statement>> mapWikiIdGrouped : mapWikidataIdGrouped.entrySet()) {
                        String keyWikidataId = mapWikiIdGrouped.getKey();
                        List<Statement> statementList = mapWikiIdGrouped.getValue();
                        for (Statement statement : statementList) {
                            String prop = statement.getPropertyId();
                            String val = statement.getValue();
                            String concatenatePropVal = prop + "_" + val;

                            // iterate in features no value
                            for (String propNoVal : propertiesNoValuesMapper) {
                                if (prop.equals(propNoVal)){
                                    featureVector[idx] = (double) 1;
                                }else{
                                    featureVector[idx] = (double) 0;
                                }
                                idx++;
                            }

                            // iterate in features with value
                            for (String propVal : propValMap) {
                                if (concatenatePropVal.equals(propVal)){
                                    featureVector[idx] = (double) 1;
                                }else{
                                    featureVector[idx] = (double) 0;
                                }
                                idx++;
                            }
                        }
                        // do the prediction based on propertyId and valueProperty collected
                        WikidataNERPredictor wikidataNERPredictor = new WikidataNERPredictor();
                        String predictedClass = wikidataNERPredictor.predict(featureVector);
                        System.out.println("Wikidata Id: " + wikidataId + "; Predicted class: " + predictedClass);

                        // store the wikidata Id and its predicted class
                        db.put(transaction,KBEnvironment.serialize(wikidataId), KBEnvironment.serialize(predictedClass));
                    }
                } catch(Exception e) {
                    LOGGER.error("Error when writing the database.", e);
                }
            }
        } catch(Exception e) {
            LOGGER.error("Error when reading the database.", e);
        } finally {
            if (kbIterator != null)
                kbIterator.close();
            transaction.commit();
            transaction.close();
            isLoaded = true;
        }
    }

    public Map<String, List<String>> loadFeatures() {
        // get the features (properties and values) from the list in the csv file
        //String fileFeatureMapper = pathSource + "/feature_mapper.csv";

        String fileFeatureMapper = "/feature_mapper.csv";
        InputStream inputStream = this.getClass().getResourceAsStream(fileFeatureMapper);
        //ClassLoader classLoader = getClass().getClassLoader();

        try {
            //File file = new File(classLoader.getResource(fileFeatureMapper).getFile());
            //InputStream inputStream = new FileInputStream(file);

            Map<String, List<String>> featureMap = new HashMap<>();
            Reader featureMapperIn = new InputStreamReader(inputStream);
            Iterable<CSVRecord> recordsFeatures = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(featureMapperIn);

            for (CSVRecord recordFeature : recordsFeatures) {
                String property = recordFeature.get("Property");
                String value = recordFeature.get("Value");
                // in order to get unique of property-value combination
                if (featureMap.keySet().contains(property)) {
                    featureMap.get(property).add(value); // if a property-value exists, get it
                } else {
                    List<String> values = new ArrayList<>();
                    values.add(value);
                    featureMap.put(property, values); // if there aren't exist yet, add a new one
                }
            }
            return featureMap;
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public List<String> loadFeaturesNoValue() {
        // get the features (properties) from the list in the csv file
        //String fileFeatureMapperNoValue = pathSource + "/feature_mapper_no_value.csv";

        String fileFeatureMapper = "/feature_mapper_no_value.csv";
        InputStream inputStream = this.getClass().getResourceAsStream(fileFeatureMapper);
        //ClassLoader classLoader = getClass().getClassLoader();

        try {
            //File file = new File(classLoader.getResource(fileFeatureMapper).getFile());
            //InputStream inputStream = new FileInputStream(file);
            List<String> featureListNoValue = new ArrayList<>();
            Reader featureMapperIn = new InputStreamReader(inputStream);
            Iterable<CSVRecord> recordsFeaturesNoValue = null;
            recordsFeaturesNoValue = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(featureMapperIn);

            for (CSVRecord recordFeatureNoValue : recordsFeaturesNoValue) {
                String property = recordFeatureNoValue.get("Property");
                if (recordFeatureNoValue != null) {
                    featureListNoValue.add(property);
                }
            }

            return featureListNoValue;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
