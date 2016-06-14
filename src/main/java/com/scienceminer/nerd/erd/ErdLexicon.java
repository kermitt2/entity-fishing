package com.scienceminer.nerd.erd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.utilities.NerdProperties;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.net.*;
import org.apache.commons.io.FileUtils;
import java.util.StringTokenizer;
import java.util.regex.*;

import java.sql.*;
import javax.naming.*;
import javax.sql.*;

import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.elasticsearch.node.NodeBuilder.*;
import org.elasticsearch.client.*;
import org.elasticsearch.common.settings.*;
import org.elasticsearch.node.*;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.action.index.*;
import org.elasticsearch.common.transport.*;
import org.elasticsearch.client.transport.*;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.FilterBuilders.*;
import org.elasticsearch.index.query.QueryBuilders.*;
import org.elasticsearch.search.*;
import org.elasticsearch.index.query.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Method for management of the lexical entries.  
 * 
 * @author Patrice Lopez
 * 
 */
public class ErdLexicon {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErdLexicon.class);

	private String elasticSearch_host = NerdProperties.getInstance().getElasticSearchHost();
	private String elasticSearch_port = NerdProperties.getInstance().getElasticSearchPort();
	private String elasticSearch_KB = null; //NerdProperties.getInstance().getElasticSearchLexiconERDName();
	private String elasticSearch_ERD_KB = null; //NerdProperties.getInstance().getElasticSearchERDKBName();

	private Client client = null;
	private Node node = null;

	private static volatile ErdLexicon instance = null;

	public static ErdLexicon getInstance() throws Exception {
   		if (instance == null) {
			getNewInstance();
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
	private static synchronized void getNewInstance() throws Exception {
		LOGGER.debug("Get new instance of Lexicon KB");		
		instance = new ErdLexicon();
	}
	
	public ErdLexicon() throws Exception {
		Settings settings = ImmutableSettings.settingsBuilder()
		        .put("cluster.name", NerdProperties.getInstance().getElasticSearchClusterName()).build();
		Client client = new TransportClient(settings)
		        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
		if (!client.admin().indices().prepareExists(elasticSearch_KB).execute().actionGet().isExists()) 
			createIndex(elasticSearch_KB);
		client.close();
	} 

	/**
	 * @param db name of the index to be deleted.
	 */
	private boolean deleteIndex(String db) throws Exception {
		boolean val = false;
		try {
			String urlStr = "http://"+elasticSearch_host+":"+elasticSearch_port+"/"+db;
			URL url = new URL(urlStr);
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			httpCon.setDoOutput(true);
			httpCon.setRequestProperty(
			    "Content-Type", "application/x-www-form-urlencoded" );
			httpCon.setRequestMethod("DELETE");
			httpCon.connect();
			System.out.println("ElasticSearch Index " + db + " deleted: status is " + 
				httpCon.getResponseCode());
			if (httpCon.getResponseCode() == 200) {
				val = true;
			}
			httpCon.disconnect();				
		}
		catch(Exception e) {
			throw new NerdException("Cannot delete the index for " + db);
		}
		return val;
	}

	/**
	 * @param db name of the index to be created and loaded.
	 */
	private boolean createIndex(String db) throws Exception {
		boolean val = false;
System.out.println(db);
		String urlStr = "http://"+elasticSearch_host+":"+elasticSearch_port+"/"+db;
		URL url = new URL(urlStr);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		httpCon.setDoOutput(true);
		httpCon.setRequestProperty(
		    "Content-Type", "application/x-www-form-urlencoded" );
		httpCon.setRequestMethod("PUT");

		String analyserStr = null;
		try {
			File file = new File("src/main/resources/elasticSearch/analyzer.json");
			analyserStr = FileUtils.readFileToString(file, "UTF-8");
		}
		catch(Exception e) {
			throw new NerdException("Cannot read custom analyzer for " + db);
		}

		httpCon.setDoOutput(true);
		httpCon.setRequestMethod("PUT");
		httpCon.addRequestProperty("Content-Type", "text/json");
		OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
		out.write(analyserStr);
		out.close();

		System.out.println("ElasticSearch custom analyser for " + db + " : status is " + 
			httpCon.getResponseCode());

		if (httpCon.getResponseCode() == 200) {
			val = true;
		}

		boolean val2 = loadMapping(db);
		if (val2 && val) {
			val = true;
		}
		else {
			val = false;
		}
		
		httpCon.disconnect();
		int total = importKB(db);
		System.out.println("\n" + total + " entries have been intialised in the index " + db);
		
		return val;
	}

	/**
	 *  @param db name of the CouchDB database to be loaded.
	 *  @param type if type is 0, the patent mappings are used. if type is 1, NPL mappings are used.
	 */
	private boolean loadMapping(String db) throws Exception {
		boolean val = false;

		String urlStr = "http://"+elasticSearch_host+":"+elasticSearch_port+"/"+db;
		urlStr += "/entry/_mapping";

		URL url = new URL(urlStr);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		httpCon.setDoOutput(true);
		httpCon.setRequestProperty(
		    "Content-Type", "application/x-www-form-urlencoded" );
		httpCon.setRequestMethod("PUT");
		String mappingStr = null;
		try {
			File file = new File("src/main/resources/elasticSearch/mapping_dictionary.json");
			mappingStr = FileUtils.readFileToString(file, "UTF-8");
		}
		catch(Exception e) {
			throw new NerdException("Cannot read mapping for " + db);
		}

		System.out.println(urlStr);

		httpCon.setDoOutput(true);
		httpCon.setRequestMethod("PUT");
		httpCon.addRequestProperty("Content-Type", "text/json");
		OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
		out.write(mappingStr);
		out.close();

		System.out.println("ElasticSearch mapping for " + db + " : status is " + 
			httpCon.getResponseCode());
		if (httpCon.getResponseCode() == 200) {
			val = true;
		}
		return val;
	}
	
	/**
	 *  Add conditional prob for a string to realise a given concept, using Google crosswiki dataset
	 */
	public int importKB(String db) throws Exception {
		//int modifications = 0;
		int modifications = 30000000;
		Settings settings = ImmutableSettings.settingsBuilder()
		        .put("cluster.name", NerdProperties.getInstance().getElasticSearchClusterName()).build();
		Client client = new TransportClient(settings)
		        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
		// iterate through entity snapshot file
		BufferedReader dis = null;
		
		// we insert the lexical entries in the KB following the dictionary format
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		bulkRequest.setRefresh(true);
		int i = 0;
		int total = 0;
        try {
            dis = new BufferedReader(new InputStreamReader(
				new GZIPInputStream(new FileInputStream("data/erd2014/dictionary.gz")), "UTF-8"));

            String l = null;
            while ((l = dis.readLine()) != null) {
                if (l.length() == 0) continue;
				total++;
				
				/*if (l.startsWith("Bowflex") || l.startsWith("bowflex")) {
					System.out.println(l);
					continue;
				}
				else if (true) {
					continue;
				}*/
				
				if ((((float)(total))/300000000) < 0.88126665) {
					if (total % 100000 == 0) {
						System.out.println(((float)(total))/300000000);
						System.out.flush();
					}
					continue;
				}
				
				/*if (true) {
					System.out.println(l);				
					continue;
				}*/
				String[] tokens = l.split("\t");			
				if (tokens.length != 2) 
					continue;
				
				// string	
				String surface = tokens[0];
				if (surface.trim().length() == 0)
					continue;
				if (surface.indexOf("_") != -1) 
					continue;

				String suffix = tokens[1];
				String[] tokens2 = suffix.split(" ");
				
				// conditional prob
				String prob = tokens2[0];
				if (prob.trim().length() ==0)
					continue;
					
				if (prob.equals("0")) {
					// not sure... to be reviewed
					continue;
				}	
				
				Double prob_c = 0.0;
				try {
					prob_c = Double.parseDouble(prob);
				}
				catch(NumberFormatException e) {					
					e.printStackTrace();
				}
				
				// if the conditional prob is too low, we prune 
				// however if it is equal to 0.0, we keep the term because it corresponds to 
				// a spelling variant that might be useful and which remains specific to the concept
				if ( (prob_c < 0.0001) && (prob_c != 0.0) ){
					continue;
				}
				
				if (toBeFiltered(surface)) {
					//System.out.println("rejecting... " + surface);
					continue;
				}
				/*else {
					System.out.println("keeping... " + surface);
				}*/
				
				// wikipedia url / e.g. concept	
				String wikiconcept = tokens2[1];
				if (wikiconcept.trim().length() == 0)
					continue;
				
				// we also identify the actual number of occurence of the surface string
				int ind1 = l.indexOf("W:");
				if (ind1 == -1) {
					continue;
				}
				int ind2 = l.indexOf("/", ind1);
				if (ind2 == -1) {
					continue;
				}
				String nbOcc = l.substring(ind1+2, ind2);
				int nb = 1;
				try {
					nb = Integer.parseInt(nbOcc);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				
				// update the corresponding wiki concept
				try {
					boolean upd = 
						updateIndex(surface, prob_c, wikiconcept, modifications, nb, bulkRequest, client, elasticSearch_KB);
					if (upd) {
						i++;
						modifications++;
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				
				if (total % 10000 == 0) {
					System.out.println(((float)(total))/300000000);
					System.out.flush();
				}
				
				if (i == 500) {
					BulkResponse bulkResponse = bulkRequest.execute().actionGet();
					if (bulkResponse.hasFailures()) {
				    	// process failures by iterating through each bulk response item	
						System.out.println(bulkResponse.buildFailureMessage()); 
					}
					bulkRequest = client.prepareBulk();
					bulkRequest.setRefresh(true);
					i = 0;
				}
			}
			// last bulk
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
		    	// process failures by iterating through each bulk response item	
				System.out.println(bulkResponse.buildFailureMessage());
			}
		}
		finally {
			try {
				if (dis != null) {
					dis.close();
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		client.close();
		return modifications;
	}
	
	static private Pattern pattern_words = 
		Pattern.compile("(wiki|article about|http|see also|bibliography|external link|disambiguation|biography|references|artikel|link|here|source|more)");
	
	static private Pattern pattern0 = Pattern.compile("(\\()(.*?)(\\))");
	static private Pattern pattern1 = Pattern.compile("(\\[)(.*?)(\\])");
	static private Pattern pattern2 = Pattern.compile("^\\p{Punct}[\\p{Punct}, ]");
	static private Pattern pattern3 = Pattern.compile("[\\p{Punct}, ]\\p{Punct}$");
	static private Pattern pattern4 = Pattern.compile("^[0-9,\\p{Punct}, ]$");
	static private Pattern pattern5 = Pattern.compile("^[\\p{Punct}]?[1-2][0-9][0-9][0-9][\\p{Punct}, ]?");
	static private Pattern pattern6 = Pattern.compile("(</(.*?)>)");
	
	public boolean toBeFiltered(String surface) {
		boolean res = false;
		surface = surface.toLowerCase().trim();
		
		if ( (surface.length()<3) || (surface.length()>35) ) {
			return true;
		}
		
		Matcher matcher = pattern_words.matcher(surface);
		if (matcher.find()) {
			return true;
		}
		matcher = pattern0.matcher(surface);
		if (matcher.find()) {
			return true;
		}
		matcher = pattern1.matcher(surface);
		if (matcher.find()) {
			return true;
		}
		matcher = pattern2.matcher(surface);
		if (matcher.find()) {
			return true;
		}
		matcher = pattern3.matcher(surface);
		if (matcher.find()) {
			return true;
		}
		matcher = pattern4.matcher(surface);
		if (matcher.find()) {
			return true;
		}
		matcher = pattern5.matcher(surface);
		if (matcher.find()) {
			return true;
		}
		matcher = pattern6.matcher(surface);
		if (matcher.find()) {
			return true;
		}
		
		if (surface.startsWith("yago-"))
			return true;
		
		// we filter Russian text
		if (surface.matches("[Читьювкпдл]"))
			return true;
		
		return res;
	}
	
	private boolean updateIndex(String surface, 
							Double prob_c, 
							String wikiconcept, 
							int num,
							int nbOcc,
							BulkRequestBuilder bulkRequest, 
							Client client, 
							String elasticSearch_KB) {
	   	boolean res = false;
	   	try {	
			// we can update only if the wikipedia concept is present in the ERD KB.
			/*SearchResponse response = client.prepareSearch(elasticSearch_ERD_KB)
					.setQuery(QueryBuilders.termQuery("originalIDs.Wikipedia", wikiconcept))    
			        .setFrom(0)
					.setSize(1)
			        .execute()
			        .actionGet();
						
			java.util.Iterator<SearchHit> hit_it = response.getHits().iterator();
			if (hit_it.hasNext()) */
			{
				XContentBuilder builder = jsonBuilder();
			
			    builder.startObject();
			
				Map<String, Object> json = new HashMap<String, Object>();
				json.put("term", surface);
				json.put("Wikipedia", wikiconcept);
				json.put("prob", prob_c);
				json.put("freq", nbOcc);
				builder.field("form", json);
				builder.endObject(); // end of record
			
				bulkRequest.add(client.prepareIndex(elasticSearch_KB, "entry", ""+num).setSource(builder));
				res = true;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return res;										
	}
	
	/**
     *	Launch the ElasticSearch indexing from the MySQL content.
     */
    public static void main(String[] args)
        throws IOException, ClassNotFoundException, 
               InstantiationException, IllegalAccessException {
		ErdLexicon lex = null;
		try {
			long start = System.currentTimeMillis();
        	lex = ErdLexicon.getInstance();
			lex.importKB("erdlex2014");
			long end = System.currentTimeMillis();
			System.out.println((end - start) + " milliseconds");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}
