package com.ugav.battalion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ugav.battalion.Level.UnitDesc;
import com.ugav.battalion.Unit.Category;

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

	void move(Position source, Position target) {
		if (!isMoveValid(source, target))
			throw new IllegalStateException();
		move0(source, target);
		arena.at(target).getUnit().setActive(false);
	}

	private void move0(Position source, Position target) {
		Tile from = arena.at(source);
		Unit unit = from.getUnit();
		from.removeUnit();
		arena.at(target).setUnit(unit);
		unit.setPos(target);
	}

	boolean isMoveValid(Position source, Position target) {
		Tile from = arena.at(source);
		Tile to = arena.at(target);
		if (!from.hasUnit() || to.hasUnit())
			return false;
		Unit unit = from.getUnit();
		return unit.getTeam() == turn && unit.isActive() && unit.isMoveValid(target);
	}

	void moveAndAttack(Position attackerPos, /* Position moveTarget, */ Position attackedPos) {
		Unit attacker = arena.at(attackerPos).getUnit();
		Unit target = arena.at(attackedPos).getUnit();
		if (!(attacker.type.category == Category.Land))
			throw new UnsupportedOperationException();

		if (!isAttackValid(attackerPos, attackedPos))
			throw new IllegalStateException();
		Position moveTarget = attacker.getMovePositionToAttack(attackedPos);

		boolean moveNearTarget = false;
		for (Position neighbor : attackedPos.neighbors()) {
			if (arena.isValidPos(neighbor) && neighbor.equals(moveTarget)) {
				moveNearTarget = true;
				break;
			}
		}
		if (!moveNearTarget)
			throw new UnsupportedOperationException();

		move0(attackerPos, moveTarget);
		doDamage(attacker, target);
		attacker.setActive(false);
	}

	void attackRange(Position attackerPos, Position targetPos) {
		Unit attacker = arena.at(attackerPos).getUnit();
		Unit target = arena.at(targetPos).getUnit();
//		if (!(attacker instanceof LongRangeUnit))
//			throw new UnsupportedOperationException();
		if (!isAttackValid(attackerPos, targetPos))
			throw new IllegalStateException();
		doDamage(attacker, target);
		attacker.setActive(false);
	}

	boolean isAttackValid(Position attackerPos, Position targetPos) {
		Tile attackerTile = arena.at(attackerPos);
		Tile targetTile = arena.at(targetPos);
		if (!attackerTile.hasUnit() || !targetTile.hasUnit())
			return false;
		Unit attacker = attackerTile.getUnit();
		return attacker.getTeam() == turn && attacker.isActive() && attacker.getTeam() != targetTile.getUnit().getTeam()
				&& attacker.isAttackValid(targetPos);
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
