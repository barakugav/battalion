package com.ugav.battalion.core;

import java.util.Objects;

public class Tile {

	private final Terrain terrain;
	private final Building building;
	private Unit unit;

	Tile(Terrain terrain, Building building, Unit unit) {
		this.terrain = Objects.requireNonNull(terrain);
		this.building = building;
		this.unit = unit;
	}

	public Terrain getTerrain() {
		return terrain;
	}

	public Building getBuilding() {
		if (!hasBuilding())
			throw new IllegalStateException();
		return building;
	}

	public boolean hasBuilding() {
		return building != null;
	}

	public Unit getUnit() {
		if (!hasUnit())
			throw new IllegalStateException();
		return unit;
	}

	public boolean hasUnit() {
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

	@Override
	public String toString() {
		String tStr = terrain.toString();
		String bStr = building != null ? building.toString() : "none";
		String uStr = unit != null ? unit.toString() : "none";
		return "<" + tStr + ", " + bStr + ", " + uStr + ">";
	}

}
