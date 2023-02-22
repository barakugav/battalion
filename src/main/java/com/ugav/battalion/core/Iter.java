package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ugav.battalion.Utils;

public interface Iter<E> extends Iterator<E> {

	default <T> Iter<T> map(Function<? super E, ? extends T> map) {
		return Utils.iteratorMap(this, map);
	}

	default Iter<E> filter(Predicate<? super E> filter) {
		return Utils.iteratorIf(this, filter);
	}

	default List<E> collectList() {
		List<E> l = new ArrayList<>();
		while (hasNext())
			l.add(next());
		return l;
	}

	default Iterable<E> forEach() {
		return Utils.iterable(this);
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
