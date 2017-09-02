package ActorModelTesting;

import ActorModel.Supervisor;
import Database.DatabaseConnection;
import Database.DatabaseConnectionPool;
import Database.SQLUtilities;
import com.lightbend.akka.sample.Printer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by rbalakrishnan on 7/27/17.
 */
public class ActorModelEncryptTest {

	@BeforeClass
	public static void setup() {
		Supervisor.setup("QA", 50, "1433", "branch_test");
	}

	/**
	 * Checks to make sure a single request can complete
	 */
	@Test
	public void testRun() {

		Supervisor.addRequest(false, "companies", "companyid", "individualname");
		boolean success = Supervisor.run();

		assertTrue(success);
	}

	/**
	 * Tests encryption accuracy for a single request
	 */
	@Test
	public void testDatabaseEncryption() {
		String[] toEncrypt = new String[]{"ccardexpyear", "companyfilingtype"};
		String primaryKey = "CompanyID";
		String tableName = "Companies";

		DatabaseConnection test = Supervisor.getConnectionPool().extractConnection();
		String query = Supervisor.selectQueryBuilder(tableName, primaryKey, toEncrypt);
		ResultSet rs = test.executeQuery(query);
		SQLUtilities.RowsToChange starting = SQLUtilities.generateListFromResultSet(rs);


		boolean encryptFlag = false;
		Supervisor.addRequest(encryptFlag, tableName, primaryKey, toEncrypt);

		Supervisor.run();

		ResultSet encryptedRs = test.executeQuery(query);
		SQLUtilities.RowsToChange ending = SQLUtilities.generateListFromResultSet(encryptedRs);

		boolean match = SQLUtilities.checkEncryption(starting, ending, encryptFlag);

		assertTrue(match);
	}

	/**
	 * Tests adding multiple requests before running Supervisor
	 */
	@Test
	public void runMultipleQueries() {
		boolean encryptFlag = false;
		Supervisor.addRequest(encryptFlag, "Achtx", "achtxid", "individualname");
		Supervisor.addRequest(encryptFlag, "Companies", "CompanyID", "ccardexpyear", "CompanyFilingType");

		boolean success = Supervisor.run();

		assertTrue(success);
	}

	@AfterClass
	public static void closeConnections() {
		Supervisor.closeConnections();
	}
}

