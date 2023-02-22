package com.ugav.battalion.core;

public enum Direction {

	XPos(1, 0), XNeg(-1, 0), YPos(0, 1), YNeg(0, -1);

	public final int dx, dy;

	Direction(int dr, int dc) {
		this.dx = dr;
		this.dy = dc;
	}

};