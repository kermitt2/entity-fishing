package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kid.KidPredictor;
import org.apache.hadoop.record.CsvRecordInput;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A factory for creating the LMDB databases used in (N)ERD Knowlegde Base for associating Wikidata Ids with their classes.
 * Wikidata Id is an entity Id for each Wikidata entity which is a number prefixed by a letter 'Q', e.g 'Q16502' for the entity 'World'
 * Class of Wikidata Ids might be one of these: NotNER, PERSON, LOCATION, ORGANIZATION, ACRONYM, ANIMAL, ARTIFACT, BUSINESS, INSTITUTION, MEASURE, AWARD, CONCEPT, CONCEPTUAL, CREATION, EVENT, LEGAL, IDENTIFIER, INSTALLATION, MEDIA, NATIONAL, SUBSTANCE, PLANT, PERIOD, TITLE, PERSON_TYPE, WEBSITE, SPORT_TEAM, UNKNOWN
 * Currently, it has been stored the prediction class until the Wikidata Id of Q412546;
 */

public class NerdKidDatabase extends StringRecordDatabase<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdKidDatabase.class);

    private KidPredictor kidPredictor;

    public NerdKidDatabase(KBEnvironment env) {
        super(env, DatabaseType.nerdKid);
        kidPredictor = new KidPredictor();
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
        System.out.println("Loading " + getName() + " database");

        if (statementDatabase == null)
            throw new NerdResourceException("The statement database is not found. Cannot get the data. Aborting. ");

        // iterate through the statement database
        KBIterator kbIterator = new KBIterator(statementDatabase);

        int counter = 0;
        try {
            // it's better to put the create write transaction in the try to avoid writing error bug
            Transaction transaction = environment.createWriteTransaction();
            long start = System.nanoTime();

            // while there are some data inside the database
            while (kbIterator.hasNext()) {
                Entry entry = kbIterator.next();
                if (counter > 0 && counter % 10000 == 0) {
                    try {
                        transaction.commit();
                        transaction.close();

                        System.out.println(counter + ": 10000 elements classified and loaded in "
                                + TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + " s");

                        start = System.nanoTime();
                        transaction = environment.createWriteTransaction();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // we got the statement Id, just collect the Ids begin with 'Q'
                byte[] key = entry.getKey();
                String wikidataId = (String) KBEnvironment.deserialize(key);

                if (wikidataId.startsWith("Q")) {
                    List<Statement> value = (List<Statement>) KBEnvironment.deserialize(entry.getValue());

                    // we have wikidataId (the wikidata Id can be get by accessing the key's statement or the concept Id of statements)
                    //                        System.out.println("Wikidata Id: " + wikidataId);
                    //                        System.out.println(Arrays.toString(statements.toArray()));

                    // prediction

                    String predictedClass = kidPredictor.predict(wikidataId, value).getPredictedClass();
//                    String predictedClass = kidPredictor.predict(wikidataId).getPredictedClass();
                    //                        System.out.println("Wikidata Id: " + wikidataId + "; predicted class: " + predictedClass);
                    db.put(transaction, KBEnvironment.serialize(wikidataId), KBEnvironment.serialize(predictedClass));
                    counter++;
                }
                //                    transaction.commit();
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
