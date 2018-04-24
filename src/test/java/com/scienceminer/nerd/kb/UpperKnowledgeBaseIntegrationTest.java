package com.scienceminer.nerd.kb;

import com.scienceminer.nerd.kb.db.KBUpperEnvironment;
import com.scienceminer.nerd.utilities.NerdConfig;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class UpperKnowledgeBaseIntegrationTest {
    private UpperKnowledgeBase target;
    private Path tempDirectory;

    @Before
    public void setUp() throws Exception {
        tempDirectory = Files.createTempDirectory(Paths.get("./target"), "tmp");

        //Fetch sample from the resource directory and load it as if loaded to LMDB
        final Path wikidataDb = Files.createTempDirectory(tempDirectory, "test-db-kb");
        final Path dataDirectoryWikidata = Files.createTempDirectory(tempDirectory, "dataDirectoryWikidata");

        //Wikidata ids
        InputStream isWikidataIds = this.getClass().getResourceAsStream("wikidataIds.csv");
        File fileWikidataIds  = new File(dataDirectoryWikidata.toFile() + File.separator + "wikidataIds.csv");
        FileUtils.copyToFile(isWikidataIds, fileWikidataIds);

        //Wikidata dump
        InputStream isWikidataDump = this.getClass().getResourceAsStream("latest-all.json.bz2");
        File fileWikidataDump  = new File(dataDirectoryWikidata.toFile() + File.separator + "latest-all.json.bz2");
        FileUtils.copyToFile(isWikidataDump, fileWikidataDump);

        target = new UpperKnowledgeBase(true) {
            private KBUpperEnvironment env;

            public void init() {
                //We only load wikidata, which is what we are going to test
                NerdConfig conf = new NerdConfig();
                conf.setDataDirectory(dataDirectoryWikidata.toFile().getAbsolutePath());
                conf.setDbDirectory(wikidataDb.toFile().getAbsolutePath());
                this.env = new KBUpperEnvironment(conf);

                try {
                    this.getEnv().buildEnvironment(conf, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            protected KBUpperEnvironment getEnv() {
                return this.env;
            }
        };

        target.init();

//        wikipedia = UpperKnowledgeBase.getInstance().getWikipediaConf("en");
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(tempDirectory.toFile());
    }

    @Test
    public void testConceptLoader() throws Exception {
        assertThat(target.getEntityCount(), is(9l));
    }

    @Test
    public void testGetConcept() throws Exception {
        Concept concept = target.getConcept("Q1000000");
        assertThat(concept, is(not(nullValue())));
        assertThat(concept.getId(), is("Q1000000"));
        assertThat(concept.getPageIdByLang("en"), is(43228764));
        assertThat(target.getPageIdByLang("Q1000000", "en"), is(43228764));
    }

    @Test
    public void testStatements() throws Exception {
        final List<Statement> statements = target.getStatements("Q22");
        assertThat(statements, hasSize(98));
        assertThat(statements.get(0).getConceptId(), is("Q22"));
        assertThat(statements.get(0).getPropertyId(), is("P1549"));
    }


    @Ignore("need to find use cases")
    @Test
    public void getProperties() throws Exception {
        final Property properties = target.getProperty("P1549");
        assertThat(properties, is(not(nullValue())));
        assertThat(properties.getId(), is("P1549"));
        assertThat(properties.getName(), is("P1549"));
    }


//    @Test
//    public void testPageId() {
//        Page page = wikipedia.getPageById(3966054);
//
//        System.out.println("pageId:" + page.getId());
//        System.out.println(page.getTitle());
//    }

//    @Test
//    public void testTitle() {
//        Page page = wikipedia.getArticleByTitle("Mexico");
//        assertThat(page, is(not(null)));
//        System.out.println(page.getTitle());
//        System.out.println("pageId:" + page.getId());
//
//    }


	/*@Test
	public void testProperties() {
		try {
			List<Property> properties = wikipedia.getProperties(3966054);

			System.out.println("properties for Mexico (" + 3966054 + ")");
			if (properties != null) {
				for (Property property : properties)
					System.out.println(property.toJson());
			} else 
				System.out.println("no property found");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}*/

	/*@Test
	public void testRelations() {
		try {
			List<Relation> relations = wikipedia.getRelations(3966054);

			System.out.println("relations for Mexico (" + 3966054 + ")");
			if (relations != null) {
				for (Relation relation : relations)
					System.out.println(relation.toJson());
			} else 
				System.out.println("no relation found");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}*/
	
	/*@After
	public void testClose() {
		try {
			UpperKnowledgeBase.getInstance().close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}*/
}

