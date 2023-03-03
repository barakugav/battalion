package com.ugav.battalion;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

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

	private final JPanel buttonsPanel;
	private final CardLayout cardLayout;
	private final ButtonsSet mainButtonSet;
	private final ButtonsSet levelsButtonSet;

	private final ArenaPanelAnimated animatedArena;

	private static final long serialVersionUID = 1L;

	MainMenuWindow(Globals globals) {
		this.globals = Objects.requireNonNull(globals);
		levels = new Levels(globals.levelSerializer);

		buttonsPanel = new JPanel();
		buttonsPanel.setLayout(cardLayout = new CardLayout());
		buttonsPanel.add(mainButtonSet = createMainButtonSet(), mainButtonSet.name);
		buttonsPanel.add(levelsButtonSet = createLevelsButtonSet(), levelsButtonSet.name);
		buttonsPanel.setOpaque(false);
		add(buttonsPanel, JLayeredPane.PALETTE_LAYER);

		Pair<Level, Iter<Action>> bgGame = getAnimatedBackgroundGame(globals);
		animatedArena = new ArenaPanelAnimated(globals, Game.fromLevel(bgGame.e1), bgGame.e2);
		add(animatedArena, JLayeredPane.DEFAULT_LAYER);

		Runnable resizeComponents = () -> {
			Dimension container = getSize();
			for (JComponent comp : List.of(buttonsPanel, animatedArena)) {
				Dimension compSize = comp.getPreferredSize();
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

		showButtonSet(mainButtonSet);
	}

	private void showButtonSet(ButtonsSet buttonsSet) {
		cardLayout.show(buttonsPanel, buttonsSet.name);
	}

	private ButtonsSet createMainButtonSet() {
		ButtonsSet buttonSet = new ButtonsSet("Main");

		buttonSet.addButton("Campaign", e -> showButtonSet(levelsButtonSet));
		buttonSet.addButton("Bonus Level", e -> this.globals.frame.openLevelGame(levels.getLevel("Bonus Level")));
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
					this.globals.frame.openLevelGame(level);
				} catch (RuntimeException ex) {
//					debug.print("failed to load file from: ", selectedFile);
					ex.printStackTrace();
				}
			}
		});
		buttonSet.addButton("Level Builder", e -> this.globals.frame.openLevelBuilder());
		buttonSet.addButton("Options", e -> this.globals.frame.openOptionsMenu());

		buttonSet.addFiller();
		return buttonSet;
	}

	private ButtonsSet createLevelsButtonSet() {
		ButtonsSet buttonSet = new ButtonsSet("Campaign");

		for (Pair<String, Level> lvl : levels.getLevels())
			buttonSet.addButton(lvl.e1, e -> this.globals.frame.openLevelGame(lvl.e2));

		buttonSet.addButton("Back", e -> showButtonSet(mainButtonSet));

		buttonSet.addFiller();
		return buttonSet;
	}

	@Override
	public void clear() {
		animatedArena.clear();
	}

	private static class ButtonsSet extends JPanel {

		private final String name;
		private int buttonCount;

		private static final long serialVersionUID = 1L;

		ButtonsSet(String name) {
			super(new GridBagLayout());
			this.name = Objects.requireNonNull(name);
			setOpaque(false);
		}

		void addButton(String label, ActionListener action) {
			GridBagConstraints c = Utils.gbConstraints(0, buttonCount++, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
			add(Utils.newButton(label, action), c);
		}

		void addFiller() {
			JPanel filler = new JPanel();
			filler.setPreferredSize(new Dimension(0, 0));
			add(filler, Utils.gbConstraints(0, buttonCount, 1, 1, GridBagConstraints.VERTICAL, 0, 1));
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
