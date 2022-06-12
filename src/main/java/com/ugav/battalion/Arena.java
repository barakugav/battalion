package com.ugav.battalion;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

class Arena {

	private final int xLen;
	private final int yLen;
	private final Tile[][] tiles;

	Arena(int xLen, int yLen, Tile[][] tiles) {
		if (xLen <= 0 || yLen <= 0)
			throw new IllegalArgumentException();
		this.xLen = xLen;
		this.yLen = yLen;
		this.tiles = new Tile[xLen][yLen];
		for (int x = 0; x < xLen; x++)
			for (int y = 0; y < yLen; y++)
				this.tiles[x][y] = tiles[x][y];
	}

	Arena(Level level) {
		xLen = level.getXLen();
		yLen = level.getYLen();

		int xLen = level.getXLen(), yLen = level.getYLen();
		tiles = new Tile[xLen][yLen];
		for (Position pos : Utils.iterable(new Position.Iterator2D(xLen, yLen)))
			tiles[pos.x][pos.y] = createTile(level.tileDesc(pos), pos);
	}

	int getXLen() {
		return xLen;
	}

	int getYLen() {
		return yLen;
	}

	Tile at(Position pos) {
		return tiles[pos.x][pos.y];
	}

	boolean isValidPos(Position pos) {
		return 0 <= pos.x && pos.x < xLen && 0 <= pos.y && pos.y < yLen;
	}

	Collection<Position> positions() {
		return new AbstractCollection<>() {

			@Override
			public int size() {
				return xLen * yLen;
			}

			@Override
			public Iterator<Position> iterator() {
				return new Position.Iterator2D(xLen, yLen);
			}

		};
	}

	Collection<Tile> tiles() {
		return new AbstractCollection<>() {

			@Override
			public int size() {
				return xLen * yLen;
			}

			@Override
			public Iterator<Tile> iterator() {
				return new Iterator<>() {

					Iterator<Position> posIt = positions().iterator();

					@Override
					public boolean hasNext() {
						return posIt.hasNext();
					}

					@Override
					public Tile next() {
						return at(posIt.next());
					}

				};
			}

		};
	}

	private Tile createTile(TileDesc desc, Position pos) {
		Terrain terrain = desc.terrain;
		Building building = createBuilding(desc.buiding);
		Unit unit = createUnit(desc.unit, pos);
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

	private Unit createUnit(UnitDesc desc, Position pos) {
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
		unit.setArena(this);
		unit.setPos(pos);
		return unit;
	}

}
