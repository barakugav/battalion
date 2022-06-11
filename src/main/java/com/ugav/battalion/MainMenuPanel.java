package com.ugav.battalion;

import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.ugav.battalion.level.HardcodedLevel1;

public class MainMenuPanel extends JPanel {

	private final GameFrame gameFrame;
	private final Data data;

	MainMenuPanel(GameFrame gameFrame) {
		this.gameFrame = Objects.requireNonNull(gameFrame);
		data = new Data();

		int levelCount = data.getLevelCount();
		setLayout(new GridLayout(levelCount, 1));

		for (int levelIdx = 0; levelIdx < data.getLevelCount(); levelIdx++)
			add(new LevelButton(levelIdx));
	}

	private void selectLevel(int levelIdx) {
		Game game = new Game(data.getLevel(levelIdx));
		game.start();
		gameFrame.getGamePlane().setGame(game);
		gameFrame.displayGame();
	}

	private class LevelButton extends JButton {

		private final int levelIdx;

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

	private static class Data {

		private static final int LEVEL_COUNT = 1;

		private final Level levels[];

		Data() {
			levels = new Level[LEVEL_COUNT];
			loadLevels();
		}

		int getLevelCount() {
			return LEVEL_COUNT;
		}

		Level getLevel(int idx) {
			return levels[idx];
		}

		private void loadLevels() {
			levels[0] = HardcodedLevel1.getLevel();
		}

	}

}
