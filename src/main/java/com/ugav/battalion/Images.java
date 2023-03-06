package com.ugav.battalion;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Direction;
import com.ugav.battalion.core.IBuilding;
import com.ugav.battalion.core.IUnit;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Utils;

class Images {

	private Images() {
	}

	static BufferedImage getImg(Object obj) {
		if (obj instanceof IUnit unit) {
			return Units.getDefault(unit);
		} else if (obj instanceof IBuilding building) {
			return Buildings.getDefault(building);
		} else if (obj instanceof Terrain terrain) {
			return Terrains.getDefault(terrain);
		} else {
			throw new IllegalArgumentException(Objects.toString(obj));
		}
	}

	static class Terrains {
		private Terrains() {
		}

		private static final Map<Desc, BufferedImage> imgs = new HashMap<>();
		static {
			for (int type = 1; type <= 7; type++)
				loadTerrain(Terrain.valueOf("FlatLand" + type), "img/terrain/flat_land_0" + type + ".png");
			for (int type = 1; type <= 2; type++)
				loadTerrain(Terrain.valueOf("Forest" + type), "img/terrain/forest_0" + type + ".png");
			for (int type = 1; type <= 4; type++)
				loadTerrain(Terrain.valueOf("Hills" + type), "img/terrain/hills_0" + type + ".png");
			for (int type = 1; type <= 4; type++)
				loadTerrain(Terrain.valueOf("Mountain" + type), "img/terrain/mountain_0" + type + ".png");
			loadTerrain(Terrain.Road, "img/terrain/road_vxvx.png");
			loadTerrain(Terrain.BridgeLow, "img/terrain/bridge_low.png");
			loadTerrain(Terrain.BridgeHigh, "img/terrain/bridge_high.png");
			loadTerrain(Terrain.ClearWater, "img/terrain/water_clear.png");
		}

		private static void loadTerrain(Terrain terrain, String path) {
			int gestureNum = gestureNum(terrain);
			BufferedImage img = loadImg(path);
			int width = img.getWidth() / gestureNum;
			for (int gesture = 0; gesture < gestureNum; gesture++)
				imgs.put(new Desc(terrain, gesture), Utils.imgSub(img, gesture * width, 0, width, img.getHeight()));
		}

		static BufferedImage getDefault(Terrain terrain) {
			return get(terrain, 0);
		}

		static BufferedImage get(Terrain terrain, int gesture) {
			return checkExists(imgs, new Desc(terrain, gesture));
		}

		static int gestureNum(Terrain terrain) {
			switch (terrain) {
			case FlatLand1:
			case FlatLand2:
			case FlatLand3:
			case FlatLand4:
			case FlatLand5:
			case FlatLand6:
			case FlatLand7:
			case Forest1:
			case Forest2:
			case Hills1:
			case Hills2:
			case Hills3:
			case Hills4:
			case Mountain1:
			case Mountain2:
			case Mountain3:
			case Mountain4:
			case Road:
			case BridgeLow:
			case BridgeHigh:
				return 1;
			case ClearWater:
			case Shore:
				return 4;
			default:
				throw new IllegalArgumentException("Unexpected value: " + terrain);
			}
		}

		private static class Desc extends ImgDesc {

			private Desc(Terrain terrain, int gesture) {
				super(terrain, Integer.valueOf(gesture));
			}

		}
	}

	static class Units {
		private Units() {
		}

