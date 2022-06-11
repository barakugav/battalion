package com.ugav.battalion;

import java.util.Arrays;

import com.ugav.battalion.Map.Neighbor;
import com.ugav.battalion.Map.Position;

abstract class Unit extends EntityAbstract {

	final Type type;
	private int health;
	private int x, y;
	private Map map;

	Unit(Type type, Team team) {
		super(team);
		if (getTeam() == Team.None)
			throw new IllegalArgumentException();
		this.type = type;
		health = type.health;
	}

	@Override
	public final void setTeam(Team team) {
		throw new UnsupportedOperationException();
	}

	int getHealth() {
		return health;
	}

	void setPos(int x, int y) {
		this.x = x;
		this.y = y;
	}

	int getX() {
		return x;
	}

	int getY() {
		return y;
	}

	void setMap(Map map) {
		this.map = map;
	}

	Map getMap() {
		return map;
	}

	abstract int getDamge();

	boolean isMoveValid(int x, int y) {
		return getMovableMap()[x][y];
	}

	abstract boolean isAttackValid(int x, int y);

	enum Category {
		Land, Water, Air
	};

	enum Type {
		Soldier(Category.Land, 50, 22, 3, 1), Tank(Category.Land, 70, 35, 6, 1);

		final Category category;
		final int health;
		final int damage;
		final int moveLimit;
		final int range;

		Type(Category category, int health, int damage, int moveLimit, int range) {
			this.category = category;
			this.health = health;
			this.damage = damage;
			this.moveLimit = moveLimit;
			this.range = range;
		}
	}

	static abstract class CloseRangeUnitAbstract extends Unit {
		CloseRangeUnitAbstract(Type type, Team team) {
			super(type, team);
		}

		@Override
		public boolean isAttackValid(final int xTo, final int yTo) {
			Map map = getMap();
			int xLen = map.getXLen(), yLen = map.getYLen();
			boolean[][] moveableMap = getMovableMap();
			boolean[][] attackableMap = new boolean[xLen][yLen];

			/* Touchable map */
			for (int x = 0; x < xLen; x++) {
				for (int y = 0; y < yLen; y++) {
					for (Position neighbor : Neighbor.of(x, y)) {
						if (map.isInMap(neighbor) && moveableMap[neighbor.x][neighbor.y]) {
							attackableMap[x][y] = true;
							break;
						}
					}
				}
			}

			return attackableMap[xTo][yTo];
		}

	}

	static class Soldier extends CloseRangeUnitAbstract {

		Soldier(Team team) {
			super(Type.Soldier, team);
		}

		@Override
		public int getDamge() {
			return type.damage;
		}

	}

	static class Tank extends CloseRangeUnitAbstract {

		Tank(Team team) {
			super(Type.Tank, team);
		}

		@Override
		public int getDamge() {
			return type.damage;
		}

	}

	boolean[][] getMovableMap() {
		final int xFrom = getX(), yFrom = getY();
		Map map = getMap();
		int xLen = map.getXLen(), yLen = map.getYLen();
		boolean[][] moveableMap = new boolean[xLen][yLen];

		int[][] moveableMap0 = new int[xLen][yLen];
		for (int x = 0; x < xLen; x++)
			Arrays.fill(moveableMap0[x], -1);
		moveableMap0[xFrom][yFrom] = 0;

		int maxMove = type.moveLimit;
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
					for (Position neighbor : Neighbor.of(x, y)) {
						if (map.isInMap(neighbor) && moveableMap0[neighbor.x][neighbor.y] != moveLen - 1) {
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

}
