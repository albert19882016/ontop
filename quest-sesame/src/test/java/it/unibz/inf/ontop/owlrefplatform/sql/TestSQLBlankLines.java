package it.unibz.inf.ontop.owlrefplatform.sql;

import it.unibz.inf.ontop.injection.QuestConfiguration;
import it.unibz.inf.ontop.owlrefplatform.core.SQLExecutableQuery;
import it.unibz.inf.ontop.owlrefplatform.owlapi.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import static org.junit.Assert.assertFalse;

/**
 * Tests that the generated SQL contains no blank lines
 * This leads to problems if the SQL is copied to the oracle command line client
 * @author Dag Hovland
 *
 */

public class TestSQLBlankLines {

	private QuestOWLConnection conn;
	private QuestOWL reasoner;


	final String owlfile = "src/test/resources/sqlgenerator/blanklines.owl";
	final String obdafile = "src/test/resources/sqlgenerator/blanklines.obda";

	private Connection sqlConnection;



	@After
	public void tearDown() throws Exception{
		conn.close();
		reasoner.dispose();
		if (!sqlConnection.isClosed()) {
			java.sql.Statement s = sqlConnection.createStatement();
			try {
				s.execute("DROP ALL OBJECTS DELETE FILES");
			} catch (SQLException sqle) {
				System.out.println("Table not found, not dropping");
			} finally {
				s.close();
				sqlConnection.close();
			}
		}
	}


	@Before
	public void setUp() throws Exception {
		try {
			sqlConnection= DriverManager.getConnection("jdbc:h2:mem:countries","sa", "");
			java.sql.Statement s = sqlConnection.createStatement();

			try {
				String text = new Scanner( new File("src/test/resources/sqlgenerator/create-h2.sql") ).useDelimiter("\\A").next();
				s.execute(text);
				//Server.startWebServer(sqlConnection);

			} catch(SQLException sqle) {
				System.out.println("Exception in creating db from script");
			}

			s.close();
			// Loading the OWL file

			// Creating a new instance of the reasoner
			QuestOWLFactory factory = new QuestOWLFactory();
	        QuestConfiguration config = QuestConfiguration.defaultBuilder()
					.ontologyFile(owlfile)
					.nativeOntopMappingFile(obdafile)
					.build();
	        reasoner = factory.createReasoner(config);

			// Now we are ready for querying
			conn = reasoner.getConnection();


		} catch (Exception exc) {
			try {
				tearDown();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}	
	}


	@Test
	public void testNoSQLBlankLines() throws Exception {

		String query = "PREFIX : <http://www.semanticweb.org/ontologies/2013/7/untitled-ontology-150#> SELECT * WHERE {?x a :Class1}";
		QuestOWLStatement st = conn.createStatement();


		String sql = ((SQLExecutableQuery)st.getExecutableQuery(query)).getSQL();
		boolean m = sql.matches("(?ms)(.*)\\n\\n(.*)");
		assertFalse(m);
	}

}
