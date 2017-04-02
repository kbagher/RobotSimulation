import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

// TODO: Auto-generated Javadoc
/**
 * The Class RobotControl.
 */
public class RobotControl {

    /** The Robot object. */
    private Robot r;

    /** The current height value for arm 1. */
    private int armOneCurrentHeight = 2;

    /** The current width value for arm 2. */
    private int armTwoCurrentWidth = 1;

    /** The current depth value for arm 3. */
    private int armThreeCurrentDepth = 0;
    
    /** Arm 2 height (not changed). */
    private final int armTwoHeight = 1;
    
    /** Arm 1 width (not changed). */
    private final int armOneWidth = 1;


    /** A stack contains all the target column blocks. */
    private Stack<Integer> targetBlocks;

    /** A stack contains all the source column blocks. */
    private Stack<Integer> sourceBlocks;

    /** A stack contains all the temporary column blocks. */
    private Stack<Integer> temporaryBlocks;

    /** Original values of Bars heights. */
    private int[] barHeights;

    /** Original values of Block heights. */
    private int[] blockHeights;

    /**
     * Instantiates a new robot control.
     *
     * @param r
     *            Robot object
     */
    public RobotControl(Robot r) {
	this.r = r;
    }

    /**
     * <p>Non-stopping stress test for moving blocks between Source, Temporary and Target</p>
     * 
     * <b>Does not work if required or ordered arguments are passed</b> 
     */
    private void stressTest() {
	moveBlock(column.source, column.target);
	moveBlock(column.source, column.temporary);
	Random rand = new Random();

	column[] columns = { column.source, column.target, column.temporary };
	while (true) {
	    System.out.println("========================");
	    printDebugVariables();
	    int fromColumnRandom = rand.nextInt(3) + 0;
	    int toColumnRandom = rand.nextInt(3) + 0;
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
		toColumnRandom = rand.nextInt(3) + 0;
	    moveBlock(c, columns[toColumnRandom]);
	    printDebugVariables();
	    System.out.println("========================");
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
	System.out.println("Source Blocks height:" + getSourceColumnHeight());
	System.out.println("-----");
	System.out.println("Temporary Blocks size:" + temporaryBlocks.size());
	System.out.println("Temporary Blocks height:" + getTemporaryColumnHeight());
	System.out.println("-----");
	System.out.println("Target Blocks size:" + targetBlocks.size());
	System.out.println("Target Blocks height:" + getTargetColumnHeight());
	System.out.println("====================");
    }

    /**
     * <p>Picks the top block from the given column.</p>
     * 
     * This process is achieved by the following steps:<br>
     * 1- Calculate arm 3 depth needed to pick the top block from the given column<br>
     * 2- Lowers arm 3 to the calculated depth<br>
     * 3- Pick the block<br>
     * 4- Rise arm 3 to depth = 0<br>
     * 
     * <p>Calculated arm 3 depth = Current arm 1 height - Block's Column height - Arm 2 height</p> 
     * 
     * @param fromColumn
     *            the column which contains the block
     */
    private void pickBlock(column fromColumn) {
	int stepsToMoveArmThree = 0;
	if (fromColumn == column.source) { // Step 1
	    stepsToMoveArmThree = this.armOneCurrentHeight - getSourceColumnHeight() - armTwoHeight;
	} else if (fromColumn == column.temporary) {
	    stepsToMoveArmThree = this.armOneCurrentHeight - getTemporaryColumnHeight() - armTwoHeight;
	} else if (fromColumn == column.target) {
	    stepsToMoveArmThree = this.armOneCurrentHeight - getTargetColumnHeight() - armTwoHeight;
	}
	changeArmThreeDepth(stepsToMoveArmThree); // Step 2
	r.pick(); // Step 3
	changeArmThreeDepth(0); // Step 4
    }

    /**
     * <p>Drop the current picked block at the given end column.</p>
     * 
     * <p>The process can be explained in the following 5 steps:<br>
     * 1- Push the block into the end column stack<br>
     * 2- Pop the block from the starting column stack<br>
     * 3- Lower Arm 3 to a calculated depth based on the end column<br>
     * 4- Drop the block<br>
     * 5- Rise arm 3 to default position (depth =0)</p>
     * 
     * Arm 3 depth = Current depth - Column height - block height - Arm 2 height.
     *
     * @param fromColumn
     *            the column which the block was picked from
     * @param toColumn
     *            the column which the block will be dropped in
     */
    private void dropBlock(column fromColumn, column toColumn) {
	int stepsToMoveArmThree = 0;
	// Step 1
	if (toColumn == column.source) { 
	    stepsToMoveArmThree = this.armOneCurrentHeight - getSourceColumnHeight() - getTopBlockHeight(fromColumn)
		    - armTwoHeight;
	    sourceBlocks.push(getTopBlockHeight(fromColumn));
	} else if (toColumn == column.target) {
	    stepsToMoveArmThree = this.armOneCurrentHeight - getTargetColumnHeight() - getTopBlockHeight(fromColumn)
		    - armTwoHeight;
	    targetBlocks.push(getTopBlockHeight(fromColumn));
	} else {
	    stepsToMoveArmThree = this.armOneCurrentHeight - getTemporaryColumnHeight()
		    - getTopBlockHeight(fromColumn) - armTwoHeight;
	    temporaryBlocks.push(getTopBlockHeight(fromColumn));
	}
	removeTopBlockFromColumn(fromColumn); // Step 2
	changeArmThreeDepth(stepsToMoveArmThree); // Step 3
	r.drop(); // Step 4
	changeArmThreeDepth(0); // Step 5
    }

    /**
     * Removes the top block from a given column.
     *
     * @param fromColumn
     *            the column in which the block exists
     * @return The height of the block which has being removed
     */
    private int removeTopBlockFromColumn(column fromColumn) {
	if (fromColumn == column.source)
	    return sourceBlocks.pop();
	else if (fromColumn == column.temporary)
	    return temporaryBlocks.pop();
	else if (fromColumn == column.target)
	    return targetBlocks.pop();
	return 0;
    }

    /**
     * <p>Change Arm 1 height to a given value. It works in both directions, Up and
     * Down.</p>
     *
     * @param newHeight the new height for the arm
     */
    private void changeArmOneHeight(int newHeight) {
	if (this.armOneCurrentHeight == newHeight) { // arm height should remain as is
	    return;
	} else if (newHeight > armOneCurrentHeight) { // move the arm up to reach the new given height
	    for (int x = this.armOneCurrentHeight; x < newHeight; x++) {
		r.up();
		this.armOneCurrentHeight = x + armTwoHeight; // avoid hitting the column
	    }
	} else if (newHeight < armOneCurrentHeight) { // move the arm down to reach the new given height
	    for (int x = this.armOneCurrentHeight; x > newHeight; x--) {
		r.down();
		this.armOneCurrentHeight = x - armTwoHeight; // avoid hitting arm 2
	    }
	}
    }

    /**
     * Change arm 2 width to a given value. It works in both directions, extend
     * and contract (forward and backward).
     * 
     * @param newWidth
     *            the new width for the arm
     */
    private void changeArmTwoWidth(int newWidth) {

	if (this.armTwoCurrentWidth == newWidth) { // arm width should remain as is
	    return;
	} else if (newWidth > armTwoCurrentWidth) { // move the arm forward to reach the new given width
	    for (int x = this.armTwoCurrentWidth; x < newWidth; x++) {
		r.extend();
		this.armTwoCurrentWidth = x + armOneWidth; // to reach above the column
	    }
	} else if (newWidth < armTwoCurrentWidth) { // move the arm backward to reach the new given width
	    for (int x = this.armTwoCurrentWidth; x > newWidth; x--) {
		r.contract();
		this.armTwoCurrentWidth = x - armOneWidth; // to reach above the column 
	    }
	}
    }

    /**
     * Change Arm 3 depth to a given value. It works in both directions, lower
     * and rise (up and down).
     * 
     * @param newDepth
     *            the new depth for the arm
     */
    private void changeArmThreeDepth(int newDepth) {

	if (this.armThreeCurrentDepth == newDepth) { // arm depth should remain as is
	    return;
	} else if (newDepth > armThreeCurrentDepth) { // move the arm down to reach the new given depth
	    for (int x = this.armThreeCurrentDepth; x < newDepth; x++) {
		r.lower();
		this.armThreeCurrentDepth = x + armTwoHeight; // avoid hitting the column
	    }
	} else if (newDepth < armThreeCurrentDepth) { // move the arm up to reach the new given depth
	    for (int x = this.armThreeCurrentDepth; x > newDepth; x--) {
		r.raise();
		this.armThreeCurrentDepth = x - armTwoHeight; // avoid hitting arm 2
	    }
	}
    }

    /**
     * <p>Return the top block's height for a given column.</p>
     * 
     * <p>This will return the height of the block without removing it from the column</p> 
     *
     * @param inColumn the column which contains the block
     * @return The top block height. Will return 0 if the column is empty
     */
    private int getTopBlockHeight(column inColumn) {
	if (inColumn == column.source)
	    return sourceBlocks.size() == 0 ? 0 : this.sourceBlocks.peek();
	else if (inColumn == column.target)
	    return targetBlocks.size() == 0 ? 0 : this.targetBlocks.peek();
	else if (inColumn == column.temporary)
	    return temporaryBlocks.size() == 0 ? 0 : this.temporaryBlocks.peek();

	System.out.println("Error in blockHeight");
	return 0;
    }

    /**
     * Return the heights bar height
     *
     * @return highest bar height
     */
    private int getHighestBar() {
	int highestValue = 0;
	for (int x = 0; x < barHeights.length; x++) {
	    if (barHeights[x] > highestValue)
		highestValue = barHeights[x];
	}
	return highestValue;
    }

    /**
     * <p>Gets the current temporary column height.</p>
     * Calculated by summing up all blocks height in the temporary column.
     *
     * @return the current temporary column height
     */
    private int getTemporaryColumnHeight() {
	int totalBlockHeights = 0;
	for (int block : this.temporaryBlocks) {
	    totalBlockHeights += block;
	}
	return totalBlockHeights;
    }

    /**
     * <p>Gets the current target column height.</p>
     * Calculated by summing up all blocks height in the target column.
     *
     * @return the current target column height
     */
    private int getTargetColumnHeight() {
	int totalBlockHeights = 0;
	for (int block : this.targetBlocks) {
	    totalBlockHeights += block;
	}
	return totalBlockHeights;
    }

    /**
     * <p>Gets the current source column height.</p>
     * Calculated by summing up all blocks height in the source column.
     *
     * @return the current source column height
     */
    private int getSourceColumnHeight() {
	int sourceBlocksHeight = 0;
	for (int i : this.sourceBlocks) {
	    sourceBlocksHeight += i;
	}
	return sourceBlocksHeight;
    }

    /**
     * <p>Calculate the minimum required height for arm 1 to able to move the block
     * between starting and ending column without hitting any obstacle.</p>
     * 
     * Block Pass Value = Block height + Maximum column height between the starting and ending column
     * excluding starting column<br>
     * 
     * <p>This might include all column in between (excluding starting column), depending on the starting and
     * ending column, such as; Target, Source, Temporary column and all the 6
     * bars.
     * </p>

     * @param fromColumn
     *            the starting column which the block will be picked from
     * @param toColumn
     *            the ending column which the block will be dropped in
     * @return minimum Arm 1 height
     */
    private int blockPass(column fromColumn, column toColumn) {
	int blockHeight = getTopBlockHeight(fromColumn);
	int maxColumnHeightFound = 0;
	/*
	 *  Always consider the smallest column index as the starting point
	 *  to handle block movement in both direction (forward and backward)  
	 */
	int currentColumnIndex = Math.min(fromColumn.getValue(), toColumn.getValue());
	
	int toColumnIndex = Math.max(fromColumn.getValue(), toColumn.getValue());
	
	for (; currentColumnIndex <= toColumnIndex; currentColumnIndex++) {
	    // exclude the starting column
	    if (currentColumnIndex == fromColumn.getValue())
		continue;
	    
	    if (currentColumnIndex == column.source.getValue()) {
		maxColumnHeightFound = Math.max(maxColumnHeightFound, getSourceColumnHeight());
	    } else if (currentColumnIndex == column.target.getValue()) {
		maxColumnHeightFound = Math.max(maxColumnHeightFound, getTargetColumnHeight());
	    } else if (currentColumnIndex == column.temporary.getValue()) {
		maxColumnHeightFound = Math.max(maxColumnHeightFound, getTemporaryColumnHeight());
	    } else { // For bars
		maxColumnHeightFound = Math.max(maxColumnHeightFound, getHighestBar());
	    }
	}
	return blockHeight + maxColumnHeightFound;
    }

    /**
     * <p>Calculate the minimum required height for arm 2 to be able to reach
     * and move between starting and ending column without hitting any obstacle.</p>
     * 
     * <p>Arm Pass Value = highest column between arm 1 and the farthest 
     * column which will be reached.<br>
     * This might include all column in between, depending on the starting and
     * ending column, such as; Target, Source, Temporary column and all the 6
     * bars.
     * </p>
     * 
     * @param fromColumn
     *            the starting column which the block will be picked from
     * @param toColumn
     *            the ending column which the block will be dropped in
     * @return arm 2 width
     */
    private int armPass(column fromColumn, column toColumn) {
	int maxColumnHeightFound = 0;
	// Determine the farthest column index for the given columns
	int maxColumnIndex = Math.max(fromColumn.getValue(), toColumn.getValue());
	// Starting from column 1 as column 0 is for the robot Arm 1
	for (int currentColumnIndex = 1; currentColumnIndex <= maxColumnIndex; currentColumnIndex++) {
	    if (currentColumnIndex == column.source.getValue()) {
		maxColumnHeightFound = Math.max(maxColumnHeightFound, getSourceColumnHeight());
	    } else if (currentColumnIndex == column.target.getValue()) {
		maxColumnHeightFound = Math.max(maxColumnHeightFound, getTargetColumnHeight());
	    } else if (currentColumnIndex == column.temporary.getValue()) {
		maxColumnHeightFound = Math.max(maxColumnHeightFound, getTemporaryColumnHeight());
	    } else { // For bars
		maxColumnHeightFound = Math.max(maxColumnHeightFound, getHighestBar());
	    }
	}
	return maxColumnHeightFound;
    }

    /**
     * <p>Calculate the absolute height required for Arm 1 to move the top block
     * from the starting column to the end column without hitting any obstacle.</p>
     * 
     * <p>The height needs to satisfy the following two rules:<br>
     * 1- Arm 2 should travel between the Starting and ending column without hitting any obstacle.<br>
     * 2- The picked block should travel from the starting column to the ending column without hitting any obstacle.</p>
     *
     * @param fromColumn
     *            The starting column which the block will be picked from
     * @param toColumn
     *            The ending column which the block will be dropped in
     * @return The calculated height for Arm 1
     */
    private int calculateHeight(column fromColumn, column toColumn) {
	/*
	 * maximum value to cover both situation:
	 * 1- Bars and moving block are higher than columns
	 * 2- Columns are higher than bars and moving block
	 */
	return Math.max(armPass(fromColumn, toColumn), blockPass(fromColumn, toColumn)) + 1;
    }

    /**
     * <p>Moves the top block from starting to ending column. works in both
     * directions (forward and backward).</p>
     * 
     * The main steps for the robot to move a block between columns are:<br>
     * 1- Go up to a calculated height<br>
     * 2- Extend the arm to the starting column<br>
     * 3- Pick the block<br>
     * 4- Contract the arm to the end column<br>
     * 5- Drop the block<br>
     * 
     * <p>This is used in all parts (A..E) to move a block between columns</p>
     * @param fromColumn
     *            the column which the block will be picked from
     * @param toColumn
     *            the column which the block will be dropped in
     */
    private void moveBlock(column fromColumn, column toColumn) {
	changeArmOneHeight(calculateHeight(fromColumn, toColumn)); // Step 1
	changeArmTwoWidth(fromColumn.getValue()); // Step 2
	pickBlock(fromColumn); // Step 3
	changeArmTwoWidth(toColumn.getValue()); // Step 4
	dropBlock(fromColumn, toColumn); // Step 5
    }

    /**
     * Checks if the given block height exists in the given Column.
     *
     * @param value
     *            the value to check if it exists or not
     * @param inColumn
     *            the Column Stack to check the value in it
     * @return true, if the value exist in the given column
     */
    private boolean isBlockExist(int value, Stack<Integer> inColumn) {
	for (int currentValue : inColumn) {
	    if (value == currentValue)
		return true;
	}
	return false;
    }

    /**
     * <p>Move blocks to target column in a specific order.</p>
     * 
     * This is a achieved using the following steps:<br>
     * 1- Iterate through the required block heights<br>
     * 2- If the current required block exists in the Source column,
     * move all the blocks above it to the Temporary column then move
     * the required block to the target column<br>
     * 3- If the current required block exists in the Temporary column,
     * move all the blocks above it to the Source column then move
     * the required block to the target column<br>
     * 4- Repeat until all blocks are moved to target
     *
     * @param required
     *            the block heights as required to be ordered
     */
    private void moveBlocksRequired(int required[]) {
	for (int currentRequiredBlock : required) { // Step 1
	    if (isBlockExist(currentRequiredBlock, sourceBlocks)) { // Step 2
		while (sourceBlocks.peek() != currentRequiredBlock) {
		    moveBlock(column.source, column.temporary);
		}
		moveBlock(column.source, column.target);
	    } else { // Step 3
		while (temporaryBlocks.peek() != currentRequiredBlock) {
		    moveBlock(column.temporary, column.source);
		}
		moveBlock(column.temporary, column.target);
	    }
	}
    }

    /**
     * <p>Count the number of similar blocks in the given column</p>
     * 
     * <p>This will count how many similar sequenced blocks
     * (similar to the top block in the column) exist in the given column using</p>  
     * 
     * @param countColumn
     *            the column where the block exists in
     * @return the number of similar blocks
     */
    private int countSimilarBlocks(column countColumn) {
	int counter = 0;
	int value = 0;

	// retrieve the top block from the column
	if (countColumn == column.source)
	    value = getTopBlockHeight(column.source);
	else if (countColumn == column.target)
	    value = getTopBlockHeight(column.target);
	else
	    value = getTopBlockHeight(column.temporary);
	
	// count the block occurrence
	for (int i = 0; i < blockHeights.length; i++) {
	    counter = value == blockHeights[i] ? ++counter : counter;
	}
	return counter;
    }

    /**
     * <p>Return the legal move direction between two columns for the Tower Of Hanoi</p>
     * 
     * <p><b>Movement Direction</b><br>
     * The direction of block movements between columns
     * is decided by satisfying the following rules:<br>
     * 1- Smaller number placed above bigger numbers<br>
     * 2- Similar numbers will use any direction<br>
     * 3- Always move the block to the empty column<br>
     *
     * @param fromColumn
     *            the starting column
     * @param toColumn
     *            the ending column
     * @param fromValue
     *            the height of top block of the starting column
     * @param toValue
     *            the height of top block of the ending column
     * @return the legal moving direction between the columns
     */
    private column[] hanoiLegalMoveDirection(column fromColumn, column toColumn, int fromValue, int toValue) {

	/*
	 * empty columns or similar blocks height
	 */
	if (fromValue == toValue) // similar block height
	    return new column[] { fromColumn, toColumn };
	if (fromValue == 0) { // starting column is empty
	    return new column[] { toColumn, fromColumn };
	} else if (toValue == 0) { // ending column is empty
	    return new column[] { fromColumn, toColumn };
	}
	
	/*
	 *  determining which block height is bigger 
	 */
	if (fromValue < toValue) {
	    return new column[] { fromColumn, toColumn };
	} else if (fromValue > toValue) {
	    return new column[] { toColumn, fromColumn };
	}
	return null;
    }

    /**
     * <p>Move blocks to the target column in descending order with the
     * limitation that a larger block cannot be placed on top of a smaller block.</p>
     * 
     * <p>The algorithm followed is for the Tower Of Hanoi using
     * iterative for an even number of discs (blocks) with supporting duplicated blocks value.</p>
     * <p><b>Movement Steps</b><br>
     * 
     * 1- Make a legal move between Source and Temporary<br>
     * 2- Make a legal move between Source and Target<br>
     * 3- Make a legal move between Temporary and Target<br>
     * 4- Repeat until all blocks are moved to target</p>
     * 
     * 
     * <p><b>Notes</b><br>
     * 1- Each movement step is repeated if the blocks have similar value<br>
     * 2- Repeated blocks in each step will be moved together 
     * before going to the next iteration
     * 
     * <p><b>Movement Steps Reference:</b><br>
     * https://en.wikipedia.org/wiki/Tower_of_Hanoi#Iterative_solution</p>
     */
    private void moveBlocksOrdered() {
	do {
	    column[] moveDirectionColumns; // calculated movement direction
	    int movesCounter = 0;
	    
	    // Moving a block between Source and Temporary
	    moveDirectionColumns = hanoiLegalMoveDirection(column.source, column.temporary, getTopBlockHeight(column.source),
		    getTopBlockHeight(column.temporary));
	    movesCounter = countSimilarBlocks(moveDirectionColumns[0]); // number of repeated blocks
	    for (int x = 0; x < movesCounter; x++) { // moving all similar blocks in this step
		moveBlock(moveDirectionColumns[0], moveDirectionColumns[1]);
	    }

	    // Moving a block between Source and Target
	    moveDirectionColumns = hanoiLegalMoveDirection(column.source, column.target, getTopBlockHeight(column.source),
		    getTopBlockHeight(column.target));
	    movesCounter = countSimilarBlocks(moveDirectionColumns[0]); // number of repeated blocks
	    for (int x = 0; x < movesCounter; x++) { // moving all similar blocks in this step
		moveBlock(moveDirectionColumns[0], moveDirectionColumns[1]);
	    }

	    // Moving a block between Temporary and target
	    moveDirectionColumns = hanoiLegalMoveDirection(column.temporary, column.target, getTopBlockHeight(column.temporary),
		    getTopBlockHeight(column.target));
	    movesCounter = countSimilarBlocks(moveDirectionColumns[0]); // number of repeated blocks
	    for (int x = 0; x < movesCounter; x++) { // moving all similar blocks in this step
		moveBlock(moveDirectionColumns[0], moveDirectionColumns[1]);
	    }
	} while (targetBlocks.size() != 4);
    }

    /**
     * Initialize class variables.
     *
     * @param barHeights
     *            the bar heights argument passed in the program arguments
     * @param blockHeights
     *            the block heights argument passed in the program arguments
     * @param required
     *            the required argument ordering passed in the program arguments
     * @param ordered
     *            the ordered argument passed in the program arguments
     */
    private void init(int barHeights[], int blockHeights[], int required[], boolean ordered) {
	this.targetBlocks = new Stack<>();
	this.sourceBlocks = new Stack<>();
	this.temporaryBlocks = new Stack<>();
	this.barHeights = barHeights;
	this.blockHeights = new int[blockHeights.length];
	// void copying array be reference (used to avoid possible problems in
	// part E)
	System.arraycopy(blockHeights, 0, this.blockHeights, 0, blockHeights.length);
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
	 * Uncomment the following method to perform stress test
	 * 
	 * Important:
	 * required and ordered parameters should not be passed in the program arguments
	 */
//	stressTest();
	
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