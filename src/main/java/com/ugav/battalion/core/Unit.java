package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.core.Level.UnitDesc;

public class Unit extends Entity implements IUnit {

	public final Type type;
	private Position pos;
	private int health;
	private final Unit transportedUnit; /* valid only if type.canTransportUnits */

	private Unit(Arena arena, Type type, Team team, Unit transportedUnit) {
		super(arena, team);
		this.type = type;
		health = type.health;

		if (type.transportUnits ^ (transportedUnit != null && transportedUnit.type.category == Unit.Category.Land))
			throw new IllegalArgumentException();
		this.transportedUnit = transportedUnit;
	}

	static Unit valueOf(Arena arena, UnitDesc desc) {
		if (!desc.type.transportUnits) {
			return new Unit(arena, desc.type, desc.team, null);

		} else {
			UnitDesc transportedUnit = desc.getTransportedUnit();
			if (transportedUnit.type.transportUnits || desc.team != transportedUnit.team)
				throw new IllegalArgumentException();
			return newTrasportUnit(arena, desc.type, new Unit(arena, transportedUnit.type, transportedUnit.team, null));
		}
	}

	static Unit newTrasportUnit(Arena arena, Type type, Unit unit) {
		if (!type.transportUnits || unit.type.category != Unit.Category.Land)
			throw new IllegalArgumentException();
		return new Unit(arena, type, unit.getTeam(), unit);
	}

	@Override
	final void setTeam(Team team) {
		throw new UnsupportedOperationException();
	}

	public int getHealth() {
		return health;
	}

	void setHealth(int health) {
		if (this.health == health)
			return;
		this.health = health;
		onChange().notify(new EntityChange(this));
	}

	public boolean isDead() {
		return health <= 0;
	}

	void setPos(Position pos) {
		if (Objects.equals(this.pos, pos))
			return;
		this.pos = pos;
		onChange().notify(new EntityChange(this));
	}

	public Position getPos() {
		return pos;
	}

	public int getDamge(Unit target) {
		return type.damage;
	}

	@Override
	public Unit getTransportedUnit() {
		return type.transportUnits ? Objects.requireNonNull(transportedUnit) : null;
	}

	boolean isMoveValid(List<Position> path) {
		if (path.isEmpty() || path.size() > type.moveLimit)
			return false;
		int[][] distanceMap = calcDistanceMap(false);
		Position prev = getPos();
		for (Position p : path) {
			if (!prev.neighbors().contains(p) || distanceMap[p.xInt()][p.yInt()] < 0)
				return false;
			prev = p;
		}
		return !arena.at(path.get(path.size() - 1)).hasUnit();
	}

	boolean isAttackValid(Unit target) {
		return target.getTeam() != getTeam() && getAttackableMap().contains(target.getPos()) && canAttack(target);
	}

	public enum Category {
		Land, Water, DeepWater, Air
	}

	public static class Weapon {
		public enum Type {
			CloseRange, LongRange, None
		}

		public final Type type;
		public final int minRange;
		public final int maxRange;

		private Weapon(Type type, int minRange, int maxRange) {
			this.type = Objects.requireNonNull(type);
			if (minRange < 0 || minRange > maxRange)
				throw new IllegalArgumentException();
			this.minRange = minRange;
			this.maxRange = maxRange;
		}

		private static Weapon closeRange() {
			return new Weapon(Type.CloseRange, 1, 1);
		}

		private static Weapon longRange(int minRange, int maxRange) {
			return new Weapon(Type.LongRange, minRange, maxRange);
		}

