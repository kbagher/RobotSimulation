import java.nio.channels.ShutdownChannelGroupException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;

class RobotControl
 {
   private Robot r;

   private  final int maxArmTwoWidth = 10;
   private  final int minArmTwoWidth = 1;
   private  final int maxArmOneHeight = 14;
   private  final int minArmOneHeight = 2;
   private  final int minArmThreeDepth = 0;   
   private  final int minBlockHeight = 1;
   private  final int maxBlockHeight = 4;
   private  final int maxBarHeight = 8;
   private  final int minBarHeight = 1;
   private  final int armTwoHeight = 1;

   private int armOneCurrentHeight = 2;
   private int armTwoCurrentWidth = 1;
   private int armThreeCurrentDepth = 0;
   private ArrayList<Integer> targetBlocks;
   private ArrayList<Integer> sourceBlocks;
   private ArrayList<Integer> temporaryBlocks;
   private int[] barHeights;
   private int[] originalBlockHeights;

   public enum column {
	    source(10), temporary(9),target(1);
	    private final int id;
	    column(int id) { this.id = id; }
	    public int getValue() { return id; }
	}

   public RobotControl(Robot r)
   {
	   this.r = r;
   }

   private void resetRobot(){
	   changeArmTwoWidth(1);
	   changeArmThreeDepth(0);
	   changeArmOneHeight(getTargetBlocksHeight() == 0?2:(getTargetBlocksHeight()+1));
   }
   
   // pick a block from the source or the target blocks column
   private void pickBlock(int barHeights[], int blockHeights[],int blockNumber,column columnType){
	   int stepsToMoveArmThree =0;
	   if(columnType==column.source){
		   stepsToMoveArmThree  = this.armOneCurrentHeight - getSourceBlocksHeight() -1;
		   sourceBlocks.remove(blockNumber);
	   }
	   else{
		   stepsToMoveArmThree  = this.armOneCurrentHeight - getTemporaryBlocksHeight()-1;
		   temporaryBlocks.remove(blockNumber);
	   }
	   changeArmThreeDepth(stepsToMoveArmThree);
	   r.pick();
	   changeArmThreeDepth(0);
   }
   
   private int getValueForBlockInColumn(column collumnType,int blockNumber){
	   if(collumnType == column.source)
		   return sourceBlocks.get(blockNumber);
	   else if (collumnType == column.temporary)
		   return temporaryBlocks.get(blockNumber);
	   else if (collumnType == column.target)
		   return targetBlocks.get(blockNumber);
	   return 0;
   }
   
   private int removeBlockFromColumnList(column collumnType,int blockNumber){
	   if(collumnType == column.source)
		   return sourceBlocks.remove(blockNumber);
	   else if (collumnType == column.temporary)
		   return temporaryBlocks.remove(blockNumber);
	   else if (collumnType == column.target)
		   return targetBlocks.remove(blockNumber);
	   return 0;
   }
   
   
   private void dropBlock(int barHeights[], int blockHeights[],int blockNumber,column fromColumn,column toColumn){
	   int stepsToMoveArmThree =0;
	   if(toColumn==column.source){
		   stepsToMoveArmThree = this.armOneCurrentHeight - 1 -  getSourceBlocksHeight() - blockHeights[blockNumber];
		   sourceBlocks.add(blockNumber);
	   }
	   else if(toColumn==column.target){
		   stepsToMoveArmThree = this.armOneCurrentHeight - 1 -  getTargetBlocksHeight() - blockHeights[blockNumber];
		   targetBlocks.add(blockNumber);
	   }
	   else{
		   stepsToMoveArmThree = this.armOneCurrentHeight - 1 -  getTemporaryBlocksHeight() - blockHeights[blockNumber];
		   temporaryBlocks.add(blockNumber);
	   }
//	   removeBlockFromColumnList(fromColumn, blockNumber);
	   changeArmThreeDepth(stepsToMoveArmThree);
	   r.drop();
	   changeArmThreeDepth(0);
   }
   
   
//   private void dropBlock(int barHeights[], int blockHeights[],int blockNumber,column fromColumn,column toColumn){
//	   int stepsToMoveArmThree =0;
//	   if(toColumn==column.source){
//		   stepsToMoveArmThree = this.armOneCurrentHeight - 1 -  getSourceBlocksHeight() - blockHeights[blockNumber];
//		   sourceBlocks.add(blockNumber);
//	   }
//	   else if(toColumn==column.target){
//		   stepsToMoveArmThree = this.armOneCurrentHeight - 1 -  getTargetBlocksHeight() - blockHeights[blockNumber];
//		   targetBlocks.add(blockNumber);
//	   }
//	   else{
//		   stepsToMoveArmThree = this.armOneCurrentHeight - 1 -  getTemporaryBlocksHeight() - blockHeights[blockNumber];
//		   temporaryBlocks.add(blockNumber);
//	   }
//	   changeArmThreeDepth(stepsToMoveArmThree);
//	   r.drop();
//	   changeArmThreeDepth(0);
//   }
   
   // change Arm one height to the new height
   private boolean changeArmOneHeight(int newHeight){
	   /*
	    * check if the new height is bigger than the maximum allowed height
	    * or lower than the minimum allowed height
	    */
	   if (newHeight > maxArmOneHeight || newHeight < minArmOneHeight) {
		   System.out.println("New height error: " + newHeight);
		   return false;
	   }
	   
	   // check if the new height is similar to the current height
	   if(this.armOneCurrentHeight == newHeight){
		   return true;
	   }
	   
	   // if the new height is higher than the current height, increase the height
	   else if(newHeight > armOneCurrentHeight){
		   for (int x=this.armOneCurrentHeight ; x < newHeight; x++) {
			   r.up();
			   this.armOneCurrentHeight = x+1;
		   }
	   }
	   // if the new height is lower than the current height, decrease the height
	   else if(newHeight < armOneCurrentHeight){
		   for (int x = this.armOneCurrentHeight; x > newHeight; x--) {
			   r.down();
			   this.armOneCurrentHeight = x-1;
		   }
	   }
	   return true;
   }
   
   private boolean changeArmTwoWidth(int newWidth){
	   /*
	    * check if the new width is bigger than the maximum allowed width
	    * or lower than the minimum allowed width
	    */
	   if (newWidth > maxArmTwoWidth || newWidth < minArmTwoWidth) {
		   System.out.println("New width error: " + newWidth);
		   return false;
	   }
	   
	   // check if the new width is similar to the current width
	   if(this.armTwoCurrentWidth == newWidth){
		   return true;
	   }
	   
	   // if the new width is higher than the current width, increase the width
	   else if(newWidth > armTwoCurrentWidth){
		   for (int x=this.armTwoCurrentWidth ; x < newWidth; x++) {
			   r.extend();
			   this.armTwoCurrentWidth = x+1;
		   }
	   }
	   // if the new height is lower than the current width, decrease the width
	   else if(newWidth < armTwoCurrentWidth){
		   for (int x = this.armTwoCurrentWidth; x > newWidth; x--) {
			   r.contract();
			   this.armTwoCurrentWidth = x-1;
		   }
	   }
	   return true;
   }

   private boolean changeArmThreeDepth(int newDepth){
	   /*
	    * check if the new depth is bigger than arm one height
	    * or lower than the minimum allowed depth
	    */
	   if (newDepth >= this.armOneCurrentHeight || newDepth < minArmThreeDepth) {
		   System.out.println("New depth error: " + newDepth);
		   return false;
	   }
	   
	   // check if the new depth is similar to the current depth
	   if(this.armThreeCurrentDepth == newDepth){
		   return true;
	   }
	   
	   // if the new depth is higher than the current depth, increase the depth
	   else if(newDepth > armThreeCurrentDepth){
		   for (int x=this.armThreeCurrentDepth ; x < newDepth; x++) {
			   r.lower();
			   this.armThreeCurrentDepth = x+1;
		   }
	   }
	   // if the new depth is lower than the current depth, decrease the depth
	   else if(newDepth < armThreeCurrentDepth){
		   for (int x = this.armThreeCurrentDepth; x > newDepth; x--) {
			   r.raise();
			   this.armThreeCurrentDepth = x-1;
		   }
	   }
	   return true;
   }
   
   private int blockHeight(int blockNumber){
	   return originalBlockHeights[blockNumber];
   }
   
   private int getHeighestBar(){
	   int highestValue =0;
	   for(int x=0;x<barHeights.length;x++){
		   if(barHeights[x]>highestValue)
			   highestValue=barHeights[x];
	   }
	   return highestValue;
   }
   
   private int getTemporaryBlocksHeight(){
	   int totalBlockHeights=0;
	   for (int blockIndex : this.temporaryBlocks) {
		   totalBlockHeights+= this.originalBlockHeights[blockIndex];
	   }
	   return totalBlockHeights;
   }  
   
   private int getTargetBlocksHeight(){
	   int totalBlockHeights=0;
	   for (int blockIndex : this.targetBlocks) {
		   totalBlockHeights+= this.originalBlockHeights[blockIndex];
	   }
	   return totalBlockHeights;
   }
   private int getSourceBlocksHeight(){
	   int sourceBlocksHeight =0;
	   for(int x=0;x<sourceBlocks.size();x++){
		   sourceBlocksHeight+=sourceBlocks.get(x);
	   }
	   return sourceBlocksHeight;
   }
   
   
   private int calculateHeightFromTemporaryToTarget(int blockNumber){
	   int maxHeight = MyMath.max(getTargetBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight());
	   if(getTemporaryBlocksHeight() - blockHeight(blockNumber) >= maxHeight){
		   return getTemporaryBlocksHeight() + 1; 
	   }  
	   return Math.max(getHeighestBar(),getTemporaryBlocksHeight()) + blockHeight(blockNumber) +1;
   }
   
   private int calculateHeightFromSourceToTarget(int blockNumber){
	   int maxHeight = MyMath.max(getTargetBlocksHeight(), getHeighestBar(), getTemporaryBlocksHeight());
	   if(getSourceBlocksHeight() - blockHeight(blockNumber) >= maxHeight){
		   return getSourceBlocksHeight() + 1; 
	   }  
	   return MyMath.max(getTargetBlocksHeight(), getHeighestBar(),getTemporaryBlocksHeight()) + blockHeight(blockNumber) +1;
   }
   private int calculateHeightFromSourceToTemporary(int blockNumber){
	   int maxHeight = MyMath.max(getHeighestBar() ,getTemporaryBlocksHeight(),getTargetBlocksHeight());
	   if(getSourceBlocksHeight() - sourceBlocks.get(blockNumber) >= maxHeight){
		   System.out.println("A");
		   return getSourceBlocksHeight() + 1;  
	   }
	   
	   else if (getTemporaryBlocksHeight() + sourceBlocks.get(blockNumber) <= maxHeight){
		   System.out.println("B");
		   return maxHeight +1;
	   }
	   System.out.println("C");
	   return (maxHeight -  getTemporaryBlocksHeight()) + getTemporaryBlocksHeight() + sourceBlocks.get(blockNumber) +1 ;
   }
   
   /*
    * Calculate the height for Arm one based on the following
    * Source blocks height
    * Target Blocks height
    * Highest bar available
    */
   private int calculateHeight(int blockNumber,int blocksArray[],int barHeights[],column fromColumn,column toColumn){
	   if((fromColumn == column.source && toColumn == column.target)){
		   return calculateHeightFromSourceToTarget(blockNumber);
	   }
	   else if (fromColumn == column.temporary && toColumn == column.target){
		   return calculateHeightFromTemporaryToTarget(blockNumber);
	   }
	   else if(fromColumn == column.source && toColumn == column.temporary){
		   System.out.println("calculateHeightFromSourceToTemporary");
		   return calculateHeightFromSourceToTemporary(blockNumber);
	   }
	   return 0;
   }
   
   private void MoveBlock(column fromColumn,column toColumn,int blockNumber,int barHeights[], int blockHeights[]){
	   changeArmOneHeight(calculateHeight(blockNumber, blockHeights, barHeights,fromColumn,toColumn));
	   changeArmTwoWidth(fromColumn.getValue());
	   pickBlock(barHeights, blockHeights, blockNumber,fromColumn);
	   changeArmTwoWidth(toColumn.getValue());
	   dropBlock(barHeights, blockHeights, blockNumber,fromColumn,toColumn);
   }

   
//////////////////////////////////////////////////////////////////////////////////
////////////////													//////////////
////////////////				NEW CODE IN PROGRESS				//////////////
////////////////													//////////////
//////////////////////////////////////////////////////////////////////////////////
   

   public void control(int barHeights[], int blockHeights[], int required[], boolean ordered)
   {

//	   blockHeights[0]=1;
//	   blockHeights[1]=1;
//	   blockHeights[2]=2;
//	   blockHeights[3]=1;

//	   barHeights[0] = 1;
//	   barHeights[1] = 1;
//	   barHeights[2] = 1;
//	   barHeights[3] = 6;
//	   barHeights[4] = 7;
//	   barHeights[5] = 4;
	   
	   this.targetBlocks = new ArrayList<>();
	   this.sourceBlocks = new ArrayList<>();
	   this.temporaryBlocks = new ArrayList<>();
	   
	   this.barHeights = barHeights;
	   this.originalBlockHeights = blockHeights;
	   
	   for(int x=0;x<blockHeights.length;x++){
		   sourceBlocks.add(blockHeights[x]);
	   }
	   
	   // The first past can be solved easily with out any arrays as the height of bars and blocks are fixed.
	   // Use the method r.up(), r.down(), r.extend(), r.contract(), r.raise(), r.lower(), r.pick(), r.drop()
	   // The code below will cause first arm to be moved up, the second arm to the right and the third to be lowered.
//	   for(int x=3;x>=0;x--){
//		   this.MoveBlock(column.source, column.temporary, x, barHeights, blockHeights);
//	   }
	   this.MoveBlock(column.source, column.temporary, 3, barHeights, blockHeights);
	   this.MoveBlock(column.source, column.temporary, 2, barHeights, blockHeights);
	   this.MoveBlock(column.source, column.temporary, 1, barHeights, blockHeights);
	   this.MoveBlock(column.source, column.temporary, 0, barHeights, blockHeights);
	   
	   this.MoveBlock(column.temporary, column.source, 0, barHeights, blockHeights);
	   this.MoveBlock(column.temporary, column.source, 1, barHeights, blockHeights);
	   this.MoveBlock(column.temporary, column.source, 2, barHeights, blockHeights);
	   this.MoveBlock(column.temporary, column.source, 3, barHeights, blockHeights);	   
//	   this.MoveBlock(column.temporary, column.target, 1, barHeights, blockHeights);
	   
//	   this.MoveBlock(column.temporary, column.source, 0, barHeights, blockHeights);
//	   this.MoveBlock(column.temporary, column.source, 1, barHeights, blockHeights);
//	   this.MoveBlock(column.temporary, column.target, 2, barHeights, blockHeights);
//	   this.MoveBlock(column.temporary, column.target, 3, barHeights, blockHeights);
	   resetRobot();

//	   r.up();  	// move the first arm up by one unit
//	   r.extend();	// move the second arm to the right by one unit
//	   r.lower();	// lowering the third arm by one unit

	   // Part B requires you to access the array barHeights passed as argument as the robot arm must move
	   // over the bars




	   // The third part requires you to access the arrays barHeights and blockHeights 
	   // as the heights of bars and blocks are allowed to vary through command line arguments




	   // The fourth part allows the user  to specify the order in which bars must 
	   // be placed in the target column. This will require you to use the use additional column
	   // which can hold temporary values





	   // The last part requires you to write the code to move from source column to target column using
	   // an additional temporary column but without placing a larger block on top of a smaller block 

   }  
}  

