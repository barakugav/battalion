package com.ugav.battalion;

import java.util.Iterator;

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

}
