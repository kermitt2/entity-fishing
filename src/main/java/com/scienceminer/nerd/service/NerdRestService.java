package com.scienceminer.nerd.service;

import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.embeddings.SimilarityScorer;
import com.scienceminer.nerd.exceptions.CustomisationException;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.exceptions.ResourceNotFound;
import com.scienceminer.nerd.kb.Lexicon;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.mention.ProcessText;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.NoSuchElementException;

import static org.apache.commons.lang3.StringUtils.isBlank;

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
    private static final String DOI = "doi";
    private static final String NBEST = "nbest";
    private static final String SENTENCE = "sentence";
    private static final String FORMAT = "format";
    private static final String CUSTOMISATION = "customisation";

    NerdRestProcessQuery nerdProcessQuery;
    NerdRestProcessFile nerdProcessFile;
    NerdRestKB nerdRestKB;

    public NerdRestService() {
        LOGGER.info("Init lexicon.");
        Lexicon.getInstance();
        LOGGER.info("Init lexicon finished.");

        LOGGER.info("Init KB resources.");
        UpperKnowledgeBase.getInstance();
        LOGGER.info("Init KB resources finished.");

        nerdProcessQuery = new NerdRestProcessQuery();
        nerdProcessFile = new NerdRestProcessFile();
        nerdRestKB = new NerdRestKB();

        //Pre-instantiate
        ProcessText.getInstance();
        SimilarityScorer.getInstance();
        NerdEngine.getInstance();
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
     * @see NerdRestProcessGeneric#getVersion()
     */
    @GET
    @Path(NerdPaths.VERSION)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersion() {
        Response response = null;
        try {
            response = Response.status(Response.Status.OK)
                    .entity(NerdRestProcessGeneric.getVersion())
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
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
        } catch (GrobidException ge) {
            response = Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("The PDF cannot be processed by grobid. " + ge.getMessage())
                    .build();
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

    private Response handleResourceNotFound(ResourceNotFound re, String identifier) {
        Response response;

        String json = null;
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{ \"message\": \"The requested resource for identifier " + identifier + " could not be found but may be available in the future.\" }");
        json = jsonBuilder.toString();

        LOGGER.error(json);
        response = Response
                .status(Response.Status.NOT_FOUND)
                .entity(json)
                .build();
        return response;
    }

    private Response handleQueryException(QueryException qe, String query) {
        Response response;

        String message = "The sent query is invalid.";

        String json = null;
        StringBuilder jsonBuilder = new StringBuilder();

        switch (qe.getReason()) {

            case QueryException.LANGUAGE_ISSUE:
                message = "The language specified is not supported or not valid. ";
                jsonBuilder.append("{ \"message\": \"" + message + "\" }");
                json = jsonBuilder.toString();
                LOGGER.error(message, qe);
                response = Response
                        .status(Response.Status.NOT_ACCEPTABLE)
                        .entity(json)
                        .build();

                break;

            case QueryException.FILE_ISSUE:
                message = "There are issues with the posted PDF file. " + qe.getMessage();
                jsonBuilder.append("{ \"message\": \"" + message + "\" }");
                json = jsonBuilder.toString();
                LOGGER.error(message);
                response = Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(json)
                        .build();

                break;

            case QueryException.WRONG_IDENTIFIER:
                message = "Wrong identifier. " + qe.getMessage();
                jsonBuilder.append("{ \"message\": \"" + message + "\" }");
                json = jsonBuilder.toString();
                response = Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(json)
                        .build();
                break;

            case QueryException.INVALID_TERM:
                message = "Wrong term identifier. " + qe.getMessage();
                jsonBuilder.append("{ \"message\": \"" + message + "\" }");
                json = jsonBuilder.toString();
                response = Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(json)
                        .build();
                break;

            default:
                LOGGER.error(message + " Query sent: " + query, qe);
                response = Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("The sent query is invalid. " + qe.getMessage())
                        .build();

                break;
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
                                          @DefaultValue(Language.EN) @QueryParam(LANG) String lang) {

        String output = null;
        Response response = null;

        try {
            output = nerdRestKB.getConceptInfo(identifier, lang);

            if (isBlank(output)) {
                response = Response.status(Response.Status.NOT_FOUND).build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(output)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

        } catch (ResourceNotFound re) {
            return handleResourceNotFound(re, identifier);
        } catch (QueryException qe) {
            return handleQueryException(qe, identifier);
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }

    @GET
    @Path(KB + "/" + TERM + "/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTermLookup(@PathParam(TERM) String term,
                                  @DefaultValue("en") @QueryParam(LANG) String lang) {

        String output = null;
        Response response = null;

        try {
            output = nerdRestKB.getTermLookup(term, lang);

            if (isBlank(output)) {
                response = Response.status(Response.Status.NOT_FOUND).build();
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
            return handleQueryException(qe, term);
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;

    }

    @GET
    @Path(KB + "/" + DOI + "/{doi}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWikiedataIdByDOI(@PathParam(DOI) String doi) {

        String output = null;
        Response response = null;

        try {
            output = nerdRestKB.getWikidataIDByDOI(doi);

            if (isBlank(output)) {
                response = Response.status(Response.Status.NOT_FOUND).build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(output)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

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
     * Customisation API
     **/
    @GET
    @Path(CUSTOMISATIONS)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomisations() {
        Response response = null;
        try {
            String output = NerdRestCustomisation.getCustomisations();

            response = Response
                    .status(Response.Status.OK)
                    .entity(output)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();

        } catch (CustomisationException ce) {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(responseJson(false, ce.getMessage()))
                    .build();

        } catch (Exception exp) {
            LOGGER.error("General error when accessing the list of existing customisations. ", exp);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;
    }

    @GET
    @Path(CUSTOMISATION + "/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomisation(@PathParam(NAME) String name) {
        Response response = null;
        try {
            String output = NerdRestCustomisation.getCustomisation(name);
            if (output == null) {
                response = Response
                        .status(Response.Status.NOT_FOUND)
                        .build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(output)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }
        } catch (CustomisationException ce) {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(responseJson(false, ce.getMessage()))
                    .build();
        } catch (Exception exp) {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;

    }

    @PUT
    @Path(CUSTOMISATION + "/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateCustomisation(@PathParam(NAME) String name, @FormDataParam(VALUE) String newContent) {
        boolean ok = false;
        Response response = null;
        try {
            ok = NerdRestCustomisation.updateCustomisation(name, newContent);

            response = Response
                    .status(Response.Status.OK)
                    .entity(responseJson(ok, null))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();

        } catch (CustomisationException ce) {
            response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(responseJson(ok, ce.getMessage()))
                    .build();

        } catch (Exception e) {
            response = Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return response;


    }

    @POST
    @Path(CUSTOMISATIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addCustomisation(@FormDataParam(NAME) String name, @FormDataParam(VALUE) String content) {
        boolean ok = false;
        Response response = null;
        try {
            ok = NerdRestCustomisation.createCustomisation(name, content);
            response = Response
                    .status(Response.Status.OK)
                    .entity(responseJson(ok, null))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();

        } catch (CustomisationException ce) {
            response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(responseJson(ok, ce.getMessage()))
                    .build();

        } catch (Exception e) {
            response = Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return response;
    }


    @Path(CUSTOMISATION + "/{name}")
    @DELETE
    public Response processDeleteNerdCustomisation(@PathParam(NAME) String name) {
        boolean ok = false;
        Response response = null;
        try {
            ok = NerdRestCustomisation.deleteCustomisation(name);
            response = Response
                    .status(Response.Status.OK)
                    .entity(responseJson(ok, null))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();

        } catch (CustomisationException ce) {
            response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(responseJson(ok, ce.getMessage()))
                    .build();

        } catch (Exception e) {
            response = Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return response;
    }


    private static String responseJson(boolean ok, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"ok\": \"" + ok + "\"");
        if (message != null) {
            sb.append(", \"status\": \"" + message + "\"");
        }
        sb.append("}");

        return sb.toString();
    }

}
