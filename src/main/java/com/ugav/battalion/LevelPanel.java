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

public class LevelPanel extends JPanel {

    private final GameFrame gameFrame;

    private final BoardTile[][] tiles;
    private int selectedX;
    private int selectedY;
    private Game game;

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
    }

    void setGame(Game game) {
	this.game = Objects.requireNonNull(game);
    }

    @Override
    public Dimension getPreferredSize() {
	return new Dimension(BOARD_SIZE_PIXEL, BOARD_SIZE_PIXEL);
    }

    private void clearSelection() {
	if (!(anySelection()))
	    return;
	int oldSelectedX = selectedX;
	int oldSelectedY = selectedY;
	selectedX = -1;
	selectedY = -1;
	tiles[oldSelectedX][oldSelectedY].repaint();
	System.out.println("clearSelection " + oldSelectedX + " " + oldSelectedY);
    }

    private boolean anySelection() {
	return selectedX >= 0 && selectedY >= 0;
    }

    private Tile getSelection() {
	if (!anySelection())
	    throw new IllegalStateException();
	return game.getTile(selectedX, selectedY);
    }

    private class BoardTile extends JPanel {

	private final int x;
	private final int y;

	public BoardTile(int x, int y) {
	    this.x = x;
	    this.y = y;

	    setOpaque(true);
	    setBackground(new Color(new Random().nextInt()));

	    addMouseListener(new MouseAdapter() {

		@Override
		public void mousePressed(MouseEvent e) {
		    if (game == null)
			return;
		    if (anySelection()) {
			Tile tile = getSelection();
			Unit unit = tile.getUnit();
			if (game.canMove(selectedX, selectedY, x, y)) {
			    System.out.printf("Move %d %d %d %d\n", selectedX, selectedY, x, y);
			    game.move(selectedX, selectedY, x, y);
			    clearSelection();
			    repaint();
			    return;
			}
			if (game.canAttak(selectedX, selectedY, x, y)) {
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
		System.out.printf("Selected %d %d\n", x, y);
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
	    return unit.canAct();
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
		if (!unit.canAct())
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
