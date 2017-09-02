package ActorModel;

import ActorModel.Actors.BatchActor;
import Database.DatabaseConnection;
import Database.DatabaseConnectionPool;
import Database.SQLUtilities;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by rbalakrishnan on 8/3/17.
 */

/**
 * Sets up, runs, and cleans up after actor model; Performs select calls
 */
public class Supervisor {

	public static DatabaseConnectionPool pool;

	//Incremented every time an update Actor completes the updateCall; this is a threadSafe data type
	public static AtomicInteger finishedCount = new AtomicInteger(0);
	public static ActorRef batchActor;
	public static int branchingFactor;
	public static List<BatchUtilities.EncryptRequest> queryQueue = new LinkedList<>();
	//A connection used to perform the select call
	public static DatabaseConnection addConnection;

	private static void connect(String database, String tcp, String dbName) {
		pool = new DatabaseConnectionPool(database, tcp, dbName);
		addConnection = pool.extractConnection();

	}

	private static boolean isConnected() {
		return pool != null;
	}

	private static void setUpActorSystem(int branchFact) {
		final ActorSystem system = ActorSystem.create("ActorEncrypt");
		try {
			branchingFactor = branchFact;
			batchActor = system.actorOf(BatchActor.props(branchingFactor), "batchActor");
		} catch (Exception e) {}

	}

	public static void setup(String whichDatabase, int branchFact, String tcp, String dbName) {
		if (!isConnected()) {
			Supervisor.connect(whichDatabase, tcp, dbName);
		}
		if (!actorSetupCompleted()) {
			setUpActorSystem(branchFact);
		}
	}


	private static boolean actorSetupCompleted() {
		return batchActor != null;
	}

	/**
	 * Adds request to queue
	 * @param encryptFlag
	 * @param tableName
	 * @param primaryKey
	 * @param toEncryptColumnNames
	 */
	public static void addRequest (boolean encryptFlag, String tableName, String primaryKey, String... toEncryptColumnNames) {
		String query = selectQueryBuilder(tableName, primaryKey, toEncryptColumnNames);
		ResultSet rs = addConnection.executeQuery(query);
		SQLUtilities.RowsToChange toChange = SQLUtilities.generateListFromResultSet(rs);
		queryQueue.add(new BatchUtilities().new EncryptRequest(encryptFlag, branchingFactor, tableName,
				toChange, primaryKey, toEncryptColumnNames));

	}

	/**
	 * Runs all requests in queue
	 * @return true if run successfully, false if encountered exception
	 */
	public static boolean run() {


		try {
			for (BatchUtilities.EncryptRequest request : queryQueue) {
//				request.testBatchIterator();
				batchActor.tell(request, ActorRef.noSender());
			}

			int queueSize = queryQueue.size();

			//Waits for finished count to equal number of requests * number of select actors
			while (true) {
				if (finishedCount.intValue() == (branchingFactor * queueSize)) {
					break;
				}

				Thread.sleep(250);
			}

			finishedCount.set(0);

			return true;
		} catch (InterruptedException e) {
			return false;
		}

	}

	public static void incrementFinishedCount() {
		finishedCount.getAndIncrement();
	}

	public static DatabaseConnectionPool getConnectionPool() {
		return pool;
	}

	public static String selectQueryBuilder(String tableName, String primaryKey, String... toEncryptColumnNames) {
		StringBuilder sb= new StringBuilder(Arrays.toString(toEncryptColumnNames));
		sb.deleteCharAt(0);
		sb.deleteCharAt(sb.length() - 1);
		String colNames = sb.toString();
		return "SELECT " + colNames + "," + primaryKey + " FROM " + tableName;
	}

	public static void closeConnections() {
		addConnection.closeConnection();
		pool.closePool();
	}

	/**
	 * Used for extracting the number of the end of a string; used for naming updateActors
	 * @param line String to be parsed
	 * @return number at end of string
	 */
	public static int getLastInt(String line)
	{
		int offset = line.length();
		for (int i = line.length() - 1; i >= 0; i--)
		{
			char c = line.charAt(i);
			if (Character.isDigit(c))
			{
				offset--;
			}
			else
			{
				if (offset == line.length())
				{
					// No int at the end
					return Integer.MIN_VALUE;
				}
				return Integer.parseInt(line.substring(offset));
			}
		}
		return Integer.parseInt(line.substring(offset));
	}


}
