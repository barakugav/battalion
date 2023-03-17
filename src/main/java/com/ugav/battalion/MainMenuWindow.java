package com.ugav.battalion;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.Border;

import com.ugav.battalion.Levels.LevelHandle;
import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.ListInt;
import com.ugav.battalion.util.Logger;
import com.ugav.battalion.util.Pair;
import com.ugav.battalion.util.Utils;

class MainMenuWindow extends JLayeredPane implements Clearable {

	private final Globals globals;

	private final JPanel tabsPanel;
	private final Tab mainTab;
	private final Tab campaignTab;
	private final Tab optionsTab;
	private final Tab aboutTab;

	private final AnimatedGames animatedGames;

	private static final long serialVersionUID = 1L;

	MainMenuWindow(Globals globals) {
		this.globals = Objects.requireNonNull(globals);

		mainTab = new MainTab();
		campaignTab = new CampaignTab();
		optionsTab = new OptionTab();
		aboutTab = new AboutTab();
		tabsPanel = createTabsPanel();
		add(tabsPanel, JLayeredPane.PALETTE_LAYER);

		animatedGames = new AnimatedGames(globals);
		add(animatedGames, JLayeredPane.DEFAULT_LAYER);

		Runnable resizeComponents = () -> {
			Dimension container = getSize();
			for (JComponent comp : List.of(tabsPanel, animatedGames)) {
				Dimension compSize = comp == animatedGames ? container : comp.getPreferredSize();
				int x = (container.width - compSize.width) / 2;
				int y = (container.height - compSize.height) / 2;
				comp.setBounds(x, y, compSize.width, compSize.height);
				comp.revalidate();
			}
		};

		resizeComponents.run();
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				resizeComponents.run();
			}

		});

		showTab(mainTab);

		animatedGames.runAnimation();
	}

	private JPanel createTabsPanel() {
		JPanel tabsPanel = new JPanel();
		tabsPanel.setLayout(new CardLayout());
		for (Tab tab : List.of(mainTab, campaignTab, optionsTab, aboutTab)) {
			JPanel tabPanel = new JPanel(new GridBagLayout());
			tabPanel.add(tab, Utils.gbConstraints(0, 0, 1, 1, GridBagConstraints.NONE, 1, 1));
			tabPanel.setOpaque(false);
			tabsPanel.add(tabPanel, tab.name);
		}
		tabsPanel.setOpaque(false);
		return tabsPanel;
	}

	private void showTab(Tab tab) {
		((CardLayout) tabsPanel.getLayout()).show(tabsPanel, tab.name);
	}

	@Override
	public void clear() {
		animatedGames.clear();
	}

	private class MainTab extends Tab {

		private static final Color ExitColor = new Color(254, 106, 106);
		private static final long serialVersionUID = 1L;

		MainTab() {
			super("Main");
			Menus.ColumnWithMargins column = new Menus.ColumnWithMargins();
			column.addComp(new Menus.Title("Main Menu"));

			Menus.ButtonColumn buttonSet = new Menus.ButtonColumn();
			buttonSet.addButton("Campaign", e -> showTab(campaignTab));
			buttonSet.addButton("Bonus Level",
					e -> globals.frame.openLevelGame(globals.levels.getLevel("Bonus Level")));
			buttonSet.addButton("Custom Level", e -> {
				JFileChooser fileChooser = Levels.createFileChooser(globals.levelSerializer.getFileType(),
						Cookies.getCookieValue(Cookies.LEVEL_DISK_LAST_DIR));
				int result = fileChooser.showOpenDialog(globals.frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					Cookies.setCookieValue(Cookies.LEVEL_DISK_LAST_DIR,
							fileChooser.getCurrentDirectory().getAbsolutePath());
					String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
					try {
						String basename = new File(selectedFile).getName();
						LevelHandle level = new LevelHandle(basename,
								() -> globals.levelSerializer.levelRead(selectedFile), -1);
						globals.frame.openLevelGame(level);
					} catch (RuntimeException ex) {
//						debug.print("failed to load file from: ", selectedFile);
						ex.printStackTrace();
					}
				}
			});
			buttonSet.addButton("Level Builder", e -> globals.frame.openLevelBuilder());
			buttonSet.addButton("Options", e -> showTab(optionsTab));
			buttonSet.addButton("About", e -> showTab(aboutTab));
			column.addComp(buttonSet);

			Menus.ButtonColumn additionalButtonSet = new Menus.ButtonColumn();
			additionalButtonSet.addButton("Exit", e -> globals.frame.exitGame()).setBackground(ExitColor);
			column.addComp(additionalButtonSet);

			add(column);
		}

	}

	private class CampaignTab extends Tab {

		private static final long serialVersionUID = 1L;

		CampaignTab() {
			super("Campaign");
			Menus.ColumnWithMargins column = new Menus.ColumnWithMargins();
			column.addComp(new Menus.Title("Campaign"));

			Menus.ButtonColumn levelsButtonSet = new Menus.ButtonColumn();
			for (LevelHandle lvl : globals.levels.getCampaign())
				levelsButtonSet.addButton(lvl.name, e -> globals.frame.openLevelGame(lvl));
			column.addComp(levelsButtonSet);

			Menus.ButtonColumn additionalButtonSet = new Menus.ButtonColumn();
			additionalButtonSet.addButton("Back", e -> showTab(mainTab));
			column.addComp(additionalButtonSet);

			add(column);
		}

	}

	private class OptionTab extends Tab {

		private static final long serialVersionUID = 1L;

		OptionTab() {
			super("Options");
			Menus.ColumnWithMargins column = new Menus.ColumnWithMargins();
			column.addComp(new Menus.Title("Options"));

			Menus.CheckboxColumn debugOptions = new Menus.CheckboxColumn();
			debugOptions.addCheckbox("showGrid", globals.debug.showGrid,
					selected -> globals.debug.showGrid = selected.booleanValue());
			debugOptions.addCheckbox("showUnitID", globals.debug.showUnitID,
					selected -> globals.debug.showUnitID = selected.booleanValue());
			debugOptions.addCheckbox("playAllTeams", globals.debug.playAllTeams,
					selected -> globals.debug.playAllTeams = selected.booleanValue());
			column.addComp(debugOptions);

			Menus.ButtonColumn additionalButtonSet = new Menus.ButtonColumn();
			additionalButtonSet.addButton("Back", e -> showTab(mainTab));
			column.addComp(additionalButtonSet);

			add(column);
		}

	}

	private class AboutTab extends Tab {

		private static final long serialVersionUID = 1L;
		private static final String Text;
		static {
			List<String> lines = new ArrayList<>();
			lines.add("Inspired by 'Battalion: Nemesis'");
			lines.add("All units and building descriptions were generated using ChatGPT.");
			lines.add("I tried generating more units images using DALL·E 2, but the");
			lines.add("results were not really of the same theme.");
			lines.add("Check me out at https://github.com/barakugav");

			StringBuilder b = new StringBuilder();
			for (int i = 0; i < lines.size(); i++) {
				b.append(lines.get(i));
				if (i < lines.size() - 1)
					b.append(System.lineSeparator());
			}
			Text = b.toString();
		}

		AboutTab() {
			super("About");
			Menus.ColumnWithMargins column = new Menus.ColumnWithMargins();
			column.addComp(new Menus.Title("About"));

			JTextArea text = new JTextArea();
			text.setEditable(false);
//			text.setWrapStyleWord(true);
//			text.setLineWrap(true);
			text.setText(Text);
			text.setBackground(new Color(150, 150, 150));
			Border border = BorderFactory.createLineBorder(new Color(40, 40, 40), 3);
			Border margin = BorderFactory.createEmptyBorder(3, 3, 3, 3);
			text.setBorder(BorderFactory.createCompoundBorder(border, margin));
			column.addComp(text);

			Menus.ButtonColumn additionalButtonSet = new Menus.ButtonColumn();
			additionalButtonSet.addButton("Back", e -> showTab(mainTab));
			column.addComp(additionalButtonSet);

			add(column);
		}

	}

	private static class Tab extends Menus.Window {

		final String name;
		private static final long serialVersionUID = 1L;

		Tab(String name) {
			this.name = Objects.requireNonNull(name);
		}

	}

	private static class AnimatedGames extends JPanel implements Clearable {

		private ArenaPanelAnimated animatedArena;
		private final Globals globals;
		private boolean running;
		private final Timer switchGameTimer;
		private final Iterator<Pair<Level, Iterable<Action>>> gamesIter;
		private final Event.Register register = new Event.Register();
		private static final int SingleGameAnimationDurationSec = 120;
		private static final long serialVersionUID = 1L;

		AnimatedGames(Globals globals) {
			this.globals = globals;
			setLayout(new GridBagLayout());
			switchGameTimer = new Timer(SingleGameAnimationDurationSec * 1000, e -> timerExec());
			switchGameTimer.setRepeats(false);

			List<Pair<Level, Iterable<Action>>> games = new ArrayList<>();
			games.add(getAnimatedBackgroundGame01());
			games.add(getAnimatedBackgroundGame02());
			gamesIter = Utils.iteratorRepeatInfty(games);
		}

		private void timerExec() {
			if (!running)
				return;
			switchAnimatedGame();
		}

		void runAnimation() {
			running = true;
			switchAnimatedGame();
		}

		private void switchAnimatedGame() {
			if (animatedArena != null) {
				animatedArena.clear();
				remove(animatedArena);
				register.unregisterAll(animatedArena.game.onAction);
			}

			Pair<Level, Iterable<Action>> bgGame = gamesIter.next();
			Logger gameActionlogger = new Logger.Enabled(globals.logger, () -> globals.debug.logGameActions);
			Game game = Game.fromLevel(bgGame.e1);
			register.register(game.onAction, e -> gameActionlogger.dbgln(e.action));
			animatedArena = new ArenaPanelAnimated(globals, game, bgGame.e2.iterator());

			add(animatedArena, Utils.gbConstraints(0, 0, 1, 1, GridBagConstraints.BOTH, 1, 1));
			animatedArena.runAnimation();
			revalidate();

			switchGameTimer.start();
		}

		@Override
		public void clear() {
			running = false;
			switchGameTimer.stop();
			if (animatedArena != null)
				animatedArena.clear();
			register.unregisterAll();
		}

		private Pair<Level, Iterable<Action>> getAnimatedBackgroundGame01() {
			Level level = globals.levelSerializer.levelRead("level/animated/animated01.xml");
			List<Action> actions = new ArrayList<>();
			actions.add(new Action.UnitMove(Cell.of(2, 3), ListInt.of(Cell.of(3, 3), Cell.of(4, 3), Cell.of(5, 3),
					Cell.of(5, 4), Cell.of(5, 5), Cell.of(5, 6))));
			actions.add(new Action.UnitMoveAndAttack(Cell.of(5, 1), ListInt.of(Cell.of(6, 1)), Cell.of(7, 1)));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(14, 8),
					ListInt.of(Cell.of(14, 7), Cell.of(14, 6), Cell.of(14, 5), Cell.of(13, 5))));
			actions.add(new Action.UnitMove(Cell.of(7, 1), ListInt.of(Cell.of(8, 1), Cell.of(9, 1), Cell.of(10, 1))));
			actions.add(new Action.UnitMoveAndAttack(Cell.of(11, 1),
					ListInt.of(Cell.of(10, 1), Cell.of(9, 1), Cell.of(8, 1), Cell.of(7, 1)), Cell.of(6, 1)));
			actions.add(new Action.UnitMoveAndAttack(Cell.of(9, 8),
					ListInt.of(Cell.of(8, 8), Cell.of(7, 8), Cell.of(6, 8)), Cell.of(6, 7)));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMoveAndAttack(Cell.of(5, 6), ListInt.of(Cell.of(5, 7), Cell.of(5, 8)),
					Cell.of(6, 8)));
			actions.add(new Action.UnitBuild(Cell.of(2, 1), Unit.Type.RocketSpecialist));
			actions.add(new Action.UnitAttackLongRange(Cell.of(2, 9), Cell.of(6, 8)));
			actions.add(new Action.UnitMove(Cell.of(6, 7),
					ListInt.of(Cell.of(7, 7), Cell.of(7, 6), Cell.of(8, 6), Cell.of(8, 5))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitBuild(Cell.of(13, 6), Unit.Type.AATank));
			actions.add(new Action.UnitAttackLongRange(Cell.of(11, 6), Cell.of(8, 5)));
			actions.add(new Action.UnitMove(Cell.of(7, 1),
					ListInt.of(Cell.of(8, 1), Cell.of(9, 1), Cell.of(10, 1), Cell.of(11, 1))));
			actions.add(new Action.UnitRepair(Cell.of(10, 1)));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(2, 1), ListInt.of(Cell.of(3, 1), Cell.of(4, 1), Cell.of(5, 1))));
			actions.add(new Action.UnitBuild(Cell.of(2, 1), Unit.Type.FighterPlane));
			actions.add(new Action.UnitMove(Cell.of(5, 8), ListInt.of(Cell.of(5, 7), Cell.of(5, 6), Cell.of(5, 5),
					Cell.of(5, 4), Cell.of(5, 3), Cell.of(4, 3))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(13, 5),
					ListInt.of(Cell.of(13, 6), Cell.of(13, 7), Cell.of(13, 8), Cell.of(14, 8))));
			actions.add(new Action.UnitMove(Cell.of(13, 6),
					ListInt.of(Cell.of(12, 6), Cell.of(11, 6), Cell.of(11, 7), Cell.of(11, 8), Cell.of(10, 8))));
			actions.add(new Action.UnitRepair(Cell.of(10, 1)));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(2, 1), ListInt.of(Cell.of(3, 1), Cell.of(4, 1), Cell.of(5, 1),
					Cell.of(5, 2), Cell.of(5, 3), Cell.of(5, 4), Cell.of(5, 5))));
			actions.add(new Action.UnitMove(Cell.of(4, 3), ListInt.of(Cell.of(3, 3), Cell.of(2, 3))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(10, 8), ListInt.of(Cell.of(9, 8))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitRepair(Cell.of(2, 3)));
			actions.add(new Action.UnitMove(Cell.of(5, 5), ListInt.of(Cell.of(5, 6), Cell.of(5, 7), Cell.of(6, 7))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(10, 1), ListInt.of(Cell.of(9, 1), Cell.of(8, 1), Cell.of(7, 1))));
			actions.add(Action.TurnEnd);
			return Pair.of(level, new Iterable<>() {

				@Override
				public Iterator<Action> iterator() {
					return Utils.iteratorRepeatInfty(actions);
				}
			});
		}

		private Pair<Level, Iterable<Action>> getAnimatedBackgroundGame02() {
			Level level = globals.levelSerializer.levelRead("level/animated/animated02.xml");
			List<Action> actions = new ArrayList<>();
			actions.add(new Action.UnitMove(Cell.of(0, 3),
					ListInt.of(Cell.of(0, 4), Cell.of(0, 5), Cell.of(0, 6), Cell.of(0, 7), Cell.of(1, 7))));
			actions.add(new Action.UnitMove(Cell.of(2, 2),
					ListInt.of(Cell.of(2, 3), Cell.of(2, 4), Cell.of(3, 4), Cell.of(3, 5), Cell.of(3, 6))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(14, 6),
					ListInt.of(Cell.of(13, 6), Cell.of(12, 6), Cell.of(11, 6), Cell.of(10, 6), Cell.of(9, 6))));
			actions.add(new Action.UnitMove(Cell.of(13, 3), ListInt.of(Cell.of(13, 4), Cell.of(13, 5), Cell.of(12, 5),
					Cell.of(12, 6), Cell.of(11, 6), Cell.of(10, 6))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitTransportFinish(Cell.of(3, 6)));
			actions.add(new Action.UnitTransportFinish(Cell.of(1, 7)));
			actions.add(new Action.UnitMove(Cell.of(3, 6), ListInt.of(Cell.of(4, 6), Cell.of(5, 6))));
			actions.add(new Action.UnitMove(Cell.of(1, 7), ListInt.of(Cell.of(2, 7), Cell.of(3, 7), Cell.of(4, 7),
					Cell.of(5, 7), Cell.of(6, 7), Cell.of(6, 8))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(11, 6), ListInt.of(Cell.of(10, 6), Cell.of(9, 6), Cell.of(8, 6))));
			actions.add(new Action.UnitMove(Cell.of(9, 6), ListInt.of(Cell.of(8, 6), Cell.of(8, 7))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMoveAndAttack(Cell.of(6, 8),
					ListInt.of(Cell.of(7, 8), Cell.of(7, 7), Cell.of(7, 6)), Cell.of(8, 6)));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(8, 6), ListInt.of(Cell.of(8, 7), Cell.of(7, 7), Cell.of(6, 7))));
			actions.add(new Action.UnitMoveAndAttack(Cell.of(10, 6), ListInt.of(Cell.of(9, 6), Cell.of(8, 6)),
					Cell.of(7, 6)));
			actions.add(new Action.UnitMoveAndAttack(Cell.of(8, 7), ListInt.of(Cell.of(7, 7)), Cell.of(7, 6)));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitBuild(Cell.of(1, 1), Unit.Type.BattleTank));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitRepair(Cell.of(6, 7)));
			actions.add(new Action.UnitMove(Cell.of(7, 7), ListInt.of(Cell.of(6, 7), Cell.of(5, 7), Cell.of(4, 7))));
			actions.add(new Action.UnitRepair(Cell.of(8, 6)));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMoveAndAttack(Cell.of(5, 6), ListInt.of(Cell.of(6, 6), Cell.of(7, 6)),
					Cell.of(8, 6)));
			actions.add(new Action.UnitMove(Cell.of(1, 1), ListInt.of(Cell.of(0, 1), Cell.of(0, 2))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMoveAndAttack(Cell.of(4, 7),
					ListInt.of(Cell.of(5, 7), Cell.of(6, 7), Cell.of(6, 6)), Cell.of(7, 6)));
			actions.add(new Action.UnitMoveAndAttack(Cell.of(8, 6), ListInt.of(), Cell.of(7, 6)));
			actions.add(new Action.UnitMove(Cell.of(6, 7), ListInt.of(Cell.of(5, 7), Cell.of(5, 6))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitBuild(Cell.of(1, 1), Unit.Type.Rifleman));
			actions.add(new Action.UnitMove(Cell.of(0, 2), ListInt.of(Cell.of(0, 3))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitRepair(Cell.of(5, 6)));
			actions.add(new Action.UnitRepair(Cell.of(8, 6)));
			actions.add(new Action.UnitMove(Cell.of(6, 6),
					ListInt.of(Cell.of(7, 6), Cell.of(8, 6), Cell.of(9, 6), Cell.of(10, 6), Cell.of(11, 6))));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(1, 1), ListInt.of(Cell.of(2, 1), Cell.of(2, 2))));
			actions.add(new Action.UnitTransport(Cell.of(0, 3), Unit.Type.LandingCraft));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitRepair(Cell.of(5, 6)));
			actions.add(new Action.UnitRepair(Cell.of(8, 6)));
			actions.add(new Action.UnitRepair(Cell.of(11, 6)));
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitTransport(Cell.of(2, 2), Unit.Type.LandingCraft));
			actions.add(Action.TurnEnd);
			actions.add(
					new Action.UnitMove(Cell.of(11, 6), ListInt.of(Cell.of(12, 6), Cell.of(13, 6), Cell.of(14, 6))));
			actions.add(new Action.UnitMove(Cell.of(8, 6), ListInt.of(Cell.of(9, 6), Cell.of(10, 6), Cell.of(11, 6),
					Cell.of(12, 6), Cell.of(12, 5), Cell.of(12, 4))));
			actions.add(new Action.UnitMove(Cell.of(5, 6), ListInt.of(Cell.of(6, 6), Cell.of(7, 6), Cell.of(8, 6))));
			actions.add(Action.TurnEnd);
			actions.add(Action.TurnEnd);
			actions.add(new Action.UnitMove(Cell.of(8, 6), ListInt.of(Cell.of(9, 6), Cell.of(10, 6), Cell.of(11, 6))));
			actions.add(new Action.UnitMove(Cell.of(12, 4), ListInt.of(Cell.of(13, 4), Cell.of(13, 3))));
			actions.add(new Action.UnitRepair(Cell.of(14, 6)));
			actions.add(Action.TurnEnd);
			return Pair.of(level, new Iterable<>() {

				@Override
				public Iterator<Action> iterator() {
					return Utils.iteratorRepeatInfty(actions);
				}
			});
		}

	}

}
