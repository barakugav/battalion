package com.ugav.battalion;

import java.util.Map;

import com.ugav.battalion.core.IUnit;
import com.ugav.battalion.util.IdentityWeakHashMap;

class Debug {

	boolean showGrid = true;
	boolean showUnitID = true;

	private int idCounter;
	private final Map<IUnit, Integer> unitsIDs = new IdentityWeakHashMap<>();

	int getUnitID(IUnit unit) {
		synchronized (unitsIDs) {
			return unitsIDs.computeIfAbsent(unit, u -> Integer.valueOf(idCounter++)).intValue();
		}
	}

}
