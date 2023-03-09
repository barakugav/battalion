package com.ugav.battalion.core;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.ugav.battalion.core.Building.ConquerEvent;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Unit.Weapon;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ListInt;
import com.ugav.battalion.util.Utils;
import com.ugav.battalion.util.ValuesCache;

public class Game {

	private final Cell.Array<Terrain> terrains;
	private final Cell.Array<Unit> units;
	private final Cell.Array<Building> buildings;

	final ValuesCache valuesCache = new ValuesCache();

	private final Map<Team, TeamData> teamData;
	private final Iterator<Team> turnIterator;
	private Team turn;
	private Team winner;

	public final Event.Notifier<EntityChange> onEntityChange = new Event.Notifier<>();
	public final Event.Notifier<UnitAdd> onUnitAdd = new Event.Notifier<>();
	public final Event.Notifier<UnitBuy> onUnitBuy = new Event.Notifier<>();
	public final Event.Notifier<UnitRemove> onUnitRemove = new Event.Notifier<>();
	public final Event.Notifier<UnitDeath> onUnitDeath = new Event.Notifier<>();
	public final Event.Notifier<UnitMove> beforeUnitMove = new Event.Notifier<>();
	public final Event.Notifier<UnitAttack> beforeUnitAttack = new Event.Notifier<>();
	public final Event.Notifier<ConquerEvent> onConquerProgress = new Event.Notifier<>();
	public final Event.Notifier<ConquerEvent> onConquerFinish = new Event.Notifier<>();
	public final Event.Notifier<MoneyChange> onMoneyChange = new Event.Notifier<>();
	public final Event.Notifier<Event> beforeTurnEnd = new Event.Notifier<>();
	public final Event.Notifier<Event> onTurnBegin = new Event.Notifier<>();
	public final Event.Notifier<TurnEnd> onTurnEnd = new Event.Notifier<>();
	public final Event.Notifier<GameEnd> onGameEnd = new Event.Notifier<>();
	public final Event.Notifier<ActionEvent> onAction = new Event.Notifier<>();

	private Game(Level level) {
		int w = level.width(), h = level.height();
		terrains = Cell.Array.fromFunc(w, h, cell -> level.at(cell).terrain);
		units = Cell.Array.fromFunc(w, h, pos -> {
			UnitDesc desc = level.at(pos).unit;
			return desc != null ? Unit.valueOf(this, desc, pos) : null;
		});
		buildings = Cell.Array.fromFunc(w, h, pos -> {
			BuildingDesc desc = level.at(pos).building;
			return desc != null ? Building.valueOf(this, desc, pos) : null;
		});

		teamData = new HashMap<>();
		turnIterator = Utils.iteratorRepeatInfty(Team.realTeams);
		turn = turnIterator.next();
		winner = null;

		for (Team team : Team.realTeams)
			teamData.put(team, new TeamData(level.getStartingMoney(team)));
	}

	private Game(Game game) {
		int w = game.width(), h = game.height();
		terrains = Cell.Array.fromFunc(w, h, pos -> game.terrain(pos));
		units = Cell.Array.fromFunc(w, h, pos -> {
			Unit unit = game.unit(pos);
			return unit != null ? Unit.copyOf(this, unit) : null;
		});
		buildings = Cell.Array.fromFunc(w, h, pos -> {
			Building building = game.building(pos);
			return building != null ? Building.copyOf(this, building) : null;
		});

		teamData = new HashMap<>();
		turnIterator = Utils.iteratorRepeatInfty(Team.realTeams);
		for (;;)
			if ((turn = turnIterator.next()).equals(game.getTurn()))
				break;
		winner = game.getWinner();

		for (Team team : Team.realTeams)
			teamData.put(team, new TeamData(game.getMoney(team)));
	}

	public static Game fromLevel(Level level) {
		return new Game(level);
	}

	public static Game copyOf(Game game) {
		return new Game(game);
	}

	public int width() {
		return terrains.width();
	}

	public int height() {
		return terrains.height();
	}

	public Terrain terrain(int cell) {
		return terrains.at(cell);
	}

	public Unit unit(int cell) {
		return units.at(cell);
	}

	public Building building(int cell) {
		return buildings.at(cell);
	}

	private void setUnit(int cell, Unit unit) {
		assert units.at(cell) == null;
		units.set(cell, Objects.requireNonNull(unit));
		valuesCache.invalidate();
	}

