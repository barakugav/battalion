package com.ugav.battalion;

import java.util.function.Consumer;

class Map {

	private final int xLen;
	private final int yLen;
	private final Tile[][] tiles;

	static final int[][] neighbors = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

	Map(int xLen, int yLen, Tile[][] tiles) {
		if (xLen <= 0 || yLen <= 0)
			throw new IllegalArgumentException();
		this.xLen = xLen;
		this.yLen = yLen;
		this.tiles = new Tile[xLen][yLen];
		for (int x = 0; x < xLen; x++)
			for (int y = 0; y < yLen; y++)
				this.tiles[x][y] = tiles[x][y].deepCopy();
	}

	Map(Level level) {
		xLen = level.getXLen();
		yLen = level.getYLen();
		tiles = level.getTiles();
	}

	int getXLen() {
		return xLen;
	}

	int getYLen() {
		return yLen;
	}

	Tile at(int x, int y) {
		return tiles[x][y];
	}

	boolean isInMap(int x, int y) {
		return 0 <= x && x < xLen && 0 <= y && y < yLen;
	}

	void forEach(Consumer<Tile> func) {
		for (int x = 0; x < xLen; x++)
			for (int y = 0; y < yLen; y++)
				func.accept(tiles[x][y]);
	}

}
