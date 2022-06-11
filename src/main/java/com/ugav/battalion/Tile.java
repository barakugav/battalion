package com.ugav.battalion;

import java.util.Objects;

class Tile {

	private final Terrain terrain;
	private final Building building;
	private Unit unit;

	Tile(Terrain terrain, Building building, Unit unit) {
		this.terrain = Objects.requireNonNull(terrain);
		this.building = building != null ? building.deepCopy() : null;
		this.unit = unit != null ? unit.deepCopy() : null;
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

	Tile deepCopy() {
		Terrain terrainCopy = terrain; /* No need to deep copy */
		Building buildingCopy = building != null ? building.deepCopy() : null;
		Unit unitCopy = unit != null ? unit.deepCopy() : null;
		return new Tile(terrainCopy, buildingCopy, unitCopy);
	}

}
