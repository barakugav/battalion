package com.ugav.battalion;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Utils;

class GestureTask implements TickTask {

	private int counter = 0;
	private static final int GestureDuration = 16;
	private static final int GestureNum;
	static {
		Set<Integer> possibleGestureNum = new HashSet<>();
		IntConsumer addGestureNum = x -> possibleGestureNum.add(Integer.valueOf(x));
		for (Unit.Type type : Unit.Type.values()) {
			addGestureNum.accept(Images.getGestureNumUnitMove(type));
			addGestureNum.accept(Images.getGestureNumUnitStand(type));
		}
		for (Building.Type type : Building.Type.values())
			addGestureNum.accept(Images.getGestureNum(type));
		addGestureNum.accept(Images.getGestureNum("Flag"));

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
