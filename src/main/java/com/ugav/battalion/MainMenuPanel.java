package com.ugav.battalion;

import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

class MainMenuPanel extends JPanel implements Clearable {

	private final Globals globals;
	private final Levels levels;

	private static final long serialVersionUID = 1L;

	MainMenuPanel(Globals globals) {
		this.globals = Objects.requireNonNull(globals);
		levels = new Levels(globals);

		int levelCount = levels.getLevels().size();
		setLayout(new GridLayout(0, 1));

		for (int levelIdx = 0; levelIdx < levelCount; levelIdx++) {
			JButton lvlButton = new JButton(String.format("Level %2d", Integer.valueOf(levelIdx)));
			final int lvlIdx = levelIdx;
			lvlButton.addActionListener(e -> this.globals.frame.loadLevel(levels.getLevels().get(lvlIdx).e2));
			add(lvlButton);
		}

		JButton customLvlButton = new JButton("Custom Level");
		customLvlButton.addActionListener(e -> {

			JFileChooser fileChooser = Levels.createFileChooser(globals.levelSerializer.getFileType());
			int result = fileChooser.showOpenDialog(globals.frame);
			if (result == JFileChooser.APPROVE_OPTION) {
				Cookies.setCookieValue(Cookies.LEVEL_DISK_LAST_DIR,
						fileChooser.getCurrentDirectory().getAbsolutePath());
				String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
				try {
					Level level = globals.levelSerializer.levelRead(selectedFile);
					this.globals.frame.loadLevel(level);
				} catch (RuntimeException ex) {
//					debug.print("failed to load file from: ", selectedFile);
					ex.printStackTrace();
				}
			}
		});
		add(customLvlButton);

		JButton lvlBuilderButton = new JButton("Level Builder");
		lvlBuilderButton.addActionListener(e -> this.globals.frame.loadLevelBuilder());
		add(lvlBuilderButton);
	}

	@Override
	public void clear() {
	}

}
