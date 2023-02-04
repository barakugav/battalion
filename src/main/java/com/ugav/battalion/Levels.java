package com.ugav.battalion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.UnitDesc;

class Levels {

	private final Map<Label, Level> levels;
	private final List<Label> campaign;

	static enum Label {
		GraphicTest, TeamTest,

		Level1, Level2,
	}

	Levels() {
		levels = new HashMap<>();
		levels.put(Label.GraphicTest, GraphicTestLevel);
		levels.put(Label.TeamTest, TeamTestLevel);
		levels.put(Label.Level1, Level1);
		levels.put(Label.Level2, Level2);

		campaign = new ArrayList<>();
//		campaign.add(Label.GraphicTest);
//		campaign.add(Label.TeamTest);
//		campaign.add(Label.Level1);
		campaign.add(Label.Level2);
	}

	List<Pair<String, Level>> getLevels() {
		List<Pair<String, Level>> lvls = new ArrayList<>();
		for (Label lvl : campaign)
			lvls.add(Pair.of(lvl.toString(), levels.get(lvl)));
		return lvls;
	}

	private static final Level GraphicTestLevel = new LevelBuilder(3, 3)
	        .setTile(0, 0, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(0, 1, Terrain.MOUNTAIN, null, null)
	        .setTile(0, 2, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.Factory, Team.Blue), UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(1, 0, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Blue), null)
	        .setTile(1, 1, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Blue), UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(1, 2, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.Factory, Team.Blue), null)
	        .setTile(2, 0, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .setTile(2, 1, Terrain.CLEAR_WATER, null, null)
	        .setTile(2, 2, Terrain.CLEAR_WATER, null, null)
	        .buildLevel();

	private static final Level TeamTestLevel = new LevelBuilder(5, 5)
	        .setTile(0, 0, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(0, 1, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(0, 2, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(0, 3, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(0, 4, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Tank, Team.Blue))
	        .setTile(1, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 0, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .setTile(4, 1, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .setTile(4, 2, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .setTile(4, 3, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Tank, Team.Red))
	        .setTile(4, 4, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .buildLevel();

	private static final Level Level1 = new LevelBuilder(9, 9)
	        .setTile(0, 0, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.Factory, Team.Red), null)
	        .setTile(0, 1, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Red), null)
	        .setTile(0, 2, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Red), null)
	        .setTile(0, 3, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Tank, Team.Red))
	        .setTile(0, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(0, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(0, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(0, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(0, 8, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 0, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Red), UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .setTile(1, 1, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Red), UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .setTile(1, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 8, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 8, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 8, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 8, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 8, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 8, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 8, Terrain.FLAT_LAND, null, null)
	        .setTile(8, 0, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(8, 1, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(8, 2, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(8, 3, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Tank, Team.Blue))
	        .setTile(8, 4, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(8, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(8, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(8, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(8, 8, Terrain.FLAT_LAND, null, null)
	        .buildLevel();

	private static final Level Level2 = new LevelBuilder(9, 9)
	        .setTile(0, 0, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.Factory, Team.Red), null)
	        .setTile(0, 1, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Red), null)
	        .setTile(0, 2, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Red), null)
	        .setTile(0, 3, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Tank, Team.Red))
	        .setTile(0, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(0, 5, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Airplane, Team.Red))
	        .setTile(0, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(0, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(0, 8, Terrain.CLEAR_WATER, null, null)
	        .setTile(1, 0, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Red), UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .setTile(1, 1, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Red), UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .setTile(1, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 5, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Airplane, Team.Red))
	        .setTile(1, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(1, 7, Terrain.CLEAR_WATER, null, null)
	        .setTile(1, 8, Terrain.CLEAR_WATER, null, UnitDesc.of(Unit.Type.Ship, Team.Red))
	        .setTile(2, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 7, Terrain.CLEAR_WATER, null, null)
	        .setTile(2, 8, Terrain.CLEAR_WATER, null, null)
	        .setTile(3, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(3, 7, Terrain.CLEAR_WATER, null, null)
	        .setTile(3, 8, Terrain.CLEAR_WATER, null, null)
	        .setTile(4, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(4, 7, Terrain.CLEAR_WATER, null, null)
	        .setTile(4, 8, Terrain.CLEAR_WATER, null, null)
	        .setTile(5, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(5, 7, Terrain.CLEAR_WATER, null, null)
	        .setTile(5, 8, Terrain.CLEAR_WATER, null, null)
	        .setTile(6, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(6, 7, Terrain.CLEAR_WATER, null, null)
	        .setTile(6, 8, Terrain.CLEAR_WATER, null, null)
	        .setTile(7, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 1, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 2, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 3, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 4, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 6, Terrain.FLAT_LAND, null, null)
	        .setTile(7, 7, Terrain.CLEAR_WATER, null, UnitDesc.of(Unit.Type.Ship, Team.Blue))
	        .setTile(7, 8, Terrain.CLEAR_WATER, null, null)
	        .setTile(8, 0, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(8, 1, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(8, 2, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(8, 3, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Tank, Team.Blue))
	        .setTile(8, 4, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(8, 5, Terrain.FLAT_LAND, null, null)
	        .setTile(8, 6, Terrain.FLAT_LAND, null, UnitDesc.of(Unit.Type.Artillery, Team.Blue))
	        .setTile(8, 7, Terrain.FLAT_LAND, null, null)
	        .setTile(8, 8, Terrain.CLEAR_WATER, null, UnitDesc.of(Unit.Type.Ship, Team.Blue))
	        .buildLevel();

	static JFileChooser createFileChooser(String fileType) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileNameExtensionFilter("Level file (*." + fileType + ")", fileType));
		String dialogDir = Cookies.getCookieValue(Cookies.LEVEL_DISK_LAST_DIR);
		if (dialogDir == null || !(new File(dialogDir).isDirectory()))
			dialogDir = System.getProperty("user.home");
		fileChooser.setCurrentDirectory(new File(dialogDir));
		return fileChooser;
	}

}
