package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.utilities.*;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;
import java.math.BigInteger;

import org.nustaq.serialization.*;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.compress.compressors.CompressorException;

import org.apache.log4j.Logger;

import com.scienceminer.nerd.kb.db.KBDatabase.DatabaseType;
import org.wikipedia.miner.db.struct.*; 
import com.scienceminer.nerd.kb.model.Wikipedia;

import org.apache.hadoop.record.*;

import org.fusesource.lmdbjni.*;
import static org.fusesource.lmdbjni.Constants.*;

/**
 * Top language-independent KB instance, which is concretely stored as a set of LMDB databases.
 * This environment is normally unique for a complete knowledge base instance. 
 * 
 */
public class KBUpperEnvironment extends KBEnvironment {

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
		//singletonConf.registerClass();*/
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
		
		//KBDatabaseFactory dbFactory = new KBDatabaseFactory(this);
		
		databasesByType = new HashMap<DatabaseType, KBDatabase>();
		
		dbConcepts = buildConceptDatabase();//new ConceptDatabase(env, DatabaseType.concepts);
		databasesByType.put(DatabaseType.concepts, dbConcepts);
		
		dbProperties = buildPropertyDatabase();
		databasesByType.put(DatabaseType.properties, dbProperties);

		dbStatements = buildStatementDatabase();
		databasesByType.put(DatabaseType.statements, dbStatements);
	}

	/**
	 * Builds a KBEnvironment, by loading all of the data files stored in the given directory into persistent databases.
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
	public void buildEnvironment(NerdConfig conf, boolean overwrite) throws IOException, CompressorException {
		System.out.println("building Environment for upper knowledge base");	
		//check all files exist and are readable before doing anything
		
		File dataDirectory = new File(conf.getDataDirectory());

		// mapping wikipedia ids to wikidata id
		File wikidata = getDataFile(dataDirectory, "wikidataIds.csv");

		// definition of properties used in Wikidata
		File wikidataProperties = getDataFile(dataDirectory, "wikidata-properties.json");

		// the actual list of statemnents in Wikidata
		File wikidataStatements = getDataFile(dataDirectory, "wikidataStatements.csv");

		//now load databases
		File dbDirectory = new File(conf.getDbDirectory());
		if (!dbDirectory.exists())
			dbDirectory.mkdirs();

		//System.out.println("Building statistics db");
		dbConcepts.loadFromFile(wikidata, overwrite);

		//System.out.println("Building Properties db");
		dbProperties.loadFromFile(wikidataProperties, overwrite);

		//System.out.println("Building Relations db");
		dbStatements.loadFromFile(wikidataStatements, overwrite);
		
		System.out.println("Environment built - " + dbConcepts.getDatabaseSize() + " concepts.");
	}

	/*private static File getDataFile(File dataDirectory, String fileName) throws IOException {
		File file = new File(dataDirectory + File.separator + fileName);
		if (!file.canRead()) {
			Logger.getLogger(KBEnvironment.class).info(file + " is not readable");
			return null;
		} else
			return file;
	}*/

	/**
	 * Create a database associating integer id of page with an InfoBox relation (relation to other page)
	 */
	private StatementDatabase buildStatementDatabase() {
		return new StatementDatabase(this);
	}

	/**
	 * Create a database associating integer id of page with an InfoBox properties (attribute/value)
	 */
	private PropertyDatabase buildPropertyDatabase() {
		return new PropertyDatabase(this);
	}

	/**
	 * Create a database associating integer id of page with an InfoBox properties (attribute/value)
	 */
	private ConceptDatabase buildConceptDatabase() {
		return new ConceptDatabase(this);
	}

	/**
	 * @param sn the name of the desired statistic
	 * @return the value of the desired statistic
	 */
	public Long retrieveStatistic(StatisticName sn) {
		throw new UnsupportedOperationException();
	}
}
