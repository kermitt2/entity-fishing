package com.scienceminer.nerd.disambiguation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.ArrayList;
import java.io.*;

import org.apache.commons.io.FileUtils;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Ignore; 

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.io.*;


public class WeightedTermTest {
	private String testPath = null;
	
	static final String testJson = "{ \"term\":\"computer\",\"score\":1.0 }";

	@Before
	public void setUp() {
		testPath = "src/test/resources/";
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