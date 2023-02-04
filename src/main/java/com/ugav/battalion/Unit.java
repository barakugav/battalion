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

	boolean isMoveValid(List<Position> path) {
		if (path.isEmpty() || path.size() > type.moveLimit)
			return false;
		int[][] distanceMap = calcDistanceMap();
		Position prev = getPos();
		for (Position pos : path) {
			if (!prev.neighbors().contains(pos) || distanceMap[pos.x][pos.y] < 0)
				return false;
			prev = pos;
		}
		return !arena.at(path.get(path.size() - 1)).hasUnit();
	}

	boolean isAttackValid(Unit target) {
		return target.getTeam() != getTeam() && getAttackableMap().contains(target.getPos())
				&& type.canAttack.contains(target.type.category);
	}

	enum Category {
		Land, Water, Air
	};

	enum Weapon {
		CloseRange, LongRange
	};

	enum Type {
		Soldier(Category.Land, Weapon.CloseRange,
				List.of(Terrain.Category.Land, Terrain.Category.Mountain, Terrain.Category.Shore),
				List.of(Category.Land, Category.Water), 50, 22, 3, 1, 1),
		Tank(Category.Land, Weapon.CloseRange, List.of(Terrain.Category.Land, Terrain.Category.Shore),
				List.of(Category.Land, Category.Water), 70, 35, 6, 1, 1),
		Artillery(Category.Land, Weapon.LongRange, List.of(Terrain.Category.Land, Terrain.Category.Shore),
				List.of(Category.Land, Category.Water, Category.Air), 70, 35, 3, 3, 5),
		Turrent(Category.Land, Weapon.LongRange, List.of(Terrain.Category.Land),
				List.of(Category.Land, Category.Water, Category.Air), 100, 30, 0, 2, 7),

		Ship(Category.Water, Weapon.CloseRange, List.of(Terrain.Category.Water), List.of(Category.Land, Category.Water),
				70, 35, 6, 1, 1),

		Airplane(
				Category.Air, Weapon.CloseRange, List.of(Terrain.Category.Land, Terrain.Category.Mountain,
						Terrain.Category.Shore, Terrain.Category.Water),
				List.of(Category.Land, Category.Water, Category.Air), 70, 35, 6, 1, 1);

		final Category category;
		final Weapon weapon;
		final List<Terrain.Category> canStand;
		final List<Category> canAttack;
		final int health;
		final int damage;
		final int moveLimit;
		final int rangeMin;
		final int rangeMax;

		Type(Category category, Weapon weapon, List<Terrain.Category> canStand, List<Category> canAttack, int health,
				int damage, int moveLimit, int rangeMin, int rangeMax) {
			this.category = category;
			this.weapon = weapon;
			this.canStand = Collections.unmodifiableList(new ArrayList<>(canStand));
			this.canAttack = Collections.unmodifiableList(new ArrayList<>(canAttack));
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
							&& type.canAttack.contains(other.type.category);
				}
			};

			checkNeighbors.accept(getPos());
			for (Position pos : reachableMap)
				checkNeighbors.accept(pos);

			return new Position.Bitmap(attackableMap);
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
						&& type.canAttack.contains(other.type.category);
			}
			return new Position.Bitmap(attackableMap);
		}

		private static int distance1Norm(Position p1, Position p2) {
			return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
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

	static class Turrent extends UnitLongRange {

		Turrent(Arena arena, Team team) {
			super(arena, Type.Turrent, team);
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
		Position.Bitmap passableMap = getPassableMap();

		/* Convert distance map to bitmap */
		boolean[][] reachableMap = new boolean[arena.getWidth()][arena.getHeight()];
		for (Position pos : arena.positions())
			reachableMap[pos.x][pos.y] = passableMap.contains(pos)
					&& (!arena.at(pos).hasUnit() || arena.at(pos).getUnit() == this);
		return new Position.Bitmap(reachableMap);
	}

	Position.Bitmap getPassableMap() {
		int[][] distanceMap = calcDistanceMap();

		/* Convert distance map to bitmap */
		boolean[][] reachableMap = new boolean[arena.getWidth()][arena.getHeight()];
		for (Position pos : arena.positions())
			reachableMap[pos.x][pos.y] = distanceMap[pos.x][pos.y] >= 0;
		return new Position.Bitmap(reachableMap);
	}

	private int[][] calcDistanceMap() {
		int width = arena.getWidth(), height = arena.getHeight();

		int[][] distanceMap = new int[width][height];
		for (int x = 0; x < width; x++)
			Arrays.fill(distanceMap[x], -1);
		distanceMap[pos.x][pos.y] = 0;

		int maxMove = type.moveLimit;
		for (int moveLen = 1; moveLen <= maxMove; moveLen++) {
			for (Position pos : arena.positions()) {
				Tile tile = arena.at(pos);
				if (distanceMap[pos.x][pos.y] >= 0 || !isTerrainPassable(tile.getTerrain())
						|| (tile.hasUnit() && tile.getUnit().getTeam() != getTeam()))
					continue;

				for (Position neighbor : pos.neighbors()) {
					if (arena.isValidPos(neighbor) && distanceMap[neighbor.x][neighbor.y] == moveLen - 1) {
						distanceMap[pos.x][pos.y] = moveLen;
						break;
					}
				}
			}
		}
		return distanceMap;
	}

	List<Position> calcPath(Position destination) {
		int[][] distanceMap = calcDistanceMap();
		if (distanceMap[destination.x][destination.y] < 0)
			throw new IllegalArgumentException("Can't reach " + destination);
		List<Position> path = new ArrayList<>(distanceMap[destination.x][destination.y]);
		for (Position pos = destination; !pos.equals(getPos());) {
			path.add(pos);
			for (Position next : pos.neighbors()) {
				if (arena.isValidPos(next) && distanceMap[next.x][next.y] == distanceMap[pos.x][pos.y] - 1) {
					pos = next;
					break;
				}
			}
		}
		Collections.reverse(path);
		return path;
	}

	abstract Position.Bitmap getAttackableMap();

	@Override
	public String toString() {
		return (getTeam() == Team.Red ? "R" : "B") + type;
	}

}
