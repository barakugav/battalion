package com.ugav.battalion;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

class LevelPanel extends JPanel {

	private final GameFrame gameFrame;
	private final Menu menu;
	private final ArenaPanel arenaPanel;

	private Game game;
	private final Images images = new Images();
	private final DebugPrintsManager debug;

	private static final int TILE_SIZE_PIXEL = 64;

	LevelPanel(GameFrame gameFrame) {
		this.gameFrame = Objects.requireNonNull(gameFrame);
		menu = new Menu();
		arenaPanel = new ArenaPanel();
		debug = new DebugPrintsManager(true); // TODO

		setLayout(new FlowLayout());
		add(menu);
		add(arenaPanel);
	}

	void setGame(Game game) {
		this.game = Objects.requireNonNull(game);
		arenaPanel.initNewGame();
		invalidate();
		repaint();
	}

	private void endTurn() {
		debug.println("End turn");
		game.turnEnd();
		if (game.isFinished()) {
			debug.println("Game finished");
			// TODO
		} else {
			game.turnBegin();
		}
		menu.refresh();
		repaint();
	}

	private class Menu extends JPanel {

		private final Map<Team, JLabel> labelMoney;
		private final JButton buttonEndTurn;

		Menu() {
			labelMoney = new HashMap<>();
			for (Team team : Team.values())
				labelMoney.put(team, new JLabel());
			buttonEndTurn = new JButton("End Turn");

			buttonEndTurn.addActionListener(e -> endTurn());

			setLayout(new GridLayout(0, 1));
			for (JLabel label : labelMoney.values())
				add(label);
			add(buttonEndTurn);

			refresh();
		}

		void refresh() {
			for (Team team : Team.values()) {
				int m = game != null ? game.getMoney(team) : 0;
				labelMoney.get(team).setText(team.toString() + ": " + m);
			}
		}

	}

	private class ArenaPanel extends JPanel {

		private final Map<Position, TileComp> tiles;
		private final Map<Unit, UnitComp> units;
		private final Map<Building, BuildingComp> buildings;
		private Position selection;
		private Position.Bitmap reachableMap = Position.Bitmap.empty;
		private Position.Bitmap attackableMap = Position.Bitmap.empty;

		ArenaPanel() {

			tiles = new HashMap<>();
			units = new IdentityHashMap<>();
			buildings = new IdentityHashMap<>();
			selection = null;

			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					tileClicked(new Position(e.getX() / TILE_SIZE_PIXEL, e.getY() / TILE_SIZE_PIXEL));
				}
			});
		}

		void initNewGame() {
			tiles.clear();
			units.clear();
			buildings.clear();
			for (Position pos : game.arena.positions()) {
				TileComp tile = new TileComp(pos);
				tiles.put(pos, tile);
				if (tile.tile().hasUnit()) {
					Unit unit = tile.tile().getUnit();
					units.put(unit, new UnitComp(unit));
				}
				if (tile.tile().hasBuilding()) {
					Building building = tile.tile().getBuilding();
					buildings.put(building, new BuildingComp(building));
				}
			}

			setPreferredSize(getPreferredSize());
		}

		@Override
		protected void paintComponent(Graphics g) {
			for (TileComp tile : tiles.values())
				tile.paintComponent(g);
			for (BuildingComp building : buildings.values())
				building.paintComponent(g);
			for (UnitComp unit : units.values())
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
					debug.println("Selected ", pos);
					selection = pos;
					Unit unit = tile.tile().getUnit();
					reachableMap = unit.getReachableMap();
					attackableMap = unit.getAttackableMap();
				}

			} else {
				if (game.isMoveValid(selection, pos)) {
					debug.println("Move ", selection, " ", pos);
					game.move(selection, pos);

				} else if (game.isAttackValid(selection, pos)) {
					debug.println("Attack ", selection, " ", pos);
					Unit target = tile.tile().getUnit();
					game.moveAndAttack(selection, pos);
					if (target.isDead())
						units.remove(target);
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
				g2.fillRect(x, y, (int) ((double) (HealthBarWidth - 2) * unit.getHealth() / unit.type.health),
						HealthBarHeight);
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
				ArenaPanel.this.drawImage(g, label, pos);
			}

		}

		private void drawImage(Graphics g, Images.Label label, Position pos) {
			BufferedImage unitImg = images.getImage(label);
			g.drawImage(unitImg, pos.x * TILE_SIZE_PIXEL, pos.y * TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, TILE_SIZE_PIXEL,
					this);
		}

	}

}
