package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
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

	public double dist(Position other) {
		return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
	}

	public double distNorm1(Position other) {
		return Math.abs(x - other.x) + Math.abs(y - other.y);
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

		public static Direction calc(Position source, Position dest) {
			for (Direction dir : Direction.values())
				if (dest.equals(source.add(dir)))
					return dir;
			throw new IllegalArgumentException();
		}

		public double dist(Position source, Position dest) {
			switch (this) {
			case XPos:
				return dest.x - source.x;
			case XNeg:
				return -(dest.x - source.x);
			case YPos:
				return dest.y - source.y;
			case YNeg:
				return -(dest.y - source.y);
			default:
				throw new IllegalStateException();
			}
		}

	};

	public static class Iterator2D implements Iterator<Position> {

		private final int width, height;
		private int x, y;
		private final boolean yfirst; // TODO needed?

		public Iterator2D(int width, int height) {
			this(width, height, true);
		}

		public Iterator2D(int width, int height, boolean yfirst) {
			if (width < 0 || height < 0)
				throw new IllegalArgumentException();
			this.width = width;
			this.height = height;
			x = y = 0;
			this.yfirst = yfirst;
		}

		public static Iterator2D of(int width, int height) {
			return new Iterator2D(width, height);
		}

		public static Iterator2D xFirst(int width, int height) {
			return new Iterator2D(width, height, false);
		}

		@Override
		public boolean hasNext() {
			return x < width && y < height;
		}

		@Override
		public Position next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Position pos = Position.of(x, y);
			if (yfirst) {
				if (++y >= height) {
					y = 0;
					x++;
				}
			} else {
				if (++x >= height) {
					x = 0;
					y++;
				}
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

		public void set(Position pos, boolean val) {
			map[pos.xInt()][pos.yInt()] = val;
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

		public Bitmap not() {
			return fromPredicate(width(), height(), p -> !test(p));
		}

		@Override
		public Bitmap and(Predicate<? super Position> predicate) {
			return fromPredicate(width(), height(), p -> test(p) && predicate.test(p));
		}

		@Override
		public Bitmap or(Predicate<? super Position> predicate) {
			return fromPredicate(width(), height(), p -> test(p) || predicate.test(p));
		}

		public Bitmap xor(Predicate<? super Position> predicate) {
			return fromPredicate(width(), height(), p -> test(p) ^ predicate.test(p));
		}

		public static Bitmap ofTrue(int width, int height) {
			return fromPredicate(width, height, p -> true);
		}

		public static Bitmap ofFalse(int width, int height) {
			return fromPredicate(width, height, p -> false);
		}

	}

	public static class Array<T> implements Function<Position, T> {

		private final Object[][] arr;

		public Array(int width, int height) {
			arr = new Object[width][height];
		}

		public static <T> Array<T> of(int width, int height) {
			return new Array<>(width, height);
		}

		public static <T> Array<T> fromFunc(int width, int height, Function<Position, T> func) {
			Array<T> arr = new Array<>(width, height);
			for (Position pos : Utils.iterable(new Position.Iterator2D(width, height)))
				arr.set(pos, func.apply(pos));
			return arr;
		}

		public int width() {
			return arr.length;
		}

		public int height() {
			return arr.length != 0 ? arr[0].length : 0;
		}

		public T at(Position p) {
			return at(p.xInt(), p.yInt());
		}

		@SuppressWarnings("unchecked")
		public T at(int x, int y) {
			return (T) arr[x][y];
		}

		@Override
		public T apply(Position p) {
			return at(p);
		}

		public void set(Position p, T val) {
			arr[p.xInt()][p.yInt()] = val;
		}

		public Object[][] toArray() {
			Object[][] arr = new Object[width()][height()];
			for (Position pos : Utils.iterable(new Position.Iterator2D(width(), height())))
				arr[pos.xInt()][pos.yInt()] = at(pos);
			return arr;
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