package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kid.NERTypePredictor;
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
 * Class for Wikidata entities might be one of these:
 * - Named-entities :   PERSON, LOCATION, ORGANIZATION, ACRONYM, ANIMAL, ARTIFACT, BUSINESS, INSTITUTION,
 * MEASURE, AWARD, CONCEPT, CONCEPTUAL, CREATION, EVENT, LEGAL, IDENTIFIER, INSTALLATION,
 * MEDIA, NATIONAL, SUBSTANCE, PLANT, PERIOD, TITLE, PERSON_TYPE, WEBSITE, SPORT_TEAM, UNKNOWN
 * - Non-named entities : OTHER;
 */

public class NerdKidDatabase extends StringRecordDatabase<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdKidDatabase.class);

    private NERTypePredictor nerTypePredictor;

    public NerdKidDatabase(KBEnvironment env) {
        super(env, DatabaseType.nerdKid);
        nerTypePredictor = new NERTypePredictor();
    }

    @Override
    public KBEntry<String, String> deserialiseCsvRecord(
            CsvRecordInput record) {
        throw new UnsupportedOperationException();

    }

    /*
     * build nerdKid database by reading the features directly from the statement database
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
        try (Transaction transaction = environment.createWriteTransaction()) {
            long start = System.nanoTime();
            /*int numAcronym = 0, numAnimal = 0, numArtifact = 0, numAward = 0,
                    numBusiness = 0, numConcept = 0, numConceptual = 0, numCreation = 0,
                    numEvent = 0, numIdentifier = 0, numInstallation = 0, numInstitution = 0,
                    numLegal = 0, numLocation = 0, numMeasure = 0, numMedia = 0, numNational = 0,
                    numOrganisation = 0, numPeriod = 0, numPerson = 0, numPersonType = 0, numPlant = 0,
                    numSportTeam = 0, numSubstance = 0, numTitle = 0, numUnknown = 0, numWebsite = 0, numOther = 0;*/
            // while there are some data inside the database
            while (kbIterator.hasNext()) {
                Entry entry = kbIterator.next();
                byte[] key = entry.getKey();
                // it's better to put the create write transaction in the try to avoid writing error bug and the database won't be created
                // we have wikidataId (the wikidata Id can be get by accessing the key's statement or the concept Id of statements)
                String wikidataId = (String) KBEnvironment.deserialize(key);
                //transaction = environment.createWriteTransaction();
                // we got the statement Id, just collect the Ids begin with 'Q'
                if (wikidataId.startsWith("Q")) {
                    // the features for prediction collected from the statement LMDB
                    List<Statement> features = (List<Statement>) KBEnvironment.deserialize(entry.getValue());
                    // prediction
                    String predictedClass = nerTypePredictor.predict(wikidataId, features).getPredictedClass();
                    //System.out.println("Wikidata Id: " + wikidataId + "; predicted class: " + predictedClass);
                    /*if (predictedClass.equals("ACRONYM")) {
                        numAcronym++;
                    } else if (predictedClass.equals("ANIMAL")) {
                        numAnimal++;
                    } else if (predictedClass.equals("ARTIFACT")) {
                        numArtifact++;
                    } else if (predictedClass.equals("AWARD")) {
                        numAward++;
                    } else if (predictedClass.equals("BUSINESS")) {
                        numBusiness++;
                    } else if (predictedClass.equals("CONCEPT")) {
                        numConcept++;
                    } else if (predictedClass.equals("CONCEPTUAL")) {
                        numConceptual++;
                    } else if (predictedClass.equals("CREATION")) {
                        numCreation++;
                    } else if (predictedClass.equals("EVENT")) {
                        numEvent++;
                    } else if (predictedClass.equals("IDENTIFIER")) {
                        numIdentifier++;
                    } else if (predictedClass.equals("INSTALLATION")) {
                        numInstallation++;
                    } else if (predictedClass.equals("INSTITUTION")) {
                        numInstitution++;
                    } else if (predictedClass.equals("LEGAL")) {
                        numLegal++;
                    } else if (predictedClass.equals("LOCATION")) {
                        numLocation++;
                    } else if (predictedClass.equals("MEASURE")) {
                        numMeasure++;
                    } else if (predictedClass.equals("MEDIA")) {
                        numMedia++;
                    } else if (predictedClass.equals("NATIONAL")) {
                        numNational++;
                    } else if (predictedClass.equals("ORGANISATION")) {
                        numOrganisation++;
                    } else if (predictedClass.equals("PERIOD")) {
                        numPeriod++;
                    } else if (predictedClass.equals("PERSON")) {
                        numPerson++;
                    } else if (predictedClass.equals("PERSON_TYPE")) {
                        numPersonType++;
                    } else if (predictedClass.equals("PLANT")) {
                        numPlant++;
                    } else if (predictedClass.equals("SPORT_TEAM")) {
                        numSportTeam++;
                    } else if (predictedClass.equals("SUBSTANCE")) {
                        numTitle++;
                    } else if (predictedClass.equals("TITLE")) {
                        numTitle++;
                    } else if (predictedClass.equals("UNKNOWN")) {
                        numUnknown++;
                    } else if (predictedClass.equals("WEBSITE")) {
                        numWebsite++;
                    } else if (predictedClass.equals("OTHER")) {
                        numOther++;
                    }*/
                    db.put(transaction, KBEnvironment.serialize(wikidataId), KBEnvironment.serialize(predictedClass));
                    counter++;
                    // show the message every 10000 elements been stored in the database
                    if (counter > 0 && counter % 10000 == 0) {
                        System.out.println(counter + " : 10000 elements have been classified and loaded in "
                                + TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + " seconds.");
                        start = System.nanoTime();
                        System.out.println(counter + " elements have been stored into " + getName() + " database");
                    }
                }
            }
            /*System.out.println("ACRONYM : " + numAcronym + " ;ANIMAL : " + numAnimal +
                            ";ARTIFACT : " + numArtifact +  ";AWARD : " + numAward +
                            ";BUSINESS : " + numBusiness + ";CONCEPT : " + numConcept +
                            ";CONCEPTUAL : " + numConceptual + ";CREATION" + numCreation +
                            ";EVENT : " + numEvent + ";IDENTIFIER : " + numIdentifier +
                            ";INSTALLATION : " + numInstallation + ";INSTITUTION : " + numInstitution +
                            ";LEGAL : " + numLegal + ";LOCATION : " + numLocation + ";MEASURE : " + numMeasure +
                            ";MEDIA : " + numMedia + ";NATIONAL : " + numNational + ";ORGANISATION : " + numOrganisation+
                            ";PERIOD : " + numPeriod + ";PERSON : " + numPerson + ";PERSON_TYPE : " + numPersonType+
                            ";PLANT : " + numPlant + ";SPORT_TEAM : " + numSportTeam + ";SUBSTANCE" + numSubstance +
                            ";TITLE : " + numTitle + ";UNKNOWN : " + numUnknown + ";WEBSITE : " + numWebsite +
                            ";OTHER : " + numOther);*/

            transaction.commit();
            transaction.close();
        } catch (Exception e) {
            LOGGER.error("Error when reading the database.", e);
        } finally {
            if (kbIterator != null)
                kbIterator.close();
            isLoaded = true;
        }
    }

    public static void main(String[] args) {
        // for building nerdKid database if it doesn't exist yet
        UpperKnowledgeBase upperKnowledgeBase = UpperKnowledgeBase.getInstance();
        upperKnowledgeBase.close();
    }
}
