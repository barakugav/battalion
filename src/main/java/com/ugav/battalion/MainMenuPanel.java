package com.ugav.battalion;

import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

class MainMenuPanel extends JPanel implements Clearable {

	private final GameFrame gameFrame;
	private final Levels levels;

	private static final long serialVersionUID = 1L;

	MainMenuPanel(GameFrame gameFrame) {
		this.gameFrame = Objects.requireNonNull(gameFrame);
		levels = new Levels();

		int levelCount = levels.getLevels().size();
		setLayout(new GridLayout(0, 1));

		for (int levelIdx = 0; levelIdx < levelCount; levelIdx++) {
			JButton lvlButton = new JButton(String.format("Level %2d", Integer.valueOf(levelIdx)));
			final int lvlIdx = levelIdx;
			lvlButton.addActionListener(e -> this.gameFrame.loadLevel(levels.getLevels().get(lvlIdx).e2));
			add(lvlButton);
		}

		JButton customLvlButton = new JButton("Custom Level");
		customLvlButton.addActionListener(e -> {

			JFileChooser fileChooser = Levels.createFileChooser(gameFrame.serializer.getFileType());
			int result = fileChooser.showOpenDialog(gameFrame);
			if (result == JFileChooser.APPROVE_OPTION) {
				Cookies.setCookieValue(Cookies.LEVEL_DISK_LAST_DIR,
						fileChooser.getCurrentDirectory().getAbsolutePath());
				String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
				try {
					Level level = gameFrame.serializer.levelRead(selectedFile);
					this.gameFrame.loadLevel(level);
				} catch (RuntimeException ex) {
//					debug.print("failed to load file from: ", selectedFile);
					ex.printStackTrace();
				}
			}
		});
		add(customLvlButton);

		JButton lvlBuilderButton = new JButton("Level Builder");
		lvlBuilderButton.addActionListener(e -> this.gameFrame.loadLevelBuilder());
		add(lvlBuilderButton);
	}

	@Override
	public void clear() {
	}

}
