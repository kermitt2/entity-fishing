package com.scienceminer.nerd.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import com.scienceminer.nerd.exceptions.NerdServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Patrice
 * 
 */
public class NerdRestUtils {

	// NER base types
	public enum Format {
		JSON	("json"),
		XML		("xml");
		
		private String name;

		Format(String name) {
          	this.name = name;
		}

		public String getName() {
			return name;
		}
	};

	private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestUtils.class);

	/**
	 * Check whether the result is null or empty.
	 *
	 * @param result is the result of the process.
	 * @return true if the result is not null and not empty, false else.
	 */
	public static boolean isResultOK(String result) {
		return StringUtils.isNotBlank(result);
	}

	/**
	 * Write an input stream in temp directory.
	 */
	public static File writeInputFile(InputStream inputStream) {
		LOGGER.debug(">> set origin document for stateless service'...");

		File originFile = null;
		OutputStream out = null;
		try {
			originFile = newTempFile("origin", "pdf");

			out = new FileOutputStream(originFile);

			byte buf[] = new byte[1024];
			int len;
			while ((len = inputStream.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} 
		catch (IOException e) {
			LOGGER.error("An internal error occurs, while writing to disk (file to write '"
					+ originFile + "').", e);
			originFile = null;
		} 
		finally {
			IOUtils.closeQuietly(out, inputStream);
		}
		return originFile;
	}

	/**
	 * Creates a new not used temprorary file and returns it.
	 * 
	 * @return
	 */
	public static File newTempFile(String fileName, String extension) {
		try {
			return File.createTempFile(fileName, extension);
		} catch (IOException e) {
			throw new NerdServiceException("Could not create temporary file, '" + fileName + "."
							+ extension + "'.");
		}
	}

	/**
	 * Delete the temporary file.
	 * 
	 * @param file
	 *            the file to delete.
	 */
	public static void removeTempFile(final File file) {
		try {
			LOGGER.debug("Removing " + file.getAbsolutePath());
			file.delete();
		} catch (Exception exp) {
			LOGGER.error("Error while deleting the temporary file: " + exp);
		}
	}

}
