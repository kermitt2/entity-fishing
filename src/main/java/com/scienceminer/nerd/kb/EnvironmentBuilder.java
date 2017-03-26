package com.scienceminer.nerd.kb;

import java.io.File;

import com.scienceminer.nerd.kb.db.*;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;

/*
 * Initialize LMDB and categories for a given pre-processed Wikipedia
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
        
        WikipediaConfiguration conf = new WikipediaConfiguration(confFile);
        Wikipedia wikipedia = new Wikipedia(conf, false);

        // mapping wikipedia categories / domains and domain assigments for all pageid
        if (lang.equals("en")) {
            System.out.println("Generating domain for all Wikipedia articles...");
            WikipediaDomainMap wikipediaDomainMap = new WikipediaDomainMap("en", conf.getDatabaseDirectory().getPath());
            try {
                wikipediaDomainMap.setWikipedia(wikipedia);
                wikipediaDomainMap.setLang(lang);
                wikipediaDomainMap.openCache();
                wikipediaDomainMap.createAllMappings();
            } finally {
                wikipediaDomainMap.saveCache();
                wikipediaDomainMap.close();
            }
        }
        wikipedia.close();
    }
    
}
