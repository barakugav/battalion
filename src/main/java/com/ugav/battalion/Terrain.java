package com.ugav.battalion;

import com.ugav.battalion.Images.Drawable;

enum Terrain implements Drawable {

	FlatLand(Category.Land), Mountain(Category.Mountain), ClearWater(Category.Water);

	final Category category;

	private Terrain(Category category) {
		this.category = category;
	}

	enum Category {
		Land, Mountain, Shore, Water;

		String shortName() {
			switch (this) {
			case Land:
				return "L";
			case Mountain:
				return "M";
			case Shore:
				return "S";
			case Water:
				return "W";
			default:
				throw new InternalError();
			}
		}
	}

}