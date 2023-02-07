package com.ugav.battalion;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.UnitDesc;

class Images {

	private Images() {
	}

	enum Label {
		/* GUI */
		Selection, Reachable, Attackable, UnitLocked, Delete;
	}

	private static final Map<Terrain, BufferedImage> terrains;
	private static final Map<Unit.Type, Map<Team, BufferedImage>> units;
	private static final Map<Building.Type, Map<Team, BufferedImage>> buildings;
	private static final Map<Object, BufferedImage> ect;
	static {
		/* Terrain */
		Map<Terrain, BufferedImage> terrains0 = new HashMap<>();
		for (int type = 1; type <= 5; type++)
			terrains0.put(Terrain.valueOf("FlatLand" + type), loadImg("img/terrain/flat_land_0" + type + ".png"));
		terrains0.put(Terrain.Trees, loadImg("img/terrain/forest.png"));
		terrains0.put(Terrain.Hills, loadImg("img/terrain/land_hills.png"));
		terrains0.put(Terrain.Mountain, loadImg("img/terrain/mountain.png"));
		terrains0.put(Terrain.MountainBig, loadImg("img/terrain/mountain_high.png"));
		terrains0.put(Terrain.Road, loadImg("img/terrain/road_vxvx.png"));
		terrains0.put(Terrain.BridgeLow, loadImg("img/terrain/bridge_low.png"));
		terrains0.put(Terrain.BridgeHigh, loadImg("img/terrain/bridge_high.png"));
		terrains0.put(Terrain.Shore, loadImg("img/terrain/shore.png"));
		terrains0.put(Terrain.ClearWater, loadImg("img/terrain/water_clear.png"));
		terrains = Collections.unmodifiableMap(terrains0);

		/* Units */
		Map<Unit.Type, Map<Team, BufferedImage>> units0 = new HashMap<>();
		BiConsumer<Unit.Type, String> addUnit = (type, path) -> {
			BufferedImage redImg = loadImg(path);
			BufferedImage blueImg = toBlue(redImg);
			units0.put(type, Map.of(Team.Red, redImg, Team.Blue, blueImg));
		};
		addUnit.accept(Unit.Type.Soldier, "img/unit/soldier.png");
		addUnit.accept(Unit.Type.Bazooka, "img/unit/bazooka.png");
		addUnit.accept(Unit.Type.Tank, "img/unit/tank.png");
		addUnit.accept(Unit.Type.TankBig, "img/unit/tank_big.png");
		addUnit.accept(Unit.Type.TankAntiAir, "img/unit/tank_anti_air.png");
		addUnit.accept(Unit.Type.Artillery, "img/unit/artillery.png");
		addUnit.accept(Unit.Type.Mortar, "img/unit/mortar.png");
		addUnit.accept(Unit.Type.Turrent, "img/unit/turrent.png");
		addUnit.accept(Unit.Type.SpeedBoat, "img/unit/speed_boat.png");
		addUnit.accept(Unit.Type.Ship, "img/unit/ship_close_range.png");
		addUnit.accept(Unit.Type.ShipAntiAir, "img/unit/ship_anti_air.png");
		addUnit.accept(Unit.Type.ShipArtillery, "img/unit/ship_artillery.png");
		addUnit.accept(Unit.Type.Airplane, "img/unit/airplane.png");
		addUnit.accept(Unit.Type.Zeppelin, "img/unit/zeppelin.png");
		units = Collections.unmodifiableMap(units0);

		/* Buildings */
		Map<Building.Type, Map<Team, BufferedImage>> buildings0 = new HashMap<>();
		BiConsumer<Building.Type, String> addBuilding = (type, path) -> {
			BufferedImage redImg = loadImg(path);
			BufferedImage blueImg = toBlue(redImg);
			BufferedImage whiteImg = toWhite(redImg);
			buildings0.put(type, Map.of(Team.Red, redImg, Team.Blue, blueImg, Team.None, whiteImg));
		};
		addBuilding.accept(Building.Type.Capital, "img/building/capital.png");
		addBuilding.accept(Building.Type.Factory, "img/building/facotry.png");
		addBuilding.accept(Building.Type.ControllerLand, "img/building/controller_land.png");
		addBuilding.accept(Building.Type.ControllerWater, "img/building/controller_water.png");
		addBuilding.accept(Building.Type.ControllerAir, "img/building/controller_air.png");
		addBuilding.accept(Building.Type.OilRefinery, "img/building/oil_refinery.png");
		addBuilding.accept(Building.Type.OilRefineryBig, "img/building/oil_refinery_big.png");
		addBuilding.accept(Building.Type.OilRig, "img/building/oil_rig.png");
		buildings = Collections.unmodifiableMap(buildings0);

		/* Ect */
		Map<Object, BufferedImage> ect0 = new HashMap<>();
		for (int quadrant = 0; quadrant < 4; quadrant++) {
			for (int variant = 0; variant < 4; variant++) {
				String suffix = "" + quadrant + variant;
				ect0.put("WaterEdge" + suffix, loadImg("img/terrain/water_edge_" + suffix + ".png"));
			}
		}
		for (int variant = 0; variant < 16; variant++) {
			String suffix = "";
			for (int b = 0; b < 4; b++)
				suffix += ((variant & (1 << b)) != 0) ? "v" : "x";
			ect0.put("Road_" + suffix, loadImg("img/terrain/road_" + suffix + ".png"));
		}
		for (boolean high : new boolean[] { true, false }) {
			for (int dir = 0; dir < 4; dir++) {
				for (boolean end : new boolean[] { true, false }) {
					String label = "bridge_" + (high ? "high" : "low");
					label += "_" + dir + (end ? "x" : "v");
					ect0.put(label, loadImg("img/terrain/" + label + ".png"));
				}
			}
		}
		for (int quadrant = 0; quadrant < 4; quadrant++) {
			for (int variant = 1; variant < 4; variant++) {
				String suffix = "" + quadrant + variant;
				ect0.put("Shore" + suffix, loadImg("img/terrain/shore_" + suffix + ".png"));
			}
		}
		ect0.put(Label.Selection, loadImg("img/gui/selection.png"));
		ect0.put(Label.Reachable, loadImg("img/gui/reachable.png"));
		ect0.put(Label.Attackable, loadImg("img/gui/attackabe.png"));
		ect0.put(Label.UnitLocked, loadImg("img/gui/unit_locked.png"));
		ect0.put(Label.Delete, loadImg("img/gui/delete.png"));
		ect = Collections.unmodifiableMap(ect0);
	}

