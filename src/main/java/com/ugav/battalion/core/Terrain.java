package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

public enum Terrain {

	FlatLand1(Category.FlatLand), FlatLand2(Category.FlatLand), FlatLand3(Category.FlatLand),
	FlatLand4(Category.FlatLand), FlatLand5(Category.FlatLand), FlatLand6(Category.FlatLand),
	FlatLand7(Category.FlatLand),

	Forest1(Category.Forest), Forest2(Category.Forest),

	Hills1(Category.Hiils), Hills2(Category.Hiils), Hills3(Category.Hiils), Hills4(Category.Hiils),

	Mountain1(Category.Mountain), Mountain2(Category.Mountain), Mountain3(Category.Mountain),
	Mountain4(Category.Mountain),

	Road(Category.Road),

	BridgeLow(Category.BridgeLow), BridgeHigh(Category.BridgeHigh),

	Shore(Category.Shore),

	ClearWater(Category.Water),

	WaterShallow1(Category.WaterShallow), WaterShallow2(Category.WaterShallow), WaterShallow3(Category.WaterShallow),
	WaterShallow4(Category.WaterShallow), WaterShallow5(Category.WaterShallow), WaterShallow6(Category.WaterShallow),
	WaterShallow7(Category.WaterShallow), WaterShallow8(Category.WaterShallow), WaterShallow9(Category.WaterShallow),
	WaterShallow10(Category.WaterShallow), WaterShallow11(Category.WaterShallow), WaterShallow12(Category.WaterShallow);

	public final Category category;

	private Terrain(Category category) {
		this.category = category;
	}

	public boolean hasWater() {
		switch (category) {
		case Water:
		case WaterShallow:
		case Shore:
		case BridgeLow:
		case BridgeHigh:
			return true;
		default:
			return false;
		}
	}

	public boolean isWater() {
		switch (category) {
		case Water:
		case WaterShallow:
			return true;
		default:
			return false;
		}
	}

	public boolean isBridge() {
		switch (category) {
		case BridgeLow:
		case BridgeHigh:
			return true;
		default:
			return false;
		}
	}

	public boolean isRoad() {
		switch (category) {
		case Road:
		case BridgeLow:
		case BridgeHigh:
			return true;
		default:
			return false;
		}
	}

	public enum Category {
		FlatLand, Forest, Hiils, Mountain, Road, BridgeLow, BridgeHigh, Shore, Water, WaterShallow;

		public List<Terrain> getTerrains() {
			List<Terrain> terrains = new ArrayList<>();
			for (Terrain terrain : Terrain.values())
				if (terrain.category == this)
					terrains.add(terrain);
			return terrains;
		}
	}

	public static Set<Direction> getBridgeConnection(int cell, IntFunction<Terrain> terrain, int width, int high) {
		if (!terrain.apply(cell).isBridge())
			throw new IllegalArgumentException("terrain is not a bridge at: " + Cell.toString(cell));

		Set<Direction> connections = EnumSet.noneOf(Direction.class);
		for (Direction dir : Direction.values()) {
			int p = Cell.add(cell, dir);
			if (!Cell.isInRect(p, width - 1, high - 1))
				continue;
			Terrain t = terrain.apply(p);
			if (t.isRoad())
				connections.add(dir);
			else if (t.isWater())
				connections.addAll(dir.orthogonal());
		}

		return connections;
	}

	public static Boolean isBridgeVertical(int cell, IntFunction<Terrain> terrain, int width, int high) {
		if (!terrain.apply(cell).isBridge())
			throw new IllegalArgumentException("terrain is not a bridge at: " + Cell.toString(cell));

		Set<Direction> connections = getBridgeConnection(cell, terrain, width, high);
		switch (connections.size()) {
		case 0:
			return Boolean.TRUE;
		case 1:
			return Boolean.valueOf(connections.iterator().next().isXDir());
		case 2:
			if (connections.equals(Direction.xDirs()))
				return Boolean.TRUE;
			if (connections.equals(Direction.yDirs()))
				return Boolean.FALSE;
		default:
			return null;
		}

	}

}