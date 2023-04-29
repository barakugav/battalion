package com.bugav.battalion;

import java.util.Iterator;
import java.util.Objects;

import com.bugav.battalion.core.Action;
import com.bugav.battalion.core.Game;

class ArenaPanelAnimated extends ArenaPanelGameAbstract {

	private final Iterator<Action> actions;
	private final GameActionsThread gameActionsThread = new GameActionsThread();

	private static final long serialVersionUID = 1L;

	ArenaPanelAnimated(Globals globals, Game game, Iterator<Action> actions) {
		super(game, globals);
		this.actions = Objects.requireNonNull(actions);

		entityLayer().initUI();
		setMapMoveByUserEnable(false);

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

		private volatile boolean running;

		@Override
		public void start() {
			running = true;
			super.start();
		}

		@Override
		public void run() {
			game.performAction(Action.Start);
			while (running && actions.hasNext())
				game.performAction(actions.next());
			running = false;
		}

		void setRunning(boolean running) {
			this.running = running;
		}

	}

}
