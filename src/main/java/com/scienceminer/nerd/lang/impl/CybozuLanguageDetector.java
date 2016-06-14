package com.scienceminer.nerd.lang.impl;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.scienceminer.nerd.lang.Language;
import com.scienceminer.nerd.lang.LanguageDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Implementation of a language identifier based on LingPipe. 
 *
 * @author Patrice Lopez
 */
public class CybozuLanguageDetector implements LanguageDetector {
    private static final Logger LOGGER  = LoggerFactory.getLogger(CybozuLanguageDetector.class);
    @Override
    public Language detect(String text) {
        Detector detector;
        try {
            detector = DetectorFactory.create();
            detector.append(text);
            ArrayList<com.cybozu.labs.langdetect.Language> probabilities = detector.getProbabilities();
            if (probabilities == null || probabilities.isEmpty()) {
                return null;
            }

            //System.out.println(probabilities);
            com.cybozu.labs.langdetect.Language l = probabilities.get(0);

            return new Language(l.lang, l.prob);
        } catch (LangDetectException e) {
            LOGGER.error("Cannot detect language", e);
            return null;
        }

    }
}
