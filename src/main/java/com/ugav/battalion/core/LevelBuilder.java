package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.DataEvent;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.TileDesc;
import com.ugav.battalion.core.Level.UnitDesc;

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

	public TileDesc at(Cell pos) {
		return tiles.at(pos);
	}

	public LevelBuilder setTile(Cell pos, TileDesc tile) {
		String errStr = checkValidTile(pos, tile);
		if (errStr != null) {
			/* TODO: message to user */
			System.err.println("Can't set tile: " + errStr);
		} else {
			tiles.set(pos, tile);
			onTileChange.notify(new TileChange(this, pos));
		}
		return this;
	}

	public LevelBuilder setTile(Cell pos, Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
		return setTile(pos, TileDesc.of(terrain, buiding, unit));
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
		for (Cell pos : Cell.Iter2D.of(width(), height()).forEach()) {
			String errStr = checkValidTile(pos, at(pos));
			if (errStr != null)
				throw new IllegalStateException("Can't build level, error at " + pos + ": " + errStr);
		}
		return new Level(tiles, startingMoney);
	}

	private String checkValidTile(Cell pos, TileDesc tile) {
		if (!pos.isInRect(width() - 1, height() - 1))
			return "out of bound";
		if (tile.hasBuilding())
			if (!tile.building.type.canBuildOn(tile.terrain))
				return "building can't stand on terrain";
		if (tile.hasUnit())
			if (!tile.unit.type.canStandOn(tile.terrain))
				return "unit can't stand on terrain";

		Function<Cell, Terrain> terrain = p -> pos.equals(p) ? tile.terrain : tiles.at(p).terrain;
		List<Cell> checkBridge = new ArrayList<>(List.of(pos));
		for (Direction dir : Direction.values()) {
			Cell p = pos.add(dir);
			if (p.isInRect(width() - 1, height() - 1))
				checkBridge.add(p);
		}
		for (Cell bridgePos : checkBridge) {
			if (EnumSet.of(Terrain.Category.BridgeLow, Terrain.Category.BridgeHigh)
					.contains(terrain.apply(bridgePos).category))
				if (Terrain.isBridgeVertical(bridgePos, terrain, width(), height()) == null)
					return "illegal bridge, can't determine orientation";
		}
		return null;
	}

	public static class TileChange extends DataEvent {

		public final Cell pos;

		public TileChange(LevelBuilder source, Cell pos) {
			super(source);
			this.pos = pos;
		}

	}

	public static class LevelReset extends DataEvent {

		public LevelReset(LevelBuilder source) {
			super(source);
		}

	}

}
