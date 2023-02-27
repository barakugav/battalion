package com.ugav.battalion.util;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

public interface ListInt {

	public int size();

	default boolean isEmpty() {
		return size() == 0;
	}

	default boolean contains(int x) {
		return indexOf(x) >= 0;
	}

	default int[] toArray() {
		int[] a = new int[size()];
		int idx = 0;
		for (Iter.Int it = iterator(); it.hasNext();)
			a[idx++] = it.next();
		return a;
	}

	public boolean add(int e);

	public boolean addAll(ListInt l);

	public boolean remove(int x);

	public void clear();

	public int get(int index);

	default int last() {
		return get(size() - 1);
	}

	public int set(int index, int x);

	public void removeIndex(int index);

	default int indexOf(int x) {
		int idx = 0;
		for (Iter.Int it = iterator(); it.hasNext(); idx++)
			if (x == it.next())
				return idx;
		return -1;
	}

	default void reverse() {
		for (int i = 0, mid = size() >> 1, j = size() - 1; i < mid; i++, j--)
			set(i, set(j, get(i)));
	}

	default Iter.Int iterator() {
		return iterator(0);
	}

	public Iter.Int iterator(int beginIdx);

	default ListInt subList(int from, int to) {
		if (from < 0 || to > size() || from > to)
			throw new IllegalArgumentException();
		return new Sub(this, from, to);
	}

	default ListInt unmodifiableView() {
		return Unmodifiable.of(this);
	}

	default ListInt copy() {
		return new ListInt.Array(this);
	}

	abstract static class Abstract implements ListInt {

		@Override
		public boolean equals(Object other) {
			if (other == this)
				return true;
			if (!(other instanceof ListInt))
				return false;
			ListInt o = (ListInt) other;
			if (size() != o.size())
				return false;
			for (Iter.Int it1 = iterator(), it2 = o.iterator(); it1.hasNext();)
				if (it1.next() != it2.next())
					return false;
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 1;
			for (Iter.Int it = iterator(); it.hasNext();)
				hash = 31 * hash + it.next();
			return hash;
		}

		@Override
		public String toString() {
			int iMax = size() - 1;
			if (iMax == -1)
				return "[]";

			StringBuilder b = new StringBuilder();
			b.append('[');
			int i = 0;
			for (Iter.Int it = iterator(); it.hasNext(); i++) {
				b.append(it.next());
				if (i == iMax)
					return b.append(']').toString();
				b.append(", ");
			}
			return b.toString();
		}

	}

	public static class Array extends Abstract {

		private int[] data;
		private int size;

		public Array() {
			this(16);
		}

		public Array(int capacity) {
			data = new int[capacity];
		}

		public Array(ListInt l) {
			data = new int[l.size()];
			addAll(l);
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public int[] toArray() {
			return Arrays.copyOf(data, size);
		}

		@Override
		public boolean add(int e) {
			reserve(1);
			data[size++] = e;
			return true;
		}

		@Override
		public boolean addAll(ListInt l) {
			if (l.isEmpty())
				return false;
			reserve(l.size());
			if (l instanceof Array) {
				Array la = (Array) l;
				System.arraycopy(la.data, 0, data, size, la.size);
				size += la.size;
			} else {
				for (Iter.Int it = l.iterator(); it.hasNext();)
					add(it.next());
			}
			return true;
		}

		private void reserve(int amount) {
			if (size + amount > data.length) {
				int newCapacity = Math.max(4, Math.max(data.length * 2, size + amount));
				data = Arrays.copyOf(data, newCapacity);
			}
		}

		@Override
		public boolean remove(int x) {
			int idx = indexOf(x);
			if (idx < 0)
				return false;
			removeIndex(idx);
			return true;
		}

		@Override
		public void clear() {
			size = 0;
		}

		@Override
		public int get(int index) {
			if (index < 0 || index >= size)
				throw new IndexOutOfBoundsException(index);
			return data[index];
		}

		@Override
		public int set(int index, int x) {
			if (index < 0 || index >= size)
				throw new IndexOutOfBoundsException(index);
			int oldVal = data[index];
			data[index] = x;
			return oldVal;
		}

		@Override
		public void removeIndex(int index) {
			if (index < 0 || index >= size)
				throw new IndexOutOfBoundsException(index);
			size--;
			for (; index < size; index++)
				data[index] = data[index + 1];
		}

		@Override
		public int indexOf(int x) {
			for (int i = 0; i < size; i++)
				if (data[i] == x)
					return i;
			return -1;
		}

		@Override
		public Iter.Int iterator(int beginIdx) {
			if (beginIdx < 0 || beginIdx > size)
				throw new IndexOutOfBoundsException(beginIdx);
			return new Iter.Int() {

				int idx = beginIdx;

				@Override
				public boolean hasNext() {
					return idx < size;
				}

				@Override
				public int next() {
					if (!hasNext())
						throw new NoSuchElementException();
					return data[idx++];
				}

			};
		}

	}

