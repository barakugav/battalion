package com.ugav.battalion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class Position {

	final int x, y;

	Position(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	@Override
	public int hashCode() {
		return x << 16 + y;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof Position))
			return false;
		Position o = (Position) other;
		return x == o.x && y == o.y;
	}

	static enum Direction {

		XPos(1, 0), XNeg(-1, 0), YPos(0, 1), YNeg(0, -1);

		final int dx, dy;

		Direction(int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
		}

	};

	List<Position> neighbors() {
		List<Position> neighbors = new ArrayList<>(Direction.values().length);
		for (Direction dir : Direction.values())
			neighbors.add(new Position(x + dir.dx, y + dir.dy));
		return neighbors;
	}

	static class Iterator2D implements Iterator<Position> {

		final int xLen, yLen;
		int x, y;

		Iterator2D(int xLen, int yLen) {
			if (xLen < 0 || yLen < 0)
				throw new IllegalArgumentException();
			this.xLen = xLen;
			this.yLen = yLen;
			x = y = 0;
		}

		@Override
		public boolean hasNext() {
			return x < xLen;
		}

		@Override
		public Position next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Position pos = new Position(x, y);
			if (++y >= yLen) {
				y = 0;
				x++;
			}
			return pos;
		}

	}

}