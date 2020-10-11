package com.ugav.battalion;

public interface Unit extends Entity {

    int getMaxHealth();

    int getHealth();

    int getDamge();

    int getMaxMove();

    @Override
    Unit deepCopy();

    ExpolredMap getExploredMap();

    void setExploredMap(ExpolredMap expolredMap);

    void invalidateExploredMap();

    static ExpolredMap getExploredMap(Map map, int fromX, int fromY) {
	int xLen = map.getXLen(), yLen = map.getYLen();
	Tile from = map.at(fromX, fromY);
	if (!from.hasUnit())
	    return ExpolredMap.empty();
	Unit unit = from.getUnit();

	boolean[][] moveableMap = new boolean[xLen][yLen];
	boolean[][] touchableMap = new boolean[xLen][yLen];
	boolean[][] attackableMap = new boolean[xLen][yLen];

	/* Moveable map */
	int[][] moveableMap0 = new int[xLen][yLen];
	for (int x = 0; x < xLen; x++)
	    for (int y = 0; y < yLen; y++)
		moveableMap0[x][y] = -1;
	moveableMap0[fromX][fromY] = 0;
	int maxMove = unit.getMaxMove();
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
		touchableMap[x][y] = nearMoveable;
	    }
	}

	for (int x = 0; x < xLen; x++)
	    for (int y = 0; y < yLen; y++)
		attackableMap[x][y] = false;
	if (unit instanceof CloseRangeUnit) {
	    /* Close range attacks are the same as touchable */
	    for (int x = 0; x < xLen; x++)
		for (int y = 0; y < yLen; y++)
		    attackableMap[x][y] = touchableMap[x][y];
	} else if (unit instanceof LongRangeUnit) {
	    /* Compute abs distance */
	    LongRangeUnit longRangeUnit = (LongRangeUnit) unit;
	    int minAttack = longRangeUnit.getMinRange();
	    int maxAttack = longRangeUnit.getMaxRange();
	    for (int x = 0; x < xLen; x++) {
		for (int y = 0; y < yLen; y++) {
		    int xDis = Math.abs(x - fromX);
		    int yDis = Math.abs(y - fromY);
		    int distance = xDis + yDis;
		    attackableMap[x][y] = minAttack <= distance && distance <= maxAttack;
		}
	    }
	} else {
	    throw new InternalError();
	}

	return new ExpolredMap(moveableMap, touchableMap, attackableMap);
    }

    public static class Soldier extends AbstractUnit implements CloseRangeUnit {

	public Soldier(Team team) {
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

    public static class Tank extends AbstractUnit implements CloseRangeUnit {

	public Tank(Team team) {
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
