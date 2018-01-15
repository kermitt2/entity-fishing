package com.scienceminer.nerd.kb;

import java.io.File;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.*;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.utilities.NerdConfig;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.grobid.core.lang.Language;

/*
 * Initialize LMDB and categories for a given pre-processed Wikipedia
 * 
 * ******* Normally ths is deprecated! **********
 */
public class EnvironmentBuilder {

    public static void main(String args[]) throws Exception {
        
        if (args.length != 1) {
            System.out.println("Please specify path to wikipedia configuration file") ;
            System.exit(1);
        }
        
        File confFile = new File(args[0]);
        if (!confFile.canRead()) {
            System.out.println("'" + args[0] + "' cannot be read");
            System.exit(1);
        }

        int ind = args[0].lastIndexOf(".");
        if (ind == -1){
            System.out.println("Language for file '" + args[0] + "' cannot be read");
            System.exit(1);
        }
            
        String lang = args[0].substring(ind-2,ind);
        System.out.println("Language is " + lang);
        
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        NerdConfig conf = mapper.readValue(confFile, NerdConfig.class);
        // this will build the LMDB databases if not available (so the first time) form the CSV files
        LowerKnowledgeBase wikipedia = new LowerKnowledgeBase(conf);

        // mapping wikipedia categories / domains and domain assigments for all pageid
        if (lang.equals(Language.EN)) {
            System.out.println("Generating domain for all Wikipedia articles...");
            WikipediaDomainMap wikipediaDomainMap = new WikipediaDomainMap(Language.EN, conf.getDbDirectory());
            try {
                wikipediaDomainMap.setWikipedia(wikipedia);
                wikipediaDomainMap.setLang(lang);
                //wikipediaDomainMap.openCache();
                wikipediaDomainMap.createAllMappings();
            } finally {
                //wikipediaDomainMap.saveCache();
                wikipediaDomainMap.close();
            }
        }
        wikipedia.close();
    }
    
}
