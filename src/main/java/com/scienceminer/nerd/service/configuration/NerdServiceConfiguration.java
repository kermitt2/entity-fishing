package com.scienceminer.nerd.service.configuration;

import io.dropwizard.Configuration;
import com.scienceminer.nerd.utilities.NerdConfig;

public class NerdServiceConfiguration extends Configuration {

    //private NerdConfig nerdConfig;

    private String corsAllowedOrigins = "*";
    private String corsAllowedMethods = "OPTIONS,GET,PUT,POST,DELETE,HEAD";
    private String corsAllowedHeaders = "X-Requested-With,Content-Type,Accept,Origin";

    /*public NerdConfig getNerdConfig() {
        return this.nerdConfig;
    }

    public void setNerdConfig(NerdConfig conf) {
        this.nerdConfig = conf;
    }*/

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public String getCorsAllowedMethods() {
        return corsAllowedMethods;
    }

    public void setCorsAllowedMethods(String corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods;
    }

    public String getCorsAllowedHeaders() {
        return corsAllowedHeaders;
    }

    public void setCorsAllowedHeaders(String corsAllowedHeaders) {
        this.corsAllowedHeaders = corsAllowedHeaders;
    }

}
