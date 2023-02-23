package com.ugav.battalion;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.ugav.battalion.computer.Player;
import com.ugav.battalion.computer.PlayerMiniMaxAlphaBeta;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.util.DebugPrintsManager;

class GameWindow extends JPanel implements Clearable {

	final Globals globals;
	private final GameSideMenu menu;
	final GameArenaPanel arenaPanel;

	final Game game;
//	private final Player computer = new Player.Random();
	private final Player computer = new PlayerMiniMaxAlphaBeta();
	private final DebugPrintsManager debug = new DebugPrintsManager(true); // TODO
	private final DataChangeRegister register = new DataChangeRegister();

	private volatile boolean actionsSuspended;

	private static final long serialVersionUID = 1L;

	GameWindow(Globals globals, Level level) {
		this.globals = Objects.requireNonNull(globals);

		if (level.width() < GameArenaPanel.DISPLAYED_ARENA_WIDTH
				|| level.height() < GameArenaPanel.DISPLAYED_ARENA_HEIGHT)
			throw new IllegalArgumentException("level size is too small");
		this.game = new GameGUIImpl(this, level);
		arenaPanel = new GameArenaPanel(this);
		menu = new GameSideMenu(this);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;

		c.gridx = 0;
		c.weightx = c.gridwidth = 5;
		c.fill = GridBagConstraints.BOTH;
		add(menu, c);
		c.gridx = 5;
		c.weightx = c.gridwidth = 20;
		c.fill = GridBagConstraints.NONE;
		add(arenaPanel, c);

		menu.initGame();
		arenaPanel.initGame();

		register.register(game.onGameEnd(), e -> {
			debug.println("Game finished");
			JOptionPane.showMessageDialog(this, "Winner: " + e.winner);
			suspendActions();
			// TODO
		});

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
		debug.println("End turn");
		gameAction(() -> {
			game.turnEnd();
			assert !game.isFinished();

			computer.playTurn(game);
			game.turnEnd();
		});
	}

	boolean isActionSuspended() {
		return actionsSuspended;
	}

	void suspendActions() {
		debug.println("Actions suspended");
		actionsSuspended = true;
	}

	void resumeActions() {
		debug.println("Actions resumed");
		actionsSuspended = false;
	}

}
