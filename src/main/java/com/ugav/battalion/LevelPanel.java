package com.ugav.battalion;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.ugav.battalion.Level.UnitDesc;

class LevelPanel extends JPanel implements Clearable {

	private final Globals globals;
	private final Menu menu;
	private final ArenaPanel arenaPanel;

	private final Game game;
	private Position selection;
	private final DebugPrintsManager debug;

	private boolean actionsSuspended;

	private static final long serialVersionUID = 1L;

	LevelPanel(Globals globals, Level level) {
		this.globals = Objects.requireNonNull(globals);

		if (level.getWidth() < ArenaPanel.DISPLAYED_ARENA_WIDTH
				|| level.getHeight() < ArenaPanel.DISPLAYED_ARENA_HEIGHT)
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
			for (Team team : Team.realTeams)
				labelMoney.put(team, new JLabel());

			JButton buttonEndTurn = new JButton("End Turn");
			buttonEndTurn.addActionListener(onActiveActions(e -> endTurn()));
			JButton buttonMainMenu = new JButton("Main Menu");
			buttonMainMenu.addActionListener(onActiveActions(e -> globals.frame.displayMainMenu()));

			setLayout(new GridLayout(0, 1));
			for (JLabel label : labelMoney.values())
				add(label);
			add(buttonEndTurn);
			add(buttonMainMenu);

			for (Team team : Team.realTeams)
				updateMoneyLabel(team, 0);
		}

		void initGame() {
			register.register(game.onMoneyChange, e -> updateMoneyLabel(e.team, e.newAmount));
		}

