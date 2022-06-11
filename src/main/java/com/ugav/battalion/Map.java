package com.ugav.battalion;

import java.util.function.Consumer;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

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
				this.tiles[x][y] = tiles[x][y];
	}

	Map(Level level) {
		xLen = level.getXLen();
		yLen = level.getYLen();

		int xLen = level.getXLen(), yLen = level.getYLen();
		tiles = new Tile[xLen][yLen];
		for (int x = 0; x < xLen; x++)
			for (int y = 0; y < yLen; y++)
				tiles[x][y] = createTile(level.tileDesc(x, y), x, y);
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
		// TODO iterator
		for (int x = 0; x < xLen; x++)
			for (int y = 0; y < yLen; y++)
				func.accept(tiles[x][y]);
	}

	private Tile createTile(TileDesc desc, int x, int y) {
		Terrain terrain = desc.terrain;
		Building building = createBuilding(desc.buiding);
		Unit unit = createUnit(desc.unit, x, y);
		return new Tile(terrain, building, unit);
	}

	@SuppressWarnings("static-method")
	private Building createBuilding(BuildingDesc desc) {
		if (desc == null)
			return null;
		switch (desc.type) {
		case OilRefinery:
			return new Building.OilRefinery(desc.team);
		case Factory:
			return new Building.Factory(desc.team);
		default:
			throw new InternalError();
		}
	}

	private Unit createUnit(UnitDesc desc, int x, int y) {
		if (desc == null)
			return null;
		Unit unit;
		switch (desc.type) {
		case Soldier:
			unit = new Unit.Soldier(desc.team);
			break;
		case Tank:
			unit = new Unit.Tank(desc.team);
			break;
		default:
			throw new InternalError();
		}
		unit.setMap(this);
		unit.setPos(x, y);
		return unit;
	}

}
