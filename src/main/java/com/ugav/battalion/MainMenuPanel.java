package com.ugav.battalion;

import java.awt.GridLayout;
import java.util.Objects;

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
    }

    private void selectLevel(int levelIdx) {
	// gameFrame.getGamePlane().setL
	// TODO
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
