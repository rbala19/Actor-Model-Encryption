package ActorModel;

import Database.SQLUtilities;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by rbalakrishnan on 8/4/17.
 */
public class BatchUtilities {

	public class EncryptRequest {

		String table;
		List<String> columnNames;
		String primaryKey;
		SQLUtilities.RowsToChange rowsToChange;
		Iterator<SQLUtilities.RowsToChange> rowIterator;
		int numBatches;
		boolean encryptFlag;


		public EncryptRequest(boolean encryptFlag, int numBatches, String table, SQLUtilities.RowsToChange rowsToChange, String primaryKey, String... columnNames) {
			this.table = table;
			this.rowsToChange = rowsToChange;
			int iterateBy = (int) Math.ceil(rowsToChange.length() / Double.valueOf(numBatches));
			this.rowIterator = this.rowsToChange.iterator(iterateBy);
			this.columnNames = Arrays.asList(columnNames);
			this.primaryKey = primaryKey;
			this.numBatches = numBatches;
			this.encryptFlag = encryptFlag;
		}

		public Batch createBatch() {
			return new Batch(rowIterator.next(), table, this);
		}

		public int testBatchIterator() {
			int count = 0;
			while (rowIterator.hasNext()) {
				rowIterator.next();
				count++;
			}

			return count;
		}

		public int getNumBatches() {
			return numBatches;
		}

		public boolean getEncryptFlag() {
			return encryptFlag;
		}
	}

	public class Batch {
		SQLUtilities.RowsToChange batch;
		String table;
		EncryptRequest request;

		public Batch(SQLUtilities.RowsToChange batch, String table, EncryptRequest request) {
			this.batch = batch;
			this.table = table;
			this.request = request;
		}

		public Batch encryptBatch(boolean encryptFlag) {
			return new Batch(batch.encryptAll(encryptFlag), table, request);
		}

		public List<String> getColumnNames() {
			return request.columnNames;
		}

		public String getPrimaryKey() {
			return request.primaryKey;
		}

		public String getTableName() {
			return table;
		}

		public EncryptRequest getRequest() {
			return request;
		}

		public SQLUtilities.RowsToChange getBatch() {
			return batch;
		}
	}
}
