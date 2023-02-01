package com.ugav.battalion;

import java.util.Objects;

class Level {

	private final int width;
	private final int height;
	private final TileDesc[][] tiles;

	static class TileDesc {
		final Terrain terrain;
		final BuildingDesc buiding;
		final UnitDesc unit;

		TileDesc(Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
			this.terrain = Objects.requireNonNull(terrain);
			this.buiding = buiding;
			this.unit = unit;
		}

		static TileDesc of(Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
			return new TileDesc(terrain, buiding, unit);
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof TileDesc))
				return false;
			TileDesc other = (TileDesc) o;

			return Objects.equals(terrain, other.terrain) && Objects.equals(buiding, other.buiding)
					&& Objects.equals(unit, other.unit);
		}

		@Override
		public int hashCode() {
			return Objects.hash(terrain, buiding, unit);
		}
	}

	static class BuildingDesc {
		final Building.Type type;
		final Team team;

		BuildingDesc(Building.Type type, Team team) {
			this.type = Objects.requireNonNull(type);
			this.team = Objects.requireNonNull(team);
		}

		static BuildingDesc of(Building.Type type, Team team) {
			return new BuildingDesc(type, team);
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
	}

	static class UnitDesc {
		final Unit.Type type;
		final Team team;

		UnitDesc(Unit.Type type, Team team) {
			this.type = Objects.requireNonNull(type);
			this.team = Objects.requireNonNull(team);
		}

		static UnitDesc of(Unit.Type type, Team team) {
			return new UnitDesc(type, team);
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
	}

	Level(TileDesc[][] tiles) {
		this.width = tiles.length;
		this.height = tiles[0].length;
		this.tiles = new TileDesc[width][height];
		for (Position pos : Utils.iterable(new Position.Iterator2D(width, height)))
			this.tiles[pos.x][pos.y] = tiles[pos.x][pos.y];
	}

	int getWidth() {
		return width;
	}

	int getHeight() {
		return height;
	}

	TileDesc tileDesc(Position pos) {
		return tiles[pos.x][pos.y];
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
			if (!Objects.equals(tileDesc(pos), other.tileDesc(pos)))
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		for (Position pos : Utils.iterable(new Position.Iterator2D(width, height)))
			hash = 31 * hash + Objects.hashCode(tileDesc(pos));
		return hash;
	}

}
