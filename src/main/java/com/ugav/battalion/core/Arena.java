package com.ugav.battalion.core;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.Utils;
import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.TileDesc;
import com.ugav.battalion.core.Level.UnitDesc;

public class Arena {

	private final Position.Array<Tile> tiles;

	public final DataChangeNotifier<EntityChange> onEntityChange = new DataChangeNotifier<>();

	Arena(Level level) {
		tiles = Position.Array.fromFunc(level.width(), level.height(), pos -> createTile(level.at(pos), pos));
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

	public Collection<Position> positions() {
		return new AbstractCollection<>() {

			@Override
			public int size() {
				return width() * height();
			}

			@Override
			public Iterator<Position> iterator() {
				return new Position.Iterator2D(width(), height());
			}

		};
	}

	public Collection<Tile> tiles() {
		return new AbstractCollection<>() {

			@Override
			public int size() {
				return width() * height();
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

	public Collection<Building> buildings() {
		return buildings(b -> true);
	}

	public Collection<Building> buildings(Team team, Predicate<Building> filter) {
		return buildings(filter.and(b -> team == b.getTeam()));
	}

	public Collection<Building> buildings(Predicate<Building> filter) {
		List<Building> buildings = new ArrayList<>();
		for (Tile tile : tiles())
			if (tile.hasBuilding() && filter.test(tile.getBuilding()))
				buildings.add(tile.getBuilding());
		return buildings;
	}

	public Collection<Unit> units() {
		return units(u -> true);
	}

	public Collection<Unit> units(Team team) {
		return units(u -> team == u.getTeam());
	}

	public Collection<Unit> units(Predicate<? super Unit> filter) {
		List<Unit> units = new ArrayList<>();
		for (Tile tile : tiles())
			if (tile.hasUnit() && filter.test(tile.getUnit()))
				units.add(tile.getUnit());
		return units;
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