	private static BufferedImage loadImg(String path) {
		try {
			return ImageIO.read(new File(path));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load img file: " + path, e);
		}
	};

	private static BufferedImage toBlue(BufferedImage redImg) {
		if (redImg.getRaster().getNumBands() != 4)
			throw new IllegalArgumentException("expected rgba format");
		return Utils.imgTransform(Utils.imgDeepCopy(redImg), rgba -> {
			int r = rgba[0], b = rgba[2];
			rgba[0] = b;
			rgba[2] = r;
		});
	}

	private static BufferedImage toWhite(BufferedImage redImg) {
		if (redImg.getRaster().getNumBands() != 4)
			throw new IllegalArgumentException("expected rgba format");
		return Utils.imgTransform(Utils.imgDeepCopy(redImg), rgba -> {
			int r = rgba[0];
			rgba[1] = rgba[2] = r;
		});
	}

	static BufferedImage getImage(Object obj) {
		BufferedImage img = getImage0(obj);
		if (img == null)
			throw new IllegalArgumentException("No image found for object: " + obj);
		return img;
	}

	private static BufferedImage getImage0(Object obj) {
		if (obj instanceof Terrain) {
			Terrain terrain = (Terrain) obj;
			return terrains.get(terrain);

		} else if (obj instanceof Unit) {
			Unit unit = (Unit) obj;
			return units.get(unit.type).get(unit.getTeam());

		} else if (obj instanceof UnitDesc) {
			UnitDesc unit = (UnitDesc) obj;
			return units.get(unit.type).get(unit.team);

		} else if (obj instanceof Building) {
			Building building = (Building) obj;
			return buildings.get(building.type).get(building.getTeam());

		} else if (obj instanceof BuildingDesc) {
			BuildingDesc building = (BuildingDesc) obj;
			return buildings.get(building.type).get(building.team);

		} else {
			return ect.get(obj);
		}
	}

	static interface Drawable {
	}

}
