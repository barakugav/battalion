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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.LongToIntFunction;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Position.Direction;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Tile;
import com.ugav.battalion.core.Unit;

class LevelGameWindow extends JPanel implements Clearable {

	private final Globals globals;
	private final SideMenu menu;
	private final ArenaPanel arenaPanel;

	private final Game game;
	private final ComputerPlayer computer = new ComputerPlayer.Random();
	private final DebugPrintsManager debug;
	private final DataChangeRegister register = new DataChangeRegister();

	private volatile boolean actionsSuspended;

	private static final long serialVersionUID = 1L;

	LevelGameWindow(Globals globals, Level level) {
		this.globals = Objects.requireNonNull(globals);

		if (level.getWidth() < ArenaPanel.DISPLAYED_ARENA_WIDTH
				|| level.getHeight() < ArenaPanel.DISPLAYED_ARENA_HEIGHT)
			throw new IllegalArgumentException("level size is too small");
		this.game = new GameGUI(level);
		menu = new SideMenu();
		arenaPanel = new ArenaPanel();
		debug = new DebugPrintsManager(true); // TODO

		setLayout(new FlowLayout());
		add(menu);
		add(arenaPanel);

		menu.initGame();
		arenaPanel.initGame();

		register.register(game.onGameEnd(), e -> {
			debug.println("Game finished");
			JOptionPane.showMessageDialog(this, "Winner: " + e.winner);
			suspendActions();
			// TODO
		});

		invalidate();
		repaint();

		(gameActionsThread = new GameActionsThread()).start();

		gameAction(() -> game.start());
	}

	private final GameActionsThread gameActionsThread;

	private class GameActionsThread extends Thread {

		final BlockingQueue<Runnable> actions = new LinkedBlockingQueue<>();
		volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				Runnable action = null;
				try {
					action = actions.poll(100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (action == null)
					continue;

				action.run();
			}
		}

	}

	void gameAction(Runnable action) {
		gameActionsThread.actions.add(action);
	}

