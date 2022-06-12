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
	final void setTeam(Team team) {
		throw new UnsupportedOperationException();
	}

	int getHealth() {
		return health;
	}

	void setHealth(int health) {
		this.health = health;
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

	abstract int getDamge(Unit target);

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
		return getReachableMap().at(target);
	}

	boolean isAttackValid(Position target) {
		return getAttackableMap().at(target);
	}

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
		Position.Bitmap getAttackableMap() {
			Arena arena = getArena();
			Position.Bitmap reachableMap = getReachableMap();
			boolean[][] attackableMap = new boolean[arena.getWidth()][arena.getHeight()];

			/* Touchable map */
			for (Position pos : arena.positions()) {
				if (!arena.at(pos).hasUnit())
					continue;
				Unit other = arena.at(pos).getUnit();
				if (other.getTeam() == getTeam())
					continue;
				for (Position neighbor : pos.neighbors()) {
					if (arena.isValidPos(neighbor) && reachableMap.at(neighbor)) {
						attackableMap[pos.x][pos.y] = true;
						break;
					}
				}
			}

			return new Position.Bitmap(attackableMap);
		}

		@Override
		Position getMovePositionToAttack(Position target) {
			Position.Bitmap reachableMap = getReachableMap();
			for (Position neighbor : target.neighbors())
				if (reachableMap.at(neighbor))
					return neighbor;
			return null;
		}

	}

	static class Soldier extends CloseRangeUnitAbstract {

		Soldier(Team team) {
			super(Type.Soldier, team);
		}

		@Override
		int getDamge(Unit target) {
			return type.damage;
		}

	}

	static class Tank extends CloseRangeUnitAbstract {

		Tank(Team team) {
			super(Type.Tank, team);
		}

		@Override
		int getDamge(Unit target) {
			return type.damage;
		}

	}

	Position.Bitmap getReachableMap() {
		Arena arena = getArena();
		int width = arena.getWidth(), height = arena.getHeight();
		boolean[][] reachableMap = new boolean[width][height];

		int[][] reachableMap0 = new int[width][height];
		for (int x = 0; x < width; x++)
			Arrays.fill(reachableMap0[x], -1);
		reachableMap0[pos.x][pos.y] = 0;

		int maxMove = type.moveLimit;
		for (int moveLen = 1; moveLen <= maxMove; moveLen++) {
			for (Position pos : arena.positions()) {
				/* Already can move here */
				if (reachableMap0[pos.x][pos.y] >= 0)
					continue;
				/* Other unit in the way */
				if (arena.at(pos).hasUnit())
					continue;
				if (!isTerrainPassable(arena.at(pos).getTerrain()))
					continue;
				/* Check if we reached any near tiles last moveLen */
				for (Position neighbor : pos.neighbors()) {
					if (arena.isValidPos(neighbor) && reachableMap0[neighbor.x][neighbor.y] == moveLen - 1) {
						reachableMap0[pos.x][pos.y] = moveLen;
						break;
					}
				}
			}
		}
		/* Convert distance map to boolean map */
		for (Position pos : arena.positions())
			reachableMap[pos.x][pos.y] = reachableMap0[pos.x][pos.y] > 0;

		return new Position.Bitmap(reachableMap);
	}

	abstract Position.Bitmap getAttackableMap();

	abstract Position getMovePositionToAttack(Position target);

	@Override
	public String toString() {
		return (getTeam() == Team.Red ? "R" : "B") + type;
	}

}
