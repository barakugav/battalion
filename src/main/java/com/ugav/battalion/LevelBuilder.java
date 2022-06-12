package com.ugav.battalion;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

class LevelBuilder {

	private final TileDesc[][] tiles;

	LevelBuilder(int width, int height) {
		if (width <= 0 || height <= 0)
			throw new IllegalArgumentException();
		tiles = new TileDesc[width][height];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				tiles[x][y] = TileDesc.of(Terrain.FLAT_LAND, null, null);
	}

	LevelBuilder setTile(int x, int y, TileDesc tile) {
		tiles[x][y] = tile;
		return this;
	}

	LevelBuilder setTile(int x, int y, Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
		return setTile(x, y, TileDesc.of(terrain, buiding, unit));
	}

	Level buildLevel() {
		return new Level(tiles);
	}

}
