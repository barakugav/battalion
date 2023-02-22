package com.ugav.battalion.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Iter<E> extends Iterator<E> {

	public static <E> Iterable<E> iterable(Iterator<E> it) {
		/*
		 * java lack nice for loop syntax using iterators, hopefully this code will be
		 * inlined by the compiler and no object will be created here
		 */
		return new Iterable<>() {

			@Override
			public Iterator<E> iterator() {
				return it;
			}
		};
	}

	public static class IterMap<T, R> implements Iter<R> {
		private final Iterator<T> it;
		private final Function<? super T, ? extends R> map;

		IterMap(Iterator<T> it, Function<? super T, ? extends R> map) {
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
		return new IterMap<>(this, map);
	}

	public static class IterIf<E> implements Iter<E> {

		private final Iterator<E> it;
		private final Predicate<? super E> condition;
		private Object nextElm;
		private static final Object NoElm = new Object();

		public IterIf(Iterator<E> it, Predicate<? super E> condition) {
			this.it = it;
			this.condition = condition;
			nextElm = NoElm;
		}

		@Override
		public boolean hasNext() {
			if (nextElm != NoElm)
				return true;
			for (; it.hasNext();) {
				E e = it.next();
				if (condition.test(e)) {
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
		return new IterIf<>(this, filter);
	}

	default List<E> collectList() {
		List<E> l = new ArrayList<>();
		while (hasNext())
			l.add(next());
		return l;
	}

	default Iterable<E> forEach() {
		return iterable(this);
	}

	static class EmptyIter<E> implements Iter<E> {

		@SuppressWarnings("rawtypes")
		private static final EmptyIter INSTANCE = new EmptyIter();

		private EmptyIter() {
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
		return EmptyIter.INSTANCE;
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

}
