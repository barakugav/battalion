package com.ugav.battalion;

class Level {

	private final int width;
	private final int height;
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

}
