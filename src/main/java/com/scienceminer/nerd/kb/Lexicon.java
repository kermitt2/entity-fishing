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

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.LanguageUtilities;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.utilities.Utilities;
import com.scienceminer.nerd.utilities.NerdConfig;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.LowerKnowledgeBase;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managing all the lexical resources used by NERD.
 *
 */
public class Lexicon {
	private static final Logger LOGGER = LoggerFactory.getLogger(Lexicon.class);
    private static volatile Lexicon instance;

	// we use map for multilingual resources, where the language code is the key
    private Map<String, Set<String>> dictionaries = null;

    public static Lexicon getInstance() {
        if (instance == null) {
			getNewInstance();
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
	private static synchronized void getNewInstance() {
		LOGGER.debug("Get new instance of Lexicon");
		//NerdProperties.getInstance();
		instance = new Lexicon();
	}

    /**
     * Hidden constructor
     */
    private Lexicon() {
        try {
            initDictionary();
		}
		catch(Exception e) {
			LOGGER.debug(e.getMessage());
            e.printStackTrace();
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
                if ((dictionary_de.contains(s1)) && (dictionary_de.contains(s2)))
                    return true;
                else
                    return false;
            } else {
                if ((dictionary_en.contains(s1)) && (dictionary_en.contains(s2)))
                    return true;
                else
                    return false;
            }
        }
        if (i2 != -1) {
            String s1 = s.substring(0, i2);
            String s2 = s.substring(i2 + 1, s.length());
            if (lang.equals(Language.DE)) {
                if ((dictionary_de.contains(s1)) && (dictionary_de.contains(s2)))
                    return true;
                else
                    return false;
            } else {
                if ((dictionary_en.contains(s1)) && (dictionary_en.contains(s2)))
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
            if ((!Character.isLetterOrDigit(c)) && !(c == '-'))
                return true;
        }
        return false;
    }
}
