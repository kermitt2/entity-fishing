package com.scienceminer.nerd.utilities;

import com.scienceminer.nerd.exceptions.*;

import java.io.*;
import java.util.*;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Useful static methods for web service requests.
 * 
 */
public class WebClientUtilities {

	/*public static void setProxy() {
		String host = NerdProperties.getInstance().getProxyHost();
		String port = NerdProperties.getInstance().getProxyPort();
	
		if ( (host != null) && (port != null) && 
			 (host.trim().length() > 0) && (port.trim().length() > 0) &&
			 (!host.trim().equals("null")) && (!port.trim().equals("null")) ) {
			System.out.println("set up proxy "  + host + ":" + port);
			System.setProperty("proxySet", "true");
			System.getProperties().put("proxyHost", host);
			System.getProperties().put("proxyPort", port);
		}
	}*/

    private static final String HMAC_SHA_ALGORITHM = "HmacSHA256";

	/** 
	 * Add the authorization information to an Idilia  POST request
	 */
    /*public static void signIdilia(HttpPost post, URI uri, String textParameter, String accessKey, String privateKey) 
		throws IOException {

      	// Get the host part from the URL
      	final String host = uri.getHost();
      	post.setHeader("Host", host);

      	// Format a date as per RFC 2616
      	final Date now = new Date();
      	final DateFormat dateFmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
      	dateFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
      	final String dateS = dateFmt.format(now);
      	post.setHeader("Date", dateS);

      	String toSign = dateS + "-" + host + "-" + uri.getPath();

		if (textParameter != null) {
			try {
		        MessageDigest md = MessageDigest.getInstance("MD5");
		        byte[] theDigest = md.digest(textParameter.getBytes(Charset.forName("UTF-8")));
		        byte[] base64md5 = Base64.encodeBase64(theDigest);
		        String strDigest = new String(base64md5);
		        toSign += "-";
		        toSign += strDigest;
		    } 
			catch (NoSuchAlgorithmException e) {
		        e.printStackTrace();
		   	}
		}

      	// Compute the signature
      	SecretKeySpec signingKey = new SecretKeySpec(privateKey.getBytes(), HMAC_SHA_ALGORITHM);
      	try {
        	Mac mac = Mac.getInstance(HMAC_SHA_ALGORITHM);
        	mac.init(signingKey);
        	byte[] rawHmac = mac.doFinal(toSign.getBytes());
        	byte[] signature = Base64.encodeBase64(rawHmac);
        	post.setHeader("Authorization", "IDILIA " + accessKey + ":" + new String(signature));
      	}
 		catch (NoSuchAlgorithmException e) {
        	e.printStackTrace();
      	} 
		catch (InvalidKeyException e) {
        	e.printStackTrace();
      	}
    }*/

}