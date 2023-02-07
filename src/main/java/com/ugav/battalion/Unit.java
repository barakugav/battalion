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

import com.ugav.battalion.Level.UnitDesc;

class Unit extends Entity {

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

	static class Weapon {
		enum Type {
			CloseRange, LongRange
		}

		final Type type;
		final int minRange;
		final int maxRange;

		private Weapon(Type type, int minRange, int maxRange) {
			this.type = Objects.requireNonNull(type);
			if (minRange < 0 || minRange > maxRange)
				throw new IllegalArgumentException();
			this.minRange = minRange;
			this.maxRange = maxRange;
		}

		static Weapon closeRange() {
			return new Weapon(Type.CloseRange, 1, 1);
		}

		static Weapon longRange(int minRange, int maxRange) {
			return new Weapon(Type.LongRange, minRange, maxRange);
		}

	}

	private static class TypeBuilder {
		final Set<Terrain.Category> canStand = EnumSet.noneOf(Terrain.Category.class);
		final Set<Category> canAttack = EnumSet.noneOf(Category.class);
		final Set<Tech> tech;

		TypeBuilder(Tech... techs) {
			this.tech = techs.length > 0 ? EnumSet.copyOf(List.of(techs)) : EnumSet.noneOf(Tech.class);
			for (Tech tech : techs)
				if (tech.op != null)
					tech.op.accept(this);
		}

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

		StandOnWater(TypeBuilder::canStand, Terrain.Category.Water, Terrain.Category.BridgeHigh,
				Terrain.Category.Shore),

		StandOnWaterDeep(TypeBuilder::canStand, Terrain.Category.Water, Terrain.Category.BridgeLow,
				Terrain.Category.BridgeHigh),

		StandOnAny(TypeBuilder::canStand, Terrain.Category.values()),

		AttLand(TypeBuilder::canAttack, Unit.Category.Land),

		AttWater(TypeBuilder::canAttack, Unit.Category.Water),

		AttAir(TypeBuilder::canAttack, Unit.Category.Air),

		AttAny(TypeBuilder::canAttack, Unit.Category.values()),

		Conquerer;

		final Consumer<TypeBuilder> op;

		Tech() {
			this.op = null;
		}

		Tech(Consumer<TypeBuilder> op) {
			this.op = Objects.requireNonNull(op);
		}

		@SuppressWarnings("unchecked")
		<T> Tech(BiConsumer<TypeBuilder, T[]> op, T... args) {
			this(t -> op.accept(t, args));
		}
	}

	enum Type {
		Soldier(Category.Land, Weapon.closeRange(), 50, 22, 3, Tech.StandOnLandExtreme, Tech.AttLand, Tech.AttWater,
				Tech.Conquerer),
		Bazooka(Category.Land, Weapon.closeRange(), 50, 22, 3, Tech.StandOnLandExtreme, Tech.AttLand, Tech.AttWater,
				Tech.Conquerer),
		Tank(Category.Land, Weapon.closeRange(), 70, 35, 6, Tech.StandOnLandRough, Tech.AttLand, Tech.AttWater),
		TankBig(Category.Land, Weapon.closeRange(), 70, 35, 6, Tech.StandOnLandRough, Tech.AttLand, Tech.AttWater),
		TankAntiAir(Category.Land, Weapon.closeRange(), 70, 35, 6, Tech.StandOnLandRough, Tech.AttAny),
		Artillery(Category.Land, Weapon.longRange(3, 5), 70, 35, 3, Tech.StandOnLandFlat, Tech.AttAny),
		Mortar(Category.Land, Weapon.longRange(3, 5), 70, 35, 3, Tech.StandOnLandFlat, Tech.AttAny),

		Turrent(Category.Land, Weapon.longRange(2, 7), 100, 30, 0, Tech.StandOnLandFlat, Tech.AttAny),

		SpeedBoat(Category.Water, Weapon.closeRange(), 70, 0, 6, Tech.StandOnWater, Tech.Conquerer),
		Ship(Category.Water, Weapon.closeRange(), 70, 35, 6, Tech.StandOnWater, Tech.AttLand, Tech.AttWater),
		ShipAntiAir(Category.Water, Weapon.closeRange(), 70, 35, 6, Tech.StandOnWater, Tech.AttAny),
		ShipArtillery(Category.Water, Weapon.longRange(2, 7), 70, 35, 6, Tech.StandOnWater, Tech.AttLand,
				Tech.AttWater),

		Airplane(Category.Air, Weapon.closeRange(), 70, 35, 6, Tech.StandOnAny, Tech.AttAny),
		Zeppelin(Category.Air, Weapon.closeRange(), 70, 35, 6, Tech.StandOnAny, Tech.AttAny);

		final Category category;
		final Weapon weapon;
		final Set<Terrain.Category> canStand;
		final Set<Category> canAttack;
		final int health;
		final int damage;
		final int moveLimit;
		final boolean canConquer;

		Type(Category category, Weapon weapon, int health, int damage, int moveLimit, Tech... techs) {
			TypeBuilder builder = new TypeBuilder(techs);

			this.category = Objects.requireNonNull(category);
			this.weapon = Objects.requireNonNull(weapon);
			this.canStand = Collections.unmodifiableSet(EnumSet.copyOf(builder.canStand));
			this.canAttack = Collections.unmodifiableSet(EnumSet.copyOf(builder.canAttack));
			this.health = health;
			this.damage = damage;
			this.moveLimit = moveLimit;
			this.canConquer = builder.tech.contains(Tech.Conquerer);
		}
	}

	static Unit valueOf(Arena arena, UnitDesc desc) {
		return new Unit(arena, desc.type, desc.team);
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

	Position.Bitmap getAttackableMap() {
		switch (type.weapon.type) {
		case CloseRange:
			return getAttackableMapCloseRange();
		case LongRange:
			return getAttackableMapLongRange();
		default:
			throw new InternalError();
		}
	}

	private Position.Bitmap getAttackableMapCloseRange() {
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

	private Position.Bitmap getAttackableMapLongRange() {
		boolean[][] attackableMap = new boolean[arena.getWidth()][arena.getHeight()];
		for (Position pos : arena.positions()) {
			int distance = distance1Norm(getPos(), pos);
			if (!arena.at(pos).hasUnit() || !(type.weapon.minRange <= distance && distance <= type.weapon.maxRange))
				continue;
			Unit other = arena.at(pos).getUnit();
			attackableMap[pos.x][pos.y] = other.getTeam() != getTeam() && type.canAttack.contains(other.type.category);
		}
		return new Position.Bitmap(attackableMap);
	}

	private static int distance1Norm(Position p1, Position p2) {
		return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
	}

	@Override
	public String toString() {
		return (getTeam() == Team.Red ? "R" : "B") + type;
	}

}
