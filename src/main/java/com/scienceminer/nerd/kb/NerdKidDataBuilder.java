package com.scienceminer.nerd.kb;

/* build the LMDB databases for Nerd-Kid environment into the CSV files
* */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.utilities.NerdConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.fusesource.lmdbjni.Env;
import org.fusesource.lmdbjni.Transaction;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NerdKidDataBuilder {
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
                System.out.println(property + value);
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
                System.out.println("Features no value: "+property);
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
    public static void main(String[] args) throws Exception {
        NerdKidDataBuilder nerdKidDataBuilder = new NerdKidDataBuilder();
        nerdKidDataBuilder.loadFeatures();

    }
}
