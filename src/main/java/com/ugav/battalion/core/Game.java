package com.ugav.battalion.core;

import java.util.List;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.DataEvent;

public interface Game {

	public Arena arena();

	public int getWidth();

	public int getHeight();

	public Tile getTile(Position pos);

	public Team getTurn();

	public int getMoney(Team team);

	public void start();

	public void turnBegin();

	public void turnEnd();

	public boolean isFinished();

	public Team getWinner();

	public List<Position> calcRealPath(Unit unit, List<Position> path);

	public void move(Unit unit, List<Position> path);

	public void moveAndAttack(Unit attacker, List<Position> path, Unit target);

	public void attackRange(Unit attacker, Unit target);

	public boolean isAttackValid(Unit attacker, Unit target);

	public void buildUnit(Building factory, Unit.Type unitType);

	public DataChangeNotifier<DataEvent.UnitAdd> onUnitAdd();

	public DataChangeNotifier<DataEvent.UnitRemove> onUnitRemove();

	public DataChangeNotifier<DataEvent.UnitMove> onBeforeUnitMove();

	public DataChangeNotifier<DataEvent.MoneyChange> onMoneyChange();

	public static Game newInstance(Level level) {
		return new GameImpl(level);
	}

}
