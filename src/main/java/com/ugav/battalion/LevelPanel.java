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
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.ugav.battalion.Unit.Weapon;

class LevelPanel extends JPanel implements Clearable {

	private final GameFrame gameFrame;
	private final Menu menu;
	private final ArenaPanel arenaPanel;

	private final Game game;
	private Position selection;
	private final Images images = new Images();
	private final DebugPrintsManager debug;

	private boolean actionsSuspended;

	private static final int TILE_SIZE_PIXEL = 64;
	private static final int DISPLAYED_ARENA_WIDTH = 8;
	private static final int DISPLAYED_ARENA_HEIGHT = 8;
	private static final long serialVersionUID = 1L;

	LevelPanel(GameFrame gameFrame, Level level) {
		this.gameFrame = Objects.requireNonNull(gameFrame);

		if (level.getWidth() < DISPLAYED_ARENA_WIDTH || level.getHeight() < DISPLAYED_ARENA_HEIGHT)
			throw new IllegalArgumentException("level size is too small");
		this.game = new Game(level);
		menu = new Menu();
		arenaPanel = new ArenaPanel();
		debug = new DebugPrintsManager(true); // TODO

		setLayout(new FlowLayout());
		add(menu);
		add(arenaPanel);

		game.start();

		menu.initGame();
		arenaPanel.initGame();
		invalidate();
		repaint();
	}

	@Override
	public void clear() {
		menu.clear();
		arenaPanel.clear();
	}

	private void endTurn() {
		debug.println("End turn");
		clearSelection();
		game.turnEnd();
		assert !game.isFinished();
		game.turnBegin();
		repaint();
	}

	private void clearSelection() {
		if (selection == null)
			return;
		debug.println("clearSelection ", selection);
		selection = null;
		arenaPanel.reachableMap = Position.Bitmap.empty;
		arenaPanel.attackableMap = Position.Bitmap.empty;
		arenaPanel.movePath.clear();
	}

	private boolean isActionSuspended() {
		return actionsSuspended;
	}

	private void suspendActions() {
		actionsSuspended = true;
	}

	private void resumeActions() {
		actionsSuspended = false;
	}

	private void checkGameStatus() {
		if (game.isFinished()) {
			debug.println("Game finished");
			JOptionPane.showMessageDialog(this, "tttt");
			suspendActions();
		}
	}

	private class Menu extends JPanel implements Clearable {

		private final Map<Team, JLabel> labelMoney;
		private final DataChangeRegister register;

		private static final long serialVersionUID = 1L;

		Menu() {
			labelMoney = new HashMap<>();
			register = new DataChangeRegister();
			for (Team team : Team.values())
				labelMoney.put(team, new JLabel());

			JButton buttonEndTurn = new JButton("End Turn");
			buttonEndTurn.addActionListener(onActiveActions(e -> endTurn()));
			JButton buttonMainMenu = new JButton("Main Menu");
			buttonMainMenu.addActionListener(onActiveActions(e -> gameFrame.displayMainMenu()));

			setLayout(new GridLayout(0, 1));
			for (JLabel label : labelMoney.values())
				add(label);
			add(buttonEndTurn);
			add(buttonMainMenu);

			for (Team team : Team.values())
				updateMoneyLabel(team, 0);
		}

		void initGame() {
			register.registerListener(game.onMoneyChange, e -> updateMoneyLabel(e.team, e.newAmount));
		}

		@Override
		public void clear() {
			register.unregisterAllListeners(game.onMoneyChange);
		}

		private void updateMoneyLabel(Team team, int money) {
			labelMoney.get(team).setText(team.toString() + ": " + money);
		}

		private ActionListener onActiveActions(ActionListener l) {
			return e -> {
				if (!isActionSuspended())
					l.actionPerformed(e);
			};
		}

	}

	private class ArenaPanel extends JPanel implements Clearable {

		private final Map<Position, TileComp> tiles;
		private final Map<Unit, UnitComp> units;
		private final Map<Building, BuildingComp> buildings;

		private Position.Bitmap passableMap = Position.Bitmap.empty;
		private Position.Bitmap reachableMap = Position.Bitmap.empty;
		private Position.Bitmap attackableMap = Position.Bitmap.empty;
		private Position hovered;
		private final List<Position> movePath;

		private Position mapPos;
		private double mapPosX, mapPosY;

		private final DataChangeRegister register;

		private static final int MapMoveTimerDelay = 10;
		private static final int MapMoveSpeed = 4;
		private static final long serialVersionUID = 1L;

