package com.bugav.battalion;

import java.util.Map;

import com.bugav.battalion.core.IUnit;
import com.bugav.battalion.util.IdentityWeakHashMap;

class Debug {

	boolean showGrid;
	boolean showUnitID;
	boolean playAllTeams;
	boolean logGameActions;
	boolean logComputerStats;
	boolean logAnimations;
	boolean skipAnimations;

	private int unitIDCounter;
	private final Map<IUnit, Integer> unitsIDs = new IdentityWeakHashMap<>();

	int getUnitID(IUnit unit) {
		synchronized (unitsIDs) {
			return unitsIDs.computeIfAbsent(unit, u -> Integer.valueOf(unitIDCounter++)).intValue();
		}
	}

}
