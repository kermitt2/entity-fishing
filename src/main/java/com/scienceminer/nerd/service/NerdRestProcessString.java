package com.scienceminer.nerd.service;

import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.main.data.SoftwareInfo;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.mention.Sentence;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.engines.Engine;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.lang.Language;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.LanguageUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.NoSuchElementException;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class NerdRestProcessString {

    /**
     * The class Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestProcessString.class);

    /**
     * Apply a language identification on the raw text and return the identified language with a
     * confidence score.
     *
     * @param text raw text string
     * @return a response object containing the identified language with a confidence score.
     */
    public static Response processLanguageIdentification(String text) {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        try {
            if(StringUtils.isBlank(text)) {
                return Response.status(Status.BAD_REQUEST).build();
            }

            LanguageUtilities languageIdentifier = LanguageUtilities.getInstance();

            Language result = null;
            synchronized (languageIdentifier) {
                result = languageIdentifier.runLanguageId(text);
            }

	 		if (result != null) {
				StringBuilder builder = new StringBuilder();
                builder
				.append("{")
				.append("\"lang\": \""+result.getLang()+"\"")
				.append(", \"conf\":").append(result.getConf())
				.append("}");

				retVal = builder.toString();
			}

            if (isBlank(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response
                        .status(Status.OK)
                        .entity(retVal)
                        .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an WSD tagger instance. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        LOGGER.debug(methodLogOut());

        return response;
    }


    /**
     * Apply a sentence segmentation on the raw text and return offset of the different
     * sentences.
     *
     * @param text raw text string
     * @return a response object containing the offsets of the identified sentences.
     */
    public static Response processSentenceSegmentation(String text) {
        LOGGER.debug(methodLogIn());

        Response response = null;
        StringBuilder buffer = new StringBuilder();
        try {
            if (StringUtils.isBlank(text)) {
                return Response.status(Status.BAD_REQUEST).build();
            }

            ProcessText processText = ProcessText.getInstance();
            List<Sentence> sentences = processText.sentenceSegmentation(text);

            if (CollectionUtils.isNotEmpty(sentences)) {
                buffer.append("{ ").append(Sentence.listToJSON(sentences)).append(" } ");

                response = Response.status(Status.OK)
                        .entity(buffer.toString())
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        LOGGER.debug(methodLogOut());

        return response;
    }

    public static String processReference(String citationText, int consolidation) {
        LibraryLoader.load();
        Engine engine = GrobidFactory.getInstance().getEngine();

        final BiblioItem processedCitation = engine.getParsers().getCitationParser().processing(citationText, consolidation);

        String wikidataID = NerdEngine.getInstance().solveCitation(processedCitation);

        // Transforming in json
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append("title").append(":").append("\"").append(processedCitation.getTitle()).append("\"");
        sb.append(",");
        sb.append("\"").append("doi").append(":").append("\"").append(processedCitation.getDOI()).append("\"");
        sb.append(",");
        sb.append("\"").append("wikidataID").append(":").append("\"").append(wikidataID).append("\"");
        sb.append(",");
        sb.append("\"").append("authors").append(":").append("\"").append(processedCitation.getAuthors()).append("\"");
        return sb.toString();
    }

    public static String methodLogIn() {
        return ">> " + NerdRestProcessString.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public static String methodLogOut() {
        return "<< " + NerdRestProcessString.class.getName() + "." +
                Thread.currentThread().getStackTrace()[1].getMethodName();
    }

}
