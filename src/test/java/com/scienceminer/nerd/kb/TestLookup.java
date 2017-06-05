package com.scienceminer.nerd.kb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.io.*;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Ignore; 

import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.utilities.NerdPropertyKeys;
import com.scienceminer.nerd.utilities.NerdConfig;

import com.scienceminer.nerd.kb.*;
import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.*;

import org.xml.sax.SAXException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 *  @author Patrice Lopez
 */
public class TestLookup {
	
	private Wikipedia wikipedia = null;
	private KnowledgeBase knowledgeBase = null;

	@Before
	public void setUp() {
		try {
			NerdProperties.getInstance();
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			
			NerdConfig conf = mapper.readValue(new File("data/wikipedia/kb.yaml"), NerdConfig.class);
            knowledgeBase = new KnowledgeBase(conf);

            conf = mapper.readValue(new File("data/wikipedia/wikipedia-en.yaml"), NerdConfig.class);
        	wikipedia = new Wikipedia(conf); 
        } catch(Exception e) {
        	e.printStackTrace();
        }
	}
   	
	@Test
	public void testPageId() {
		try {
			Page page = wikipedia.getPageById(3966054);

			System.out.println("pageId:" + page.getId());
			System.out.println(page.getTitle());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testTitle() {
		try {
			Page page = wikipedia.getArticleByTitle("Mexico");
			if (page != null) {
				System.out.println(page.getTitle());
				System.out.println("pageId:" + page.getId());
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/*@Test
	public void testProperties() {
		try {
			List<Property> properties = wikipedia.getProperties(3966054);

			System.out.println("properties for Mexico (" + 3966054 + ")");
			if (properties != null) {
				for (Property property : properties)
					System.out.println(property.toJson());
			} else 
				System.out.println("no property found");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}*/

	/*@Test
	public void testRelations() {
		try {
			List<Relation> relations = wikipedia.getRelations(3966054);

			System.out.println("relations for Mexico (" + 3966054 + ")");
			if (relations != null) {
				for (Relation relation : relations)
					System.out.println(relation.toJson());
			} else 
				System.out.println("no relation found");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}*/
	
	@After
	public void testClose() {
		try {
			wikipedia.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}

