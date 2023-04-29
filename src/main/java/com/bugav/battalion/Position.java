package com.bugav.battalion;

import java.util.Comparator;

import com.bugav.battalion.core.Cell;
import com.bugav.battalion.core.Direction;
import com.bugav.battalion.util.Utils;

class Position implements Comparable<Position> {

	public final double x, y;

	public Position(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public static Position of(double x, double y) {
		return new Position(x, y);
	}

	public static Position fromCell(int cell) {
		return new Position(Cell.x(cell), Cell.y(cell));
	}

	public int toCell() {
		return Cell.of(xInt(), yInt());
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

	public static double dist(Position source, Position dest, Direction relativeTo) {
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

	public boolean isInRect(double width, double height) {
		return isInRect(0, 0, width, height);
	}

	public boolean isInRect(double x1, double y1, double x2, double y2) {
		return x1 <= x && x <= x2 && y1 <= y && y <= y2;
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