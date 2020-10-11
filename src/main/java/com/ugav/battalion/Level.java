package com.ugav.battalion;

public class Level {

    private final int xLen;
    private final int yLen;
    private final Tile[][] tiles;

    public Level(Tile[][] tiles) {
	this.xLen = tiles.length;
	this.yLen = tiles[0].length;
	this.tiles = new Tile[xLen][yLen];
	for (int x = 0; x < xLen; x++)
	    for (int y = 0; y < yLen; y++)
		this.tiles[x][y] = tiles[x][y].deepCopy();
    }

    int getXLen() {
	return xLen;
    }

    int getYLen() {
	return yLen;
    }

    Tile[][] getTiles() {
	Tile[][] tileCopy = new Tile[xLen][yLen];
	for (int x = 0; x < xLen; x++)
	    for (int y = 0; y < yLen; y++)
		tileCopy[x][y] = tiles[x][y].deepCopy();
	return tileCopy;
    }

}
