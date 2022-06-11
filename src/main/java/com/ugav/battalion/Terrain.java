package com.ugav.battalion;

public class Terrain implements Drawable {

	public static final FlatLand FLAT_LAND = new FlatLand();
	public static final ClearWater CLEAR_WATER = new ClearWater();
	public static final Mountain MOUNTAIN = new Mountain();

	static class Land extends Terrain {

		private Land() {
		}

	}

	static class FlatLand extends Land {

		private FlatLand() {
		}

	}

	static class Mountain extends Land {

		private Mountain() {
		}

	}

	static class ClearWater extends Terrain {

		private ClearWater() {
		}

	}

}