package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.DataEvent;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.TileDesc;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.util.Iter;

public class LevelBuilder {

	private Cell.Array<TileDesc> tiles;
	private final Map<Team, Integer> startingMoney;
	public final DataChangeNotifier<TileChange> onTileChange = new DataChangeNotifier<>();
	public final DataChangeNotifier<LevelReset> onResetChange = new DataChangeNotifier<>();

	public LevelBuilder(int width, int height) {
		startingMoney = new HashMap<>();
		reset(width, height);
	}

	public LevelBuilder(Level level) {
		startingMoney = new HashMap<>();
		reset(level);
	}

	public void reset(int width, int height) {
		if (!(Level.MINIMUM_WIDTH <= width && width < 100 && Level.MINIMUM_HEIGHT <= height && height < 100))
			throw new IllegalArgumentException();
		tiles = Cell.Array.fromFunc(width, height, p -> TileDesc.of(Terrain.FlatLand1, null, null));
		startingMoney.clear();
		onResetChange.notify(new LevelReset(this));
	}

	public void reset(Level level) {
		int width = level.width(), height = level.height();
		tiles = Cell.Array.fromFunc(width, height, pos -> Objects.requireNonNull(level.at(pos)));
		startingMoney.clear();
		onResetChange.notify(new LevelReset(this));
	}

	public TileDesc at(int cell) {
		return tiles.at(cell);
	}

	public LevelBuilder setTile(int cell, TileDesc tile) {
		String errStr = checkValidTile(cell, tile);
		if (errStr != null) {
			/* TODO: message to user */
			System.err.println("Can't set tile: " + errStr);
		} else {
			tiles.set(cell, tile);
			onTileChange.notify(new TileChange(this, cell));
		}
		return this;
	}

	public LevelBuilder setTile(int cell, Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
		return setTile(cell, TileDesc.of(terrain, buiding, unit));
	}

	public int width() {
		return tiles.width();
	}

	public int height() {
		return tiles.height();
	}

	public void setStartingMoney(Team team, int money) {
		startingMoney.put(team, Integer.valueOf(money));
	}

	public Level buildLevel() {
		for (Iter.Int it = Cell.Iter2D.of(width(), height()); it.hasNext();) {
			int cell = it.next();
			String errStr = checkValidTile(cell, at(cell));
			if (errStr != null)
				throw new IllegalStateException("Can't build level, error at " + cell + ": " + errStr);
		}
		return new Level(tiles, startingMoney);
	}

	private String checkValidTile(int cell, TileDesc tile) {
		if (!Cell.isInRect(cell, width() - 1, height() - 1))
			return "out of bound";
		if (tile.hasBuilding())
			if (!tile.building.type.canBuildOn(tile.terrain))
				return "building can't stand on terrain";
		if (tile.hasUnit())
			if (!tile.unit.type.canStandOn(tile.terrain))
				return "unit can't stand on terrain";

		IntFunction<Terrain> terrain = p -> cell == p ? tile.terrain : tiles.at(p).terrain;
		List<Integer> checkBridge = new ArrayList<>(List.of(cell));
		for (Direction dir : Direction.values()) {
			int p = Cell.add(cell, dir);
			if (Cell.isInRect(p, width() - 1, height() - 1))
				checkBridge.add(p);
		}
		for (int bridgePos : checkBridge) {
			if (EnumSet.of(Terrain.Category.BridgeLow, Terrain.Category.BridgeHigh)
					.contains(terrain.apply(bridgePos).category))
				if (Terrain.isBridgeVertical(bridgePos, terrain, width(), height()) == null)
					return "illegal bridge, can't determine orientation";
		}
		return null;
	}

	public static class TileChange extends DataEvent {

		public final int cell;

		public TileChange(LevelBuilder source, int cell) {
			super(source);
			this.cell = cell;
		}

	}

	public static class LevelReset extends DataEvent {

		public LevelReset(LevelBuilder source) {
			super(source);
		}

	}

}
