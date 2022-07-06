package com.scienceminer.nerd.service;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
//import com.scienceminer.nerd.service.configuration.NerdServiceConfiguration;
import io.dropwizard.Configuration;
import com.scienceminer.nerd.service.NerdRestService;
import com.scienceminer.nerd.service.HealthCheck;
import com.scienceminer.nerd.service.NerdRestProcessFile;
import com.scienceminer.nerd.service.NerdRestProcessQuery;
import com.scienceminer.nerd.service.NerdRestProcessGeneric;
import com.scienceminer.nerd.service.NerdRestProcessString;
import com.scienceminer.nerd.service.NerdRestCustomisation;
import com.scienceminer.nerd.service.NerdRestKB;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class NerdServiceModule extends DropwizardAwareModule<Configuration> {

    @Override
    public void configure(Binder binder) {
        // Generic modules
        binder.bind(HealthCheck.class);
        binder.bind(NerdRestProcessGeneric.class);

        // Core components
        binder.bind(NerdRestProcessQuery.class);
        binder.bind(NerdRestProcessFile.class);
        binder.bind(NerdRestProcessString.class);
        binder.bind(NerdRestCustomisation.class);
        binder.bind(NerdRestKB.class);

        // web services
        binder.bind(NerdRestService.class);
    }

    @Provides
    protected ObjectMapper getObjectMapper() {
        return getEnvironment().getObjectMapper();
    }

    @Provides
    protected MetricRegistry provideMetricRegistry() {
        return getMetricRegistry();
    }

    //for unit tests
    protected MetricRegistry getMetricRegistry() {
        return getEnvironment().metrics();
    }

    @Provides
    Client provideClient() {
        return ClientBuilder.newClient();
    }

}