package com.ugav.battalion.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.ugav.battalion.core.Unit.Type;

public class Level {

	private final Position.Array<TileDesc> tiles;
	private final Map<Team, Integer> startingMoney;

	public static final int MINIMUM_WIDTH = 10;
	public static final int MINIMUM_HEIGHT = 10;

	Level(Position.Array<TileDesc> tiles, Map<Team, Integer> startingMoney) {
		int width = tiles.width();
		int height = tiles.height();
		if (width < MINIMUM_WIDTH || height < MINIMUM_HEIGHT)
			throw new IllegalArgumentException("illegal size: " + width + " " + height);
		this.tiles = Position.Array.fromFunc(width, height, tiles);
		this.startingMoney = Collections.unmodifiableMap(new HashMap<>(startingMoney));
	}

	public int width() {
		return tiles.width();
	}

	public int height() {
		return tiles.height();
	}

	public TileDesc at(Position pos) {
		return tiles.at(pos);
	}

	public int getStartingMoney(Team team) {
		Integer m = startingMoney.get(team);
		return m != null ? m.intValue() : 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Level))
			return false;
		Level other = (Level) o;

		if (width() != other.width() || height() != other.height())
			return false;
		for (Position pos : Position.Iterator2D.of(width(), height()).forEach())
			if (!Objects.equals(at(pos), other.at(pos)))
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		for (Position pos : Position.Iterator2D.of(width(), height()).forEach())
			hash = 31 * hash + Objects.hashCode(at(pos));
		return hash;
	}

	public static class TileDesc {
		public final Terrain terrain;
		public final BuildingDesc building;
		public final UnitDesc unit;

		TileDesc(Terrain terrain, BuildingDesc building, UnitDesc unit) {
			this.terrain = Objects.requireNonNull(terrain);
			this.building = building;
			this.unit = unit;
		}

		public static TileDesc of(Terrain terrain, BuildingDesc building, UnitDesc unit) {
			return new TileDesc(terrain, building, unit);
		}

		public boolean hasUnit() {
			return unit != null;
		}

		public UnitDesc getUnit() {
			return unit;
		}

		public boolean hasBuilding() {
			return building != null;
		}

		public BuildingDesc getBuilding() {
			return building;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof TileDesc))
				return false;
			TileDesc other = (TileDesc) o;

			return Objects.equals(terrain, other.terrain) && Objects.equals(building, other.building)
					&& Objects.equals(unit, other.unit);
		}

		@Override
		public int hashCode() {
			return Objects.hash(terrain, building, unit);
		}

		@Override
		public String toString() {
			return "(" + terrain + ", " + building + ", " + unit + ")";
		}
	}

	public static class BuildingDesc implements IBuilding {
		public final Building.Type type;
		public final Team team;

		BuildingDesc(Building.Type type, Team team) {
			this.type = Objects.requireNonNull(type);
			this.team = Objects.requireNonNull(team);
		}

		public static BuildingDesc of(Building.Type type, Team team) {
			return new BuildingDesc(type, team);
		}

		public static BuildingDesc copyOf(BuildingDesc desc) {
			return new BuildingDesc(desc.type, desc.team);
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
		private final UnitDesc transportedUnit;

		private UnitDesc(Unit.Type type, Team team, UnitDesc transportedUnit) {
			this.type = Objects.requireNonNull(type);
			this.team = Objects.requireNonNull(team);

			if (type.transportUnits ^ (transportedUnit != null && transportedUnit.type.category == Unit.Category.Land))
				throw new IllegalArgumentException();
			this.transportedUnit = transportedUnit;
		}

		public static UnitDesc of(Unit.Type type, Team team) {
			if (type.transportUnits)
				throw new IllegalArgumentException();
			return new UnitDesc(type, team, null);
		}

		public static UnitDesc transporter(Unit.Type type, UnitDesc unit) {
			if (!type.transportUnits || unit.type.category != Unit.Category.Land)
				throw new IllegalArgumentException();
			return new UnitDesc(type, unit.team, unit);
		}

		public static UnitDesc copyOf(UnitDesc desc) {
			return new UnitDesc(desc.type, desc.team, desc.transportedUnit);
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

	}

}
