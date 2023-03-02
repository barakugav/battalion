package com.ugav.battalion;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Objects;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import com.ugav.battalion.core.Level;
import com.ugav.battalion.util.Pair;
import com.ugav.battalion.util.Utils;

class MainMenuWindow extends JPanel implements Clearable {

	private final Globals globals;
	private final Levels levels;

	private final JPanel buttonsPanel;
	private final CardLayout cardLayout;
	private final ButtonsSet mainButtonSet;
	private final ButtonsSet levelsButtonSet;

	private static final long serialVersionUID = 1L;

	MainMenuWindow(Globals globals) {
		this.globals = Objects.requireNonNull(globals);
		levels = new Levels(globals.levelSerializer);

		setLayout(new GridBagLayout());

		buttonsPanel = new JPanel();
		buttonsPanel.setLayout(cardLayout = new CardLayout());
		buttonsPanel.add(mainButtonSet = createMainButtonSet(), mainButtonSet.name);
		buttonsPanel.add(levelsButtonSet = createLevelsButtonSet(), levelsButtonSet.name);
		add(buttonsPanel, Utils.gbConstraints(0, 0, 1, 1, GridBagConstraints.NONE, 1, 1));

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
	}

	private static class ButtonsSet extends JPanel {

		private final String name;
		private int buttonCount;

		private static final long serialVersionUID = 1L;

		ButtonsSet(String name) {
			super(new GridBagLayout());
			this.name = Objects.requireNonNull(name);
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
