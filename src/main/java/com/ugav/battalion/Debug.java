package com.ugav.battalion;

import java.util.Map;
import java.util.WeakHashMap;

import com.ugav.battalion.core.IUnit;

class Debug {

	boolean showGrid = true;
	boolean showUnitID = true;

	private int idCounter;
	private final Map<IUnit, Integer> unitsIDs = new WeakHashMap<>();

	int getUnitID(IUnit unit) {
		synchronized (unitsIDs) {
			return unitsIDs.computeIfAbsent(unit, u -> Integer.valueOf(idCounter++)).intValue();
		}
	}

}
