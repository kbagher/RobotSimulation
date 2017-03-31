
import java.util.*;

public class MyMath {
	public static int max(int a, int b, int c) {
		return Math.max(Math.max(a, b), c);
	}

	public static int min(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}
	
	public static int max(int a, int b, int c, int d) {
		return Math.max(Math.max(Math.max(a, b), c), d);
	}

	public static int max(int a, int b, int c, int d, int e) {
		return Math.max(Math.max(Math.max(Math.max(a, b), c), d), e);
	}

	public static int max(int a, int b, int c, int d, int e, int f) {
		return Math.max(Math.max(Math.max(Math.max(Math.max(a, b), c), d), e), f);
	}

	public static int max(int srcArray[]) {
		int maxValue = 0;
		for (int i : srcArray) {
			if (i > maxValue)
				maxValue = i;
		}
		return maxValue;
	}
	
	public static int max(ArrayList<Integer> srcList) {
		int maxValue = 0;
		for (Integer i : srcList) {
			if (i > maxValue)
				maxValue = i;
		}
		return maxValue;
	}
	
	
}
