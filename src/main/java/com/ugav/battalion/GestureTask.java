package com.ugav.battalion;

import java.util.HashSet;
import java.util.Set;

import com.ugav.battalion.core.Unit;

class GestureTask implements Runnable {

	private int counter = 0;
	private static final int GestureDuration = 16;
	private static final int GestureNum;
	static {
		Set<Integer> possibleGestureNum = new HashSet<>();
		for (Unit.Type type : Unit.Type.values())
			possibleGestureNum.add(Integer.valueOf(Images.getGestureNum(type)));
		int gestureNum = 1;
		for (int x : Utils.toArray(possibleGestureNum)) {
			if (gestureNum >= 1 << 16 && x >= 1 << 16)
				throw new RuntimeException("overflow");
			gestureNum *= x;
		}
		GestureNum = gestureNum;
	}

	@Override
	public void run() {
		final int cycleLength = GestureNum * GestureDuration;
		counter = (counter + 1) % cycleLength;
	}

	int getGesture() {
		return counter / GestureDuration;
	}

}
