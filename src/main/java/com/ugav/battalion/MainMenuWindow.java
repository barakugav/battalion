package com.ugav.battalion;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level;
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

		Game game = Game.fromLevel(levels.getLevel("Level 01"));
		animatedArena = new ArenaPanelAnimated(game, globals);
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

//		Action action1 = new Action.UnitMove(Cell.of(2, 1), ListInt.of(Cell.of(2, 2)));
//		Action action2 = new Action.TurnEnd();
//		Action action3 = new Action.TurnEnd();
//		Action action4 = new Action.UnitMove(Cell.of(2, 2), ListInt.of(Cell.of(2, 1)));
//		Action action5 = new Action.TurnEnd();
//		Action action6 = new Action.TurnEnd();
//		List<Action> actions0 = List.of(action1, action2, action3, action4, action5, action6);
//		List<Action> actions = new ArrayList<>();
//		for (int i = 0; i < 100; i++)
//			actions.addAll(actions0);
//		animatedArena.animatedActions(actions);
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

}
