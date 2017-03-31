
public class Hanoi {
	public static column[] getMoveDirection(column fromColumn, column toColumn,int fromValue , int toValue) {
	
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
}
