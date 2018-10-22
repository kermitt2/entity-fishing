package com.scienceminer.nerd.main.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SoftwareInfo {
    private String name;
    private String version;
    private String description;

    private static final Logger LOGGER = LoggerFactory.getLogger(SoftwareInfo.class);


    public SoftwareInfo(String name, String version, String description){
        this.name = name;
        this.version = version;
        this.description = description;
    }

    public static SoftwareInfo getInstance() {
        String name = null, version = null, description = null;
        Properties properties = new Properties();
        try {
            properties.load(SoftwareInfo.class.getResourceAsStream("/service.properties"));
            name = properties.getProperty("name");
            version = properties.getProperty("version");
            description = properties.getProperty("description");
        }catch (Exception e){
            LOGGER.error("General error when extracting the version of this application", e);
        }
        return new SoftwareInfo(name, version, description);
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
