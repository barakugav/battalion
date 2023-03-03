package com.ugav.battalion;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Game;

class ArenaPanelAnimated extends ArenaPanelGameAbstract {

	private static final long serialVersionUID = 1L;

	ArenaPanelAnimated(Game game, Globals globals) {
		super(game, globals);

		entityLayer().initUI();
		setMapMoveByKeyEnable(false);

		initGame();
		(gameActionsThread = new GameActionsThread()).start();
		gameAction(new Action.Start());
	}

	private final GameActionsThread gameActionsThread;

	private class GameActionsThread extends Thread {

		private final BlockingQueue<Action> actions = new LinkedBlockingQueue<>();
		private volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				Action action = null;
				try {
					action = actions.poll(100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (action != null)
					performAction(action);
			}
		}

		void setRunning(boolean running) {
			this.running = running;
		}

		private void performAction(Action action) {
			action.apply(game);
		}

	}

	private void gameAction(Action action) {
		gameActionsThread.actions.add(action);
	}

	void animatedActions(List<Action> actions) {
		for (Action action : actions)
			gameAction(action);
	}

	@Override
	public void clear() {
		gameActionsThread.setRunning(false);
		super.clear();
	}

}
