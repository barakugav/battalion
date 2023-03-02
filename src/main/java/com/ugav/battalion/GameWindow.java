package com.ugav.battalion;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.ugav.battalion.computer.Player;
import com.ugav.battalion.computer.PlayerMiniMaxAlphaBeta;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Logger;
import com.ugav.battalion.util.Utils;

class GameWindow extends JPanel implements Clearable {

	final Globals globals;
	private final GameSideMenu menu;
	final ArenaPanelGame arenaPanel;

	final Game game;
//	private final Player computer = new Player.Random();
	private final Player computer = new PlayerMiniMaxAlphaBeta();
	private final Logger logger = new Logger(true); // TODO
	private final Event.Register register = new Event.Register();

	private final AtomicInteger actionsSuspended = new AtomicInteger();

	private static final long serialVersionUID = 1L;

	GameWindow(Globals globals, Level level) {
		this.globals = Objects.requireNonNull(globals);

		if (level.width() < ArenaPanelGame.DISPLAYED_ARENA_WIDTH
				|| level.height() < ArenaPanelGame.DISPLAYED_ARENA_HEIGHT)
			throw new IllegalArgumentException("level size is too small");
		game = Game.fromLevel(level);
		arenaPanel = new ArenaPanelGame(this);
		menu = new GameSideMenu(this);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;

		c.gridx = 0;
		c.gridwidth = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		add(menu, c);
		c.gridx = 5;
		c.gridwidth = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		add(arenaPanel, c);

		menu.initGame();
		arenaPanel.initGame();

		register.register(game.onGameEnd, Utils.swingListener(e -> {
			logger.dbgln("Game finished");
			JOptionPane.showMessageDialog(this, "Winner: " + e.winner);
			suspendActions();
			// TODO
		}));

		(gameActionsThread = new GameActionsThread()).start();

		gameAction(() -> game.start());
	}

	private final GameActionsThread gameActionsThread;

	private class GameActionsThread extends Thread {

		final BlockingQueue<Runnable> actions = new LinkedBlockingQueue<>();
		volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				Runnable action = null;
				try {
					action = actions.poll(100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (action == null)
					continue;

				action.run();
			}
		}

	}

	void gameAction(Runnable action) {
		gameActionsThread.actions.add(action);
	}

	@Override
	public void clear() {
		register.unregisterAll();
		menu.clear();
		arenaPanel.clear();
		gameActionsThread.running = false;
		try {
			gameActionsThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	void endTurn() {
		logger.dbgln("End turn");
		gameAction(() -> {
			game.turnEnd();
			assert !game.isFinished();

			suspendActions();
			computer.playTurn(game);
			game.turnEnd();
			resumeActions();
		});
	}

	boolean isActionSuspended() {
		return actionsSuspended.get() > 0;
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
