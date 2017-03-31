import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

class RobotControl {

	/** The r. */
	private Robot r;

	/** The maximum allowed height value for arm 1. */
	private final int maxArmOneHeight = 14;

	/** The minimum allowed height value for arm 1. */
	private final int minArmOneHeight = 2;

	/** The current height value for arm 1. */
	private int armOneCurrentHeight = 2;

	/** The maximum allowed width value for arm 2. */
	private final int maxArmTwoWidth = 10;

	/** The minimum allowed width value for arm 2. */
	private final int minArmTwoWidth = 1;

	/** The current width value for arm 1. */
	private int armTwoCurrentWidth = 1;

	/** The minimum depth value for arm 3. */
	private final int minArmThreeDepth = 0;

	/** The current depth value for arm 3. */
	private int armThreeCurrentDepth = 0;

	/** Stack containing all the target column blocks. */
	private Stack<Integer> targetBlocks;

	/** Stack containing all the source column blocks. */
	private Stack<Integer> sourceBlocks;

	/** Stack containing all the temporary column blocks. */
	private Stack<Integer> temporaryBlocks;

	/** Bars heights. */
	private int[] originalBarHeights;

	/** Block heights. */
	private int[] originalblockHeights;

	/**
	 * Instantiates a new robot control.
	 *
	 * @param r
	 *            Robot object
	 */
	public RobotControl(Robot r) {
		this.r = r;
	}

	private void stressTest() {
		moveBlock(column.source, column.target);
		moveBlock(column.source, column.temporary);
		column[] columns = { column.source, column.target, column.temporary };
		while (true) {
			int fromColumnRandom = ThreadLocalRandom.current().nextInt(0, 3);
			int toColumnRandom = ThreadLocalRandom.current().nextInt(0, 3);
			column c = columns[fromColumnRandom];
			if (c == column.source) {
				if (sourceBlocks.size() == 0)
					continue;
			} else if (c == column.target) {
				if (targetBlocks.size() == 0)
					continue;
			} else if (c == column.temporary) {
				if (temporaryBlocks.size() == 0)
					continue;
			}
			while (toColumnRandom == fromColumnRandom)
				toColumnRandom = ThreadLocalRandom.current().nextInt(0, 3);
			moveBlock(c, columns[toColumnRandom]);
		}
	}

	/**
	 * Prints the debug variables.
	 */
	private void printDebugVariables() {
		System.out.println("======= Debug ======");
		System.out.println("Arm One Current height:" + this.armOneCurrentHeight);
		System.out.println("Arm Two Current width:" + this.armTwoCurrentWidth);
		System.out.println("Arm Three Current Depth:" + this.armThreeCurrentDepth);
		System.out.println("-----");
		System.out.println("Source Blocks size:" + sourceBlocks.size());
		System.out.println("Source Blocks height:" + getSourceBlocksHeight());
		System.out.println("-----");
		System.out.println("Temporary Blocks size:" + temporaryBlocks.size());
		System.out.println("Temporary Blocks height:" + getTemporaryBlocksHeight());
		System.out.println("-----");
		System.out.println("Target Blocks size:" + targetBlocks.size());
		System.out.println("Target Blocks height:" + getTargetBlocksHeight());
		System.out.println("====================");
	}

	/**
	 * Reset the robot arms to the default value.
	 */
	private void resetRobot() {
		changeArmTwoWidth(1); // default arm width value = 1
		changeArmThreeDepth(0); // default arm depth value = 0
		// The target block height will equal the blocks height, otherwise it
		// will be 2
		changeArmOneHeight(getTargetBlocksHeight() == 0 ? 2 : (getTargetBlocksHeight() + 1));
	}

	/**
	 * Picks the top block from a given column. by doing the following steps:
	 * 1- Calculate the arm depth based on the total blocks height in the given column
	 * 2- Lowers arm 3 to the calculated depth
	 * 3- Picks the block
	 * 4- Rise Arm 3 Depth to 0
	 *
	 * Arm 3 starting depth is always 0 
	 * @param fromColumn the column which contains the block
	 */
	private void pickBlock(column fromColumn) {
		int stepsToMoveArmThree = 0;
		if (fromColumn == column.source) {
			stepsToMoveArmThree = this.armOneCurrentHeight - getSourceBlocksHeight() - 1;
		} else if (fromColumn == column.temporary) {
			stepsToMoveArmThree = this.armOneCurrentHeight - getTemporaryBlocksHeight() - 1;
		} else if (fromColumn == column.target) {
			stepsToMoveArmThree = this.armOneCurrentHeight - getTargetBlocksHeight() - 1;
		}
		changeArmThreeDepth(stepsToMoveArmThree);
		r.pick();
		changeArmThreeDepth(0);
	}

