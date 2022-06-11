package com.ugav.battalion;

class Level {

	private final int xLen;
	private final int yLen;
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
		this.xLen = tiles.length;
		this.yLen = tiles[0].length;
		this.tiles = new TileDesc[xLen][yLen];
		for (int x = 0; x < xLen; x++)
			for (int y = 0; y < yLen; y++)
				this.tiles[x][y] = tiles[x][y];
	}

	int getXLen() {
		return xLen;
	}

	int getYLen() {
		return yLen;
	}

	TileDesc tileDesc(int x, int y) {
		return tiles[x][y];
	}

}
