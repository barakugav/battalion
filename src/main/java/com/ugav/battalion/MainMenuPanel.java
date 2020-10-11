package com.ugav.battalion;

import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.JPanel;

import com.ugav.battalion.Building.Factory;
import com.ugav.battalion.Building.OilRefinery;
import com.ugav.battalion.Unit.Soldier;

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
	    LevelBuilder builder = new LevelBuilder(3, 3);
	    builder.setTile(0, 0, Terrain.FLAT_LAND, null, new Soldier(Team.Blue));
	    builder.setTile(0, 1, Terrain.MOUNTAIN, null, null);
	    builder.setTile(0, 2, Terrain.FLAT_LAND, new Factory(Team.Blue), new Soldier(Team.Blue));
	    builder.setTile(1, 0, Terrain.FLAT_LAND, new OilRefinery(Team.Blue), null);
	    builder.setTile(1, 1, Terrain.FLAT_LAND, new OilRefinery(Team.Blue), new Soldier(Team.Blue));
	    builder.setTile(1, 2, Terrain.FLAT_LAND, new Factory(Team.Blue), null);
	    builder.setTile(2, 0, Terrain.FLAT_LAND, null, null);
	    builder.setTile(2, 1, Terrain.CLEAR_WATER, null, null);
	    builder.setTile(2, 2, Terrain.CLEAR_WATER, null, null);
	    levels[0] = builder.buildLevel();
	}

    }

}
