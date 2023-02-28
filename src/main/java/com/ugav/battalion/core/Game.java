package com.ugav.battalion.core;

import java.util.Objects;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.DataEvent;
import com.ugav.battalion.util.ListInt;

public interface Game {

	public Arena arena();

	default int width() {
		return arena().width();
	}

	default int height() {
		return arena().height();
	}

	default Terrain getTerrain(int cell) {
		return arena().terrain(cell);
	}

	default Unit getUnit(int cell) {
		return arena().unit(cell);
	}

	default Building getBuilding(int cell) {
		return arena().building(cell);
	}

	public Team getTurn();

	public int getMoney(Team team);

	public void start();

	public void turnEnd();

	public boolean isFinished();

	public Team getWinner();

	public ListInt calcRealPath(Unit unit, ListInt path);

	public void move(Unit unit, ListInt path);

	public void moveAndAttack(Unit attacker, ListInt path, Unit target);

	public void attackRange(Unit attacker, Unit target);

	public boolean isAttackValid(Unit attacker, Unit target);

	public Unit buildUnit(Building factory, Unit.Type unitType);

	public Unit unitTransport(Unit transportedUnit, Unit.Type transportType);

	public Unit transportFinish(Unit trasportedUnit);

	public DataChangeNotifier<UnitAdd> onUnitAdd();

	public DataChangeNotifier<UnitRemove> onUnitRemove();

	public DataChangeNotifier<MoneyChange> onMoneyChange();

	public DataChangeNotifier<DataEvent> onTurnEnd();

	public DataChangeNotifier<GameEnd> onGameEnd();

	public static Game newInstance(Level level) {
		return GameImpl.fromLevel(level);
	}

	public static Game copyOf(Game game) {
		return GameImpl.copyOf(game);
	}

	public static class EntityChange extends DataEvent {

		public EntityChange(Entity source) {
			super(Objects.requireNonNull(source));
		}

		public Entity source() {
			return (Entity) source;
		}

	}

	public static class UnitAdd extends DataEvent {

		public final Unit unit;

		public UnitAdd(Game source, Unit unit) {
			super(Objects.requireNonNull(source));
			this.unit = Objects.requireNonNull(unit);
		}

	}

	public static class UnitRemove extends DataEvent {

		public final Unit unit;

		public UnitRemove(Game source, Unit unit) {
			super(Objects.requireNonNull(source));
			this.unit = Objects.requireNonNull(unit);
		}

	}

	public static class MoneyChange extends DataEvent {

		public final Team team;
		public final int newAmount;

		public MoneyChange(Game source, Team team, int newAmount) {
			super(Objects.requireNonNull(source));
			this.team = Objects.requireNonNull(team);
			this.newAmount = newAmount;
		}

	}

	public static class GameEnd extends DataEvent {

		public final Team winner;

		public GameEnd(Game source, Team winner) {
			super(Objects.requireNonNull(source));
			this.winner = Objects.requireNonNull(winner);
		}

	}

}
