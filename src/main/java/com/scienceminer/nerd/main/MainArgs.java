package com.scienceminer.nerd.main;

/**
 * Class containing args of the command line {@link Main}.
 *
 * @author Patrice
 */
public class MainArgs {

    private String pathInputDirectory;
	
	private String resultDirectoryPath;
	
    private String processMethodName;
	
    private String path2grobidHome;

    private String path2grobidProperty;

	private int nbThreads = 1; // default
	
	private String lang;
	
    /**
     * @return the processMethodName
     */
    public final String getProcessMethodName() {
        return processMethodName;
    }

    /**
     * @param pProcessMethodName the processMethodName to set
     */
    public final void setProcessMethodName(final String pProcessMethodName) {
        processMethodName = pProcessMethodName.toLowerCase();
    }
	
    /**
     * @return the path2InputDirectory
     */
    public final String getPathInputDirectory() {
        return pathInputDirectory;
    }

    /**
     * @param pPath2grobidHome the path2grobidHome to set
     */
    public final void setPathInputDirectory(final String pPathInputDirectory) {
        pathInputDirectory = pPathInputDirectory;
    }

    /**
     * To set the path to Grobid home as parameter instead of a property.
     *
     * @return the path2grobidHome
     */
    public final String getPath2grobidHome() {
        return path2grobidHome;
    }

    /**
     * @param pPath2grobidHome the path2grobidHome to set
     */
    public final void setPath2grobidHome(final String pPath2grobidHome) {
        path2grobidHome = pPath2grobidHome;
    }

    /**
     * To set the path to the Grobid property file as parameter instead of a property.
     *
     * @return the path2grobidProperty
     */
    public final String getPath2grobidProperty() {
        return path2grobidProperty;
    }

    /**
     * @param pPath2grobidProperty the path2grobidProperty to set
     */
    public final void setPath2grobidProperty(final String pPath2grobidProperty) {
        path2grobidProperty = pPath2grobidProperty;
    }
	
    public String getResultDirectoryPath() {
        return resultDirectoryPath;
    }

    public void setResultDirectoryPath(String path) {
        this.resultDirectoryPath = path;
    }
	
    public int getNbThreads() {
        return nbThreads;
    }

    public void setNbThreads(int nb) {
        this.nbThreads = nb;
    }

    public String getLang() {
        return this.lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
	
}
