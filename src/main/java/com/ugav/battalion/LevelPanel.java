package com.ugav.battalion;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JPanel;

class LevelPanel extends JPanel {

	private final GameFrame gameFrame;

	private final Map<Position, TileComp> tiles;
	private final List<UnitComp> units;
	private final List<BuildingComp> buildings;
	private Position selection;
	private Position.Bitmap reachableMap = Position.Bitmap.empty;
	private Position.Bitmap attackableMap = Position.Bitmap.empty;
	private Game game;
	private final Images images = new Images();
	private final DebugPrintsManager debug;

	private static final int TILE_SIZE_PIXEL = 64;

	LevelPanel(GameFrame gameFrame) {
		this.gameFrame = Objects.requireNonNull(gameFrame);

		tiles = new HashMap<>();
		units = new ArrayList<>();
		buildings = new ArrayList<>();
		selection = null;
		debug = new DebugPrintsManager(true); // TODO

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				tileClicked(new Position(e.getX() / TILE_SIZE_PIXEL, e.getY() / TILE_SIZE_PIXEL));
			}
		});
	}

//	private TileComp tile(Position pos) {
//		return tiles.get(pos);
//	}

	void setGame(Game game) {
		this.game = Objects.requireNonNull(game);

		tiles.clear();
		units.clear();
		buildings.clear();
		for (Position pos : game.arena.positions()) {
			TileComp tile = new TileComp(pos);
			tiles.put(pos, tile);
			if (tile.tile().hasUnit())
				units.add(new UnitComp(tile.tile().getUnit()));
			if (tile.tile().hasBuilding())
				buildings.add(new BuildingComp(tile.tile().getBuilding()));
		}

		setPreferredSize(getPreferredSize());
		invalidate();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		for (TileComp tile : tiles.values())
			tile.paintComponent(g);
		for (BuildingComp building : buildings)
			building.paintComponent(g);
		for (UnitComp unit : units)
			unit.paintComponent(g);

		if (selection != null) {
			tiles.get(selection).drawImage(g, Images.Label.Selection);
			for (Position pos : reachableMap)
				tiles.get(pos).drawImage(g, Images.Label.Reachable);
			for (Position pos : attackableMap)
				tiles.get(pos).drawImage(g, Images.Label.Attackable);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(TILE_SIZE_PIXEL * game.arena.getWidth(), TILE_SIZE_PIXEL * game.arena.getHeight());
	}

	private void clearSelection() {
		if (selection == null)
			return;
		debug.println("clearSelection ", selection);
		selection = null;
		reachableMap = Position.Bitmap.empty;
		attackableMap = Position.Bitmap.empty;
	}

	private void tileClicked(Position pos) {
		if (game == null)
			return;
		TileComp tile = tiles.get(pos);
		if (selection == null) {
			if (tile.canSelect()) {
				debug.println("Selected ", tile.pos);
				selection = tile.pos;
				Unit unit = tile.tile().getUnit();
				reachableMap = unit.getReachableMap();
				attackableMap = unit.getAttackableMap();
			}

		} else {
			if (game.isMoveValid(selection, tile.pos)) {
				debug.println("Move ", selection, " ", tile.pos);
				game.move(selection, tile.pos);

			} else if (game.isAttackValid(selection, tile.pos)) {
				debug.println("Attack ", selection, " ", tile.pos);
				game.moveAndAttack(selection, tile.pos);
			}

			clearSelection();
		}
		repaint();
	}

	private class UnitComp {
		private final Unit unit;

		private final int HealthBarWidth = 26;
		private final int HealthBarHeight = 4;
		private final int HealthBarBottomMargin = 3;

		UnitComp(Unit unit) {
			this.unit = unit;
		}

		void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			Composite oldComp = g2.getComposite();
			if (unit.getTeam() == game.getTurn() && !unit.isActive())
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			drawImage(g, Images.Label.of(unit), unit.getPos());
			g2.setComposite(oldComp);

			int x = (int) ((unit.getPos().x + 0.5) * TILE_SIZE_PIXEL - HealthBarWidth * 0.5);
			int y = (unit.getPos().y + 1) * TILE_SIZE_PIXEL - HealthBarHeight - HealthBarBottomMargin;
			g2.setColor(Color.GREEN);
			g2.fillRect(x, y, (int) ((double) HealthBarWidth * unit.getHealth() / unit.type.health), HealthBarHeight);
			g2.setColor(Color.BLACK);
			g2.drawRect(x, y, HealthBarWidth, HealthBarHeight);
		}

	}

	private class BuildingComp {
		private final Building building;

		BuildingComp(Building building) {
			this.building = building;
		}

		void paintComponent(Graphics g) {
			drawImage(g, Images.Label.of(building), building.getPos());
		}

	}

	private class TileComp {

		private final Position pos;

		TileComp(Position pos) {
			this.pos = pos;
		}

		private boolean canSelect() {
			if (!tile().hasUnit())
				return false;
			Unit unit = tile().getUnit();
			return unit.isActive();
		}

		void paintComponent(Graphics g) {
			drawImage(g, Images.Label.of(tile().getTerrain()));
		}

		private Tile tile() {
			return game.getTile(pos);
		}

		void drawImage(Graphics g, Images.Label label) {
			LevelPanel.this.drawImage(g, label, pos);
		}

	}

	private void drawImage(Graphics g, Images.Label label, Position pos) {
		BufferedImage unitImg = images.getImage(label);
		g.drawImage(unitImg, pos.x * TILE_SIZE_PIXEL, pos.y * TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, this);
	}

}
