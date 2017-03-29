import java.nio.channels.ShutdownChannelGroupException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;

import javax.swing.JOptionPane;

class RobotControl {
	private Robot r;

	private final int maxArmOneHeight = 14; // maximum allowed height value for
											// arm 1
	private final int minArmOneHeight = 2; // minimum allowed height value for
											// arm 1
	private int armOneCurrentHeight = 2; // current height value for arm 1

	private final int maxArmTwoWidth = 10; // maximum allowed width value for
											// arm 2
	private final int minArmTwoWidth = 1; // minimum allowed width value for arm
											// 2
	private int armTwoCurrentWidth = 1; // current width value for arm 1

	private final int minArmThreeDepth = 0; // minimum allowed depth value for
											// arm 3
	private int armThreeCurrentDepth = 0; // current depth value for arm 3

	private Stack<Integer> targetBlocks; // a list for storing target blocks for
											// question D
	private Stack<Integer> sourceBlocks; // a list for storing source blocks for
											// question D
	private Stack<Integer> temporaryBlocks; // a list for storing temporary
											// blocks for question D

	private int[] originalBarHeights; // original bar height that was provided
										// by the user

	public enum column {
		source(10), temporary(9), target(1);
		private final int id;

		column(int id) {
			this.id = id;
		}

		public int getValue() {
			return id;
		}
	}

	public RobotControl(Robot r) {
		this.r = r;
	}

