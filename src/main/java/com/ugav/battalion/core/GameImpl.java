package com.ugav.battalion.core;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Unit.Weapon;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ListInt;
import com.ugav.battalion.util.Utils;

class GameImpl implements Game {

	final Arena arena;
	private final Map<Team, TeamData> teamData;
	private final Iterator<Team> turnIterator;
	private Team turn;
	private Team winner;

	final Event.Notifier<UnitAdd> onUnitAdd = new Event.Notifier<>();
	final Event.Notifier<UnitRemove> onUnitRemove = new Event.Notifier<>();
	final Event.Notifier<MoneyChange> onMoneyChange = new Event.Notifier<>();
	final Event.Notifier<Event> onTurnEnd = new Event.Notifier<>();
	final Event.Notifier<GameEnd> onGameEnd = new Event.Notifier<>();

	private GameImpl(Level level) {
		arena = Arena.fromLevel(level);
		teamData = new HashMap<>();
		turnIterator = Utils.iteratorRepeatInfty(Team.realTeams);
		turn = turnIterator.next();
		winner = null;

		for (Team team : Team.realTeams)
			teamData.put(team, new TeamData(level.getStartingMoney(team)));
	}

	private GameImpl(Game game) {
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
		return new GameImpl(level);
	}

	public static Game copyOf(Game game) {
		return new GameImpl(game);
	}

	@Override
	public Arena arena() {
		return arena;
	}

	@Override
	public Team getTurn() {
		return turn;
	}

	@Override
	public int getMoney(Team team) {
		return teamData.get(team).money;
	}

	@Override
	public void start() {
		turnBegin();
	}

	private void turnBegin() {
		for (Building building : arena.buildings().forEach())
			building.setActive(building.canBeActive() && building.getTeam() == turn);

		for (Unit unit : arena.units().forEach())
			unit.setActive(unit.getTeam() == turn);
	}

	@Override
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

	@Override
	public boolean isFinished() {
		Set<Team> alive = getAliveTeams();
		winner = alive.size() > 1 ? null : alive.iterator().next();
		return winner != null;
	}

	@Override
	public Team getWinner() {
		return winner;
	}

	@Override
	public void move(Unit unit, ListInt path) {
		if (path.isEmpty() || !isMoveValid(unit, path))
			throw new IllegalStateException(Cell.toString(path));
		move0(unit, path);
		unit.setActive(false);
	}

	private void move0(Unit unit, ListInt path) {
		int source = unit.getPos();
		int destination = path.last();
		arena.removeUnit(source);
		arena.setUnit(destination, unit);
		unit.setPos(destination);
	}

	private boolean isMoveValid(Unit unit, ListInt path) {
		return unit.getTeam() == turn && unit.isActive() && unit.isMoveValid(path);
	}

	@Override
	public ListInt calcRealPath(Unit unit, ListInt path) {
		Cell.Bitmap passableMap = unit.getPassableMap(false);
		for (int i = 0; i < path.size(); i++) {
			if (!passableMap.contains(path.get(i))) {
				path = path.subList(0, i);
				break;
			}
		}
		return new ListInt.Array(path);
	}

	@Override
	public void moveAndAttack(Unit attacker, ListInt path, Unit target) {
		if (attacker.type.weapon.type != Weapon.Type.CloseRange)
			throw new UnsupportedOperationException("Only close range weapon are supported");

		if (!path.isEmpty() && !attacker.isMoveValid(path))
			throw new IllegalStateException();
		if (!Cell.areNeighbors(path.last(), target.getPos()))
			throw new IllegalStateException();
		if (!isAttackValid(attacker, target))
			throw new IllegalStateException();

		if (!path.isEmpty())
			move0(attacker, path);
		doDamage(attacker, target);
		attacker.setActive(false);
	}

	@Override
	public void attackRange(Unit attacker, Unit target) {
		if (attacker.type.weapon.type != Weapon.Type.LongRange)
			throw new UnsupportedOperationException("Only long range weapon are supported");

		if (!isAttackValid(attacker, target))
			throw new IllegalStateException();

		doDamage(attacker, target);
		attacker.setActive(false);
	}

	@Override
	public boolean isAttackValid(Unit attacker, Unit target) {
		return attacker.getTeam() == turn && attacker.isActive() && attacker.isAttackValid(target);
	}

	private void doDamage(Unit attacker, Unit target) {
		int damage = attacker.getDamge(target);
		if (target.getHealth() <= damage) {
			arena.removeUnit(target.getPos());
			target.setHealth(0);
			onUnitRemove.notify(new UnitRemove(this, target));

			if (isFinished())
				onGameEnd.notify(new GameEnd(this, getWinner()));

		} else {
			target.setHealth(target.getHealth() - damage);
		}
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
	public Event.Notifier<UnitAdd> onUnitAdd() {
		return onUnitAdd;
	}

	@Override
	public Event.Notifier<UnitRemove> onUnitRemove() {
		return onUnitRemove;
	}

	@Override
	public Event.Notifier<MoneyChange> onMoneyChange() {
		return onMoneyChange;
	}

	@Override
	public Event.Notifier<Event> onTurnEnd() {
		return onTurnEnd;
	}

	@Override
	public Event.Notifier<GameEnd> onGameEnd() {
		return onGameEnd;
	}

	private static class TeamData {
		int money;

		TeamData(int startingMoney) {
			if (startingMoney < 0)
				throw new IllegalArgumentException();
			money = startingMoney;
		}
	}

}
