package com.scienceminer.nerd.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
//import com.scienceminer.nerd.service.configuration.NerdServiceConfiguration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("health")
@Singleton
@Produces(APPLICATION_JSON)
public class HealthCheck extends com.codahale.metrics.health.HealthCheck {

    @Inject
    public HealthCheck() {
    }

    @GET
    public Response alive() {
        return Response.ok().build();
    }

    @Override
    protected Result check() throws Exception {
        return Result.healthy();
        //Result.unhealthy("Problem description...");
    }
}
