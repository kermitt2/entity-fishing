package com.scienceminer.nerd.service;

import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.kb.Lexicon;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Query;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.util.NoSuchElementException;

/**
 * RESTFul service for the NERD system.
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

    NerdRestProcessQuery nerdProcessQuery;
    NerdRestProcessFile nerdProcessFile;

    public NerdRestService() {
        LOGGER.info("Init lexicon.");
        Lexicon.getInstance();
        LOGGER.info("Init lexicon finished.");

        LOGGER.info("Init KB resources.");
        UpperKnowledgeBase.getInstance();
        LOGGER.info("Init KB resources finished.");

        nerdProcessQuery = new NerdRestProcessQuery();
        nerdProcessFile = new NerdRestProcessFile();
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
        String json = null;
        Response response = null;

        try {

            if (inputStream != null) {
                json = nerdProcessFile.processQueryAndPdfFile(query, inputStream);
            } else {
                json = nerdProcessQuery.processQuery(query);
            }

            if (json == null) {
                response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(json)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

        } catch (QueryException qe) {
            return handleQueryException(qe, query);
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }

    /**
     * Same as processQueryJson when the user send only the query and can avoid using multipart/form-data
     */
    @POST
    @Path(DISAMBIGUATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processQueryJsonNoMultipart(String query) {
        String output = null;
        Response response = null;

        try {
            output = nerdProcessQuery.processQuery(query);

            if (output == null) {
                response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(output)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

        } catch (QueryException qe) {
            return handleQueryException(qe, query);
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }

    private Response handleQueryException(QueryException qe, String query) {
        Response response;
        if (qe.getReason().equals(QueryException.LANGUAGE_ISSUE)) {
            final String message = "The language specified is not supported or not valid. ";
            LOGGER.error(message, qe);
            response = Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(message)
                    .build();
        } else if(qe.getReason().equals(QueryException.FILE_ISSUE)){
            final String message = "The file specified is not valid, null or empty. ";
            LOGGER.error(message);
            response = Response.status(Response.Status.BAD_REQUEST)
                    .entity(message)
                    .build();
        } else {
            LOGGER.error("The sent query is invalid. Query sent: " + query, qe);
            response = Response.status(Response.Status.BAD_REQUEST).build();
        }

        return response;
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

    /*@Path(ADMIN)
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
    }*/

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
        return NerdRestCustomisation.getCustomisations();
    }

    @GET
    @Path(CUSTOMISATION + "/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomisation(@PathParam(NAME) String name) {
        return NerdRestCustomisation.getCustomisation(name);
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
        return NerdRestCustomisation.createCustomisation(name, content);
    }


    @Path(CUSTOMISATION + "/{name}")
    @DELETE
    public Response processDeleteNerdCustomisation(@PathParam(NAME) String name) {
        return NerdRestCustomisation.processDeleteNerdCustomisation(name);
    }

}
