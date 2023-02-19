package com.ugav.battalion;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.core.Team;

class LevelGameWindow extends JPanel implements Clearable {

	final Globals globals;
	private final SideMenu menu;
	final GameArenaPanel arenaPanel;

	final Game game;
	private final ComputerPlayer computer = new ComputerPlayer.Random();
	private final DebugPrintsManager debug = new DebugPrintsManager(true); // TODO
	private final DataChangeRegister register = new DataChangeRegister();

	private volatile boolean actionsSuspended;

	private static final long serialVersionUID = 1L;

	LevelGameWindow(Globals globals, Level level) {
		this.globals = Objects.requireNonNull(globals);

		if (level.width() < GameArenaPanel.DISPLAYED_ARENA_WIDTH
				|| level.height() < GameArenaPanel.DISPLAYED_ARENA_HEIGHT)
			throw new IllegalArgumentException("level size is too small");
		this.game = new GameGUI(this, level);
		menu = new SideMenu();
		arenaPanel = new GameArenaPanel(this);

		setLayout(new FlowLayout());
		add(menu);
		add(arenaPanel);

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

	private void endTurn() {
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

	private class SideMenu extends JPanel implements Clearable {

		private final Map<Team, JLabel> labelMoney;
		private final DataChangeRegister register = new DataChangeRegister();

		private static final long serialVersionUID = 1L;

		SideMenu() {
			labelMoney = new HashMap<>();
			for (Team team : Team.realTeams)
				labelMoney.put(team, new JLabel());

			JButton buttonEndTurn = new JButton("End Turn");
			buttonEndTurn.addActionListener(onlyIfActionsEnabled(e -> endTurn()));
			JButton buttonMainMenu = new JButton("Main Menu");
			buttonMainMenu.addActionListener(onlyIfActionsEnabled(e -> globals.frame.displayMainMenu()));

			setLayout(new GridLayout(0, 1));
			for (JLabel label : labelMoney.values())
				add(label);
			add(buttonEndTurn);
			add(buttonMainMenu);

			for (Team team : Team.realTeams)
				updateMoneyLabel(team, game.getMoney(team));
		}

		void initGame() {
			register.register(game.onMoneyChange(), e -> updateMoneyLabel(e.team, e.newAmount));
		}

		@Override
		public void clear() {
			register.unregisterAll();
		}

		private void updateMoneyLabel(Team team, int money) {
			labelMoney.get(team).setText(team.toString() + ": " + money);
		}

		private ActionListener onlyIfActionsEnabled(ActionListener l) {
			return e -> {
				if (!isActionSuspended())
					l.actionPerformed(e);
			};
		}

	}

}
