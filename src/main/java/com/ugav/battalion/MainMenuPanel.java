package com.ugav.battalion;

import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import com.ugav.battalion.core.Level;

class MainMenuPanel extends JPanel implements Clearable {

	private final Globals globals;
	private final Levels levels;

	private static final long serialVersionUID = 1L;

	MainMenuPanel(Globals globals) {
		this.globals = Objects.requireNonNull(globals);
		levels = new Levels(globals.levelSerializer);

		setLayout(new GridLayout(0, 1));

		for (Pair<String, Level> lvl : levels.getLevels()) {
			JButton lvlButton = new JButton(lvl.e1);
			lvlButton.addActionListener(e -> this.globals.frame.openLevelGame(lvl.e2));
			add(lvlButton);
		}

		JButton customLvlButton = new JButton("Custom Level");
		customLvlButton.addActionListener(e -> {

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
		add(customLvlButton);

		JButton lvlBuilderButton = new JButton("Level Builder");
		lvlBuilderButton.addActionListener(e -> this.globals.frame.openLevelBuilder());
		add(lvlBuilderButton);

		JButton optionsButton = new JButton("Options");
		optionsButton.addActionListener(e -> this.globals.frame.openOptionsMenu());
		add(optionsButton);
	}

	@Override
	public void clear() {
	}

}