		private static Weapon none() {
			return new Weapon(Type.None, 0, 0);
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

		AttDeepWater(TypeBuilder::canAttack, Unit.Category.DeepWater),

		AttAir(TypeBuilder::canAttack, Unit.Category.Air),

		AttAny(TypeBuilder::canAttack, Unit.Category.values()),

		Conquerer, Invisible, UnitTransporter;

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

	public enum Type {
		Soldier(Category.Land, Weapon.closeRange(), 50, 22, 3, Tech.StandOnLandExtreme, Tech.AttLand, Tech.AttWater,
				Tech.Conquerer),
		Bazooka(Category.Land, Weapon.closeRange(), 50, 30, 3, Tech.StandOnLandExtreme, Tech.AttLand, Tech.AttWater,
				Tech.Conquerer),
		Tank(Category.Land, Weapon.closeRange(), 70, 35, 6, Tech.StandOnLandRough, Tech.AttLand, Tech.AttWater),
		TankBig(Category.Land, Weapon.closeRange(), 140, 70, 4, Tech.StandOnLandRough, Tech.AttLand, Tech.AttWater),
		TankAntiAir(Category.Land, Weapon.closeRange(), 70, 17, 5, Tech.StandOnLandRough, Tech.AttAny),
		Artillery(Category.Land, Weapon.longRange(3, 5), 40, 40, 4, Tech.StandOnLandFlat, Tech.AttAny),
		Mortar(Category.Land, Weapon.longRange(2, 3), 50, 40, 5, Tech.StandOnLandFlat, Tech.AttAny),

		Turrent(Category.Land, Weapon.longRange(2, 5), 100, 40, 0, Tech.StandOnLandFlat, Tech.AttAny),

		SpeedBoat(Category.Water, Weapon.closeRange(), 50, 15, 5, Tech.StandOnWater, Tech.Conquerer),
		Ship(Category.Water, Weapon.closeRange(), 90, 45, 5, Tech.StandOnWater, Tech.AttLand, Tech.AttWater),
		ShipAntiAir(Category.Water, Weapon.closeRange(), 90, 17, 5, Tech.StandOnWater, Tech.AttAny),
		ShipArtillery(Category.Water, Weapon.longRange(3, 6), 140, 50, 4, Tech.StandOnWater, Tech.AttLand,
				Tech.AttWater),
		Submarine(Category.DeepWater, Weapon.closeRange(), 25, 35, 4, Tech.StandOnWaterDeep, Tech.AttWater,
				Tech.Invisible, Tech.AttDeepWater),
		ShipTransporter(Category.Water, Weapon.none(), 90, 0, 5, Tech.StandOnWater, Tech.UnitTransporter),

		Airplane(Category.Air, Weapon.closeRange(), 50, 30, 7, Tech.StandOnAny, Tech.AttAny),
		Zeppelin(Category.Air, Weapon.closeRange(), 110, 80, 4, Tech.StandOnAny, Tech.AttAny),
		AirTransporter(Category.Air, Weapon.none(), 50, 0, 6, Tech.StandOnAny, Tech.UnitTransporter);

		public final Category category;
		public final Weapon weapon;
		private final Set<Terrain.Category> canStand;
		private final Set<Category> canAttack;
		public final int health;
		public final int damage;
		public final int moveLimit;
		public final boolean canConquer;
		public final boolean invisible;
		public final boolean transportUnits;

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
			this.invisible = builder.tech.contains(Tech.Invisible);
			this.transportUnits = builder.tech.contains(Tech.UnitTransporter);
		}

		public boolean canStandOn(Terrain terrain) {
			return canStand.contains(terrain.category);
		}
	}

	public boolean canAttack(Unit other) {
		return type.canAttack.contains(other.type.category);
	}

	public Position.Bitmap getReachableMap() {
		return getReachableMap(true);
	}

	private Position.Bitmap getReachableMap(boolean invisiableEnable) {
		return getPassableMap(invisiableEnable).and(p -> {
			if (!arena.at(p).hasUnit() || (invisiableEnable && !arena.isUnitVisible(p, getTeam())))
				return true;
			return arena.at(p).getUnit() == this;
		});
	}

	public Position.Bitmap getPassableMap() {
		return getPassableMap(true);
	}

	Position.Bitmap getPassableMap(boolean invisiableEnable) {
		int[][] distanceMap = calcDistanceMap(invisiableEnable);
		return Position.Bitmap.fromPredicate(arena.width(), arena.height(), p -> distanceMap[p.xInt()][p.yInt()] >= 0);
	}