	@Override
	public void clear() {
		register.unregisterAll();
		menu.clear();
		arenaPanel.clear();
		gameActionsThread.running = false;
		try {
			gameActionsThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void endTurn() {
		debug.println("End turn");
		gameAction(() -> {
			game.turnEnd();
			assert !game.isFinished();

			computer.playTurn(game);
			game.turnEnd();
		});
	}

	private boolean isActionSuspended() {
		return actionsSuspended;
	}

	private void suspendActions() {
		debug.println("Actions suspended");
		actionsSuspended = true;
	}

	private void resumeActions() {
		debug.println("Actions resumed");
		actionsSuspended = false;
	}

	private class SideMenu extends JPanel implements Clearable {

		private final Map<Team, JLabel> labelMoney;
		private final DataChangeRegister register = new DataChangeRegister();

		private static final long serialVersionUID = 1L;

		SideMenu() {
			labelMoney = new HashMap<>();
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
			register.register(game.onMoneyChange(), e -> updateMoneyLabel(e.team, e.newAmount));
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
			AbstractArenaPanel<AbstractArenaPanel.TileComp, AbstractArenaPanel.BuildingComp, ArenaPanel.EntityLayer.UnitComp>
			implements Clearable {

		private Position selection;
		private UnitMenu unitMenu;

		private final DataChangeRegister register = new DataChangeRegister();

		private static final long serialVersionUID = 1L;

		ArenaPanel() {
			register.register(entityLayer.onTileClick, e -> tileClicked(e.pos));

			register.register(onMapMove, e -> closeUnitMenu());

			updateArenaSize(game.getWidth(), game.getHeight());
		}

		@Override
		EntityLayer createEntityLayer() {
			return new EntityLayer();
		}

		EntityLayer entityLayer() {
			return (EntityLayer) entityLayer;
		}

		void initGame() {
			entityLayer().reset();

			mapViewSet(Position.of(0, 0));
		}

		@Override
		public void clear() {
			register.unregisterAll();

			super.clear();
		}

		private boolean isUnitSelected() {
			return selection != null && game.getTile(selection).hasUnit();
		}

		private void tileClicked(Position pos) {
			closeUnitMenu();

			if (game == null || isActionSuspended())
				return;
			if (selection == null) {
				trySelect(pos);

			} else if (isUnitSelected()) {
				unitSecondSelection(pos);
			}
			repaint();
		}

		private void trySelect(Position pos) {
			Tile tile = game.getTile(pos);

			if (tile.hasUnit()) {
				if (!tile.getUnit().isActive())
					return;
				debug.println("Selected unit ", pos);
				selection = pos;
				Unit unit = tile.getUnit();
				entityLayer().passableMap = unit.getPassableMap();
				entityLayer().reachableMap = unit.getReachableMap();
				entityLayer().attackableMap = unit.getAttackableMap();

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

		private void unitSecondSelection(Position target) {
			Tile targetTile = game.getTile(target);
			Unit selectedUnit = game.getTile(selection).getUnit();

			if (selection.equals(target)) {
				/* double click */
				openUnitMenu(selectedUnit);

			} else if (!game.arena().isUnitVisible(target, selectedUnit.getTeam())) {
				if (entityLayer().reachableMap.contains(target))
					entityLayer().unitMove(selectedUnit, target);
				clearSelection();

			} else if (game.isAttackValid(selectedUnit, targetTile.getUnit())) {
				entityLayer().unitAttack(selectedUnit, targetTile.getUnit());
				clearSelection();

			} else {
				clearSelection();
			}
		}

		void closeUnitMenu() {
			if (unitMenu == null)
				return;
			unitMenu.setVisible(false);
			remove(unitMenu);
			unitMenu = null;
		}

		private void openUnitMenu(Unit unit) {
			closeUnitMenu();
			clearSelection();

			unitMenu = new UnitMenu(unit);
			add(unitMenu, JLayeredPane.POPUP_LAYER);
			Dimension unitMenuSize = unitMenu.getPreferredSize();

			int unitXMiddle = displayedX((unit.getPos().x + 0.5) * TILE_SIZE_PIXEL);
			int x = unitXMiddle - unitMenuSize.width / 2;
			x = Math.max(0, Math.min(x, getWidth() - unitMenuSize.width));

			int unitY = displayedY(unit.getPos().y * TILE_SIZE_PIXEL);
			int yOverlap = (int) (TILE_SIZE_PIXEL * 0.25);
			int y = unitY - unitMenuSize.height + yOverlap;
			y = Math.max(0, Math.min(y, getHeight() - unitMenuSize.height));
			if (y + unitMenuSize.height > unitY + yOverlap)
				y = unitY + TILE_SIZE_PIXEL - yOverlap;

			unitMenu.setBounds(x, y, unitMenuSize.width, unitMenuSize.height);
			revalidate();
		}

		private void clearSelection() {
			if (selection == null)
				return;
			debug.println("clearSelection ", selection);
			selection = null;
			entityLayer().reachableMap = Position.Bitmap.Empty;
			entityLayer().attackableMap = Position.Bitmap.Empty;
			entityLayer().movePath.clear();
			repaint();
		}

		private class EntityLayer extends
				AbstractArenaPanel.EntityLayer<AbstractArenaPanel.TileComp, AbstractArenaPanel.BuildingComp, EntityLayer.UnitComp> {

			private static final long serialVersionUID = 1L;

			private Position.Bitmap passableMap = Position.Bitmap.Empty;
			private Position.Bitmap reachableMap = Position.Bitmap.Empty;
			private Position.Bitmap attackableMap = Position.Bitmap.Empty;
			private final List<Position> movePath;

			private final DataChangeRegister register = new DataChangeRegister();

			EntityLayer() {
				super(ArenaPanel.this);

				movePath = new ArrayList<>();

				register.register(game.onUnitAdd(), e -> {
					addUnitComp(e.unit);
					repaint();
				});
				register.register(game.onUnitRemove(), e -> {
					if (e.unit.getPos().equals(selection))
						clearSelection();
					UnitComp unitComp = units.remove(e.unit);
					if (unitComp != null)
						unitComp.clear();
					repaint();
				});
				register.register(game.arena().onEntityChange, e -> {
					if (e.source instanceof Unit) {
						Unit unit = (Unit) e.source;
						UnitComp unitComp = units.get(unit);
						if (unit.isDead()) {
							units.remove(unit);
							unitComp.clear();
						}
					}
					repaint();
				});
				register.register(game.onTurnEnd(), e -> clearSelection());

				register.register(onHoverChange, e -> hoveredUpdated(e.pos));
				register.register(((GameGUI) game).onBeforeUnitMove, e -> {
					units.get(e.unit).moveAnimation(e.path);
				});
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
				Unit attacker = game.getTile(selection).getUnit();
				switch (attacker.type.weapon.type) {
				case LongRange:
					movePath.clear();
					break;

				case CloseRange:
					Position last = movePath.isEmpty() ? attacker.getPos() : movePath.get(movePath.size() - 1);
					if (targetPos.neighbors().contains(last) && (!game.arena().isUnitVisible(last, game.getTurn())
							|| game.getTile(last).getUnit() == attacker))
						break;
					movePath.clear();
					movePath.addAll(Objects.requireNonNull(attacker.calcPathForAttack(targetPos)));
					break;

				case None:
					movePath.clear();
					break;

				default:
					throw new IllegalArgumentException("Unexpected value: " + attacker.type.weapon.type);
				}
				repaint();
			}

			private void updateMovePath(Position targetPos) {
				Unit unit = game.getTile(selection).getUnit();

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
					if (targetPos.equals(unit.getPos()))
						movePath.clear();
					else {

						/* Append to the end of the move path */
						int index = movePath.indexOf(targetPos);
						if (index == -1)
							movePath.add(targetPos);
						else
							movePath.subList(index, movePath.size()).clear();
					}

				} else {
					/* Unable to append to end of current move path, calculate new route */
					movePath.clear();
					movePath.addAll(unit.calcPath(targetPos));
				}
				repaint();
			}

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				if (selection != null) {
					tiles.get(selection).drawImage(g, Images.Label.Selection);
					for (Position pos : passableMap)
						tiles.get(pos).drawImage(g, Images.Label.Passable);
					for (Position pos : attackableMap)
						tiles.get(pos).drawImage(g, Images.Label.Attackable);

				}
				if (isUnitSelected()) {
					if (movePath.isEmpty()) {
						drawRelativeToMap(g, "MovePathSourceNone", selection);
					} else {
						LongToIntFunction calcDir = idx0 -> {
							int idx = (int) idx0;
							Position p1 = idx >= 0 ? movePath.get(idx) : selection;
							Position p2 = movePath.get(idx + 1);
							if (p1.distNorm1(p2) != 1)
								throw new IllegalStateException();
							if (p1.add(Direction.XPos).equals(p2))
								return 0;
							if (p1.add(Direction.YNeg).equals(p2))
								return 1;
							if (p1.add(Direction.XNeg).equals(p2))
								return 2;
							if (p1.add(Direction.YPos).equals(p2))
								return 3;
							throw new IllegalStateException();
						};

						int sourceDir = calcDir.applyAsInt(-1);
						drawRelativeToMap(g, "MovePathSource" + sourceDir, selection);

						for (int idx = 0; idx < movePath.size() - 1; idx++) {
							int prevDir = calcDir.applyAsInt(idx - 1);
							int dir = calcDir.applyAsInt(idx);
							String label = "MovePath";
							if (prevDir == dir) {
								label += "Straight" + (dir % 2 == 0 ? "Horizontal" : "Vertical");
							} else {
								label += "Turn" + ((dir == (prevDir + 1) % 4) ? dir : Utils.mod(dir - 1, 4));
							}
							drawRelativeToMap(g, label, movePath.get(idx));
						}

						Position dest = movePath.get(movePath.size() - 1);
						int destDir = (calcDir.applyAsInt(movePath.size() - 2) + 2) % 4;
						String destLabel = "MovePathDest" + (reachableMap.contains(dest) ? "" : "Unstand");
						drawRelativeToMap(g, destLabel + destDir, dest);
					}
				}
			}

			private void unitMove(Unit unit, Position destination) {
				if (!destination.equals(movePath.get(movePath.size() - 1))) {
					movePath.clear();
					movePath.addAll(unit.calcPath(destination));
				}
				List<Position> path = game.calcRealPath(unit, movePath);
				debug.println("Move ", unit.getPos(), " ", destination);
				gameAction(() -> game.move(unit, path));
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
					gameAction(() -> game.moveAndAttack(attacker, path, target));
					break;

				case LongRange:
					gameAction(() -> game.attackRange(attacker, target));
					break;

				case None:
					break;

				default:
					throw new IllegalArgumentException("Unexpected value: " + attacker.type.weapon.type);
				}

			}

			void reset() {
				removeAllEntityComps();
				for (Position pos : game.arena().positions()) {
					TileComp tileComp = new TileComp(ArenaPanel.this, pos);
					Tile tile = game.getTile(pos);
					tiles.put(pos, tileComp);
					if (tile.hasUnit())
						addUnitComp(tile.getUnit());

					if (tile.hasBuilding()) {
						Building building = tile.getBuilding();
						buildings.put(building, new BuildingComp(ArenaPanel.this, pos, building));
					}
				}
			}

			private void addUnitComp(Unit unit) {
				units.put(unit, new UnitComp(unit));
			}

			private class UnitComp extends AbstractArenaPanel.UnitComp {

				private final int HealthBarWidth = 26;
				private final int HealthBarHeight = 4;
				private final int HealthBarBottomMargin = 3;

				private List<Position> animationMovePath;
				private int animationCursor;
				private static final int animationResolution = 16;
				private static final int animationFreq = 1000 / 60;

				UnitComp(Unit unit) {
					super(ArenaPanel.this, unit);
				}

				@Override
				Unit unit() {
					return (Unit) super.unit();
				}

				@Override
				Position pos() {
					if (isMoveAnimationActive()) {
						int idx = animationCursor / animationResolution;
						double frac = (animationCursor % animationResolution + 1) / (double) animationResolution;
						Position p1 = animationMovePath.get(idx);
						Position p2 = animationMovePath.get(idx + 1);
						double x = p1.x + (p2.x - p1.x) * frac;
						double y = p1.y + (p2.y - p1.y) * frac;
						return Position.of(x, y);

					} else {
						return unit().getPos();
					}
				}

				@Override
				void paintComponent(Graphics g) {
					Position pos = pos();

					final Team playerTeam = Team.Red;
					if (!isMoveAnimationActive() && !game.arena().isUnitVisible(pos, playerTeam))
						return;
					Graphics2D g2 = (Graphics2D) g;

					/* Draw unit */
					Composite oldComp = g2.getComposite();
					if (unit().getTeam() == game.getTurn() && !unit().isActive())
						g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
					super.paintComponent(g);
					g2.setComposite(oldComp);

					/* Draw health bar */
					int x = displayedX(pos.x * TILE_SIZE_PIXEL), y = displayedY(pos.y * TILE_SIZE_PIXEL);
					int healthBarX = (int) (x + 0.5 * TILE_SIZE_PIXEL - HealthBarWidth * 0.5);
					int healthBarY = y + TILE_SIZE_PIXEL - HealthBarHeight - HealthBarBottomMargin;
					g2.setColor(Color.GREEN);
					g2.fillRect(healthBarX + 1, healthBarY,
							(int) ((double) (HealthBarWidth - 1) * unit().getHealth() / unit().type.health),
							HealthBarHeight);
					g2.setColor(Color.BLACK);
					g2.drawRect(healthBarX, healthBarY, HealthBarWidth, HealthBarHeight);
				}

				void moveAnimation(List<Position> animationPath) {
					if (animationPath.size() < 2)
						return;

					suspendActions();
					UnitComp unitComp = units.get(unit());

					animationMovePath = new ArrayList<>(animationPath);
					animationCursor = 0;

					Timer animationTimer = new Timer(animationFreq, null);
					animationTimer.addActionListener(e -> {
						repaint();
						unitComp.advanceMoveAnimation();
						if (!unitComp.isMoveAnimationActive()) {
							animationTimer.stop();
							repaint();
							resumeActions();
						}
					});
					animationTimer.setRepeats(true);
					animationTimer.start();
					while (animationTimer.isRunning())
						;
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

		private class UnitMenu extends JPanel {

			private static final long serialVersionUID = 1L;

			UnitMenu(Unit unit) {
				super(new GridLayout(1, 0));

				/* Transparent background, draw only buttons */
				setOpaque(false);

				if (!unit.type.transportUnits) {
					boolean transportAirEn = unit.type.category == Unit.Category.Land
							&& Unit.Type.AirTransporter.canStandOn(getTerrain(unit.getPos()));
					createUnitMenuButton(Images.Label.UnitMenuTransportAir, transportAirEn,
							e -> gameAction(() -> game.unitTransport(unit, Unit.Type.AirTransporter)));

					boolean transportWaterEn = unit.type.category == Unit.Category.Land
							&& Unit.Type.ShipTransporter.canStandOn(getTerrain(unit.getPos()));
					createUnitMenuButton(Images.Label.UnitMenuTransportWater, transportWaterEn,
							e -> gameAction(() -> game.unitTransport(unit, Unit.Type.ShipTransporter)));
				} else {
					Unit transportedUnit = unit.getTransportedUnit();

					boolean transportFinishEn = transportedUnit.type.canStandOn(getTerrain(unit.getPos()));
					createUnitMenuButton(Images.Label.UnitMenuTransportFinish, transportFinishEn,
							e -> gameAction(() -> game.transportFinish(unit)));
				}

				boolean repairEn = unit.getHealth() < unit.type.health;
				createUnitMenuButton(Images.Label.UnitMenuRepair, repairEn, e -> System.out.println("UnitMenuRepair"));

				createUnitMenuButton(Images.Label.UnitMenuCancel, true, e -> {
				});
			}

			private void createUnitMenuButton(Images.Label label, boolean enable, ActionListener l) {
				BufferedImage img = Images.getImage(label);
				if (!enable)
					img = Utils.imgTransparent(img, 0.5);
				JButton button = new JButton(new ImageIcon(img));
				button.setBorder(BorderFactory.createEmptyBorder());
				button.setContentAreaFilled(false);
				button.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
				if (enable)
					button.addActionListener(e -> {
						closeUnitMenu();
						l.actionPerformed(e);
					});
				add(button);
			}

		}

		@Override
		Terrain getTerrain(Position pos) {
			return game.getTile(pos).getTerrain();
		}

		@Override
		Object getTrasporterUnit(Object unit) {
			Unit u = ((Unit) unit);
			return u.type.transportUnits ? u.getTransportedUnit() : null;
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
			gameAction(() -> game.buildUnit(factory, sale.type));
			arenaPanel.clearSelection();
			dispose();
		}
	}

}