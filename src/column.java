import java.util.ArrayList;
import java.util.Stack;

public class Column {
    private Stack<Integer> blocks;
    private ColumnType type;
    private static ArrayList<Column> columns;

    public Column(ColumnType type, int[] blocks) {
	this.type = type;
	this.blocks = new Stack<>();
	if (blocks != null) {
	    for (int block : blocks) {
		this.blocks.add(block);
	    }
	}

	if (columns == null)
	    columns = new ArrayList<>();
	columns.add(this);
    }

    public static Column getColumnByType(ColumnType type) {
	for (Column column : columns) {
	    if (column.getType() == type)
		return column;
	}
	return null;
    }

    public static ArrayList<Column> getColumns() {
	return columns;
    }

    public static boolean columnExists(int columnIndex) {
	boolean exists = false;
	for (Column column : columns) {
	    if (column.type.getValue() == columnIndex) {
		exists = true;
		break;
	    }
	}
	return exists;
    }

    public ColumnType getType() {
	return type;
    }

    public int getHeight() {
	int totalBlockHeights = 0;
	for (int block : this.blocks) {
	    totalBlockHeights += block;
	}
	return totalBlockHeights;
    }

    public Stack<Integer> getBlocks() {
	return this.blocks;
    }

    public int getTopBlockHeight() {
	return blocks.size() == 0 ? 0 : this.blocks.peek();
    }

    public void addBlock(int blockHeight) {
	this.blocks.push(blockHeight);
    }

    public int removeBlock() {
	return this.blocks.pop();
    }

}
