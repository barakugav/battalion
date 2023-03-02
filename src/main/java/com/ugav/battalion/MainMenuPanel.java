package com.ugav.battalion;

import java.awt.CardLayout;
import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import com.ugav.battalion.core.Level;
import com.ugav.battalion.util.Utils;

class MainMenuPanel extends JPanel implements Clearable {

	private final Globals globals;
	private final Levels levels;

	private final CardLayout cardLayout;
	private final ButtonsSet mainButtonSet;
	private final ButtonsSet levelsButtonSet;

	private static final long serialVersionUID = 1L;

	MainMenuPanel(Globals globals) {
		this.globals = Objects.requireNonNull(globals);
		levels = new Levels(globals.levelSerializer);

		setLayout(cardLayout = new CardLayout());
		add(mainButtonSet = createMainButtonSet(), mainButtonSet.name);
		add(levelsButtonSet = createLevelsButtonSet(), levelsButtonSet.name);

		showButtonSet(mainButtonSet);
	}

	private void showButtonSet(ButtonsSet buttonsSet) {
		cardLayout.show(this, buttonsSet.name);
	}

	private ButtonsSet createMainButtonSet() {
		ButtonsSet buttonSet = new ButtonsSet("Main");

		buttonSet.add(Utils.newButton("Campaign", e -> showButtonSet(levelsButtonSet)));
		buttonSet.add(
				Utils.newButton("Bonus Level", e -> this.globals.frame.openLevelGame(levels.getLevel("Bonus Level"))));
		buttonSet.add(Utils.newButton("Custom Level", e -> {
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
		}));
		buttonSet.add(Utils.newButton("Level Builder", e -> this.globals.frame.openLevelBuilder()));
		buttonSet.add(Utils.newButton("Options", e -> this.globals.frame.openOptionsMenu()));

		return buttonSet;
	}

	private ButtonsSet createLevelsButtonSet() {
		ButtonsSet buttonSet = new ButtonsSet("Campaign");

		for (Pair<String, Level> lvl : levels.getLevels())
			buttonSet.add(Utils.newButton(lvl.e1, e -> this.globals.frame.openLevelGame(lvl.e2)));

		buttonSet.add(Utils.newButton("Back", e -> showButtonSet(mainButtonSet)));

		return buttonSet;
	}

	@Override
	public void clear() {
	}

	private static class ButtonsSet extends JPanel {

		private final String name;

		private static final long serialVersionUID = 1L;

		ButtonsSet(String name) {
			super(new GridLayout(0, 1));
			this.name = Objects.requireNonNull(name);
		}
	}

}
