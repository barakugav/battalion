package com.ugav.battalion;

class Terrain implements Drawable {

	static final FlatLand FLAT_LAND = new FlatLand();
	static final ClearWater CLEAR_WATER = new ClearWater();
	static final Mountain MOUNTAIN = new Mountain();

	static class Land extends Terrain {

		private Land() {
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

	}

	static class FlatLand extends Land {

		private FlatLand() {
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

	}

	static class Mountain extends Land {

		private Mountain() {
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

	}

	static class ClearWater extends Terrain {

		private ClearWater() {
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

	}

}