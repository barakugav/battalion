package com.ugav.battalion.core;

import java.util.Objects;
import java.util.function.Supplier;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.util.Iter;

@SuppressWarnings("unchecked")
public class Arena {

	private final Cell.Array<Terrain> terrains;
	private final Cell.Array<Unit> units;
	private final Cell.Array<Building> buildings;

	public final DataChangeNotifier<EntityChange> onEntityChange = new DataChangeNotifier<>();

	private Arena(Level level) {
		int w = level.width(), h = level.height();
		terrains = Cell.Array.fromFunc(w, h, cell -> level.at(cell).terrain);
		units = Cell.Array.fromFunc(w, h, pos -> createUnit(level.at(pos).unit, pos));
		buildings = Cell.Array.fromFunc(w, h, pos -> createBuilding(level.at(pos).building, pos));
	}

	private Arena(Arena arena) {
		int w = arena.width(), h = arena.height();
		terrains = Cell.Array.fromFunc(w, h, pos -> arena.terrain(pos));
		units = Cell.Array.fromFunc(w, h, pos -> {
			Unit unit = arena.unit(pos);
			return unit != null ? Unit.copyOf(this, unit) : null;
		});
		buildings = Cell.Array.fromFunc(w, h, pos -> {
			Building building = arena.building(pos);
			return building != null ? Building.copyOf(this, building) : null;
		});
	}

	static Arena fromLevel(Level level) {
		return new Arena(level);
	}

	static Arena copyOf(Arena arena) {
		return new Arena(arena);
	}

	public int width() {
		return terrains.width();
	}

	public int height() {
		return terrains.height();
	}

	public Terrain terrain(int cell) {
		return terrains.at(cell);
	}

	public Unit unit(int cell) {
		return units.at(cell);
	}

	public Building building(int cell) {
		return buildings.at(cell);
	}

	void setUnit(int cell, Unit unit) {
		assert units.at(cell) == null;
		units.set(cell, Objects.requireNonNull(unit));
		increaseModCount();
	}

	void removeUnit(int cell) {
		assert units.at(cell) != null;
		units.set(cell, null);
		increaseModCount();
	}

	public boolean isValidCell(int cell) {
		return Cell.isInRect(cell, width() - 1, height() - 1);
	}

	public Iter.Int cells() {
		return new Cell.Iter2D(width(), height());
	}

	public Iter<Building> buildings() {
		return cells().map(this::building).filter(Objects::nonNull);
	}

	public Iter<Unit> units(Team team) {
		return units().filter(u -> team == u.getTeam());
	}

	public Iter<Unit> units() {
		return cells().map(this::unit).filter(Objects::nonNull);
	}

	public Iter<Unit> enemiesSeenBy(Team viewer) {
		return units().filter(u -> u.getTeam() != viewer && isUnitVisible(u.getPos(), viewer));
	}

	private Building createBuilding(BuildingDesc desc, int cell) {
		if (desc == null)
			return null;
		Building building = Building.valueOf(this, desc);
		building.setPos(cell);
		return building;
	}

	private Unit createUnit(UnitDesc desc, int cell) {
		if (desc == null)
			return null;
		Unit unit = Unit.valueOf(this, desc);
		unit.setPos(cell);
		return unit;
	}

	private final Cached<Cell.Bitmap>[] visibleUnitBitmap;
	{
		visibleUnitBitmap = new Cached[Team.values().length];
		for (int i = 0; i < visibleUnitBitmap.length; i++) {
			Team viewer = Team.values()[i];
			visibleUnitBitmap[i] = newCached(() -> Cell.Bitmap.fromPredicate(width(), height(), pos -> {
				Unit unit = unit(pos);
				if (unit == null)
					return false;

				if (!unit.type.invisible || unit.getTeam() == viewer)
					return true;
				for (int n : Cell.neighbors(pos)) {
					if (!isValidCell(n))
						continue;
					Unit neighbor = this.unit(n);
					if (neighbor != null && neighbor.getTeam() == viewer)
						return true;
				}
				return false;
			}));
		}

	}

	private Cell.Bitmap getVisibleUnitBitmap(Team viewer) {
		return visibleUnitBitmap[viewer.ordinal()].get();
	}

	public boolean isUnitVisible(int cell, Team viewer) {
		if (!isValidCell(cell))
			throw new IllegalArgumentException();
		return getVisibleUnitBitmap(viewer).contains(cell);
	}

	private volatile int modCount;

	private void increaseModCount() {
		modCount++;
	}

	<T> Cached<T> newCached(Supplier<? extends T> calc) {
		return new Cached<>(calc);
	}

	class Cached<T> implements Supplier<T> {
		private volatile T val;
		private volatile int cachedModcount;
		private final Supplier<? extends T> calc;

		Cached(Supplier<? extends T> calc) {
			cachedModcount = -1;
			this.calc = Objects.requireNonNull(calc);
		}

		@Override
		public T get() {
			if (modCount != cachedModcount) {
				val = calc.get();
				cachedModcount = modCount;
			}
			return val;
		}

	}

}