	private void removeUnit(Unit unit) {
		int pos = unit.getPos();
		assert unit == this.unit(pos);
		units.set(pos, null);
		valuesCache.invalidate();
	}

	public boolean isValidCell(int cell) {
		return Cell.isInRect(cell, width() - 1, height() - 1);
	}

	public Iter.Int cells() {
		return Cell.Iter2D.of(width(), height());
	}

	public Iter<Building> buildings() {
		return cells().map(this::building).filter(Objects::nonNull);
	}

	public Iter<Unit> units(Team team) {
		return units().filter(u -> team == u.getTeam());
	}

	public Iter<Unit> units() {
		return cells().map(this::unit).filter(Objects::nonNull);
	}

	public Iter<Unit> enemiesSeenBy(Team viewer) {
		return units().filter(u -> u.getTeam() != viewer && isUnitVisible(u.getPos(), viewer));
	}

	private final Supplier<Cell.Bitmap>[] visibleUnitBitmap;
	{
		@SuppressWarnings("unchecked")
		Supplier<Cell.Bitmap>[] visibleUnitBitmap0 = new Supplier[Team.values().length];
		visibleUnitBitmap = visibleUnitBitmap0;
		for (Team viewer : Team.values()) {
			visibleUnitBitmap[viewer.ordinal()] = valuesCache
					.newVal(() -> Cell.Bitmap.fromPredicate(width(), height(), pos -> {
						Unit unit = unit(pos);
						if (unit == null)
							return false;

						if (!unit.type.invisible || unit.getTeam() == viewer)
							return true;
						for (int n : Cell.neighbors(pos)) {
							if (!isValidCell(n))
								continue;
							Unit neighbor = unit(n);
							if (neighbor != null && neighbor.getTeam() == viewer)
								return true;
						}
						return false;
					}));
		}
	}

	private Cell.Bitmap getVisibleUnitBitmap(Team viewer) {
		return visibleUnitBitmap[viewer.ordinal()].get();
	}

	public boolean isUnitVisible(int cell, Team viewer) {
		if (!isValidCell(cell))
			throw new IllegalArgumentException();
		return getVisibleUnitBitmap(viewer).contains(cell);
	}

	public Team getTurn() {
		return turn;
	}

	public int getMoney(Team team) {
		TeamData data = teamData.get(team);
		return data != null ? data.money : 0;
	}

	private void moneyChange(Team team, int delta) {
		TeamData data = teamData.get(team);
		if (data.money + delta < 0)
			throw new IllegalStateException();
		data.money += delta;
		assert data.money >= 0;
		onMoneyChange.notify(new MoneyChange(this, team, delta, data.money));
	}

	public void performAction(Action action0) {
		onAction.notify(new ActionEvent(this, action0));

		if (action0 instanceof Action.Start action) {
			start();
		} else if (action0 instanceof Action.TurnEnd action) {
			turnEnd();
		} else if (action0 instanceof Action.UnitMove action) {
			move(unit(action.source), action.path);
		} else if (action0 instanceof Action.UnitMoveAndAttack action) {
			moveAndAttack(unit(action.attacker), action.path, unit(action.target));
		} else if (action0 instanceof Action.UnitAttackLongRange action) {
			attackRange(unit(action.attacker), unit(action.target));
		} else if (action0 instanceof Action.UnitBuild action) {
			buildUnit(building(action.factory), action.unit);
		} else if (action0 instanceof Action.UnitTransport action) {
			unitTransport(unit(action.unit), action.transport);
		} else if (action0 instanceof Action.UnitTransportFinish action) {
			transportFinish(unit(action.unit));
		} else if (action0 instanceof Action.UnitRepair action) {
			unitRepair(unit(action.unit));
		} else {
			throw new IllegalArgumentException(Objects.toString(action0));
		}
	}

	private void start() {
		turnBegin();
	}

	private void turnBegin() {
		for (Building building : buildings().forEach())
			building.setActive(building.canBeActive() && building.getTeam() == turn);

		for (Unit unit : units().forEach())
			unit.setActive(unit.getTeam() == turn);

		onTurnBegin.notify(new Event(this));
	}

