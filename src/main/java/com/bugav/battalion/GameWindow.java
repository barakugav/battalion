package com.bugav.battalion;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;

import com.bugav.battalion.Levels.LevelHandle;
import com.bugav.battalion.computer.Greedy;
import com.bugav.battalion.computer.Player;
import com.bugav.battalion.core.Action;
import com.bugav.battalion.core.Game;
import com.bugav.battalion.core.Team;
import com.bugav.battalion.util.Event;
import com.bugav.battalion.util.Logger;
import com.bugav.battalion.util.Utils;

class GameWindow extends JPanel implements Clearable {

	final LevelHandle level;
	final Globals globals;
	private final GameSideMenu menu;
	final ArenaPanelGame arenaPanel;

	final Game game;
	private final GameStats stats;
	private final Player computer;
	private final Logger computerLogger;
	private final Event.Register register = new Event.Register();

	private final AtomicInteger actionsSuspended = new AtomicInteger();

	private static final long serialVersionUID = 1L;

	GameWindow(Globals globals, LevelHandle level) {
		/* start in a suspended state */
		suspendActions();

		this.globals = Objects.requireNonNull(globals);
		this.level = Objects.requireNonNull(level);

		game = Game.fromLevel(level.level.get());
		stats = new GameStats(game);
		arenaPanel = new ArenaPanelGame(this);
		menu = new GameSideMenu(this);

		computer = new Greedy.Player();
		computerLogger = new Logger.Enabled(globals.logger, () -> globals.debug.logComputerStats);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;

		c.gridx = 0;
		c.gridwidth = 1;
		c.weightx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		add(menu, c);
		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		add(arenaPanel, c);

		menu.initGame();
		arenaPanel.initGame();

		register.register(arenaPanel.animationTask.onAnimationBegin, e -> suspendActions());
		register.register(arenaPanel.animationTask.onAnimationEnd, e -> resumeActions());
		register.register(game.beforeTurnEnd, Utils.swingListener(e -> {
			if (game.getTurn() == ArenaPanelGame.player && !globals.debug.playAllTeams)
				suspendActions();
		}));
		register.register(game.onTurnEnd, Utils.swingListener(e -> {
			if (e.nextTurn == ArenaPanelGame.player && !globals.debug.playAllTeams)
				resumeActions();
		}));
		register.register(game.onGameEnd,
				Utils.swingListener(e -> arenaPanel.showPopup(new GameMenu.GameEndPopup(this, e.winner, stats), 50)));

		Logger gameActionlogger = new Logger.Enabled(globals.logger, () -> globals.debug.logGameActions);
		register.register(game.onAction, e -> gameActionlogger.dbgln(e.action));

		(gameActionsThread = new GameActionsThread()).start();

		gameAction(Action.Start);

		arenaPanel.runMapOverviewAnimation();

		resumeActions();
	}

	private final GameActionsThread gameActionsThread;

	private class GameActionsThread extends Thread {

		private final BlockingQueue<Action> actions = new LinkedBlockingQueue<>();
		private volatile boolean running = true;
		private volatile boolean playComputerTurn;

		@Override
		public void run() {
			while (running) {
				Action action = null;
				try {
					action = actions.poll(100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (action != null) {
					game.performAction(action);
					continue;
				}

				if (playComputerTurn) {
					playComputerTurn = false;
					computerTurn();

				}
			}
		}

		private void computerTurn() {
			final Team us = game.getTurn();
			for (;;) {
				long t0 = System.currentTimeMillis();
				Game visibleGame = Game.modificationOf(game, u -> game.isUnitVisible(u.getPos(), us));
				Action action = computer.chooseAction(visibleGame);
				long t1 = System.currentTimeMillis();
				computerLogger.dbgln("Engine action computed in " + (t1 - t0) + "ms");
				if (action == null || action == Action.TurnEnd) {
					game.performAction(Action.TurnEnd);
					return;
				}
				game.performAction(action);
				assert game.getTurn() == us;
			}
		}

		void setRunning(boolean running) {
			this.running = running;
		}

		void playComputerTurn() {
			playComputerTurn = true;
		}

	}

	void gameAction(Action action) {
		gameActionsThread.actions.add(action);
	}

	@SuppressWarnings("removal")
	@Override
	public void clear() {
		gameActionsThread.setRunning(false);
		register.unregisterAll();
		menu.clear();
		arenaPanel.clear();
		stats.clear();
		try {
			gameActionsThread.join(2 * 1000);
			if (gameActionsThread.isAlive()) {
				System.err.println("Game thread join timeout. stopping it.");
				for (StackTraceElement stackElm : gameActionsThread.getStackTrace())
					System.err.println(stackElm);
				gameActionsThread.stop();
				gameActionsThread.join();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	void endTurn() {
		gameAction(Action.TurnEnd);
		if (!globals.debug.playAllTeams)
			gameActionsThread.playComputerTurn();
	}

	boolean isActionEnabled() {
		return actionsSuspended.get() == 0;
	}

	void suspendActions() {
		if (actionsSuspended.getAndIncrement() == 0)
			; // logger.dbgln("Actions suspended");
	}

	void resumeActions() {
		if (actionsSuspended.decrementAndGet() == 0)
			; // logger.dbgln("Actions resumed");
	}

}
