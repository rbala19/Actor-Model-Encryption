package Database;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.*;




/**
 * Created by rbalakrishnan on 7/27/17.
 */
public class DatabaseConnectionPool {
	String tcp;
	String dbName;
	ComboPooledDataSource cpds;
	static final String QA = "iop3dev-sqlsvr-p.ie.intuit.net";
	static final String local = "windev";


	public DatabaseConnectionPool(String tcp, String dbName) {
		this.tcp = tcp;
		this.dbName = dbName;
		connect();
	}

	public boolean connect() {

		cpds = new ComboPooledDataSource();
		try {
			cpds.setDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver"); //loads the jdbc driver
			cpds.setJdbcUrl( "jdbc:sqlserver://" + QA + ":" + tcp + ";" +
				"databaseName=" + dbName + ";"  + "user=pubuser;password=pubuser" );
			cpds.setUser("pubuser");
			cpds.setPassword("pubuser");


			cpds.setMinPoolSize(5);
			cpds.setAcquireIncrement(5);
			cpds.setMaxPoolSize(35);

		} catch (Exception e) {}



		return true;
	}

	public DatabaseConnection extractConnection() {
		try {
			return new DatabaseConnection(cpds.getConnection());
		} catch (SQLException s) {}

		return null ;
	}

	public void closePool() {
		try {
			if (cpds != null) {
				cpds.close();
			}
		} catch (Exception e){}
	}
}

