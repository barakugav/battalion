package com.ugav.battalion;

import java.util.Arrays;
import java.util.function.Consumer;

abstract class Unit extends Entity {

	final Type type;
	final Arena arena;
	private Position pos;
	private int health;

	Unit(Arena arena, Type type, Team team) {
		super(team);
		this.arena = arena;
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
		onChange.notify(new DataEvent(this));
	}

	boolean isDead() {
		return health <= 0;
	}

	void setPos(Position pos) {
		this.pos = pos;
		onChange.notify(new DataEvent(this));
	}

	Position getPos() {
		return pos;
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
		Soldier(Category.Land, 50, 22, 3, 1), Tank(Category.Land, 70, 35, 6, 1), Tank2(Category.Land, 70, 35, 6, 1);

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
		CloseRangeUnitAbstract(Arena arena, Type type, Team team) {
			super(arena, type, team);
		}

		@Override
		Position.Bitmap getAttackableMap() {
			Position.Bitmap reachableMap = getReachableMap();
			boolean[][] attackableMap = new boolean[arena.getWidth()][arena.getHeight()];

			Consumer<Position> checkNeighbors = pos -> {
				for (Position neighbor : pos.neighbors()) {
					if (!arena.isValidPos(neighbor) || !arena.at(neighbor).hasUnit())
						continue;
					Unit other = arena.at(neighbor).getUnit();
					if (other.getTeam() == getTeam())
						continue;
					attackableMap[neighbor.x][neighbor.y] = true;
				}
			};

			checkNeighbors.accept(getPos());
			for (Position pos : reachableMap)
				checkNeighbors.accept(pos);

			return new Position.Bitmap(attackableMap);
		}

		@Override
		Position getMovePositionToAttack(Position target) {
			Position.Bitmap reachableMap = getReachableMap();
			for (Position neighbor : target.neighbors())
				if (neighbor.equals(getPos()) || reachableMap.at(neighbor))
					return neighbor;
			return null;
		}

	}

	static class Soldier extends CloseRangeUnitAbstract {

		Soldier(Arena arena, Team team) {
			super(arena, Type.Soldier, team);
		}

		@Override
		int getDamge(Unit target) {
			return type.damage;
		}

	}

	static class Tank extends CloseRangeUnitAbstract {

		Tank(Arena arena, Team team) {
			super(arena, Type.Tank, team);
		}

		@Override
		int getDamge(Unit target) {
			return type.damage;
		}

	}

	Position.Bitmap getReachableMap() {
		int width = arena.getWidth(), height = arena.getHeight();

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

		/* Convert distance map to bitmap */
		boolean[][] reachableMap = new boolean[width][height];
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
