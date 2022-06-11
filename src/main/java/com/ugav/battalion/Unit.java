package com.ugav.battalion;

interface Unit extends Entity {

	int getMaxHealth();

	int getHealth();

	int getDamge();

	int getMaxMove();

	@Override
	Unit deepCopy();

	void setPos(int x, int y);

	int getX();

	int getY();

	void setMap(Map map);

	Map getMap();

	boolean isMoveValid(int x, int y);

	boolean isAttackValid(int x, int y);

	abstract class UnitAbstract extends EntityAbstract implements Unit {

		private int health;
		private int x, y;
		private Map map;

		UnitAbstract(Team team) {
			super(team);
			if (getTeam() == Team.None)
				throw new IllegalArgumentException();
			health = getMaxHealth();
		}

		@Override
		public final void setTeam(Team team) {
			throw new UnsupportedOperationException();
		}

		@Override
		public final int getHealth() {
			return health;
		}

		@Override
		public void setPos(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public int getX() {
			return x;
		}

		@Override
		public int getY() {
			return y;
		}

		@Override
		public void setMap(Map map) {
			this.map = map;
		}

		@Override
		public Map getMap() {
			return map;
		}

	}

	static abstract class CloseRangeUnitAbstract extends UnitAbstract implements CloseRangeUnit {

		CloseRangeUnitAbstract(Team team) {
			super(team);
		}

		private boolean[][] calcMovableMap() {
			final int xFrom = getX(), yFrom = getY();
			Map map = getMap();
			int xLen = map.getXLen(), yLen = map.getYLen();

			boolean[][] moveableMap = new boolean[xLen][yLen];

			/* Moveable map */
			int[][] moveableMap0 = new int[xLen][yLen];
			for (int x = 0; x < xLen; x++)
				for (int y = 0; y < yLen; y++)
					moveableMap0[x][y] = -1;
			moveableMap0[xFrom][yFrom] = 0;
			int maxMove = getMaxMove();
			for (int moveLen = 1; moveLen <= maxMove; moveLen++) {
				for (int x = 0; x < xLen; x++) {
					for (int y = 0; y < yLen; y++) {
						/* Already can move here */
						if (moveableMap0[x][y] >= 0)
							continue;
						/* Other unit in the way */
						if (map.at(x, y).hasUnit())
							continue;
						/* TODO, check surface */
						/* Check if we reached any near tiles last moveLen */
						boolean nearMoveable = false;
						for (int neighbor = 0; neighbor < Map.neighbors.length; neighbor++) {
							int x2 = x + Map.neighbors[neighbor][0];
							int y2 = y + Map.neighbors[neighbor][1];
							if (map.isInMap(x2, y2) && moveableMap0[x2][y2] != moveLen - 1) {
								nearMoveable = true;
								break;
							}
						}
						if (!nearMoveable)
							continue;
						moveableMap0[x][y] = moveLen;
					}
				}
			}
			/* Convert distance map to boolean map */
			for (int x = 0; x < xLen; x++)
				for (int y = 0; y < yLen; y++)
					moveableMap[x][y] = moveableMap0[x][y] > 0;

			return moveableMap;
		}

		@Override
		public boolean isMoveValid(int x, int y) {
			return calcMovableMap()[x][y];
		}

		@Override
		public boolean isAttackValid(final int xTo, final int yTo) {
			Map map = getMap();
			int xLen = map.getXLen(), yLen = map.getYLen();

			boolean[][] moveableMap = calcMovableMap();
			boolean[][] attackableMap = new boolean[xLen][yLen];

			/* Touchable map */
			for (int x = 0; x < xLen; x++) {
				for (int y = 0; y < yLen; y++) {
					boolean nearMoveable = false;
					for (int neighbor = 0; neighbor < Map.neighbors.length; neighbor++) {
						int x2 = x + Map.neighbors[neighbor][0];
						int y2 = y + Map.neighbors[neighbor][1];
						if (map.isInMap(x2, y2) && moveableMap[x2][y]) {
							nearMoveable = true;
							break;
						}
					}
					attackableMap[x][y] = nearMoveable;
				}
			}

			return attackableMap[xTo][yTo];
		}

	}

	static class Soldier extends CloseRangeUnitAbstract implements CloseRangeUnit {

		Soldier(Team team) {
			super(team);
		}

		@Override
		public int getMaxHealth() {
			return 50;
		}

		@Override
		public int getDamge() {
			return 22;
		}

		@Override
		public int getMaxMove() {
			return 3;
		}

		@Override
		public Unit deepCopy() {
			return new Soldier(getTeam());
		}

	}

	static class Tank extends CloseRangeUnitAbstract implements CloseRangeUnit {

		Tank(Team team) {
			super(team);
		}

		@Override
		public int getMaxHealth() {
			return 70;
		}

		@Override
		public int getDamge() {
			return 35;
		}

		@Override
		public int getMaxMove() {
			return 6;
		}

		@Override
		public Tank deepCopy() {
			return new Tank(getTeam());
		}

	}

}
