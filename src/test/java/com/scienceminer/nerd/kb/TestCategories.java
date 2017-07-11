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

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.model.*;

import org.xml.sax.SAXException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public class TestCategories {
	
	private LowerKnowledgeBase wikipedia = null;

	@Before
	public void setUp() {
		try {
			NerdProperties.getInstance();
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            NerdConfig conf = mapper.readValue(new File("data/wikipedia/wikipedia-en.yaml"), NerdConfig.class);
        	wikipedia = new LowerKnowledgeBase(conf); 
        } catch(Exception e) {
        	e.printStackTrace();
        }
	}
   	
	@Test
	public void testCategoryHierarchy() {
		try {
			Page page = wikipedia.getPageById(32768);

			com.scienceminer.nerd.kb.model.Category[] categories = ((Article)page).getParentCategories();
			System.out.println("pageId:" + page.getId());
			for(int l=0; l<categories.length;l++){
			    System.out.println(categories[l].getId());
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
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