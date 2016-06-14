package com.scienceminer.nerd.kb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.lang.Language;
import com.scienceminer.nerd.utilities.NerdProperties;
import org.grobid.core.utilities.OffsetPosition;

import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managing all the lexical resources.
 *
 * @author Patrice Lopez
 */
public class Lexicon {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(Lexicon.class);
    // private static volatile Boolean instanceController = false;
    private static volatile Lexicon instance;

	// we use map for multilingual resources, where the language code is the key
    private Map<String, Set<String>> dictionaries = null;
	private Map<String, Wikipedia> wikipedias = null;
    private Map<String, WikipediaDomainMap> wikipediaDomainMaps = null;
    private Map<String, FreeBaseTypeMap> freeBaseTypeMaps = null;

    public static Lexicon getInstance() {
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
	private static synchronized void getNewInstance() {
		LOGGER.debug("Get new instance of Lexicon");
		NerdProperties.getInstance();
		instance = new Lexicon();
	}

    /**
     * Hidden constructor
     */
    private Lexicon() {
        initDictionary();
		
		//WikipediaConfiguration conf = 
		//	new WikipediaConfiguration(new File(NerdProperties.getInstance().getWikipediaMinerConfigPath()));
		try {
			LOGGER.info("Initiating Wikipedia DBs");
			wikipedias = new HashMap<String, Wikipedia>(); 
            wikipediaDomainMaps = new HashMap<String,WikipediaDomainMap>();
            freeBaseTypeMaps = new HashMap<String,FreeBaseTypeMap>();
			
			WikipediaConfiguration conf = 
				new WikipediaConfiguration(new File("data/wikipedia/wikipedia-en.xml"));
			Wikipedia wikipedia_en = new Wikipedia(conf, true); // with distinct thread for accessing data
			while(!wikipedia_en.isReady()) {
				Thread.sleep(1000);
			}
			wikipedias.put(Language.EN, wikipedia_en);
            
            WikipediaDomainMap wikipediaDomainMaps_en = new WikipediaDomainMap();
            wikipediaDomainMaps_en.setLang(Language.EN);
            wikipediaDomainMaps_en.setWikipedia(wikipedia_en);
            wikipediaDomainMaps_en.openUse();
            wikipediaDomainMaps.put(Language.EN, wikipediaDomainMaps_en);
            
            /*FreeBaseTypeMap freeBaseTypeMaps_en = new FreeBaseTypeMap();
            freeBaseTypeMaps_en.setLang(Language.EN);
            freeBaseTypeMaps_en.setWikipedia(wikipedia_en);
            freeBaseTypeMaps_en.openUse();
            freeBaseTypeMaps.put(Language.EN, freeBaseTypeMaps_en);*/
			
			conf = new WikipediaConfiguration(new File("data/wikipedia/wikipedia-de.xml"));
			Wikipedia wikipedia_de = new Wikipedia(conf, true); // with distinct thread for accessing data
			while(!wikipedia_de.isReady()) {
				Thread.sleep(1000);
			}
			wikipedias.put(Language.DE, wikipedia_de);
            
            /*WikipediaDomainMap wikipediaDomainMaps_de = new WikipediaDomainMap();
            wikipediaDomainMaps_de.setLang(Language.DE);
            wikipediaDomainMaps_de.setWikipedia(wikipedia_de);
            wikipediaDomainMaps_de.openUse();*/
            wikipediaDomainMaps.put(Language.DE, wikipediaDomainMaps_en);

            /*FreeBaseTypeMap freeBaseTypeMaps_de = new FreeBaseTypeMap();
            freeBaseTypeMaps_de.setLang(Language.DE);
            freeBaseTypeMaps_de.setWikipedia(wikipedia_de);
            freeBaseTypeMaps_de.openUse();
            freeBaseTypeMaps.put(Language.DE, freeBaseTypeMaps_de);*/
			
			conf = new WikipediaConfiguration(new File("data/wikipedia/wikipedia-fr.xml"));
			Wikipedia wikipedia_fr = new Wikipedia(conf, true); // with distinct thread for accessing data
			while(!wikipedia_fr.isReady()) {
				Thread.sleep(1000);
			}
			wikipedias.put(Language.FR, wikipedia_fr);

            /*WikipediaDomainMap wikipediaDomainMaps_fr = new WikipediaDomainMap();
            wikipediaDomainMaps_fr.setLang(Language.FR);
            wikipediaDomainMaps_fr.setWikipedia(wikipedia_fr);
            wikipediaDomainMaps_fr.openUse();*/
            wikipediaDomainMaps.put(Language.FR, wikipediaDomainMaps_en);

            /*FreeBaseTypeMap freeBaseTypeMaps_fr = new FreeBaseTypeMap();
            freeBaseTypeMaps_fr.setLang(Language.FR);
            freeBaseTypeMaps_fr.setWikipedia(wikipedia_fr);
            freeBaseTypeMaps_fr.openUse();
            freeBaseTypeMaps.put(Language.FR, freeBaseTypeMaps_fr);*/
			
			LOGGER.info("End of Initialization of Wikipedia DBs");
		}
		catch(Exception e) {
			LOGGER.debug(e.getMessage());
		}
        //initNames();
		// the loading of the journal and conference names is lazy
        //addDictionary(dictionaries, NerdProperties.getNerdHomePath() + "/wordforms/english.wf", Language.EN);
        //addDictionary(dictionaries, NerdProperties.getNerdHomePath() + "/wordforms/german.wf", Language.EN);

		//initTermino();

        //initCountryCodes();
        //addCountryCodes(NerdProperties.getNerdHomePath() + "/lexicon/countries/CountryCodes.xml");
    }

    private void initDictionary() {
    	LOGGER.info("Initiating dictionaries");
		dictionaries = new HashMap<String, Set<String>>();
        Set<String> dictionary_en = new HashSet<String>();
		dictionaries.put(Language.EN, dictionary_en);
        Set<String> dictionary_de = new HashSet<String>();
		dictionaries.put(Language.DE, dictionary_de);
        Set<String> dictionary_fr = new HashSet<String>();
		dictionaries.put(Language.FR, dictionary_fr);
        LOGGER.info("End of Initialization of dictionaries");
    }

    public final void addDictionary(String path, String lang) {
        File file = new File(path);
        if (!file.exists()) {
            throw new NerdResourceException("Cannot add entries to dictionary (language '" + lang +
                    "'), because file '" + file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new NerdResourceException("Cannot add entries to dictionary (language '" + lang +
                    "'), because cannot read file '" + file.getAbsolutePath() + "'.");
        }
        InputStream ist = null;
        InputStreamReader isr = null;
        BufferedReader dis = null;
		
		Set<String> dictionary_en = dictionaries.get(Language.EN);
		Set<String> dictionary_fr = dictionaries.get(Language.FR);
		Set<String> dictionary_de = dictionaries.get(Language.DE);
        try {
            //if (NerdProperties.isResourcesInHome())
                ist = new FileInputStream(file);
            //else
            //    ist = getClass().getResourceAsStream(path);


            isr = new InputStreamReader(ist, "UTF8");
            dis = new BufferedReader(isr);

            String l = null;
            while ((l = dis.readLine()) != null) {
                if (l.length() == 0) continue;
                // the first token, separated by a tabulation, gives the word form
                if (lang.equals(Language.EN)) {
                    // multext format
                    StringTokenizer st = new StringTokenizer(l, "\t");
                    if (st.hasMoreTokens()) {
                        String word = st.nextToken();
                        if (!dictionary_en.contains(word))
                            dictionary_en.add(word);
                    }
                } else if (lang.equals(Language.DE)) {
                    // celex format
                    StringTokenizer st = new StringTokenizer(l, "\\");
                    if (st.hasMoreTokens()) {
                        st.nextToken(); // id
                        String word = st.nextToken();
                        word = word.replace("\"a", "ä");
                        word = word.replace("\"u", "ü");
                        word = word.replace("\"o", "ö");
                        word = word.replace("$", "ß");
                        if (!dictionary_de.contains(word))
                            dictionary_de.add(word);
                    }
                }
            }
        } catch (FileNotFoundException e) {
//	    	e.printStackTrace();
            throw new NerdException("An exception occured while running Nerd.", e);
        } catch (IOException e) {
//	    	e.printStackTrace();
            throw new NerdException("An exception occured while running Nerd.", e);
        } finally {
            try {
                if (ist != null)
                    ist.close();
                if (isr != null)
                    isr.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new NerdResourceException("Cannot close all streams.", e);
            }
        }
    }

    /**
     * Lexical look-up, default is English
     * @param s a string to test
     * @return true if in the dictionary
     */
    public boolean inDictionary(String s) {
        return inDictionary(s, Language.EN);
    }

    public boolean inDictionary(String s, String lang) {
		Set<String> dictionary_en = dictionaries.get(Language.EN);
		Set<String> dictionary_fr = dictionaries.get(Language.FR);
		Set<String> dictionary_de = dictionaries.get(Language.DE);
        if (s == null)
            return false;
        if ((s.endsWith(".")) | (s.endsWith(",")) | (s.endsWith(":")) | (s.endsWith(";")) | (s.endsWith(".")))
            s = s.substring(0, s.length() - 1);
        int i1 = s.indexOf('-');
        int i2 = s.indexOf(' ');
        if (i1 != -1) {
            String s1 = s.substring(0, i1);
            String s2 = s.substring(i1 + 1, s.length());
            if (lang.equals(Language.DE)) {
                if ((dictionary_de.contains(s1)) & (dictionary_de.contains(s2)))
                    return true;
                else
                    return false;
            } else {
                if ((dictionary_en.contains(s1)) & (dictionary_en.contains(s2)))
                    return true;
                else
                    return false;
            }
        }
        if (i2 != -1) {
            String s1 = s.substring(0, i2);
            String s2 = s.substring(i2 + 1, s.length());
            if (lang.equals(Language.DE)) {
                if ((dictionary_de.contains(s1)) & (dictionary_de.contains(s2)))
                    return true;
                else
                    return false;
            } else {
                if ((dictionary_en.contains(s1)) & (dictionary_en.contains(s2)))
                    return true;
                else
                    return false;
            }
        } else {
            if (lang.equals(Language.DE)) {
                return dictionary_de.contains(s);
            } else {
                return dictionary_en.contains(s);
            }
        }
    }

    /**
     * Indicate if we have a punctuation
     */
    public boolean isPunctuation(String s) {
        if (s.length() != 1)
            return false;
        else {
            char c = s.charAt(0);
            if ((!Character.isLetterOrDigit(c)) & !(c == '-'))
                return true;
        }
        return false;
    }

	public Wikipedia getWikipediaConf(String lang) {
		return wikipedias.get(lang);
	}
	
	public Map<String, Wikipedia> getWikipediaConfs() {
		return wikipedias;
	}

    public Map<String, WikipediaDomainMap> getWikipediaDomainMaps () {
        return wikipediaDomainMaps;
    }

    public Map<String, FreeBaseTypeMap> getFreeBaseTypeMaps () {
        return freeBaseTypeMaps;
    }
}
