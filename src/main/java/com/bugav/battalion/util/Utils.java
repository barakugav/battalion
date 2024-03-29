package com.bugav.battalion.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Utils {

	private Utils() {
		throw new RuntimeException();
	}

	public static <E> Iter<E> iteratorRepeat(java.lang.Iterable<E> iterable, int repeat) {
		return IteratorRepeat.create(iterable, repeat);
	}

	public static <E> Iter<E> iteratorRepeatInfty(java.lang.Iterable<E> iterable) {
		return IteratorRepeat.infty(iterable);
	}

	private static class IteratorRepeat<E> implements Iter<E> {

		private final java.lang.Iterable<E> iterable;
		private Iterator<E> it;
		private int repeat;

		private static final int RepeatInfty = -1;

		private IteratorRepeat(java.lang.Iterable<E> iterable, int repeat) {
			this.iterable = iterable;
			this.repeat = repeat;
			it = iterable.iterator();
		}

		static <E> IteratorRepeat<E> create(java.lang.Iterable<E> iterable, int repeat) {
			if (repeat < 0)
				throw new IllegalArgumentException();
			return new IteratorRepeat<>(iterable, RepeatInfty);
		}

		static <E> IteratorRepeat<E> infty(java.lang.Iterable<E> iterable) {
			return new IteratorRepeat<>(iterable, RepeatInfty);
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

	public static BufferedImage imgTransparent(Image img, float alpha) {
		BufferedImage tImg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = tImg.createGraphics();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		g2.drawImage(img, 0, 0, null);
		g2.dispose();
		return tImg;
	}

	public static BufferedImage imgDarken(BufferedImage img, float dark) {
		BufferedImage dImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = dImg.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return new RescaleOp(dark, 0, null).filter(dImg, null);
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

	public static BufferedImage imgSub(BufferedImage img, int x, int y, int width, int height) {
		BufferedImage sub = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = sub.createGraphics();
		g2.drawImage(img, 0, 0, width, height, x, y, x + width, y + height, null);
		g2.dispose();
		return sub;
	}

	public static BufferedImage imgSubCircle(BufferedImage img, int x, int y, int width, int height) {
		BufferedImage sub = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = sub.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setClip(new Ellipse2D.Double(0, 0, width, height));
		g2.drawImage(img, 0, 0, width, height, x, y, x + width, y + height, null);
		g2.dispose();
		return sub;
	}

	public static Dimension max(Dimension d1, Dimension d2) {
		return new Dimension(Math.max(d1.width, d2.width), Math.max(d1.height, d2.height));
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

	public static <T> List<T> listCollect(Iterable<T> it) {
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

	public static <T> List<T> listOf(T elm, List<T> l) {
		List<T> r = new ArrayList<>(1 + l.size());
		r.add(elm);
		r.addAll(l);
		return r;
	}

	static <T> List<T> listOf(List<T> l, @SuppressWarnings("unchecked") T... ts) {
		List<T> r = new ArrayList<>(l.size() + ts.length);
		r.addAll(l);
		r.addAll(List.of(ts));
		return r;
	}

	public static GridBagConstraints gbConstraints(int x, int y, int width, int height) {
		return gbConstraints(x, y, width, height, GridBagConstraints.NONE, 0, 0);
	}

	public static GridBagConstraints gbConstraints(int x, int y, int width, int height, int fill, double weightx,
			double weighty) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.weightx = weightx;
		c.weighty = weighty;
		c.gridwidth = width;
		c.gridheight = height;
		c.fill = fill;
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

	/* NOT efficient */
	static int gcd(int a, int b) {
		return a == 0 ? b : gcd(b % a, a);
	}

	/* NOT efficient */
	static int gcdArr(int a[]) {
		int r = a[0];
		for (int x : a)
			if ((r = gcd(r, x)) == 1)
				return 1;
		for (int x : a)
			assert x % r == 0;
		return r;
	}

	public static int[] toArray(Collection<Integer> c) {
		int[] arr = new int[c.size()];
		int idx = 0;
		for (Integer x : c)
			arr[idx++] = x.intValue();
		return arr;
	}

	public static void swingRun(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch (InvocationTargetException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void swingRunLater(int delayMs, Runnable runnable) {
		Timer timer = new Timer(delayMs, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				runnable.run();
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	public static <E extends Event, L extends Event.Listener<? super E>> Event.Listener<E> swingListener(L listener) {
		return e -> swingRun(() -> listener.onEvent(e));
	}

	public static JButton newButton(String label, ActionListener action) {
		JButton button = new JButton(label);
		button.addActionListener(action);
		return button;
	}

	public static JComponent fillerComp() {
		JPanel comp = new JPanel();
		comp.setPreferredSize(new Dimension(0, 0));
		comp.setOpaque(false);
		return comp;
	}

	public static class RoundedPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private Color backgroundColor;

		public RoundedPanel() {
			setOpaque(false);
		}

		@Override
		public void setBackground(Color color) {
			backgroundColor = color;
		}

		@Override
		public Color getBackground() {
			return backgroundColor != null ? backgroundColor : super.getBackground();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			int width = getWidth();
			int height = getHeight();
			Graphics2D graphics = (Graphics2D) g;
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			graphics.setColor(getBackground());
			graphics.fillOval(0, 0, width, height);
		}
	}

	public static void assertBlk(BooleanSupplier a) {
		assert a.getAsBoolean();
	}

}
