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

import org.xml.sax.SAXException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *  @author Patrice Lopez
 */
public class TestCustomisation {
	
	@Before
	public void setUp() {
		NerdProperties.getInstance();
	}
	
	@Test
	public void testCreate() {
		try {
			Customisation customisation = Customisation.getInstance();
			String createJson = "{ \"wikipedia\" : [4764461, 51499, 1014346], \"freebase\": [\"/m/0cm2xh\", \"/m/0dl4z\", \"/m/02kxg_\", \"/m/06v9th\"], \"texts\": [\"World War I (WWI or WW1 or World War One), also known as the First World War or the Great War, was a global war centred in Europe that began on 28 July 1914 and lasted until 11 November 1918.\", \"The war drew in all the world's economic great powers, which were assembled in two opposing alliances: the Allies (based on the Triple Entente of the United Kingdom, France and the Russian Empire) and the Central Powers of Germany and Austria-Hungary.\"] }";
			String message = customisation.createCustomisation("testWW1", createJson);
			System.out.println(message);
			List<String> customisations = customisation.getCustomisations();
			for(String cust : customisations) {
				System.out.println(cust);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testExtend() {
		try {
			Customisation customisation = Customisation.getInstance();
			String createJson = "{ \"wikipedia\" : [4764461, 51499], \"freebase\": [\"/m/0cm2xh\", \"/m/0dl4z\", \"/m/02kxg_\", \"/m/06v9th\"], \"texts\": [\"World War I (WWI or WW1 or World War One), also known as the First World War or the Great War, was a global war centred in Europe that began on 28 July 1914 and lasted until 11 November 1918.\", \"The war drew in all the world's economic great powers, which were assembled in two opposing alliances: the Allies (based on the Triple Entente of the United Kingdom, France and the Russian Empire) and the Central Powers of Germany and Austria-Hungary.\"] }";
			System.out.println("A : " + createJson);
			
			String message = customisation.createCustomisation("test2WW1", createJson);
			System.out.println("create: " + message);
			List<String> customisations = customisation.getCustomisations();
			for(String cust : customisations) {
				System.out.println(cust);
			}
			String updateJson = "{ \"wikipedia\" : [4764461, 1014346], \"freebase\": [], \"texts\": [\"No, no\"], \"description\" : \"This the war\" }";
			System.out.println("B : " + updateJson);
			
			message = customisation.extendCustomisation("testWW1", updateJson);
			System.out.println("extend: " + message);
			String cust = customisation.getCustomisation("testWW1");
			System.out.println("A + B : " + cust);
			
			message = customisation.deleteCustomisation("test2WW1");
			System.out.println("delete : " + message);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
   	
	@Test
	public void testDelete() {
		try {
			Customisation customisation = Customisation.getInstance();
			String message = customisation.deleteCustomisation("testWW1");
			System.out.println(message);
			List<String> customisations = customisation.getCustomisations();
			for(String cust : customisations) {
				System.out.println(cust);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@After
	public void save() {
		try {
			Customisation customisation = Customisation.getInstance();
			customisation.save();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}