package com.ugav.battalion;

import java.util.Objects;

public class ExpolredMap {

    private final boolean[][] moveableMap;
    private final boolean[][] touchableMap;
    private final boolean[][] attackableMap;

    ExpolredMap(boolean[][] moveableMap, boolean[][] touchableMap, boolean[][] attackableMap) {
	this.moveableMap = Objects.requireNonNull(moveableMap);
	this.touchableMap = Objects.requireNonNull(touchableMap);
	this.attackableMap = Objects.requireNonNull(attackableMap);
    }

    boolean canMove(int x, int y) {
	return moveableMap[x][y];
    }

    boolean canTouch(int x, int y) {
	return touchableMap[x][y];
    }

    boolean canAttack(int x, int y) {
	return attackableMap[x][y];
    }

    static ExpolredMap empty() {
	return EmptyExpolredMap.INSTANCE;
    }

    private static class EmptyExpolredMap extends ExpolredMap {

	private static final EmptyExpolredMap INSTANCE = new EmptyExpolredMap();

	private static final boolean[][] EMPTY_2D_BOOLEAN_ARR = new boolean[0][];

	EmptyExpolredMap() {
	    super(EMPTY_2D_BOOLEAN_ARR, EMPTY_2D_BOOLEAN_ARR, EMPTY_2D_BOOLEAN_ARR);
	}

	boolean canMove(int x, int y) {
	    return false;
	}

	boolean canTouch(int x, int y) {
	    return false;
	}

	boolean canAttack(int x, int y) {
	    return false;
	}

    }

}
