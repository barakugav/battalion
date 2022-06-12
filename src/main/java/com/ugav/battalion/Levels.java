package com.ugav.battalion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.UnitDesc;

class Levels {

	private final Map<Label, Level> levels;
	private final List<Label> campaign;

	static enum Label {
		GraphicTest, TeamTest,
	}

	Levels() {
		levels = new HashMap<>();
		levels.put(Label.GraphicTest, GraphicTestLevel);
		levels.put(Label.TeamTest, TeamTestLevel);

		campaign = new ArrayList<>();
		campaign.add(Label.GraphicTest);
		campaign.add(Label.TeamTest);
	}

	List<Pair<String, Level>> getLevels() {
		List<Pair<String, Level>> lvls = new ArrayList<>();
		for (Label lvl : campaign)
			lvls.add(Pair.of(lvl.toString(), levels.get(lvl)));
		return lvls;
	}

	private static final Level GraphicTestLevel = new LevelBuilder(3, 3)
	        .setTile(0, 0, Terrain.FLAT_LAND, null,  UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(0, 1, Terrain.MOUNTAIN, null, null)
	        .setTile(0, 2, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.Factory, Team.Blue), UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(1, 0, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Blue), null)
	        .setTile(1, 1, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Blue), UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(1, 2, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.Factory, Team.Blue), null)
	        .setTile(2, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 1, Terrain.CLEAR_WATER, null, null)
	        .setTile(2, 2, Terrain.CLEAR_WATER, null, null)
	        .buildLevel();

	private static final Level TeamTestLevel = new LevelBuilder(9, 9)
	        .setTile(0, 0, Terrain.FLAT_LAND, null,  UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(0, 1, Terrain.MOUNTAIN, null, null)
	        .setTile(0, 2, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.Factory, Team.Blue), UnitDesc.of(Unit.Type.Soldier, Team.Blue))
	        .setTile(1, 0, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Blue), null)
	        .setTile(1, 1, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.OilRefinery, Team.Red), UnitDesc.of(Unit.Type.Soldier, Team.Red))
	        .setTile(1, 2, Terrain.FLAT_LAND, BuildingDesc.of(Building.Type.Factory, Team.Blue), null)
	        .setTile(2, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 1, Terrain.CLEAR_WATER, null, null)
	        .setTile(2, 2, Terrain.CLEAR_WATER, null, null)
	        .buildLevel();

}
