package com.ugav.battalion;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

class Arena {

	private final int width;
	private final int height;
	private final Tile[][] tiles;

	Arena(int width, int height, Tile[][] tiles) {
		if (width <= 0 || height <= 0)
			throw new IllegalArgumentException();
		this.width = width;
		this.height = height;
		this.tiles = new Tile[width][height];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				this.tiles[x][y] = tiles[x][y];
	}

	Arena(Level level) {
		width = level.getWidth();
		height = level.getHeight();

		int width = level.getWidth(), height = level.getHeight();
		tiles = new Tile[width][height];
		for (Position pos : Utils.iterable(new Position.Iterator2D(width, height)))
			tiles[pos.x][pos.y] = createTile(level.tileDesc(pos), pos);
	}

	int getWidth() {
		return width;
	}

	int getHeight() {
		return height;
	}

	Tile at(Position pos) {
		return tiles[pos.x][pos.y];
	}

	boolean isValidPos(Position pos) {
		return 0 <= pos.x && pos.x < width && 0 <= pos.y && pos.y < height;
	}

	Collection<Position> positions() {
		return new AbstractCollection<>() {

			@Override
			public int size() {
				return width * height;
			}

			@Override
			public Iterator<Position> iterator() {
				return new Position.Iterator2D(width, height);
			}

		};
	}

	Collection<Tile> tiles() {
		return new AbstractCollection<>() {

			@Override
			public int size() {
				return width * height;
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
		Building building = createBuilding(desc.buiding, pos);
		Unit unit = createUnit(desc.unit, pos);
		return new Tile(terrain, building, unit);
	}

	@SuppressWarnings("static-method")
	private Building createBuilding(BuildingDesc desc, Position pos) {
		if (desc == null)
			return null;
		Building building;
		switch (desc.type) {
		case OilRefinery:
			building = new Building.OilRefinery(desc.team);
			break;
		case Factory:
			building = new Building.Factory(desc.team);
			break;
		default:
			throw new InternalError();
		}
		building.setPos(pos);
		return building;
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

	@Override
	public String toString() {
		return Utils.toString(tiles);
	}

}
