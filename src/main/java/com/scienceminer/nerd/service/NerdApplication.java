package com.scienceminer.nerd.service;

import com.google.inject.Module;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.servlets.CrossOriginFilter;
//import com.scienceminer.nerd.service.configuration.NerdServiceConfiguration;
import io.dropwizard.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import java.util.Arrays;
import java.util.EnumSet;

public class NerdApplication extends Application<Configuration> {
    private static final String RESOURCES = "/service";

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdApplication.class);

    public static void main(String[] args) throws Exception {
        new NerdApplication().run(args);
    }

    @Override
    public String getName() {
        return "entity-fishing";
    }

    private Iterable<? extends Module> getGuiceModules() {
        return Arrays.asList(new NerdServiceModule());
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        GuiceBundle<Configuration> guiceBundle = GuiceBundle.defaultBuilder(Configuration.class)
                .modules(getGuiceModules())
                .build();
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new AssetsBundle("/web", "/", "index.html", "assets"));
        //bootstrap.addCommand(new CreateTrainingCommand());
    }

    @Override
    public void run(Configuration configuration, Environment environment) {

        new DropwizardExports(environment.metrics()).register();
        ServletRegistration.Dynamic registration = environment.admin().addServlet("Prometheus", new MetricsServlet());
        registration.addMapping("/metrics/prometheus");

        /*String allowedOrigins = configuration.getCorsAllowedOrigins();
        String allowedMethods = configuration.getCorsAllowedMethods();
        String allowedHeaders = configuration.getCorsAllowedHeaders();*/

        // Enable CORS headers
        final FilterRegistration.Dynamic cors =
            environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        /*cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, allowedOrigins);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, allowedMethods);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, allowedHeaders);*/

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, RESOURCES + "/*");

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        environment.jersey().setUrlPattern(RESOURCES + "/*");
    }
}
