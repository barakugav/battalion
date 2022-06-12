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
		return getMovableMap()[target.x][target.y];
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
			int xLen = arena.getXLen(), yLen = arena.getYLen();
			boolean[][] moveableMap = getMovableMap();
			boolean[][] attackableMap = new boolean[xLen][yLen];

			/* Touchable map */
			for (Position pos : Utils.iterable(new Position.Iterator2D(xLen, yLen))) {
				for (Position neighbor : pos.neighbors()) {
					if (arena.isValidPos(neighbor) && moveableMap[neighbor.x][neighbor.y]) {
						attackableMap[pos.x][pos.y] = true;
						break;
					}
				}
			}

			return attackableMap[target.x][target.y];
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
		int xLen = arena.getXLen(), yLen = arena.getYLen();
		boolean[][] moveableMap = new boolean[xLen][yLen];

		int[][] moveableMap0 = new int[xLen][yLen];
		for (int x = 0; x < xLen; x++)
			Arrays.fill(moveableMap0[x], -1);
		moveableMap0[pos.x][pos.y] = 0;

		int maxMove = type.moveLimit;
		for (int moveLen = 1; moveLen <= maxMove; moveLen++) {
			for (Position pos : arena.positions()) {
				/* Already can move here */
				if (moveableMap0[pos.x][pos.y] >= 0)
					continue;
				/* Other unit in the way */
				if (arena.at(pos).hasUnit())
					continue;
				if (!isTerrainPassable(arena.at(pos).getTerrain()))
					continue;
				/* Check if we reached any near tiles last moveLen */
				for (Position neighbor : pos.neighbors()) {
					if (arena.isValidPos(neighbor) && moveableMap0[neighbor.x][neighbor.y] == moveLen - 1) {
						moveableMap0[pos.x][pos.y] = moveLen;
						break;
					}
				}
			}
		}
		/* Convert distance map to boolean map */
		for (Position pos : arena.positions())
			moveableMap[pos.x][pos.y] = moveableMap0[pos.x][pos.y] > 0;

		return moveableMap;
	}

}
