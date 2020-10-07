package com.ugav.battalion;

import java.util.Objects;

public class Tile {

    private final Surface surface;
    private final Building building;
    private Unit unit;

    private Tile(Surface surface, Building building, Unit unit) {
	this.surface = Objects.requireNonNull(surface);
	this.building = building;
	this.unit = unit;
    }

    public Surface getSurface() {
	return surface;
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
	Surface surfaceCopy = surface; /* No need to deep copy */
	Building buildingCopy = building != null ? building.deepCopy() : null;
	Unit unitCopy = unit != null ? unit.deepCopy() : null;
	return new Tile(surfaceCopy, buildingCopy, unitCopy);
    }

}
