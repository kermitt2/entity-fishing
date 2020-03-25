package com.scienceminer.nerd.kid;

import au.com.bytecode.opencsv.CSVWriter;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.kb.db.NerdKidDatabase;
import org.nerd.kid.service.NerdKidPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WikiIdRandomizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiIdRandomizer.class);

    public List<String> randomItemUnknown() {
        String label = null;
        Random random = new Random();
        List<String> ids = new ArrayList<>();
        UpperKnowledgeBase target = UpperKnowledgeBase.getInstance();
        int i = 0;
        while (i < 100) {
            // take any random Id
            int randomId = random.nextInt(35030908) + 1;
            String wikiId = "Q" + String.valueOf(randomId);
            // get the predicted label of random id
            label = target.getPredictedClassByWikidataId(wikiId);
            if (label == null) {
                continue;
            }else{
                if (label.equals("UNKNOWN")) {
                    ids.add(wikiId);
                    i++;
                }
            }
        }
        return ids;
    }

    public void writeToCsv(List<String> result) throws IOException {
        String fileOutput = "data/nerdKid/UnknownWikiIds.csv";
        CSVWriter csvWriter = null;
        List<String> wikiIds = new ArrayList<String>();
        String[] header = {"WikidataID,Class"};
        try {
            csvWriter = new CSVWriter(new FileWriter(fileOutput), '\n', CSVWriter.NO_QUOTE_CHARACTER);
            csvWriter.writeNext(header);
            if (result != null) {

                for (String id : result) {
                    wikiIds.add(id);
                    System.out.println(id + ",UNKNOWN");
                }
                csvWriter.writeNext(wikiIds.toArray(new String[wikiIds.size()]));
            }
        } catch (IOException e) {
            LOGGER.error("Error when creating a CSV file.", e);
        } finally {
            csvWriter.flush();
            csvWriter.close();
        }
    }

    public static void main(String[] args) throws IOException {
        WikiIdRandomizer wikiIdRandomizer = new WikiIdRandomizer();
        List<String> randomizedWikidataId = wikiIdRandomizer.randomItemUnknown();
        wikiIdRandomizer.writeToCsv(randomizedWikidataId);
    }
}