		private static final Map<Desc, BufferedImage> imgs = new HashMap<>();
		static {
			loadUnit(Unit.Type.Soldier, "img/unit/soldier.png");
			loadUnit(Unit.Type.Bazooka, "img/unit/bazooka.png");
			loadUnit(Unit.Type.Tank, "img/unit/tank.png");
			loadUnit(Unit.Type.TankBig, "img/unit/tank_big.png");
			loadUnit(Unit.Type.TankInvisible, "img/unit/tank_invisible.png");
			loadUnit(Unit.Type.TankAntiAir, "img/unit/tank_anti_air.png");
			loadUnit(Unit.Type.Artillery, "img/unit/artillery.png");
			loadUnit(Unit.Type.Mortar, "img/unit/mortar.png");
			loadUnit(Unit.Type.Turrent, "img/unit/turrent.png");
			loadUnit(Unit.Type.SpeedBoat, "img/unit/speed_boat.png");
			loadUnit(Unit.Type.Ship, "img/unit/ship_close_range.png");
			loadUnit(Unit.Type.ShipAntiAir, "img/unit/ship_anti_air.png");
			loadUnit(Unit.Type.ShipArtillery, "img/unit/ship_artillery.png");
			loadUnit(Unit.Type.Submarine, "img/unit/submarine.png");
			loadUnit(Unit.Type.ShipTransporter, "img/unit/unit_ship_water.png");
			loadUnit(Unit.Type.Airplane, "img/unit/airplane.png");
			loadUnit(Unit.Type.Zeppelin, "img/unit/zeppelin.png");
			loadUnit(Unit.Type.AirTransporter, "img/unit/unit_ship_air.png");
		}

		private static void loadUnit(Unit.Type type, String path) {
			boolean differentGestures = hasDifferentStandMoveGesture(type);
			int gestureNumStand = standGestureNum(type);
			int gestureNum = gestureNumStand + moveGestureNum0(type);
			BufferedImage img = loadImg(path);
			int width = img.getWidth() / 4;
			int height = img.getHeight() / gestureNum;

			for (int gesture = 0; gesture < gestureNum; gesture++) {
				for (int orientatoinIdx = 0; orientatoinIdx < 4; orientatoinIdx++) {
					BufferedImage redImg = Utils.imgSub(img, orientatoinIdx * width, gesture * height, width, height);
					BufferedImage blueImg = toBlue(redImg);
					Direction orientatoin = orientationIdxToObj(orientatoinIdx);

					int standGesture = -1, moveGesture = -1;
					if (differentGestures && gesture < gestureNumStand) {
						standGesture = gesture;
					} else if (differentGestures && gesture >= gestureNumStand) {
						moveGesture = gesture - gestureNumStand;
					} else {
						standGesture = moveGesture = gesture;
					}

					if (standGesture >= 0) {
						imgs.put(Desc.ofStand(type, Team.Red, orientatoin, standGesture), redImg);
						imgs.put(Desc.ofStand(type, Team.Blue, orientatoin, standGesture), blueImg);
					}
					if (moveGesture >= 0) {
						imgs.put(Desc.ofMove(type, Team.Red, orientatoin, moveGesture), redImg);
						imgs.put(Desc.ofMove(type, Team.Blue, orientatoin, moveGesture), blueImg);
					}
				}
			}
		}

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

		static BufferedImage getDefault(IUnit unit) {
			return standImg(unit, null, 0);
		}

		static BufferedImage standImg(IUnit unit, Direction orientation, int gesture) {
			if (gesture >= standGestureNum(unit.getType()))
				throw new IllegalArgumentException();
			return checkExists(imgs, Desc.ofStand(unit, orientation, gesture));
		}

		static BufferedImage moveImg(IUnit unit, Direction orientation, int gesture) {
			if (gesture >= moveGestureNum(unit.getType()))
				throw new IllegalArgumentException();
			return checkExists(imgs, Desc.ofMove(unit, orientation, gesture));
		}

		static int standGestureNum(Unit.Type type) {
			switch (type) {
			case Soldier:
			case Bazooka:
				return 1;
			case Tank:
			case TankBig:
			case TankInvisible:
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
				throw new IllegalArgumentException("Unexpected value: " + type);
			}
		}

		static int moveGestureNum(Unit.Type type) {
			return hasDifferentStandMoveGesture(type) ? moveGestureNum0(type) : standGestureNum(type);
		}

		private static boolean hasDifferentStandMoveGesture(Unit.Type type) {
			return moveGestureNum0(type) > 0;
		}

