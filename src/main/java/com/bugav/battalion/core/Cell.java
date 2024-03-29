package com.bugav.battalion.core;

import java.util.BitSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import com.bugav.battalion.util.Iter;
import com.bugav.battalion.util.ListInt;

public class Cell {

	private Cell() {
	}

	public static int of(int x, int y) {
		return ((x & 0xffff) << 16) + (y & 0xffff);
	}

	public static short x(int cell) {
		return (short) (cell >> 16);
	}

	public static short y(int cell) {
		return (short) (cell & 0xffff);
	}

	public static String toString(int cell) {
		return "(" + x(cell) + ", " + y(cell) + ")";
	}

	public static int add(int cell, Direction dir) {
		return of(x(cell) + dir.dx, y(cell) + dir.dy);
	}

	public static int add(int cell, Direction dir, Direction... dirs) {
		int x = x(cell) + dir.dx, y = y(cell) + dir.dy;
		for (Direction d : dirs) {
			x += d.dx;
			y += d.dy;
		}
		return of(x, y);
	}

	public static double dist(int cell1, int cell2) {
		int x1 = x(cell1), x2 = x(cell2);
		int y1 = y(cell1), y2 = y(cell2);
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	public static int distNorm1(int cell1, int cell2) {
		return Math.abs(x(cell1) - x(cell2)) + Math.abs(y(cell1) - y(cell2));
	}

	public static int dist(int cell1, int cell2, Direction relativeTo) {
		switch (relativeTo) {
		case XPos:
			return x(cell2) - x(cell1);
		case XNeg:
			return -(x(cell2) - x(cell1));
		case YPos:
			return y(cell2) - y(cell1);
		case YNeg:
			return -(y(cell2) - y(cell1));
		default:
			throw new IllegalStateException();
		}
	}

	public static Direction diffDir(int source, int dest) {
		for (Direction dir : Direction.values())
			if (dest == add(source, dir))
				return dir;
		throw new IllegalArgumentException();
	}

	public static Iter.Int neighbors(int cell) {
		return new Iter.Int() {

			int idx;

			@Override
			public boolean hasNext() {
				return idx < Direction.values().length;
			}

			@Override
			public int next() {
				if (!hasNext())
					throw new NoSuchElementException();
				Direction dir = Direction.values()[idx++];
				return of(Cell.x(cell) + dir.dx, Cell.y(cell) + dir.dy);
			}

		};
	}

	public static boolean areNeighbors(int cell1, int cell2) {
		return distNorm1(cell1, cell2) == 1;
	}

	public static boolean isInRect(int cell, int width, int height) {
		return isInRect(cell, 0, 0, width, height);
	}

	public static boolean isInRect(int cell, double x1, double y1, double x2, double y2) {
		return isInRect(x(cell), y(cell), x1, y1, x2, y2);
	}

	public static boolean isInRect(int x, double y, double x1, double y1, double x2, double y2) {
		return x1 <= x && x <= x2 && y1 <= y && y <= y2;
	}

	public static class Iter2D implements Iter.Int {

		private final short xBegin, xEnd, yEnd;
		private short x, y;

		private Iter2D(int xBegin, int xEnd, int yBegin, int yEnd) {
			if (xBegin > xEnd || yBegin > yEnd)
				throw new IllegalArgumentException();
			this.xBegin = (short) xBegin;
			this.xEnd = (short) xEnd;
			this.yEnd = (short) yEnd;
			x = (short) xBegin;
			y = (short) yBegin;
		}

		public static Iter2D of(int width, int height) {
			return new Iter2D(0, width, 0, height);
		}

		public static Iter2D of(int xBegin, int xEnd, int yBegin, int yEnd) {
			return new Iter2D(xBegin, xEnd, yBegin, yEnd);
		}

		@Override
		public boolean hasNext() {
			return x < xEnd && y < yEnd;
		}

		@Override
		public int next() {
			if (!hasNext())
				throw new NoSuchElementException();
			int cell = Cell.of(x, y);
			if (++x >= xEnd) {
				x = xBegin;
				y++;
			}
			return cell;
		}

	}

	public static class Bitmap implements IntPredicate {

		private final BitSet map;
		private final int width, height;

		public static Bitmap empty() {
			return new Bitmap(0, 0);
		}

		private Bitmap(int width, int height) {
			this.width = width;
			this.height = height;
			this.map = new BitSet(width * height);
		}

		public static Bitmap fromPredicate(int width, int height, IntPredicate predicate) {
			Bitmap map = new Bitmap(width, height);
			for (Iter.Int it = Iter2D.of(width, height); it.hasNext();) {
				int cell = it.next();
				map.set(cell, predicate.test(cell));
			}
			return map;
		}

		public int width() {
			return width;
		}

		public int height() {
			return height;
		}

		public boolean contains(int cell) {
			return contains(x(cell), y(cell));
		}

		public boolean contains(int x, int y) {
			return isInRange(x, y) && map.get(indexOf(x, y));
		}

		public void set(int cell, boolean val) {
			set(x(cell), y(cell), val);
		}

		public void set(int x, int y, boolean val) {
			if (!isInRange(x, y))
				throw new IndexOutOfBoundsException();
			map.set(indexOf(x, y), val);
		}

		public Iter.Int cells() {
			return Iter2D.of(width, height).filter(this::contains);
		}

		public Bitmap not() {
			return fromPredicate(width, height, cell -> !contains(cell));
		}

		@Override
		public Bitmap and(IntPredicate predicate) {
			return fromPredicate(width, height, cell -> contains(cell) && predicate.test(cell));
		}

		@Override
		public Bitmap or(IntPredicate predicate) {
			return fromPredicate(width, height, cell -> contains(cell) || predicate.test(cell));
		}

		public Bitmap xor(IntPredicate predicate) {
			return fromPredicate(width, height, cell -> contains(cell) ^ predicate.test(cell));
		}

		public static Bitmap ofTrue(int width, int height) {
			return fromPredicate(width, height, p -> true);
		}

		public static Bitmap ofFalse(int width, int height) {
			return fromPredicate(width, height, p -> false);
		}

		private int indexOf(int x, int y) {
			return x * height + y;
		}

		private boolean isInRange(int x, int y) {
			return 0 <= x && x < width && 0 <= y && y < height;
		}

		@Override
		public boolean test(int cell) {
			return contains(cell);
		}

	}

	public static class Array<T> {

		private final Object[][] arr;

		public Array(int width, int height) {
			arr = new Object[width][height];
		}

		public static <T> Array<T> of(int width, int height) {
			return new Array<>(width, height);
		}

		public static <T> Array<T> fromFunc(int width, int height, IntFunction<T> func) {
			Array<T> arr = new Array<>(width, height);
			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++)
					arr.set(x, y, func.apply(Cell.of(x, y)));
			return arr;
		}

		public static <T> Array<T> copyOf(Array<T> a) {
			Array<T> arr = new Array<>(a.width(), a.height());
			for (int x = 0; x < a.width(); x++)
				for (int y = 0; y < a.height(); y++)
					arr.set(x, y, a.at(x, y));
			return arr;
		}

		public int width() {
			return arr.length;
		}

		public int height() {
			return arr.length != 0 ? arr[0].length : 0;
		}

		public T at(int cell) {
			return at(x(cell), y(cell));
		}

		@SuppressWarnings("unchecked")
		public T at(int x, int y) {
			return (T) arr[x][y];
		}

		public void set(int cell, T val) {
			set(x(cell), y(cell), val);
		}

		public void set(int x, int y, T val) {
			arr[x][y] = val;
		}

		public Object[][] toArray() {
			final int w = width(), h = height();
			Object[][] arr = new Object[w][h];
			for (int x = 0; x < w; x++)
				for (int y = 0; y < h; y++)
					arr[x][y] = at(x, y);
			return arr;
		}

		@Override
		public boolean equals(Object other) {
			if (other == this)
				return true;
			if (!(other instanceof Array<?>))
				return false;
			Array<?> o = (Array<?>) other;

			if (width() != o.width() || height() != o.height())
				return false;
			for (int x = 0; x < width(); x++)
				for (int y = 0; y < height(); y++)
					if (!Objects.equals(at(x, y), o.at(x, y)))
						return false;
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 1;
			for (int x = 0; x < width(); x++)
				for (int y = 0; y < height(); y++)
					hash = 31 * hash + Objects.hashCode(at(x, y));
			return hash;
		}

	}

	public static int compare(int cell1, int cell2) {
		int c;
		short x1 = x(cell1), x2 = x(cell2);
		if ((c = Short.compare(x1, x2)) != 0)
			return c;
		short y1 = y(cell1), y2 = y(cell2);
		if ((c = Short.compare(y1, y2)) != 0)
			return c;
		return 0;
	}

	public static String toString(ListInt cells) {
		int iMax = cells.size() - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		int i = 0;
		for (Iter.Int it = cells.iterator(); it.hasNext(); i++) {
			b.append(toString(it.next()));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
		return b.toString();
	}

}
