package com.ugav.battalion;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.UnitDesc;

class HardcodedLevel1 {

	private static final Level LEVEL = new LevelBuilder(3, 3)
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

	private HardcodedLevel1() {
		throw new InternalError();
	}

	static Level getLevel() {
		return LEVEL;
	}

}
