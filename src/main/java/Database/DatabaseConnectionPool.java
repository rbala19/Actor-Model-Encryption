package Database;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by rbalakrishnan on 7/27/17.
 */

/**
 * Creates a Database Connection pool using the c3p0 library
 * Also contains mappings for the nicknames of database to which the actor model can connect
 */
public class DatabaseConnectionPool {
	String tcp;
	String dbName;
	ComboPooledDataSource cpds;
	static Map<String, String> DatabaseToURLMap;
	String currentDatabase;


	public DatabaseConnectionPool(String currentDatabase, String tcp, String dbName) {
		this.tcp = tcp;
		this.dbName = dbName;
		DatabaseToURLMap = new HashMap<>();
		DatabaseToURLMap.put("QA", "iop3dev-sqlsvr-p.ie.intuit.net");
		DatabaseToURLMap.put("local", "windev");

		this.currentDatabase = DatabaseToURLMap.get(currentDatabase);
		connect();
	}

	public boolean connect() {

		cpds = new ComboPooledDataSource();
		try {
			cpds.setDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver"); //loads the jdbc driver
			cpds.setJdbcUrl( "jdbc:sqlserver://" + currentDatabase + ":" + tcp + ";" +
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

