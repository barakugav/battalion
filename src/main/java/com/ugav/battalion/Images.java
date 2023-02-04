package com.ugav.battalion;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.UnitDesc;

class Images {

	private static final Map<Label, BufferedImage> images;
	static {
		Map<Label, BufferedImage> images0 = new HashMap<>();

		Function<String, BufferedImage> loadImg = path -> {
			try {
				return ImageIO.read(new File(path));
			} catch (IOException e) {
				System.err.println("Failed to load img file: " + path);
				throw new UncheckedIOException(e);
			}
		};
		BiConsumer<Label, String> addImg = (label, path) -> {
			images0.put(label, loadImg.apply(path));
		};
		BiConsumer<Pair<Label, Label>, String> addImgRedBlue = (labels, path) -> {
			BufferedImage imgRed = loadImg.apply(path);

			ColorModel colorModel = imgRed.getColorModel();
			WritableRaster swapped = imgRed.getRaster().createWritableChild(0, 0, imgRed.getWidth(), imgRed.getHeight(),
					0, 0, new int[] { 2, 1, 0, 3 });
			BufferedImage imgBlue = new BufferedImage(colorModel, swapped, colorModel.isAlphaPremultiplied(), null);

			images0.put(labels.e1, imgRed);
			images0.put(labels.e2, imgBlue);
		};

		/* Terrains */
		addImg.accept(Label.FlatLand, "img/terrain/flat_land.png");
		addImg.accept(Label.Mountain, "img/terrain/mountain.png");
		addImg.accept(Label.ClearWater, "img/terrain/water_clear.png");

		/* Units */
		addImgRedBlue.accept(Pair.of(Label.SoldierRed, Label.SoldierBlue), "img/unit/soldier.png");
		addImgRedBlue.accept(Pair.of(Label.TankRed, Label.TankBlue), "img/unit/tank.png");
		addImgRedBlue.accept(Pair.of(Label.ArtilleryRed, Label.ArtilleryBlue), "img/unit/artillery.png");
		addImgRedBlue.accept(Pair.of(Label.TurrentRed, Label.TurrentBlue), "img/unit/turrent.png");
		addImgRedBlue.accept(Pair.of(Label.ShipRed, Label.ShipBlue), "img/unit/ship_close_range.png");
		addImgRedBlue.accept(Pair.of(Label.AirplaneRed, Label.AirplaneBlue), "img/unit/airplane.png");

		/* Buildings */
		addImgRedBlue.accept(Pair.of(Label.FactoryRed, Label.FactoryBlue), "img/building/facotry.png");
		addImgRedBlue.accept(Pair.of(Label.OilRefineryRed, Label.OilRefineryBlue), "img/building/oil_refinery.png");

		/* GUI */
		addImg.accept(Label.Selection, "img/gui/selection.png");
		addImg.accept(Label.Reachable, "img/gui/reachable.png");
		addImg.accept(Label.Attackable, "img/gui/attackabe.png");
		addImg.accept(Label.UnitLocked, "img/gui/unit_locked.png");
		images = Collections.unmodifiableMap(images0);
	}

	private Images() {
		throw new InternalError();
	}

	static BufferedImage getImage(Drawable obj) {
		return getImage(Label.valueOf(obj));
	}

	static BufferedImage getImage(Label label) {
		BufferedImage image = images.get(label);
		if (image == null)
			throw new InternalError("Image not found for label: " + label);
		return image;
	}

	static enum Label {
		/* Terrains */
		FlatLand, Mountain, ClearWater,

		/* Units */
		SoldierRed, SoldierBlue, TankRed, TankBlue, ArtilleryRed, ArtilleryBlue, TurrentRed, TurrentBlue, ShipRed,
		ShipBlue, AirplaneRed, AirplaneBlue,

		/* Buildings */
		FactoryRed, FactoryBlue, OilRefineryRed, OilRefineryBlue,

		/* GUI */
		Selection, Reachable, Attackable, UnitLocked;

		static Label valueOf(Drawable obj) {
			if (obj instanceof Terrain) {
				Terrain terrain = (Terrain) obj;
				return valueOf(terrain.type);

			} else if (obj instanceof Unit) {
				Unit unit = (Unit) obj;
				return valueOf(unit.type, unit.getTeam());

			} else if (obj instanceof Building) {
				Building building = (Building) obj;
				return valueOf(building.type, building.getTeam());

			} else if (obj instanceof UnitDesc) {
				UnitDesc unit = (UnitDesc) obj;
				return valueOf(unit.type, unit.team);

			} else if (obj instanceof BuildingDesc) {
				BuildingDesc building = (BuildingDesc) obj;
				return valueOf(building.type, building.team);

			} else {
				throw new InternalError("Unsupported drawable object: " + obj);
			}
		}

		private static Label valueOf(Terrain.Type terrainType) {
			switch (terrainType) {
			case FlatLand:
				return FlatLand;
			case Mountain:
				return Mountain;
			case ClearWater:
				return ClearWater;
			default:
				throw new InternalError("Unsupported terrain type: " + terrainType);
			}
		}

		private static Label valueOf(Unit.Type unitType, Team team) {
			switch (unitType) {
			case Soldier:
				return team == Team.Red ? SoldierRed : SoldierBlue;
			case Tank:
				return team == Team.Red ? TankRed : TankBlue;
			case Artillery:
				return team == Team.Red ? ArtilleryRed : ArtilleryBlue;
			case Turrent:
				return team == Team.Red ? TurrentRed : TurrentBlue;
			case Ship:
				return team == Team.Red ? ShipRed : ShipBlue;
			case Airplane:
				return team == Team.Red ? AirplaneRed : AirplaneBlue;
			default:
				throw new InternalError("Unsupported unit type: " + unitType);
			}
		}

		private static Label valueOf(Building.Type buildingType, Team team) {
			switch (buildingType) {
			case Factory:
				return team == Team.Red ? FactoryRed : FactoryBlue;
			case OilRefinery:
				return team == Team.Red ? OilRefineryRed : OilRefineryBlue;
			default:
				throw new InternalError("Unsupported building type: " + buildingType);
			}
		}
	}

	interface Drawable {
	}

}
