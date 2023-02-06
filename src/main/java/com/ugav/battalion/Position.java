package com.ugav.battalion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

class Position implements Comparable<Position> {

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

	Position add(Direction dir) {
		return new Position(x + dir.dx, y + dir.dy);
	}

	List<Position> neighbors() {
		/* TODO return view instead of actually creating an array list each time */
		List<Position> neighbors = new ArrayList<>(Direction.values().length);
		for (Direction dir : Direction.values())
			neighbors.add(add(dir));
		return neighbors;
	}

	boolean isInRect(int x1, int y1, int x2, int y2) {
		return x1 <= x && x <= x2 && y1 <= y && y <= y2;
	}

	static enum Direction {

		XPos(1, 0), XNeg(-1, 0), YPos(0, 1), YNeg(0, -1);

		final int dx, dy;

		Direction(int dr, int dc) {
			this.dx = dr;
			this.dy = dc;
		}

	};

	static class Iterator2D implements Iterator<Position> {

		final int width, height;
		int x, y;

		Iterator2D(int width, int height) {
			if (width < 0 || height < 0)
				throw new IllegalArgumentException();
			this.width = width;
			this.height = height;
			x = y = 0;
		}

		@Override
		public boolean hasNext() {
			return x < width;
		}

		@Override
		public Position next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Position pos = new Position(x, y);
			if (++y >= height) {
				y = 0;
				x++;
			}
			return pos;
		}

	}

	static class Bitmap implements Predicate<Position>, Iterable<Position> {

		private final boolean[][] map;

		static final Bitmap empty = new Bitmap(new boolean[0][0]);

		Bitmap(boolean[][] map) {
			this.map = map;
		}

		boolean contains(Position pos) {
			return 0 <= pos.x && pos.x < map.length && 0 <= pos.y && pos.y < map[pos.x].length && map[pos.x][pos.y];
		}

		@Override
		public boolean test(Position pos) {
			return contains(pos);
		}

		@Override
		public Iterator<Position> iterator() {
			if (map.length == 0 || map[0].length == 0)
				return Collections.emptyIterator();
			return Utils.iteratorIf(new Iterator2D(map.length, map[0].length), this);
		}

	}

	@Override
	public int compareTo(Position o) {
		if (x != o.x)
			return Integer.compare(x, o.x);
		if (y != o.y)
			return Integer.compare(y, o.y);
		return 0;
	}

	static <T extends Position> Comparator<T> comparator() {
		return comparator(false, false);
	}

	static <T extends Position> Comparator<T> comparator(boolean xreverse, boolean yreverse) {
		int xmul = xreverse ? -1 : 1;
		int ymul = yreverse ? -1 : 1;
		return (p1, p2) -> {
			if (p1.x != p2.x)
				return Integer.compare(p1.x, p2.x) * xmul;
			if (p1.y != p2.y)
				return Integer.compare(p1.y, p2.y) * ymul;
			return 0;
		};
	}

}