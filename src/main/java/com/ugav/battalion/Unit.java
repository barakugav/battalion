package com.ugav.battalion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

	int getDamge(Unit target) {
		return type.damage;
	}

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

	boolean isAttackValid(Unit target) {
		return target.getTeam() != getTeam() && getAttackableMap().at(target.getPos())
				&& type.attackable.contains(target.type.category);
	}

	enum Category {
		Land, Water, Air
	};

	enum Weapon {
		CloseRange, LongRange
	};

	enum Type {
		Soldier(Category.Land, Weapon.CloseRange, List.of(Category.Land, Category.Water), 50, 22, 3, 1, 1),
		Tank(Category.Land, Weapon.CloseRange, List.of(Category.Land, Category.Water), 70, 35, 6, 1, 1),
		Artillery(Category.Land, Weapon.LongRange, List.of(Category.Land, Category.Water, Category.Air), 70, 35, 3, 3,
				5),

		Ship(Category.Water, Weapon.CloseRange, List.of(Category.Land, Category.Water), 70, 35, 6, 1, 1),

		Airplane(Category.Air, Weapon.CloseRange, List.of(Category.Land, Category.Water, Category.Air), 70, 35, 6, 1,
				1);

		final Category category;
		final Weapon weapon;
		final List<Category> attackable;
		final int health;
		final int damage;
		final int moveLimit;
		final int rangeMin;
		final int rangeMax;

		Type(Category category, Weapon weapon, List<Category> attackable, int health, int damage, int moveLimit,
				int rangeMin, int rangeMax) {
			this.category = category;
			this.weapon = weapon;
			this.attackable = Collections.unmodifiableList(new ArrayList<>(attackable));
			this.health = health;
			this.damage = damage;
			this.moveLimit = moveLimit;
			this.rangeMin = rangeMin;
			this.rangeMax = rangeMax;
		}
	}

	static abstract class UnitCloseRange extends Unit {

		UnitCloseRange(Arena arena, Type type, Team team) {
			super(arena, type, team);
			if (type.weapon != Weapon.CloseRange)
				throw new InternalError();
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
					attackableMap[neighbor.x][neighbor.y] = other.getTeam() != getTeam()
							&& type.attackable.contains(other.type.category);
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

	static abstract class UnitLongRange extends Unit {

		UnitLongRange(Arena arena, Type type, Team team) {
			super(arena, type, team);
			if (type.weapon != Weapon.LongRange)
				throw new InternalError();
		}

		@Override
		Position.Bitmap getAttackableMap() {
			boolean[][] attackableMap = new boolean[arena.getWidth()][arena.getHeight()];
			for (Position pos : arena.positions()) {
				int distance = distance1Norm(getPos(), pos);
				if (!arena.at(pos).hasUnit() || !(type.rangeMin <= distance && distance <= type.rangeMax))
					continue;
				Unit other = arena.at(pos).getUnit();
				attackableMap[pos.x][pos.y] = other.getTeam() != getTeam()
						&& type.attackable.contains(other.type.category);
			}
			return new Position.Bitmap(attackableMap);
		}

		private static int distance1Norm(Position p1, Position p2) {
			return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
		}

		@Override
		Position getMovePositionToAttack(Position target) {
			return getPos();
		}

	}

	static class Soldier extends UnitCloseRange {

		Soldier(Arena arena, Team team) {
			super(arena, Type.Soldier, team);
		}

	}

	static class Tank extends UnitCloseRange {

		Tank(Arena arena, Team team) {
			super(arena, Type.Tank, team);
		}

	}

	static class Artillery extends UnitLongRange {

		Artillery(Arena arena, Team team) {
			super(arena, Type.Artillery, team);
		}

	}

	static class Ship extends UnitCloseRange {

		Ship(Arena arena, Team team) {
			super(arena, Type.Ship, team);
		}

	}

	static class Airplane extends UnitCloseRange {

		Airplane(Arena arena, Team team) {
			super(arena, Type.Airplane, team);
		}

	}

	Position.Bitmap getReachableMap() {
		int width = arena.getWidth(), height = arena.getHeight();

		int[][] passableBitmap = new int[width][height];
		for (int x = 0; x < width; x++)
			Arrays.fill(passableBitmap[x], -1);
		passableBitmap[pos.x][pos.y] = 0;

		int maxMove = type.moveLimit;
		for (int moveLen = 1; moveLen <= maxMove; moveLen++) {
			for (Position pos : arena.positions()) {
				Tile tile = arena.at(pos);
				if (passableBitmap[pos.x][pos.y] >= 0 || !isTerrainPassable(tile.getTerrain())
						|| (tile.hasUnit() && tile.getUnit().getTeam() != getTeam()))
					continue;

				for (Position neighbor : pos.neighbors()) {
					if (arena.isValidPos(neighbor) && passableBitmap[neighbor.x][neighbor.y] == moveLen - 1) {
						passableBitmap[pos.x][pos.y] = moveLen;
						break;
					}
				}
			}
		}

		/* Convert distance map to bitmap */
		boolean[][] reachableMap = new boolean[width][height];
		for (Position pos : arena.positions())
			reachableMap[pos.x][pos.y] = passableBitmap[pos.x][pos.y] > 0 && !arena.at(pos).hasUnit();
		return new Position.Bitmap(reachableMap);
	}

	abstract Position.Bitmap getAttackableMap();

	abstract Position getMovePositionToAttack(Position target);

	@Override
	public String toString() {
		return (getTeam() == Team.Red ? "R" : "B") + type;
	}

}
