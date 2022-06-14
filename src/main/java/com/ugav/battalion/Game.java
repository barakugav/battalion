package com.ugav.battalion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ugav.battalion.Level.UnitDesc;
import com.ugav.battalion.Unit.Weapon;

class Game {

	final Arena arena;
	private final Map<Team, TeamData> teamData;
	private final Iterator<Team> turnIterator;
	private Team turn;
	private Team winner;
	private final List<Unit> deadQueue;

	final DataChangeNotifier<DataEvent.NewUnit> onNewUnit;
	final DataChangeNotifier<DataEvent.MoneyChange> onMoneyChange;

	private static class TeamData {
		int money;
	}

	Game(Level level) {
		arena = new Arena(level);
		teamData = new HashMap<>();
		turnIterator = Utils.iteratorRepeatInfty(List.of(Team.values()));
		turn = turnIterator.next();
		winner = null;
		deadQueue = new ArrayList<>();
		onNewUnit = new DataChangeNotifier<>();
		onMoneyChange = new DataChangeNotifier<>();

		for (Position pos : arena.positions()) {
			Tile tile = arena.at(pos);
			if (!tile.hasUnit())
				continue;
			Unit u = tile.getUnit();
			u.setPos(pos);
		}

		for (Team team : Team.values())
			teamData.put(team, new TeamData());
	}

	int getWidth() {
		return arena.getWidth();
	}

	int getHeight() {
		return arena.getHeight();
	}

	Tile getTile(Position pos) {
		return arena.at(pos);
	}

	Team getTurn() {
		return turn;
	}

	int getMoney(Team team) {
		return teamData.get(team).money;
	}

	void start() {
		turnBegin();
	}

	void turnBegin() {
		for (Tile tile : arena.tiles()) {
			if (tile.hasBuilding()) {
				Building building = tile.getBuilding();
				building.setActive(building.getTeam() == turn);
			}
			if (tile.hasUnit()) {
				Unit unit = tile.getUnit();
				unit.setActive(unit.getTeam() == turn);
			}
		}
	}

	void turnEnd() {
		for (Unit dead : deadQueue)
			dead.clear();
		deadQueue.clear();

		if (isFinished()) {
			Set<Team> alive = getAlive();
			winner = alive.size() == 1 ? alive.iterator().next() : null;
			return;
		}

		Set<Team> moneyChanged = new HashSet<>();
		for (Tile tile : arena.tiles()) {
			if (tile.hasBuilding()) {
				Building building = tile.getBuilding();
				int gain = building.getMoneyGain();
				if (gain != 0) {
					teamData.get(building.getTeam()).money += gain;
					moneyChanged.add(building.getTeam());
				}
			}
		}
		for (Team team : moneyChanged)
			onMoneyChange.notify(new DataEvent.MoneyChange(this, team, teamData.get(team).money));

		turn = turnIterator.next();
	}

	private Set<Team> getAlive() {
		Set<Team> alive = new HashSet<>();
		for (Tile tile : arena.tiles())
			if (tile.hasUnit())
				alive.add(tile.getUnit().getTeam());
		return alive;
	}

	boolean isFinished() {
		return getAlive().size() <= 1;
	}

	Team getWinner() {
		return winner;
	}

	void move(Unit unit, List<Position> path) {
		if (!isMoveValid(unit, path))
			throw new IllegalStateException();
		move0(unit, path);
		unit.setActive(false);
	}

	private void move0(Unit unit, List<Position> path) {
		Position source = unit.getPos();
		Position destination = path.get(path.size() - 1);
		arena.at(source).removeUnit();
		arena.at(destination).setUnit(unit);
		unit.setPos(destination);
	}

	boolean isMoveValid(Unit unit, List<Position> path) {
		return unit.getTeam() == turn && unit.isActive() && unit.isMoveValid(path);
	}

	void moveAndAttack(Unit attacker, List<Position> path, Unit target) {
		if (attacker.type.weapon != Weapon.CloseRange)
			throw new UnsupportedOperationException("Only close range weapon are supported");

		if (!path.isEmpty() && !attacker.isMoveValid(path))
			throw new IllegalStateException();
		if (!isAttackValid(attacker, target))
			throw new IllegalStateException();

		if (!path.isEmpty())
			move0(attacker, path);
		doDamage(attacker, target);
		attacker.setActive(false);
	}

	void attackRange(Unit attacker, Unit target) {
		if (attacker.type.weapon != Weapon.LongRange)
			throw new UnsupportedOperationException("Only long range weapon are supported");

		if (!isAttackValid(attacker, target))
			throw new IllegalStateException();

		doDamage(attacker, target);
		attacker.setActive(false);
	}

	boolean isAttackValid(Unit attacker, Unit target) {
		return attacker.getTeam() == turn && attacker.isActive() && attacker.isAttackValid(target);
	}

	private void doDamage(Unit attacker, Unit target) {
		int damage = attacker.getDamge(target);
		if (target.getHealth() <= damage) {
			target.setHealth(0);
			deadQueue.add(target);
			arena.at(target.getPos()).removeUnit();
		} else {
			target.setHealth(target.getHealth() - damage);
		}
	}

	void buildUnit(Building.Factory factory, Unit.Type unitType) {
		Position pos = factory.getPos();
		if (!factory.isActive() || arena.at(pos).hasUnit())
			throw new IllegalStateException();

		List<Building.Factory.UnitSale> sales = factory.getAvailableUnits();
		Building.Factory.UnitSale sale = null;
		for (Building.Factory.UnitSale s : sales)
			if (s.type == unitType)
				sale = s;
		if (sale == null)
			throw new IllegalStateException();
		Team team = factory.getTeam();
		TeamData data = teamData.get(team);
		if (data.money < sale.price)
			throw new IllegalStateException();

		data.money -= sale.price;
		Unit unit = arena.createUnit(UnitDesc.of(unitType, team), pos);
		arena.at(pos).setUnit(unit);

		onMoneyChange.notify(new DataEvent.MoneyChange(this, team, data.money));
		onNewUnit.notify(new DataEvent.NewUnit(this, unit));
	}

}
