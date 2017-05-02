import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import com.sun.media.sound.MidiUtils.TempoCache;

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
    private final int ARM_TWO_HEIGHT = 1;
    
    /** Arm 1 width (not changed). */
    private final int ARM_ONE_WIDTH = 1;

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
     * 
     * @param showDebugVariables prints debug variables
     */
    @SuppressWarnings("unused")
    private void stressTest(Column source,Column temp, Column target,boolean showDebugVariables) {
	moveBlock(source, target);
	moveBlock(source, temp);
	Random rand = new Random();

	while (true) {
	    if (showDebugVariables) {
		System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		printDebugVariables(source,temp,target);
	    }
	    int fromColumnRandom = rand.nextInt(3) + 0;
	    int toColumnRandom = rand.nextInt(3) + 0;
	    Column c = Column.getColumns().get(fromColumnRandom);
	    
	    if (c.getHeight()==0) continue;
	    
	    while (toColumnRandom == fromColumnRandom){
		toColumnRandom = rand.nextInt(3) + 0;
	    }
	    if (showDebugVariables) {
		System.out.println("Moving From "+c + " To "+Column.getColumns().get(toColumnRandom));
	    }
	    moveBlock(c, Column.getColumns().get(toColumnRandom));
	    if (showDebugVariables) {
		printDebugVariables(source,temp,target);
		System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxx");
	    }
	}
    }

    /**
     * Prints the debug variables.
     */
    private void printDebugVariables(Column source,Column temp, Column target) {
	System.out.println("======= Debug ======");
	System.out.println("Arm One Current height:" + this.armOneCurrentHeight);
	System.out.println("Arm Two Current width:" + this.armTwoCurrentWidth);
	System.out.println("Arm Three Current Depth:" + this.armThreeCurrentDepth);
	System.out.println("-----");
	System.out.println("Source Blocks size:" + source.getBlocks().size());
	System.out.println("Source Blocks height:" + source.getHeight());
	System.out.println("-----");
	System.out.println("Temporary Blocks size:" + temp.getBlocks().size());
	System.out.println("Temporary Blocks height:" + temp.getHeight());
	System.out.println("-----");
	System.out.println("Target Blocks size:" + target.getBlocks().size());
	System.out.println("Target Blocks height:" + target.getHeight());
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
    private void pickBlock(Column fromColumn) {
	int stepsToMoveArmThree = 0;
	/* 
	 * calculate movement steps needed for arm 3 depending on the starting column
	 */
	stepsToMoveArmThree = this.armOneCurrentHeight - fromColumn.getHeight() - ARM_TWO_HEIGHT;
	
	// lower arm 3
	changeArmThreeDepth(stepsToMoveArmThree);
	
	// pick the block from the current column 
	r.pick();
	
	// reset arm 3 depth to it's default depth of 0
	changeArmThreeDepth(0);
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
    private void dropBlock(Column fromColumn, Column toColumn) {
	int stepsToMoveArmThree = 0;

	/*
	 * calculate movement steps needed for arm 3 depending on the ending
	 * column
	 */
	stepsToMoveArmThree = this.armOneCurrentHeight - toColumn.getHeight() - fromColumn.getTopBlockHeight()
		- ARM_TWO_HEIGHT;

	toColumn.addBlock(fromColumn.removeBlock());

	/*
	 * lower robot determine if robot should lower arm 1 or arm 3 this will
	 * reduce the number of unnecessary steps
	 */
	boolean lowerArmOne = (armOneCurrentHeight - stepsToMoveArmThree) <= armPass(Column.getColumnByType(ColumnType.target), toColumn)
		? false : true;
	if (lowerArmOne) {
	    // lower arm 1
	    changeArmOneHeight(armOneCurrentHeight - stepsToMoveArmThree);
	} else {
	    // lower arm 3
	    changeArmThreeDepth(stepsToMoveArmThree);
	}

	// drop block at the current column
	r.drop();

	// reset arm 3 depth to it's default depth of 0
	changeArmThreeDepth(0);
    }

    /**
     * <p>Change Arm 1 height to a given value. It works in both directions, Up and
     * Down.</p>
     *
     * @param newHeight the new height for the arm
     */
    private void changeArmOneHeight(int newHeight) {
	// arm height should remain as is
	if (this.armOneCurrentHeight == newHeight) {
	    return;
	}
	// move the arm up to reach the new given height
	else if (newHeight > armOneCurrentHeight) {
	    for (int x = this.armOneCurrentHeight; x < newHeight; x++) {
		r.up();
		// avoid hitting the column
		this.armOneCurrentHeight = x + ARM_TWO_HEIGHT;
	    }
	}
	// move the arm down to reach the new given height
	else if (newHeight < armOneCurrentHeight) {
	    for (int x = this.armOneCurrentHeight; x > newHeight; x--) {
		r.down();
		// avoid hitting arm 2
		this.armOneCurrentHeight = x - ARM_TWO_HEIGHT;
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

	// arm width should remain as is
	if (this.armTwoCurrentWidth == newWidth) {
	    return;
	}
	// move the arm forward to reach the new given width
	else if (newWidth > armTwoCurrentWidth) {
	    for (int oldArmTwoWidth = this.armTwoCurrentWidth; oldArmTwoWidth < newWidth; oldArmTwoWidth++) {
		r.extend();
		// to reach above the column
		this.armTwoCurrentWidth = oldArmTwoWidth + ARM_ONE_WIDTH;
	    }
	}
	// move the arm backward to reach the new given width
	else if (newWidth < armTwoCurrentWidth) {
	    for (int x = this.armTwoCurrentWidth; x > newWidth; x--) {
		r.contract();
		// to reach above the column
		this.armTwoCurrentWidth = x - ARM_ONE_WIDTH;
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

	// arm depth should remain as is
	if (this.armThreeCurrentDepth == newDepth) {
	    return;
	}
	// move the arm down to reach the new given depth
	else if (newDepth > armThreeCurrentDepth) {
	    for (int x = this.armThreeCurrentDepth; x < newDepth; x++) {
		r.lower();
		// avoid hitting the column
		this.armThreeCurrentDepth = x + ARM_TWO_HEIGHT;
	    }
	}
	// move the arm up to reach the new given depth
	else if (newDepth < armThreeCurrentDepth) {
	    for (int x = this.armThreeCurrentDepth; x > newDepth; x--) {
		r.raise();

		// avoid hitting arm 2
		this.armThreeCurrentDepth = x - ARM_TWO_HEIGHT;
	    }
	}
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
    private int blockPass(Column fromColumn, Column toColumn) {
	int blockHeight = fromColumn.getTopBlockHeight();
	int maxColumnHeightFound = 0;
	/*
	 * Always consider the smallest column index as the starting point to
	 * handle block movement in both direction (forward and backward)
	 */
	int fromColumnIndex = Math.min(fromColumn.getType().getValue(), toColumn.getType().getValue());

	// fathers column will be reached
	int toColumnIndex = Math.max(fromColumn.getType().getValue(), toColumn.getType().getValue());

	for (Column column : Column.getColumns()) {
	    boolean sameColumn = column.getType().getValue() == fromColumn.getType().getValue();
	    boolean inRange = column.getType().getValue() >= fromColumnIndex
		    && column.getType().getValue() <= toColumnIndex;
	    if (!sameColumn && inRange)
		maxColumnHeightFound = Math.max(maxColumnHeightFound, column.getHeight());
	}
	maxColumnHeightFound = Math.max(maxColumnHeightFound, getHighestBar());
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
    private int armPass(Column fromColumn, Column toColumn) {
	// track the highest column
	int maxColumnHeightFound = 0;
	
	// Determine the farthest column index for the given columns
	int maxColumnIndex = Math.max(fromColumn.getType().getValue(), toColumn.getType().getValue());
	
	// Start searching between column 1 and the farthest column the arm will reach
	for (Column column : Column.getColumns()) {
	    if (column.getType().getValue() <= maxColumnIndex)
		maxColumnHeightFound = Math.max(maxColumnHeightFound, column.getHeight());
	}
	// compare with bars height
	maxColumnHeightFound = Math.max(maxColumnHeightFound, getHighestBar());
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
    private int calculateHeight(Column fromColumn, Column toColumn) {
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
     * 1- Go up to a calculated height making sure the arm can pass without collision<br>
     * 2- Extend the arm to the starting column<br>
     * 3- Pick the block<br>
     * 4- Change arm 1 height making sure the block and arm will pass without collision<br>
     * 5- Contract the arm to the end column<br>
     * 6- Drop the block<br>

     * 
     * <p>This is used in all parts (A,B,C,D and E) to move a block between columns</p>
     * @param fromColumn
     *            the column which the block will be picked from
     * @param toColumn
     *            the column which the block will be dropped in
     */
    private void moveBlock(Column fromColumn, Column toColumn) {
	/*
	 * In some cases, the starting and ending columns falls behind the column
	 * in which the previous block was dropped in.
	 * If this column's height is higher than the starting and ending columns,
	 * arm 2 width should be changed before changing arm 1 height to avoid possible
	 * collision
	 */
	if (Math.max(fromColumn.getType().getValue(), toColumn.getType().getValue()) < this.armTwoCurrentWidth) {
	    changeArmTwoWidth(fromColumn.getType().getValue());
	}
	
	// go up making sure the arm pass
	changeArmOneHeight(armPass(fromColumn, toColumn)+1);
	
	// extend to reach the column
	changeArmTwoWidth(fromColumn.getType().getValue());
	
	// pick block from the given column
	pickBlock(fromColumn);
	
	// Change arm 1 height making sure the block and arm will pass obstacles
	changeArmOneHeight(calculateHeight(fromColumn, toColumn));
	
	// contract arm 2 making sure the block will pass any obstacles
	changeArmTwoWidth(toColumn.getType().getValue());
	
	// drop the current block at the given column
	dropBlock(fromColumn, toColumn);
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
    private boolean isBlockExist(int value, Column inColumn) {
	for (int currentValue : inColumn.getBlocks()) {
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
    private void moveBlocksRequired(Column source,Column temporary,Column target,int required[]) {
	for (int currentRequiredBlock : required) { // Step 1
	    if (isBlockExist(currentRequiredBlock, source)) { // Step 2
		while (source.getTopBlockHeight() != currentRequiredBlock) {
		    moveBlock(source, temporary);
		}
		moveBlock(source, target);
	    } else { // Step 3
		while (temporary.getTopBlockHeight()!= currentRequiredBlock) {
		    moveBlock(temporary, source);
		}
		moveBlock(temporary, target);
	    }
	}
    }

    /**
     * <p>Count the number of similar blocks in the given column</p>
     * 
     * <p>This will count how many similar (similar to the top block in the column)
     * sequenced blocks exist in the given column using</p>  
     * 
     * @param countColumn
     *            the column where the block exists in
     * @return the number of similar blocks
     */
    private int countSimilarBlocks(Column countColumn) {
	int counter = 0;
	int value = 0;

	// retrieve the top block from the column
	value = countColumn.getTopBlockHeight();
	
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
    private Column[] hanoiLegalMoveDirection(Column fromColumn, Column toColumn) {
	// similar block height
	if (fromColumn.getTopBlockHeight() == toColumn.getTopBlockHeight())
	    return new Column[] { fromColumn, toColumn };
	
	// starting column is empty
	if (fromColumn.getTopBlockHeight() == 0) {
	    return new Column[] { toColumn, fromColumn };
	}
	// ending column is empty
	else if (toColumn.getTopBlockHeight() == 0) {
	    return new Column[] { fromColumn, toColumn };
	}
	
	// starting column is smaller than ending 
	if (fromColumn.getTopBlockHeight() < toColumn.getTopBlockHeight()) {
	    return new Column[] { fromColumn, toColumn };
	}
	// starting column is bigger than ending
	else if (fromColumn.getTopBlockHeight() > toColumn.getTopBlockHeight()) {
	    return new Column[] { toColumn, fromColumn };
	}
	return null;
    }

    /**
     * <p>Move blocks to the target column in descending order with the
     * limitation that a larger block cannot be placed on top of a smaller block.</p>
     * 
     * <p>The algorithm followed is for the Tower Of Hanoi using
     * iterative for an <b>even</b> number of discs (blocks) with supporting duplicated blocks value.</p>
     * <p><b>Movement Steps</b><br>
     * 
     * 1- Make a legal move between Source and Temporary<br>
     * 2- Make a legal move between Source and Target<br>
     * 3- Make a legal move between Temporary and Target<br>
     * 4- Repeat until all blocks are moved to target</p>
     * 
     * 
     * <p><b>Notes</b><br>
     * 1- Each movement step is repeated if the consecutive blocks have similar value<br>
     * 2- Repeated blocks in each step will be moved together 
     * before going to the next iteration
     * 
     * <p><b>Movement Steps Reference:</b><br>
     * https://en.wikipedia.org/wiki/Tower_of_Hanoi#Iterative_solution</p>
     */
    private void moveBlocksOrdered(Column source,Column temporary,Column target) {
	
	// convert to set to remove duplicated values
	Set<Integer> uniqueBlocks = new HashSet<Integer>(source.getBlocks());
	
	// determine if the number of blocks are even or odd
	boolean isEven = uniqueBlocks.size() % 2 == 0 ? true : false;
	
	// will stop once all blocks are moved to the target column
	while (true) {
	    
	    // legal movement direction between columns
	    Column[] moveDirectionColumns;
	    // number of movements for duplicated consecutive blocks
	    int movesCounter = 0;
	    /*
	     *  The destination column will be determined based on the number of blocks,
	     *  Even or odd blocks. 
	     */
	    Column to;

	    // Even: Moving a block between Source and Temporary
	    // Odd: Moving a block between Source and Target
	    to = isEven ? temporary:target;
	    moveDirectionColumns = hanoiLegalMoveDirection(source, to);
  	    // determine the number of repeated blocks
	    movesCounter = countSimilarBlocks(moveDirectionColumns[0]);
	    // moving all similar blocks in this step
	    for (int x = 0; x < movesCounter; x++) {
		moveBlock(moveDirectionColumns[0], moveDirectionColumns[1]);
	    }
	    
	    // stop if all blocks are moved to destination 
	    if(target.getBlocks().size() == this.blockHeights.length)
		break;
	    
	    // Even : Moving a block between Source and Target
	    // Odd: Moving a block between Source and Temporary
	    to = isEven ? target:temporary;
	    moveDirectionColumns = hanoiLegalMoveDirection(source, to);
	    // determine the number of repeated blocks
	    movesCounter = countSimilarBlocks(moveDirectionColumns[0]);
  	    // moving all similar blocks in this step
	    for (int x = 0; x < movesCounter; x++) {
		moveBlock(moveDirectionColumns[0], moveDirectionColumns[1]);
	    }

	    // stop if all blocks are moved to destination
	    if(target.getBlocks().size() == this.blockHeights.length)
		break;
	    
	    
	    // Even and Odd: Moving a block between Temporary and target
	    moveDirectionColumns = hanoiLegalMoveDirection(temporary, target);
	    // determine the number of repeated blocks
	    movesCounter = countSimilarBlocks(moveDirectionColumns[0]);
	    // moving all similar blocks in this step
	    for (int x = 0; x < movesCounter; x++) {
		moveBlock(moveDirectionColumns[0], moveDirectionColumns[1]);
	    }
	    
	    // stop if all blocks are moved to destination
	    if(target.getBlocks().size() == this.blockHeights.length)
		break;
	}
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
//	this.targetBlocks = new Stack<>();
//	this.sourceBlocks = new Stack<>();
//	this.temporaryBlocks = new Stack<>();
	this.barHeights = barHeights;
	this.blockHeights = new int[blockHeights.length];
	// void copying array be reference (used to avoid possible referencing problems in
	// part E)
	System.arraycopy(blockHeights, 0, this.blockHeights, 0, blockHeights.length);
//	for (int x = 0; x < blockHeights.length; x++) {
//	    sourceBlocks.push(blockHeights[x]);
//	}
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
	 * Uncomment the following method call to perform stress test
	 * 
	 * Important:
	 * required and ordered parameters should not be passed in the program arguments
	 * 
	 * stressTest(true);
	 */
	
	
	Column source = new Column(ColumnType.source, blockHeights);
	Column temp = new Column(ColumnType.temporary, null);
	Column target = new Column(ColumnType.target, null);
	
	stressTest(source,temp,target,false);
	
	if (ordered) { // Part E
	    moveBlocksOrdered(source,temp,target);
	} else if (required[0] == 0) { // Part A,B and C
	    for (int x = 0; x < blockHeights.length; x++) {
		moveBlock(source, target);
	    }
	} else if (required[0] != 0) { // Part D
	    moveBlocksRequired(source,temp,target,required);
	}
    }
}