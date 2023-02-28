package com.ugav.battalion.core;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Unit.Weapon;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ListInt;
import com.ugav.battalion.util.Utils;

public class Game {

	final Arena arena;
	private final Map<Team, TeamData> teamData;
	private final Iterator<Team> turnIterator;
	private Team turn;
	private Team winner;

	public final Event.Notifier<UnitAdd> onUnitAdd = new Event.Notifier<>();
	public final Event.Notifier<UnitRemove> onUnitRemove = new Event.Notifier<>();
	public final Event.Notifier<UnitMove> beforeUnitMove = new Event.Notifier<>();
	public final Event.Notifier<UnitAttack> beforeUnitAttack = new Event.Notifier<>();
	public final Event.Notifier<MoneyChange> onMoneyChange = new Event.Notifier<>();
	public final Event.Notifier<Event> onTurnEnd = new Event.Notifier<>();
	public final Event.Notifier<GameEnd> onGameEnd = new Event.Notifier<>();

	private Game(Level level) {
		arena = Arena.fromLevel(level);
		teamData = new HashMap<>();
		turnIterator = Utils.iteratorRepeatInfty(Team.realTeams);
		turn = turnIterator.next();
		winner = null;

		for (Team team : Team.realTeams)
			teamData.put(team, new TeamData(level.getStartingMoney(team)));
	}

