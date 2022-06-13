package com.ugav.battalion;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
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
		if (this.game != null) {
			menu.clearGame();
			arenaPanel.clearGame();
		}

		this.game = Objects.requireNonNull(game);
		menu.initGame();
		arenaPanel.initGame();
		invalidate();
		repaint();
	}

	private void endTurn() {
		debug.println("End turn");
		arenaPanel.clearSelection();
		game.turnEnd();
		if (game.isFinished()) {
			debug.println("Game finished");
			// TODO
		} else {
			game.turnBegin();
		}
		repaint();
	}

	private class Menu extends JPanel {

		private final Map<Team, JLabel> labelMoney;
		private final JButton buttonEndTurn;
		private final DataChangeRegister register;

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

			register = new DataChangeRegister();

			for (Team team : Team.values())
				updateMoneyLabel(team, 0);
		}

		void initGame() {
			register.registerListener(game.onMoneyChange, e -> updateMoneyLabel(e.team, e.newAmount));
		}

		void clearGame() {
			register.unregisterAllListeners(game.onMoneyChange);
		}

		private void updateMoneyLabel(Team team, int money) {
			labelMoney.get(team).setText(team.toString() + ": " + money);
		}

	}

	private class ArenaPanel extends JPanel {

		private final Map<Position, TileComp> tiles;
		private final Map<Unit, UnitComp> units;
		private final Map<Building, BuildingComp> buildings;
		private Position selection;
		private Position.Bitmap reachableMap = Position.Bitmap.empty;
		private Position.Bitmap attackableMap = Position.Bitmap.empty;
		private final DataChangeRegister register;

		ArenaPanel() {
			tiles = new HashMap<>();
			units = new IdentityHashMap<>();
			buildings = new IdentityHashMap<>();
			selection = null;
			register = new DataChangeRegister();

			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					tileClicked(new Position(e.getX() / TILE_SIZE_PIXEL, e.getY() / TILE_SIZE_PIXEL));
				}
			});
		}

		void initGame() {
			for (Position pos : game.arena.positions()) {
				TileComp tile = new TileComp(pos);
				tiles.put(pos, tile);
				if (tile.tile().hasUnit())
					addUnitComp(tile.tile().getUnit());

				if (tile.tile().hasBuilding()) {
					Building building = tile.tile().getBuilding();
					buildings.put(building, new BuildingComp(building));
				}
			}

			setPreferredSize(getPreferredSize());

			register.registerListener(game.onNewUnit, e -> {
				addUnitComp(e.unit);
				repaint();
			});
		}

		void clearGame() {
			for (TileComp tile : tiles.values())
				tile.clear();
			for (BuildingComp building : buildings.values())
				building.clear();
			for (UnitComp unit : units.values())
				unit.clear();
			tiles.clear();
			units.clear();
			buildings.clear();

			register.unregisterAllListeners(game.onNewUnit);
		}

		private void addUnitComp(Unit unit) {
			units.put(unit, new UnitComp(unit));
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

		void clearSelection() {
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
			TileComp tileComp = tiles.get(pos);
			if (selection == null) {
				trySelect(tileComp);

			} else {
				actionAfterSelection(tileComp);
				clearSelection();
			}
			repaint();
		}

		private void trySelect(TileComp tileComp) {
			if (!tileComp.canSelect())
				return;
			debug.println("Selected ", tileComp.pos);
			selection = tileComp.pos;
			Tile tile = tileComp.tile();

			if (tile.hasUnit()) {
				Unit unit = tile.getUnit();
				reachableMap = unit.getReachableMap();
				attackableMap = unit.getAttackableMap();

			} else if (tile.hasBuilding()) {
				Building building = tile.getBuilding();
				if (building instanceof Building.Factory) {
					Building.Factory factory = (Building.Factory) building;
					FactoryMenu factoryMenu = new FactoryMenu(gameFrame, factory);
					factoryMenu.setVisible(true);
				}

			} else {
				throw new InternalError();
			}
		}

		private void actionAfterSelection(TileComp tileComp) {
			if (!tileComp.tile().hasUnit()) {
				if (game.isMoveValid(selection, tileComp.pos)) {
					debug.println("Move ", selection, " ", tileComp.pos);
					game.move(selection, tileComp.pos);
				}
			} else if (game.isAttackValid(selection, tileComp.pos)) {
				debug.println("Attack ", selection, " ", tileComp.pos);
				Unit target = tileComp.tile().getUnit();
				game.moveAndAttack(selection, tileComp.pos);
				if (target.isDead())
					units.remove(target);
			}
		}

		private class UnitComp {
			private final Unit unit;
			private final DataChangeRegister register;

			private final int HealthBarWidth = 26;
			private final int HealthBarHeight = 4;
			private final int HealthBarBottomMargin = 3;

			UnitComp(Unit unit) {
				this.unit = unit;
				register = new DataChangeRegister();

				register.registerListener(unit.onChange, e -> {
					if (unit.isDead())
						units.remove(unit);
				});
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
				g2.fillRect(x + 1, y, (int) ((double) (HealthBarWidth - 1) * unit.getHealth() / unit.type.health),
						HealthBarHeight);
				g2.setColor(Color.BLACK);
				g2.drawRect(x, y, HealthBarWidth, HealthBarHeight);
			}

			void clear() {
				register.unregisterAllListeners(unit.onChange);
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

			void clear() {
			}

		}

		private class TileComp {

			private final Position pos;

			TileComp(Position pos) {
				this.pos = pos;
			}

			private boolean canSelect() {
				if (tile().hasUnit()) {
					Unit unit = tile().getUnit();
					return unit.isActive();
				}
				if (tile().hasBuilding()) {
					Building building = tile().getBuilding();
					return building.isActive();
				}
				return false;
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

			@Override
			public String toString() {
				return pos.toString();
			}

			void clear() {
			}

		}

		private void drawImage(Graphics g, Images.Label label, Position pos) {
			BufferedImage unitImg = images.getImage(label);
			g.drawImage(unitImg, pos.x * TILE_SIZE_PIXEL, pos.y * TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, TILE_SIZE_PIXEL,
					this);
		}

	}

	private class FactoryMenu extends JDialog {

		private final Building.Factory factory;

		FactoryMenu(JFrame parent, Building.Factory factory) {
			super(parent);
			this.factory = factory;
			initUI();
		}

		private void initUI() {
			setTitle("Factory");

			int unitCount = Unit.Type.values().length;
			setLayout(new GridLayout(1, unitCount));

			List<Building.Factory.UnitSale> sales = factory.getAvailableUnits();

			for (int unitIdx = 0; unitIdx < unitCount; unitIdx++) {
				Unit.Type unit = Unit.Type.values()[unitIdx];
				Building.Factory.UnitSale unitSale = null;
				for (Building.Factory.UnitSale sale : sales)
					if (sale.type == unit)
						unitSale = sale;

				JPanel saleComp = new JPanel();
				saleComp.setLayout(new GridBagLayout());
				JComponent upperComp;
				JComponent lowerComp;

				if (unitSale != null) {
					upperComp = new JLabel(new ImageIcon(images.getImage(Images.Label.of(unit, Team.Red))));
					lowerComp = new JLabel("" + unitSale.price);
				} else {
					upperComp = new JLabel(new ImageIcon(images.getImage(Images.Label.UnitLocked)));
					lowerComp = new JLabel("none");
				}

				GridBagConstraints c = new GridBagConstraints();
				c.gridx = c.gridy = 0;
				c.gridwidth = 1;
				c.gridheight = 3;
				saleComp.add(upperComp, c);
				c.gridy = 3;
				c.gridheight = 1;
				saleComp.add(lowerComp, c);

				if (unitSale != null) {
					Building.Factory.UnitSale sale = unitSale;
					saleComp.addMouseListener(new MouseAdapter() {
						@Override
						public void mousePressed(MouseEvent e) {
							buyNewUnit(sale);
						}
					});
				}

				add(saleComp);
			}

			pack();
			setLocationRelativeTo(getParent());
		}

		private void buyNewUnit(Building.Factory.UnitSale sale) {
			game.buildUnit(factory, sale.type);
			dispose();
		}
	}

}
