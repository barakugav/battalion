package com.ugav.battalion;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

class Utils {

	private Utils() {
		throw new InternalError();
	}

	static <E> Iterable<E> iterable(Iterator<E> it) {
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

	static <E> Iterator<E> iteratorIf(Iterator<E> it, Predicate<? super E> condition) {
		return new IteratorIf<>(it, condition);
	}

	private static class IteratorIf<E> implements Iterator<E> {

		private final Iterator<E> it;
		private final Predicate<? super E> condition;
		private Object nextElm;
		private static final Object NoElm = new Object();

		IteratorIf(Iterator<E> it, Predicate<? super E> condition) {
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

	static <E> Iterator<E> iteratorRepeat(Iterable<E> iterable, int repeat) {
		return new IteratorRepeat<>(iterable, repeat);
	}

	static <E> Iterator<E> iteratorRepeatInfty(Iterable<E> iterable) {
		return iteratorRepeat(iterable, -1);
	}

	private static class IteratorRepeat<E> implements Iterator<E> {

		private final Iterable<E> iterable;
		private Iterator<E> it;
		private int repeat;

		IteratorRepeat(Iterable<E> iterable, int repeat) {
			this.iterable = iterable;
			this.repeat = repeat;
			it = iterable.iterator();
		}

		@Override
		public boolean hasNext() {
			if (it.hasNext())
				return true;
			if (repeat == 0)
				return false;
			it = iterable.iterator();
			if (!it.hasNext())
				return false;
			if (repeat > 0)
				repeat--;
			return true;
		}

		@Override
		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			return it.next();
		}

	}

	static <T> String toString(T[][] a) {
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";
		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			b.append(Arrays.toString(a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

}
