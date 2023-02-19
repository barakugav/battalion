package com.ugav.battalion;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import com.ugav.battalion.core.Arena;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Tile;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.core.Unit.Type;

class GameGUIImpl implements Game {

	private final Game game;
	private final GameWindow gui;

	GameGUIImpl(GameWindow gui, Level level) {
		this.gui = Objects.requireNonNull(gui);
		game = Game.newInstance(level);
	}

	@Override
	public Arena arena() {
		return game.arena();
	}

	@Override
	public int width() {
		return game.width();
	}

	@Override
	public int height() {
		return game.height();
	}

	@Override
	public Tile getTile(Position pos) {
		return game.getTile(pos);
	}

	@Override
	public Team getTurn() {
		return game.getTurn();
	}

	@Override
	public int getMoney(Team team) {
		return game.getMoney(team);
	}

	@Override
	public void start() {
		checkCorrectThread();
		run(() -> game.start());
	}

	@Override
	public void turnEnd() {
		checkCorrectThread();
		run(() -> game.turnEnd());
	}

	@Override
	public boolean isFinished() {
		return game.isFinished();
	}

	@Override
	public Team getWinner() {
		return game.getWinner();
	}

	@Override
	public List<Position> calcRealPath(Unit unit, List<Position> path) {
		return game.calcRealPath(unit, path);
	}

	@Override
	public void move(Unit unit, List<Position> path) {
		checkCorrectThread();
		List<Position> animationPath = Utils.listOf(unit.getPos(), path);
		gui.arenaPanel.animateUnitMove(unit, animationPath, () -> game.move(unit, path));
	}

	@Override
	public void moveAndAttack(Unit attacker, List<Position> path, Unit target) {
		checkCorrectThread();
		List<Position> animationPath = Utils.listOf(attacker.getPos(), path);
		gui.arenaPanel.animateUnitMoveAndAttack(attacker, animationPath, target.getPos(),
				() -> game.moveAndAttack(attacker, path, target));
	}

	@Override
	public void attackRange(Unit attacker, Unit target) {
		checkCorrectThread();
		run(() -> game.attackRange(attacker, target));
	}

	@Override
	public boolean isAttackValid(Unit attacker, Unit target) {
		return game.isAttackValid(attacker, target);
	}

	@Override
	public Unit buildUnit(Building factory, Type unitType) {
		checkCorrectThread();
		return run(() -> game.buildUnit(factory, unitType));
	}

	@Override
	public Unit unitTransport(Unit transportedUnit, Unit.Type transportType) {
		checkCorrectThread();
		return run(() -> game.unitTransport(transportedUnit, transportType));
	}

	@Override
	public Unit transportFinish(Unit trasportedUnit) {
		checkCorrectThread();
		return run(() -> game.transportFinish(trasportedUnit));
	}

	@Override
	public DataChangeNotifier<UnitAdd> onUnitAdd() {
		return game.onUnitAdd();
	}

	@Override
	public DataChangeNotifier<UnitRemove> onUnitRemove() {
		return game.onUnitRemove();
	}

	@Override
	public DataChangeNotifier<MoneyChange> onMoneyChange() {
		return game.onMoneyChange();
	}

	@Override
	public DataChangeNotifier<DataEvent> onTurnEnd() {
		return game.onTurnEnd();
	}

	@Override
	public DataChangeNotifier<GameEnd> onGameEnd() {
		return game.onGameEnd();
	}

	private static void checkCorrectThread() {
		if (SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Can't change the data from GUI thread");
	}

	private static void run(Runnable runnable) {
		try {
			SwingUtilities.invokeAndWait(runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static <R> R run(Supplier<R> runnable) {
		Utils.Holder<R> holder = new Utils.Holder<>();
		run(() -> {
			holder.val = runnable.get();
		});
		return holder.val;
	}

}
