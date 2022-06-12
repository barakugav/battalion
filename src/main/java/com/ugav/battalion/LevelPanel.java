package com.ugav.battalion;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.swing.JPanel;

class LevelPanel extends JPanel {

	private final GameFrame gameFrame;

	private final Map<Position, BoardTile> tiles;
	private Position selection;
	private Game game;
	private final Images images = new Images();
	private final DebugPrintsManager debug;

	private static final int TILE_SIZE_PIXEL = 64;

	LevelPanel(GameFrame gameFrame) {
		this.gameFrame = Objects.requireNonNull(gameFrame);

		tiles = new HashMap<>();
		selection = null;
		debug = new DebugPrintsManager(true); // TODO
	}

	private BoardTile tile(Position pos) {
		return tiles.get(pos);
	}

	void setGame(Game game) {
		this.game = Objects.requireNonNull(game);

		int xLen = game.arena.getXLen(), yLen = game.arena.getYLen();
		setLayout(new GridLayout(xLen, yLen));
		setPreferredSize(getPreferredSize());

		tiles.clear();
		for (Position pos : game.arena.positions()) {
			BoardTile tile = new BoardTile(pos);
			tiles.put(pos, tile);
			add(tile);
		}

		invalidate();
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		int xLen = game.arena.getXLen(), yLen = game.arena.getYLen();
		return new Dimension(TILE_SIZE_PIXEL * xLen, TILE_SIZE_PIXEL * yLen);
	}

	private void clearSelection() {
		if (!(isAnySelected()))
			return;
		debug.println("clearSelection ", selection);
		tile(selection).repaint();
		selection = null;
	}

	private boolean isAnySelected() {
		return selection != null;
	}

	private Tile getSelection() {
		if (!isAnySelected())
			throw new IllegalStateException();
		return game.getTile(selection);
	}

	private class BoardTile extends JPanel {

		private final Position pos;

		BoardTile(Position pos) {
			this.pos = pos;

			setOpaque(true);
			setBackground(new Color(new Random().nextInt()));

			addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {
					if (game == null)
						return;
					if (isAnySelected()) {
						Tile tile = getSelection();
						Unit unit = tile.getUnit();
						if (game.isMoveValid(selection, pos)) {
							debug.println("Move ", selection, " ", pos);
							game.move(selection, pos);
							clearSelection();
							repaint();
							return;
						}
						if (game.isAttackValid(selection, pos)) {
							// game.a
						}
					}
					select();
				}

			});
		}

		private void move() {

		}

		private void attack() {

		}

		private void select() {
			boolean select = !isSelected() && canSelect();
			clearSelection();
			if (select) {
				debug.println("Selected ", pos);
				selection = pos;
				repaint();
			}
		}

		private boolean isSelected() {
			return pos.equals(selection);
		}

		private boolean canSelect() {
			if (!tile().hasUnit())
				return false;
			Unit unit = tile().getUnit();
			return unit.isActive();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (game == null)
				return;

			drawImage(g, Images.Label.of(tile().getTerrain()));
			if (tile().hasBuilding())
				drawImage(g, Images.Label.of(tile().getBuilding()));
			if (tile().hasUnit()) {
				Unit unit = tile().getUnit();
				Graphics2D g2 = (Graphics2D) g;
				Composite oldComp = g2.getComposite();
				if (!unit.isActive())
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				drawImage(g, Images.Label.of(unit));
				g2.setComposite(oldComp);
			}
			if (isSelected())
				drawImage(g, Images.Label.Selection);
		}

		private void drawImage(Graphics g, Images.Label label) {
			BufferedImage unitImg = images.getImage(label);
			g.drawImage(unitImg, 0, 0, TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, this);
		}

		private Tile tile() {
			return game.getTile(pos);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(TILE_SIZE_PIXEL, TILE_SIZE_PIXEL);
		}

	}

}
