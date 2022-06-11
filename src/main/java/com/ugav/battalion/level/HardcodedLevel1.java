package com.ugav.battalion.level;

import com.ugav.battalion.Building.Factory;
import com.ugav.battalion.Building.OilRefinery;
import com.ugav.battalion.Level;
import com.ugav.battalion.LevelBuilder;
import com.ugav.battalion.Team;
import com.ugav.battalion.Terrain;
import com.ugav.battalion.Unit.Soldier;

public class HardcodedLevel1 {

    private static final Level LEVEL = new LevelBuilder(3, 3)
	        .setTile(0, 0, Terrain.FLAT_LAND, null, new Soldier(Team.Blue))
	        .setTile(0, 1, Terrain.MOUNTAIN, null, null)
	        .setTile(0, 2, Terrain.FLAT_LAND, new Factory(Team.Blue), new Soldier(Team.Blue))
	        .setTile(1, 0, Terrain.FLAT_LAND, new OilRefinery(Team.Blue), null)
	        .setTile(1, 1, Terrain.FLAT_LAND, new OilRefinery(Team.Blue), new Soldier(Team.Blue))
	        .setTile(1, 2, Terrain.FLAT_LAND, new Factory(Team.Blue), null)
	        .setTile(2, 0, Terrain.FLAT_LAND, null, null)
	        .setTile(2, 1, Terrain.CLEAR_WATER, null, null)
	        .setTile(2, 2, Terrain.CLEAR_WATER, null, null)
	        .buildLevel();

	private HardcodedLevel1() {
		throw new InternalError();
	}

	public static Level getLevel() {
		return LEVEL;
	}

}