		ArenaPanel() {
			tiles = new HashMap<>();
			units = new IdentityHashMap<>();
			buildings = new IdentityHashMap<>();
			movePath = new ArrayList<>();
			register = new DataChangeRegister();

			mapPos = new Position(0, 0);
			mapPosX = mapPosY = 0;

			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					requestFocusInWindow();
					tileClicked(new Position(displayedXInv(e.getX()) / TILE_SIZE_PIXEL,
							displayedYInv(e.getY()) / TILE_SIZE_PIXEL));
				}
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					int x = displayedXInv(e.getX()) / TILE_SIZE_PIXEL, y = displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
					if (hovered == null || hovered.x != x || hovered.y != y) {
						hovered = new Position(x, y);
						hoveredUpdated();
					}
				}
			});
			addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					Position.Direction dir = keyToDir(e.getKeyCode());
					if (dir != null)
						mapMove(dir);
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
		}

		void mapMove(Position.Direction dir) {
			Position mapPosNew = mapPos.add(dir);
			if (!mapPosNew.isInRect(0, 0, game.getWidth() - DISPLAYED_ARENA_WIDTH,
					game.getWidth() - DISPLAYED_ARENA_HEIGHT))
				return;
			mapPos = mapPosNew;
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

		void hoveredUpdated() {
			if (!isUnitSelected() || isActionSuspended())
				return;
			Unit unit = game.arena.at(selection).getUnit();

			if (attackableMap.contains(hovered)) {
				if (unit.type.weapon == Weapon.LongRange) {
					movePath.clear();
					repaint();
					return;
				}
				Position targetPos = hovered;
				Position last = movePath.isEmpty() ? unit.getPos() : movePath.get(movePath.size() - 1);
				if (targetPos.neighbors().contains(last))
					return;
				movePath.clear();

				List<Position> bestPath = null;
				for (Position p : targetPos.neighbors()) {
					if (!reachableMap.contains(p))
						continue;
					List<Position> path = unit.calcPath(p);
					if (bestPath == null || path.size() < bestPath.size())
						bestPath = path;
				}
				assert bestPath != null;
				movePath.addAll(bestPath);
				repaint();
				return;
			}

			if (passableMap.contains(hovered)) {
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

			register.registerListener(game.onUnitAdd, e -> {
				addUnitComp(e.unit);
				repaint();
			});

			mapPos = new Position(0, 0);
			mapPosX = mapPosY = 0;
		}

		@Override
		public void clear() {
			for (TileComp tile : tiles.values())
				tile.clear();
			for (BuildingComp building : buildings.values())
				building.clear();
			for (UnitComp unit : units.values())
				unit.clear();
			tiles.clear();
			units.clear();
			buildings.clear();

			register.unregisterAllListeners(game.onUnitAdd);
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
					int prevX = displayedX(prev.x * TILE_SIZE_PIXEL), prevY = displayedY(prev.y * TILE_SIZE_PIXEL);
					int pX = displayedX(pos.x * TILE_SIZE_PIXEL), pY = displayedY(pos.y * TILE_SIZE_PIXEL);

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
			return new Dimension(TILE_SIZE_PIXEL * DISPLAYED_ARENA_WIDTH, TILE_SIZE_PIXEL * DISPLAYED_ARENA_HEIGHT);
		}

		private boolean isUnitSelected() {
			return selection != null && game.arena.at(selection).hasUnit();
		}

		private void tileClicked(Position pos) {
			if (game == null || isActionSuspended())
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
				passableMap = unit.getPassableMap();
				reachableMap = unit.getReachableMap();
				attackableMap = unit.getAttackableMap();

			} else if (tile.hasBuilding()) {
				Building building = tile.getBuilding();
				if (building instanceof Building.Factory) {
					Building.Factory factory = (Building.Factory) building;
					FactoryMenu factoryMenu = new FactoryMenu(gameFrame, factory);
					factoryMenu.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosing(WindowEvent e) {
							clearSelection();
							repaint();
						}
					});
					factoryMenu.setVisible(true);
				}

			} else {
				throw new InternalError();
			}
		}

		private static <E> List<E> list(E e, List<? extends E> l) {
			List<E> ll = new ArrayList<>();
			ll.add(e);
			ll.addAll(l);
			return ll;
		}

		private void unitSecondSelection(Position pos) {
			Tile tile = game.arena.at(pos);
			Unit unit = tiles.get(selection).tile().getUnit();
			Unit target;
			if (!tile.hasUnit()) {
				if (reachableMap.contains(pos)) {
					if (!pos.equals(movePath.get(movePath.size() - 1))) {
						movePath.clear();
						movePath.addAll(unit.calcPath(pos));
					}
					debug.println("Move ", unit.getPos(), " ", pos);
					List<Position> animationPath = list(unit.getPos(), movePath);
					List<Position> curreMovePath = new ArrayList<>(movePath);
					units.get(unit).moveAnimation(animationPath, () -> game.move(unit, curreMovePath));
				}

				return;
			} else if (game.isAttackValid(unit, target = tile.getUnit())) {
				debug.println("Attack ", unit.getPos(), " ", pos);

				if (unit.type.weapon == Weapon.CloseRange) {
					Position moveToPos = movePath.isEmpty() ? unit.getPos() : movePath.get(movePath.size() - 1);
					Position targetPos = target.getPos();
					if (!moveToPos.neighbors().contains(targetPos) || !reachableMap.contains(moveToPos)) {
						movePath.clear();
						if (!unit.getPos().neighbors().contains(targetPos)) {
							List<Position> bestPath = null;
							for (Position p : targetPos.neighbors()) {
								if (!reachableMap.contains(p))
									continue;
								List<Position> path = unit.calcPath(p);
								if (bestPath == null || path.size() < bestPath.size())
									bestPath = path;
							}
							assert bestPath != null;
							movePath.addAll(bestPath);
						}
					}
					if (movePath.isEmpty()) {
						game.moveAndAttack(unit, movePath, target);
					} else {
						List<Position> animationPath = list(unit.getPos(), movePath);
						List<Position> curreMovePath = new ArrayList<>(movePath);
						units.get(unit).moveAnimation(animationPath,
								() -> game.moveAndAttack(unit, curreMovePath, target));
					}

				} else if (unit.type.weapon == Weapon.LongRange) {
					game.attackRange(unit, target);

				} else {
					throw new InternalError("Unknown unit weapon type: " + unit.type.weapon);
				}
			}
		}

		private class UnitComp implements Clearable {
			private final Unit unit;
			private final DataChangeRegister register;

			private final int HealthBarWidth = 26;
			private final int HealthBarHeight = 4;
			private final int HealthBarBottomMargin = 3;

			private List<Position> animationMovePath;
			private int animationCursor;
			private static final int animationResolution = 16;
			private static final int animationDelay = 12;

			UnitComp(Unit unit) {
				this.unit = unit;
				register = new DataChangeRegister();

				register.registerListener(unit.onChange, e -> {
					if (unit.isDead()) {
						units.remove(unit);
						checkGameStatus();
					}
				});
			}

			void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;

				double x, y;
				if (isMoveAnimationActive()) {
					int idx = animationCursor / animationResolution;
					double frac = (animationCursor % animationResolution + 1) / (double) animationResolution;
					Position p1 = animationMovePath.get(idx);
					Position p2 = animationMovePath.get(idx + 1);
					x = displayedX((p1.x + (p2.x - p1.x) * frac) * TILE_SIZE_PIXEL);
					y = displayedY((p1.y + (p2.y - p1.y) * frac) * TILE_SIZE_PIXEL);

				} else {
					x = displayedX(unit.getPos().x * TILE_SIZE_PIXEL);
					y = displayedY(unit.getPos().y * TILE_SIZE_PIXEL);
				}

				/* Draw unit */
				Composite oldComp = g2.getComposite();
				if (unit.getTeam() == game.getTurn() && !unit.isActive())
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				int unitImgX = (int) x;
				int unitImgY = (int) y;
				BufferedImage unitImg = images.getImage(Images.Label.valueOf(unit));
				g.drawImage(unitImg, unitImgX, unitImgY, TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, null);
				g2.setComposite(oldComp);

				/* Draw health bar */
				int healthBarX = (int) (x + 0.5 * TILE_SIZE_PIXEL - HealthBarWidth * 0.5);
				int healthBarY = (int) (y + TILE_SIZE_PIXEL - HealthBarHeight - HealthBarBottomMargin);
				g2.setColor(Color.GREEN);
				g2.fillRect(healthBarX + 1, healthBarY,
						(int) ((double) (HealthBarWidth - 1) * unit.getHealth() / unit.type.health), HealthBarHeight);
				g2.setColor(Color.BLACK);
				g2.drawRect(healthBarX, healthBarY, HealthBarWidth, HealthBarHeight);
			}

			@Override
			public void clear() {
				register.unregisterAllListeners(unit.onChange);
			}

			void moveAnimation(List<Position> animationPath, Runnable onFinish) {
				suspendActions();
				UnitComp unitC = units.get(unit);

				assert animationPath.size() >= 2;
				animationMovePath = new ArrayList<>(animationPath);
				animationCursor = 0;

				Timer animationTimer = new Timer(animationDelay, null);
				animationTimer.addActionListener(e -> {
					repaint();
					unitC.advanceMoveAnimation();
					if (!unitC.isMoveAnimationActive()) {
						animationTimer.stop();
						onFinish.run();
						repaint();
						resumeActions();
					}
				});
				animationTimer.setRepeats(true);
				animationTimer.start();
			}

			boolean isMoveAnimationActive() {
				return animationMovePath != null;
			}

			void advanceMoveAnimation() {
				if (animationMovePath == null)
					return;
				assert animationCursor < animationMovePath.size() * animationResolution;
				animationCursor++;
				if (animationCursor == (animationMovePath.size() - 1) * animationResolution) {
					animationMovePath = null;
					animationCursor = 0;
				}
			}

		}

		private class BuildingComp implements Clearable {
			private final Building building;

			BuildingComp(Building building) {
				this.building = building;
			}

			void paintComponent(Graphics g) {
				drawImage(g, Images.Label.valueOf(building), building.getPos());
			}

			@Override
			public void clear() {
			}

		}

		private class TileComp implements Clearable {

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

			@Override
			public void clear() {
			}

		}

		private void drawImage(Graphics g, Images.Label label, Position pos) {
			BufferedImage unitImg = images.getImage(label);
			g.drawImage(unitImg, displayedX(pos.x * TILE_SIZE_PIXEL), displayedY(pos.y * TILE_SIZE_PIXEL),
					TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, this);
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
