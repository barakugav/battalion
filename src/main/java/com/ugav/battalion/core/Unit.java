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
import com.ugav.battalion.util.Iter;

public class Unit extends Entity implements IUnit {

	public final Type type;
	private int pos;
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

	static Unit copyOf(Arena arena, Unit unit) {
		Unit copy;
		if (!unit.type.transportUnits) {
			copy = new Unit(arena, unit.type, unit.getTeam(), null);
		} else {
			Unit transportedUnit = unit.getTransportedUnit();
			if (transportedUnit.type.transportUnits || unit.getTeam() != transportedUnit.getTeam())
				throw new IllegalArgumentException();
			copy = newTrasportUnit(arena, unit.getType(), copyOf(arena, transportedUnit));
		}
		copy.pos = unit.pos;
		copy.health = unit.health;
		copy.setActive(unit.isActive());
		return copy;
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

	void setPos(int cell) {
		if (this.pos == cell)
			return;
		this.pos = cell;
		onChange().notify(new EntityChange(this));
	}

	public int getPos() {
		return pos;
	}

	public int getDamge(Unit target) {
		return type.damage;
	}

	@Override
	public Unit getTransportedUnit() {
		return type.transportUnits ? Objects.requireNonNull(transportedUnit) : null;
	}

	boolean isMoveValid(List<Integer> path) {
		if (path.isEmpty() || path.size() > type.moveLimit)
			return false;
		int[][] distanceMap = calcDistanceMap(false);
		int prev = getPos();
		for (int p : path) {
			if (!Cell.areNeighbors(prev, p) || distanceMap[Cell.x(p)][Cell.y(p)] < 0)
				return false;
			prev = p;
		}
		return arena.unit(path.get(path.size() - 1)) == null;
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

	public Cell.Bitmap getReachableMap() {
		return getReachableMap(true);
	}

	private final Arena.Cached<Cell.Bitmap> reachableMapInvisiableEnable;
	private final Arena.Cached<Cell.Bitmap> reachableMapInvisiableDisable;
	{
		reachableMapInvisiableEnable = arena.newCached(() -> getReachableMap0(true));
		reachableMapInvisiableDisable = arena.newCached(() -> getReachableMap0(false));
	}

	Cell.Bitmap getReachableMap(boolean invisiableEnable) {
		return invisiableEnable ? reachableMapInvisiableEnable.get() : reachableMapInvisiableDisable.get();
	}

	private Cell.Bitmap getReachableMap0(boolean invisiableEnable) {
		return getPassableMap(invisiableEnable)
				.and(p -> arena.unit(p) == null || (invisiableEnable && !arena.isUnitVisible(p, getTeam())));
	}

	public Cell.Bitmap getPassableMap() {
		return getPassableMap(true);
	}

	private final Arena.Cached<Cell.Bitmap> passableMapInvisiableEnable;
	private final Arena.Cached<Cell.Bitmap> passableMapInvisiableDisable;
	{
		passableMapInvisiableEnable = arena.newCached(() -> getPassableMap0(true));
		passableMapInvisiableDisable = arena.newCached(() -> getPassableMap0(false));
	}

	Cell.Bitmap getPassableMap(boolean invisiableEnable) {
		return invisiableEnable ? passableMapInvisiableEnable.get() : passableMapInvisiableDisable.get();
	}

	private Cell.Bitmap getPassableMap0(boolean invisiableEnable) {
		int[][] distanceMap = calcDistanceMap(invisiableEnable);
		return Cell.Bitmap.fromPredicate(arena.width(), arena.height(),
				cell -> distanceMap[Cell.x(cell)][Cell.y(cell)] >= 0);
	}

	private final Arena.Cached<int[][]> distanceMapInvisiableEnable;
	private final Arena.Cached<int[][]> distanceMapInvisiableDisable;
	{
		distanceMapInvisiableEnable = arena.newCached(() -> calcDistanceMap0(true));
		distanceMapInvisiableDisable = arena.newCached(() -> calcDistanceMap0(false));
	}

	private int[][] calcDistanceMap(boolean invisiableEnable) {
		return invisiableEnable ? distanceMapInvisiableEnable.get() : distanceMapInvisiableDisable.get();
	}

	private int[][] calcDistanceMap0(boolean invisiableEnable) {
		int width = arena.width(), height = arena.height();

		int[][] distanceMap = new int[width][height];
		for (int x = 0; x < width; x++)
			Arrays.fill(distanceMap[x], -1);

		int[] fifo = new int[Math.min(4 * type.moveLimit * type.moveLimit, width * height)];
		int fifoBegin = 0, fifoEnd = 0;
		distanceMap[Cell.x(pos)][Cell.y(pos)] = 0;
		fifo[fifoEnd++] = pos;

		while (fifoBegin != fifoEnd) {
			int p = fifo[fifoBegin++];
			int d = distanceMap[Cell.x(p)][Cell.y(p)];
			assert d >= 0;

			if (d >= type.moveLimit)
				continue;
			for (int neighbor : Cell.neighbors(p)) {
				if (!arena.isValidCell(neighbor) || distanceMap[Cell.x(neighbor)][Cell.y(neighbor)] >= 0)
					continue;
				Terrain terrain = arena.terrain(neighbor);
				if (!type.canStandOn(terrain))
					continue;
				Unit unit = arena.unit(neighbor);
				if (unit != null && !(invisiableEnable && !arena.isUnitVisible(neighbor, getTeam()))
						&& unit.getTeam() != getTeam())
					continue;

				distanceMap[Cell.x(neighbor)][Cell.y(neighbor)] = d + 1;
				fifo[fifoEnd++] = neighbor;
			}
		}

		return distanceMap;
	}

	public List<Integer> calcPath(int destination) {
		int[][] distanceMap = calcDistanceMap(true);
		if (distanceMap[Cell.x(destination)][Cell.y(destination)] < 0)
			throw new IllegalArgumentException("Can't reach " + destination);
		List<Integer> path = new ArrayList<>(distanceMap[Cell.x(destination)][Cell.y(destination)]);
		for (int p = destination; p != getPos();) {
			path.add(p);
			for (int next : Cell.neighbors(p)) {
				if (arena.isValidCell(next)
						&& distanceMap[Cell.x(next)][Cell.y(next)] == distanceMap[Cell.x(p)][Cell.y(p)] - 1) {
					p = next;
					break;
				}
			}
		}
		Collections.reverse(path);
		return path;
	}

	public List<Integer> calcPathForAttack(int targetPos) {
		int[][] distanceMap = calcDistanceMap(true);
		final int NoValue = Cell.valueOf(-1, -1);
		int destination = NoValue;
		int length = Integer.MAX_VALUE;
		for (int dest : Cell.neighbors(targetPos)) {
			if (!arena.isValidCell(dest))
				continue;
			int l = distanceMap[Cell.x(dest)][Cell.y(dest)];
			if (l < 0)
				continue;
			if (destination == NoValue || length > l) {
				destination = dest;
				length = l;
			}
		}
		if (destination == NoValue)
			throw new IllegalArgumentException("Can't attack target: " + Cell.toString(targetPos));

		return calcPath(destination);
	}

	public Cell.Bitmap getAttackableMap() {
		return getAttackableMap(true);
	}

	private final Arena.Cached<Cell.Bitmap> attackableMapInvisiableEnable;
	private final Arena.Cached<Cell.Bitmap> attackableMapInvisiableDisable;
	{
		attackableMapInvisiableEnable = arena.newCached(() -> getAttackableMap0(true));
		attackableMapInvisiableDisable = arena.newCached(() -> getAttackableMap0(false));
	}

	private Cell.Bitmap getAttackableMap(boolean invisiableEnable) {
		return invisiableEnable ? attackableMapInvisiableEnable.get() : attackableMapInvisiableDisable.get();
	}

	private Cell.Bitmap getAttackableMap0(boolean invisiableEnable) {
		switch (type.weapon.type) {
		case CloseRange:
			return getAttackableMapCloseRange(invisiableEnable);
		case LongRange:
			return getAttackableMapLongRange(invisiableEnable);
		case None:
			return Cell.Bitmap.Empty;
		default:
			throw new IllegalArgumentException("Unexpected value: " + type.weapon.type);
		}
	}

	private Cell.Bitmap getAttackableMapCloseRange(boolean invisiableEnable) {
		Cell.Bitmap reachableMap = getReachableMap(invisiableEnable);
		Cell.Bitmap attackableMap = Cell.Bitmap.ofFalse(arena.width(), arena.height());

		for (Iter.Int it = reachableMap.cells(); it.hasNext();) {
			int cell = it.next();
			for (int n : Cell.neighbors(cell)) {
				if (!arena.isValidCell(n))
					continue;
				Unit neighbor = arena.unit(n);
				if (neighbor == null || (invisiableEnable && !arena.isUnitVisible(n, getTeam())))
					continue;
				attackableMap.set(n, neighbor.getTeam() != getTeam() && canAttack(neighbor));
			}
		}

		return attackableMap;
	}

	private Cell.Bitmap getAttackableMapLongRange(boolean invisiableEnable) {
		return Cell.Bitmap.fromPredicate(arena.width(), arena.height(), p -> {
			int distance = Cell.distNorm1(getPos(), p);
			Unit target = arena.unit(p);
			if (target == null || (invisiableEnable && !arena.isUnitVisible(p, getTeam())))
				return false;
			if (!(type.weapon.minRange <= distance && distance <= type.weapon.maxRange)
					|| target.getTeam() == getTeam())
				return false;
			return canAttack(target);
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
