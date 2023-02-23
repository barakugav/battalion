package com.ugav.battalion.core;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

public enum Terrain {

	FlatLand1(Category.FlatLand), FlatLand2(Category.FlatLand), FlatLand3(Category.FlatLand),
	FlatLand4(Category.FlatLand), FlatLand5(Category.FlatLand),

	Trees(Category.RoughLand), Hills(Category.RoughLand),

	Mountain(Category.ExtremeLand), MountainBig(Category.ExtremeLand),

	Road(Category.Road),

	BridgeLow(Category.BridgeLow), BridgeHigh(Category.BridgeHigh),

	Shore(Category.Shore),

	ClearWater(Category.Water);

	public final Category category;

	private Terrain(Category category) {
		this.category = category;
	}

	public enum Category {
		FlatLand, RoughLand, ExtremeLand, Road, BridgeLow, BridgeHigh, Shore, Water;
	}

	public static Set<Direction> getBridgeConnection(int cell, IntFunction<Terrain> terrain, int width, int high) {
		Set<Terrain.Category> connectCategoties = EnumSet.of(Terrain.Category.Road, Terrain.Category.BridgeLow,
				Terrain.Category.BridgeHigh);
		IntPredicate isInRange = p -> Cell.isInRect(p, width - 1, high - 1);

		if (!EnumSet.of(Terrain.BridgeLow, Terrain.BridgeHigh).contains(terrain.apply(cell)))
			throw new IllegalArgumentException("terrain is not a bridge at: " + cell);

		Set<Direction> connections = EnumSet.noneOf(Direction.class);
		for (Direction dir : EnumSet.of(Direction.XPos, Direction.YNeg, Direction.XNeg, Direction.YPos)) {
			int p = Cell.add(cell, dir);
			if (!isInRange.test(p))
				continue;
			Terrain.Category c = terrain.apply(p).category;
			if (isInRange.test(p) && connectCategoties.contains(c))
				connections.add(dir);
		}

		return connections;
	}

	public static Boolean isBridgeVertical(int cell, IntFunction<Terrain> terrain, int width, int high) {
		if (!EnumSet.of(Terrain.BridgeLow, Terrain.BridgeHigh).contains(terrain.apply(cell)))
			throw new IllegalArgumentException("terrain is not a bridge at: " + cell);

		Set<Direction> connections = getBridgeConnection(cell, terrain, width, high);
		switch (connections.size()) {
		case 0:
			return Boolean.TRUE;
		case 1:
			return Boolean.valueOf(EnumSet.of(Direction.XPos, Direction.XNeg).contains(connections.iterator().next()));
		case 2:
			if (connections.equals(Set.of(Direction.XPos, Direction.XNeg)))
				return Boolean.TRUE;
			if (connections.equals(Set.of(Direction.YPos, Direction.YNeg)))
				return Boolean.FALSE;
		default:
			return null;
		}

	}

}