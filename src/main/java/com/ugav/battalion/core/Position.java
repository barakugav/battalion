package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import com.ugav.battalion.Utils;

public class Position implements Comparable<Position> {

	public final double x, y;

	public Position(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public static Position of(double x, double y) {
		return new Position(x, y);
	}

	public int xInt() {
		if (!Utils.isInteger(x))
			throw new IllegalStateException(toString());
		return (int) x;
	}

	public int yInt() {
		if (!Utils.isInteger(y))
			throw new IllegalStateException(toString());
		return (int) y;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	@Override
	public int hashCode() {
		return Double.hashCode(x) ^ Double.hashCode(y);
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

	public Position add(Direction dir) {
		return Position.of(x + dir.dx, y + dir.dy);
	}

	public List<Position> neighbors() {
		/* TODO return view instead of actually creating an array list each time */
		List<Position> neighbors = new ArrayList<>(Direction.values().length);
		for (Direction dir : Direction.values())
			neighbors.add(add(dir));
		return neighbors;
	}

	public boolean isInRect(double width, double height) {
		return isInRect(0, 0, width, height);
	}

	public boolean isInRect(double x1, double y1, double x2, double y2) {
		return x1 <= x && x <= x2 && y1 <= y && y <= y2;
	}

	public static enum Direction {

		XPos(1, 0), XNeg(-1, 0), YPos(0, 1), YNeg(0, -1);

		public final int dx, dy;

		Direction(int dr, int dc) {
			this.dx = dr;
			this.dy = dc;
		}

	};

	public static class Iterator2D implements Iterator<Position> {

		private final int width, height;
		private int x, y;

		public Iterator2D(int width, int height) {
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
			Position pos = Position.of(x, y);
			if (++y >= height) {
				y = 0;
				x++;
			}
			return pos;
		}

	}

	public static class Bitmap implements Predicate<Position>, Iterable<Position> {

		private final boolean[][] map;

		public static final Bitmap Empty = new Bitmap(new boolean[0][0]);

		public Bitmap(boolean[][] map) {
			this.map = map;
		}

		public static Bitmap fromPredicate(int width, int height, Predicate<Position> predicate) {
			boolean[][] map = new boolean[width][height];
			for (Position pos : Utils.iterable(new Iterator2D(width, height)))
				map[pos.xInt()][pos.yInt()] = predicate.test(pos);
			return new Bitmap(map);
		}

		public int width() {
			return map.length;
		}

		public int height() {
			return map.length != 0 ? map[0].length : 0;
		}

		public boolean contains(Position pos) {
			return 0 <= pos.x && pos.x < map.length && 0 <= pos.y && pos.y < map[pos.xInt()].length
					&& map[pos.xInt()][pos.yInt()];
		}

		@Override
		public boolean test(Position pos) {
			return contains(pos);
		}

		@Override
		public Iterator<Position> iterator() {
			if (width() == 0 || height() == 0)
				return Collections.emptyIterator();
			return Utils.iteratorIf(new Iterator2D(width(), height()), this);
		}

		@Override
		public Bitmap and(Predicate<? super Position> predicate) {
			return fromPredicate(width(), height(), p -> test(p) && predicate.test(p));
		}

	}

	@Override
	public int compareTo(Position o) {
		if (x != o.x)
			return Double.compare(x, o.x);
		if (y != o.y)
			return Double.compare(y, o.y);
		return 0;
	}

	public static <T extends Position> Comparator<T> comparator() {
		return comparator(false, false);
	}

	public static <T extends Position> Comparator<T> comparator(boolean xreverse, boolean yreverse) {
		int xmul = xreverse ? -1 : 1;
		int ymul = yreverse ? -1 : 1;
		return (p1, p2) -> {
			if (p1.x != p2.x)
				return Double.compare(p1.x, p2.x) * xmul;
			if (p1.y != p2.y)
				return Double.compare(p1.y, p2.y) * ymul;
			return 0;
		};
	}

}