package com.scienceminer.nerd.service;

import com.scienceminer.nerd.kb.Lexicon;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.utilities.NerdServiceProperties;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

/**
 * RESTful service for the NERD system.
 */
@Singleton
@Path(NerdPaths.ROOT)
public class NerdRestService implements NerdPaths {
    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestService.class);

    private static final String SHA1 = "authToken";
    private static final String NAME = "name";
    private static final String PROFILE = "profile";
    private static final String VALUE = "value";
    private static final String QUERY = "query";
    private static final String XML = "xml";
    private static final String NERD = "nerd";
    private static final String TEXT = "text";
    private static final String TERM = "term";
    private static final String ID = "id";
    private static final String FILE = "file";
    private static final String LANG = "lang";
    private static final String ONLY_NER = "onlyNER";
    private static final String NBEST = "nbest";
    private static final String SENTENCE = "sentence";
    private static final String FORMAT = "format";
    private static final String CUSTOMISATION = "customisation";

    public NerdRestService() {
        LOGGER.info("Init Servlet NerdRestService.");
        NerdServiceProperties.getInstance();
        LOGGER.info("Init of Servlet NerdRestService finished.");

        LOGGER.info("Init lexicon.");
        Lexicon.getInstance();
        LOGGER.info("Init lexicon finished.");

        LOGGER.info("Init KB resources.");
        UpperKnowledgeBase.getInstance();
        LOGGER.info("Init KB resources finished.");
    }

    /**
     * @see com.scienceminer.nerd.service.NerdRestProcessGeneric#isAlive()
     */
    @GET
    @Path(NerdPaths.IS_ALIVE)
    @Produces(MediaType.TEXT_PLAIN)
    public Response isAlive() {
        return NerdRestProcessGeneric.isAlive();
    }

    /**
     * @see com.scienceminer.nerd.service.NerdRestProcessGeneric#getDescriptionAsHtml(UriInfo)
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path(NERD)
    public Response getDescription_html(@Context UriInfo uriInfo) {
        return NerdRestProcessGeneric.getDescriptionAsHtml(uriInfo);
    }

    /**
     * Sentence Segmentation
     **/

    @GET
    @Path(SEGMENTATION)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processSentenceSegmentationGet(@QueryParam(TEXT) String text) {
        return NerdRestProcessString.processSentenceSegmentation(text);
    }


    @POST
    @Path(SEGMENTATION)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response processSentenceSegmentationPost(@FormDataParam(TEXT) String text) {
        return NerdRestProcessString.processSentenceSegmentation(text);
    }

    /** Language Identification **/

    /**
     * @see com.scienceminer.nerd.service.NerdRestProcessString#processLanguageIdentification(String)
     */
    @GET
    @Path(LANGUAGE)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processLanguageIdentificationGet(@QueryParam(TEXT) String text) {
        return NerdRestProcessString.processLanguageIdentification(text);
    }

    /**
     * @see com.scienceminer.nerd.service.NerdRestProcessString#processLanguageIdentification(String)
     */
    @POST
    @Path(LANGUAGE)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response processLanguageIdentificationPost(@FormDataParam(TEXT) String text) {
        return NerdRestProcessString.processLanguageIdentification(text);
    }

    @POST
    @Path(DISAMBIGUATE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processQueryJson(@FormDataParam(QUERY) String query,
                                     @FormDataParam(FILE) InputStream inputStream) {
        if (inputStream != null) {
            return NerdRestProcessFile.processQueryAndPdfFile(query, inputStream);
        } else {
            return NerdRestProcessQuery.processQuery(query);
        }
    }

    /**
     * Same as processQueryJson when the user send only the query and can avoid using multipart/form-data
     */
    @POST
    @Path(DISAMBIGUATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processQueryJsonNoMultipart(String query) {
        return NerdRestProcessQuery.processQuery(query);
    }

    /*@POST
    @Path(DISAMBIGUATE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    public Response processQueryXml(@FormDataParam(QUERY) String query,
                                    @FormDataParam(FILE) InputStream inputStream) {
        return Response.status(new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return 501;
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.SERVER_ERROR;
            }

            @Override
            public String getReasonPhrase() {
                return "Not implemented";
            }
        }).build();
    }*/


    /**
     * Admin API
     **/

    @Path(ADMIN)
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response getAdmin_htmlGet(@QueryParam(SHA1) String sha1) {
        return NerdRestProcessAdmin.getAdminParams(sha1);
    }

    @Path(ADMIN_PROPERTIES)
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getAllProperties(@QueryParam(SHA1) String sha1) {
        return NerdRestProcessAdmin.getAllPropertiesValues(sha1);
    }

    @Path(ADMIN + "/property/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getProperty(@QueryParam(SHA1) String sha1, @PathParam(NAME) String propertyName) {
        return NerdRestProcessAdmin.getProperty(sha1, propertyName);
    }


    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(ADMIN + "/property/{name}")
    @PUT
    public Response updateProperty(@QueryParam(SHA1) String sha1,
                                   @PathParam(NAME) String propertyName,
                                   @FormDataParam(VALUE) String newValue) {
        return NerdRestProcessAdmin.changePropertyValue(sha1, propertyName, newValue);
    }

    /**
     * KB operations
     **/

    @Path(KB + "/" + CONCEPT + "/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getConceptInformation(@PathParam(ID) String identifier,
                                          @DefaultValue("en") @QueryParam(LANG) String lang) {
        return NerdRestKB.getConceptInfo(identifier, lang);
    }

    @GET
    @Path(KB + "/" + TERM + "/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTermLookup(@PathParam(TERM) String term,
                                  @DefaultValue("en") @QueryParam(LANG) String lang) {
        return NerdRestKB.getTermLookup(term, lang);
    }

    /**
     * Customisation API
     **/

    @GET
    @Path(CUSTOMISATIONS)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomisations() {
        return NerdRestCustomisation.processNerdCustomisations();
    }

    @GET
    @Path(CUSTOMISATION + "/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomisation(@PathParam(NAME) String name) {
        return NerdRestCustomisation.processNerdCustomisation(name);
    }

    @PUT
    @Path(CUSTOMISATION + "/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateCustomisation(@PathParam(NAME) String name, @FormDataParam(VALUE) String newContent) {
        return NerdRestCustomisation.updateCustomisation(name, newContent);
    }

    @POST
    @Path(CUSTOMISATIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addCustomisation(@FormDataParam(NAME) String name, @FormDataParam(VALUE) String content) {
        return NerdRestCustomisation.createNewCustomisation(name, content);
    }


    @Path(CUSTOMISATION + "/{name}")
    @DELETE
    public Response processDeleteNerdCustomisation(@PathParam(NAME) String name) {
        return NerdRestCustomisation.processDeleteNerdCustomisation(name);
    }

}
