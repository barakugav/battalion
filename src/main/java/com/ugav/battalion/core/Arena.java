package com.ugav.battalion.core;

import java.util.Objects;
import java.util.function.Supplier;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.UnitDesc;

@SuppressWarnings("unchecked")
public class Arena {

	private final Cell.Array<Terrain> terrains;
	private final Cell.Array<Unit> units;
	private final Cell.Array<Building> buildings;

	public final DataChangeNotifier<EntityChange> onEntityChange = new DataChangeNotifier<>();

	private Arena(Level level) {
		int w = level.width(), h = level.height();
		terrains = Cell.Array.fromFunc(w, h, pos -> level.at(pos).terrain);
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

	public Terrain terrain(Cell pos) {
		return terrains.at(pos);
	}

	public Unit unit(Cell pos) {
		return units.at(pos);
	}

	public Building building(Cell pos) {
		return buildings.at(pos);
	}

	void setUnit(Cell pos, Unit unit) {
		assert units.at(pos) == null;
		units.set(pos, Objects.requireNonNull(unit));
		increaseModCount();
	}

	void removeUnit(Cell pos) {
		assert units.at(pos) != null;
		units.set(pos, null);
		increaseModCount();
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

	public Iter<Building> buildings() {
		return positions().iterator().map(this::building).filter(Objects::nonNull);
	}

	public Iter<Unit> units(Team team) {
		return units().filter(u -> team == u.getTeam());
	}

	public Iter<Unit> units() {
		return positions().iterator().map(this::unit).filter(Objects::nonNull);
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
				for (Cell n : pos.neighbors()) {
					if (!isValidPos(n))
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

	public boolean isUnitVisible(Cell pos, Team viewer) {
		if (!isValidPos(pos))
			throw new IllegalArgumentException();
		return getVisibleUnitBitmap(viewer).contains(pos);
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
