package com.ugav.battalion;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.ugav.battalion.Building.Factory;
import com.ugav.battalion.Building.OilRefinery;
import com.ugav.battalion.Terrain.ClearWater;
import com.ugav.battalion.Terrain.FlatLand;
import com.ugav.battalion.Terrain.Mountain;
import com.ugav.battalion.Unit.Soldier;
import com.ugav.battalion.Unit.Tank;

class Images {

	private final Map<Label, BufferedImage> images;

	Images() {
		images = new HashMap<>();

		Function<String, BufferedImage> loadImg = path -> {
			try {
				return ImageIO.read(new File(path));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};
		BiConsumer<Label, String> addImg = (label, path) -> {
			images.put(label, loadImg.apply(path));
		};
		BiConsumer<Pair<Label, Label>, String> addImgRedBlue = (labels, path) -> {
			BufferedImage imgRed = loadImg.apply(path);

			ColorModel colorModel = imgRed.getColorModel();
			WritableRaster swapped = imgRed.getRaster().createWritableChild(0, 0, imgRed.getWidth(), imgRed.getHeight(),
					0, 0, new int[] { 2, 1, 0, 3 });
			BufferedImage imgBlue = new BufferedImage(colorModel, swapped, colorModel.isAlphaPremultiplied(), null);

			images.put(labels.e1, imgRed);
			images.put(labels.e2, imgBlue);
		};

		/* Terrains */
		addImg.accept(Label.FlatLand, "img/terrain/flat_land.png");
		addImg.accept(Label.Mountain, "img/terrain/mountain.png");
		addImg.accept(Label.ClearWater, "img/terrain/clear_water.png");

		/* Units */
		addImgRedBlue.accept(Pair.of(Label.SoldierRed, Label.SoldierBlue), "img/unit/soldier.png");
		addImgRedBlue.accept(Pair.of(Label.TankRed, Label.TankBlue), "img/unit/tank.png");

		/* Buildings */
		addImgRedBlue.accept(Pair.of(Label.FactoryRed, Label.FactoryBlue), "img/building/facotry.png");
		addImgRedBlue.accept(Pair.of(Label.OilRefineryRed, Label.OilRefineryBlue), "img/building/oil_refinery.png");

		/* GUI */
		addImg.accept(Label.Selection, "img/gui/selection.png");
	}

	BufferedImage getImage(Label label) {
		BufferedImage image = images.get(label);
		if (image == null)
			throw new InternalError("Image not found for label: " + label);
		return image;
	}

	static enum Label {
		/* Terrains */
		FlatLand, Mountain, ClearWater,

		/* Units */
		SoldierRed, SoldierBlue, TankRed, TankBlue,

		/* Buildings */
		FactoryRed, FactoryBlue, OilRefineryRed, OilRefineryBlue,

		/* GUI */
		Selection;

		static Label of(Drawable obj) {
			if (obj instanceof Terrain) {
				Terrain terrain = (Terrain) obj;
				if (terrain instanceof FlatLand)
					return FlatLand;
				else if (terrain instanceof Mountain)
					return Mountain;
				else if (terrain instanceof ClearWater)
					return ClearWater;

			} else if (obj instanceof Unit) {
				Unit unit = (Unit) obj;
				Team team = unit.getTeam();
				if (unit instanceof Soldier)
					return team == Team.Red ? SoldierRed : SoldierBlue;
				else if (unit instanceof Tank)
					return team == Team.Red ? TankRed : TankBlue;

			} else if (obj instanceof Building) {
				Building building = (Building) obj;
				Team team = building.getTeam();
				if (building instanceof OilRefinery)
					return team == Team.Red ? OilRefineryRed : OilRefineryBlue;
				else if (building instanceof Factory)
					return team == Team.Red ? FactoryRed : FactoryBlue;
				return FactoryRed;
			}
			throw new InternalError();
		}
	}

}
