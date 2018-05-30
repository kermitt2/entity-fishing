package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.Statement;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.hadoop.record.CsvRecordInput;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.Transaction;
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
        super(env, DatabaseType.nerdKid);
    }

    @Override
    public KBEntry<String, List<String>> deserialiseCsvRecord(
            CsvRecordInput record) throws IOException {
        throw new UnsupportedOperationException();
    }


    /*
    * build Nerd_kid database
    * */

    public void buildNerdKidDatabase(StatementDatabase statementDatabase, boolean overwrite) throws Exception {
        // read the features from the mapper
        Map<String, List<String>> resultFeatures = this.loadFeatures();
        List<String> propertiesNoValuesMapper = this.loadFeaturesNoValue();

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

        if (isLoaded && !overwrite)
            return;
        System.out.println("Loading " + statementDatabase.getName() + " database");

        if (statementDatabase == null)
            throw new NerdResourceException("The database is not found.");

        // iterate through the statement database
        KBIterator kbIterator = new KBIterator(statementDatabase);

        try {
            // while there are some data inside the database
            while (kbIterator.hasNext()) {
                Entry entry = kbIterator.next();
                byte[] key = entry.getKey();
                byte[] value = entry.getValue();
                String wikidataId = null;
                String predictedClass = null;

                // it's better to put the create write transaction in the try to avoid writing error bug
                try (Transaction transaction = environment.createWriteTransaction()) {
                    // we got the statement Id, just collect the Ids begin with 'Q'
                    String statementId = (String) KBEnvironment.deserialize(key);
                    if (statementId.startsWith("Q")) {
                        // we have wikidataId (the wikidata Id can be get by accessing the key's statement or the concept Id of statements)
                        wikidataId = statementId;
                        // collect the content of the statement database into a list
                        List<Statement> statements = (List<Statement>) KBUpperEnvironment.deserialize(value);

//                        System.out.println("Wikidata Id: " + wikidataId);
//                        System.out.println(Arrays.toString(statements.toArray()));

                        List<String> featuresNoValueCollected = new ArrayList<>();
                        List<String> featuresCollected = new ArrayList<>();
                        List<Double> featureVector = new ArrayList<>();

                        for (Statement statement : statements) {
                            // for each wikidata Id, we have some features (properties + values)
                            String prop = statement.getPropertyId();
                            String val = statement.getValue();
                            String concatenatePropVal = prop + "_" + val;

                            // collect the features collected into the lists
                            featuresNoValueCollected.add(prop);
                            featuresCollected.add(concatenatePropVal);
                        }

                        // iterate in features no value
                        for (String propNoVal : propertiesNoValuesMapper) {
                            // put 1 if features of entity's feature match with the features in the mapper, otherwise put 0
                            if (featuresNoValueCollected.contains(propNoVal)) {
                                featureVector.add((double) 1);
                            } else {
                                featureVector.add((double) 0);
                            }
                        }

                        // iterate in features with value
                        for (String propVal : propValMap) {
                            // put 1 if features of entity's feature match with the features in the mapper, otherwise put 0
                            if (featuresCollected.contains(propVal)) {
                                featureVector.add((double) 1);
                            } else {
                                featureVector.add((double) 0);
                            }
                        }
                        // convert the features vector into array of double since Smile can just predict in this data format via method reference
                        double[] featureVectorArr = featureVector.stream().mapToDouble(Double::doubleValue).toArray();

                        // do the prediction based on propertyId and valueProperty collected
                        WikidataNERPredictor wikidataNERPredictor = new WikidataNERPredictor();
                        predictedClass = wikidataNERPredictor.predict(featureVectorArr);
 //                       System.out.println("Predicted class: " + predictedClass);
                    }
                    db.put(transaction, KBEnvironment.serialize(wikidataId), KBEnvironment.serialize(predictedClass));
                    transaction.commit();
                } catch (Exception e) {
                    LOGGER.error("Error when writing the database.", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error when reading the database.", e);
        } finally {
            if (kbIterator != null)
                kbIterator.close();
            isLoaded = true;
        }
    }

    public Map<String, List<String>> loadFeatures() {
        // get the features (properties and values) from the list in the csv file

        String fileFeatureMapper = "/feature_mapper.csv";
        InputStream inputStream = this.getClass().getResourceAsStream(fileFeatureMapper);
        try {
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

        String fileFeatureMapper = "/feature_mapper_no_value.csv";
        InputStream inputStream = this.getClass().getResourceAsStream(fileFeatureMapper);

        try {
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
