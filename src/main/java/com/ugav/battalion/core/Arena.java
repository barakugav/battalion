package com.ugav.battalion.core;

import java.util.Objects;
import java.util.function.Supplier;

import com.ugav.battalion.core.Building.ConquerEvent;
import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ValuesCache;

@SuppressWarnings("unchecked")
public class Arena {

	private final Cell.Array<Terrain> terrains;
	private final Cell.Array<Unit> units;
	private final Cell.Array<Building> buildings;

	final ValuesCache valuesCache = new ValuesCache();

	public final Event.Notifier<EntityChange> onEntityChange = new Event.Notifier<>();
	public final Event.Notifier<ConquerEvent> onConquer = new Event.Notifier<>();

	private Arena(Level level) {
		int w = level.width(), h = level.height();
		terrains = Cell.Array.fromFunc(w, h, cell -> level.at(cell).terrain);
		units = Cell.Array.fromFunc(w, h, pos -> {
			UnitDesc desc = level.at(pos).unit;
			return desc != null ? Unit.valueOf(this, desc, pos) : null;
		});
		buildings = Cell.Array.fromFunc(w, h, pos -> {
			BuildingDesc desc = level.at(pos).building;
			return desc != null ? Building.valueOf(this, desc, pos) : null;
		});
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
		valuesCache.invalidate();
	}

	void removeUnit(int cell) {
		assert units.at(cell) != null;
		units.set(cell, null);
		valuesCache.invalidate();
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

	private final Supplier<Cell.Bitmap>[] visibleUnitBitmap;
	{
		visibleUnitBitmap = new Supplier[Team.values().length];
		for (Team viewer : Team.values()) {
			visibleUnitBitmap[viewer.ordinal()] = valuesCache
					.newVal(() -> Cell.Bitmap.fromPredicate(width(), height(), pos -> {
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

}
