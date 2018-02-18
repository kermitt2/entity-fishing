package com.scienceminer.nerd.utilities;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.grobid.core.factory.*;
import org.grobid.core.main.*;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.layout.LayoutToken;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.scienceminer.nerd.disambiguation.NerdEntity;

import org.apache.commons.lang3.StringUtils;
import com.scienceminer.nerd.exceptions.NerdException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some utilities methods that I don't know where to put.
 *
 */
public class Utilities {

	private static final Logger LOGGER = LoggerFactory.getLogger(Utilities.class);

	/**
	 * Deletes all files and subdirectories under dir. Returns true if all
	 * deletions were successful. If a deletion fails, the method stops
	 * attempting to delete and returns false.
	 */
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// the directory is now empty so delete it
		return dir.delete();
	}

	/**
	 * Cleaninig of the body of text prior to term extraction. Try to remove the
	 * pdf extraction garbage and the citation marks.
	 */
	public static String cleanBody(String text) {
		if (text == null)
			return null;
		String res = "";

		// clean pdf weird output for math. glyphs
		Pattern cleaner = Pattern.compile("[-]?[a-z][\\d]+[ ]*");
		// System.out.println("RegEx Syntax error! There is something wrong with my pattern"
		// + rs);
		Matcher m = cleaner.matcher(text);
		res = m.replaceAll("");

		Pattern cleaner2 = Pattern.compile("[\\w]*[@|#|=]+[\\w]+");
		// System.out.println("RegEx Syntax error! There is something wrong with my pattern"
		// + rs);
		Matcher m2 = cleaner2.matcher(res);
		res = m2.replaceAll("");

		res = res.replace("Introduction", "");

		// clean citation markers?

		return res;
	}

	public static String uploadFile(String urlmsg, String path, String name) {
		try {
			System.out.println("Sending: " + urlmsg);
			URL url = new URL(urlmsg);

			File outFile = new File(path, name);
			FileOutputStream out = new FileOutputStream(outFile);
			// Writer out = new OutputStreamWriter(os,"UTF-8");

			// Serve the file
			InputStream in = url.openStream();
			byte[] buf = new byte[4 * 1024]; // 4K buffer
			int bytesRead;
			while ((bytesRead = in.read(buf)) != -1) {
				out.write(buf, 0, bytesRead);
			}

			out.close();
			in.close();
			return path + name;
		} catch (Exception e) {
			throw new NerdException(
					"An exception occured while running Nerd.", e);
		}
		// return null;
	}

	public static String punctuationsSub = "([,;])";

	/**
	 * Return the name of directory to use given the os and the architecture.<br>
	 * Possibles returned values should match one of the following:<br>
	 * win-32<br>
	 * lin-32<br>
	 * lin-64<br>
	 * mac-64<br>
	 *
	 * @return name of the directory corresponding to the os name and
	 *         architecture.
	 */
	public static String getOsNameAndArch() {
		String osPart = System.getProperty("os.name").replace(" ", "")
				.toLowerCase().substring(0, 3);
		String archPart = System.getProperty("sun.arch.data.model");
		return String.format("%s-%s", osPart, archPart);
	}

	/**
	 * Convert a string to boolean.
	 *
	 * @param value
	 *            the value to convert
	 * @return true if the string value is "true", false is it equals to
	 *         "false". <br>
	 *         If the value does not correspond to one of these 2 values, return
	 *         false.
	 */
	public static boolean stringToBoolean(String value) {
		boolean res = false;
		if (StringUtils.isNotBlank(value)
				&& Boolean.toString(true).equalsIgnoreCase(value.trim())) {
			res = true;
		}
		return res;
	}

	/**
	 * Call a java method using the method name given in string.
	 *
	 * @param obj
	 *            Class in which the method is.
	 * @param args
	 *            the arguments of the method.
	 * @param methodName
	 *            the name of the method.
	 * @return result of the called method.
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public static Object launchMethod(Object obj, Object[] args,
			String methodName) throws Exception {
		Class[] paramTypes = null;
		if (args != null) {
			paramTypes = new Class[args.length];
			for (int i = 0; i < args.length; ++i) {
				paramTypes[i] = args[i].getClass();
			}
		}
		return getMethod(obj, paramTypes, methodName).invoke(obj, args);
	}

	/**
	 * Call a java method using the method name given in string.
	 *
	 * @param obj
	 *            Class in which the method is.
	 * @param args
	 *            the arguments of the method.
	 * @param paramTypes
	 *            types of the arguments.
	 * @param methodName
	 *            the name of the method.
	 * @return result of the called method.
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public static Object launchMethod(Object obj, Object[] args,
			Class[] paramTypes, String methodName) throws Exception {
		return getMethod(obj, paramTypes, methodName).invoke(obj, args);
	}

	/**
	 * Get the method given in string in input corresponding to the given
	 * arguments.
	 *
	 * @param obj
	 *            Class in which the method is.
	 * @param paramTypes
	 *            types of the arguments.
	 * @param methodName
	 *            the name of the method.
	 * @return Methood
	 *
	 * @throws NoSuchMethodException
	 */
	@SuppressWarnings("rawtypes")
	public static Method getMethod(Object obj, Class[] paramTypes,
			String methodName) throws NoSuchMethodException {
		Method method = obj.getClass().getMethod(methodName, paramTypes);
		return method;
	}

	/**
	 * Creates a file and writes some content in it.
	 *
	 * @param file
	 *            The file to write in.
	 * @param content
	 *            the content to write
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void writeInFile(String file, String content)
			throws FileNotFoundException, IOException {
		FileWriter filew = new FileWriter(new File(file));
		BufferedWriter buffw = new BufferedWriter(filew);
		buffw.write(content);
		buffw.close();
	}

	/**
	 * Read a file and return the content.
	 *
	 * @param pPathToFile
	 *            path to file to read.
	 * @return String contained in the document.
	 * @throws IOException
	 */
	public static String readFile(String pPathToFile) throws IOException {
		StringBuffer out = new StringBuffer();
		FileInputStream inputStrem = new FileInputStream(new File(pPathToFile));
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte buf[] = new byte[1024];
		int len;
		while ((len = inputStrem.read(buf)) > 0) {
			outStream.write(buf, 0, len);
			out.append(outStream.toString());
		}
		inputStrem.close();
		outStream.close();

		return out.toString();
	}

	/**
	 * Format a date in string using pFormat.
	 *
	 * @param pDate the date to parse.
	 * @param pFormat the format to use following SimpleDateFormat patterns.
	 *
	 * @return the formatted date.
	 */
	public static String dateToString(Date pDate, String pFormat){
		SimpleDateFormat dateFormat = new SimpleDateFormat(pFormat);
		return dateFormat.format(pDate);
	}

	public static void initGrobid() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		try {
	        NerdConfig conf = mapper.readValue(new File("data/config/mention.yaml"), NerdConfig.class);
			String pGrobidHome = conf.getGrobidHome();

			GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
        	GrobidProperties.getInstance(grobidHomeFinder);
            LibraryLoader.load();
			LOGGER.info(">>>>>>>> GROBID_HOME="+GrobidProperties.get_GROBID_HOME_PATH());
		}
		catch(Exception e) {
			throw new NerdException("Fail to initalise the grobid-ner component.", e);
		}
	}

	// standard JDK serialization
	public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}

	// standard JDK deserialization
	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}

	public static List<LayoutToken> getWindow(NerdEntity entity, List<LayoutToken> tokens, int size, String lang) {
		int start = entity.getOffsetStart();
		int end = entity.getOffsetEnd();

		return getWindow(start, end, tokens, size, lang);
	}

	public static List<LayoutToken> getWindow(int start, int end, List<LayoutToken> tokens, int size, String lang) {
		List<LayoutToken> subTokens = new ArrayList<LayoutToken>();

		// first locate the entity in the token list
		int pos = 0;
		for(LayoutToken token : tokens) {
			if ( (token.getOffset() >= start) && ((token.getOffset()+token.getText().length()) <= end) )
				break;
			pos++;
		}

		int posStart = pos - size;
		if (posStart < 0)
			posStart = 0;
		int posEnd = pos + size;
		if (posEnd >= tokens.size())
			posEnd = tokens.size()-1;

		for(int p = posStart; p <= posEnd; p++) {
			if (p != pos) {
				subTokens.add(tokens.get(p));
			}
		}

		return subTokens;
	}

	public static double getOccCount(String term, String string) {
        return (double) StringUtils.countMatches(string, term);
    }

    public static float inner(int size, float[] v1, int offset1, float[] v2, int offset2) {
        if( size % 4 != 0 ) {
            throw new IllegalArgumentException( "Vector size must be a multiple of 4" );
        }

        float x0 = 0, x1 = 0, x2 = 0, x3 = 0;
        // manually unroll to help the compiler autovectorizer
        // (current JVM does not support vectorized accumulation)
        for(int i = 0; i < size; i += 4) {
            x0 += v1[offset1 + i + 0] * v2[offset2 + i + 0];
            x1 += v1[offset1 + i + 1] * v2[offset2 + i + 1];
            x2 += v1[offset1 + i + 2] * v2[offset2 + i + 2];
            x3 += v1[offset1 + i + 3] * v2[offset2 + i + 3];
        }

        return x0 + x1 + x2 + x3;
    }

}