	private void turnEnd() {
		beforeTurnEnd.notify(new Event(this));

		for (Building building : buildings().forEach()) {
			int gain = building.getMoneyGain();
			if (gain != 0 && teamData.containsKey(building.getTeam()))
				moneyChange(building.getTeam(), gain);
		}

		Team prevTurn = turn;
		turn = turnIterator.next();

		/* Conquer buildings */
		for (Building building : buildings().forEach()) {
			Unit unit = unit(building.getPos());
			if (unit != null && unit.type.canConquer) {
				if (unit.getTeam() == turn)
					building.tryConquer(unit);
			} else {
				building.tryConquer(null);
			}
		}

		onTurnEnd.notify(new TurnEnd(this, prevTurn, turn));

		turnBegin();
	}

	private Set<Team> getAliveTeams() {
		Set<Team> alive = EnumSet.noneOf(Team.class);
		for (Team team : Team.realTeams)
			if (units(team).hasNext())
				alive.add(team);
		return alive;
	}

	public boolean isFinished() {
		Set<Team> alive = getAliveTeams();
		winner = alive.size() > 1 ? null : alive.iterator().next();
		return winner != null;
	}

	public Team getWinner() {
		return winner;
	}

	private void move(Unit unit, ListInt path) {
		path = calcRealPath(unit, path);
		if (path.isEmpty() || !isMoveValid(unit, path))
			throw new IllegalStateException(Cell.toString(path));
		move0(unit, path);
		unit.setActive(false);
	}

	private void move0(Unit unit, ListInt path) {
		beforeUnitMove.notify(new UnitMove(this, unit, path));
		int source = unit.getPos();
		int destination = path.last();
		removeUnit(unit);
		setUnit(destination, unit);
		unit.setPos(destination);

		Building oldBuilding = building(source);
		if (oldBuilding != null)
			oldBuilding.tryConquer(null);
		Building newBuilding = building(destination);
		if (newBuilding != null && unit.type.canConquer)
			newBuilding.tryConquer(unit);
	}

	private boolean isMoveValid(Unit unit, ListInt path) {
		return unit.getTeam() == turn && unit.isActive() && unit.isMoveValid(path);
	}

	private static ListInt calcRealPath(Unit unit, ListInt path) {
		Cell.Bitmap passableMap = unit.getPassableMap(false);
		for (int i = 0; i < path.size(); i++)
			if (!passableMap.contains(path.get(i)))
				return path.subList(0, i);
		return path;
	}

	private void moveAndAttack(Unit attacker, ListInt path, Unit target) {
		if (attacker.type.weapon.type != Weapon.Type.CloseRange)
			throw new UnsupportedOperationException("Only close range weapon are supported");

		int last = path.size() > 0 ? path.last() : attacker.getPos();
		if (!Cell.areNeighbors(last, target.getPos()))
			throw new IllegalStateException();

		ListInt realPath = calcRealPath(attacker, path);

		if (!realPath.isEmpty() && !attacker.isMoveValid(realPath))
			throw new IllegalStateException("Invalid path: " + Cell.toString(realPath));
		if (!isAttackValid(attacker, target))
			throw new IllegalStateException();

		if (!realPath.isEmpty())
			move0(attacker, realPath);
		if (realPath.size() == path.size())
			attack(attacker, target);
		attacker.setActive(false);
	}

	private void attackRange(Unit attacker, Unit target) {
		if (attacker.type.weapon.type != Weapon.Type.LongRange)
			throw new UnsupportedOperationException("Only long range weapon are supported");

		if (!isAttackValid(attacker, target))
			throw new IllegalStateException();

		attack(attacker, target);
		attacker.setActive(false);
	}

