package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kid.ClassPredictor;
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

    private ClassPredictor classPredictor;

    public NerdKidDatabase(KBEnvironment env) {
        super(env, DatabaseType.nerdKid);
        classPredictor = new ClassPredictor();
    }

    @Override
    public KBEntry<String, String> deserialiseCsvRecord(
            CsvRecordInput record) {
        throw new UnsupportedOperationException();

    }

    /*
     * build Nerd_kid database
     */
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
            long start = System.nanoTime();

            // while there are some data inside the database
            while (kbIterator.hasNext()) {
                Entry entry = kbIterator.next();
                byte[] key = entry.getKey();
                // it's better to put the create write transaction in the try to avoid writing error bug and the database won't be created
                try (Transaction transaction = environment.createWriteTransaction()) {
                    // we have wikidataId (the wikidata Id can be get by accessing the key's statement or the concept Id of statements)
                    String wikidataId = (String) KBEnvironment.deserialize(key);
                    //transaction = environment.createWriteTransaction();
                    // we got the statement Id, just collect the Ids begin with 'Q'
                    if (wikidataId.startsWith("Q")) {
                        List<Statement> value = (List<Statement>) KBEnvironment.deserialize(entry.getValue());
                        // prediction
                        String predictedClass = classPredictor.predict(wikidataId, value).getPredictedClass();
                        //System.out.println("Wikidata Id: " + wikidataId + "; predicted class: " + predictedClass);

                        db.put(transaction, KBEnvironment.serialize(wikidataId), KBEnvironment.serialize(predictedClass));
                        counter++;
                        // show the message every 10000 elements been stored in the database
                        if (counter > 0 && counter % 10000 == 0) {
                            System.out.println(counter + " : 10000 elements have been classified and loaded in "
                                    + TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + " seconds.");
                            start = System.nanoTime();
                        }
                        transaction.commit();
                        transaction.close();
                    }
                } catch (Exception e) {
                    LOGGER.error("Error when writing the database.", e);
                }
            }
            System.out.println(counter + " elements have been stored into " + getName() + " database");
        } catch (Exception e) {
            LOGGER.error("Error when reading the database.", e);
        } finally {
            if (kbIterator != null)
                kbIterator.close();
            isLoaded = true;
        }
    }

    public static void main(String[] args) {
        // for building database
        UpperKnowledgeBase upperKnowledgeBase = UpperKnowledgeBase.getInstance();
        upperKnowledgeBase.close();
    }
}
