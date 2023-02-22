package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ugav.battalion.util.Iter;

public class Cell implements Comparable<Cell> {

	public final short x;
	public final short y;

	private Cell(int x, int y) {
		this.x = (short) x;
		this.y = (short) y;
	}

	public static Cell of(int x, int y) {
		return new Cell(x, y);
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	@Override
	public int hashCode() {
		return (x << 16) + y;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof Cell))
			return false;
		Cell o = (Cell) other;
		return x == o.x && y == o.y;
	}

	public Cell add(Direction dir) {
		return Cell.of(x + dir.dx, y + dir.dy);
	}

	public double dist(Cell other) {
		int x1 = x, x2 = other.x;
		int y1 = y, y2 = other.y;
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	public int distNorm1(Cell other) {
		return Math.abs(x - other.x) + Math.abs(y - other.y);
	}

	public static int dist(Cell source, Cell dest, Direction relativeTo) {
		switch (relativeTo) {
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

	public static Direction diffDir(Cell source, Cell dest) {
		for (Direction dir : Direction.values())
			if (dest.equals(source.add(dir)))
				return dir;
		throw new IllegalArgumentException();
	}

	public List<Cell> neighbors() {
		/* TODO return view instead of actually creating an array list each time */
		List<Cell> neighbors = new ArrayList<>(Direction.values().length);
		for (Direction dir : Direction.values())
			neighbors.add(add(dir));
		return neighbors;
	}

	public boolean isInRect(int width, int height) {
		return isInRect(0, 0, width, height);
	}

	public boolean isInRect(int x1, int y1, int x2, int y2) {
		return x1 <= x && x <= x2 && y1 <= y && y <= y2;
	}

	public static class Iter2D implements Iter<Cell> {

		private final int width, height;
		private int x, y;

		public Iter2D(int width, int height) {
			if (width < 0 || height < 0)
				throw new IllegalArgumentException();
			this.width = width;
			this.height = height;
			x = y = 0;
		}

		public static Iter2D of(int width, int height) {
			return new Iter2D(width, height);
		}

		@Override
		public boolean hasNext() {
			return x < width && y < height;
		}

		@Override
		public Cell next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Cell pos = Cell.of(x, y);
			if (++x >= width) {
				x = 0;
				y++;
			}
			return pos;
		}

	}

	public static class Bitmap implements Predicate<Cell>, Iterable<Cell> {

		private final boolean[][] map;

		public static final Bitmap Empty = new Bitmap(new boolean[0][0]);

		public Bitmap(boolean[][] map) {
			this.map = map;
		}

		public static Bitmap fromPredicate(int width, int height, Predicate<Cell> predicate) {
			boolean[][] map = new boolean[width][height];
			for (Cell pos : Iter2D.of(width, height).forEach())
				map[pos.x][pos.y] = predicate.test(pos);
			return new Bitmap(map);
		}

		public int width() {
			return map.length;
		}

		public int height() {
			return map.length != 0 ? map[0].length : 0;
		}

		public boolean contains(Cell pos) {
			return 0 <= pos.x && pos.x < width() && 0 <= pos.y && pos.y < height() && map[pos.x][pos.y];
		}

		public void set(Cell pos, boolean val) {
			map[pos.x][pos.y] = val;
		}

		@Override
		public boolean test(Cell pos) {
			return contains(pos);
		}

		@Override
		public Iterator<Cell> iterator() {
			return new Iter2D(width(), height()).filter(this);
		}

		public Bitmap not() {
			return fromPredicate(width(), height(), p -> !test(p));
		}

		@Override
		public Bitmap and(Predicate<? super Cell> predicate) {
			return fromPredicate(width(), height(), p -> test(p) && predicate.test(p));
		}

		@Override
		public Bitmap or(Predicate<? super Cell> predicate) {
			return fromPredicate(width(), height(), p -> test(p) || predicate.test(p));
		}

		public Bitmap xor(Predicate<? super Cell> predicate) {
			return fromPredicate(width(), height(), p -> test(p) ^ predicate.test(p));
		}

		public static Bitmap ofTrue(int width, int height) {
			return fromPredicate(width, height, p -> true);
		}

		public static Bitmap ofFalse(int width, int height) {
			return fromPredicate(width, height, p -> false);
		}

	}

	public static class Array<T> implements Function<Cell, T> {

		private final Object[][] arr;

		public Array(int width, int height) {
			arr = new Object[width][height];
		}

		public static <T> Array<T> of(int width, int height) {
			return new Array<>(width, height);
		}

		public static <T> Array<T> fromFunc(int width, int height, Function<Cell, T> func) {
			Array<T> arr = new Array<>(width, height);
			for (Cell pos : Iter2D.of(width, height).forEach())
				arr.set(pos, func.apply(pos));
			return arr;
		}

		public int width() {
			return arr.length;
		}

		public int height() {
			return arr.length != 0 ? arr[0].length : 0;
		}

		public T at(Cell p) {
			return at(p.x, p.y);
		}

		@SuppressWarnings("unchecked")
		public T at(int x, int y) {
			return (T) arr[x][y];
		}

		@Override
		public T apply(Cell p) {
			return at(p);
		}

		public void set(Cell p, T val) {
			arr[p.x][p.y] = val;
		}

		public Object[][] toArray() {
			Object[][] arr = new Object[width()][height()];
			for (Cell pos : Iter2D.of(width(), height()).forEach())
				arr[pos.x][pos.y] = at(pos);
			return arr;
		}

	}

	@Override
	public int compareTo(Cell o) {
		int c;
		if ((c = Short.compare(x, o.x)) != 0)
			return c;
		if ((c = Short.compare(y, o.y)) != 0)
			return c;
		return 0;
	}

}
