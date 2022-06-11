package com.ugav.battalion;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.ugav.battalion.Building.Factory;
import com.ugav.battalion.Building.OilRefinery;
import com.ugav.battalion.Terrain.ClearWater;
import com.ugav.battalion.Terrain.FlatLand;
import com.ugav.battalion.Terrain.Mountain;
import com.ugav.battalion.Unit.Soldier;
import com.ugav.battalion.Unit.Tank;

class ImageManager {

	private ImageManager() {
		throw new InternalError();
	}

	/* Terrains */
	static final String FLAT_LAND = "flat_land";
	static final String MOUNTAIN = "mountain";
	static final String CLEAR_WATER = "clear_water";

	/* Units */
	static final String SOLDIER = "soldier";
	static final String TANK = "tank";

	/* Buildings */
	static final String FACTORY = "facotry";
	static final String OIL_REFINERY = "oil_refinery";

	/* GUI */
	static final String SELECTION = "selection";

	private static final java.util.Map<String, BufferedImage> images = new HashMap<>();

	static {
		java.util.Map<String, String> images_paths = new HashMap<>();

		/* Terrains */
		images_paths.put(FLAT_LAND, "img/terrain/flat_land.png");
		images_paths.put(MOUNTAIN, "img/terrain/mountain.png");
		images_paths.put(CLEAR_WATER, "img/terrain/clear_water.png");

		/* Units */
		images_paths.put(SOLDIER, "img/unit/soldier.png");
		images_paths.put(TANK, "img/unit/tank.png");

		/* Buildings */
		images_paths.put(FACTORY, "img/building/facotry.png");
		images_paths.put(OIL_REFINERY, "img/building/oil_refinery.png");

		/* GUI */
		images_paths.put(SELECTION, "img/gui/selection.png");

		try {
			for (java.util.Map.Entry<String, String> entry : images_paths.entrySet()) {
				BufferedImage image = ImageIO.read(new File(entry.getValue()));
				images.put(entry.getKey(), image);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static BufferedImage getImage(String label) {
		BufferedImage image = images.get(label);
		if (image == null)
			throw new IllegalArgumentException("Image not found for label: " + label);
		return image;
	}

	static String getLabel(Drawable obj) {
		/* Terrains */
		if (obj instanceof FlatLand)
			return FLAT_LAND;
		else if (obj instanceof Mountain)
			return MOUNTAIN;
		else if (obj instanceof ClearWater)
			return CLEAR_WATER;
		/* Units */
		else if (obj instanceof Soldier)
			return SOLDIER;
		else if (obj instanceof Tank)
			return TANK;
		/* Buildings */
		else if (obj instanceof OilRefinery)
			return OIL_REFINERY;
		else if (obj instanceof Factory)
			return FACTORY;
		/* Not Found */
		else
			throw new IllegalArgumentException();

	}

}
