package com.ugav.battalion;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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

public class Utils {

	private Utils() {
		throw new InternalError();
	}

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

	public static <E> Iterator<E> iteratorIf(Iterator<E> it, Predicate<? super E> condition) {
		return new IteratorIf<>(it, condition);
	}

	private static class IteratorIf<E> implements Iterator<E> {

		private final Iterator<E> it;
		private final Predicate<? super E> condition;
		private Object nextElm;
		private static final Object NoElm = new Object();

		public IteratorIf(Iterator<E> it, Predicate<? super E> condition) {
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

	public static <E> Iterator<E> iteratorRepeat(Iterable<E> iterable, int repeat) {
		if (repeat < 0)
			throw new IllegalArgumentException();
		return new IteratorRepeat<>(iterable, repeat);
	}

	public static <E> Iterator<E> iteratorRepeatInfty(Iterable<E> iterable) {
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

	public static <T> String toString(T[][] a) {
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

	public static String buildPath(String base, String... names) {
		String path = new File(base).getAbsolutePath();
		for (String name : names)
			path = new File(path, name).getAbsolutePath();
		return path;
	}

	public static List<String> readLines(String filename) throws FileNotFoundException, IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			for (String line; (line = br.readLine()) != null;) {
				lines.add(line);
			}
		}
		return lines;
	}

	public static BufferedImage imgDeepCopy(BufferedImage img) {
		ColorModel cm = img.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = img.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	public static BufferedImage imgTransform(BufferedImage img, Consumer<int[]> op) {
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

	public static BufferedImage bufferedImageFromImage(Image img) {
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();
		return bimage;
	}

	public static BufferedImage imgTransparent(Image img, double alpha) {
		BufferedImage tImg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = tImg.createGraphics();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
		g2.drawImage(img, 0, 0, null);
		g2.dispose();
		return tImg;
	}

	public static BufferedImage imgRotate(BufferedImage img, double theta) {
		if (theta == 0)
			return img;
		AffineTransform transform = new AffineTransform();
		transform.rotate(theta, img.getWidth() / 2, img.getHeight() / 2);
		AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
		return op.filter(img, null);
	}

	public static BufferedImage imgMirror(BufferedImage img) {
		AffineTransform transform = AffineTransform.getScaleInstance(-1, 1);
		transform.translate(-img.getWidth(null), 0);
		AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		return op.filter(img, null);
	}

	public static class Holder<T> {
		public T val;

		public Holder() {
			this(null);
		}

		public Holder(T val) {
			this.val = val;
		}
	}

	public static <T> List<T> sorted(Collection<T> c) {
		return sorted(c, null);
	}

	public static <T> List<T> sorted(Collection<T> c, Comparator<? super T> cmp) {
		List<T> l = new ArrayList<>(c);
		l.sort(cmp);
		return l;
	}

	static <T> List<T> listCollect(Iterable<T> it) {
		if (it instanceof Collection<?>)
			return new ArrayList<>((Collection<T>) it);
		return listCollect(it.iterator());
	}

	static <T> List<T> listCollect(Iterator<T> it) {
		List<T> l = new ArrayList<>();
		while (it.hasNext())
			l.add(it.next());
		return l;
	}

	public static GridBagConstraints gbConstraints(int x, int y, int width, int height) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		c.gridheight = height;
		return c;
	}

	public static boolean isInteger(double x) {
		return x == Math.floor(x) && !Double.isInfinite(x);
	}

	public static int mod(int x, int m) {
		if (m <= 0)
			throw new IllegalArgumentException();
		int r = x % m;
		if (r < 0)
			r = (r + m) % m;
		assert r >= 0;
		return r;
	}

}
