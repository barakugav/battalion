package com.ugav.battalion;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

class Arena {

	private final int rows;
	private final int cols;
	private final Tile[][] tiles;

	Arena(int rows, int cols, Tile[][] tiles) {
		if (rows <= 0 || cols <= 0)
			throw new IllegalArgumentException();
		this.rows = rows;
		this.cols = cols;
		this.tiles = new Tile[rows][cols];
		for (int r = 0; r < rows; r++)
			for (int c = 0; c < cols; c++)
				this.tiles[r][c] = tiles[r][c];
	}

	Arena(Level level) {
		rows = level.getrows();
		cols = level.getcols();

		int rows = level.getrows(), cols = level.getcols();
		tiles = new Tile[rows][cols];
		for (Position pos : Utils.iterable(new Position.Iterator2D(rows, cols)))
			tiles[pos.row][pos.col] = createTile(level.tileDesc(pos), pos);
	}

	int getrows() {
		return rows;
	}

	int getcols() {
		return cols;
	}

	Tile at(Position pos) {
		return tiles[pos.row][pos.col];
	}

	boolean isValidPos(Position pos) {
		return 0 <= pos.row && pos.row < rows && 0 <= pos.col && pos.col < cols;
	}

	Collection<Position> positions() {
		return new AbstractCollection<>() {

			@Override
			public int size() {
				return rows * cols;
			}

			@Override
			public Iterator<Position> iterator() {
				return new Position.Iterator2D(rows, cols);
			}

		};
	}

	Collection<Tile> tiles() {
		return new AbstractCollection<>() {

			@Override
			public int size() {
				return rows * cols;
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
