package com.scienceminer.nerd.utilities;

import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import org.apache.pdfbox.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Class for managing stopword lists in several languages and associated methods.
 *
 */
public class Stopwords {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(Stopwords.class);
    
    private static volatile Stopwords instance;

    // stopword map per language
    private Map<String, Set<String>> allStopwords = null;

    // fast matchers
    //private Map<String, FastMatcher> allMatcherPrefix = null;
    //private Map<String, FastMatcher> allMatcherSuffix = null;

    // list of languages coming with a stopword list
    private List<String> languages = UpperKnowledgeBase.TARGET_LANGUAGES;

    public static Stopwords getInstance() {
        if (instance == null) {
            //double check idiom
            // synchronized (instanceController) {
                if (instance == null)
                    getNewInstance();
            // }
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance()  {
        LOGGER.debug("Get new instance of Stopwords");
        instance = new Stopwords();
    }
    
    /**
     * Hidden constructor 
     */
    private Stopwords()  {
        loadStopWords();        
    }

    public void loadStopWords() {
        // load the stop words
        for(String lang : languages) {
            Set<String> stopwords = null;
            BufferedReader br0 = null;
            try {
                stopwords = new HashSet<String>(); 
                br0 = new BufferedReader(
                    new InputStreamReader(new FileInputStream("data/stopwords/" + lang + ".txt"), "UTF8"));     
                String s0;
                while((s0 = br0.readLine()) != null) {
                    if (s0.trim().length() == 0) 
                        continue;
                    stopwords.add(s0.trim());
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            finally {
                IOUtils.closeQuietly(br0);
            }
            if ( (stopwords != null) && (stopwords.size() > 0) ) {
                if (allStopwords == null)
                    allStopwords = new HashMap<>();
                allStopwords.put(lang, stopwords);
            }

            // create fast matcher for the stopwords prefixes/suffixes
            /*FastMatcher matcherPrefix = new FastMatcher();
            for(String stopword : stopwords) {
                matcherPrefix.loadTerm(stopword+" ");
            }

            if (allMatcherPrefix == null) {
                allMatcherPrefix = new HashMap<String, FastMatcher>();
            }
            allMatcherPrefix.add(matcherPrefix);

            FastMatcher matcherSuffix = new FastMatcher();
            for(String stopword : stopwords) {
                matcherPrefix.loadTerm(" " + stopword);
            }

            if (allMatcherSuffix == null) {
                allMatcherSuffix = new HashMap<String, FastMatcher>();
            }
            allMatcherSuffix.add(matcherSuffix);*/
        }
    }

    /**
     *  Check efficiently if a complex term starts with a stop word
     */
    public boolean startsWithStopword(String term, String lang) {
        if (term == null)
            return false;
        boolean result = false;
        term = term.trim();
        Set<String> stopwords = allStopwords.get(lang);
        int ind = term.indexOf(" ");
        if (ind == -1)
            ind = term.indexOf("'");
        if (ind == -1) {
            if ( (stopwords != null) && (stopwords.contains(term)) )
                return true;
            else 
                return false;
        } else if (stopwords != null) {
            String subterm = term.substring(0, ind);
            if (stopwords.contains(subterm))
                return true;
            else 
                return false; 
        }
        return result;
    }

    /**
     *  Check efficiently if a complex term ends with a stop word
     */
    public boolean endsWithStopword(String term, String lang) {
        if (term == null)
            return false;
        boolean result = false;
        term = term.trim();
        Set<String> stopwords = allStopwords.get(lang);
        int ind = term.lastIndexOf(" ");
        if (ind == -1)
            ind = term.lastIndexOf("'");
        if (ind == -1) {
            if ( (stopwords != null) && (stopwords.contains(term)) )
                return true;
            else 
                return false;
        } else if (stopwords != null) {
            String subterm = term.substring(ind+1, term.length());
            if (stopwords.contains(subterm))
                return true;
            else 
                return false; 
        }
        return result;
    }

	public boolean isStopword(String text, String lang) {
		Set<String> stopwords = allStopwords.get(lang);
		return stopwords.contains(text.trim());
	}
}