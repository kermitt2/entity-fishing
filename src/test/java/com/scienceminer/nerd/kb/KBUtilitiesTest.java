package com.scienceminer.nerd.kb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.scienceminer.nerd.kb.model.Article;
import com.scienceminer.nerd.kb.model.Page;
import com.scienceminer.nerd.utilities.NerdConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class KBUtilitiesTest {
	
	//private LowerKnowledgeBase wikipedia = null;

	@Before
	public void setUp() {
		try {
        	UpperKnowledgeBase.getInstance(); 
        	//wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf("en");
        } catch(Exception e) {
        	e.printStackTrace();
        }
	}

	@Test
	public void testGetStatements(){
		try{
			List<Statement> statements = UpperKnowledgeBase.getInstance().getStatements("Q76");
			System.out.println(statements);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	@Test
	public void testGetReverseStatements(){
		try {
			List<Statement> reverseStatements = UpperKnowledgeBase.getInstance().getReverseStatements("Q76");
			System.out.println(reverseStatements);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	@Test
	public void testImmediateTaxonParents() {
		try {
			List<String> parents = UpperKnowledgeBase.getInstance().getParentTaxons("Q7377");
System.out.println(parents);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testLoadFeatures(){
		try{
			Map<String, List<String>> features = UpperKnowledgeBase.getInstance().getFeatures();
			System.out.println("Features = " + features);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testLoadFeaturesNoValue(){
		try{
			List<String> featuresNoValue = UpperKnowledgeBase.getInstance().getFeaturesNoValue();
			System.out.println("Features No Value = " + featuresNoValue);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testFullTaxonParents() {
		try {
			List<String> parents = UpperKnowledgeBase.getInstance().getFullParentTaxons("Q18498");
System.out.println("Q18498: " + parents);

			parents = UpperKnowledgeBase.getInstance().getFullParentTaxons("Q3200306");
System.out.println("Q3200306: " + parents);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testKingdomMethods() {
		try {
			boolean isAnimal = KBUtilities.isAnimal("Q18498");
			System.out.println("Q18498.isAnimal = " + isAnimal);

			isAnimal = KBUtilities.isAnimal("Q3200306");
			System.out.println("Q3200306.isAnimal = " + isAnimal);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*@After
	public void testClose() {
		try {
			wikipedia.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}*/
}