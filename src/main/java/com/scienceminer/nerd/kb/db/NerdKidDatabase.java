package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kid.KidPredictor;
import org.apache.hadoop.record.CsvRecordInput;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for creating the LMDB databases used in (N)ERD Knowlegde Base for associating Wikidata Ids with their classes.
 * Wikidata Id is an entity Id for each Wikidata entity which is a number prefixed by a letter 'Q', e.g 'Q16502' for the entity 'World'
 * Class of Wikidata Ids might be one of these: NotNER, PERSON, LOCATION, ORGANIZATION, ACRONYM, ANIMAL, ARTIFACT, BUSINESS, INSTITUTION, MEASURE, AWARD, CONCEPT, CONCEPTUAL, CREATION, EVENT, LEGAL, IDENTIFIER, INSTALLATION, MEDIA, NATIONAL, SUBSTANCE, PLANT, PERIOD, TITLE, PERSON_TYPE, WEBSITE, SPORT_TEAM, UNKNOWN
 * Currently, it has been stored the prediction class until the Wikidata Id of Q412546;
 */

public class NerdKidDatabase extends StringRecordDatabase<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdKidDatabase.class);

    public NerdKidDatabase(KBEnvironment env) {
        super(env, DatabaseType.nerdKid);
    }

    @Override
    public KBEntry<String, String> deserialiseCsvRecord(
            CsvRecordInput record) {
        throw new UnsupportedOperationException();
    }

    /*
     * build Nerd_kid database
     * */

    public void buildNerdKidDatabase(StatementDatabase statementDatabase, boolean overwrite) throws Exception {

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

//                        System.out.println("Wikidata Id: " + wikidataId);
//                        System.out.println(Arrays.toString(statements.toArray()));

                        // prediction
                        KidPredictor kidPredictor = new KidPredictor();
                        predictedClass = kidPredictor.predict(wikidataId).getPredictedClass();
                        System.out.println("Wikidata Id: " + wikidataId + "; predicted class: " + predictedClass);
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

    public static void main(String[] args) {
        UpperKnowledgeBase upperKnowledgeBase = UpperKnowledgeBase.getInstance();
        upperKnowledgeBase.close();

    }
}
