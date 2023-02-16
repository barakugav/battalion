package com.ugav.battalion;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.IUnit;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Position.Direction;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;

class Images {

	private Images() {
	}

	enum Label {
		/* GUI */
		Selection, Passable, Attackable, UnitLocked, Delete,

		UnitMenuTransportAir, UnitMenuTransportWater, UnitMenuTransportFinish, UnitMenuRepair, UnitMenuCancel,
	}

	private static class UnitImgDesc {

		private final Object[] keys;

		UnitImgDesc(Unit.Type type, Team team, Direction orientation) {
			if (type == null || team == null || orientation == null)
				throw new NullPointerException();
			keys = new Object[] { type, team, orientation };
		}

		static UnitImgDesc of(IUnit unit, Direction orientation) {
			return new UnitImgDesc(unit.getType(), unit.getTeam(), orientation != null ? orientation : Direction.XPos);
		}

		@Override
		public boolean equals(Object other) {
			if (other == this)
				return true;
			if (!(other instanceof UnitImgDesc))
				return false;
			UnitImgDesc o = (UnitImgDesc) other;
			return Arrays.equals(keys, o.keys);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(keys);
		}

		@Override
		public String toString() {
			return Arrays.toString(keys);
		}

	}

	private static final Map<Terrain, BufferedImage> terrains;
	private static final Map<UnitImgDesc, BufferedImage> units;
	private static final Map<UnitImgDesc, BufferedImage> unitsMini;
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
		Map<UnitImgDesc, BufferedImage> units0 = new HashMap<>();
		BiConsumer<Unit.Type, String> addUnit1gesture = (type, path) -> units0.putAll(addUnit(type, path, 1));
		BiConsumer<Unit.Type, String> addUnit4gesture = (type, path) -> units0.putAll(addUnit(type, path, 4));
		BiConsumer<Unit.Type, String> addUnit5gesture = (type, path) -> units0.putAll(addUnit(type, path, 5));
		addUnit5gesture.accept(Unit.Type.Soldier, "img/unit/soldier.png");
		addUnit5gesture.accept(Unit.Type.Bazooka, "img/unit/bazooka.png");
		addUnit4gesture.accept(Unit.Type.Tank, "img/unit/tank.png");
		addUnit4gesture.accept(Unit.Type.TankBig, "img/unit/tank_big.png");
		addUnit4gesture.accept(Unit.Type.TankAntiAir, "img/unit/tank_anti_air.png");
		addUnit4gesture.accept(Unit.Type.Artillery, "img/unit/artillery.png");
		addUnit4gesture.accept(Unit.Type.Mortar, "img/unit/mortar.png");
		addUnit1gesture.accept(Unit.Type.Turrent, "img/unit/turrent.png");
		addUnit4gesture.accept(Unit.Type.SpeedBoat, "img/unit/speed_boat.png");
		addUnit4gesture.accept(Unit.Type.Ship, "img/unit/ship_close_range.png");
		addUnit4gesture.accept(Unit.Type.ShipAntiAir, "img/unit/ship_anti_air.png");
		addUnit4gesture.accept(Unit.Type.ShipArtillery, "img/unit/ship_artillery.png");
		addUnit4gesture.accept(Unit.Type.Submarine, "img/unit/submarine.png");
		addUnit4gesture.accept(Unit.Type.ShipTransporter, "img/unit/unit_ship_water.png");
		addUnit4gesture.accept(Unit.Type.Airplane, "img/unit/airplane.png");
		addUnit4gesture.accept(Unit.Type.Zeppelin, "img/unit/zeppelin.png");
		addUnit4gesture.accept(Unit.Type.AirTransporter, "img/unit/unit_ship_air.png");
		units = Collections.unmodifiableMap(units0);
		Map<UnitImgDesc, BufferedImage> unitsMini0 = new HashMap<>(units);
		unitsMini0.replaceAll((desc, img) -> miniUnitImg(img));
		unitsMini = Collections.unmodifiableMap(unitsMini0);

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

		ect0.put("MovePathSourceNone", loadImg("img/gui/move_path_source_none.png"));
		BufferedImage moveSource = loadImg("img/gui/move_path_source_down.png");
		BufferedImage movePathDest = loadImg("img/gui/move_path_destination_down.png");
		BufferedImage movePathDestUnstand = loadImg("img/gui/move_path_destination_unstandable_down.png");
		BufferedImage movePathStraight = loadImg("img/gui/move_path_straight.png");
		BufferedImage movePathTurn = loadImg("img/gui/move_path_turn_down_right.png");
		for (int dir = 0; dir < 4; dir++) {
			double rotateAngle = -(dir + 1) * Math.PI / 2;
			ect0.put("MovePathSource" + dir, Utils.imgRotate(moveSource, rotateAngle));
			ect0.put("MovePathDest" + dir, Utils.imgRotate(movePathDest, rotateAngle));
			ect0.put("MovePathDestUnstand" + dir, Utils.imgRotate(movePathDestUnstand, rotateAngle));
			ect0.put("MovePathTurn" + dir, Utils.imgRotate(movePathTurn, rotateAngle));
		}
		ect0.put("MovePathStraightVertical", Utils.imgRotate(movePathStraight, 0));
		ect0.put("MovePathStraightHorizontal", Utils.imgRotate(movePathStraight, Math.PI / 2));

