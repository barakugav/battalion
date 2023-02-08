package com.ugav.battalion.core;

import java.util.Objects;

import com.ugav.battalion.Utils;

public class Level {

	private final int width;
	private final int height;
	private final TileDesc[][] tiles;

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

	public static class BuildingDesc {
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

	}

	public static class UnitDesc {
		public final Unit.Type type;
		public final Team team;

		UnitDesc(Unit.Type type, Team team) {
			this.type = Objects.requireNonNull(type);
			this.team = Objects.requireNonNull(team);
		}

		public static UnitDesc of(Unit.Type type, Team team) {
			return new UnitDesc(type, team);
		}

		public static UnitDesc copyOf(UnitDesc desc) {
			return new UnitDesc(desc.type, desc.team);
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
	}

	public static final int MINIMUM_WIDTH = 10;
	public static final int MINIMUM_HEIGHT = 10;

	Level(TileDesc[][] tiles) {
		this.width = tiles.length;
		this.height = tiles[0].length;
		if (width < MINIMUM_WIDTH || height < MINIMUM_HEIGHT)
			throw new IllegalArgumentException("illegal size: " + width + " " + height);
		this.tiles = new TileDesc[width][height];
		for (Position pos : Utils.iterable(new Position.Iterator2D(width, height)))
			this.tiles[pos.xInt()][pos.yInt()] = Objects.requireNonNull(tiles[pos.xInt()][pos.yInt()]);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public TileDesc at(Position pos) {
		return tiles[pos.xInt()][pos.yInt()];
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Level))
			return false;
		Level other = (Level) o;

		if (width != other.width || height != other.height)
			return false;
		for (Position pos : Utils.iterable(new Position.Iterator2D(width, height)))
			if (!Objects.equals(at(pos), other.at(pos)))
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		for (Position pos : Utils.iterable(new Position.Iterator2D(width, height)))
			hash = 31 * hash + Objects.hashCode(at(pos));
		return hash;
	}

}
