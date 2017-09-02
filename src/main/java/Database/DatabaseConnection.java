package Database;

import java.sql.*;

/**
 * Created by rbalakrishnan on 8/3/17.
 */

/**
 * Represents a singular Database Connection
 */
public class DatabaseConnection {

	Connection con;

	DatabaseConnection(Connection con) {
		this.con = con;
	}

	public void closeConnection() {

		if (con != null) try {con.close(); } catch (Exception e) {}
	}



	public ResultSet executeQuery(String query) {
		ResultSet returnSet = null;
		try {
			Statement stmt = con.createStatement();
			stmt.setFetchSize(100);
			returnSet = stmt.executeQuery(query);
		} catch (SQLException s) {}
		finally {
			return returnSet;
		}
	}

	public void turnOffAutoCommit() {
		try {
			con.setAutoCommit(false);
		} catch (SQLException s) {}
	}

	public DatabaseMetaData getMetaData() {
		try {
			return con.getMetaData();
		} catch (Exception e) {}

		return null;
	}

	public PreparedStatement createPreparedStatement(String query) {
		try {
			return con.prepareStatement(query);
		} catch (Exception e) {}

		return null;
	}

	public void commit() {
		try {
			con.commit();
		} catch (Exception e) {}
	}
}
