package com.ugav.battalion.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ListInt;

public class Unit extends Entity implements IUnit {

	public final Type type;
	private int pos;
	private int health;
	private final Unit transportedUnit; /* valid only if type.canTransportUnits */

	private Unit(Game game, Type type, Team team, Unit transportedUnit) {
		super(game, team);
		this.type = type;
		health = type.health;

		if (type.transportUnits ^ (transportedUnit != null && transportedUnit.type.category == Unit.Category.Land))
			throw new IllegalArgumentException();
		this.transportedUnit = transportedUnit;
	}

	static Unit valueOf(Game game, UnitDesc desc, int initPos) {
		Unit unit;
		if (!desc.type.transportUnits) {
			unit = new Unit(game, desc.type, desc.team, null);

		} else {
			UnitDesc transportedUnit = desc.getTransportedUnit();
			if (transportedUnit.type.transportUnits || desc.team != transportedUnit.team)
				throw new IllegalArgumentException();
			unit = newTrasportUnit(game, desc.type, new Unit(game, transportedUnit.type, transportedUnit.team, null));
		}
		unit.setPos(initPos);
		return unit;
	}

	static Unit copyOf(Game game, Unit unit) {
		Unit copy;
		if (!unit.type.transportUnits) {
			copy = new Unit(game, unit.type, unit.getTeam(), null);
		} else {
			Unit transportedUnit = unit.getTransportedUnit();
			if (transportedUnit.type.transportUnits || unit.getTeam() != transportedUnit.getTeam())
				throw new IllegalArgumentException();
			copy = newTrasportUnit(game, unit.getType(), copyOf(game, transportedUnit));
		}
		copy.pos = unit.pos;
		copy.health = unit.health;
		copy.setActive(unit.isActive());
		return copy;
	}

