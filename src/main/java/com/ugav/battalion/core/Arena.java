package com.ugav.battalion.core;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.Utils;
import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.TileDesc;
import com.ugav.battalion.core.Level.UnitDesc;

public class Arena {

	private final Position.Array<Tile> tiles;

	public final DataChangeNotifier<EntityChange> onEntityChange = new DataChangeNotifier<>();

	private Arena(Level level) {
		tiles = Position.Array.fromFunc(level.width(), level.height(), pos -> createTile(level.at(pos), pos));
	}

	private Arena(Arena arena) {
		tiles = Position.Array.fromFunc(arena.width(), arena.height(), pos -> Tile.copyOf(this, arena.at(pos)));
	}

	static Arena fromLevel(Level level) {
		return new Arena(level);
	}

	static Arena copyOf(Arena arena) {
		return new Arena(arena);
	}

	public int width() {
		return tiles.width();
	}

	public int height() {
		return tiles.height();
	}

	public Tile at(Position pos) {
		return tiles.at(pos);
	}

	public boolean isValidPos(Position pos) {
		return pos.isInRect(width() - 1, height() - 1);
	}

	private class Positions extends ICollection.Abstract<Position> {

		@Override
		public int size() {
			return width() * height();
		}

		@Override
		public Iter<Position> iterator() {
			return new Position.Iterator2D(width(), height());
		}

	}

	private final Positions positionsView = new Positions();

	public ICollection<Position> positions() {
		return positionsView;
	}

	private class Tiles extends ICollection.Abstract<Tile> {

		@Override
		public int size() {
			return width() * height();
		}

		@Override
		public Iter<Tile> iterator() {
			return positions().iterator().map(p -> at(p));
		}

	}

	private final Tiles tilesView = new Tiles();

	public ICollection<Tile> tiles() {
		return tilesView;
	}

	public Iter<Building> buildings() {
		return tiles().iterator().filter(Tile::hasBuilding).map(Tile::getBuilding);
	}

	public Iter<Unit> units(Team team) {
		return units().filter(u -> team == u.getTeam());
	}

	public Iter<Unit> units() {
		return tiles().iterator().filter(Tile::hasUnit).map(Tile::getUnit);
	}

	private Tile createTile(TileDesc desc, Position pos) {
		Terrain terrain = desc.terrain;
		Building building = createBuilding(desc.building, pos);
		Unit unit = createUnit(desc.unit, pos);
		return Tile.of(terrain, building, unit);
	}

	private Building createBuilding(BuildingDesc desc, Position pos) {
		if (desc == null)
			return null;
		Building building = Building.valueOf(this, desc);
		building.setPos(pos);
		return building;
	}

	private Unit createUnit(UnitDesc desc, Position pos) {
		if (desc == null)
			return null;
		Unit unit = Unit.valueOf(this, desc);
		unit.setPos(pos);
		return unit;
	}

	public boolean isUnitVisible(Position pos, Team viewer) {
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
		return Utils.toString(tiles.toArray());
	}

}
