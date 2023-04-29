package com.bugav.battalion.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

import com.bugav.battalion.core.Level.BuildingDesc;
import com.bugav.battalion.core.Level.UnitDesc;
import com.bugav.battalion.util.Event;
import com.bugav.battalion.util.Iter;
import com.bugav.battalion.util.ListInt;

public class LevelBuilder {

	private Cell.Array<Terrain> terrains;
	private Cell.Array<BuildingDesc> buildings;
	private Cell.Array<UnitDesc> units;
	private final Map<Team, Integer> startingMoney;
	public final Event.Notifier<TileChange> onTileChange = new Event.Notifier<>();
	public final Event.Notifier<LevelReset> onResetChange = new Event.Notifier<>();

	public LevelBuilder(int width, int height) {
		startingMoney = new HashMap<>();
		reset(width, height);
	}

	public LevelBuilder(Level level) {
		startingMoney = new HashMap<>();
		reset(level);
	}

	public void reset(int width, int height) {
		if (width < 0 || height < 0)
			throw new IllegalArgumentException();
		terrains = Cell.Array.fromFunc(width, height, p -> Terrain.FlatLand1);
		buildings = Cell.Array.of(width, height);
		units = Cell.Array.of(width, height);
		startingMoney.clear();
		onResetChange.notify(new LevelReset(this));
	}

	public void reset(Level level) {
		int width = level.width(), height = level.height();
		terrains = Cell.Array.fromFunc(width, height, pos -> Objects.requireNonNull(level.terrain(pos)));
		buildings = Cell.Array.fromFunc(width, height, pos -> level.building(pos));
		units = Cell.Array.fromFunc(width, height, pos -> level.unit(pos));
		startingMoney.clear();
		onResetChange.notify(new LevelReset(this));
	}

	public int width() {
		return terrains.width();
	}

	public int height() {
		return terrains.height();
	}

	public Terrain terrain(int cell) {
		return terrains.at(cell);
	}

	public BuildingDesc building(int cell) {
		return buildings.at(cell);
	}

	public UnitDesc unit(int cell) {
		return units.at(cell);
	}

	public void setTerrain(int cell, Terrain terrain) {
		BuildingDesc building = this.building(cell);
		if (building != null && !building.type.canBuildOn(terrain))
			building = null;

		UnitDesc unit = this.unit(cell);
		if (unit != null && !unit.type.canStandOn(terrain))
			unit = null;

		String errStr = checkValidTile(cell, terrain, building, unit);
		if (errStr != null) {
			/* TODO: message to user */
			System.err.println("Can't set tile: " + errStr);
		} else {
			terrains.set(cell, terrain);
			units.set(cell, unit);
			buildings.set(cell, building);
			onTileChange.notify(new TileChange(this, cell));
		}
	}

	public void setBuilding(int cell, BuildingDesc building) {
		if (building == null || building.type.canBuildOn(terrain(cell))) {
			buildings.set(cell, building);
			onTileChange.notify(new TileChange(this, cell));
		}
	}

	public void setUnit(int cell, UnitDesc unit) {
		if (unit == null || unit.type.canStandOn(terrain(cell))) {
			units.set(cell, unit);
			onTileChange.notify(new TileChange(this, cell));
		}
	}

	public void setStartingMoney(Team team, int money) {
		startingMoney.put(team, Integer.valueOf(money));
	}

	public Level buildLevel() {
		for (Iter.Int it = Cell.Iter2D.of(width(), height()); it.hasNext();) {
			int cell = it.next();
			String errStr = checkValidTile(cell, terrain(cell), building(cell), unit(cell));
			if (errStr != null)
				throw new IllegalStateException("Can't build level, error at " + cell + ": " + errStr);
		}
		return new Level(terrains, buildings, units, startingMoney);
	}

	private String checkValidTile(int cell, Terrain terrain, BuildingDesc building, UnitDesc unit) {
		if (!Cell.isInRect(cell, width() - 1, height() - 1))
			return "out of bound";
		if (building != null && !building.type.canBuildOn(terrain))
			return "building can't stand on terrain";
		if (unit != null && !unit.type.canStandOn(terrain))
			return "unit can't stand on terrain";

		IntFunction<Terrain> getTerrain = p -> cell == p ? terrain : this.terrain(p);
		ListInt checkBridge = new ListInt.Array();
		checkBridge.add(cell);
		for (Direction dir : Direction.values()) {
			int p = Cell.add(cell, dir);
			if (Cell.isInRect(p, width() - 1, height() - 1))
				checkBridge.add(p);
		}

		for (Iter.Int it = checkBridge.iterator(); it.hasNext();) {
			int bridgePos = it.next();
			if (getTerrain.apply(bridgePos).isBridge())
				if (Terrain.isBridgeVertical(bridgePos, getTerrain, width(), height()) == null)
					return "illegal bridge, can't determine orientation";
		}
		return null;
	}

	public static class TileChange extends Event {

		public final int cell;

		public TileChange(LevelBuilder source, int cell) {
			super(source);
			this.cell = cell;
		}

	}

	public static class LevelReset extends Event {

		public LevelReset(LevelBuilder source) {
			super(source);
		}

	}

}
