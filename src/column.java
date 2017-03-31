/**
 * Column enumerated class. Class will store the column name for easy access and
 * the column position
 */
public enum column {

	/** The source column with value 10. */
	source(10),
	/** The temporary column with value 9. */
	temporary(9),
	/** The target column with value 1. */
	target(1);

	/** The column value. */
	private final int columnValue;

	/**
	 * Instantiates a new column.
	 *
	 * @param columnValue the columnValue
	 */
	column(int columnValue) {
		this.columnValue = columnValue;
	}

	/**
	 * Get the column value.
	 *
	 * @return the column value
	 */
	public int getValue() {
		return columnValue;
	}
}