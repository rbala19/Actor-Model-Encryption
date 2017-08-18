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
public class UpdateActor extends AbstractActor {

	DatabaseConnection connection;
	BatchUtilities.Batch encryptedBatch;
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public static Props props() {
		return Props.create(UpdateActor.class, () -> new UpdateActor());
	}

	public UpdateActor() {
		this.encryptedBatch = null;

		this.connection = Supervisor.getConnectionPool().extractConnection();
	}

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

	private void notifySupervisorOfTermination() {
		Supervisor.incrementFinishedCount();
	}


}