	private void attack(Unit attacker, Unit target) {
		doDamage(attacker, target);

		if (target.isDead() || !target.canAttack(attacker))
			return;
		boolean attackBack = false;
		switch (target.type.weapon.type) {
		case CloseRange:
			attackBack = ListInt.of(Cell.neighbors(target.getPos())).contains(attacker.getPos());
			break;
		case LongRange:
			attackBack = target.getAttackableMap().contains(attacker.getPos());
			break;
		case None:
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + target.type.weapon.type);
		}
		if (attackBack)
			doDamage(target, attacker);
	}

	public boolean isAttackValid(Unit attacker, Unit target) {
		return attacker.getTeam() == turn && attacker.isActive() && attacker.isAttackValid(target);
	}

	private void doDamage(Unit attacker, Unit target) {
		int damage = attacker.getDamge(target);
		int newHealth = Math.max(0, target.getHealth() - damage);

		beforeUnitAttack.notify(new UnitAttack(this, attacker, target));
		target.setHealth(newHealth);

		if (target.isDead()) {
			removeUnit(target);
			onUnitDeath.notify(new UnitDeath(this, target));
			onUnitRemove.notify(new UnitRemove(this, target));

			if (isFinished())
				onGameEnd.notify(new GameEnd(this, getWinner()));
		}
	}

	private Unit buildUnit(Building factory, Unit.Type unitType) {
		int pos = factory.getPos();
		if (!factory.type.canBuildUnits || !factory.isActive() || unit(pos) != null)
			throw new IllegalStateException();
		if (!unitType.canStandOn(terrain(factory.getPos())))
			throw new IllegalStateException();

		Map<Unit.Type, Building.UnitSale> sales = factory.getAvailableUnits();
		Building.UnitSale sale = sales.get(unitType);
		Team team = factory.getTeam();
		moneyChange(team, -sale.price);

		Unit unit = Unit.valueOf(this, UnitDesc.of(unitType, team), pos);
		setUnit(pos, unit);
		onUnitAdd.notify(new UnitAdd(this, unit));
		onUnitBuy.notify(new UnitBuy(this, unit));

		return unit;
	}

	private Unit unitTransport(Unit transportedUnit, Unit.Type transportType) {
		int pos = transportedUnit.getPos();
		Team team = transportedUnit.getTeam();

		if (!transportedUnit.isActive() || transportedUnit.type.category != Unit.Category.Land)
			throw new IllegalArgumentException();
		if (!transportType.transportUnits || !transportType.canStandOn(terrain(pos)))
			throw new IllegalArgumentException();
		if (transportType.category == Unit.Category.Land && !canBuildLandUnits(team))
			throw new IllegalArgumentException();
		if (EnumSet.of(Unit.Category.Water, Unit.Category.DeepWater).contains(transportType.category)
				&& !canBuildWaterUnits(team))
			throw new IllegalArgumentException();
		if (transportType.category == Unit.Category.Air && !canBuildAirUnits(team))
			throw new IllegalArgumentException();

		moneyChange(team, -transportType.price);

		transportedUnit.setActive(false);
		removeUnit(transportedUnit);
		onUnitRemove.notify(new UnitRemove(this, transportedUnit));

		Unit newUnit = Unit.newTrasportUnit(this, transportType, transportedUnit);
		setUnit(pos, newUnit);
		newUnit.setPos(pos);
		newUnit.setActive(false);

		onUnitAdd.notify(new UnitAdd(this, newUnit));

		return newUnit;
	}

	private Unit transportFinish(Unit trasportUnit) {
		int pos = trasportUnit.getPos();

		if (!trasportUnit.isActive() || !trasportUnit.type.transportUnits)
			throw new IllegalArgumentException();
		Unit transportedUnit = trasportUnit.getTransportedUnit();
		if (!transportedUnit.type.canStandOn(terrain(pos)))
			throw new IllegalArgumentException();

		trasportUnit.setActive(false);
		removeUnit(trasportUnit);
		onUnitRemove.notify(new UnitRemove(this, trasportUnit));

		setUnit(pos, transportedUnit);
		transportedUnit.setPos(pos);
		transportedUnit.setActive(true);

		onUnitAdd.notify(new UnitAdd(this, transportedUnit));

		return transportedUnit;
	}

	private void unitRepair(Unit unit) {
		if (!unit.isActive() || unit.getHealth() == unit.type.health)
			throw new IllegalArgumentException();

		final int cost = 0; // TODO
		moneyChange(unit.getTeam(), -cost);

		unit.setHealth(unit.type.health);
		unit.setActive(false);
	}

	private final Supplier<Boolean>[] canBuildLandUnits;
	private final Supplier<Boolean>[] canBuildWaterUnits;
	private final Supplier<Boolean>[] canBuildAirUnits;
	{
		@SuppressWarnings("unchecked")
		Supplier<Boolean>[] canBuildLandUnits0 = new Supplier[Team.values().length];
		@SuppressWarnings("unchecked")
		Supplier<Boolean>[] canBuildWaterUnits0 = new Supplier[Team.values().length];
		@SuppressWarnings("unchecked")
		Supplier<Boolean>[] canBuildAirUnits0 = new Supplier[Team.values().length];
		canBuildLandUnits = canBuildLandUnits0;
		canBuildWaterUnits = canBuildWaterUnits0;
		canBuildAirUnits = canBuildAirUnits0;
		for (Team team : Team.values()) {
			canBuildLandUnits[team.ordinal()] = valuesCache.newVal(() -> Boolean
					.valueOf(buildings().filter(b -> team == b.getTeam() && b.type.allowUnitBuildLand).hasNext()));
			canBuildWaterUnits[team.ordinal()] = valuesCache.newVal(() -> Boolean
					.valueOf(buildings().filter(b -> team == b.getTeam() && b.type.allowUnitBuildWater).hasNext()));
			canBuildAirUnits[team.ordinal()] = valuesCache.newVal(() -> Boolean
					.valueOf(buildings().filter(b -> team == b.getTeam() && b.type.allowUnitBuildAir).hasNext()));
		}
	}

	public boolean canBuildLandUnits(Team team) {
		return canBuildLandUnits[team.ordinal()].get().booleanValue();
	}

	public boolean canBuildWaterUnits(Team team) {
		return canBuildWaterUnits[team.ordinal()].get().booleanValue();
	}

	public boolean canBuildAirUnits(Team team) {
		return canBuildAirUnits[team.ordinal()].get().booleanValue();
	}

	private static class TeamData {
		int money;

		TeamData(int startingMoney) {
			if (startingMoney < 0)
				throw new IllegalArgumentException();
			money = startingMoney;
		}
	}

	public static class EntityChange extends Event {

		public EntityChange(Entity source) {
			super(Objects.requireNonNull(source));
		}

		public Entity source() {
			return (Entity) source;
		}

	}

	public static class UnitBuy extends Event {

		public final Unit unit;

		public UnitBuy(Game source, Unit unit) {
			super(Objects.requireNonNull(source));
			this.unit = Objects.requireNonNull(unit);
		}

	}

	public static class UnitAdd extends Event {

		public final Unit unit;

		public UnitAdd(Game source, Unit unit) {
			super(Objects.requireNonNull(source));
			this.unit = Objects.requireNonNull(unit);
		}

	}

	public static class UnitDeath extends Event {

		public final Unit unit;

		public UnitDeath(Game source, Unit unit) {
			super(Objects.requireNonNull(source));
			this.unit = Objects.requireNonNull(unit);
		}

	}

	public static class UnitRemove extends Event {

		public final Unit unit;

		public UnitRemove(Game source, Unit unit) {
			super(Objects.requireNonNull(source));
			this.unit = Objects.requireNonNull(unit);
		}

	}

	public static class UnitMove extends Event {

		public final Unit unit;
		public final ListInt path;

		public UnitMove(Game source, Unit unit, ListInt path) {
			super(Objects.requireNonNull(source));
			this.unit = Objects.requireNonNull(unit);
			this.path = path.copy().unmodifiableView();
		}

	}

	public static class UnitAttack extends Event {

		public final Unit attacker;
		public final Unit target;

		public UnitAttack(Game source, Unit attacker, Unit target) {
			super(Objects.requireNonNull(source));
			this.attacker = Objects.requireNonNull(attacker);
			this.target = Objects.requireNonNull(target);
		}

	}

	public static class MoneyChange extends Event {

		public final Team team;
		public final int delta;
		public final int newAmount;

		public MoneyChange(Game source, Team team, int delta, int newAmount) {
			super(Objects.requireNonNull(source));
			this.team = Objects.requireNonNull(team);
			this.delta = delta;
			this.newAmount = newAmount;
		}

	}

	public static class TurnEnd extends Event {

		public final Team prevTurn;
		public final Team nextTurn;

		public TurnEnd(Game source, Team prevTurn, Team nextTurn) {
			super(source);
			this.prevTurn = Objects.requireNonNull(prevTurn);
			this.nextTurn = Objects.requireNonNull(nextTurn);
		}

	}

	public static class GameEnd extends Event {

		public final Team winner;

		public GameEnd(Game source, Team winner) {
			super(Objects.requireNonNull(source));
			this.winner = Objects.requireNonNull(winner);
		}

	}

	public static class ActionEvent extends Event {

		public final Action action;

		public ActionEvent(Game source, Action action) {
			super(source);
			this.action = Objects.requireNonNull(action);
		}

	}

}
