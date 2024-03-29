package com.bugav.battalion.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.bugav.battalion.core.Unit.Type;

public class Level {

	private final Cell.Array<Terrain> terrains;
	private final Cell.Array<BuildingDesc> buildings;
	private final Cell.Array<UnitDesc> units;
	private final Map<Team, Integer> startingMoney;

	Level(Cell.Array<Terrain> terrains, Cell.Array<BuildingDesc> buildings, Cell.Array<UnitDesc> units,
			Map<Team, Integer> startingMoney) {
		this.terrains = Cell.Array.copyOf(terrains);
		this.buildings = Cell.Array.copyOf(buildings);
		this.units = Cell.Array.copyOf(units);
		this.startingMoney = Collections.unmodifiableMap(new HashMap<>(startingMoney));
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

	public BuildingDesc building(int cell) {
		return buildings.at(cell);
	}

	public UnitDesc unit(int cell) {
		return units.at(cell);
	}

	public int getStartingMoney(Team team) {
		Integer m = startingMoney.get(team);
		return m != null ? m.intValue() : 0;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof Level))
			return false;
		Level o = (Level) other;

		if (width() != o.width() || height() != o.height())
			return false;

		return terrains.equals(o.terrains) && buildings.equals(o.buildings) && units.equals(o.units)
				&& startingMoney.equals(o.startingMoney);
	}

	@Override
	public int hashCode() {
		return Objects.hash(terrains, buildings, units, startingMoney);
	}

	public static class BuildingDesc implements IBuilding {
		public final Building.Type type;
		public final Team team;
		public final boolean active;

		private BuildingDesc(Building.Type type, Team team, boolean active) {
			this.type = Objects.requireNonNull(type);
			this.team = team;
			this.active = active;
		}

		public static BuildingDesc of(Building.Type type, Team team) {
			return of(type, team, false);
		}

		public static BuildingDesc of(Building.Type type, Team team, boolean active) {
			return new BuildingDesc(type, team, active);
		}

		public static BuildingDesc copyOf(BuildingDesc desc) {
			return new BuildingDesc(desc.type, desc.team, desc.active);
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof BuildingDesc))
				return false;
			BuildingDesc other = (BuildingDesc) o;

			return type.equals(other.type) && team.equals(other.team);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, team);
		}

		@Override
		public String toString() {
			return "(" + type + ", " + team + ")";
		}

		@Override
		public Building.Type getType() {
			return type;
		}

		@Override
		public Team getTeam() {
			return team;
		}

	}

	public static class UnitDesc implements IUnit {
		public final Unit.Type type;
		public final Team team;
		public final int health;
		public final boolean active;
		public final boolean repairing;
		private final UnitDesc transportedUnit;

		private UnitDesc(Unit.Type type, Team team, UnitDesc transportedUnit, int health, boolean active,
				boolean repairing) {
			this.type = Objects.requireNonNull(type);
			this.team = Objects.requireNonNull(team);
			if (!(0 < health && health <= type.health))
				throw new IllegalArgumentException();
			this.health = health;
			this.active = active;
			this.repairing = repairing;

			if (type.transportUnits ^ (transportedUnit != null && transportedUnit.type.category == Unit.Category.Land))
				throw new IllegalArgumentException();
			this.transportedUnit = transportedUnit;
		}

		public static UnitDesc of(Unit.Type type, Team team) {
			return of(type, team, type.health, true, false);
		}

		public static UnitDesc of(Unit.Type type, Team team, int health, boolean active, boolean repairing) {
			if (type.transportUnits)
				throw new IllegalArgumentException();
			return new UnitDesc(type, team, null, health, active, repairing);
		}

		public static UnitDesc transporter(Unit.Type type, UnitDesc unit) {
			return transporter(type, unit, type.health, true, false);
		}

		public static UnitDesc transporter(Unit.Type type, UnitDesc unit, int health, boolean active,
				boolean repairing) {
			if (!type.transportUnits || unit.type.category != Unit.Category.Land)
				throw new IllegalArgumentException();
			return new UnitDesc(type, unit.team, unit, health, active, repairing);
		}

		public static UnitDesc copyOf(UnitDesc desc) {
			return new UnitDesc(desc.type, desc.team, desc.transportedUnit, desc.health, desc.active, desc.repairing);
		}

		@Override
		public UnitDesc getTransportedUnit() {
			return type.transportUnits ? Objects.requireNonNull(transportedUnit) : null;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof UnitDesc))
				return false;
			UnitDesc other = (UnitDesc) o;

			return type.equals(other.type) && team.equals(other.team);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, team);
		}

		@Override
		public String toString() {
			return "(" + type + ", " + team + ")";
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public Team getTeam() {
			return team;
		}

		@Override
		public int getHealth() {
			return health;
		}

	}

}