		private static int moveGestureNum0(Unit.Type type) {
			switch (type) {
			case Soldier:
			case Bazooka:
				return 4;
			default:
				return 0;
			}
		}

		private static class Desc extends ImgDesc {

			private static final Object StandTag = new Object();
			private static final Object MoveTag = new Object();

			private Desc(Unit.Type type, Team team, Direction orientation, Object gestureTag, int gesture) {
				super(type, team, orientation, gestureTag, Integer.valueOf(gesture));
			}

			static Desc ofStand(Unit.Type type, Team team, Direction orientation, int gesture) {
				orientation = orientation != null ? orientation : Direction.XPos;
				return new Desc(type, team, orientation, StandTag, gesture);
			}

			static Desc ofStand(IUnit unit, Direction orientation, int gesture) {
				return ofStand(unit.getType(), unit.getTeam(), orientation, gesture);
			}

			static Desc ofMove(Unit.Type type, Team team, Direction orientation, int gesture) {
				orientation = orientation != null ? orientation : Direction.XPos;
				return new Desc(type, team, orientation, MoveTag, gesture);
			}

			static Desc ofMove(IUnit unit, Direction orientation, int gesture) {
				return ofMove(unit.getType(), unit.getTeam(), orientation, gesture);
			}

		}

	}

	static class Buildings {
		private Buildings() {
		}

		private static final Map<Desc, BufferedImage> imgs = new HashMap<>();
		static {
			loadBuilding(Building.Type.Capital, "img/building/capital.png");
			loadBuilding(Building.Type.Factory, "img/building/facotry.png");
			loadBuilding(Building.Type.ControllerLand, "img/building/controller_land.png");
			loadBuilding(Building.Type.ControllerWater, "img/building/controller_water.png");
			loadBuilding(Building.Type.ControllerAir, "img/building/controller_air.png");
			loadBuilding(Building.Type.OilRefinery, "img/building/oil_refinery.png");
			loadBuilding(Building.Type.OilRefineryBig, "img/building/oil_refinery_big.png");
			loadBuilding(Building.Type.OilRig, "img/building/oil_rig.png");
		}

		static private void loadBuilding(Building.Type type, String path) {
			int gestureNum = gestureNum(type);
			BufferedImage img = loadImg(path);
			int width = img.getWidth() / gestureNum;
			for (int gesture = 0; gesture < gestureNum; gesture++) {
				BufferedImage redImg = Utils.imgSub(img, gesture * width, 0, width, img.getHeight());
				BufferedImage blueImg = toBlue(redImg);
				BufferedImage whiteImg = toWhite(redImg);
				imgs.put(new Desc(type, Team.Red, gesture), redImg);
				imgs.put(new Desc(type, Team.Blue, gesture), blueImg);
				imgs.put(new Desc(type, Team.None, gesture), whiteImg);
			}
		}

		static BufferedImage getDefault(IBuilding building) {
			return get(building, 0);
		}

		static BufferedImage get(IBuilding building, int gesture) {
			return checkExists(imgs, Desc.of(building, gesture));
		}

		static int gestureNum(Building.Type type) {
			switch (type) {
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
				throw new IllegalArgumentException("Unexpected value: " + type);
			}
		}

		private static class Desc extends ImgDesc {

			private Desc(Building.Type type, Team team, int gesture) {
				super(type, team, Integer.valueOf(gesture));
			}

			static Desc of(IBuilding building, int gesture) {
				return new Desc(building.getType(), building.getTeam(), gesture);
			}

		}
	}

	static class WaterEdges {
		private WaterEdges() {
		}

