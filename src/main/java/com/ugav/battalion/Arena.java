package com.ugav.battalion;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

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
		return pos.isInRect(width - 1, height - 1);
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

	Collection<Building> buildings(Team team, Predicate<Building> filter) {
		return buildings(filter.and(b -> team == b.getTeam()));
	}

	Collection<Building> buildings(Predicate<Building> filter) {
		List<Building> buildings = new ArrayList<>();
		for (Tile tile : tiles())
			if (tile.hasBuilding() && filter.test(tile.getBuilding()))
				buildings.add(tile.getBuilding());
		return buildings;
	}

	private Tile createTile(TileDesc desc, Position pos) {
		Terrain terrain = desc.terrain;
		Building building = createBuilding(desc.building, pos);
		Unit unit = createUnit(desc.unit, pos);
		return new Tile(terrain, building, unit);
	}

	private Building createBuilding(BuildingDesc desc, Position pos) {
		if (desc == null)
			return null;
		Building building = Building.valueOf(this, desc);
		building.setPos(pos);
		return building;
	}

	Unit createUnit(UnitDesc desc, Position pos) {
		if (desc == null)
			return null;
		Unit unit = Unit.valueOf(this, desc);
		unit.setPos(pos);
		return unit;
	}

	boolean isUnitVisible(Position pos, Team viewer) {
		if (!isValidPos(pos))
			throw new IllegalArgumentException();
		if (!at(pos).hasUnit())
			return false;
		Unit unit = at(pos).getUnit();

		if (!unit.type.invisible || unit.getTeam() == viewer)
			return true;
		for (Position n : pos.neighbors()) {
			if (!isValidPos(n))
				continue;
			Tile tile = at(n);
			if (tile.hasUnit() && tile.getUnit().getTeam() == viewer)
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return Utils.toString(tiles);
	}

}
