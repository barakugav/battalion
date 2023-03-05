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
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ListInt;
import com.ugav.battalion.util.Pair;
import com.ugav.battalion.util.Utils;

class MainMenuWindow extends JLayeredPane implements Clearable {

	private final Globals globals;
	private final Levels levels;

	private final JPanel tabsPanel;
	private final Tab mainTab;
	private final Tab campaignTab;
	private final Tab optionsTab;

	private final ArenaPanelAnimated animatedArena;

	private static final long serialVersionUID = 1L;

	MainMenuWindow(Globals globals) {
		this.globals = Objects.requireNonNull(globals);
		levels = new Levels(globals.levelSerializer);

		mainTab = new MainTab();
		campaignTab = new CampaignTab();
		optionsTab = new OptionTab();
		tabsPanel = createTabsPanel();
		add(tabsPanel, JLayeredPane.PALETTE_LAYER);

		Pair<Level, Iter<Action>> bgGame = getAnimatedBackgroundGame(globals);
		animatedArena = new ArenaPanelAnimated(globals, Game.fromLevel(bgGame.e1), bgGame.e2);
		add(animatedArena, JLayeredPane.DEFAULT_LAYER);

		Runnable resizeComponents = () -> {
			Dimension container = getSize();
			for (JComponent comp : List.of(tabsPanel, animatedArena)) {
				Dimension compSize = comp == animatedArena ? container : comp.getPreferredSize();
				int x = (container.width - compSize.width) / 2;
				int y = (container.height - compSize.height) / 2;
				comp.setBounds(x, y, compSize.width, compSize.height);
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

		/* start animation after a second of delay */
		Timer timer = new Timer(1000, e -> animatedArena.runAnimation());
		timer.setRepeats(false);
		timer.start();
	}

	private JPanel createTabsPanel() {
		JPanel tabsPanel = new JPanel();
		tabsPanel.setLayout(new CardLayout());
		for (Tab tab : List.of(mainTab, campaignTab, optionsTab)) {
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
		animatedArena.clear();
	}

	private class MainTab extends Tab {

		private static final Color ExitColor = new Color(254, 106, 106);
		private static final long serialVersionUID = 1L;

		MainTab() {
			super("Main");
			addTitle("Main Menu");

			Menus.ButtonColumn buttonSet = new Menus.ButtonColumn();
			buttonSet.addButton("Campaign", e -> showTab(campaignTab));
			buttonSet.addButton("Bonus Level",
					e -> globals.frame.openLevelGame(levels.getLevel("Bonus Level"), "Bonus Level"));
			buttonSet.addButton("Custom Level", e -> {
				JFileChooser fileChooser = Levels.createFileChooser(globals.levelSerializer.getFileType(),
						Cookies.getCookieValue(Cookies.LEVEL_DISK_LAST_DIR));
				int result = fileChooser.showOpenDialog(globals.frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					Cookies.setCookieValue(Cookies.LEVEL_DISK_LAST_DIR,
							fileChooser.getCurrentDirectory().getAbsolutePath());
					String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
					try {
						Level level = globals.levelSerializer.levelRead(selectedFile);
						String basename = new File(selectedFile).getName();
						globals.frame.openLevelGame(level, basename);
					} catch (RuntimeException ex) {
//						debug.print("failed to load file from: ", selectedFile);
						ex.printStackTrace();
					}
				}
			});
			buttonSet.addButton("Level Builder", e -> globals.frame.openLevelBuilder());
			buttonSet.addButton("Options", e -> showTab(optionsTab));
			addComp(buttonSet);

			Menus.ButtonColumn additionalButtonSet = new Menus.ButtonColumn();
			additionalButtonSet.addButton("Exit", e -> globals.frame.exitGame()).setBackground(ExitColor);
			addComp(additionalButtonSet);
		}

	}

	private class CampaignTab extends Tab {

		private static final long serialVersionUID = 1L;

		CampaignTab() {
			super("Campaign");
			addTitle("Campaign");

			Menus.ButtonColumn levelsButtonSet = new Menus.ButtonColumn();
			for (Pair<String, Level> lvl : levels.getLevels())
				levelsButtonSet.addButton(lvl.e1, e -> globals.frame.openLevelGame(lvl.e2, lvl.e1));
			addComp(levelsButtonSet);

			Menus.ButtonColumn additionalButtonSet = new Menus.ButtonColumn();
			additionalButtonSet.addButton("Back", e -> showTab(mainTab));
			addComp(additionalButtonSet);
		}

	}

	private class OptionTab extends Tab {

		private static final long serialVersionUID = 1L;

		OptionTab() {
			super("Options");
			addTitle("Options");

			Menus.CheckboxColumn debugOptions = new Menus.CheckboxColumn();
			debugOptions.addCheckbox("showGrid", globals.debug.showGrid,
					selected -> globals.debug.showGrid = selected.booleanValue());
			debugOptions.addCheckbox("showUnitID", globals.debug.showUnitID,
					selected -> globals.debug.showUnitID = selected.booleanValue());
			debugOptions.addCheckbox("playAllTeams", globals.debug.playAllTeams,
					selected -> globals.debug.playAllTeams = selected.booleanValue());
			addComp(debugOptions);

			Menus.ButtonColumn additionalButtonSet = new Menus.ButtonColumn();
			additionalButtonSet.addButton("Back", e -> showTab(mainTab));
			addComp(additionalButtonSet);
		}

	}

	private static class Tab extends Menus.Window {

		final String name;
		private static final long serialVersionUID = 1L;

		Tab(String name) {
			this.name = Objects.requireNonNull(name);
		}

	}

	private static Pair<Level, Iter<Action>> getAnimatedBackgroundGame(Globals globals) {
		Level level = globals.levelSerializer.levelRead("level/animated01.xml");
		List<Action> actions = new ArrayList<>();
		actions.add(new Action.UnitMove(Cell.of(2, 3),
				ListInt.of(Cell.of(3, 3), Cell.of(4, 3), Cell.of(5, 3), Cell.of(5, 4), Cell.of(5, 5), Cell.of(5, 6))));
		actions.add(new Action.UnitMoveAndAttack(Cell.of(5, 1), ListInt.of(Cell.of(6, 1)), Cell.of(7, 1)));
		actions.add(new Action.TurnEnd());
		actions.add(new Action.UnitMove(Cell.of(14, 8),
				ListInt.of(Cell.of(14, 7), Cell.of(14, 6), Cell.of(14, 5), Cell.of(13, 5))));
		actions.add(new Action.UnitMove(Cell.of(7, 1), ListInt.of(Cell.of(8, 1), Cell.of(9, 1), Cell.of(10, 1))));
		actions.add(new Action.UnitMoveAndAttack(Cell.of(11, 1),
				ListInt.of(Cell.of(10, 1), Cell.of(9, 1), Cell.of(8, 1), Cell.of(7, 1)), Cell.of(6, 1)));
		actions.add(new Action.UnitMoveAndAttack(Cell.of(9, 8), ListInt.of(Cell.of(8, 8), Cell.of(7, 8), Cell.of(6, 8)),
				Cell.of(6, 7)));
		actions.add(new Action.TurnEnd());
		actions.add(
				new Action.UnitMoveAndAttack(Cell.of(5, 6), ListInt.of(Cell.of(5, 7), Cell.of(5, 8)), Cell.of(6, 8)));
		actions.add(new Action.UnitBuild(Cell.of(2, 1), Unit.Type.Bazooka));
		actions.add(new Action.UnitAttackLongRange(Cell.of(2, 9), Cell.of(6, 8)));
		actions.add(new Action.UnitMove(Cell.of(6, 7),
				ListInt.of(Cell.of(7, 7), Cell.of(7, 6), Cell.of(8, 6), Cell.of(8, 5))));
		actions.add(new Action.TurnEnd());
		actions.add(new Action.UnitBuild(Cell.of(13, 6), Unit.Type.TankAntiAir));
		actions.add(new Action.UnitAttackLongRange(Cell.of(11, 6), Cell.of(8, 5)));
		actions.add(new Action.UnitMove(Cell.of(7, 1),
				ListInt.of(Cell.of(8, 1), Cell.of(9, 1), Cell.of(10, 1), Cell.of(11, 1))));
		actions.add(new Action.UnitRepair(Cell.of(10, 1)));
		actions.add(new Action.TurnEnd());
		actions.add(new Action.UnitMove(Cell.of(2, 1), ListInt.of(Cell.of(3, 1), Cell.of(4, 1), Cell.of(5, 1))));
		actions.add(new Action.UnitBuild(Cell.of(2, 1), Unit.Type.Airplane));
		actions.add(new Action.UnitMove(Cell.of(5, 8),
				ListInt.of(Cell.of(5, 7), Cell.of(5, 6), Cell.of(5, 5), Cell.of(5, 4), Cell.of(5, 3), Cell.of(4, 3))));
		actions.add(new Action.TurnEnd());
		actions.add(new Action.UnitMove(Cell.of(13, 5),
				ListInt.of(Cell.of(13, 6), Cell.of(13, 7), Cell.of(13, 8), Cell.of(14, 8))));
		actions.add(new Action.UnitMove(Cell.of(13, 6),
				ListInt.of(Cell.of(12, 6), Cell.of(11, 6), Cell.of(11, 7), Cell.of(11, 8), Cell.of(10, 8))));
		actions.add(new Action.TurnEnd());
		actions.add(new Action.UnitMove(Cell.of(2, 1), ListInt.of(Cell.of(3, 1), Cell.of(4, 1), Cell.of(5, 1),
				Cell.of(5, 2), Cell.of(5, 3), Cell.of(5, 4), Cell.of(5, 5))));
		actions.add(new Action.UnitMove(Cell.of(4, 3), ListInt.of(Cell.of(3, 3), Cell.of(2, 3))));
		actions.add(new Action.TurnEnd());
		actions.add(new Action.UnitMove(Cell.of(10, 8), ListInt.of(Cell.of(9, 8))));
		actions.add(new Action.TurnEnd());
		actions.add(new Action.UnitRepair(Cell.of(2, 3)));
		actions.add(new Action.UnitMove(Cell.of(5, 5), ListInt.of(Cell.of(5, 6), Cell.of(5, 7), Cell.of(6, 7))));
		actions.add(new Action.TurnEnd());
		actions.add(new Action.UnitMove(Cell.of(10, 1), ListInt.of(Cell.of(9, 1), Cell.of(8, 1), Cell.of(7, 1))));
		actions.add(new Action.TurnEnd());
		return Pair.of(level, Utils.iteratorRepeatInfty(actions));
	}

}
