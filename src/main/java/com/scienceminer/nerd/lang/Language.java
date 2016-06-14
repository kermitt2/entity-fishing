package com.scienceminer.nerd.lang;

import com.scienceminer.nerd.exceptions.NerdException;

/**
 * Class for representing a language associated to a text following some uncertainty. 
 * Contrary to the Grobid Language class, this version is mutable for Jackson JSON 
 * instanciation. 
 *
 * @author Patrice Lopez
 */ 

public final class Language {
    //common language constants
    public static final String EN = "en";
    public static final String DE = "de";
    public static final String FR = "fr";

    private String lang;
    private double conf;

	// default construction for jackson mapping
	public Language() {}

    public Language(String lang, double conf) {
        if (lang == null) {
            throw new NerdException("Language id cannot be null");
        }

        if ((lang.length() != 3 && 
			(lang.length() != 2) && 
			(!lang.equals("sorb"))) || 
			!(Character.isLetter(lang.charAt(0)) && Character.isLetter(lang.charAt(1)))) {
            throw new NerdException("Language id should consist of two or three letters, but was: " + lang);
        }

        this.lang = lang;
        this.conf = conf;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

	public String toJSON() {
		return "{\"lang\":\""+lang+"\", \"conf\": "+conf+"}";
	}

    public double getConf() {
        return conf;
    }

	public void setConf(double conf) {
		this.conf = conf;
	}

    public String toString() {
        return lang + ";" + conf;
    }
}
