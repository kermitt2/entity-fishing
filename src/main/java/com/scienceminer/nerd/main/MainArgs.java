package com.scienceminer.nerd.main;

/**
 * Class containing args of the command line {@link Main}.
 *
 * @author Patrice
 */
public class MainArgs {

    private String input;
	
	private String output;
	
    private String processMethodName;
	
    private String path2grobidHome;

    private String path2grobidProperty;

	private String nbThreads = "1"; // default
	
	private String lang;

    private String quantizer;

    private boolean hashheader = false; // header in vector file is absent, when relevant

    private String error = "0.01"; // default error margin, when relevant

    private boolean w2v = false; // default, not use this format for output
	
    private String rho = "-1";

    private String max = "-1";

    private String vectorFile;

    /**
     * @return the processMethodName
     */
    public final String getProcessMethodName() {
        return this.processMethodName;
    }

    /**
     * @param pProcessMethodName the processMethodName to set
     */
    public final void setProcessMethodName(final String pProcessMethodName) {
        this.processMethodName = pProcessMethodName.toLowerCase();
    }
	
    /**
     * @return the input path
     */
    public final String getInput() {
        return this.input;
    }

    /**
     * @param pPathInputDirectory the input path to set
     */
    public final void setInput(final String pPathInputDirectory) {
        this.input = pPathInputDirectory;
    }

    /**
     * To set the path to Grobid home as parameter instead of a property
     *
     * @return the path2grobidHome
     */
    public final String getPath2grobidHome() {
        return this.path2grobidHome;
    }

    /**
     * @param pPath2grobidHome the path2grobidHome to set
     */
    public final void setPath2grobidHome(final String pPath2grobidHome) {
        this.path2grobidHome = pPath2grobidHome;
    }

    /**
     * To set the path to the Grobid property file as parameter instead of a property.
     *
     * @return the path2grobidProperty
     */
    public final String getPath2grobidProperty() {
        return this.path2grobidProperty;
    }

    /**
     * @param pPath2grobidProperty the path2grobidProperty to set
     */
    public final void setPath2grobidProperty(final String pPath2grobidProperty) {
        this.path2grobidProperty = pPath2grobidProperty;
    }
	
    public String getOutput() {
        return this.output;
    }

    public void setOutput(String path) {
        this.output = path;
    }
	
    public String getNbThreads() {
        return this.nbThreads;
    }

    public void setNbThreads(String nb) {
        this.nbThreads = nb;
    }

    public String getLang() {
        return this.lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getQuantizer() {
        return this.quantizer;
    }

    public void setQuantizer(String quantizer) {
        this.quantizer = quantizer;
    }

    public boolean getHashheader() {
        return this.hashheader;
    }

    public void setHashheader(boolean hash) {
        this.hashheader = hash;
    }

    public String getError() {
        return this.error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setW2v(Boolean w2v) {
        this.w2v = w2v;
    }

    public boolean getW2v() {
        return this.w2v;
    }

    public final String getVectorFile() {
        return this.vectorFile;
    }

    public final void setVectorFile(final String vectorFile) {
        this.vectorFile = vectorFile;
    }

    public String getRho() {
        return this.rho;
    }

    public void setRho(String rho) {
        this.rho = rho;
    }

    public String getMax() {
        return this.max;
    }

    public void setMax(String max) {
        this.max = max;
    }

}
