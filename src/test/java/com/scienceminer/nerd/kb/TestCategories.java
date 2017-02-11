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

import com.scienceminer.nerd.kb.db.*;
import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.*;

import org.xml.sax.SAXException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *  @author Patrice Lopez
 */
public class TestCategories {
	
	private Wikipedia wikipedia = null;

	@Before
	public void setUp() {
		try {
			NerdProperties.getInstance();
			WikipediaConfiguration conf = 
					new WikipediaConfiguration(new File("data/wikipedia/wikipedia-en.xml"));
        	wikipedia = new Wikipedia(conf, false); // no distinct thread for accessing data
        } catch(Exception e) {
        	e.printStackTrace();
        }
	}
   	
	@Test
	public void testCategoryHierarchy() {
		try {
			Page page = wikipedia.getPageById(32768);

			org.wikipedia.miner.model.Category[] categories = ((Article)page).getParentCategories();
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