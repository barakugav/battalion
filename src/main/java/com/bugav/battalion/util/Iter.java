package com.bugav.battalion.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public interface Iter<E> extends Iterator<E> {

	public static <E> java.lang.Iterable<E> iterable(Iterator<E> it) {
		/*
		 * java lack nice for loop syntax using iterators, hopefully this code will be
		 * inlined by the compiler and no object will be created here
		 */
		return new java.lang.Iterable<>() {

			@Override
			public Iterator<E> iterator() {
				return it;
			}
		};
	}

	public static <E> Iterable<E> iterable(Iter<E> it) {
		/*
		 * java lack nice for loop syntax using iterators, hopefully this code will be
		 * inlined by the compiler and no object will be created here
		 */
		return new Iterable<>() {

			@Override
			public Iter<E> iterator() {
				return it;
			}
		};
	}

	public static class Map<T, R> implements Iter<R> {
		private final Iterator<T> it;
		private final Function<? super T, ? extends R> map;

		Map(Iterator<T> it, Function<? super T, ? extends R> map) {
			this.it = Objects.requireNonNull(it);
			this.map = Objects.requireNonNull(map);
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public R next() {
			return map.apply(it.next());
		}

	}

	default <T> Iter<T> map(Function<? super E, ? extends T> map) {
		return new Map<>(this, map);
	}

	public static class MapBool<T> implements Iter.Bool {
		private final Iterator<T> it;
		private final Predicate<? super T> map;

		MapBool(Iterator<T> it, Predicate<? super T> map) {
			this.it = Objects.requireNonNull(it);
			this.map = Objects.requireNonNull(map);
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public boolean next() {
			return map.test(it.next());
		}

	}

	default Iter.Bool mapBool(Predicate<? super E> map) {
		return new MapBool<>(this, map);
	}

	public static class MapInt<T> implements Iter.Int {
		private final Iterator<T> it;
		private final ToIntFunction<? super T> map;

		MapInt(Iterator<T> it, ToIntFunction<? super T> map) {
			this.it = Objects.requireNonNull(it);
			this.map = Objects.requireNonNull(map);
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public int next() {
			return map.applyAsInt(it.next());
		}

	}

	default Iter.Int mapInt(ToIntFunction<? super E> map) {
		return new MapInt<>(this, map);
	}

	public static class Filter<E> implements Iter<E> {

		private final Iterator<E> it;
		private final Predicate<? super E> filter;
		private Object nextElm;
		private static final Object NoElm = new Object();

		public Filter(Iterator<E> it, Predicate<? super E> filter) {
			this.it = it;
			this.filter = filter;
			nextElm = NoElm;
		}

		@Override
		public boolean hasNext() {
			if (nextElm != NoElm)
				return true;
			for (; it.hasNext();) {
				E e = it.next();
				if (filter.test(e)) {
					nextElm = e;
					return true;
				}
			}
			return false;
		}

		@Override
		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			@SuppressWarnings("unchecked")
			E e = (E) nextElm;
			nextElm = NoElm;
			return e;
		}
	}

	default Iter<E> filter(Predicate<? super E> filter) {
		return new Filter<>(this, filter);
	}

	default List<E> toList() {
		List<E> l = new ArrayList<>();
		while (hasNext())
			l.add(next());
		return l;
	}

	static class Enumerate<E> implements Iter<Indexed<E>> {

		private final Iter<E> it;
		private int idx;

		Enumerate(Iter<E> it) {
			this.it = Objects.requireNonNull(it);
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public Indexed<E> next() {
			return new Indexed<>(idx++, it.next());
		}

	}

	public static class Indexed<E> {
		public final int idx;
		public final E elm;

		Indexed(int idx, E elm) {
			this.idx = idx;
			this.elm = elm;
		}
	}

	default Iter<Indexed<E>> enumerate() {
		return new Enumerate<>(this);
	}

	default Iterable<E> forEach() {
		return iterable(this);
	}

	default boolean contains(E e) {
		if (e != null) {
			while (hasNext())
				if (e.equals(next()))
					return true;
		} else {
			while (hasNext())
				if (null == next())
					return true;
		}
		return false;
	}

	static class Empty<E> implements Iter<E> {

		@SuppressWarnings("rawtypes")
		private static final Empty INSTANCE = new Empty();

		private Empty() {
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public E next() {
			throw new NoSuchElementException();
		}

	}

	@SuppressWarnings("unchecked")
	static <E> Iter<E> empty() {
		return Empty.INSTANCE;
	}

	static class IterWrapper<E> implements Iter<E> {

		private final Iterator<? extends E> it;

		private IterWrapper(Iterator<? extends E> it) {
			this.it = Objects.requireNonNull(it);
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public E next() {
			return it.next();
		}

	}

	static <E> Iter<E> of(Iterator<E> it) {
		return (it instanceof Iter<E>) ? (Iter<E>) it : new IterWrapper<>(it);
	}

	static <E> Iter<E> of(java.lang.Iterable<E> it) {
		return of(it.iterator());
	}

	public interface Int {

		boolean hasNext();

		int next();

		public static class Map<R> implements Iter<R> {
			private final Iter.Int it;
			private final IntFunction<? extends R> map;

			Map(Iter.Int it, IntFunction<? extends R> map) {
				this.it = Objects.requireNonNull(it);
				this.map = Objects.requireNonNull(map);
			}

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public R next() {
				return map.apply(it.next());
			}

		}

		default <T> Iter<T> map(IntFunction<? extends T> map) {
			return new Map<>(this, map);
		}

		public static class MapInt implements Iter.Int {
			private final Iter.Int it;
			private final Func.IntToInt map;

			MapInt(Iter.Int it, Func.IntToInt map) {
				this.it = Objects.requireNonNull(it);
				this.map = Objects.requireNonNull(map);
			}

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public int next() {
				return map.apply(it.next());
			}

		}

		default Iter.Int mapInt(Func.IntToInt map) {
			return new MapInt(this, map);
		}

		public static class Filter implements Iter.Int {

			private final Iter.Int it;
			private final IntPredicate filter;
			private int nextElm;
			private boolean nextValid;

			public Filter(Iter.Int it, IntPredicate filter) {
				this.it = it;
				this.filter = filter;
				nextValid = false;
			}

			@Override
			public boolean hasNext() {
				if (nextValid)
					return true;
				for (; it.hasNext();) {
					int e = it.next();
					if (filter.test(e)) {
						nextElm = e;
						nextValid = true;
						return true;
					}
				}
				return false;
			}

			@Override
			public int next() {
				if (!hasNext())
					throw new NoSuchElementException();
				nextValid = false;
				return nextElm;
			}
		}

		default Iter.Int filter(IntPredicate filter) {
			return new Filter(this, filter);
		}

		default boolean contains(int e) {
			while (hasNext())
				if (e == next())
					return true;
			return false;
		}

		default ListInt toList() {
			ListInt l = new ListInt.Array();
			while (hasNext())
				l.add(next());
			return l;
		}

		default int[] toArray() {
			int[] arr = new int[16];
			int size = 0;
			while (hasNext()) {
				if (size >= arr.length)
					arr = Arrays.copyOf(arr, size * 2);
				arr[size++] = next();
			}
			if (size != arr.length)
				arr = Arrays.copyOf(arr, size);
			return arr;
		}

		default int sum() {
			int s = 0;
			while (hasNext())
				s += next();
			return s;
		}

		default int max() {
			return max(next());
		}

		default int max(int defaultVal) {
			int m = defaultVal;
			while (hasNext())
				m = Math.max(m, next());
			return m;
		}

		default int min() {
			return min(next());
		}

		default int min(int defaultVal) {
			int m = defaultVal;
			while (hasNext())
				m = Math.min(m, next());
			return m;
		}

		static class Empty implements Iter.Int {

			private static final Empty INSTANCE = new Empty();

			private Empty() {
			}

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public int next() {
				throw new NoSuchElementException();
			}

		}

		static Iter.Int empty() {
			return Empty.INSTANCE;
		}

	}

	public interface Bool {

		boolean hasNext();

		boolean next();

		default boolean[] toArray() {
			boolean[] arr = new boolean[16];
			int size = 0;
			while (hasNext()) {
				if (size >= arr.length)
					arr = Arrays.copyOf(arr, size * 2);
				arr[size++] = next();
			}
			if (size != arr.length)
				arr = Arrays.copyOf(arr, size);
			return arr;
		}

		default boolean any() {
			while (hasNext())
				if (next())
					return true;
			return false;
		}

		default boolean all() {
			while (hasNext())
				if (!next())
					return false;
			return true;
		}

		default Iter.Bool not() {
			return Not.of(this);
		}

		static class Not implements Iter.Bool {
			private final Iter.Bool it;

			private Not(Iter.Bool it) {
				this.it = Objects.requireNonNull(it);
			}

			static Iter.Bool of(Iter.Bool it) {
				if (it instanceof Not)
					return ((Not) it).it;
				else
					return new Not(it);
			}

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public boolean next() {
				return !it.next();
			}

		}

		static class Empty implements Iter.Bool {

			private static final Empty INSTANCE = new Empty();

			private Empty() {
			}

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public boolean next() {
				throw new NoSuchElementException();
			}

		}

		static Iter.Bool empty() {
			return Empty.INSTANCE;
		}

	}

	public static interface Iterable<E> extends java.lang.Iterable<E> {

		@Override
		Iter<E> iterator();

		public static interface Int {
			Iter.Int iterator();
		}

	}

}
