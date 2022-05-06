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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;

public class CategoriesIntegrationTest {
	
	private LowerKnowledgeBase wikipedia = null;

	@Before
	public void setUp() {
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            NerdConfig conf = mapper.readValue(new File("data/config/wikipedia-en.yaml"), NerdConfig.class);
        	wikipedia = new LowerKnowledgeBase(conf); 
        } catch(Exception e) {
        	e.printStackTrace();
        }
	}
   	
	@Test
	public void testCategoryHierarchy() {
		try {
			Page page = wikipedia.getPageById(32768);
			assertThat(page.getId(), is(32768));

			com.scienceminer.nerd.kb.model.Category[] categories = ((Article)page).getParentCategories();

			/*for(int l=0; l<categories.length;l++){
			    System.out.println(categories[l].getId());
			}*/

			assertThat(categories[0].getId(), is(1116081));
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