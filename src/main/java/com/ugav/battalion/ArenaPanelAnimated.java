package com.ugav.battalion;

import java.util.Objects;

import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.util.Iter;

class ArenaPanelAnimated extends ArenaPanelGameAbstract {

	private final Iter<Action> actions;
	private final GameActionsThread gameActionsThread = new GameActionsThread();

	private static final long serialVersionUID = 1L;

	ArenaPanelAnimated(Globals globals, Game game, Iter<Action> actions) {
		super(game, globals);
		this.actions = Objects.requireNonNull(actions);

		entityLayer().initUI();
		setMapMoveByKeyEnable(false);

		initGame();
	}

	void runAnimation() {
		gameActionsThread.start();
	}

	@Override
	public void clear() {
		gameActionsThread.setRunning(false);
		super.clear();
	}

	private class GameActionsThread extends Thread {

		private volatile boolean running = true;

		@Override
		public void run() {
			game.performAction(new Action.Start());
			while (running && actions.hasNext())
				game.performAction(actions.next());
			running = false;
		}

		void setRunning(boolean running) {
			this.running = running;
		}

	}

}
