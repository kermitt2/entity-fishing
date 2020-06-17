package com.scienceminer.nerd.main.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SoftwareInfo {
    private String name;
    private String version;
    private String description;

    private static final Logger LOGGER = LoggerFactory.getLogger(SoftwareInfo.class);

    private static SoftwareInfo INSTANCE = null;

    private SoftwareInfo(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    public static SoftwareInfo getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        //Default in case of issues of any nature
        INSTANCE = new SoftwareInfo("entity-fishing", "N/A", "Entity Recognition and Disambiguation");
        
        Properties properties = new Properties();
        try {
            properties.load(SoftwareInfo.class.getResourceAsStream("/service.properties"));
            INSTANCE = new SoftwareInfo(properties.getProperty("name"), properties.getProperty("version"), properties.getProperty("description"));
        } catch (Exception e) {
            LOGGER.error("General error when extracting the version of this application", e);
        }

        return INSTANCE; 
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"name\": \"" + this.getName() + "\"")
                .append(", \"version\": \"" + this.getVersion() + "\"")
                .append(", \"description\": \"" + this.getDescription() + "\"")
                .append("}");
        return sb.toString();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


}
