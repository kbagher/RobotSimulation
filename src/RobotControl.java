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

	/** Stack containing all the target column blocks.*/
	private Stack<Integer> targetBlocks;

	/** Stack containing all the source column blocks. */
	private Stack<Integer> sourceBlocks; 
	
	/** Stack containing all the temporary column blocks. */
	private Stack<Integer> temporaryBlocks; 
	
	/** Bars heights. */
	private int[] originalBarHeights;

	/**
	 * Instantiates a new robot control.
	 *
	 * @param r Robot object
	 */
	public RobotControl(Robot r) {
		this.r = r;
	}
	
	
	private void stressTest(){
		MoveBlock(column.source, column.target);
		MoveBlock(column.source, column.temporary);
		column[] columns = {column.source,column.target,column.temporary};
		while(true){
			int fromColumnRandom = ThreadLocalRandom.current().nextInt(0, 3);
			int toColumnRandom = ThreadLocalRandom.current().nextInt(0, 3);
			column c = columns[fromColumnRandom];
			if(c == column.source){
				if(sourceBlocks.size()==0)
					continue;	
			}
			else if(c == column.target){
				if(targetBlocks.size()==0)
					continue;	
			}
			else if(c == column.temporary){
				if(temporaryBlocks.size()==0)
					continue;	
			}
			while(toColumnRandom == fromColumnRandom)
				 toColumnRandom = ThreadLocalRandom.current().nextInt(0, 3);
			MoveBlock(c, columns[toColumnRandom]);
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

	/**
	 * Reset the robot arms to the default value.
	 */
	private void resetRobot() {
		changeArmTwoWidth(1);		// default arm width value = 1
		changeArmThreeDepth(0);		// default arm depth value = 0
		// The target block height will equal the blocks height, otherwise it will be 2
		changeArmOneHeight(getTargetBlocksHeight() == 0 ? 2 : (getTargetBlocksHeight() + 1));
	}

	// pick a block from the source or the target blocks column
	/**
	 * Hello all.
	 *
	 * @param columnType the column type
	 */
	private void pickBlock(column columnType) {
		int stepsToMoveArmThree = 0;
		if (columnType == column.source) {
			stepsToMoveArmThree = this.armOneCurrentHeight - getSourceBlocksHeight() - 1;
		}
		else if (columnType == column.temporary) {
			stepsToMoveArmThree = this.armOneCurrentHeight - getTemporaryBlocksHeight() - 1;
		}
		else if (columnType == column.target) {
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
//			printDebugVariables();
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getSourceBlocksHeight() - lastBlockHeight(fromColumn);
			sourceBlocks.push(lastBlockHeight(fromColumn));
		} else if (toColumn == column.target) {
			System.out.println("Dropping in target");
//			printDebugVariables();
			stepsToMoveArmThree = this.armOneCurrentHeight - 1 - getTargetBlocksHeight() - lastBlockHeight(fromColumn);
			targetBlocks.push(lastBlockHeight(fromColumn));
		} else {
			System.out.println("Dropping in temporary");
//			printDebugVariables();
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
	 * @param collumnType the collumn type
	 * @return the int
	 */
	private int removeLastBlockFromColumnList(column collumnType) {
		
		if (collumnType == column.source)
			return sourceBlocks.pop();
		else if (collumnType == column.temporary)
			return temporaryBlocks.pop();
		else if (collumnType == column.target)
			return targetBlocks.pop();
		return 0;
	}

	/**
	 * Change arm one height.
	 *
	 * @param newHeight the new height
	 * @return true, if successful
	 */
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

	/**
	 * Change arm two width.
	 *
	 * @param newWidth the new width
	 * @return true, if successful
	 */
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

	/**
	 * Change arm three depth.
	 *
	 * @param newDepth the new depth
	 * @return true, if successful
	 */
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

	/**
	 * Last block height.
	 *
	 * @param blockInColumn the block in column
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
	 * Gets the heighest bar.
	 *
	 * @return the heighest bar
	 */
	private int getHeighestBar() {
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

	/**
	 * Calculate height from temporary to target.
	 *
	 * @return the int
	 */
	private int calculateHeightFromTemporaryToTarget() {
		int maxHeight = MyMath.max(getTargetBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight(),getSourceBlocksHeight());
		if (getTemporaryBlocksHeight() - lastBlockHeight(column.temporary) >= maxHeight) {
			System.out.println("A");
			return getTemporaryBlocksHeight() + 1;
		}
		System.out.println("B");
		return MyMath.max(getHeighestBar(), getTemporaryBlocksHeight(), getTargetBlocksHeight(),getSourceBlocksHeight())
				+ lastBlockHeight(column.temporary) + 2;
	}

	/**
	 * Calculate height from source to target.
	 *
	 * @return the int
	 */
	private int calculateHeightFromSourceToTarget() {
		int maxHeight = MyMath.max(getTargetBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight());
		if (getSourceBlocksHeight() - lastBlockHeight(column.source) >= maxHeight) {
			return getSourceBlocksHeight() + 1;
		}
		return MyMath.max(getTargetBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight())
				+ lastBlockHeight(column.source) + 1;
	}
	
	private int calculateHeightFromTargetToSource() {
		int maxHeight = MyMath.max(getSourceBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight());
		if (getTargetBlocksHeight() - lastBlockHeight(column.target) >= maxHeight) {
			return getTargetBlocksHeight() + 1;
		}
		return MyMath.max(getSourceBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight())
				+ lastBlockHeight(column.target) + 1;
	}

	private int calculateHeightFromTargetToTemporary() {
		int maxHeight = MyMath.max(getTargetBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight(),getSourceBlocksHeight());
		if (getTargetBlocksHeight() - lastBlockHeight(column.target) >= maxHeight) {
			return getTargetBlocksHeight() + 1;
		}
		return MyMath.max(getHeighestBar(), getTemporaryBlocksHeight(), getTargetBlocksHeight(),getSourceBlocksHeight())
				+ lastBlockHeight(column.target) + 1;
	}

	
	
	/**
	 * Calculate height from source to temporary.
	 *
	 * @return the int
	 */
	private int calculateHeightFromSourceToTemporary() {
		int maxHeight = MyMath.max(getTemporaryBlocksHeight(), getTargetBlocksHeight(), getHeighestBar(),getSourceBlocksHeight());
		// if(getSourceBlocksHeight() - lastBlockHeight(column.source) >=
		// maxHeight){
		if (getSourceBlocksHeight() - lastBlockHeight(column.source) >= maxHeight) {
			System.out.println("A");
			return getSourceBlocksHeight() + 1;
		} else if (getTemporaryBlocksHeight() + lastBlockHeight(column.source) <= maxHeight) {
			System.out.println("B");
			return maxHeight + 1;
		}
		System.out.println("C");
		return getTemporaryBlocksHeight() + lastBlockHeight(column.source) + 1;
	}

	/**
	 * Calculate height from temporary to source.
	 *
	 * @return the int
	 */
	private int calculateHeightFromTemporaryToSource() {
		int maxHeight = MyMath.max(getHeighestBar(), getSourceBlocksHeight(), getTargetBlocksHeight(),getTemporaryBlocksHeight());
		if (getTemporaryBlocksHeight() - lastBlockHeight(column.temporary) >= maxHeight) {
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

	
	
	/**
	 * Calculate height.
	 *
	 * @param fromColumn the from column
	 * @param toColumn the to column
	 * @return the int
	 */
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
		} else if (fromColumn == column.target && toColumn == column.source) {
			System.out.println("calculateHeightFromTargetToSource");
			return calculateHeightFromTargetToSource();
		} else if (fromColumn == column.target && toColumn == column.temporary) {
			System.out.println("calculateHeightFromTargetToTemporary");
			return calculateHeightFromTargetToTemporary();
		}
		return 0;
	}

	/**
	 * Move block.
	 *
	 * @param fromColumn the from column
	 * @param toColumn the to column
	 */
	private void MoveBlock(column fromColumn, column toColumn) {
		changeArmOneHeight(calculateHeight(fromColumn, toColumn));
		changeArmTwoWidth(fromColumn.getValue());
		pickBlock(fromColumn);
		changeArmTwoWidth(toColumn.getValue());
		dropBlock(fromColumn, toColumn);
	}

	/**
	 * Checks if is value exist.
	 *
	 * @param value the value
	 * @param myArray the my array
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
	 * Sort array.
	 *
	 * @param sourceArray the source array
	 * @return the int[]
	 */
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

	/**
	 * Move blocks required.
	 *
	 * @param required the required
	 */
	private void moveBlocksRequired(int required[]) {
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
	
	private column getMaxValueColumn() {
		int source= sourceBlocks.size()==0?0:sourceBlocks.peek();
		int temporary = temporaryBlocks.size()==0?0:temporaryBlocks.peek();
		int target = targetBlocks.size()==0?0:targetBlocks.peek();
		int maxValue = MyMath.max(source, target, temporary);

		if (sourceBlocks.peek() == maxValue)
			return column.source;
		else if (targetBlocks.peek() == maxValue)
			return column.target;
		else
			return column.temporary;
	}
	
	private column getMidValueColumn(){
		int[] values = new int[3];
		values[0]=sourceBlocks.peek();
		values[1]=targetBlocks.peek();
		values[2]=temporaryBlocks.peek();
		values = sortArray(values);
		for (int x=0;x<3;x++) {
			if(sourceBlocks.peek() == values[1])
				return column.source;
			else if(targetBlocks.peek() == values[1])
				return column.target;
			else
				return column.temporary;
		}
		return null;
	}
	
	private column getMinValueColumn(){
		System.out.println(targetBlocks.size());
		int source= sourceBlocks.size()==0?0:sourceBlocks.peek();
		int temporary = temporaryBlocks.size()==0?0:temporaryBlocks.peek();
		int target = targetBlocks.size()==0?0:targetBlocks.peek();
		int minValue = MyMath.min(source, target, temporary);
		
		if(sourceBlocks.peek() ==minValue)
			return column.source;
		else if(targetBlocks.peek() == minValue)
			return column.target;
		else
			return column.temporary;
	}
	
	private int getMaximumBlockHeight(){
		int source= sourceBlocks.size()==0?0:sourceBlocks.peek();
		int temporary = temporaryBlocks.size()==0?0:temporaryBlocks.peek();
		int target = targetBlocks.size()==0?0:targetBlocks.peek();
		return MyMath.max(source, target, temporary);
	}
	private int getMinimumBlockHeight(){
		int source= sourceBlocks.size()==0?0:sourceBlocks.peek();
		int temporary = temporaryBlocks.size()==0?0:temporaryBlocks.peek();
		int target = targetBlocks.size()==0?0:targetBlocks.peek();
		return MyMath.min(source, target, temporary);
	}
	
	
	private void moveBlocksOrdered(){
		column lastMovedToColumn=null;
		MoveBlock(column.source, column.target);
		MoveBlock(column.source, column.temporary);
		while(targetBlocks.size()!=4){
			if(lastMovedToColumn==null){
				lastMovedToColumn =column.temporary;
				MoveBlock(getMinValueColumn(), getMidValueColumn());
			}
			else{
				if(getMinimumBlockHeight()==0){
					MoveBlock(getMaxValueColumn(), getMinValueColumn());
					lastMovedToColumn = getMinValueColumn();
				}
//				if(minValue ==0){
//					
//				}
			}
			System.out.println("MIN: "+getMinimumBlockHeight());
		}
	}
	

	/**
	 * Control.
	 *
	 * @param barHeights the bar heights
	 * @param blockHeights the block heights
	 * @param required the required
	 * @param ordered the ordered
	 */
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

		
//		MoveBlock(column.source, column.target);
//		MoveBlock(column.source, column.target);
//		MoveBlock(column.target, column.temporary);
//		MoveBlock(column.source, column.temporary);
//		MoveBlock(column.target, column.temporary);
//		resetRobot();
//		moveBlocksRequired(required);
		
		MoveBlock(column.source, column.temporary);
		MoveBlock(column.source, column.target);
		MoveBlock(column.target, column.source);
		MoveBlock(column.temporary, column.target);		
//		 stressTest();
		
		// Part A
//		for(int x=0;x<4;x++){
//			MoveBlock(column.source, column.target);
//		}
		
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

//		moveBlocksOrdered();
		
	}
}
