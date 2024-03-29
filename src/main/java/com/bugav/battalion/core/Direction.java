package com.bugav.battalion.core;

import java.util.EnumSet;
import java.util.Set;

public enum Direction {

	XPos(1, 0), YPos(0, 1), XNeg(-1, 0), YNeg(0, -1);

	public final int dx, dy;

	Direction(int dr, int dc) {
		this.dx = dr;
		this.dy = dc;
	}

	public Direction opposite() {
		switch (this) {
		case XPos:
			return XNeg;
		case XNeg:
			return XPos;
		case YPos:
			return YNeg;
		case YNeg:
			return YPos;
		default:
			throw new IllegalArgumentException("Unexpected value: " + this);
		}
	}

	public static Set<Direction> xDirs() {
		return EnumSet.of(XPos, XNeg);
	}

	public static Set<Direction> yDirs() {
		return EnumSet.of(YPos, YNeg);
	}

	public Set<Direction> orthogonal() {
		return isXDir() ? yDirs() : xDirs();
	}

	public boolean isXDir() {
		switch (this) {
		case XPos:
		case XNeg:
			return true;
		case YPos:
		case YNeg:
			return false;
		default:
			throw new IllegalArgumentException("Unexpected value: " + this);
		}
	}

};