	private void printDebugVariables() {
		System.out.println("======= Debug ======");
		System.out.println("Arm One Current height:" + this.armOneCurrentHeight);
		System.out.println("Arm Two Current width:" + this.armTwoCurrentWidth);
		System.out.println("Arm Three Current Depth:" + this.armThreeCurrentDepth);
		System.out.println("-----");
		System.out.println("Source Blocks height:" + getSourceBlocksHeight());
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

	private void resetRobot() {
		changeArmTwoWidth(1);
		changeArmThreeDepth(0);
		changeArmOneHeight(getTargetBlocksHeight() == 0 ? 2 : (getTargetBlocksHeight() + 1));
	}

	// pick a block from the source or the target blocks column
	private void pickBlock(column columnType) {
		int stepsToMoveArmThree = 0;
		if (columnType == column.source) {
			stepsToMoveArmThree = this.armOneCurrentHeight - getSourceBlocksHeight() - 1;
		} else {
			stepsToMoveArmThree = this.armOneCurrentHeight - getTemporaryBlocksHeight() - 1;
		}
		changeArmThreeDepth(stepsToMoveArmThree);
		r.pick();
		changeArmThreeDepth(0);
	}

	private void dropBlock(column fromColumn, column toColumn) {
		int stepsToMoveArmThree = 0;
		if (toColumn == column.source) {
			System.out.println("Dropping in Source");
			printDebugVariables();
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getSourceBlocksHeight() - lastBlockHeight(fromColumn);
			sourceBlocks.push(lastBlockHeight(fromColumn));
		} else if (toColumn == column.target) {
			System.out.println("Dropping in target");
			printDebugVariables();
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getTargetBlocksHeight() - lastBlockHeight(fromColumn);
			targetBlocks.push(lastBlockHeight(fromColumn));
		} else {
			System.out.println("Dropping in temporary");
			printDebugVariables();
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getTemporaryBlocksHeight()
					- lastBlockHeight(fromColumn);
			temporaryBlocks.push(lastBlockHeight(fromColumn));
		}
		removeLastBlockFromColumnList(fromColumn);
		changeArmThreeDepth(stepsToMoveArmThree);
		r.drop();
		changeArmThreeDepth(0);
	}

	private int removeLastBlockFromColumnList(column collumnType) {
		if (collumnType == column.source)
			return sourceBlocks.pop();
		else if (collumnType == column.temporary)
			return temporaryBlocks.pop();
		else if (collumnType == column.target)
			return targetBlocks.pop();
		return 0;
	}

	// change Arm one height to the new height
	private boolean changeArmOneHeight(int newHeight) {
		/*
		 * check if the new height is bigger than the maximum allowed height or
		 * lower than the minimum allowed height
		 */
		if (newHeight > maxArmOneHeight || newHeight < minArmOneHeight) {
			System.out.println("New height error: " + newHeight);
			return false;
		}

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

	private boolean changeArmTwoWidth(int newWidth) {
		/*
		 * check if the new width is bigger than the maximum allowed width or
		 * lower than the minimum allowed width
		 */
		if (newWidth > maxArmTwoWidth || newWidth < minArmTwoWidth) {
			System.out.println("New width error: " + newWidth);
			return false;
		}

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

	private boolean changeArmThreeDepth(int newDepth) {
		/*
		 * check if the new depth is bigger than arm one height or lower than
		 * the minimum allowed depth
		 */
		if (newDepth >= this.armOneCurrentHeight || newDepth < minArmThreeDepth) {
			System.out.println("New depth error: " + newDepth);
			return false;
		}

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

	private int getHeighestBar() {
		int highestValue = 0;
		for (int x = 0; x < originalBarHeights.length; x++) {
			if (originalBarHeights[x] > highestValue)
				highestValue = originalBarHeights[x];
		}
		return highestValue;
	}

	private int getTemporaryBlocksHeight() {
		int totalBlockHeights = 0;
		for (int block : this.temporaryBlocks) {
			totalBlockHeights += block;
		}
		return totalBlockHeights;
	}

	private int getTargetBlocksHeight() {
		int totalBlockHeights = 0;
		for (int block : this.targetBlocks) {
			totalBlockHeights += block;
		}
		return totalBlockHeights;
	}

	private int getSourceBlocksHeight() {
		int sourceBlocksHeight = 0;
		for (int i : this.sourceBlocks) {
			sourceBlocksHeight += i;
		}
		return sourceBlocksHeight;
	}

	private int calculateHeightFromTemporaryToTarget() {
		int maxHeight = MyMath.max(getTargetBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight());
		if (getTemporaryBlocksHeight() - lastBlockHeight(column.temporary) >= maxHeight) {
			return getTemporaryBlocksHeight() + 1;
		}
		return MyMath.max(getHeighestBar(), getTemporaryBlocksHeight(), getTargetBlocksHeight())
				+ lastBlockHeight(column.temporary) + 1;
	}

	private int calculateHeightFromSourceToTarget() {
		int maxHeight = MyMath.max(getTargetBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight());
		if (getSourceBlocksHeight() - lastBlockHeight(column.source) >= maxHeight) {
			return getSourceBlocksHeight() + 1;
		}
		return MyMath.max(getTargetBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight())
				+ lastBlockHeight(column.source) + 1;
	}

	private int calculateHeightFromSourceToTemporary() {
		int maxHeight = MyMath.max(getTemporaryBlocksHeight(), getTargetBlocksHeight(), getHeighestBar());
		// if(getSourceBlocksHeight() - lastBlockHeight(column.source) >=
		// maxHeight){
		if (getSourceBlocksHeight() >= maxHeight) {
			System.out.println("A");
			return getSourceBlocksHeight() + 1;
		} else if (getTemporaryBlocksHeight() + lastBlockHeight(column.source) <= maxHeight) {
			System.out.println("B");
			return maxHeight + 1;
		}
		System.out.println("C");
		return getTemporaryBlocksHeight() + lastBlockHeight(column.source) + 1;
	}

	private int calculateHeightFromTemporaryToSource() {
		int maxHeight = MyMath.max(getHeighestBar(), getSourceBlocksHeight(), getTargetBlocksHeight());
		if (getTemporaryBlocksHeight() >= maxHeight) {
			System.out.println("A");
			return getTemporaryBlocksHeight() + 1;
		} else if (getSourceBlocksHeight() + lastBlockHeight(column.temporary) <= maxHeight) {
			System.out.println("B");
			return maxHeight + 1;
		}
		System.out.println("C");
		return (maxHeight - getTemporaryBlocksHeight()) + getTemporaryBlocksHeight() + lastBlockHeight(column.temporary)
				+ 1;
	}

	/*
	 * Calculate the height for Arm one based on the following Source blocks
	 * height Target Blocks height Highest bar available
	 */
	private int calculateHeight(column fromColumn, column toColumn) {
		if ((fromColumn == column.source && toColumn == column.target)) {
			System.out.println("calculateHeightFromSourceToTarget");
			return calculateHeightFromSourceToTarget();
		} else if (fromColumn == column.temporary && toColumn == column.target) {
			System.out.println("calculateHeightFromTemporaryToTarget");
			return calculateHeightFromTemporaryToTarget();
		} else if (fromColumn == column.source && toColumn == column.temporary) {
			System.out.println("calculateHeightFromSourceToTemporary");
			return calculateHeightFromSourceToTemporary();
		} else if (fromColumn == column.temporary && toColumn == column.source) {
			System.out.println("calculateHeightFromTemporaryToSource");
			return calculateHeightFromTemporaryToSource();
		}
		return 0;
	}

	private void MoveBlock(column fromColumn, column toColumn) {
		changeArmOneHeight(calculateHeight(fromColumn, toColumn));
		changeArmTwoWidth(fromColumn.getValue());
		pickBlock(fromColumn);
		changeArmTwoWidth(toColumn.getValue());
		dropBlock(fromColumn, toColumn);
	}

	private boolean isValueExist(int value, Stack<Integer> myArray) {
		boolean exist = false;
		for (int i : myArray) {
			if (value == i)
				;
			return true;
		}
		return exist;
	}

	private int[] sortArray(int[] sourceArray) {
		int temp = 0;
		for (int i = 0; i < sourceArray.length; i++) {
			for (int j = 1; j < (sourceArray.length - i); j++) {

				if (sourceArray[j - 1] < sourceArray[j]) {
					temp = sourceArray[j - 1];
					sourceArray[j - 1] = sourceArray[j];
					sourceArray[j] = temp;
				}

			}
		}
		return sourceArray;
	}

	private void moveBlocksRequired(int required[], boolean ordered) {
		if(ordered){
			required = new int[4];
			for (int i = 0; i < sourceBlocks.size(); i++) {
				required[i] = sourceBlocks.get(i);
			}
			required = sortArray(required);
		}
		
		for (int i : required) {
			if (isValueExist(i, sourceBlocks)) {
				while (sourceBlocks.peek() != i) {
					MoveBlock(column.source, column.temporary);
				}
				MoveBlock(column.source, column.target);
			} else {
				while (temporaryBlocks.peek() != i) {
					MoveBlock(column.temporary, column.source);
				}
				MoveBlock(column.temporary, column.target);
			}
		}
		resetRobot();
	}

	public void control(int barHeights[], int blockHeights[], int required[], boolean ordered) {

		/*
		 * The following lines are used to initialize the used variables PLEASE
		 * DON'T DELETE THEM
		 */
		this.targetBlocks = new Stack<>();
		this.sourceBlocks = new Stack<>();
		this.temporaryBlocks = new Stack<>();
		this.originalBarHeights = barHeights;
		for (int x = 0; x < blockHeights.length; x++) {
			sourceBlocks.push(blockHeights[x]);
		}
		/////// Done initializing variables ///////

		// this.MoveBlock(column.source, column.temporary);
		// this.MoveBlock(column.source, column.target);
		// this.MoveBlock(column.source, column.temporary);
		// this.MoveBlock(column.source, column.temporary);
		// //
		// this.MoveBlock(column.temporary, column.source);
		// this.MoveBlock(column.temporary, column.target);
		// this.MoveBlock(column.temporary, column.source);
		//
		// resetRobot();
		
		moveBlocksRequired(required, ordered);

		// Part A

		// Part B

		// Part C

		// Part D

		// The fourth part allows the user to specify the order in which bars
		// must
		// be placed in the target column. This will require you to use the use
		// additional column
		// which can hold temporary values

		// The last part requires you to write the code to move from source
		// column to target column using
		// an additional temporary column but without placing a larger block on
		// top of a smaller block

	}
}
