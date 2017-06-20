package com.scienceminer.nerd.erd.corpus;

import com.scienceminer.nerd.erd.ErdAnnotationShort;
import com.scienceminer.nerd.exceptions.*;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.elasticsearch.node.NodeBuilder.*;
import org.elasticsearch.common.settings.*;
import org.elasticsearch.client.*;
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
import org.elasticsearch.search.sort.*;*/

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.*;

/**
 * SAX parser handler for the Yahoo L24 dataset.
 * 
 */
public class YahooL24SaxHandler extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

	// minimum number of tokens for a query to be considered
	static int MIN_QUERY_LENGTH = 3;

	private List<String> queries = null;
	private List<ErdAnnotationShort> annotations = null;

	// working variables
	private String currentQuery = null;
	private String currentMention = null;
	private String currentWiki = null;
	
	private ObjectMapper mapper = new ObjectMapper();
	//private String elasticSearch_ERD_KB = null;//NerdProperties.getInstance().getElasticSearchERDKBName();
	
    public YahooL24SaxHandler() {
		queries = new ArrayList<String>();
		annotations = new ArrayList<ErdAnnotationShort>();
    }

	public List<String> getQueries() {
		return queries;
	} 
	
	public List<ErdAnnotationShort> getAnnotations() {
		return annotations;
	}

    public void characters(char[] buffer, int start, int length) {
       	accumulator.append(buffer, start, length);
 	}

    public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName) throws SAXException {
        if (qName.equals("text")) {
            // this is the query
            String query = accumulator.toString();

			String[] tokens = query.split(" ");
			if (tokens.length > 2) {
				currentQuery = query.replace("(", " ").replace(")", " ").replace("OR", "").replace("\"", "").replace("  ", " ");
				queries.add(currentQuery);
				System.out.println("yahoo-"+(queries.size()-1) + "\t" + query);
			}
			else {
				currentQuery = null;
			}
        }
		else if (qName.equals("target")) {
            // this is the wikipedia page used for annotation
            currentWiki = accumulator.toString();
			currentWiki = currentWiki.replace("http://en.wikipedia.org/wiki/", "");
        }
		else if (qName.equals("span")) {
            // this is the annotated mention
            currentMention = accumulator.toString();
        }
		else if (qName.equals("annotation")) {
            // we keep the query if we have:
  			// - at least 3 tokens
			if ( (currentQuery != null) && (currentMention != null) && (currentWiki != null) ) {
				String freeBaseID = null;
				/*Settings settings = ImmutableSettings.settingsBuilder()
				        .put("client.transport.sniff", true).build();
				Client client = new TransportClient(settings)
				        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
				// the wikipedia page has to be in the ERD snapshot
				// we retrieve the concept first 
				SearchResponse response = client.prepareSearch(elasticSearch_ERD_KB)
						.setTypes("concept")
						.setQuery(QueryBuilders.multiMatchQuery(currentWiki, 
							"originalIDs.Wikipedia", "originalIDs.Wikipedia_redirects"))  
				        .setFrom(0)
						.setSize(1)
				        .execute()
				        .actionGet();

				java.util.Iterator<SearchHit> hit_it = response.getHits().iterator();
				while (hit_it.hasNext()) {
					SearchHit hit = hit_it.next();
					JsonNode resJsonStruct = null;
					try { 
						resJsonStruct = mapper.readTree(hit.getSourceAsString());

						JsonNode freebaseNode = resJsonStruct.path("originalIDs").path("FreeBase");
						if (!freebaseNode.isMissingNode()) {
							freeBaseID = freebaseNode.getTextValue();
						}

						if (freeBaseID == null) {
							continue;
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
				*/
				if (freeBaseID != null) {
					ErdAnnotationShort a = new ErdAnnotationShort();
					a.setQid("yahoo-"+(queries.size()-1));
					a.setInterpretationSet(0);
					a.setPrimaryId(freeBaseID);
					a.setMentionText(currentMention);
					a.setScore(1.0);
					annotations.add(a);
				}
				
				//client.close();
			}
			currentQuery = null;
			currentMention = null;
			currentWiki = null;
        }
		else if (qName.equals("query")) {
            // we keep the query if we have:
  			// - at least 3 tokens
        }
		else if (qName.equals("session")) {
			currentQuery = null;
			currentMention = null;
			currentWiki = null;
		}
		accumulator.setLength(0);
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
            throws SAXException {
		accumulator.setLength(0);
    }

}