		private static final Map<Desc, BufferedImage> imgs = new HashMap<>();
		static {
			boolean[] bs = new boolean[] { false, true };
			for (int quadrant = 0; quadrant < 4; quadrant++) {
				for (boolean connected1 : bs) {
					for (boolean connected2 : bs) {
						int variant = (connected1 ? 1 : 0) + (connected2 ? 2 : 0);
						String suffix = "" + quadrant + variant;
						BufferedImage img = loadImg("img/terrain/water_edge_" + suffix + ".png");
						int gestureNum = Terrains.gestureNum(Terrain.ClearWater);
						int width = img.getWidth() / gestureNum;
						for (int gesture = 0; gesture < gestureNum; gesture++) {
							BufferedImage subImg = Utils.imgSub(img, gesture * width, 0, width, img.getHeight());
							imgs.put(new Desc(quadrant, connected1, connected2, gesture), subImg);
						}
					}
				}
			}
		}

		static BufferedImage get(int quadrant, boolean connected1, boolean connected2, int gesture) {
			return checkExists(imgs, new Desc(quadrant, connected1, connected2, gesture));
		}

		private static class Desc extends ImgDesc {

			@SuppressWarnings("boxing")
			Desc(int quadrant, boolean connected1, boolean connected2, int gesture) {
				super(quadrant, connected1, connected2, gesture);
			}

		}

	}

	static class Roads {
		private Roads() {
		}

		private final static Map<Desc, BufferedImage> imgs = new HashMap<>();
		static {
			boolean[] bs = new boolean[] { false, true };
			for (boolean xpos : bs) {
				for (boolean yneg : bs) {
					for (boolean xneg : bs) {
						for (boolean ypos : bs) {
							String suffix = "";
							suffix += xpos ? "v" : "x";
							suffix += yneg ? "v" : "x";
							suffix += xneg ? "v" : "x";
							suffix += ypos ? "v" : "x";
							imgs.put(new Desc(xpos, yneg, xneg, ypos), loadImg("img/terrain/road_" + suffix + ".png"));
						}
					}
				}
			}
		}

		static BufferedImage get(boolean xpos, boolean yneg, boolean xneg, boolean ypos) {
			return checkExists(imgs, new Desc(xpos, yneg, xneg, ypos));
		}

		private static class Desc extends ImgDesc {

			@SuppressWarnings("boxing")
			Desc(boolean xpos, boolean yneg, boolean xneg, boolean ypos) {
				super(xpos, yneg, xneg, ypos);
			}
		}

	}

	static class Bridges {
		private Bridges() {
		}

		private static final Map<Desc, BufferedImage> imgs = new HashMap<>();
		static {
			boolean[] bs = new boolean[] { false, true };
			for (boolean high : bs) {
				for (Direction dir : Direction.values()) {
					for (boolean isConnectedToLand : bs) {
						String suffix = (high ? "high" : "low");
						suffix += "_" + dir.ordinal() + (isConnectedToLand ? "x" : "v");
						imgs.put(new Desc(high, dir, isConnectedToLand),
								loadImg("img/terrain/bridge_" + suffix + ".png"));
					}
				}
			}
		}

		static BufferedImage get(Terrain bridge, Direction dir, boolean isConnectedToLand) {
			if (!EnumSet.of(Terrain.BridgeLow, Terrain.BridgeHigh).contains(bridge))
				throw new IllegalArgumentException(Objects.toString(bridge));
			boolean isHigh = bridge == Terrain.BridgeHigh;
			return checkExists(imgs, new Desc(isHigh, dir, isConnectedToLand));
		}

		private static class Desc extends ImgDesc {

			@SuppressWarnings("boxing")
			Desc(boolean isHign, Direction dir, boolean isConnectedToLand) {
				super(isHign, dir, isConnectedToLand);
			}

		}

	}

	static class Shores {
		private Shores() {
		}

