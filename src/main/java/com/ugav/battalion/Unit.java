package com.ugav.battalion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
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
	}

	enum Weapon {
		CloseRange, LongRange
	}

	private static class TypeBuilder {
		final Set<Terrain.Category> canStand = EnumSet.noneOf(Terrain.Category.class);
		final Set<Category> canAttack = EnumSet.noneOf(Category.class);
		boolean canConquer = false;

		void canStand(Terrain.Category... categories) {
			canStand.addAll(List.of(categories));
		}

		void canAttack(Category... categories) {
			canAttack.addAll(List.of(categories));
		}

	}

	private enum Tech {
		StandOnLandFlat(TypeBuilder::canStand, Terrain.Category.FlatLand, Terrain.Category.Road,
				Terrain.Category.BridgeLow, Terrain.Category.BridgeHigh),

		StandOnLandRough(TypeBuilder::canStand, Terrain.Category.FlatLand, Terrain.Category.RoughLand,
				Terrain.Category.Shore, Terrain.Category.Road, Terrain.Category.BridgeLow, Terrain.Category.BridgeHigh),

		StandOnLandExtreme(TypeBuilder::canStand, Terrain.Category.FlatLand, Terrain.Category.RoughLand,
				Terrain.Category.Shore, Terrain.Category.ExtremeLand, Terrain.Category.Road, Terrain.Category.BridgeLow,
				Terrain.Category.BridgeHigh),

		StandOnWater(TypeBuilder::canStand, Terrain.Category.Water, Terrain.Category.BridgeHigh),

		StandOnWaterDeep(TypeBuilder::canStand, Terrain.Category.Water, Terrain.Category.BridgeLow,
				Terrain.Category.BridgeHigh),

		StandOnAny(TypeBuilder::canStand, Terrain.Category.values()),

		AttLand(TypeBuilder::canAttack, Unit.Category.Land),

		AttWater(TypeBuilder::canAttack, Unit.Category.Water),

		AttAir(TypeBuilder::canAttack, Unit.Category.Air),

		AttAny(TypeBuilder::canAttack, Unit.Category.values()),

		Conquerer(t -> t.canConquer = true);

		final Consumer<TypeBuilder> op;

		Tech(Consumer<TypeBuilder> op) {
			this.op = Objects.requireNonNull(op);
		}

		@SuppressWarnings("unchecked")
		<T> Tech(BiConsumer<TypeBuilder, T[]> op, T... args) {
			this(t -> op.accept(t, args));
		}
	}

	enum Type {
		Soldier(Category.Land, Weapon.CloseRange, 50, 22, 3, 1, 1,
				EnumSet.of(Tech.StandOnLandExtreme, Tech.AttLand, Tech.AttWater, Tech.Conquerer)),
		Tank(Category.Land, Weapon.CloseRange, 70, 35, 6, 1, 1,
				EnumSet.of(Tech.StandOnLandRough, Tech.AttLand, Tech.AttWater)),
		Artillery(Category.Land, Weapon.LongRange, 70, 35, 3, 3, 5, EnumSet.of(Tech.StandOnLandFlat, Tech.AttAny)),
		Turrent(Category.Land, Weapon.LongRange, 100, 30, 0, 2, 7, EnumSet.of(Tech.StandOnLandFlat, Tech.AttAny)),

		Ship(Category.Water, Weapon.CloseRange, 70, 35, 6, 1, 1,
				EnumSet.of(Tech.StandOnWater, Tech.AttLand, Tech.AttWater)),

		Airplane(Category.Air, Weapon.CloseRange, 70, 35, 6, 1, 1, EnumSet.of(Tech.StandOnAny, Tech.AttAny));

		final Category category;
		final Weapon weapon;
		final Set<Terrain.Category> canStand;
		final Set<Category> canAttack;
		final int health;
		final int damage;
		final int moveLimit;
		final int rangeMin;
		final int rangeMax;
		final boolean canConquer;

		Type(Category category, Weapon weapon, int health, int damage, int moveLimit, int rangeMin, int rangeMax,
				Set<Tech> techs) {
			TypeBuilder builder = new TypeBuilder();
			for (Tech tech : techs)
				tech.op.accept(builder);

			this.category = category;
			this.weapon = weapon;
			this.canStand = Collections.unmodifiableSet(EnumSet.copyOf(builder.canStand));
			this.canAttack = Collections.unmodifiableSet(EnumSet.copyOf(builder.canAttack));
			this.health = health;
			this.damage = damage;
			this.moveLimit = moveLimit;
			this.rangeMin = rangeMin;
			this.rangeMax = rangeMax;
			this.canConquer = builder.canConquer;
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
				if (distanceMap[pos.x][pos.y] >= 0 || !type.canStand.contains(tile.getTerrain().category)
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
