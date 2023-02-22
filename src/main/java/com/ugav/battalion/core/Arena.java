package com.ugav.battalion.core;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.Utils;
import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.TileDesc;
import com.ugav.battalion.core.Level.UnitDesc;

public class Arena {

	private final Cell.Array<Tile> tiles;

	public final DataChangeNotifier<EntityChange> onEntityChange = new DataChangeNotifier<>();

	private Arena(Level level) {
		tiles = Cell.Array.fromFunc(level.width(), level.height(), pos -> createTile(level.at(pos), pos));
	}

	private Arena(Arena arena) {
		tiles = Cell.Array.fromFunc(arena.width(), arena.height(), pos -> Tile.copyOf(this, arena.at(pos)));
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

	public Tile at(Cell pos) {
		return tiles.at(pos);
	}

	public boolean isValidPos(Cell pos) {
		return pos.isInRect(width() - 1, height() - 1);
	}

	private class Cells extends ICollection.Abstract<Cell> {

		@Override
		public int size() {
			return width() * height();
		}

		@Override
		public Iter<Cell> iterator() {
			return new Cell.Iterator2D(width(), height());
		}

	}

	private final Cells cellsView = new Cells();

	public ICollection<Cell> positions() {
		return cellsView;
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

	private Tile createTile(TileDesc desc, Cell pos) {
		Terrain terrain = desc.terrain;
		Building building = createBuilding(desc.building, pos);
		Unit unit = createUnit(desc.unit, pos);
		return Tile.of(terrain, building, unit);
	}

	private Building createBuilding(BuildingDesc desc, Cell pos) {
		if (desc == null)
			return null;
		Building building = Building.valueOf(this, desc);
		building.setPos(pos);
		return building;
	}

	private Unit createUnit(UnitDesc desc, Cell pos) {
		if (desc == null)
			return null;
		Unit unit = Unit.valueOf(this, desc);
		unit.setPos(pos);
		return unit;
	}

	public boolean isUnitVisible(Cell pos, Team viewer) {
		if (!isValidPos(pos))
			throw new IllegalArgumentException();
		if (!at(pos).hasUnit())
			return false;
		Unit unit = at(pos).getUnit();

		if (!unit.type.invisible || unit.getTeam() == viewer)
			return true;
		for (Cell n : pos.neighbors()) {
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
