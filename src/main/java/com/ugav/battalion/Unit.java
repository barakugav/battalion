package com.ugav.battalion;

import java.util.Arrays;

abstract class Unit extends Entity {

	final Type type;
	private int health;
	private Position pos;
	private Arena arena;

	Unit(Type type, Team team) {
		super(team);
		if (getTeam() == Team.None)
			throw new IllegalArgumentException();
		this.type = type;
		health = type.health;
	}

	@Override
	public final void setTeam(Team team) {
		throw new UnsupportedOperationException();
	}

	int getHealth() {
		return health;
	}

	void setPos(Position pos) {
		this.pos = pos;
	}

	Position getPos() {
		return pos;
	}

	void setArena(Arena arena) {
		this.arena = arena;
	}

	Arena getArena() {
		return arena;
	}

	abstract int getDamge();

	boolean isTerrainPassable(Terrain terrain) {
		switch (terrain.type.category) {
		case Land:
			return type.category != Category.Water;
		case Mountain:
			return type == Type.Soldier;
		case Water:
			return type.category != Category.Land;
		default:
			throw new InternalError();
		}
	}

	boolean isMoveValid(Position target) {
		return getMovableMap()[target.row][target.col];
	}

	abstract boolean isAttackValid(Position target);

	enum Category {
		Land, Water, Air
	};

	enum Type {
		Soldier(Category.Land, 50, 22, 3, 1), Tank(Category.Land, 70, 35, 6, 1);

		final Category category;
		final int health;
		final int damage;
		final int moveLimit;
		final int range;

		Type(Category category, int health, int damage, int moveLimit, int range) {
			this.category = category;
			this.health = health;
			this.damage = damage;
			this.moveLimit = moveLimit;
			this.range = range;
		}
	}

	static abstract class CloseRangeUnitAbstract extends Unit {
		CloseRangeUnitAbstract(Type type, Team team) {
			super(type, team);
		}

		@Override
		public boolean isAttackValid(Position target) {
			Arena arena = getArena();
			int rows = arena.getrows(), cols = arena.getcols();
			boolean[][] moveableMap = getMovableMap();
			boolean[][] attackableMap = new boolean[rows][cols];

			/* Touchable map */
			for (Position pos : Utils.iterable(new Position.Iterator2D(rows, cols))) {
				for (Position neighbor : pos.neighbors()) {
					if (arena.isValidPos(neighbor) && moveableMap[neighbor.row][neighbor.col]) {
						attackableMap[pos.row][pos.col] = true;
						break;
					}
				}
			}

			return attackableMap[target.row][target.col];
		}

	}

	static class Soldier extends CloseRangeUnitAbstract {

		Soldier(Team team) {
			super(Type.Soldier, team);
		}

		@Override
		public int getDamge() {
			return type.damage;
		}

	}

	static class Tank extends CloseRangeUnitAbstract {

		Tank(Team team) {
			super(Type.Tank, team);
		}

		@Override
		public int getDamge() {
			return type.damage;
		}

	}

	boolean[][] getMovableMap() {
		Arena arena = getArena();
		int rows = arena.getrows(), cols = arena.getcols();
		boolean[][] moveableMap = new boolean[rows][cols];

		int[][] moveableMap0 = new int[rows][cols];
		for (int r = 0; r < rows; r++)
			Arrays.fill(moveableMap0[r], -1);
		moveableMap0[pos.row][pos.col] = 0;

		int maxMove = type.moveLimit;
		for (int moveLen = 1; moveLen <= maxMove; moveLen++) {
			for (Position pos : arena.positions()) {
				/* Already can move here */
				if (moveableMap0[pos.row][pos.col] >= 0)
					continue;
				/* Other unit in the way */
				if (arena.at(pos).hasUnit())
					continue;
				if (!isTerrainPassable(arena.at(pos).getTerrain()))
					continue;
				/* Check if we reached any near tiles last moveLen */
				for (Position neighbor : pos.neighbors()) {
					if (arena.isValidPos(neighbor) && moveableMap0[neighbor.row][neighbor.col] == moveLen - 1) {
						moveableMap0[pos.row][pos.col] = moveLen;
						break;
					}
				}
			}
		}
		/* Convert distance map to boolean map */
		for (Position pos : arena.positions())
			moveableMap[pos.row][pos.col] = moveableMap0[pos.row][pos.col] > 0;

		return moveableMap;
	}

}
