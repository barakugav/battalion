package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.DataEvent;
import com.ugav.battalion.Utils;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Unit.Weapon;

public class Game {

	final Arena arena;
	private final Map<Team, TeamData> teamData;
	private final Iterator<Team> turnIterator;
	private Team turn;
	private Team winner;
	private final List<Unit> deadQueue;

	public final DataChangeNotifier<DataEvent.UnitAdd> onUnitAdd = new DataChangeNotifier<>();
	public final DataChangeNotifier<DataEvent.UnitRemove> onUnitRemove = new DataChangeNotifier<>();
	public final DataChangeNotifier<DataEvent.UnitMove> onBeforeUnitMove = new DataChangeNotifier<>();
	public final DataChangeNotifier<DataEvent.MoneyChange> onMoneyChange = new DataChangeNotifier<>();

	private static class TeamData {
		int money;
	}

	public Game(Level level) {
		arena = new Arena(level);
		teamData = new HashMap<>();
		turnIterator = Utils.iteratorRepeatInfty(Team.realTeams);
		turn = turnIterator.next();
		winner = null;
		deadQueue = new ArrayList<>();
		for (Position pos : arena.positions()) {
			Tile tile = arena.at(pos);
			if (!tile.hasUnit())
				continue;
			Unit u = tile.getUnit();
			u.setPos(pos);
		}

		for (Team team : Team.realTeams)
			teamData.put(team, new TeamData());
	}

	public Arena arena() {
		return arena;
	}

	public int getWidth() {
		return arena.getWidth();
	}

	public int getHeight() {
		return arena.getHeight();
	}

	public Tile getTile(Position pos) {
		return arena.at(pos);
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

	public void turnBegin() {
		/* Conquer buildings */
		for (Tile tile : arena.tiles())
			if (tile.hasBuilding() && tile.hasUnit() && tile.getUnit().type.canConquer
					&& tile.getUnit().getTeam() == turn)
				tile.getBuilding().tryConquer(tile.getUnit().getTeam());

		for (Tile tile : arena.tiles()) {
			if (tile.hasBuilding()) {
				Building building = tile.getBuilding();
				building.setActive(building.canBeActive() && building.getTeam() == turn);
			}
			if (tile.hasUnit()) {
				Unit unit = tile.getUnit();
				unit.setActive(unit.getTeam() == turn);
			}
		}
	}

	public void turnEnd() {
		for (Unit dead : deadQueue)
			dead.clear();
		deadQueue.clear();

		if (isFinished()) {
			Set<Team> alive = getAliveTeams();
			winner = alive.size() == 1 ? alive.iterator().next() : null;
			return;
		}

		Set<Team> moneyChanged = new HashSet<>();
		for (Tile tile : arena.tiles()) {
			if (tile.hasBuilding()) {
				Building building = tile.getBuilding();
				int gain = building.getMoneyGain();
				if (gain != 0 && teamData.containsKey(building.getTeam())) {
					teamData.get(building.getTeam()).money += gain;
					moneyChanged.add(building.getTeam());
				}
			}
		}
		for (Team team : moneyChanged)
			onMoneyChange.notify(new DataEvent.MoneyChange(this, team, teamData.get(team).money));

		turn = turnIterator.next();
	}

	private Set<Team> getAliveTeams() {
		Set<Team> alive = EnumSet.noneOf(Team.class);
		for (Team team : Team.realTeams)
			if (!arena.units(team).isEmpty())
				alive.add(team);
		return alive;
	}

	public boolean isFinished() {
		return getAliveTeams().size() <= 1;
	}

	public Team getWinner() {
		return winner;
	}

	public void move(Unit unit, List<Position> path) {
		if (path.isEmpty() || !isMoveValid(unit, path))
			throw new IllegalStateException();
		move0(unit, path);
		unit.setActive(false);
	}

	private void move0(Unit unit, List<Position> path) {
		onBeforeUnitMove.notify(new DataEvent.UnitMove(this, unit, path));
		Position source = unit.getPos();
		Position destination = path.get(path.size() - 1);
		arena.at(source).removeUnit();
		arena.at(destination).setUnit(unit);
		unit.setPos(destination);
	}

	private boolean isMoveValid(Unit unit, List<Position> path) {
		return unit.getTeam() == turn && unit.isActive() && unit.isMoveValid(path);
	}

	public List<Position> calcRealPath(Unit unit, List<Position> path) {
		Position.Bitmap passableMap = unit.getPassableMap(false);
		for (int i = 0; i < path.size(); i++) {
			if (!passableMap.contains(path.get(i))) {
				path = path.subList(0, i);
				break;
			}
		}
		return new ArrayList<>(path);
	}

	public void moveAndAttack(Unit attacker, List<Position> path, Unit target) {
		if (attacker.type.weapon.type != Weapon.Type.CloseRange)
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
		if (target.getHealth() <= damage) {
			arena.at(target.getPos()).removeUnit();
			target.setHealth(0);
			deadQueue.add(target);
			onUnitRemove.notify(new DataEvent.UnitRemove(this, target));
		} else {
			target.setHealth(target.getHealth() - damage);
		}
	}

	public void buildUnit(Building factory, Unit.Type unitType) {
		Position pos = factory.getPos();
		if (!factory.type.canBuildUnits || !factory.isActive() || arena.at(pos).hasUnit())
			throw new IllegalStateException();

		Map<Unit.Type, Building.UnitSale> sales = factory.getAvailableUnits();
		Building.UnitSale sale = sales.get(unitType);
		Team team = factory.getTeam();
		TeamData data = teamData.get(team);
		if (data.money < sale.price)
			throw new IllegalStateException();

		data.money -= sale.price;
		Unit unit = arena.createUnit(UnitDesc.of(unitType, team), pos);
		arena.at(pos).setUnit(unit);

		onMoneyChange.notify(new DataEvent.MoneyChange(this, team, data.money));
		onUnitAdd.notify(new DataEvent.UnitAdd(this, unit));
	}

}