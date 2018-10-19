package com.scienceminer.nerd.main.data;

import com.scienceminer.nerd.main.Main;

import java.io.IOException;
import java.util.Properties;

public class SoftwareInfo {

    private SoftwareInfo(String name, String version, String description) {
        this.name =
    }

    public static SoftwareInfo getInstance() {
        Properties properties = new Properties();

        try {
            properties.load(Main.class.getResourceAsStream("/service.properties"));

            String name = properties.getProperty("name");
            String version = properties.getProperty("version");
            String description = properties.getProperty("description");
            return new SoftwareInfo(name, version, description);
        }catch (IOException e){
            e.printStackTrace();

            //throw NerdException
        }

    }

    String name;

    String version;

    String description;

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