	/**
	 * Drop block.
	 *
	 * @param fromColumn
	 *            the from column
	 * @param toColumn
	 *            the to column
	 */
	private void dropBlock(column fromColumn, column toColumn) {
		int stepsToMoveArmThree = 0;
		if (toColumn == column.source) {
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getSourceBlocksHeight() - lastBlockHeight(fromColumn);
			sourceBlocks.push(lastBlockHeight(fromColumn));
		} else if (toColumn == column.target) {
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getTargetBlocksHeight() - lastBlockHeight(fromColumn);
			targetBlocks.push(lastBlockHeight(fromColumn));
		} else {
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getTemporaryBlocksHeight()
					- lastBlockHeight(fromColumn);
			temporaryBlocks.push(lastBlockHeight(fromColumn));
		}
		removeLastBlockFromColumnList(fromColumn);
		changeArmThreeDepth(stepsToMoveArmThree);
		r.drop();
		changeArmThreeDepth(0);
		printDebugVariables();
	}

	/**
	 * Removes the last block from column list.
	 *
	 * @param columnType
	 *            the column type
	 * @return the height of the block which has being removed
	 */
	private int removeLastBlockFromColumnList(column columnType) {
		if (columnType == column.source)
			return sourceBlocks.pop();
		else if (columnType == column.temporary)
			return temporaryBlocks.pop();
		else if (columnType == column.target)
			return targetBlocks.pop();
		return 0;
	}

	/**
	 * Change Arm 1 height to a given value.
	 * It works in both directions, Up and Down.
	 *
	 * @param newHeight the new height for the arm
	 */
	private void changeArmOneHeight(int newHeight) {

		if (this.armOneCurrentHeight == newHeight) { // arm height should remain as is
			return;
		}
		else if (newHeight > armOneCurrentHeight) { // move the arm up to reach the new given height
			for (int x = this.armOneCurrentHeight; x < newHeight; x++) {
				r.up();
				this.armOneCurrentHeight = x + 1;
			}
		}
		else if (newHeight < armOneCurrentHeight) { // move the arm down to reach the new given height
			for (int x = this.armOneCurrentHeight; x > newHeight; x--) {
				r.down();
				this.armOneCurrentHeight = x - 1;
			}
		}
	}

	/**
	 * Change Arm 2 width to a given value.
	 * It works in both directions, extend and contract (forward and backward).
	 * 
	 * @param newWidth the new width for the arm
	 */
	private void changeArmTwoWidth(int newWidth) {

		if (this.armTwoCurrentWidth == newWidth) { // arm width should remain as is
			return;
		}
		else if (newWidth > armTwoCurrentWidth) { // move the arm forward to reach the new given width
			for (int x = this.armTwoCurrentWidth; x < newWidth; x++) {
				r.extend();
				this.armTwoCurrentWidth = x + 1;
			}
		}
		else if (newWidth < armTwoCurrentWidth) { // move the arm backward to reach the new given width
			for (int x = this.armTwoCurrentWidth; x > newWidth; x--) {
				r.contract();
				this.armTwoCurrentWidth = x - 1;
			}
		}
	}

	/**
	 * Change Arm 3 depth to a given value.
	 * It works in both directions, lower and rise (up and down).
	 * 
	 * @param newDepth the new depth for the arm
	 */
	private void changeArmThreeDepth(int newDepth) {

		if (this.armThreeCurrentDepth == newDepth) { // arm depth should remain as is
			return;
		}

		else if (newDepth > armThreeCurrentDepth) { // move the arm down to reach the new given depth
			for (int x = this.armThreeCurrentDepth; x < newDepth; x++) {
				r.lower();
				this.armThreeCurrentDepth = x + 1;
			}
		}
		else if (newDepth < armThreeCurrentDepth) { // move the arm up to reach the new given depth
			for (int x = this.armThreeCurrentDepth; x > newDepth; x--) {
				r.raise();
				this.armThreeCurrentDepth = x - 1;
			}
		}
	}

	/**
	 * Last block height.
	 *
	 * @param blockInColumn
	 *            the block in column
	 * @return the int
	 */
	private int lastBlockHeight(column blockInColumn) {
		if (blockInColumn == column.source)
			return sourceBlocks.size() == 0 ? 0 : this.sourceBlocks.peek();
		else if (blockInColumn == column.target)
			return targetBlocks.size() == 0 ? 0 : this.targetBlocks.peek();
		else if (blockInColumn == column.temporary)
			return temporaryBlocks.size() == 0 ? 0 : this.temporaryBlocks.peek();

		System.out.println("Error in blockHeight");
		return 0;
	}