	private Game(Game game) {
		arena = Arena.copyOf(game.arena());
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

	public Arena arena() {
		return arena;
	}

	public int width() {
		return arena().width();
	}

	public int height() {
		return arena().height();
	}

	public Terrain getTerrain(int cell) {
		return arena().terrain(cell);
	}

	public Unit getUnit(int cell) {
		return arena().unit(cell);
	}

	public Building getBuilding(int cell) {
		return arena().building(cell);
	}

	public Team getTurn() {
		return turn;
	}

	public int getMoney(Team team) {
		return teamData.get(team).money;
	}

	public void start() {
		turnBegin();
	}

	private void turnBegin() {
		for (Building building : arena.buildings().forEach())
			building.setActive(building.canBeActive() && building.getTeam() == turn);

		for (Unit unit : arena.units().forEach())
			unit.setActive(unit.getTeam() == turn);
	}

	public void turnEnd() {
		Set<Team> moneyChanged = new HashSet<>();
		for (Building building : arena.buildings().forEach()) {
			int gain = building.getMoneyGain();
			if (gain != 0 && teamData.containsKey(building.getTeam())) {
				teamData.get(building.getTeam()).money += gain;
				moneyChanged.add(building.getTeam());
			}
		}

		for (Team team : moneyChanged)
			onMoneyChange.notify(new MoneyChange(this, team, teamData.get(team).money));

		turn = turnIterator.next();

		/* Conquer buildings */
		for (Iter.Int it = Cell.Iter2D.of(arena.width(), arena.height()); it.hasNext();) {
			int cell = it.next();
			Building building = arena.building(cell);
			Unit unit = arena.unit(cell);
			if (building == null || unit == null)
				continue;
			if (unit.type.canConquer && unit.getTeam() == turn)
				building.tryConquer(unit.getTeam());
		}

		turnBegin();

		onTurnEnd.notify(new Event(this));
	}

	private Set<Team> getAliveTeams() {
		Set<Team> alive = EnumSet.noneOf(Team.class);
		for (Team team : Team.realTeams)
			if (arena.units(team).hasNext())
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

	public void move(Unit unit, ListInt path) {
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
		arena.removeUnit(source);
		arena.setUnit(destination, unit);
		unit.setPos(destination);
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

	public void moveAndAttack(Unit attacker, ListInt path, Unit target) {
		if (attacker.type.weapon.type != Weapon.Type.CloseRange)
			throw new UnsupportedOperationException("Only close range weapon are supported");

		int last = path.size() > 0 ? path.last() : attacker.getPos();
		if (!Cell.areNeighbors(last, target.getPos()))
			throw new IllegalStateException();

		ListInt realPath = calcRealPath(attacker, path);

		if (!realPath.isEmpty() && !attacker.isMoveValid(realPath))
			throw new IllegalStateException();
		if (!isAttackValid(attacker, target))
			throw new IllegalStateException();

		if (!realPath.isEmpty())
			move0(attacker, realPath);
		if (realPath.size() == path.size())
			doDamage(attacker, target);
		attacker.setActive(false);
	}

	public void attackRange(Unit attacker, Unit target) {
		if (attacker.type.weapon.type != Weapon.Type.LongRange)
			throw new UnsupportedOperationException("Only long range weapon are supported");

		if (!isAttackValid(attacker, target))
			throw new IllegalStateException();

		doDamage(attacker, target);
		attacker.setActive(false);
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
			arena.removeUnit(target.getPos());
			onUnitRemove.notify(new UnitRemove(this, target));

			if (isFinished())
				onGameEnd.notify(new GameEnd(this, getWinner()));
		}
	}

	public Unit buildUnit(Building factory, Unit.Type unitType) {
		int pos = factory.getPos();
		if (!factory.type.canBuildUnits || !factory.isActive() || arena.unit(pos) != null)
			throw new IllegalStateException();

		Map<Unit.Type, Building.UnitSale> sales = factory.getAvailableUnits();
		Building.UnitSale sale = sales.get(unitType);
		Team team = factory.getTeam();
		TeamData data = teamData.get(team);
		if (data.money < sale.price)
			throw new IllegalStateException();

		data.money -= sale.price;
		Unit unit = Unit.valueOf(arena, UnitDesc.of(unitType, team));
		unit.setPos(pos);
		arena.setUnit(pos, unit);

		onMoneyChange.notify(new MoneyChange(this, team, data.money));
		onUnitAdd.notify(new UnitAdd(this, unit));

		return unit;
	}

	public Unit unitTransport(Unit transportedUnit, Unit.Type transportType) {
		int pos = transportedUnit.getPos();

		if (!transportedUnit.isActive() || transportedUnit.type.category != Unit.Category.Land)
			throw new IllegalArgumentException();
		if (!transportType.transportUnits || !transportType.canStandOn(arena.terrain(pos)))
			throw new IllegalArgumentException();

		transportedUnit.setActive(false);
		arena.removeUnit(pos);
		onUnitRemove.notify(new UnitRemove(this, transportedUnit));

		Unit newUnit = Unit.newTrasportUnit(arena, transportType, transportedUnit);
		arena.setUnit(pos, newUnit);
		newUnit.setPos(pos);
		newUnit.setActive(false);

		// TODO money

		onUnitAdd.notify(new UnitAdd(this, newUnit));

		return newUnit;
	}

	public Unit transportFinish(Unit trasportedUnit) {
		int pos = trasportedUnit.getPos();

		if (!trasportedUnit.isActive() || !trasportedUnit.type.transportUnits)
			throw new IllegalArgumentException();
		Unit transportedUnit = trasportedUnit.getTransportedUnit();
		if (!transportedUnit.type.canStandOn(arena.terrain(pos)))
			throw new IllegalArgumentException();

		trasportedUnit.setActive(false);
		arena.removeUnit(pos);
		onUnitRemove.notify(new UnitRemove(this, trasportedUnit));

		arena.setUnit(pos, transportedUnit);
		transportedUnit.setPos(pos);
		transportedUnit.setActive(true);

		onUnitAdd.notify(new UnitAdd(this, transportedUnit));

		return transportedUnit;
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

	public static class UnitAdd extends Event {

		public final Unit unit;

		public UnitAdd(Game source, Unit unit) {
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
		public final int newAmount;

		public MoneyChange(Game source, Team team, int newAmount) {
			super(Objects.requireNonNull(source));
			this.team = Objects.requireNonNull(team);
			this.newAmount = newAmount;
		}

	}

	public static class GameEnd extends Event {

		public final Team winner;

		public GameEnd(Game source, Team winner) {
			super(Objects.requireNonNull(source));
			this.winner = Objects.requireNonNull(winner);
		}

	}

}
