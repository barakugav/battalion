package com.ugav.battalion;

import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

class MainMenuPanel extends JPanel {

	private final GameFrame gameFrame;
	private final Levels levels;

	private static final long serialVersionUID = 1L;

	MainMenuPanel(GameFrame gameFrame) {
		this.gameFrame = Objects.requireNonNull(gameFrame);
		levels = new Levels();

		int levelCount = levels.getLevels().size();
		setLayout(new GridLayout(levelCount, 1));

		for (int levelIdx = 0; levelIdx < levelCount; levelIdx++)
			add(new LevelButton(levelIdx));
	}

	void selectLevel(int levelIdx) {
		Game game = new Game(levels.getLevels().get(levelIdx).e2);
		game.start();
		gameFrame.getGamePlane().setGame(game);
		gameFrame.displayGame();
	}

	private class LevelButton extends JButton {

		private final int levelIdx;

		private static final long serialVersionUID = 1L;

		LevelButton(int levelIdx) {
			if (levelIdx < 0)
				throw new IllegalArgumentException();
			this.levelIdx = levelIdx;

			setText(String.format("Level %2d", Integer.valueOf(levelIdx)));
			setSize(100, 100);
			addActionListener(e -> {
				selectLevel(this.levelIdx);
			});
		}

	}

}
