package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.exceptions.NerdException;
import com.scienceminer.nerd.utilities.NerdProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import java.io.BufferedReader;
import java.util.List;    

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.io.*;

/**
 * Representation of a customization. A customisation is currently a list of wikipedia articles that
 * will weight the relatedness score when ranking candidates for disambiguation so it is a NerdContext.
 *  
 * @author Patrice Lopez
 * 
 */
public class NerdCustomisation extends NerdContext { 
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(NerdCustomisation.class);
	
	private String name = null;

	public NerdCustomisation() {
		super();
	}
	
	public void setName(String theName) {
		name = theName;
	}

	public String getName() {
		return name;
	}
}