package com.ugav.battalion;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
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
		if (repeat < 0)
			throw new IllegalArgumentException();
		return new IteratorRepeat<>(iterable, repeat);
	}

	static <E> Iterator<E> iteratorRepeatInfty(Iterable<E> iterable) {
		return new IteratorRepeat<>(iterable, -1);
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
			if (repeat > 0)
				repeat--;
			return it.hasNext();
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

	static String buildPath(String base, String... names) {
		String path = new File(base).getAbsolutePath();
		for (String name : names)
			path = new File(path, name).getAbsolutePath();
		return path;
	}

	static List<String> readLines(String filename) throws FileNotFoundException, IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			for (String line; (line = br.readLine()) != null;) {
				lines.add(line);
			}
		}
		return lines;
	}

	static BufferedImage imgDeepCopy(BufferedImage img) {
		ColorModel cm = img.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = img.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	static BufferedImage imgTransform(BufferedImage img, Consumer<int[]> op) {
		int[] pixel = new int[img.getRaster().getNumBands()];
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				pixel = img.getRaster().getPixel(x, y, pixel);
				op.accept(pixel);
				img.getRaster().setPixel(x, y, pixel);
			}
		}
		return img;
	}

	static class Holder<T> {
		T val;

		Holder() {
			this(null);
		}

		Holder(T val) {
			this.val = val;
		}
	}

	static <T> List<T> sorted(Collection<T> c) {
		return sorted(c, null);
	}

	static <T> List<T> sorted(Collection<T> c, Comparator<? super T> cmp) {
		List<T> l = new ArrayList<>(c);
		l.sort(cmp);
		return l;
	}

}