		@Override
		public void clear() {
			register.unregisterAll();
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

	private class ArenaPanel extends
			AbstractArenaPanel<AbstractArenaPanel.TileComp, AbstractArenaPanel.BuildingComp, ArenaPanel.UnitComp>
			implements Clearable {

		private Position.Bitmap passableMap = Position.Bitmap.empty;
		private Position.Bitmap reachableMap = Position.Bitmap.empty;
		private Position.Bitmap attackableMap = Position.Bitmap.empty;
		private final List<Position> movePath;

		private final DataChangeRegister register;

		private static final long serialVersionUID = 1L;

		ArenaPanel() {
			movePath = new ArrayList<>();
			register = new DataChangeRegister();

			register.register(onTileClick, e -> tileClicked(e.pos));
			register.register(onHoverChange, e -> hoveredUpdated(e.pos));

			updateArenaSize(game.arena.getWidth(), game.arena.getHeight());
		}

		void hoveredUpdated(Position hovered) {
			if (!isUnitSelected() || isActionSuspended())
				return;

			if (attackableMap.contains(hovered)) {
				updateAttackMovePath(hovered);

			} else if (passableMap.contains(hovered)) {
				updateMovePath(hovered);
			}
		}

		private void updateAttackMovePath(Position targetPos) {
			Unit attacker = game.arena.at(selection).getUnit();
			switch (attacker.type.weapon.type) {
			case LongRange:
				movePath.clear();
				break;

			case CloseRange:
				Position last = movePath.isEmpty() ? attacker.getPos() : movePath.get(movePath.size() - 1);
				if (targetPos.neighbors().contains(last) && (!game.arena.isUnitVisible(last, game.getTurn())
						|| game.arena.at(last).getUnit() == attacker))
					break;
				movePath.clear();
				movePath.addAll(Objects.requireNonNull(attacker.calcPathForAttack(targetPos)));
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + attacker.type.weapon.type);
			}
			repaint();
		}

		private void updateMovePath(Position targetPos) {
			Unit unit = game.arena.at(selection).getUnit();

			/* Update move path from unit position to hovered position */
			Position last = movePath.isEmpty() ? unit.getPos() : movePath.get(movePath.size() - 1);

			if (movePath.contains(targetPos)) {
				/* Already contained in path, remove all unnecessary steps */
				while (!movePath.isEmpty()) {
					if (movePath.get(movePath.size() - 1).equals(targetPos))
						break;
					movePath.remove(movePath.size() - 1);
				}

			} else if (movePath.size() < unit.type.moveLimit && last.neighbors().contains(targetPos)) {
				/* Append to the end of the move path */
				movePath.add(targetPos);

			} else {
				/* Unable to append to end of current move path, calculate new route */
				movePath.clear();
				movePath.addAll(unit.calcPath(targetPos));
			}
			repaint();
		}

		void initGame() {
			for (Position pos : game.arena.positions()) {
				TileComp tileComp = new TileComp(this, pos);
				Tile tile = game.arena.at(pos);
				tiles.put(pos, tileComp);
				if (tile.hasUnit())
					addUnitComp(tile.getUnit());

				if (tile.hasBuilding()) {
					Building building = tile.getBuilding();
					buildings.put(building, new BuildingComp(this, pos));
				}
			}

			setPreferredSize(getPreferredSize());

			register.register(game.onUnitAdd, e -> {
				addUnitComp(e.unit);
				repaint();
			});

			mapViewSet(Position.of(0, 0));
		}

		@Override
		public void clear() {
			register.unregisterAll();

			for (TileComp tile : tiles.values())
				tile.clear();
			for (BuildingComp building : buildings.values())
				building.clear();
			for (UnitComp unit : units.values())
				unit.clear();
			tiles.clear();
			units.clear();
			buildings.clear();

			super.clear();
		}

		private void addUnitComp(Unit unit) {
			units.put(unit, new UnitComp(unit));
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

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
			Tile tile = game.arena.at(pos);

			if (tile.hasUnit()) {
				if (!tile.getUnit().isActive())
					return;
				debug.println("Selected unit ", pos);
				selection = pos;
				Unit unit = tile.getUnit();
				passableMap = unit.getPassableMap();
				reachableMap = unit.getReachableMap();
				attackableMap = unit.getAttackableMap();

			} else if (tile.hasBuilding()) {
				if (!tile.getBuilding().isActive())
					return;
				debug.println("Selected building ", pos);
				selection = pos;
				Building building = tile.getBuilding();
				if (building.type.canBuildUnits) {
					FactoryMenu factoryMenu = new FactoryMenu(globals.frame, building);
					factoryMenu.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosing(WindowEvent e) {
							clearSelection();
							repaint();
						}
					});
					factoryMenu.setVisible(true);
				}
			}
		}

		private static <E> List<E> list(E e, List<? extends E> l) {
			List<E> ll = new ArrayList<>();
			ll.add(e);
			ll.addAll(l);
			return ll;
		}

		private void unitMove(Unit unit, Position destination) {
			if (!destination.equals(movePath.get(movePath.size() - 1))) {
				movePath.clear();
				movePath.addAll(unit.calcPath(destination));
			}
			debug.println("Move ", unit.getPos(), " ", destination);
			List<Position> path = game.calcRealPath(unit, movePath);
			List<Position> animationPath = list(unit.getPos(), path);
			units.get(unit).moveAnimation(animationPath, () -> game.move(unit, path));
		}

		private void unitAttack(Unit attacker, Unit target) {
			debug.println("Attack ", attacker.getPos(), " ", target.getPos());
			switch (attacker.type.weapon.type) {
			case CloseRange:
				Position moveTarget = movePath.isEmpty() ? attacker.getPos() : movePath.get(movePath.size() - 1);
				Position targetPos = target.getPos();
				if (!moveTarget.neighbors().contains(targetPos) || !reachableMap.contains(moveTarget)) {
					movePath.clear();
					if (!attacker.getPos().neighbors().contains(targetPos)) {
						movePath.addAll(Objects.requireNonNull(attacker.calcPathForAttack(targetPos)));
					}
				}

				List<Position> path = game.calcRealPath(attacker, movePath);
				List<Position> animationPath = list(attacker.getPos(), path);
				units.get(attacker).moveAnimation(animationPath, () -> game.moveAndAttack(attacker, path, target));
				break;
			case LongRange:
				game.attackRange(attacker, target);
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + attacker.type.weapon.type);
			}

		}

		private void unitSecondSelection(Position target) {
			Tile targetTile = game.arena.at(target);
			Unit selectedUnit = game.arena.at(selection).getUnit();
			if (!game.arena.isUnitVisible(target, selectedUnit.getTeam())) {
				if (reachableMap.contains(target))
					unitMove(selectedUnit, target);

			} else if (game.isAttackValid(selectedUnit, targetTile.getUnit())) {
				unitAttack(selectedUnit, targetTile.getUnit());
			}
		}

		@Override
		Terrain getTerrain(Position pos) {
			return game.arena.at(pos).getTerrain();
		}

		@Override
		Object getBuilding(Position pos) {
			return game.arena.at(pos).getBuilding();
		}

		@Override
		Object getUnit(Position pos) {
			return game.arena.at(pos).getUnit();
		}

		private class UnitComp extends AbstractArenaPanel.UnitComp {
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
				super(ArenaPanel.this, unit.getPos());
				this.unit = unit;
				register = new DataChangeRegister();

				register.register(unit.onChange, e -> {
					if (unit.isDead()) {
						units.remove(unit);
						clear();
						checkGameStatus();
					} else {
						pos = unit.getPos();
					}
				});
			}

			@Override
			void paintComponent(Graphics g) {
				final Team playerTeam = Team.Red;
				if (!isMoveAnimationActive() && !game.arena.isUnitVisible(pos, playerTeam))
					return;
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
				drawImage(g, unit, unitImgX, unitImgY);
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
				register.unregisterAll();
			}

			void moveAnimation(List<Position> animationPath, Runnable onFinish) {
				if (animationPath.size() < 2) {
					onFinish.run();
					return;
				}

				suspendActions();
				UnitComp unitComp = units.get(unit);

				animationMovePath = new ArrayList<>(animationPath);
				animationCursor = 0;

				Timer animationTimer = new Timer(animationDelay, null);
				animationTimer.addActionListener(e -> {
					repaint();
					unitComp.advanceMoveAnimation();
					if (!unitComp.isMoveAnimationActive()) {
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

			@Override
			boolean isPaintDelayed() {
				return isMoveAnimationActive();
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

	}

	private class FactoryMenu extends JDialog {

		private final Building factory;

		private static final long serialVersionUID = 1L;

		FactoryMenu(JFrame parent, Building factory) {
			super(parent);

			if (!factory.type.canBuildUnits)
				throw new IllegalArgumentException(factory.type.toString());
			this.factory = factory;

			initUI();
		}

		private void initUI() {
			setTitle("Factory");

			int unitCount = Unit.Type.values().length;
			setLayout(new GridLayout(1, unitCount));

			Map<Unit.Type, Building.UnitSale> sales = factory.getAvailableUnits();

			List<Unit.Type> landUnits = List.of(Unit.Type.Soldier, Unit.Type.Bazooka, Unit.Type.TankAntiAir,
					Unit.Type.Tank, Unit.Type.Mortar, Unit.Type.Artillery, Unit.Type.TankBig);
			List<Unit.Type> waterUnits = List.of(Unit.Type.SpeedBoat, Unit.Type.ShipAntiAir, Unit.Type.Ship,
					Unit.Type.ShipArtillery, Unit.Type.Submarine);
			List<Unit.Type> airUnits = List.of(Unit.Type.Airplane, Unit.Type.Airplane);
			List<Unit.Type> unitsOrder = new ArrayList<>();
			unitsOrder.addAll(landUnits);
			unitsOrder.addAll(waterUnits);
			unitsOrder.addAll(airUnits);

			for (Unit.Type unit : unitsOrder) {
				JPanel saleComp = new JPanel();
				saleComp.setLayout(new GridBagLayout());
				JComponent upperComp;
				JComponent lowerComp;

				Building.UnitSale unitSale = sales.get(unit);
				if (unitSale != null) {
					upperComp = new JLabel(new ImageIcon(Images.getImage(UnitDesc.of(unit, factory.getTeam()))));
					lowerComp = new JLabel(Integer.toString(unitSale.price));
				} else {
					upperComp = new JLabel(new ImageIcon(Images.getImage(Images.Label.UnitLocked)));
					lowerComp = new JLabel("none");
				}

				saleComp.add(upperComp, Utils.gbConstraints(0, 0, 1, 3));
				saleComp.add(lowerComp, Utils.gbConstraints(0, 3, 1, 1));

				if (unitSale != null) {
					Building.UnitSale sale = unitSale;
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

		private void buyNewUnit(Building.UnitSale sale) {
			if (sale.price > game.getMoney(factory.getTeam()))
				return;
			game.buildUnit(factory, sale.type);
			clearSelection();
			dispose();
		}
	}

}
