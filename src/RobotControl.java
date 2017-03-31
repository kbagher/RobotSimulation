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
	 * Hello all.
	 *
	 * @param columnType
	 *            the column type
	 */
	private void pickBlock(column columnType) {
		int stepsToMoveArmThree = 0;
		if (columnType == column.source) {
			stepsToMoveArmThree = this.armOneCurrentHeight - getSourceBlocksHeight() - 1;
		} else if (columnType == column.temporary) {
			stepsToMoveArmThree = this.armOneCurrentHeight - getTemporaryBlocksHeight() - 1;
		} else if (columnType == column.target) {
			stepsToMoveArmThree = this.armOneCurrentHeight - getTargetBlocksHeight() - 1;
		}
		changeArmThreeDepth(stepsToMoveArmThree);
		r.pick();
		changeArmThreeDepth(0);

	}

	/**
	 * Drop block.
	 *
	 * @param fromColumn the from column
	 * @param toColumn the to column
	 */
	private void dropBlock(column fromColumn, column toColumn) {
		int stepsToMoveArmThree = 0;
		if (toColumn == column.source) {
			System.out.println("Dropping in Source");
			// printDebugVariables();
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getSourceBlocksHeight() - lastBlockHeight(fromColumn);
			sourceBlocks.push(lastBlockHeight(fromColumn));
		} else if (toColumn == column.target) {
			System.out.println("Dropping in target");
			// printDebugVariables();
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getTargetBlocksHeight() - lastBlockHeight(fromColumn);
			targetBlocks.push(lastBlockHeight(fromColumn));
		} else {
			System.out.println("Dropping in temporary");
			// printDebugVariables();
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
	 * Change arm one height.
	 *
	 * @param newHeight
	 *            the new height
	 * @return true, if successful
	 */
	// change Arm one height to the new height
	private boolean changeArmOneHeight(int newHeight) {
//		/*
//		 * check if the new height is bigger than the maximum allowed height or
//		 * lower than the minimum allowed height
//		 */
//		if (newHeight > maxArmOneHeight || newHeight < minArmOneHeight) {
//			System.out.println("New height error: " + newHeight);
//			return false;
//		}

		// check if the new height is similar to the current height
		if (this.armOneCurrentHeight == newHeight) {
			return true;
		}

		// if the new height is higher than the current height, increase the
		// height
		else if (newHeight > armOneCurrentHeight) {
			for (int x = this.armOneCurrentHeight; x < newHeight; x++) {
				r.up();
				this.armOneCurrentHeight = x + 1;
			}
		}
		// if the new height is lower than the current height, decrease the
		// height
		else if (newHeight < armOneCurrentHeight) {
			for (int x = this.armOneCurrentHeight; x > newHeight; x--) {
				r.down();
				this.armOneCurrentHeight = x - 1;
			}
		}
		return true;
	}

	/**
	 * Change arm two width.
	 *
	 * @param newWidth
	 *            the new width
	 * @return true, if successful
	 */
	private boolean changeArmTwoWidth(int newWidth) {
//		/*
//		 * check if the new width is bigger than the maximum allowed width or
//		 * lower than the minimum allowed width
//		 */
//		if (newWidth > maxArmTwoWidth || newWidth < minArmTwoWidth) {
//			System.out.println("New width error: " + newWidth);
//			return false;
//		}

		// check if the new width is similar to the current width
		if (this.armTwoCurrentWidth == newWidth) {
			return true;
		}

		// if the new width is higher than the current width, increase the width
		else if (newWidth > armTwoCurrentWidth) {
			for (int x = this.armTwoCurrentWidth; x < newWidth; x++) {
				r.extend();
				this.armTwoCurrentWidth = x + 1;
			}
		}
		// if the new height is lower than the current width, decrease the width
		else if (newWidth < armTwoCurrentWidth) {
			for (int x = this.armTwoCurrentWidth; x > newWidth; x--) {
				r.contract();
				this.armTwoCurrentWidth = x - 1;
			}
		}
		return true;
	}

	/**
	 * Change arm three depth.
	 *
	 * @param newDepth
	 *            the new depth
	 * @return true, if successful
	 */
	private boolean changeArmThreeDepth(int newDepth) {

		// check if the new depth is similar to the current depth
		if (this.armThreeCurrentDepth == newDepth) {
			return true;
		}

		// if the new depth is higher than the current depth, increase the depth
		else if (newDepth > armThreeCurrentDepth) {
			for (int x = this.armThreeCurrentDepth; x < newDepth; x++) {
				r.lower();
				this.armThreeCurrentDepth = x + 1;
			}
		}
		// if the new depth is lower than the current depth, decrease the depth
		else if (newDepth < armThreeCurrentDepth) {
			for (int x = this.armThreeCurrentDepth; x > newDepth; x--) {
				r.raise();
				this.armThreeCurrentDepth = x - 1;
			}
		}
		return true;
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
	 * Move block.
	 *
	 * @param fromColumn
	 *            the from column
	 * @param toColumn
	 *            the to column
	 */
	private void moveBlock(column fromColumn, column toColumn) {
		changeArmOneHeight(calculateHeight(fromColumn, toColumn));
		changeArmTwoWidth(fromColumn.getValue());
		pickBlock(fromColumn);
		changeArmTwoWidth(toColumn.getValue());
		dropBlock(fromColumn, toColumn);
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
		int counter=0;
		int value=0;
		Stack<Integer> tmpColumn;
		
		if(countColumn==column.source)
			value = lastBlockHeight(column.source);
		else if(countColumn==column.target)
			value = lastBlockHeight(column.target);
		else
			value = lastBlockHeight(column.temporary);
			
		for (int i = 0; i < originalblockHeights.length; i++) {
			
			counter = value == originalblockHeights[i] ? ++counter: counter;
		}
		return counter;
	}

	private column[] hanoiMoveDirection(column fromColumn, column toColumn,int fromValue , int toValue) {
	
		if (fromValue == toValue)
			return new column[]{fromColumn,toColumn};
		if (fromValue == 0) {
			return new column[]{toColumn,fromColumn};
		} else if (toValue == 0) {
			return new column[]{fromColumn,toColumn};
		}
		if (fromValue < toValue) {
			return new column[]{fromColumn,toColumn};
		} else if (fromValue > toValue) {
			return new column[]{toColumn,fromColumn};
		}
		return null;
	}
	
	private void moveBlocksOrdered() {
		do{
			column[] tmpColumns;
			
			int movesCounter=0;
			// Source and Temporary
			tmpColumns = hanoiMoveDirection(column.source, column.temporary, lastBlockHeight(column.source),
					lastBlockHeight(column.temporary));
			movesCounter = countOccurrenceOfLastBlock(tmpColumns[0]);
			for(int x=0;x<movesCounter;x++){
				moveBlock(tmpColumns[0], tmpColumns[1]);	
			}
			
			// Source and Target
			tmpColumns = hanoiMoveDirection(column.source, column.target, lastBlockHeight(column.source),
					lastBlockHeight(column.target));
			movesCounter = countOccurrenceOfLastBlock(tmpColumns[0]);
			for(int x=0;x<movesCounter;x++){
				moveBlock(tmpColumns[0], tmpColumns[1]);	
			}

			// Temporary and target 
			tmpColumns = hanoiMoveDirection(column.temporary, column.target, lastBlockHeight(column.temporary),
					lastBlockHeight(column.target));
			movesCounter = countOccurrenceOfLastBlock(tmpColumns[0]);
			for(int x=0;x<movesCounter;x++){
				moveBlock(tmpColumns[0], tmpColumns[1]);	
			}
		}while (targetBlocks.size() != 4);
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
		 * The following lines are used to initialize the used variables
		 * PLEASE DON'T REMOVE THEM
		 */
		this.targetBlocks = new Stack<>();
		this.sourceBlocks = new Stack<>();
		this.temporaryBlocks = new Stack<>();
		this.originalBarHeights = barHeights;
		this.originalblockHeights = new int[blockHeights.length];
		System.arraycopy(blockHeights, 0, this.originalblockHeights, 0, blockHeights.length);
		for (int x = 0; x < blockHeights.length; x++) {
			sourceBlocks.push(blockHeights[x]);
		}
		/////// Done initializing variables ///////

		// stressTest();


//		moveBlock(column.source, column.temporary);
//		moveBlock(column.source, column.target);
//		moveBlock(column.temporary, column.target);
//		moveBlock(column.source, column.temporary);
//		moveBlock(column.target, column.source);
//		moveBlock(column.target, column.temporary);
//		moveBlock(column.source, column.temporary);
//		moveBlock(column.source, column.target);
//		moveBlock(column.temporary, column.target);
//		moveBlock(column.temporary, column.source);
//		moveBlock(column.target, column.source);
//		moveBlock(column.temporary, column.target);
//		moveBlock(column.source, column.temporary);
//		moveBlock(column.source, column.target);
//		moveBlock(column.temporary, column.target);

		
		if (ordered) {
			moveBlocksOrdered();
		}
		// Part A,B and C
		else if (required[0] == 0) {
			for (int x = 0; x < barHeights.length; x++) {
				moveBlock(column.source, column.target);
			}
		}
		// Part D
		else if (required[0] != 0) {
			moveBlocksRequired(required);
		}

	}
}
