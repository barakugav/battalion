package com.ugav.battalion;

import java.util.Objects;

class Tile {

	private final Terrain terrain;
	private final Building building;
	private Unit unit;

	Tile(Terrain terrain, Building building, Unit unit) {
		this.terrain = Objects.requireNonNull(terrain);
		this.building = building;
		this.unit = unit;
	}

	Terrain getTerrain() {
		return terrain;
	}

	Building getBuilding() {
		if (!hasBuilding())
			throw new IllegalStateException();
		return building;
	}

	boolean hasBuilding() {
		return building != null;
	}

	Unit getUnit() {
		if (!hasUnit())
			throw new IllegalStateException();
		return unit;
	}

	boolean hasUnit() {
		return unit != null;
	}

	void setUnit(Unit unit) {
		Objects.requireNonNull(unit);
		if (this.unit != null)
			throw new IllegalStateException();
		this.unit = unit;
	}

	void removeUnit() {
		unit = null;
	}

}
