package com.ugav.battalion;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

class LevelBuilder {

	private final TileDesc[][] tiles;

	LevelBuilder(int rows, int cols) {
		if (rows <= 0 || cols <= 0)
			throw new IllegalArgumentException();
		tiles = new TileDesc[rows][cols];
		for (int r = 0; r < rows; r++)
			for (int c = 0; c < cols; c++)
				tiles[r][c] = TileDesc.of(Terrain.FLAT_LAND, null, null);
	}

	LevelBuilder setTile(int r, int c, TileDesc tile) {
		tiles[r][c] = tile;
		return this;
	}

	LevelBuilder setTile(int r, int c, Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
		return setTile(r, c, TileDesc.of(terrain, buiding, unit));
	}

	Level buildLevel() {
		return new Level(tiles);
	}

}
