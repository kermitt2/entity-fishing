package com.scienceminer.nerd.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.io.*;

import org.apache.commons.io.FileUtils;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Ignore; 

import com.scienceminer.nerd.utilities.NerdProperties;
import com.scienceminer.nerd.utilities.NerdPropertyKeys;
import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import org.grobid.core.data.Entity;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *  @author Patrice Lopez
 */
public class TestQueryParser {
	private String testPath = null;

	@Before
	public void setUp() {
		NerdProperties.getInstance();
		testPath = NerdProperties.getTestPath();
	}

	@Test
	public void testReadQuery() {
		ObjectMapper mapper = null;
		try {
			mapper = new ObjectMapper();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	
		NerdQuery aQuery = new NerdQuery();
		aQuery.setText("bla bla");
		
		String json = aQuery.toJSON();
		System.out.println("aQuery1: " + json);
		
		try {
			aQuery = mapper.readValue(json, NerdQuery.class);
		}
		catch(IOException e) {
			e.printStackTrace();
		}	
		
		String theQuery = null;
		System.out.println(testPath+"/query.json");
		File queryFile = new File(testPath+"/query.json");
		if (!queryFile.exists()) {
			throw new NerdException("Cannot start test, because test resource folder is not correctly set.");
		}
		try {
			theQuery = FileUtils.readFileToString(queryFile);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		try {
			NerdQuery nerdQuery = mapper.readValue(theQuery, NerdQuery.class);
			System.out.println(nerdQuery.toJSON());
		}
		catch(JsonGenerationException e) {
			e.printStackTrace();
		}
		catch (JsonMappingException e) {
		   	e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
	}
}