		ect0.put(Label.Selection, loadImg("img/gui/selection.png"));
		ect0.put(Label.Passable, loadImg("img/gui/passable.png"));
		ect0.put(Label.Attackable, loadImg("img/gui/attackabe.png"));
		ect0.put(Label.UnitLocked, loadImg("img/gui/unit_locked.png"));
		ect0.put(Label.Delete, loadImg("img/gui/delete.png"));

		BiConsumer<Label, String> addUnitMenuIcon = (label, path) -> {
			BufferedImage img = loadImg("img/gui/unit_menu_box.png");
			BufferedImage icon = loadImg(path);
			if (img.getWidth() != icon.getWidth() || img.getHeight() != icon.getHeight())
				throw new IllegalArgumentException();
			img.getGraphics().drawImage(icon, 0, 0, null);
			ect0.put(label, img);
		};
		addUnitMenuIcon.accept(Label.UnitMenuTransportAir, "img/gui/unit_menu_transport_air.png");
		addUnitMenuIcon.accept(Label.UnitMenuTransportWater, "img/gui/unit_menu_transport_water.png");
		addUnitMenuIcon.accept(Label.UnitMenuTransportFinish, "img/gui/unit_menu_transport_finish.png");
		addUnitMenuIcon.accept(Label.UnitMenuRepair, "img/gui/unit_menu_repair.png");
		addUnitMenuIcon.accept(Label.UnitMenuCancel, "img/gui/unit_menu_cancel.png");
		ect = Collections.unmodifiableMap(ect0);
	}

	private static BufferedImage loadImg(String path) {
		try {
			return ImageIO.read(new File(path));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load img file: " + path, e);
		}
	};

	private static Direction orientationIdxToObj(int index) {
		switch (index) {
		case 0:
			return Direction.XPos;
		case 1:
			return Direction.YPos;
		case 2:
			return Direction.XNeg;
		case 3:
			return Direction.YNeg;
		default:
			throw new IllegalArgumentException();
		}
	}

	private static Map<UnitImgDesc, BufferedImage> addUnit(Unit.Type type, String path, int gestureNum) {
		BufferedImage[][] imgs = new BufferedImage[gestureNum][4];
		BufferedImage img = loadImg(path);
		int width = img.getWidth() / 4;
		int height = img.getHeight() / gestureNum;
		for (int gesture = 0; gesture < gestureNum; gesture++)
			for (int orientatoin = 0; orientatoin < 4; orientatoin++)
				imgs[gesture][orientatoin] = Utils.imgSub(img, orientatoin * width, gesture * height, width, height);

		Map<UnitImgDesc, BufferedImage> units = new HashMap<>();
		final int gesture = 0;
		for (int orientatoinIdx = 0; orientatoinIdx < 4; orientatoinIdx++) {
			BufferedImage redImg = imgs[gesture][orientatoinIdx];
			BufferedImage blueImg = toBlue(redImg);
			Direction orientatoin = orientationIdxToObj(orientatoinIdx);
			units.put(new UnitImgDesc(type, Team.Red, orientatoin), redImg);
			units.put(new UnitImgDesc(type, Team.Blue, orientatoin), blueImg);
		}
		return units;
	}

	private static BufferedImage miniUnitImg(BufferedImage img) {
		final int miniWidth = 28;
		int height = img.getHeight() * miniWidth / img.getWidth();
		return Utils.bufferedImageFromImage(img.getScaledInstance(miniWidth, height, Image.SCALE_SMOOTH));
	}

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

	static BufferedImage getUnitImage(IUnit unit, Direction orientation) {
		return Objects.requireNonNull(units.get(UnitImgDesc.of(unit, orientation)));
	}

	private static BufferedImage getImage0(Object obj) {
		boolean mini = obj instanceof Mini;
		if (mini)
			obj = ((Mini) obj).obj;

		if (obj instanceof Terrain) {
			Terrain terrain = (Terrain) obj;
			return terrains.get(terrain);

		} else if (obj instanceof Unit) {
			Unit unit = (Unit) obj;
			return (mini ? unitsMini : units).get(UnitImgDesc.of(unit, null));

		} else if (obj instanceof UnitDesc) {
			UnitDesc unit = (UnitDesc) obj;
			return (mini ? unitsMini : units).get(UnitImgDesc.of(unit, null));

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

	static class Mini {
		private final Object obj;

		private Mini(Object obj) {
			this.obj = obj;
		}

		static Mini of(Object obj) {
			return new Mini(obj);
		}
	}

}