	/**
	 * Gets the highest bar.
	 *
	 * @return the highest bar
	 */
	private int getHighestBar() {
		int highestValue = 0;
		for (int x = 0; x < originalBarHeights.length; x++) {
			if (originalBarHeights[x] > highestValue)
				highestValue = originalBarHeights[x];
		}
		return highestValue;
	}

	/**
	 * Gets the temporary blocks height.
	 *
	 * @return the temporary blocks height
	 */
	private int getTemporaryBlocksHeight() {
		int totalBlockHeights = 0;
		for (int block : this.temporaryBlocks) {
			totalBlockHeights += block;
		}
		return totalBlockHeights;
	}

	/**
	 * Gets the target blocks height.
	 *
	 * @return the target blocks height
	 */
	private int getTargetBlocksHeight() {
		int totalBlockHeights = 0;
		for (int block : this.targetBlocks) {
			totalBlockHeights += block;
		}
		return totalBlockHeights;
	}

	/**
	 * Gets the source blocks height.
	 *
	 * @return the source blocks height
	 */
	private int getSourceBlocksHeight() {
		int sourceBlocksHeight = 0;
		for (int i : this.sourceBlocks) {
			sourceBlocksHeight += i;
		}
		return sourceBlocksHeight;
	}

	private int blockPass(column fromColumn, column toColumn) {
		int blockHeight = lastBlockHeight(fromColumn);
		int maxHeight = 0;
		int x = Math.min(fromColumn.getValue(), toColumn.getValue());
		int tmpMax = Math.max(fromColumn.getValue(), toColumn.getValue());
		for (; x <= tmpMax; x++) {
			if (x == fromColumn.getValue())
				continue;
			if (x == column.source.getValue()) {
				maxHeight = Math.max(maxHeight, getSourceBlocksHeight());
			} else if (x == column.target.getValue()) {
				maxHeight = Math.max(maxHeight, getTargetBlocksHeight());
			} else if (x == column.temporary.getValue()) {
				maxHeight = Math.max(maxHeight, getTemporaryBlocksHeight());
			} else {
				maxHeight = Math.max(maxHeight, getHighestBar());
			}
		}
		return blockHeight + maxHeight;
	}

	private int armPass(column fromColumn, column toColumn) {
		int maxHeight = 0;
		int tmpMax = Math.max(fromColumn.getValue(), toColumn.getValue());
		for (int x = 1; x <= tmpMax; x++) {
			if (x == column.source.getValue()) {
				maxHeight = Math.max(maxHeight, getSourceBlocksHeight());
			} else if (x == column.target.getValue()) {
				maxHeight = Math.max(maxHeight, getTargetBlocksHeight());
			} else if (x == column.temporary.getValue()) {
				maxHeight = Math.max(maxHeight, getTemporaryBlocksHeight());
			} else {
				maxHeight = Math.max(maxHeight, getHighestBar());
			}
		}
		return maxHeight;
	}

	/**
	 * Calculate the height for Arm one based on the following Source blocks
	 * height Target Blocks height Highest bar available
	 *
	 * @param fromColumn
	 *            the from column
	 * @param toColumn
	 *            the to column
	 * @return the int
	 */
	private int calculateHeight(column fromColumn, column toColumn) {
		System.out.println("Arm Pass:" + armPass(fromColumn, toColumn));
		System.out.println("Block Pass:" + blockPass(fromColumn, toColumn));
		return Math.max(armPass(fromColumn, toColumn), blockPass(fromColumn, toColumn)) + 1;
	}

	/**
	 * Moves the top block from a given column to another (works in both directions)
	 *
	 * @param fromColumn the column which the block will be picked from  
	 * @param toColumn the column which the block will be dropped in
	 */
	private void moveBlock(column fromColumn, column toColumn) {
		/*
		 * The main steps for the robot to move a block from one column to the other are:
		 * 1- Go up to a calculated height 
		 * 2- Extend the arm to the starting column
		 * 3- Pick the block
		 * 4- Contract the arm to the end column
		 * 5- Drop the block
		 * 
		 * This is used in all parts (A..E) to move a block from one column to the other
		 */
		changeArmOneHeight(calculateHeight(fromColumn, toColumn)); // Step 1
		changeArmTwoWidth(fromColumn.getValue()); // Step 2
		pickBlock(fromColumn); // Step 3
		changeArmTwoWidth(toColumn.getValue()); // Step 4
		dropBlock(fromColumn, toColumn); // Step 5
	}

