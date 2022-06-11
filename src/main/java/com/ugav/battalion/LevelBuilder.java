package com.ugav.battalion;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

class LevelBuilder {

	private final TileDesc[][] tiles;

	LevelBuilder(int xLen, int yLen) {
		if (xLen <= 0 || yLen <= 0)
			throw new IllegalArgumentException();
		tiles = new TileDesc[xLen][yLen];
		for (int x = 0; x < xLen; x++)
			for (int y = 0; y < yLen; y++)
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
