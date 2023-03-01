package com.ugav.battalion.core;

import java.util.EnumSet;
import java.util.Set;

public enum Direction {

	XPos(1, 0), XNeg(-1, 0), YPos(0, 1), YNeg(0, -1);

	public final int dx, dy;

	Direction(int dr, int dc) {
		this.dx = dr;
		this.dy = dc;
	}

	Direction opposite() {
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

	Set<Direction> orthogonal() {
		switch (this) {
		case XPos:
		case XNeg:
			return EnumSet.of(YPos, YNeg);
		case YPos:
		case YNeg:
			return EnumSet.of(XPos, XNeg);
		default:
			throw new IllegalArgumentException("Unexpected value: " + this);
		}
	}

};