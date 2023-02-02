package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.ugav.battalion.Images.Drawable;
import com.ugav.battalion.Images.Label;

abstract class AbstractArenaPanel extends JPanel implements Clearable {

	private Position mapPos;
	private double mapPosX, mapPosY;
	private Position hovered;

	final DataChangeNotifier<HoverChangeEvent> onHoverChange;
	final DataChangeNotifier<TileClickEvent> onTileClick;

	static class HoverChangeEvent extends DataEvent {

		final Position pos;

		HoverChangeEvent(AbstractArenaPanel source, Position pos) {
			super(source);
			this.pos = pos;
		}

	}

	static class TileClickEvent extends DataEvent {

		final Position pos;

		TileClickEvent(AbstractArenaPanel source, Position pos) {
			super(source);
			this.pos = pos;
		}

	}

	private static final int TILE_SIZE_PIXEL = 64;
	private static final int DISPLAYED_ARENA_WIDTH = 8;
	private static final int DISPLAYED_ARENA_HEIGHT = 8;

	private static final int MapMoveTimerDelay = 10;
	private static final int MapMoveSpeed = 4;
	private static final long serialVersionUID = 1L;

	AbstractArenaPanel() {
		onHoverChange = new DataChangeNotifier<>();
		onTileClick = new DataChangeNotifier<>();

		mapPos = new Position(0, 0);
		mapPosX = mapPosY = 0;

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
				int clickx = displayedXInv(e.getX()) / TILE_SIZE_PIXEL;
				int clicky = displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
				onTileClick.notify(new TileClickEvent(AbstractArenaPanel.this, new Position(clickx, clicky)));
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int x = displayedXInv(e.getX()) / TILE_SIZE_PIXEL, y = displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
				if (hovered == null || hovered.x != x || hovered.y != y) {
					hovered = new Position(x, y);
					onHoverChange.notify(new HoverChangeEvent(AbstractArenaPanel.this, hovered));
				}
			}
		});
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				Position.Direction dir = keyToDir(e.getKeyCode());
				if (dir != null)
					mapViewMove(dir);
			}

			private Position.Direction keyToDir(int keyCode) {
				switch (keyCode) {
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_A:
					return Position.Direction.XNeg;
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_D:
					return Position.Direction.XPos;
				case KeyEvent.VK_UP:
				case KeyEvent.VK_W:
					return Position.Direction.YNeg;
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_S:
					return Position.Direction.YPos;
				default:
					return null;
				}

			}
		});
		setFocusable(true);
		requestFocusInWindow();

		Timer mapMoveTimer = new Timer(MapMoveTimerDelay, e -> {
			double dx = mapPos.x * TILE_SIZE_PIXEL - mapPosX;
			double dy = mapPos.y * TILE_SIZE_PIXEL - mapPosY;
			if (dx == 0 && dy == 0)
				return;
			int speed = MapMoveSpeed;
			double cx = dx / Math.sqrt(dx * dx + dy * dy) * speed;
			double cy = dy / Math.sqrt(dx * dx + dy * dy) * speed;
			mapPosX += Math.abs(cx) >= Math.abs(dx) ? dx : cx;
			mapPosY += Math.abs(cy) >= Math.abs(dy) ? dy : cy;
			repaint();
		});
		mapMoveTimer.setRepeats(true);
		mapMoveTimer.start();

		setPreferredSize(getPreferredSize());
	}

	abstract int getArenaWidth();

	abstract int getArenaHeight();

	void mapViewMove(Position.Direction dir) {
		Position mapPosNew = mapPos.add(dir);
		if (!mapPosNew.isInRect(0, 0, getArenaWidth() - DISPLAYED_ARENA_WIDTH,
				getArenaHeight() - DISPLAYED_ARENA_HEIGHT))
			return;
		mapPos = mapPosNew;
		repaint();
	}

	void mapViewSet(Position pos) {
		if (!pos.isInRect(0, 0, getArenaWidth() - DISPLAYED_ARENA_WIDTH, getArenaHeight() - DISPLAYED_ARENA_HEIGHT))
			return;
		mapPos = pos;
		mapPosX = pos.x;
		mapPosY = pos.y;
		repaint();
	}

	int displayedX(double x) {
		return (int) (x - mapPosX);
	}

	int displayedY(double y) {
		return (int) (y - mapPosY);
	}

	int displayedXInv(int x) {
		return (int) (x + mapPosX);
	}

	int displayedYInv(int y) {
		return (int) (y + mapPosY);
	}

	@Override
	public void clear() {
	}

	@Override
	protected void paintComponent(Graphics g) {
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(TILE_SIZE_PIXEL * DISPLAYED_ARENA_WIDTH, TILE_SIZE_PIXEL * DISPLAYED_ARENA_HEIGHT);
	}

	void drawImage(Graphics g, Object obj, Position pos) {
		BufferedImage img;
		if (obj instanceof Label)
			img = Images.getImage((Label) obj);
		else if (obj instanceof Drawable)
			img = Images.getImage((Drawable) obj);
		else
			throw new IllegalArgumentException(Objects.toString(obj));
		g.drawImage(img, displayedX(pos.x * TILE_SIZE_PIXEL), displayedY(pos.y * TILE_SIZE_PIXEL), TILE_SIZE_PIXEL,
				TILE_SIZE_PIXEL, this);
	}

}