		private static final Map<Desc, BufferedImage> imgs = new HashMap<>();
		static {
			boolean[] bs = new boolean[] { false, true };
			for (int quadrant = 0; quadrant < 4; quadrant++) {
				for (boolean connected1 : bs) {
					for (boolean connected2 : bs) {
						if (!connected1 && !connected2)
							continue;
						int variant = (connected1 ? 1 : 0) + (connected2 ? 2 : 0);
						String suffix = "" + quadrant + variant;
						BufferedImage img = loadImg("img/terrain/shore_" + suffix + ".png");
						int gestureNum = Terrains.gestureNum(Terrain.Shore);
						int width = img.getWidth() / gestureNum;
						for (int gesture = 0; gesture < gestureNum; gesture++) {
							BufferedImage subImg = Utils.imgSub(img, gesture * width, 0, width, img.getHeight());
							imgs.put(new Desc(quadrant, connected1, connected2, gesture), subImg);
						}
					}
				}
			}
		}

		static BufferedImage get(int quadrant, boolean connected1, boolean connected2, int gesture) {
			return checkExists(imgs, new Desc(quadrant, connected1, connected2, gesture));
		}

		private static class Desc extends ImgDesc {

			@SuppressWarnings("boxing")
			Desc(int quadrant, boolean connected1, boolean connected2, int gesture) {
				super(quadrant, connected1, connected2, gesture);
			}

		}

	}

	static class MiniMap {
		private MiniMap() {
		}

		private static final Map<Terrain.Category, BufferedImage> terrains = new HashMap<>();
		private static final Map<Team, BufferedImage> unit = new HashMap<>();
		private static final Map<Team, BufferedImage> building = new HashMap<>();
		static {
			terrains.put(Terrain.Category.FlatLand, loadImg("img/gui/minimap_land.png"));
			terrains.put(Terrain.Category.Forest, loadImg("img/gui/minimap_land.png"));
			terrains.put(Terrain.Category.Hiils, loadImg("img/gui/minimap_land.png"));
			terrains.put(Terrain.Category.Mountain, loadImg("img/gui/minimap_extreme_land.png"));
			terrains.put(Terrain.Category.Water, loadImg("img/gui/minimap_water.png"));
			terrains.put(Terrain.Category.Shore, loadImg("img/gui/minimap_shore.png"));
			terrains.put(Terrain.Category.Road, loadImg("img/gui/minimap_road.png"));
			terrains.put(Terrain.Category.BridgeLow, loadImg("img/gui/minimap_road.png"));
			terrains.put(Terrain.Category.BridgeHigh, loadImg("img/gui/minimap_road.png"));
			unit.put(Team.Red, loadImg("img/gui/minimap_unit.png"));
			unit.put(Team.Blue, toBlue(loadImg("img/gui/minimap_unit.png")));
			building.put(Team.Red, loadImg("img/gui/minimap_building.png"));
			building.put(Team.Blue, toBlue(loadImg("img/gui/minimap_building.png")));
			building.put(Team.None, toWhite(loadImg("img/gui/minimap_building.png")));
		}

		static BufferedImage terrain(Terrain.Category t) {
			return checkExists(terrains, t);
		}

		static BufferedImage unit(Team team) {
			return checkExists(unit, team);
		}

		static BufferedImage building(Team team) {
			return checkExists(building, team);
		}

	}

