package com.ugav.battalion;

import com.ugav.battalion.Images.Drawable;

enum Terrain implements Drawable {

	FlatLand1(Category.FlatLand), FlatLand2(Category.FlatLand), FlatLand3(Category.FlatLand),
	FlatLand4(Category.FlatLand), FlatLand5(Category.FlatLand),

	Trees(Category.RoughLand), Hills(Category.RoughLand),

	Mountain(Category.ExtremeLand), MountainBig(Category.ExtremeLand),

	ClearWater(Category.Water);

	final Category category;

	private Terrain(Category category) {
		this.category = category;
	}

	enum Category {
		FlatLand, RoughLand, ExtremeLand, Shore, Water;
	}

}