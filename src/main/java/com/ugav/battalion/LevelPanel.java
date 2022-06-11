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
import java.util.Objects;
import java.util.Random;

import javax.swing.JPanel;

class LevelPanel extends JPanel {

	private final GameFrame gameFrame;

	private final BoardTile[][] tiles;
	private int selectedX;
	private int selectedY;
	private Game game;
	private final DebugPrintsManager debug;

	private static final int BOARD_SIZE = 3;
	private static final int TILE_SIZE_PIXEL = 64;
	private static final int BOARD_SIZE_PIXEL = TILE_SIZE_PIXEL * BOARD_SIZE;

	LevelPanel(GameFrame gameFrame) {
		this.gameFrame = Objects.requireNonNull(gameFrame);

		setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
		setPreferredSize(getPreferredSize());

		tiles = new BoardTile[BOARD_SIZE][BOARD_SIZE];
		for (int x = 0; x < BOARD_SIZE; x++)
			for (int y = 0; y < BOARD_SIZE; y++)
				add(tiles[x][y] = new BoardTile(x, y));

		selectedX = -1;
		selectedY = -1;

		debug = new DebugPrintsManager(true); // TODO
	}

	void setGame(Game game) {
		this.game = Objects.requireNonNull(game);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(BOARD_SIZE_PIXEL, BOARD_SIZE_PIXEL);
	}

	private void clearSelection() {
		if (!(isAnySelected()))
			return;
		int oldSelectedX = selectedX;
		int oldSelectedY = selectedY;
		selectedX = -1;
		selectedY = -1;
		tiles[oldSelectedX][oldSelectedY].repaint();
		debug.println("clearSelection ", Integer.valueOf(oldSelectedX), " ", Integer.valueOf(oldSelectedY));
	}

	private boolean isAnySelected() {
		return selectedX >= 0 && selectedY >= 0;
	}

	private Tile getSelection() {
		if (!isAnySelected())
			throw new IllegalStateException();
		return game.getTile(selectedX, selectedY);
	}

	private class BoardTile extends JPanel {

		private final int x;
		private final int y;

		BoardTile(int x, int y) {
			this.x = x;
			this.y = y;

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
						if (game.isMoveValid(selectedX, selectedY, x, y)) {
							debug.format("Move %d %d %d %d\n", Integer.valueOf(selectedX), Integer.valueOf(selectedY),
									Integer.valueOf(x), Integer.valueOf(y));
							game.move(selectedX, selectedY, x, y);
							clearSelection();
							repaint();
							return;
						}
						if (game.isAttackValid(selectedX, selectedY, x, y)) {
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
				debug.format("Selected %d %d\n", Integer.valueOf(x), Integer.valueOf(y));
				selectedX = x;
				selectedY = y;
				repaint();
			}
		}

		private boolean isSelected() {
			return selectedX == x && selectedY == y;
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

			drawImage(g, ImageManager.getLabel(tile().getTerrain()));
			if (tile().hasBuilding())
				drawImage(g, ImageManager.getLabel(tile().getBuilding()));
			if (tile().hasUnit()) {
				Unit unit = tile().getUnit();
				Graphics2D g2 = (Graphics2D) g;
				Composite oldComp = g2.getComposite();
				if (!unit.isActive())
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				drawImage(g, ImageManager.getLabel(unit));
				g2.setComposite(oldComp);
			}
			if (isSelected())
				drawImage(g, ImageManager.SELECTION);
		}

		private void drawImage(Graphics g, String label) {
			BufferedImage unitImg = ImageManager.getImage(label);
			g.drawImage(unitImg, 0, 0, TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, this);
		}

		private Tile tile() {
			return game.getTile(x, y);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(TILE_SIZE_PIXEL, TILE_SIZE_PIXEL);
		}

	}

}
