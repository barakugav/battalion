package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.DataEvent;
import com.ugav.battalion.Utils;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.TileDesc;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Position.Direction;

public class LevelBuilder {

	private TileDesc[][] tiles;
	public final DataChangeNotifier<DataEvent.TileChange> onTileChange = new DataChangeNotifier<>();
	public final DataChangeNotifier<DataEvent.LevelReset> onResetChange = new DataChangeNotifier<>();

	public LevelBuilder(int width, int height) {
		reset(width, height);
	}

	public LevelBuilder(Level level) {
		reset(level);
	}

	public void reset(int width, int height) {
		if (!(Level.MINIMUM_WIDTH <= width && width < 100 && Level.MINIMUM_HEIGHT <= height && height < 100))
			throw new IllegalArgumentException();
		tiles = new TileDesc[width][height];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				tiles[x][y] = TileDesc.of(Terrain.FlatLand1, null, null);
		onResetChange.notify(new DataEvent.LevelReset(this));
	}

	public void reset(Level level) {
		int width = level.getWidth(), height = level.getHeight();
		tiles = new TileDesc[width][height];
		for (Position pos : Utils.iterable(new Position.Iterator2D(width, height)))
			tiles[pos.xInt()][pos.yInt()] = Objects.requireNonNull(level.at(pos));
		onResetChange.notify(new DataEvent.LevelReset(this));
	}

	public TileDesc at(Position pos) {
		return tiles[pos.xInt()][pos.yInt()];
	}

	public LevelBuilder setTile(Position pos, TileDesc tile) {
		String errStr = checkValidTile(pos, tile);
		if (errStr != null) {
			/* TODO: message to user */
			System.err.println("Can't set tile: " + errStr);
		} else {
			tiles[pos.xInt()][pos.yInt()] = tile;
			onTileChange.notify(new DataEvent.TileChange(this, pos));
		}
		return this;
	}

	public LevelBuilder setTile(Position pos, Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
		return setTile(pos, TileDesc.of(terrain, buiding, unit));
	}

	public int getWidth() {
		return tiles.length;
	}

	public int getHeight() {
		return tiles[0].length;
	}

	public Level buildLevel() {
		int width = getWidth(), height = getHeight();
		for (Position pos : Utils.iterable(new Position.Iterator2D(width, height))) {
			String errStr = checkValidTile(pos, at(pos));
			if (errStr != null)
				throw new IllegalStateException("Can't build level, error at " + pos + ": " + errStr);
		}
		return new Level(tiles);
	}

	private String checkValidTile(Position pos, TileDesc tile) {
		if (!pos.isInRect(getWidth() - 1, getHeight() - 1))
			return "out of bound";
		if (tile.hasBuilding())
			if (!tile.building.type.canBuildOn.contains(tile.terrain.category))
				return "building can't stand on terrain";
		if (tile.hasUnit())
			if (!tile.unit.type.canStand.contains(tile.terrain.category))
				return "unit can't stand on terrain";

		Function<Position, Terrain> terrain = p -> pos.equals(p) ? tile.terrain : tiles[p.xInt()][p.yInt()].terrain;
		List<Position> checkBridge = new ArrayList<>(List.of(pos));
		for (Direction dir : Direction.values()) {
			Position p = pos.add(dir);
			if (p.isInRect(getWidth() - 1, getHeight() - 1))
				checkBridge.add(p);
		}
		for (Position bridgePos : checkBridge) {
			if (EnumSet.of(Terrain.Category.BridgeLow, Terrain.Category.BridgeHigh)
					.contains(terrain.apply(bridgePos).category))
				if (Terrain.isBridgeVertical(bridgePos, terrain, getWidth(), getHeight()) == null)
					return "illegal bridge, can't determine orientation";
		}
		return null;
	}

}
