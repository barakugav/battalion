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

		/* Water edge */
		WaterEdge00, WaterEdge01, WaterEdge02, WaterEdge03, WaterEdge10, WaterEdge11, WaterEdge12, WaterEdge13,
		WaterEdge20, WaterEdge21, WaterEdge22, WaterEdge23, WaterEdge30, WaterEdge31, WaterEdge32, WaterEdge33,

		/* GUI */
		Selection, Reachable, Attackable, UnitLocked;
	}

	private static final Map<Terrain, BufferedImage> terrains;
	private static final Map<Unit.Type, Map<Team, BufferedImage>> units;
	private static final Map<Building.Type, Map<Team, BufferedImage>> buildings;
	private static final Map<Label, BufferedImage> ect;
	static {
		/* Terrain */
		Map<Terrain, BufferedImage> terrains0 = new HashMap<>();
		for (int type = 1; type <= 5; type++)
			terrains0.put(Terrain.valueOf("FlatLand" + type), loadImg("img/terrain/flat_land_0" + type + ".png"));
		terrains0.put(Terrain.Trees, loadImg("img/terrain/forest.png"));
		terrains0.put(Terrain.Hills, loadImg("img/terrain/land_hills.png"));
		terrains0.put(Terrain.Mountain, loadImg("img/terrain/mountain.png"));
		terrains0.put(Terrain.MountainBig, loadImg("img/terrain/mountain_high.png"));
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
		addUnit.accept(Unit.Type.Tank, "img/unit/tank.png");
		addUnit.accept(Unit.Type.Artillery, "img/unit/artillery.png");
		addUnit.accept(Unit.Type.Turrent, "img/unit/turrent.png");
		addUnit.accept(Unit.Type.Ship, "img/unit/ship_close_range.png");
		addUnit.accept(Unit.Type.Airplane, "img/unit/airplane.png");
		units = Collections.unmodifiableMap(units0);

		/* Buildings */
		Map<Building.Type, Map<Team, BufferedImage>> buildings0 = new HashMap<>();
		BiConsumer<Building.Type, String> addBuilding = (type, path) -> {
			BufferedImage redImg = loadImg(path);
			BufferedImage blueImg = toBlue(redImg);
			BufferedImage whiteImg = toWhite(redImg);
			buildings0.put(type, Map.of(Team.Red, redImg, Team.Blue, blueImg, Team.None, whiteImg));
		};
		addBuilding.accept(Building.Type.Factory, "img/building/facotry.png");
		addBuilding.accept(Building.Type.OilRefinery, "img/building/oil_refinery.png");
		addBuilding.accept(Building.Type.OilRefineryBig, "img/building/oil_refinery_big.png");
		addBuilding.accept(Building.Type.OilRig, "img/building/oil_rig.png");
		buildings = Collections.unmodifiableMap(buildings0);

		/* Ect */
		Map<Label, BufferedImage> ect0 = new HashMap<>();
		for (int quadrant = 0; quadrant < 4; quadrant++) {
			for (int variant = 0; variant < 4; variant++) {
				String suffix = "" + quadrant + variant;
				ect0.put(Label.valueOf("WaterEdge" + suffix), loadImg("img/terrain/water_edge_" + suffix + ".png"));
			}
		}
		ect0.put(Label.Selection, loadImg("img/gui/selection.png"));
		ect0.put(Label.Reachable, loadImg("img/gui/reachable.png"));
		ect0.put(Label.Attackable, loadImg("img/gui/attackabe.png"));
		ect0.put(Label.UnitLocked, loadImg("img/gui/unit_locked.png"));
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

		} else if (obj instanceof Label) {
			Label label = (Label) obj;
			return ect.get(label);

		} else {
			throw new InternalError("Unsupported drawable object: " + obj);
		}
	}

	static interface Drawable {
	}

}
