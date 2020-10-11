package com.ugav.battalion;

public class LevelBuilder {

    private final int xLen;
    private final int yLen;
    private final Tile[][] tiles;

    public LevelBuilder(int xLen, int yLen) {
	if (xLen <= 0 || yLen <= 0)
	    throw new IllegalArgumentException();
	this.xLen = xLen;
	this.yLen = yLen;
	tiles = new Tile[xLen][yLen];
	for (int x = 0; x < xLen; x++)
	    for (int y = 0; y < yLen; y++)
		tiles[x][y] = new Tile(Terrain.FLAT_LAND, null, null);
    }

    void setTile(int x, int y, Tile tile) {
	tiles[x][y] = tile.deepCopy();
    }

    void setTile(int x, int y, Terrain terrain, Building building, Unit unit) {
	setTile(x, y, new Tile(terrain, building, unit));
    }

    void setBuilding(int x, int y, Building building) {
	Tile tile = tiles[x][y];
	tiles[x][y] = new Tile(tile.getTerrain(), building, tile.hasUnit() ? tile.getUnit() : null);
    }

    void setUnit(int x, int y, Unit unit) {
	Tile tile = tiles[x][y];
	tiles[x][y] = new Tile(tile.getTerrain(), tile.hasBuilding() ? tile.getBuilding() : null, unit);
    }

    Level buildLevel() {
	return new Level(tiles);
    }

}
