package com.ugav.battalion;

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
import com.ugav.battalion.core.IBuilding;
import com.ugav.battalion.core.IUnit;
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

	private static class ImgDesc {

		private final Object[] keys;

		ImgDesc(Object... keys) {
			this.keys = Objects.requireNonNull(keys);
		}

		@Override
		public boolean equals(Object other) {
			if (other == this)
				return true;
			if (!(other instanceof ImgDesc))
				return false;
			ImgDesc o = (ImgDesc) other;
			return getClass().equals(o.getClass()) && Arrays.equals(keys, o.keys);
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

	private static class UnitImgDesc extends ImgDesc {

		private UnitImgDesc(Unit.Type type, Team team, Direction orientation, int gesture) {
			super(type, team, orientation, Integer.valueOf(gesture));
		}

		static UnitImgDesc of(IUnit unit, Direction orientation, int gesture) {
			orientation = orientation != null ? orientation : Direction.XPos;
			return new UnitImgDesc(unit.getType(), unit.getTeam(), orientation, gesture);
		}

	}

	private static class BuildingImgDesc extends ImgDesc {

		private BuildingImgDesc(Building.Type type, Team team, int gesture) {
			super(type, team, Integer.valueOf(gesture));
		}

		static BuildingImgDesc of(IBuilding building, int gesture) {
			return new BuildingImgDesc(building.getType(), building.getTeam(), gesture);
		}

	}

	private static final Map<Object, BufferedImage> images;
	static {
		Map<Object, BufferedImage> images0 = new HashMap<>();

		/* Terrain */
		for (int type = 1; type <= 5; type++)
			images0.put(Terrain.valueOf("FlatLand" + type), loadImg("img/terrain/flat_land_0" + type + ".png"));
		images0.put(Terrain.Trees, loadImg("img/terrain/forest.png"));
		images0.put(Terrain.Hills, loadImg("img/terrain/land_hills.png"));
		images0.put(Terrain.Mountain, loadImg("img/terrain/mountain.png"));
		images0.put(Terrain.MountainBig, loadImg("img/terrain/mountain_high.png"));
		images0.put(Terrain.Road, loadImg("img/terrain/road_vxvx.png"));
		images0.put(Terrain.BridgeLow, loadImg("img/terrain/bridge_low.png"));
		images0.put(Terrain.BridgeHigh, loadImg("img/terrain/bridge_high.png"));
		images0.put(Terrain.Shore, loadImg("img/terrain/shore.png"));
		images0.put(Terrain.ClearWater, loadImg("img/terrain/water_clear.png"));

		/* Units */
		BiConsumer<Unit.Type, String> addUnit = (type, path) -> images0.putAll(loadUnitImgs(type, path));
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
		addUnit.accept(Unit.Type.Submarine, "img/unit/submarine.png");
		addUnit.accept(Unit.Type.ShipTransporter, "img/unit/unit_ship_water.png");
		addUnit.accept(Unit.Type.Airplane, "img/unit/airplane.png");
		addUnit.accept(Unit.Type.Zeppelin, "img/unit/zeppelin.png");
		addUnit.accept(Unit.Type.AirTransporter, "img/unit/unit_ship_air.png");

		/* Buildings */
		BiConsumer<Building.Type, String> addBuilding = (type, path) -> images0.putAll(loadBuildingImgs(type, path));
		addBuilding.accept(Building.Type.Capital, "img/building/capital.png");
		addBuilding.accept(Building.Type.Factory, "img/building/facotry.png");
		addBuilding.accept(Building.Type.ControllerLand, "img/building/controller_land.png");
		addBuilding.accept(Building.Type.ControllerWater, "img/building/controller_water.png");
		addBuilding.accept(Building.Type.ControllerAir, "img/building/controller_air.png");
		addBuilding.accept(Building.Type.OilRefinery, "img/building/oil_refinery.png");
		addBuilding.accept(Building.Type.OilRefineryBig, "img/building/oil_refinery_big.png");
		addBuilding.accept(Building.Type.OilRig, "img/building/oil_rig.png");

		/* Ect */
		for (int quadrant = 0; quadrant < 4; quadrant++) {
			for (int variant = 0; variant < 4; variant++) {
				String suffix = "" + quadrant + variant;
				images0.put("WaterEdge" + suffix, loadImg("img/terrain/water_edge_" + suffix + ".png"));
			}
		}
		for (int variant = 0; variant < 16; variant++) {
			String suffix = "";
			for (int b = 0; b < 4; b++)
				suffix += ((variant & (1 << b)) != 0) ? "v" : "x";
			images0.put("Road_" + suffix, loadImg("img/terrain/road_" + suffix + ".png"));
		}
		for (boolean high : new boolean[] { true, false }) {
			for (int dir = 0; dir < 4; dir++) {
				for (boolean end : new boolean[] { true, false }) {
					String label = "bridge_" + (high ? "high" : "low");
					label += "_" + dir + (end ? "x" : "v");
					images0.put(label, loadImg("img/terrain/" + label + ".png"));
				}
			}
		}
		for (int quadrant = 0; quadrant < 4; quadrant++) {
			for (int variant = 1; variant < 4; variant++) {
				String suffix = "" + quadrant + variant;
				images0.put("Shore" + suffix, loadImg("img/terrain/shore_" + suffix + ".png"));
			}
		}

		images0.put("MovePathSourceNone", loadImg("img/gui/move_path_source_none.png"));
		BufferedImage moveSource = loadImg("img/gui/move_path_source_down.png");
		BufferedImage movePathDest = loadImg("img/gui/move_path_destination_down.png");
		BufferedImage movePathDestUnstand = loadImg("img/gui/move_path_destination_unstandable_down.png");
		BufferedImage movePathStraight = loadImg("img/gui/move_path_straight.png");
		BufferedImage movePathTurn = loadImg("img/gui/move_path_turn_down_right.png");
		for (int dir = 0; dir < 4; dir++) {
			double rotateAngle = -(dir + 1) * Math.PI / 2;
			images0.put("MovePathSource" + dir, Utils.imgRotate(moveSource, rotateAngle));
			images0.put("MovePathDest" + dir, Utils.imgRotate(movePathDest, rotateAngle));
			images0.put("MovePathDestUnstand" + dir, Utils.imgRotate(movePathDestUnstand, rotateAngle));
			images0.put("MovePathTurn" + dir, Utils.imgRotate(movePathTurn, rotateAngle));
		}
		images0.put("MovePathStraightVertical", Utils.imgRotate(movePathStraight, 0));
		images0.put("MovePathStraightHorizontal", Utils.imgRotate(movePathStraight, Math.PI / 2));

		images0.put(Label.Selection, loadImg("img/gui/selection.png"));
		images0.put(Label.Passable, loadImg("img/gui/passable.png"));
		images0.put(Label.Attackable, loadImg("img/gui/attackabe.png"));
		images0.put(Label.UnitLocked, loadImg("img/gui/unit_locked.png"));
		images0.put(Label.Delete, loadImg("img/gui/delete.png"));

		BiConsumer<Label, String> addUnitMenuIcon = (label, path) -> {
			BufferedImage img = loadImg("img/gui/unit_menu_box.png");
			BufferedImage icon = loadImg(path);
			if (img.getWidth() != icon.getWidth() || img.getHeight() != icon.getHeight())
				throw new IllegalArgumentException();
			img.getGraphics().drawImage(icon, 0, 0, null);
			images0.put(label, img);
		};
		addUnitMenuIcon.accept(Label.UnitMenuTransportAir, "img/gui/unit_menu_transport_air.png");
		addUnitMenuIcon.accept(Label.UnitMenuTransportWater, "img/gui/unit_menu_transport_water.png");
		addUnitMenuIcon.accept(Label.UnitMenuTransportFinish, "img/gui/unit_menu_transport_finish.png");
		addUnitMenuIcon.accept(Label.UnitMenuRepair, "img/gui/unit_menu_repair.png");
		addUnitMenuIcon.accept(Label.UnitMenuCancel, "img/gui/unit_menu_cancel.png");

		images = Collections.unmodifiableMap(images0);
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

	private static Map<UnitImgDesc, BufferedImage> loadUnitImgs(Unit.Type type, String path) {
		int gestureNum = getGestureNum(type);
		BufferedImage img = loadImg(path);
		int width = img.getWidth() / 4;
		int height = img.getHeight() / gestureNum;

		Map<UnitImgDesc, BufferedImage> units = new HashMap<>();
		for (int gesture = 0; gesture < gestureNum; gesture++) {
			for (int orientatoinIdx = 0; orientatoinIdx < 4; orientatoinIdx++) {
				BufferedImage redImg = Utils.imgSub(img, orientatoinIdx * width, gesture * height, width, height);
				BufferedImage blueImg = toBlue(redImg);
				Direction orientatoin = orientationIdxToObj(orientatoinIdx);
				units.put(new UnitImgDesc(type, Team.Red, orientatoin, gesture), redImg);
				units.put(new UnitImgDesc(type, Team.Blue, orientatoin, gesture), blueImg);
			}
		}
		return units;
	}

	private static Map<BuildingImgDesc, BufferedImage> loadBuildingImgs(Building.Type type, String path) {
		int gestureNum = getGestureNum(type);
		BufferedImage img = loadImg(path);
		int width = img.getWidth() / gestureNum;

		if (type == Building.Type.OilRefinery)
			System.out.println();

		Map<BuildingImgDesc, BufferedImage> buildings = new HashMap<>();
		for (int gesture = 0; gesture < gestureNum; gesture++) {
			BufferedImage redImg = Utils.imgSub(img, gesture * width, 0, width, img.getHeight());
			BufferedImage blueImg = toBlue(redImg);
			BufferedImage whiteImg = toWhite(redImg);
			buildings.put(new BuildingImgDesc(type, Team.Red, gesture), redImg);
			buildings.put(new BuildingImgDesc(type, Team.Blue, gesture), blueImg);
			buildings.put(new BuildingImgDesc(type, Team.None, gesture), whiteImg);
		}
		return buildings;

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

	static BufferedImage getUnitImage(IUnit unit, Direction orientation, int gesture) {
		return getImage(UnitImgDesc.of(unit, orientation, gesture));
	}

	static BufferedImage getBuildingImage(IBuilding building, int gesture) {
		return getImage(BuildingImgDesc.of(building, gesture));
	}

	static BufferedImage getImage(Object obj) {
		BufferedImage img = getImage0(obj);
		if (img == null)
			throw new IllegalArgumentException("No image found for object: " + obj);
		return img;
	}

	private static BufferedImage getImage0(Object obj) {
		if (obj instanceof IUnit) {
			IUnit unit = (IUnit) obj;
			return images.get(UnitImgDesc.of(unit, null, 0));

		} else if (obj instanceof IBuilding) {
			IBuilding building = (IBuilding) obj;
			return images.get(BuildingImgDesc.of(building, 0));

		} else {
			return images.get(obj);
		}
	}

	static int getGestureNum(Object obj) {
		if (obj instanceof Unit.Type) {
			switch ((Unit.Type) obj) {
			case Soldier:
			case Bazooka:
				return 5;
			case Tank:
			case TankBig:
			case TankAntiAir:
			case Artillery:
			case Mortar:
			case SpeedBoat:
			case Ship:
			case ShipAntiAir:
			case ShipArtillery:
			case Submarine:
			case ShipTransporter:
			case Airplane:
			case Zeppelin:
			case AirTransporter:
				return 4;
			case Turrent:
				return 1;
			default:
				/* fall through */
			}
		} else if (obj instanceof Building.Type) {
			switch ((Building.Type) obj) {
			case OilRefinery:
			case OilRefineryBig:
				return 7;
			case OilRig:
				return 4;
			case Factory:
			case Capital:
			case ControllerLand:
			case ControllerWater:
			case ControllerAir:
				return 1;
			default:
				/* fall through */
			}
		}
		throw new IllegalArgumentException("Unexpected value: " + obj);
	}

}
