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
import java.util.ArrayList;
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

import com.ugav.battalion.Unit.Weapon;

class LevelPanel extends JPanel {

	private final GameFrame gameFrame;
	private final Menu menu;
	private final ArenaPanel arenaPanel;

	private Game game;
	private Position selection;
	private final Images images = new Images();
	private final DebugPrintsManager debug;

	private static final int TILE_SIZE_PIXEL = 64;

	private static final long serialVersionUID = 1L;

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
		clearSelection();
		game.turnEnd();
		if (game.isFinished()) {
			debug.println("Game finished");
			// TODO
		} else {
			game.turnBegin();
		}
		repaint();
	}

	void clearSelection() {
		if (selection == null)
			return;
		debug.println("clearSelection ", selection);
		selection = null;
		arenaPanel.reachableMap = Position.Bitmap.empty;
		arenaPanel.attackableMap = Position.Bitmap.empty;
		arenaPanel.movePath.clear();
	}

	private class Menu extends JPanel {

		private final Map<Team, JLabel> labelMoney;
		private final JButton buttonEndTurn;
		private final DataChangeRegister register;

		private static final long serialVersionUID = 1L;

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

		private Position.Bitmap reachableMap = Position.Bitmap.empty;
		private Position.Bitmap attackableMap = Position.Bitmap.empty;
		private Position hovered;
		private final List<Position> movePath;

		private final DataChangeRegister register;

		private static final long serialVersionUID = 1L;

		ArenaPanel() {
			tiles = new HashMap<>();
			units = new IdentityHashMap<>();
			buildings = new IdentityHashMap<>();
			movePath = new ArrayList<>();
			register = new DataChangeRegister();

			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					tileClicked(new Position(e.getX() / TILE_SIZE_PIXEL, e.getY() / TILE_SIZE_PIXEL));
				}
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					int x = e.getX() / TILE_SIZE_PIXEL, y = e.getY() / TILE_SIZE_PIXEL;
					if (hovered == null || hovered.x != x || hovered.y != y) {
						hovered = new Position(x, y);
						hoveredUpdated();
					}
				}
			});
		}

		void hoveredUpdated() {
			if (!isUnitSelected())
				return;
			Unit unit = game.arena.at(selection).getUnit();

			if (reachableMap.at(hovered)) {
				/* Update move path from unit position to hovered position */
				Position last = movePath.isEmpty() ? unit.getPos() : movePath.get(movePath.size() - 1);

				if (movePath.contains(hovered)) {
					/* Already contained in path, remove all unnecessary steps */
					while (!movePath.isEmpty()) {
						if (movePath.get(movePath.size() - 1).equals(hovered))
							break;
						movePath.remove(movePath.size() - 1);
					}

				} else if (movePath.size() < unit.type.moveLimit && last.neighbors().contains(hovered)) {
					/* Append to the end of the move path */
					movePath.add(hovered);

				} else {
					/* Unable to append to end of current move path, calculate new route */
					movePath.clear();
					movePath.addAll(unit.calcPath(hovered));
				}
				repaint();
			}
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
			if (isUnitSelected()) {
				Unit unit = game.arena.at(selection).getUnit();
				g.setColor(new Color(100, 0, 0));
				Position prev = unit.getPos();
				for (Position pos : movePath) {
					int prevX = prev.x * TILE_SIZE_PIXEL, prevY = prev.y * TILE_SIZE_PIXEL;
					int pX = pos.x * TILE_SIZE_PIXEL, pY = pos.y * TILE_SIZE_PIXEL;

					int ovalRadius = TILE_SIZE_PIXEL / 2;
					int ovalOffset = (TILE_SIZE_PIXEL - ovalRadius) / 2;
					g.fillOval(prevX + ovalOffset, prevY + ovalOffset, ovalRadius, ovalRadius);
					g.fillOval(pX + ovalOffset, pY + ovalOffset, ovalRadius, ovalRadius);

					int rCenterX = (prevX + pX) / 2 + TILE_SIZE_PIXEL / 2;
					int rCenterY = (prevY + pY) / 2 + TILE_SIZE_PIXEL / 2;
					int rWidth = prevX == pX ? ovalRadius : TILE_SIZE_PIXEL;
					int rHeight = prevY == pY ? ovalRadius : TILE_SIZE_PIXEL;
					g.fillRect(rCenterX - rWidth / 2, rCenterY - rHeight / 2, rWidth, rHeight);

					prev = pos;
				}
			}
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(TILE_SIZE_PIXEL * game.arena.getWidth(), TILE_SIZE_PIXEL * game.arena.getHeight());
		}

		private boolean isUnitSelected() {
			return selection != null && game.arena.at(selection).hasUnit();
		}

		private void tileClicked(Position pos) {
			if (game == null)
				return;
			if (selection == null) {
				trySelect(pos);

			} else if (isUnitSelected()) {
				unitSecondSelection(pos);
				clearSelection();
			}
			repaint();
		}

		private void trySelect(Position pos) {
			TileComp tileComp = tiles.get(pos);
			if (!tileComp.canSelect())
				return;
			debug.println("Selected ", pos);
			selection = pos;
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

		private void unitSecondSelection(Position pos) {
			Tile tile = game.arena.at(pos);
			Unit unit = tiles.get(selection).tile().getUnit();
			Unit target;
			if (!tile.hasUnit()) {
				if (!pos.equals(movePath.get(movePath.size() - 1))) {
					movePath.clear();
					movePath.addAll(unit.calcPath(hovered));
				}
				if (game.isMoveValid(unit, movePath)) {
					debug.println("Move ", unit.getPos(), " ", pos);
					game.move(unit, movePath);
				}
				return;
			} else if (game.isAttackValid(unit, target = tile.getUnit())) {
				debug.println("Attack ", unit.getPos(), " ", pos);

				if (unit.type.weapon == Weapon.CloseRange) {
					assert (movePath.isEmpty() ? unit.getPos() : movePath.get(movePath.size() - 1)).neighbors()
							.contains(target.getPos());
					game.moveAndAttack(unit, movePath, target);

				} else if (unit.type.weapon == Weapon.LongRange) {
					game.attackRange(unit, target);

				} else {
					throw new InternalError("Unknown unit weapon type: " + unit.type.weapon);
				}
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
				drawImage(g, Images.Label.valueOf(unit), unit.getPos());
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
				drawImage(g, Images.Label.valueOf(building), building.getPos());
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
				drawImage(g, Images.Label.valueOf(tile().getTerrain()));
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

		private static final long serialVersionUID = 1L;

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
					upperComp = new JLabel(new ImageIcon(images.getImage(Images.Label.valueOf(unit, Team.Red))));
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
			if (sale.price > game.getMoney(factory.getTeam()))
				return;
			game.buildUnit(factory, sale.type);
			clearSelection();
			dispose();
		}
	}

}
