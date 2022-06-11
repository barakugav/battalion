package com.ugav.battalion;

import java.util.function.Consumer;

class Map {

	private final int xLen;
	private final int yLen;
	private final Tile[][] tiles;

	static class Position {
		final int x, y;

		Position(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	static enum Neighbor {

		XPos(1, 0), XNeg(-1, 0), YPos(0, 1), YNeg(0, -1);

		final int dx, dy;

		Neighbor(int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
		}

		Position from(int x, int y) {
			return new Position(x + dx, y + dy);
		}

		Position from(Position pos) {
			return from(pos.x, pos.y);
		}

		static Position[] of(int x, int y) {
			Position[] pos = new Position[Neighbor.values().length];
			for (int i = 0; i < Neighbor.values().length; i++)
				pos[i] = Neighbor.values()[i].from(x, y);
			return pos;
		}

	};

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

	Tile at(Position pos) {
		return at(pos.x, pos.y);
	}

	boolean isInMap(int x, int y) {
		return 0 <= x && x < xLen && 0 <= y && y < yLen;
	}

	boolean isInMap(Position pos) {
		return isInMap(pos.x, pos.y);
	}

	void forEach(Consumer<Tile> func) {
		for (int x = 0; x < xLen; x++)
			for (int y = 0; y < yLen; y++)
				func.accept(tiles[x][y]);
	}

}
