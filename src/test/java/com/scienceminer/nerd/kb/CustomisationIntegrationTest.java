package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.utilities.NerdProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class CustomisationIntegrationTest {
	
	@Before
	public void setUp() {
		NerdProperties.getInstance();
	}
	
	@Test
	public void testCreate() {
		try {
			Customisations customisations = Customisations.getInstance();
			String createJson = "{ \"wikipedia\" : [4764461, 51499, 1014346], \"freebase\": [\"/m/0cm2xh\", \"/m/0dl4z\", \"/m/02kxg_\", \"/m/06v9th\"], \"texts\": [\"World War I (WWI or WW1 or World War One), also known as the First World War or the Great War, was a global war centred in Europe that began on 28 July 1914 and lasted until 11 November 1918.\", \"The war drew in all the world's economic great powers, which were assembled in two opposing alliances: the Allies (based on the Triple Entente of the United Kingdom, France and the Russian Empire) and the Central Powers of Germany and Austria-Hungary.\"] }";
			String message = customisations.createCustomisation("testWW1", createJson);
			System.out.println(message);
			List<String> theCustomisations = customisations.getCustomisations();
			for(String cust : theCustomisations) {
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
			Customisations customisations = Customisations.getInstance();
			String createJson = "{ \"wikipedia\" : [4764461, 51499], \"freebase\": [\"/m/0cm2xh\", \"/m/0dl4z\", \"/m/02kxg_\", \"/m/06v9th\"], \"texts\": [\"World War I (WWI or WW1 or World War One), also known as the First World War or the Great War, was a global war centred in Europe that began on 28 July 1914 and lasted until 11 November 1918.\", \"The war drew in all the world's economic great powers, which were assembled in two opposing alliances: the Allies (based on the Triple Entente of the United Kingdom, France and the Russian Empire) and the Central Powers of Germany and Austria-Hungary.\"] }";
			System.out.println("A : " + createJson);
			
			String message = customisations.createCustomisation("test2WW1", createJson);
			System.out.println("create: " + message);
			List<String> theCustomisations = customisations.getCustomisations();
			for(String cust : theCustomisations) {
				System.out.println(cust);
			}
			String updateJson = "{ \"wikipedia\" : [4764461, 1014346], \"freebase\": [], \"texts\": [\"No, no\"], \"description\" : \"This the war\" }";
			System.out.println("B : " + updateJson);
			
			message = customisations.updateCustomisation("testWW1", updateJson);
			System.out.println("extend: " + message);
			String cust = customisations.getCustomisation("testWW1");
			System.out.println("A + B : " + cust);
			
			message = customisations.deleteCustomisation("test2WW1");
			System.out.println("delete : " + message);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
   	
	@Test
	public void testDelete() {
		try {
			Customisations customisations = Customisations.getInstance();
			String message = customisations.deleteCustomisation("testWW1");
			System.out.println(message);
			List<String> theCustomisations = customisations.getCustomisations();
			for(String cust : theCustomisations) {
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
			Customisations customisations = Customisations.getInstance();
			customisations.save();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}