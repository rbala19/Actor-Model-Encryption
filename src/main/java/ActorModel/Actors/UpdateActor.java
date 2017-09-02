package ActorModel.Actors;

import ActorModel.BatchUtilities;
import ActorModel.Supervisor;
import Database.DatabaseConnection;
import Database.SQLUtilities;
import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by rbalakrishnan on 7/28/17.
 */

/**
 * Lowest level in hierarchy; Receives encrypted batch and performs update call to database.
 */
public class UpdateActor extends AbstractActor {

	DatabaseConnection connection;
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public static Props props() {
		return Props.create(UpdateActor.class, () -> new UpdateActor());
	}

	public UpdateActor() {
		this.connection = Supervisor.getConnectionPool().extractConnection();
	}

	/**
	 * Upon receiving an encrypted batch, makes update call, logs completion, closes connection, and terminates self
	 */
	public Receive createReceive() {
		return receiveBuilder()
				.match(BatchUtilities.Batch.class, batch -> {
					updateDatabase(batch);
					log.info("Completed Batch");
					notifySupervisorOfTermination();
					Thread.sleep(2000);
					connection.closeConnection();
					getContext().stop(getSelf());
				}).build();

	}


	/**
	 * Updates Database
	 * @param batch Encrypted batch received from select actor
	 */
	public void updateDatabase(BatchUtilities.Batch batch) {

		connection.turnOffAutoCommit();

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		try {

			String updateQuery = buildQueryFramework(batch.getTableName(), batch.getColumnNames(), batch.getPrimaryKey());

			PreparedStatement batchUpdate = connection.createPreparedStatement(updateQuery);

			for (SQLUtilities.Row r : batch.getBatch()) {
				int index = 1;
				for (SQLUtilities.RowObject obj: r) {
					batchUpdate.setObject(index, obj.getValue());
					index++;
				}


				batchUpdate.addBatch();
			}

			batchUpdate.executeBatch();

			connection.commit();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds update call
	 * @param table
	 * @param columnNames
	 * @param primaryKey
	 * @return Update Query
	 */
	private String buildQueryFramework(String table, List<String> columnNames, String primaryKey) {


		StringBuilder updateQuery = new StringBuilder("UPDATE " + table
				+ " SET");

		//Add Column Names
		for (String column: columnNames) {
			updateQuery.append(" " + column + " = ?,");
		}

		updateQuery.deleteCharAt(updateQuery.length() - 1);

		updateQuery.append(" WHERE " + primaryKey + " = " + "?");

		return updateQuery.toString();
	}

	/**
	 * Increments Supervisor's finish count
	 */
	private void notifySupervisorOfTermination() {
		Supervisor.incrementFinishedCount();
	}


}
