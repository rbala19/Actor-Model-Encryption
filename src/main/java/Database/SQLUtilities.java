package Database;

import ActorModel.EncryptUtilities;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rbalakrishnan on 7/27/17.
 */

/**
 * Encapsulation of data to be encrypted
 */
public class SQLUtilities {

	/**
	 * Converts database cursor to in memory list
	 * @param rs DB cursor
	 * @return RowsToChange
	 */
	public static RowsToChange generateListFromResultSet(ResultSet rs) {
		SQLUtilities.RowsToChange result = new SQLUtilities().new RowsToChange();

		try {
			ResultSetMetaData rsmd = rs.getMetaData();

			int columnCount = rsmd.getColumnCount();

			String[] typeMapper = new String[columnCount];
			for (int i = 0 ; i < columnCount; i++) {
				typeMapper[i] = mapSQLTypeToJavaType(rsmd.getColumnTypeName(i + 1));
			}



			while (rs.next())
			{
				RowObject[] rowToStore = new RowObject[columnCount];
				for (int column = 1; column <= columnCount; column++)
				{
					Object value = rs.getObject(column);
					rowToStore[column - 1] = new SQLUtilities().new RowObject(value, typeMapper[column - 1]);
				}

				result.addRowToChange(new SQLUtilities().new Row(rowToStore));
			}

			rs.close();

			return result;

		} catch (SQLException s) {}

		return result;

	}

	/**
	 * Maps SQL datatype to java data types - used for detecting object type during runtime for encryption
	 * @param SQLType
	 * @return
	 */
	private static String mapSQLTypeToJavaType(String SQLType) {
		switch(SQLType) {
			case "varchar":
				return "String";
			case "int":
				return "Integer";
		}
			return "null";
	}

	/**
	 * Used to validate encryption
	 * @param original
	 * @param encrypted
	 * @param encryptionFlag
	 * @return true if encrypted version of original matches encrypted
	 */
	public static boolean checkEncryption(RowsToChange original, RowsToChange encrypted, boolean encryptionFlag) {
		if (original.length() != encrypted.length()) {
//			encrypted.toChange.removeAll(original.toChange);
			return false;
		}

		int length = original.length();
		for (int i = 0; i < length; i++) {
			Row orig = original.get(i);
			Row encr = encrypted.get(i);
			Row encrTest = orig.encryptRow(encryptionFlag);
			for (int j = 0; j < encr.length(); j++) {
				Object testValue = encrTest.get(j).getValue();
				Object value = encr.get(j).getValue();
				if (testValue == null && value == null) {
					continue;
				}
				if (!testValue.equals(value)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Fundamental class encapsulating data the requires encryption
	 */
	public class RowsToChange implements Iterable<Row>{
		private List<Row> toChange;

		public RowsToChange() {
			toChange = new ArrayList<>();
		}

		public RowsToChange(List<Row> toChange) {
			this.toChange = toChange;
		}

		public int length() {
			return toChange.size();
		}

		public void addRowToChange(Row r) {
			toChange.add(r);
		}

		public Iterator<Row> iterator() {
			return toChange.iterator();
		}

		/**
		 * Special iterator that iterates based on batch size; See BatchUtilities for usage
		 * @param iterateBy
		 * @return RowsToChange iterator
		 */
		public Iterator<RowsToChange> iterator(int iterateBy) {
			Iterator<RowsToChange> itr = new Iterator<RowsToChange>() {

				private int currentIndex = 0;

				public boolean hasNext() {
					return currentIndex < toChange.size();
				}

				public RowsToChange next() {
					int endIndex = currentIndex + iterateBy;
					if (endIndex > toChange.size()) {
						endIndex = toChange.size();
					}
					RowsToChange toReturn = new RowsToChange(toChange.subList(currentIndex, endIndex));
					currentIndex += iterateBy;
					return toReturn;
				}
			};

			return itr;
		}

		public RowsToChange encryptAll(boolean encryptFlag) {
			RowsToChange result = new RowsToChange(toChange.stream().map(temp -> {
				return temp.encryptRow(encryptFlag);
			}).collect(Collectors.toList()));
			return result;
		}

		public Row get(int i) {
			return toChange.get(i);
		}

	}

	/**
	 * A row in a RowsToChange
	 */
	public class Row implements Iterable<RowObject>{
		ArrayList<RowObject> container;

		public boolean equals(Object o) {
			if (o instanceof Row) {
				Row r = (Row) o;
				for (int i = 0; i < container.size(); i++) {
					if (r.get(i).getValue() == null && get(i).getValue() == null) {
						continue;
					}
					if (r.get(i).getValue() == null && get(i).getValue() != null) {
						return false;
					}
					if (r.get(i).getValue() != null && get(i).getValue() == null) {
						return false;
					}
					if (!r.get(i).getValue().equals(get(i).getValue())) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		public Row(RowObject... r) {
			container = new ArrayList<>();
			Collections.addAll(container, r);
		}

		public Iterator<RowObject> iterator() {
			return container.iterator();
		}

		public Row(ArrayList<RowObject> r) {
			container = r;
		}

		public Row encryptRow(boolean encryptFlag) {
			ArrayList<RowObject> encrypted = new ArrayList<>();
			int size = container.size();
			for (int i = 0; i < size - 1; i++) {
				RowObject rowObj = container.get(i);
				String type = rowObj.getType();
				if (encryptFlag) {
					encrypted.add(new RowObject(EncryptUtilities.encrypt(rowObj.getValue(), type), type));
				} else {
					encrypted.add(new RowObject(EncryptUtilities.decrypt(rowObj.getValue(), type), type));
				}
			}
			encrypted.add(container.get(size - 1));

			return new Row(encrypted);

		}

		public RowObject get(int index) {
			if (index >= container.size()) {
				throw new IndexOutOfBoundsException("Index value must be smaller than " + container.size());
			}
			return container.get(index);
		}

		public int length() {
			return container.size();
		}



	}

	/**
	 * A value in a Row; contains object type as a String in addition to the object itself
	 */

	public class RowObject {
		Object o;
		String type;

		public RowObject(Object o, String type) {
			this.o = o;
			this.type = type;
		}

		public Object getValue() {
			return o;
		}

		public String getType() {
			return type;
		}
	}

	/**
	 * Utility method for finding primary key automatically; not used currently
	 * @param connection
	 * @param tableName
	 * @return primary key
	 */
	public static String findPrimaryKey(DatabaseConnection connection, String tableName) {

		String query = "SELECT  i.name AS IndexName, OBJECT_NAME(ic.OBJECT_ID) AS TableName, COL_NAME(ic.OBJECT_ID,ic.column_id) AS ColumnName " +
				"FROM    sys.indexes AS i INNER JOIN sys.index_columns AS ic ON  i.OBJECT_ID = ic.OBJECT_ID AND i.index_id = ic.index_id " +
				"WHERE   i.is_primary_key = 1 AND OBJECT_NAME(ic.OBJECT_ID) = " + tableName;
		ResultSet result = null;
		ResultSet rs = null;
		try {
			result = connection.executeQuery(query);

			//Checks if result set is not empty
			if (result.isBeforeFirst()) {
				result.next();
				return result.getString(1);
			} else {
				//Uses default primary key of Table name + "id" if it exists
				DatabaseMetaData md = connection.con.getMetaData();
				rs = md.getColumns(null, null, tableName, tableName + "id");
				if (rs.next()) {
					return tableName + "id";
				} else {
					return null;
				}
			}
		} catch (SQLException s) {}
		finally {
			try {
				if (result != null) {
					result.close();
				}
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException s) {}
		}

		return null;

	}





}