	private int[][] calcDistanceMap(boolean invisiableEnable) {
		int width = arena.width(), height = arena.height();

		int[][] distanceMap = new int[width][height];
		for (int x = 0; x < width; x++)
			Arrays.fill(distanceMap[x], -1);
		distanceMap[pos.xInt()][pos.yInt()] = 0;

		int maxMove = type.moveLimit;
		for (int moveLen = 1; moveLen <= maxMove; moveLen++) {
			for (Position p : arena.positions()) {
				Tile tile = arena.at(p);
				if (distanceMap[p.xInt()][p.yInt()] >= 0 || !type.canStandOn(tile.getTerrain()))
					continue;
				if (tile.hasUnit() && !(invisiableEnable && !arena.isUnitVisible(p, getTeam()))
						&& tile.getUnit().getTeam() != getTeam())
					continue;

				for (Position neighbor : p.neighbors()) {
					if (arena.isValidPos(neighbor) && distanceMap[neighbor.xInt()][neighbor.yInt()] == moveLen - 1) {
						distanceMap[p.xInt()][p.yInt()] = moveLen;
						break;
					}
				}
			}
		}
		return distanceMap;
	}

	public List<Position> calcPath(Position destination) {
		int[][] distanceMap = calcDistanceMap(true);
		if (distanceMap[destination.xInt()][destination.yInt()] < 0)
			throw new IllegalArgumentException("Can't reach " + destination);
		List<Position> path = new ArrayList<>(distanceMap[destination.xInt()][destination.yInt()]);
		for (Position p = destination; !p.equals(getPos());) {
			path.add(p);
			for (Position next : p.neighbors()) {
				if (arena.isValidPos(next)
						&& distanceMap[next.xInt()][next.yInt()] == distanceMap[p.xInt()][p.yInt()] - 1) {
					p = next;
					break;
				}
			}
		}
		Collections.reverse(path);
		return path;
	}

	public List<Position> calcPathForAttack(Position targetPos) {
		Position.Bitmap reachableMap = getReachableMap();
		List<Position> bestPath = null;
		for (Position p : targetPos.neighbors()) {
			if (!reachableMap.contains(p))
				continue;
			List<Position> path = calcPath(p);
			if (bestPath == null || path.size() < bestPath.size())
				bestPath = path;
		}
		return bestPath;
	}

	public Position.Bitmap getAttackableMap() {
		return getAttackableMap(true);
	}

	private Position.Bitmap getAttackableMap(boolean invisiableEnable) {
		switch (type.weapon.type) {
		case CloseRange:
			return getAttackableMapCloseRange(invisiableEnable);
		case LongRange:
			return getAttackableMapLongRange(invisiableEnable);
		case None:
			return Position.Bitmap.Empty;
		default:
			throw new InternalError();
		}
	}

	private Position.Bitmap getAttackableMapCloseRange(boolean invisiableEnable) {
		Position.Bitmap reachableMap = getReachableMap(invisiableEnable);

		boolean[][] attackableMap = new boolean[arena.width()][arena.height()];
		for (Position p : reachableMap) {
			for (Position neighbor : p.neighbors()) {
				if (!arena.isValidPos(neighbor))
					continue;
				if (!arena.at(neighbor).hasUnit() || (invisiableEnable && !arena.isUnitVisible(neighbor, getTeam())))
					continue;
				Unit other = arena.at(neighbor).getUnit();
				attackableMap[neighbor.xInt()][neighbor.yInt()] = other.getTeam() != getTeam() && canAttack(other);
			}
		}

		return new Position.Bitmap(attackableMap);
	}

	private Position.Bitmap getAttackableMapLongRange(boolean invisiableEnable) {
		return Position.Bitmap.fromPredicate(arena.width(), arena.height(), p -> {
			int distance = (int) getPos().distNorm1(p);
			if (!arena.at(p).hasUnit() || (invisiableEnable && !arena.isUnitVisible(p, getTeam())))
				return false;
			Unit other = arena.at(p).getUnit();
			if (!(type.weapon.minRange <= distance && distance <= type.weapon.maxRange) || other.getTeam() == getTeam())
				return false;
			return canAttack(other);

		});
	}

	@Override
	public String toString() {
		return "" + getTeam().toString().charAt(0) + type;
	}

	@Override
	public Type getType() {
		return type;
	}

}
