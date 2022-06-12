package com.ugav.battalion;

import com.ugav.battalion.Images.Drawable;

class Terrain implements Drawable {

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

	enum Type {
		FlatLand(Category.Land), Mountain(Category.Mountain), ClearWater(Category.Water);

		final Category category;

		Type(Category category) {
			this.category = category;
		}
	}

	final Type type;

	Terrain(Type type) {
		this.type = type;
	}

	static final FlatLand FLAT_LAND = new FlatLand();
	static final ClearWater CLEAR_WATER = new ClearWater();
	static final Mountain MOUNTAIN = new Mountain();

	static class Land extends Terrain {

		private Land(Type type) {
			super(type);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

	}

	static class FlatLand extends Land {

		private FlatLand() {
			super(Type.FlatLand);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

	}

	static class Mountain extends Land {

		private Mountain() {
			super(Type.Mountain);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

	}

	static class ClearWater extends Terrain {

		private ClearWater() {
			super(Type.ClearWater);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

	}

}