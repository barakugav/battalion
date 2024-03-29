package com.bugav.battalion.core;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.bugav.battalion.core.Building.ConquerEvent;
import com.bugav.battalion.core.Level.BuildingDesc;
import com.bugav.battalion.core.Level.UnitDesc;
import com.bugav.battalion.core.Unit.Weapon;
import com.bugav.battalion.util.Event;
import com.bugav.battalion.util.Iter;
import com.bugav.battalion.util.ListInt;
import com.bugav.battalion.util.Utils;
import com.bugav.battalion.util.ValuesCache;

public class Game {

	private final Cell.Array<Terrain> terrains;
	private final Cell.Array<Unit> units;
	private final Cell.Array<Building> buildings;

	final ValuesCache unitsCache = new ValuesCache();
	final ValuesCache buildingsCache = new ValuesCache();

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
	public final Event.Notifier<TeamEliminateEvent> onTeamElimination = new Event.Notifier<>();
	public final Event.Notifier<GameEnd> onGameEnd = new Event.Notifier<>();
	public final Event.Notifier<ActionEvent> onAction = new Event.Notifier<>();
	public final Event.Notifier<Event> onActionEnd = new Event.Notifier<>();

	private Game(Level level) {
		int w = level.width(), h = level.height();
		terrains = Cell.Array.fromFunc(w, h, cell -> level.terrain(cell));
		units = Cell.Array.fromFunc(w, h, pos -> {
			UnitDesc desc = level.unit(pos);
			return desc != null ? Unit.valueOf(this, desc, pos) : null;
		});
		buildings = Cell.Array.fromFunc(w, h, pos -> {
			BuildingDesc desc = level.building(pos);
			return desc != null ? Building.valueOf(this, desc, pos) : null;
		});

		teamData = new HashMap<>();
		turnIterator = Utils.iteratorRepeatInfty(List.of(Team.values()));
		turn = turnIterator.next();
		winner = null;

		for (Team team : Team.values())
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
		turnIterator = Utils.iteratorRepeatInfty(List.of(Team.values()));
		while (!(turn = turnIterator.next()).equals(game.getTurn()))
			;
		winner = game.winner;

		for (Team team : Team.values())
			teamData.put(team, new TeamData(game.getMoney(team)));
	}

	public static Game fromLevel(Level level) {
		return new Game(level);
	}

	public static Game copyOf(Game game) {
		return new Game(game);
	}

	public static Game modificationOf(Game game, Predicate<Unit> unitsFilter) {
		Game ngame = copyOf(game);
		for (Unit unit : ngame.units().toList())
			if (!unitsFilter.test(unit))
				ngame.removeUnit(unit);
		return ngame;
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
		unit.setPos(cell);
		if (unit.type.transportUnits)
			unit.getTransportedUnit().setPos(cell);
		unitsCache.invalidate();
	}

