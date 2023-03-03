package com.ugav.battalion;

import java.util.Objects;

import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.util.Iter;

class ArenaPanelAnimated extends ArenaPanelGameAbstract {

	private final Iter<Action> actions;

	private static final long serialVersionUID = 1L;

	ArenaPanelAnimated(Globals globals, Game game, Iter<Action> actions) {
		super(game, globals);
		this.actions = Objects.requireNonNull(actions);

		entityLayer().initUI();
		setMapMoveByKeyEnable(false);

		initGame();
		(gameActionsThread = new GameActionsThread()).start();
	}

	private final GameActionsThread gameActionsThread;

	private class GameActionsThread extends Thread {

		private volatile boolean running = true;

		@Override
		public void run() {
			new Action.Start().apply(game);
			while (running && actions.hasNext())
				performAction(actions.next());
			running = false;
		}

		void setRunning(boolean running) {
			this.running = running;
		}

		private void performAction(Action action) {
			action.apply(game);
		}

	}

	@Override
	public void clear() {
		gameActionsThread.setRunning(false);
		super.clear();
	}

}
