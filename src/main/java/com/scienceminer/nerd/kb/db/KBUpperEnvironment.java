package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.utilities.*;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;

import org.nustaq.serialization.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import com.scienceminer.nerd.kb.model.hadoop.*; 
import com.scienceminer.nerd.kb.LowerKnowledgeBase;
import com.scienceminer.nerd.kb.*;

import org.apache.hadoop.record.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * Top language-independent KB instance, which is concretely stored as a set of LMDB databases.
 * This environment is normally unique for a complete knowledge base instance. 
 * 
 */
public class KBUpperEnvironment extends KBEnvironment {
	private static final Logger LOGGER = LoggerFactory.getLogger(KBUpperEnvironment.class);	

	// the different databases of the KB
	private ConceptDatabase dbConcepts = null;
	private StatementDatabase dbStatements = null;
	private PropertyDatabase dbProperties = null;

	/**
	 * Constructor
	 */	
	public KBUpperEnvironment(NerdConfig conf) {
		super(conf);
		// register classes to be serialized
		//singletonConf.registerClass(Property.class, Statement.class);
		initDatabases();
	}
	
	/**
	 * Returns the {@link DatabaseType#page} database
	 */
	public ConceptDatabase getDbConcepts() {
		return dbConcepts;
	}

	/**
	 * Returns the {@link DatabaseType#properties} database
	 */
	public PropertyDatabase getDbProperties() {
		return dbProperties;
	}

	/**
	 * Returns the {@link DatabaseType#statements} database
	 */
	public StatementDatabase getDbStatements() {
		return dbStatements;
	}
	
	@Override
	protected void initDatabases() {
		System.out.println("\ninit upper level language independent environment");
				
		databasesByType = new HashMap<DatabaseType, KBDatabase>();
		
		dbConcepts = buildConceptDatabase();//new ConceptDatabase(env, DatabaseType.concepts);
		databasesByType.put(DatabaseType.concepts, dbConcepts);
		
		dbProperties = buildPropertyDatabase();
		databasesByType.put(DatabaseType.properties, dbProperties);

		dbStatements = buildStatementDatabase();
		databasesByType.put(DatabaseType.statements, dbStatements);
	}

	/**
	 * Builds a KBEnvironment by loading all of the data files stored in the given directory into persistent databases.
	 * 
	 * It will not create the environment or any databases unless all of the required files are found in the given directory. 
	 * 
	 * It will not delete any existing databases, and will only overwrite them if explicitly specified (even if they are incomplete).
	 * 
	 * @param conf a configuration specifying where the databases are to be stored, etc.
	 * @param overwrite true if existing databases should be overwritten, otherwise false
	 * @throws IOException if any of the required files cannot be read
	 */
	@Override
	public void buildEnvironment(NerdConfig conf, boolean overwrite) throws Exception {
		System.out.println("building Environment for upper knowledge base");	
		//check all files exist and are readable before doing anything
		
		File dataDirectory = new File(conf.getDataDirectory());

		// mapping wikipedia ids to wikidata id
		File wikidata = getDataFile(dataDirectory, "wikidataIds.csv");

		// definition of properties used in Wikidata
		//File wikidataProperties = getDataFile(dataDirectory, "wikidata-properties.json");

		// the actual list of statements in Wikidata with the JSON Wikidata dump file
		File wikidataStatements = getDataFile(dataDirectory, "latest-all.json.bz2");

		//now load databases
		File dbDirectory = new File(conf.getDbDirectory());
		if (!dbDirectory.exists())
			dbDirectory.mkdirs();

		//System.out.println("Building Concept db");                                                     	
		dbConcepts.loadFromFile(wikidata, overwrite);

		//System.out.println("Building Properties db");
		dbProperties.loadFromFile(wikidataStatements, overwrite);

		//System.out.println("Building Statement db");
		dbStatements.loadFromFile(wikidataStatements, overwrite);
		
		System.out.println("Environment built - " + dbConcepts.getDatabaseSize() + " concepts.");
	}

	private StatementDatabase buildStatementDatabase() {
		return new StatementDatabase(this);
	}

	private PropertyDatabase buildPropertyDatabase() {
		return new PropertyDatabase(this);
	}

	private ConceptDatabase buildConceptDatabase() {
		return new ConceptDatabase(this);
	}

	public Long retrieveStatistic(StatisticName sn) {
		throw new UnsupportedOperationException();
	}
}
