package com.ugav.battalion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class Position {

	final int row, col;

	Position(int row, int col) {
		this.row = row;
		this.col = col;
	}

	@Override
	public String toString() {
		return "(" + row + ", " + col + ")";
	}

	@Override
	public int hashCode() {
		return row << 16 + col;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof Position))
			return false;
		Position o = (Position) other;
		return row == o.row && col == o.col;
	}

	static enum Direction {

		RowPos(1, 0), RowNeg(-1, 0), ColPos(0, 1), ColNeg(0, -1);

		final int dr, dc;

		Direction(int dr, int dc) {
			this.dr = dr;
			this.dc = dc;
		}

	};

	List<Position> neighbors() {
		List<Position> neighbors = new ArrayList<>(Direction.values().length);
		for (Direction dir : Direction.values())
			neighbors.add(new Position(row + dir.dr, col + dir.dc));
		return neighbors;
	}

	static class Iterator2D implements Iterator<Position> {

		final int rows, cols;
		int r, c;

		Iterator2D(int rows, int cols) {
			if (rows < 0 || cols < 0)
				throw new IllegalArgumentException();
			this.rows = rows;
			this.cols = cols;
			r = c = 0;
		}

		@Override
		public boolean hasNext() {
			return r < rows;
		}

		@Override
		public Position next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Position pos = new Position(r, c);
			if (++c >= cols) {
				c = 0;
				r++;
			}
			return pos;
		}

	}

}