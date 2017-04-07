package com.scienceminer.nerd.disambiguation;

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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;

/**
 *  @author Patrice Lopez
 */
public class TestWeightedTerm {
	private String testPath = null;
	
	static final String testJson = "{ \"term\":\"computer\",\"score\":1.0 }";

	@Before
	public void setUp() {
		NerdProperties.getInstance();
		testPath = NerdProperties.getTestPath();
	}

	@Test
	public void testJson() {
		try {
			ObjectMapper mapper = new ObjectMapper();  
			WeightedTerm toto = mapper.readValue(testJson, WeightedTerm.class);
		}
		catch (JsonParseException e) {
			e.printStackTrace();
		} 
		catch (JsonMappingException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}