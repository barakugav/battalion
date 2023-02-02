package com.ugav.battalion;

import java.util.Objects;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

class LevelBuilder {

	private TileDesc[][] tiles;
	final DataChangeNotifier<DataEvent.TileChange> onTileChange;
	final DataChangeNotifier<DataEvent.LevelReset> onResetChange;

	LevelBuilder(int width, int height) {
		onTileChange = new DataChangeNotifier<>();
		onResetChange = new DataChangeNotifier<>();
		reset(width, height);
	}

	LevelBuilder(Level level) {
		onTileChange = new DataChangeNotifier<>();
		onResetChange = new DataChangeNotifier<>();
		reset(level);
	}

	void reset(int width, int height) {
		if (!(1 <= width && width < 100 && 1 <= height && height < 100))
			throw new IllegalArgumentException();
		tiles = new TileDesc[width][height];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				tiles[x][y] = TileDesc.of(Terrain.FLAT_LAND, null, null);
		onResetChange.notify(new DataEvent.LevelReset(this));
	}

	void reset(Level level) {
		int width = level.getWidth(), height = level.getHeight();
		tiles = new TileDesc[width][height];
		for (Position pos : Utils.iterable(new Position.Iterator2D(width, height)))
			tiles[pos.x][pos.y] = Objects.requireNonNull(level.tileDesc(pos));
		onResetChange.notify(new DataEvent.LevelReset(this));
	}

	TileDesc at(Position pos) {
		return tiles[pos.x][pos.y];
	}

	LevelBuilder setTile(int x, int y, TileDesc tile) {
		tiles[x][y] = Objects.requireNonNull(tile);
		onTileChange.notify(new DataEvent.TileChange(this, new Position(x, y)));
		return this;
	}

	LevelBuilder setTile(int x, int y, Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
		return setTile(x, y, TileDesc.of(terrain, buiding, unit));
	}

	int getWidth() {
		return tiles.length;
	}

	int getHeight() {
		return tiles[0].length;
	}

	Level buildLevel() {
		return new Level(tiles);
	}

}