	public static class Unmodifiable extends Abstract {

		private final ListInt l;

		private Unmodifiable(ListInt l) {
			this.l = Objects.requireNonNull(l);
		}

		public static Unmodifiable of(ListInt l) {
			if (l instanceof Unmodifiable)
				return (Unmodifiable) l;
			else
				return new Unmodifiable(l);
		}

		@Override
		public int size() {
			return l.size();
		}

		@Override
		public int[] toArray() {
			return l.toArray();
		}

		@Override
		public boolean add(int e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(ListInt l) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(int x) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int get(int index) {
			return l.get(index);
		}

		@Override
		public int set(int index, int x) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeIndex(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int indexOf(int x) {
			return l.indexOf(x);
		}

		@Override
		public void reverse() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iter.Int iterator(int beginIdx) {
			return l.iterator(beginIdx);
		}

	}

	static class Sub extends Abstract {

		private final ListInt l;
		private final int from, to;

		Sub(ListInt l, int from, int to) {
			if (from < 0 || to > l.size() || from > to)
				throw new IllegalArgumentException();
			this.l = l;
			this.from = from;
			this.to = to;
		}

		@Override
		public int size() {
			return to - from;
		}

		@Override
		public boolean add(int e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(ListInt l) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(int x) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int get(int index) {
			if (index < 0 || index >= size())
				throw new IndexOutOfBoundsException(index);
			return l.get(from + index);
		}

		@Override
		public int set(int index, int x) {
			if (index < 0 || index >= size())
				throw new IndexOutOfBoundsException(index);
			return l.set(from + index, x);
		}

		@Override
		public void removeIndex(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iter.Int iterator(int beginIdx) {
			if (beginIdx < 0 || beginIdx > size())
				throw new IndexOutOfBoundsException(beginIdx);
			return new Iter.Int() {

				Iter.Int it = l.iterator(from + beginIdx);
				int remaining = size() - beginIdx;

				@Override
				public int next() {
					if (!hasNext())
						throw new NoSuchElementException();
					remaining--;
					return it.next();
				}

				@Override
				public boolean hasNext() {
					return remaining > 0;
				}
			};
		}

		@Override
		public ListInt subList(int from, int to) {
			if (from < 0 || to > size() || from > to)
				throw new IllegalArgumentException();
			return new Sub(l, this.from + from, this.from + to);
		}

	}

	public static ListInt of(int... data) {
		return new ListInt.Abstract() {

			@Override
			public int size() {
				return data.length;
			}

			@Override
			public int set(int index, int x) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void removeIndex(int index) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean remove(int x) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Iter.Int iterator(int beginIdx) {
				if (beginIdx < 0 || beginIdx > size())
					throw new IndexOutOfBoundsException(beginIdx);
				return new Iter.Int() {

					int idx = beginIdx;

					@Override
					public boolean hasNext() {
						return idx < size();
					}

					@Override
					public int next() {
						if (!hasNext())
							throw new NoSuchElementException();
						return data[idx++];
					}

				};
			}

			@Override
			public int get(int index) {
				return data[index];
			}

			@Override
			public void clear() {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean addAll(ListInt l) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean add(int e) {
				throw new UnsupportedOperationException();
			}
		};
	}

}
