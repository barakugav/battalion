package com.ugav.battalion;

class Level {

	private final int rows;
	private final int cols;
	private final TileDesc[][] tiles;

	static class TileDesc {
		final Terrain terrain;
		final BuildingDesc buiding;
		final UnitDesc unit;

		TileDesc(Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
			this.terrain = terrain;
			this.buiding = buiding;
			this.unit = unit;
		}

		static TileDesc of(Terrain terrain, BuildingDesc buiding, UnitDesc unit) {
			return new TileDesc(terrain, buiding, unit);
		}
	}

	static class BuildingDesc {
		final Building.Type type;
		final Team team;

		BuildingDesc(Building.Type type, Team team) {
			this.type = type;
			this.team = team;
		}

		static BuildingDesc of(Building.Type type, Team team) {
			return new BuildingDesc(type, team);
		}
	}

	static class UnitDesc {
		final Unit.Type type;
		final Team team;

		UnitDesc(Unit.Type type, Team team) {
			this.type = type;
			this.team = team;
		}

		static UnitDesc of(Unit.Type type, Team team) {
			return new UnitDesc(type, team);
		}
	}

	Level(TileDesc[][] tiles) {
		this.rows = tiles.length;
		this.cols = tiles[0].length;
		this.tiles = new TileDesc[rows][cols];
		for (Position pos : Utils.iterable(new Position.Iterator2D(rows, cols)))
			this.tiles[pos.row][pos.col] = tiles[pos.row][pos.col];
	}

	int getrows() {
		return rows;
	}

	int getcols() {
		return cols;
	}

	TileDesc tileDesc(Position pos) {
		return tiles[pos.row][pos.col];
	}

}
