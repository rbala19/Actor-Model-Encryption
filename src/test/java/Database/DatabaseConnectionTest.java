package Database;

import com.lightbend.akka.sample.Printer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * Created by rbalakrishnan on 7/27/17.
 */
public class DatabaseConnectionTest {


	@Test
	public void testDatabaseConnection() {
		DatabaseConnectionPool pool = new DatabaseConnectionPool("local","1433", "branch_test");
		DatabaseConnection connection = pool.extractConnection();
		ResultSet test = connection.executeQuery("Select top 10 * from companies");
		boolean notEmpty = false;
		try {
			notEmpty = test.isBeforeFirst();
		} catch (SQLException s) {}

		assertEquals(notEmpty, true);
	}
}
