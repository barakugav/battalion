package com.ugav.battalion;


import java.util.Objects;

public class Tile {

    private final Terrain terrain;
    private final Building building;
    private Unit unit;

    Tile(Terrain terrain, Building building, Unit unit) {
	this.terrain = Objects.requireNonNull(terrain);
	this.building = building != null ? building.deepCopy() : null;
	this.unit = unit != null ? unit.deepCopy() : null;
    }

    public Terrain getTerrain() {
	return terrain;
    }

    public Building getBuilding() {
	return building;
    }

    public boolean hasBuilding() {
	return building != null;
    }

    public Unit getUnit() {
	return unit;
    }

    public boolean hasUnit() {
	return unit != null;
    }

    public void setUnit(Unit unit) {
	Objects.requireNonNull(unit);
	if (this.unit != null)
	    throw new IllegalStateException();
	this.unit = unit;
    }

    public void removeUnit() {
	unit = null;
    }

    public Tile deepCopy() {
	Terrain terrainCopy = terrain; /* No need to deep copy */
	Building buildingCopy = building != null ? building.deepCopy() : null;
	Unit unitCopy = unit != null ? unit.deepCopy() : null;
	return new Tile(terrainCopy, buildingCopy, unitCopy);
    }

}
