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
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.core.Unit.Type;
import com.ugav.battalion.util.Utils;

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
	public List<Integer> calcRealPath(Unit unit, List<Integer> path) {
		return game.calcRealPath(unit, path);
	}

	@Override
	public void move(Unit unit, List<Integer> path) {
		checkCorrectThread();
		List<Integer> realPath = game.calcRealPath(unit, path);
		List<Integer> animationPath = Utils.listOf(unit.getPos(), realPath);
		run(() -> gui.arenaPanel.animateUnitMove(unit, animationPath, () -> game.move(unit, realPath)));
	}

	@Override
	public void moveAndAttack(Unit attacker, List<Integer> path, Unit target) {
		checkCorrectThread();
		List<Integer> realPath = game.calcRealPath(attacker, path);
		List<Integer> animationPath = Utils.listOf(attacker.getPos(), realPath);
		if (path.size() == realPath.size())
			run(() -> gui.arenaPanel.animateUnitMoveAndAttack(attacker, animationPath, target.getPos(),
					() -> game.moveAndAttack(attacker, path, target)));
		else
			run(() -> gui.arenaPanel.animateUnitMove(attacker, animationPath, () -> game.move(attacker, realPath)));
	}

	@Override
	public void attackRange(Unit attacker, Unit target) {
		checkCorrectThread();
		run(() -> gui.arenaPanel.animateUnitAttackRange(attacker, target.getPos(),
				() -> game.attackRange(attacker, target)));
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

	private void run(Runnable runnable) {
		try {
			SwingUtilities.invokeAndWait(runnable);
			while (gui.arenaPanel.isAnimationActive())
				;
		} catch (InvocationTargetException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private <R> R run(Supplier<R> runnable) {
		Utils.Holder<R> holder = new Utils.Holder<>();
		run(() -> {
			holder.val = runnable.get();
		});
		return holder.val;
	}

}