	static final BufferedImage Selection;
	static final BufferedImage Passable;
	static final BufferedImage Attackable;
	static final BufferedImage PotentiallyAttackable;
	static final BufferedImage UnitLocked;
	static final BufferedImage Delete;
	static final BufferedImage UnitMenuTransportAir;
	static final BufferedImage UnitMenuTransportWater;
	static final BufferedImage UnitMenuTransportFinish;
	static final BufferedImage UnitMenuRepair;
	static final BufferedImage UnitMenuCancel;
	static final BufferedImage FactoryMenuImg;
	static final BufferedImage CheckboxUnselected;
	static final BufferedImage CheckboxUnselectedHovered;
	static final BufferedImage CheckboxSelected;
	static final BufferedImage CheckboxSelectedHovered;
	static final BufferedImage CheckboxPressed;
	static final BufferedImage FrameIcon;
	static {
		Selection = loadImg("img/gui/selection.png");
		Passable = loadImg("img/gui/passable.png");
		Attackable = loadImg("img/gui/attackabe.png");
		PotentiallyAttackable = loadImg("img/gui/potentially_attackable.png");
		UnitLocked = loadImg("img/gui/unit_locked.png");
		Delete = loadImg("img/gui/delete.png");
		FactoryMenuImg = loadImg("img/gui/factory_menu_img.png");
		CheckboxUnselected = loadImg("img/gui/checkbox_unselected.png");
		CheckboxUnselectedHovered = loadImg("img/gui/checkbox_unselected_hovered.png");
		CheckboxSelected = loadImg("img/gui/checkbox_selected.png");
		CheckboxSelectedHovered = loadImg("img/gui/checkbox_selected_hovered.png");
		CheckboxPressed = loadImg("img/gui/checkbox_pressed.png");
		FrameIcon = loadImg("img/gui/frame_icon.png");

		Function<String, BufferedImage> addUnitMenuIcon = path -> {
			BufferedImage img = loadImg("img/gui/unit_menu_box.png");
			BufferedImage icon = loadImg(path);
			if (img.getWidth() != icon.getWidth() || img.getHeight() != icon.getHeight())
				throw new IllegalArgumentException();
			img.getGraphics().drawImage(icon, 0, 0, null);
			return img;
		};
		UnitMenuTransportAir = addUnitMenuIcon.apply("img/gui/unit_menu_transport_air.png");
		UnitMenuTransportWater = addUnitMenuIcon.apply("img/gui/unit_menu_transport_water.png");
		UnitMenuTransportFinish = addUnitMenuIcon.apply("img/gui/unit_menu_transport_finish.png");
		UnitMenuRepair = addUnitMenuIcon.apply("img/gui/unit_menu_repair.png");
		UnitMenuCancel = addUnitMenuIcon.apply("img/gui/unit_menu_cancel.png");
	}

	static class MovePath {
		private MovePath() {
		}

		static final BufferedImage None;
		static final BufferedImage Vertical;
		static final BufferedImage Horizontal;
		private static final BufferedImage[] source = new BufferedImage[Direction.values().length];
		private static final BufferedImage[] destination = new BufferedImage[Direction.values().length];
		private static final BufferedImage[] destinationNoStand = new BufferedImage[Direction.values().length];
		private static final BufferedImage[] turn = new BufferedImage[Direction.values().length];
		static {
			None = loadImg("img/gui/move_path_source_none.png");
			BufferedImage straight = loadImg("img/gui/move_path_straight.png");
			Vertical = Utils.imgRotate(straight, 0);
			Horizontal = Utils.imgRotate(straight, Math.PI / 2);

			BufferedImage sourceImg = loadImg("img/gui/move_path_source_down.png");
			BufferedImage destImg = loadImg("img/gui/move_path_destination_down.png");
			BufferedImage destNoStandImg = loadImg("img/gui/move_path_destination_unstandable_down.png");
			BufferedImage turnImg = loadImg("img/gui/move_path_turn_down_right.png");
			for (Direction dir : Direction.values()) {
				int dirIdx = dir.ordinal();
				double rotateAngle = (dirIdx - 1) * Math.PI / 2;
				source[dirIdx] = Utils.imgRotate(sourceImg, rotateAngle);
				destination[dirIdx] = Utils.imgRotate(destImg, rotateAngle);
				destinationNoStand[dirIdx] = Utils.imgRotate(destNoStandImg, rotateAngle);
				turn[dirIdx] = Utils.imgRotate(turnImg, (3 - dirIdx) * Math.PI / 2);
			}
		}

		static BufferedImage source(Direction dir) {
			return source[dir.ordinal()];
		}

		static BufferedImage destination(Direction dir) {
			return destination[dir.ordinal()];
		}

		static BufferedImage destinationNoStand(Direction dir) {
			return destinationNoStand[dir.ordinal()];
		}