	private void removeUnit(Unit unit) {
		int pos = unit.getPos();
		assert unit == this.unit(pos);
		units.set(pos, null);
		unitsCache.invalidate();
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

	public Iter<Building> buildings(Team team) {
		return buildings().filter(b -> team == b.getTeam());
	}

	public Iter<Unit> units() {
		return cells().map(this::unit).filter(Objects::nonNull);
	}

	public Iter<Unit> units(Team team) {
		return units().filter(u -> team == u.getTeam());
	}

	public Iter<Unit> unitsSeenBy(Team viewer) {
		return units().filter(u -> isUnitVisible(u.getPos(), viewer));
	}

	public Iter<Unit> enemiesSeenBy(Team viewer) {
		return units().filter(u -> u.getTeam() != viewer && isUnitVisible(u.getPos(), viewer));
	}

	private final Map<Team, Supplier<Cell.Bitmap>> visibleUnitBitmap = new EnumMap<>(Team.class);
	{
		for (Team viewer : Team.values()) {
			visibleUnitBitmap.put(viewer, unitsCache.newVal(() -> Cell.Bitmap.fromPredicate(width(), height(), pos -> {
				Unit unit = unit(pos);
				if (unit == null)
					return false;

				if (!unit.type.invisible || unit.getTeam() == viewer)
					return true;
				for (Iter.Int nit = Cell.neighbors(pos); nit.hasNext();) {
					int n = nit.next();
					if (!isValidCell(n))
						continue;
					Unit neighbor = unit(n);
					if (neighbor != null && neighbor.getTeam() == viewer)
						return true;
				}
				return false;
			})));
		}
	}

	private Cell.Bitmap getVisibleUnitBitmap(Team viewer) {
		return visibleUnitBitmap.get(viewer).get();
	}

	public boolean isUnitVisible(int cell, Team viewer) {
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

		try {
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
			onActionEnd.notify(new Event(this));
		} catch (GameEndException e) {
			onGameEnd.notify(new GameEnd(this, getWinner()));
		}
	}

	private void start() {
		turnBegin(turn);
	}

	private void turnBegin(Team newTurn) {
		for (Building building : buildings().forEach())
			building.setActive(building.canBeActive() && building.getTeam() == newTurn);

		for (Unit unit : units().forEach())
			unit.setActive(unit.getTeam() == newTurn);

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
		Team nextTurn = turnIterator.next();

		/* Conquer buildings */
		for (Building building : buildings().forEach()) {
			Unit unit = unit(building.getPos());
			if (unit != null && unit.type.canConquer) {
				if (unit.getTeam() == nextTurn)
					building.tryConquer(unit);
			} else {
				building.tryConquer(null);
			}
		}

		/* Repair units */
		for (Unit unit : units(nextTurn).filter(Unit::isRepairing).forEach()) {
			unit.setHealth(unit.getHealth() + unit.repairAmount());
			unit.setRepairing(false);
		}

		turnBegin(nextTurn);

		turn = nextTurn;
		onTurnEnd.notify(new TurnEnd(this, prevTurn, turn));
	}

	private final Map<Team, BooleanSupplier> isTeamAlive = new EnumMap<>(Team.class);
	{
		for (Team team : Team.values())
			isTeamAlive.put(team, unitsCache.newValBool(() -> units(team).hasNext()));
	}

	private boolean isTeamAlive(Team team) {
		return isTeamAlive.get(team).getAsBoolean();
	}

	public boolean isFinished() {
		Set<Team> alive = EnumSet.noneOf(Team.class);
		for (Team team : Team.values())
			if (isTeamAlive(team))
				alive.add(team);
		assert !alive.isEmpty();
		winner = alive.size() > 1 ? null : alive.iterator().next();
		return winner != null;
	}

	public Team getWinner() {
		if (!isFinished())
			throw new IllegalStateException();
		return winner;
	}

	void eliminateTeam(Team team) {
		onTeamElimination.notify(new TeamEliminateEvent(this, team));

		for (Unit unit : units(team).forEach()) {
			unit.setHealth(0);
			removeUnit(unit);
			onUnitRemove.notify(new UnitRemove(this, unit));
		}
		for (Building building : buildings(team).forEach())
			building.setTeam(null);

		if (isFinished())
			throw new GameEndException();
	}

	private void move(Unit unit, ListInt path) {
		ListInt realPath = calcRealPath(unit, path);
		if (realPath.isEmpty() || !isMoveValid(unit, realPath))
			throw new IllegalStateException(Cell.toString(realPath));
		move0(unit, realPath, path);
		unit.setActive(false);
	}

	private void move0(Unit unit, ListInt path, ListInt plannedPath) {
		beforeUnitMove.notify(new UnitMove(this, unit, path, plannedPath));
		int source = unit.getPos();
		int destination = path.last();
		removeUnit(unit);
		setUnit(destination, unit);

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
			move0(attacker, realPath, path);
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
		boolean attackBack = !target.isRepairing();
		doDamage(attacker, target);

		if (target.isDead() || !target.canAttack(attacker.type))
			return;
		switch (target.type.weapon.type) {
		case CloseRange:
			attackBack = attackBack && Cell.neighbors(target.getPos()).contains(attacker.getPos());
			break;
		case LongRange:
			attackBack = attackBack && target.getAttackableMap().contains(attacker.getPos());
			break;
		case None:
			attackBack = false;
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
		target.setRepairing(false);
		if (target.isDead()) {
			onUnitDeath.notify(new UnitDeath(this, attacker, target));
			removeUnit(target);
			onUnitRemove.notify(new UnitRemove(this, target));

			if (!isTeamAlive(target.getTeam()))
				eliminateTeam(target.getTeam());
		}
	}

	private Unit buildUnit(Building factory, Unit.Type unitType) {
		if (!factory.canBuildUnit(unitType))
			throw new IllegalStateException();

		Map<Unit.Type, Building.UnitSale> sales = factory.getAvailableUnits();
		Building.UnitSale sale = sales.get(unitType);
		Team team = factory.getTeam();
		moneyChange(team, -sale.price);

		int pos = factory.getPos();
		Unit unit = Unit.valueOf(this, UnitDesc.of(unitType, team), pos);
		setUnit(pos, unit);
		unit.setActive(false);
		onUnitAdd.notify(new UnitAdd(this, unit));
		onUnitBuy.notify(new UnitBuy(this, unit));

		return unit;
	}

	private Unit unitTransport(Unit transportedUnit, Unit.Type transportType) {
		if (!transportedUnit.canTransported(transportType))
			throw new IllegalArgumentException(transportedUnit.toString());

		Team team = transportedUnit.getTeam();
		moneyChange(team, -transportType.price);

		transportedUnit.setActive(false);
		removeUnit(transportedUnit);
		onUnitRemove.notify(new UnitRemove(this, transportedUnit));

		Unit newUnit = Unit.newTrasportUnit(this, transportType, transportedUnit);
		int pos = transportedUnit.getPos();
		setUnit(pos, newUnit);
		newUnit.setActive(false);

		onUnitAdd.notify(new UnitAdd(this, newUnit));

		return newUnit;
	}

	private Unit transportFinish(Unit trasportUnit) {
		if (!trasportUnit.canFinishTransport())
			throw new IllegalArgumentException();

		trasportUnit.setActive(false);
		removeUnit(trasportUnit);
		onUnitRemove.notify(new UnitRemove(this, trasportUnit));

		Unit transportedUnit = trasportUnit.getTransportedUnit();
		int pos = trasportUnit.getPos();
		setUnit(pos, transportedUnit);
		transportedUnit.setActive(true);

		onUnitAdd.notify(new UnitAdd(this, transportedUnit));

		return transportedUnit;
	}

	private void unitRepair(Unit unit) {
		if (!unit.canRepair())
			throw new IllegalArgumentException();

		final int cost = unit.getRepairCost();
		moneyChange(unit.getTeam(), -cost);

		unit.setRepairing(true);
		unit.setActive(false);
	}

	private final Map<Team, BooleanSupplier> canBuildLandUnits = new EnumMap<>(Team.class);
	private final Map<Team, BooleanSupplier> canBuildWaterUnits = new EnumMap<>(Team.class);
	private final Map<Team, BooleanSupplier> canBuildAirUnits = new EnumMap<>(Team.class);
	{
		for (Team team : Team.values()) {
			canBuildLandUnits.put(team, buildingsCache.newValBool(
					() -> (buildings().filter(b -> team == b.getTeam() && b.type.allowUnitBuildLand).hasNext())));
			canBuildWaterUnits.put(team, buildingsCache.newValBool(
					() -> (buildings().filter(b -> team == b.getTeam() && b.type.allowUnitBuildWater).hasNext())));
			canBuildAirUnits.put(team, buildingsCache.newValBool(
					() -> (buildings().filter(b -> team == b.getTeam() && b.type.allowUnitBuildAir).hasNext())));
		}
	}

	public boolean canBuildLandUnits(Team team) {
		return canBuildLandUnits.get(team).getAsBoolean();
	}

	public boolean canBuildWaterUnits(Team team) {
		return canBuildWaterUnits.get(team).getAsBoolean();
	}

	public boolean canBuildAirUnits(Team team) {
		return canBuildAirUnits.get(team).getAsBoolean();
	}

	public boolean canBuildUnitType(Team team, Unit.Type type) {
		switch (type.category) {
		case Land:
			return canBuildLandUnits(team);
		case Water:
		case DeepWater:
			return canBuildWaterUnits(team);
		case Air:
			return canBuildAirUnits(team);
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		}
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

		public final Unit attacker;
		public final Unit unit;

		public UnitDeath(Game source, Unit attacker, Unit unit) {
			super(Objects.requireNonNull(source));
			this.attacker = Objects.requireNonNull(attacker);
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
		public final ListInt plannedPath;

		public UnitMove(Game source, Unit unit, ListInt path, ListInt plannedPath) {
			super(Objects.requireNonNull(source));
			this.unit = Objects.requireNonNull(unit);
			this.path = path.copy().unmodifiableView();
			this.plannedPath = plannedPath.copy().unmodifiableView();
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

	public static class TeamEliminateEvent extends Event {
		public final Team team;

		public TeamEliminateEvent(Game source, Team team) {
			super(source);
			this.team = Objects.requireNonNull(team);
		}

	}

	private static class GameEndException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		/**
		 * The exception is used in the valid flow to 'exit' from the current stack
		 * execution in case of a game end. A bit ugly but greatly simplify the code.
		 */
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