	/**
	 * Checks if is value exist.
	 *
	 * @param value
	 *            the value
	 * @param myArray
	 *            the my array
	 * @return true, if is value exist
	 */
	private boolean isValueExist(int value, Stack<Integer> myArray) {
		for (int i : myArray) {
			if (value == i)
				return true;
		}
		return false;
	}

	/**
	 * Move blocks required.
	 *
	 * @param required
	 *            the required
	 */
	private void moveBlocksRequired(int required[]) {
		for (int i : required) {
			if (isValueExist(i, sourceBlocks)) {
				while (sourceBlocks.peek() != i) {
					moveBlock(column.source, column.temporary);
				}
				moveBlock(column.source, column.target);
			} else {
				while (temporaryBlocks.peek() != i) {
					moveBlock(column.temporary, column.source);
				}
				moveBlock(column.temporary, column.target);
			}
		}
	}

	private int countOccurrenceOfLastBlock(column countColumn) {
		int counter = 0;
		int value = 0;
		Stack<Integer> tmpColumn;

		if (countColumn == column.source)
			value = lastBlockHeight(column.source);
		else if (countColumn == column.target)
			value = lastBlockHeight(column.target);
		else
			value = lastBlockHeight(column.temporary);

		for (int i = 0; i < originalblockHeights.length; i++) {

			counter = value == originalblockHeights[i] ? ++counter : counter;
		}
		return counter;
	}

	private column[] hanoiMoveDirection(column fromColumn, column toColumn, int fromValue, int toValue) {

		if (fromValue == toValue)
			return new column[] { fromColumn, toColumn };
		if (fromValue == 0) {
			return new column[] { toColumn, fromColumn };
		} else if (toValue == 0) {
			return new column[] { fromColumn, toColumn };
		}
		if (fromValue < toValue) {
			return new column[] { fromColumn, toColumn };
		} else if (fromValue > toValue) {
			return new column[] { toColumn, fromColumn };
		}
		return null;
	}

	private void moveBlocksOrdered() {
		do {
			column[] tmpColumns;

			int movesCounter = 0;
			// Source and Temporary
			tmpColumns = hanoiMoveDirection(column.source, column.temporary, lastBlockHeight(column.source),
					lastBlockHeight(column.temporary));
			movesCounter = countOccurrenceOfLastBlock(tmpColumns[0]);
			for (int x = 0; x < movesCounter; x++) {
				moveBlock(tmpColumns[0], tmpColumns[1]);
			}

			// Source and Target
			tmpColumns = hanoiMoveDirection(column.source, column.target, lastBlockHeight(column.source),
					lastBlockHeight(column.target));
			movesCounter = countOccurrenceOfLastBlock(tmpColumns[0]);
			for (int x = 0; x < movesCounter; x++) {
				moveBlock(tmpColumns[0], tmpColumns[1]);
			}

			// Temporary and target
			tmpColumns = hanoiMoveDirection(column.temporary, column.target, lastBlockHeight(column.temporary),
					lastBlockHeight(column.target));
			movesCounter = countOccurrenceOfLastBlock(tmpColumns[0]);
			for (int x = 0; x < movesCounter; x++) {
				moveBlock(tmpColumns[0], tmpColumns[1]);
			}
		} while (targetBlocks.size() != 4);
	}

	private void init(int barHeights[], int blockHeights[], int required[], boolean ordered) {
		this.targetBlocks = new Stack<>();
		this.sourceBlocks = new Stack<>();
		this.temporaryBlocks = new Stack<>();
		this.originalBarHeights = barHeights;
		this.originalblockHeights = new int[blockHeights.length];
		System.arraycopy(blockHeights, 0, this.originalblockHeights, 0, blockHeights.length);
		for (int x = 0; x < blockHeights.length; x++) {
			sourceBlocks.push(blockHeights[x]);
		}
	}

	/**
	 * Control.
	 *
	 * @param barHeights
	 *            the bar heights
	 * @param blockHeights
	 *            the block heights
	 * @param required
	 *            the required
	 * @param ordered
	 *            the ordered
	 */
	public void control(int barHeights[], int blockHeights[], int required[], boolean ordered) {

		/*
		 * Initializing used variables Please don't comment this line
		 */
		init(barHeights, blockHeights, required, ordered);

		/*
		 * Handling passed argument to determine the question. This handles
		 * questions A to E, so there is no need to comment any part.
		 */
		if (ordered) { // Part E
			moveBlocksOrdered();
		} else if (required[0] == 0) { // Part A,B and C
			for (int x = 0; x < barHeights.length; x++) {
				moveBlock(column.source, column.target);
			}
		} else if (required[0] != 0) { // Part D
			moveBlocksRequired(required);
		}

	}
}