	static Unit newTrasportUnit(Game game, Type type, Unit unit) {
		if (!type.transportUnits || unit.type.category != Unit.Category.Land)
			throw new IllegalArgumentException();
		return new Unit(game, type, unit.getTeam(), unit);
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

	@Override
	public int getPos() {
		return pos;
	}

	public int getDamge(Unit target) {
		// health 100% => damage 100%
		// health 0% => damage 50%
		double h = (double) getHealth() / type.health;
		double d = type.damage * (h / 2 + 0.5);
		return Math.max(1, (int) d);
	}

	@Override
	public Unit getTransportedUnit() {
		return type.transportUnits ? Objects.requireNonNull(transportedUnit) : null;
	}

	boolean isMoveValid(ListInt path) {
		if (path.isEmpty() || path.size() > type.moveLimit)
			return false;
		MovementMap movementMap = getMovementMap(false);
		int prev = getPos();
		for (Iter.Int it = path.iterator(); it.hasNext();) {
			int p = it.next();
			if (!Cell.areNeighbors(prev, p) || movementMap.getDistanceTo(p) > type.moveLimit)
				return false;
			prev = p;
		}
		return game.unit(path.last()) == null;
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
		StandOnLandFlat(TypeBuilder::canStand, Terrain.Category.FlatLand, Terrain.Category.Forest,
				Terrain.Category.Road, Terrain.Category.BridgeLow, Terrain.Category.BridgeHigh),

		StandOnLandRough(TypeBuilder::canStand, Terrain.Category.FlatLand, Terrain.Category.Forest,
				Terrain.Category.Hiils, Terrain.Category.Shore, Terrain.Category.Road, Terrain.Category.BridgeLow,
				Terrain.Category.BridgeHigh),

		StandOnLandExtreme(TypeBuilder::canStand, Terrain.Category.FlatLand, Terrain.Category.Forest,
				Terrain.Category.Hiils, Terrain.Category.Mountain, Terrain.Category.Shore, Terrain.Category.Road,
				Terrain.Category.BridgeLow, Terrain.Category.BridgeHigh),

		StandOnWater(TypeBuilder::canStand, Terrain.Category.Water, Terrain.Category.WaterShallow,
				Terrain.Category.BridgeHigh, Terrain.Category.Shore),

		StandOnWaterDeep(TypeBuilder::canStand, Terrain.Category.Water, Terrain.Category.WaterShallow,
				Terrain.Category.BridgeLow, Terrain.Category.BridgeHigh, Terrain.Category.Shore),

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
		Rifleman(Category.Land, Weapon.closeRange(), 50, 22, 3, 75, Tech.StandOnLandExtreme, Tech.AttLand,
				Tech.AttWater, Tech.Conquerer),
		RocketSpecialist(Category.Land, Weapon.closeRange(), 50, 30, 3, 100, Tech.StandOnLandExtreme, Tech.AttLand,
				Tech.AttWater, Tech.Conquerer),
		BattleTank(Category.Land, Weapon.closeRange(), 70, 35, 6, 270, Tech.StandOnLandRough, Tech.AttLand,
				Tech.AttWater),
		TitanTank(Category.Land, Weapon.closeRange(), 140, 70, 4, 470, Tech.StandOnLandRough, Tech.AttLand,
				Tech.AttWater),
		StealthTank(Category.Land, Weapon.closeRange(), 40, 30, 5, 450, Tech.StandOnLandRough, Tech.AttLand,
				Tech.AttWater, Tech.Invisible),
		AATank(Category.Land, Weapon.closeRange(), 70, 17, 5, 230, Tech.StandOnLandRough, Tech.AttAny),
		Artillery(Category.Land, Weapon.longRange(3, 5), 40, 40, 4, 470, Tech.StandOnLandRough, Tech.AttAny),
		Mortar(Category.Land, Weapon.longRange(2, 3), 50, 40, 5, 300, Tech.StandOnLandFlat, Tech.AttAny),

		Turrent(Category.Land, Weapon.longRange(2, 5), 100, 40, 0, 500, Tech.StandOnLandFlat, Tech.AttAny),

		SpeedBoat(Category.Water, Weapon.closeRange(), 50, 15, 5, 200, Tech.StandOnWater, Tech.Conquerer),
		Corvette(Category.Water, Weapon.closeRange(), 90, 45, 5, 500, Tech.StandOnWater, Tech.AttLand, Tech.AttWater),
		AACruiser(Category.Water, Weapon.closeRange(), 90, 17, 5, 450, Tech.StandOnWater, Tech.AttAny),
		Battleship(Category.Water, Weapon.longRange(3, 6), 140, 50, 4, 800, Tech.StandOnWater, Tech.AttLand,
				Tech.AttWater),
		Submarine(Category.DeepWater, Weapon.closeRange(), 25, 35, 4, 475, Tech.StandOnWaterDeep, Tech.AttWater,
				Tech.Invisible, Tech.AttDeepWater),
		LandingCraft(Category.Water, Weapon.none(), 90, 0, 5, 25, Tech.StandOnWater, Tech.UnitTransporter),

		FighterPlane(Category.Air, Weapon.closeRange(), 50, 30, 7, 340, Tech.StandOnAny, Tech.AttAny),
		ZeppelinBomber(Category.Air, Weapon.closeRange(), 110, 80, 4, 650, Tech.StandOnAny, Tech.AttAny),
		TransportPlane(Category.Air, Weapon.none(), 50, 0, 6, 30, Tech.StandOnAny, Tech.UnitTransporter);

		public final Category category;
		public final Weapon weapon;
		private final Set<Terrain.Category> canStandOn;
		private final Set<Category> canAttack;
		public final int health;
		public final int damage;
		public final int moveLimit;
		public final boolean canConquer;
		public final boolean invisible;
		public final boolean transportUnits;
		public final int price;

		Type(Category category, Weapon weapon, int health, int damage, int moveLimit, int price, Tech... techs) {
			TypeBuilder builder = new TypeBuilder(techs);

			this.category = Objects.requireNonNull(category);
			this.weapon = Objects.requireNonNull(weapon);
			this.canStandOn = Collections.unmodifiableSet(EnumSet.copyOf(builder.canStand));
			this.canAttack = Collections.unmodifiableSet(EnumSet.copyOf(builder.canAttack));
			this.health = health;
			this.damage = damage;
			this.moveLimit = moveLimit;
			this.canConquer = builder.tech.contains(Tech.Conquerer);
			this.invisible = builder.tech.contains(Tech.Invisible);
			this.transportUnits = builder.tech.contains(Tech.UnitTransporter);
			this.price = price;
		}

		public boolean canStandOn(Terrain terrain) {
			return canStandOn.contains(terrain.category);
		}
	}

	public boolean canAttack(Unit other) {
		return type.canAttack.contains(other.type.category);
	}

	public Cell.Bitmap getReachableMap() {
		return getReachableMap(true);
	}

	private final Supplier<Cell.Bitmap> reachableMapInvisiableEnable;
	private final Supplier<Cell.Bitmap> reachableMapInvisiableDisable;
	{
		reachableMapInvisiableEnable = game.valuesCache.newVal(() -> getReachableMap0(true));
		reachableMapInvisiableDisable = game.valuesCache.newVal(() -> getReachableMap0(false));
	}

	Cell.Bitmap getReachableMap(boolean invisiableEnable) {
		return invisiableEnable ? reachableMapInvisiableEnable.get() : reachableMapInvisiableDisable.get();
	}

	private Cell.Bitmap getReachableMap0(boolean invisiableEnable) {
		int unitPos = getPos();
		Team us = getTeam();
		return getPassableMap(invisiableEnable)
				.and(p -> p == unitPos || game.unit(p) == null || (invisiableEnable && !game.isUnitVisible(p, us)));
	}

	public Cell.Bitmap getPassableMap() {
		return getPassableMap(true);
	}

	private final Supplier<Cell.Bitmap> passableMapInvisiableEnable;
	private final Supplier<Cell.Bitmap> passableMapInvisiableDisable;
	{
		passableMapInvisiableEnable = game.valuesCache.newVal(() -> getPassableMap0(true));
		passableMapInvisiableDisable = game.valuesCache.newVal(() -> getPassableMap0(false));
	}

	Cell.Bitmap getPassableMap(boolean invisiableEnable) {
		return invisiableEnable ? passableMapInvisiableEnable.get() : passableMapInvisiableDisable.get();
	}

	private Cell.Bitmap getPassableMap0(boolean invisiableEnable) {
		MovementMap movementMap = getMovementMap(invisiableEnable);
		return Cell.Bitmap.fromPredicate(game.width(), game.height(),
				cell -> movementMap.getDistanceTo(cell) <= type.moveLimit);
	}

	private final Supplier<MovementMap> movementMapInvisiableEnable;
	private final Supplier<MovementMap> movementMapInvisiableDisable;
	{
		movementMapInvisiableEnable = game.valuesCache.newVal(() -> calcMovementMap0(true));
		movementMapInvisiableDisable = game.valuesCache.newVal(() -> calcMovementMap0(false));
	}

	private MovementMap getMovementMap(boolean invisiableEnable) {
		return invisiableEnable ? movementMapInvisiableEnable.get() : movementMapInvisiableDisable.get();
	}

	private static class MovementMap {

		/* Each cell is: */
		/* 14 bits of distance map, 0x3FFF for unreachable */
		/* 2 bits for direction for source */
		private final short[][] map;

		private static final int DistanceMask = 0x3FFF;
		private static final int DistanceShift = 0;
		private static final int DirToSourceMask = 0xc000;
		private static final int DirToSourceShift = 14;

		private static final int DistanceUnreachable = 0x3FFF;

		MovementMap(int w, int h) {
			map = new short[w][h];
			short initVal = 0;
			initVal |= (DistanceUnreachable << DistanceShift) & DistanceMask;
			initVal |= (Direction.XPos.ordinal() << DirToSourceShift) & DirToSourceMask;
			for (int x = 0; x < w; x++)
				for (int y = 0; y < h; y++)
					map[x][y] = initVal;
		}

		int getDistanceTo(int cell) {
			return (map[Cell.x(cell)][Cell.y(cell)] & DistanceMask) >> DistanceShift;
		}

		boolean isReachable(int cell) {
			return getDistanceTo(cell) != DistanceUnreachable;
		}

		Direction getDirToSource(int cell) {
			assert isReachable(cell);
			int d = (map[Cell.x(cell)][Cell.y(cell)] & DirToSourceMask) >> DirToSourceShift;
			return Direction.values()[d];
		}

		void set(int cell, int dist, Direction dir) {
			if (dist > DistanceUnreachable)
				throw new IllegalArgumentException();
			short val = 0;
			val |= (dist << DistanceShift) & DistanceMask;
			val |= (dir.ordinal() << DirToSourceShift) & DirToSourceMask;
			map[Cell.x(cell)][Cell.y(cell)] = val;
			assert getDistanceTo(cell) == dist;
			assert getDirToSource(cell) == dir;
			// String.format("val=0x%04x dir=%d getDir=%d", val, dir.ordinal(),
			// getDirToSource(cell).ordinal())
		}
	}

	private MovementMap calcMovementMap0(boolean invisiableEnable) {
		int width = game.width(), height = game.height();
		MovementMap movementMap = new MovementMap(width, height);

		int[] fifo = new int[width * height];
		int fifoBegin = 0, fifoEnd = 0;
		movementMap.set(pos, 0, /* arbitrary */ Direction.XPos);
		fifo[fifoEnd++] = pos;

		while (fifoBegin != fifoEnd) {
			int p = fifo[fifoBegin++];
			int d = movementMap.getDistanceTo(p);
			assert d != MovementMap.DistanceUnreachable;

			for (Direction dir : Direction.values()) {
				int neighbor = Cell.add(p, dir);
				if (!game.isValidCell(neighbor) || movementMap.isReachable(neighbor))
					continue;
				if (!type.canStandOn(game.terrain(neighbor)))
					continue;
				Unit unit = game.unit(neighbor);
				if (unit != null && !(invisiableEnable && !game.isUnitVisible(neighbor, getTeam()))
						&& unit.getTeam() != getTeam())
					continue;

				movementMap.set(neighbor, d + 1, dir.opposite());
				fifo[fifoEnd++] = neighbor;
			}
		}

		return movementMap;
	}

	public int getDistanceTo(int cell) {
		int d = getMovementMap(true).getDistanceTo(cell);
		return d != MovementMap.DistanceUnreachable ? d : -1;
	}

	public boolean isEnemyInRange() {
		final Team us = getTeam();
		Cell.Bitmap attackableMap = getAttackableMap();
		return game.enemiesSeenBy(us).mapBool(u -> attackableMap.contains(u.getPos())).any();
	}

	public ListInt calcPath(int destination) {
		MovementMap movementMap = getMovementMap(true);
		if (movementMap.getDistanceTo(destination) > type.moveLimit)
			throw new IllegalArgumentException("Can't reach " + destination);

		ListInt path = new ListInt.Array(movementMap.getDistanceTo(destination));
		for (int p = destination; p != getPos(); p = Cell.add(p, movementMap.getDirToSource(p)))
			path.add(p);
		path.reverse();
		return path;
	}

	public ListInt calcPathForAttack(int targetPos) {
		MovementMap movementMap = getMovementMap(true);
		final int NoValue = Cell.of(-1, -1);
		int destination = NoValue;
		int length = Integer.MAX_VALUE;
		for (int dest : Cell.neighbors(targetPos)) {
			if (dest == getPos()) {
				destination = dest;
				break;
			}
			if (!game.isValidCell(dest) || movementMap.getDistanceTo(dest) > type.moveLimit)
				continue;
			if (game.isUnitVisible(dest, getTeam()))
				continue;
			int l = movementMap.getDistanceTo(dest);
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
//		return getAttackableMap(true);
		return attackableMapInvisiableEnable.get();
	}

	private final Supplier<Cell.Bitmap> attackableMapInvisiableEnable;
//	private final Supplier<Cell.Bitmap> attackableMapInvisiableDisable;
	{
		attackableMapInvisiableEnable = game.valuesCache.newVal(() -> getAttackableMap0(true));
//		attackableMapInvisiableDisable = game.valuesCache.newVal(() -> getAttackableMap0(false));
	}

//	private Cell.Bitmap getAttackableMap(boolean invisiableEnable) {
//		return invisiableEnable ? attackableMapInvisiableEnable.get() : attackableMapInvisiableDisable.get();
//	}

	private Cell.Bitmap getAttackableMap0(boolean invisiableEnable) {
		return getPotentiallyAttackableMap(invisiableEnable).and(p -> {
			Unit target = game.unit(p);
			if (target == null || (invisiableEnable && !game.isUnitVisible(p, getTeam())))
				return false;
			return target.getTeam() != getTeam() && canAttack(target);
		});
	}

	public Cell.Bitmap getPotentiallyAttackableMap() {
		return getPotentiallyAttackableMap(true);
	}

	private final Supplier<Cell.Bitmap> potentiallyAttackableMapInvisiableEnable;
	private final Supplier<Cell.Bitmap> potentiallyAttackableMapInvisiableDisable;
	{
		potentiallyAttackableMapInvisiableEnable = game.valuesCache.newVal(() -> getPotentiallyAttackableMap0(true));
		potentiallyAttackableMapInvisiableDisable = game.valuesCache.newVal(() -> getPotentiallyAttackableMap0(false));
	}

	private Cell.Bitmap getPotentiallyAttackableMap(boolean invisiableEnable) {
		return invisiableEnable ? potentiallyAttackableMapInvisiableEnable.get()
				: potentiallyAttackableMapInvisiableDisable.get();
	}

	private Cell.Bitmap getPotentiallyAttackableMap0(boolean invisiableEnable) {
		switch (type.weapon.type) {
		case CloseRange:
			return getPotentiallyAttackableMapCloseRange(invisiableEnable);
		case LongRange:
			return getPotentiallyAttackableMapLongRange();
		case None:
			return Cell.Bitmap.empty();
		default:
			throw new IllegalArgumentException("Unexpected value: " + type.weapon.type);
		}
	}

	private Cell.Bitmap getPotentiallyAttackableMapCloseRange(boolean invisiableEnable) {
		Cell.Bitmap reachableMap = getReachableMap(invisiableEnable);
		Cell.Bitmap attackableMap = Cell.Bitmap.ofFalse(game.width(), game.height());
		for (Iter.Int it = reachableMap.cells(); it.hasNext();)
			for (int n : Cell.neighbors(it.next()))
				if (game.isValidCell(n))
					attackableMap.set(n, true);
		return attackableMap;
	}

	private Cell.Bitmap getPotentiallyAttackableMapLongRange() {
		return Cell.Bitmap.fromPredicate(game.width(), game.height(), p -> {
			int distance = Cell.distNorm1(getPos(), p);
			return type.weapon.minRange <= distance && distance <= type.weapon.maxRange;
		});
	}

	@Override
	public String toString() {
		return "" + getTeam().toString().charAt(0) + type + Cell.toString(getPos());
	}

	@Override
	public Type getType() {
		return type;
	}

}