		static BufferedImage turn(Direction d1, Direction d2) {
			final int dirs = Direction.values().length;
			int i1 = dirs - 1 - d1.ordinal(), i2 = dirs - 1 - d2.ordinal();
			return ((i1 + 1) % dirs == i2) ? turn[i2] : turn[i1];
		}

	}

	static class Ect {
		private Ect() {
		}

		static final int FlagGestureNum = 4;
		static final int AttackGestureNum = 3;
		static final int ExplosionGestureNum = 15;

		private static final BufferedImage[][] flag = new BufferedImage[Team.values().length][FlagGestureNum];
		private static final BufferedImage[][] flagGlow = new BufferedImage[Team.values().length][FlagGestureNum];
		private static final BufferedImage[] attack = new BufferedImage[ExplosionGestureNum];
		private static final BufferedImage[] explosion = new BufferedImage[ExplosionGestureNum];
		static {
			loadFlagImages("img/building/flag.png");
			loadFlagGlowImages("img/building/flag_glow.png");
			loadAttackImages("img/gui/attack_animation.png");
			loadExplosionImages("img/gui/explosion_animation.png");
		}

		static BufferedImage flag(Team team, int gesture) {
			return flag[team.ordinal()][gesture];
		}

		static BufferedImage flagGlow(Team team, int gesture) {
			return flagGlow[team.ordinal()][gesture];
		}

		static BufferedImage attack(int gesture) {
			return attack[gesture];
		}

		static BufferedImage explosion(int gesture) {
			return explosion[gesture];
		}

		private static void loadFlagImages(String path) {
			int gestureNum = FlagGestureNum;
			BufferedImage img = loadImg(path);
			int width = img.getWidth() / gestureNum;
			for (int gesture = 0; gesture < gestureNum; gesture++) {
				BufferedImage redImg = Utils.imgSub(img, gesture * width, 0, width, img.getHeight());
				BufferedImage blueImg = toBlue(redImg);
				BufferedImage whiteImg = toWhite(redImg);
				flag[Team.Red.ordinal()][gesture] = redImg;
				flag[Team.Blue.ordinal()][gesture] = blueImg;
				flag[Team.None.ordinal()][gesture] = whiteImg;
			}
		}

		private static void loadFlagGlowImages(String path) {
			int gestureNum = FlagGestureNum;
			BufferedImage img = loadImg(path);
			int width = img.getWidth() / gestureNum;
			for (int gesture = 0; gesture < gestureNum; gesture++) {
				BufferedImage redImg = Utils.imgSub(img, gesture * width, 0, width, img.getHeight());
				BufferedImage blueImg = toBlue(redImg);
				BufferedImage whiteImg = toWhite(redImg);
				flagGlow[Team.Red.ordinal()][gesture] = redImg;
				flagGlow[Team.Blue.ordinal()][gesture] = blueImg;
				flagGlow[Team.None.ordinal()][gesture] = whiteImg;
			}
		}

		private static void loadAttackImages(String path) {
			int gestureNum = AttackGestureNum;
			BufferedImage fullImg = loadImg(path);
			int width = fullImg.getWidth() / gestureNum;
			for (int gesture = 0; gesture < gestureNum; gesture++)
				attack[gesture] = Utils.imgSub(fullImg, gesture * width, 0, width, fullImg.getHeight());
		}

		private static void loadExplosionImages(String path) {
			int gestureNum = ExplosionGestureNum;
			BufferedImage fullImg = loadImg(path);
			int width = fullImg.getWidth() / gestureNum;
			for (int gesture = 0; gesture < gestureNum; gesture++)
				explosion[gesture] = Utils.imgSub(fullImg, gesture * width, 0, width, fullImg.getHeight());
		}

	}

	private static <K> BufferedImage checkExists(Map<K, BufferedImage> imgs, K key) {
		BufferedImage img = imgs.get(key);
		if (img == null)
			throw new IllegalArgumentException("No image found for key: " + key);
		return img;
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

}
