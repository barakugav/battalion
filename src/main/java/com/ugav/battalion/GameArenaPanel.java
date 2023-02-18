package com.ugav.battalion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongToIntFunction;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import com.ugav.battalion.FactoryMenu.UnitBuy;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Position.Direction;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Tile;
import com.ugav.battalion.core.Unit;

public class GameArenaPanel extends
		ArenaPanelAbstract<ArenaPanelAbstract.TerrainComp, GameArenaPanel.EntityLayer.BuildingComp, GameArenaPanel.EntityLayer.UnitComp>
		implements Clearable {

	private final Globals globals;
	private final LevelGameWindow window;
	private final Game game;
	private Position selection;
	private UnitMenu unitMenu;

	private final DebugPrintsManager debug = new DebugPrintsManager(true); // TODO
	private final DataChangeRegister register = new DataChangeRegister();

	private static final long serialVersionUID = 1L;

	GameArenaPanel(LevelGameWindow window) {
		this.window = window;
		this.globals = window.globals;
		this.game = window.game;

		entityLayer().initUI();

		register.register(entityLayer.onTileClick, e -> tileClicked(e.pos));

		register.register(onMapMove, e -> closeUnitMenu());

		updateArenaSize(game.width(), game.height());
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

		if (window.isActionSuspended())
			return;
		if (selection == null) {
			trySelect(pos);

		} else if (isUnitSelected()) {
			unitSecondSelection(pos);
		}
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
				FactoryMenu factoryMenu = new FactoryMenu(globals.frame, game, building);
				DataListener<UnitBuy> unitButListener = e -> {
					window.gameAction(() -> game.buildUnit((Building) e.source, e.sale.type));
					clearSelection();
				};
				register.register(factoryMenu.onUnitBuy, unitButListener);
				factoryMenu.addWindowListener(new WindowAdapter() {

					private void closed() {
						register.unregister(factoryMenu.onUnitBuy, unitButListener);
						clearSelection();
					}

					@Override
					public void windowClosed(WindowEvent e) {
						closed();
					}

					@Override
					public void windowClosing(WindowEvent e) {
						closed();
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
	}

	void animateUnitMove(Unit unit, List<Position> path, Runnable future) {
		entityLayer().units.get(unit).moveAnimation(path, future);
	}

	class EntityLayer extends
			ArenaPanelAbstract.EntityLayer<ArenaPanelAbstract.TerrainComp, EntityLayer.BuildingComp, EntityLayer.UnitComp> {

		private static final long serialVersionUID = 1L;

		private Position.Bitmap passableMap = Position.Bitmap.Empty;
		private Position.Bitmap reachableMap = Position.Bitmap.Empty;
		private Position.Bitmap attackableMap = Position.Bitmap.Empty;
		private final List<Position> movePath;

		private final Animation.Task animationTask = new Animation.Task();
		private final GestureTask gestureTask = new GestureTask();

		private final DataChangeRegister register = new DataChangeRegister();

		EntityLayer() {
			super(GameArenaPanel.this);

			movePath = new ArrayList<>();
		}

		void initUI() {
			register.register(game.onUnitAdd(), e -> addUnitComp(e.unit));
			register.register(game.onUnitRemove(), e -> {
				if (e.unit.getPos().equals(selection))
					clearSelection();
				UnitComp unitComp = units.remove(e.unit);
				if (unitComp != null)
					unitComp.clear();
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
			});
			register.register(game.onTurnEnd(), e -> clearSelection());

			register.register(onHoverChange, e -> hoveredUpdated(e.pos));

			register.register(animationTask.onAnimationBegin, e -> window.suspendActions());
			register.register(animationTask.onAnimationEnd, e -> window.resumeActions());

			tickTaskManager.addTask(100, animationTask);
			tickTaskManager.addTask(100, gestureTask);

			tickTaskManager.start();
		}

		@Override
		public void clear() {
			register.unregisterAll();

			super.clear();
		}

		void hoveredUpdated(Position hovered) {
			if (!isUnitSelected() || window.isActionSuspended())
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
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (selection != null) {
				terrains.get(selection).drawImage(g, Images.Label.Selection);
				for (Position pos : passableMap)
					terrains.get(pos).drawImage(g, Images.Label.Passable);
				for (Position pos : attackableMap)
					terrains.get(pos).drawImage(g, Images.Label.Attackable);

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
			window.gameAction(() -> game.move(unit, path));
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
				window.gameAction(() -> game.moveAndAttack(attacker, path, target));
				break;

			case LongRange:
				window.gameAction(() -> game.attackRange(attacker, target));
				break;

			case None:
				break;

			default:
				throw new IllegalArgumentException("Unexpected value: " + attacker.type.weapon.type);
			}

		}

		void reset() {
			removeAllArenaComps();
			for (Position pos : game.arena().positions()) {
				TerrainComp tileComp = new TerrainComp(GameArenaPanel.this, pos);
				Tile tile = game.getTile(pos);
				terrains.put(pos, tileComp);
				if (tile.hasUnit())
					addUnitComp(tile.getUnit());

				if (tile.hasBuilding()) {
					Building building = tile.getBuilding();
					buildings.put(building, new BuildingComp(pos, building));
				}
			}
		}

		private void addUnitComp(Unit unit) {
			units.put(unit, new UnitComp(unit));
		}

		class BuildingComp extends ArenaPanelAbstract.BuildingComp {

			BuildingComp(Position pos, Building building) {
				super(GameArenaPanel.this, pos, building);
			}

			@Override
			int getGasture() {
				return gestureTask.getGesture() % Images.getGestureNum(building().getType());
			}

			@Override
			int getFlagGesture() {
				return gestureTask.getGesture() % Images.getGestureNum("Flag");
			}

		}

		private static class UnitMoveAnimation implements Animation {

			private final UnitComp comp;
			private final List<Position> path;
			private int cursor;
			private static final int StepSize = 16;

			UnitMoveAnimation(UnitComp comp, List<Position> path) {
				this.comp = comp;
				this.path = Collections.unmodifiableList(new ArrayList<>(path));
			}

			@Override
			public boolean advanceAnimationStep() {
				if (cursor >= path.size() * StepSize)
					throw new NoSuchElementException();

				int idx = cursor / StepSize;
				double frac = (cursor % StepSize + 1) / (double) StepSize;
				Position p1 = path.get(idx);
				Position p2 = path.get(idx + 1);
				comp.orientation = Direction.calc(p1, p2);
				double x = p1.x + (p2.x - p1.x) * frac;
				double y = p1.y + (p2.y - p1.y) * frac;
				comp.pos = Position.of(x, y);

				return ++cursor < (path.size() - 1) * StepSize;
			}
		}

		class UnitComp extends ArenaPanelAbstract.UnitComp {

			private Direction orientation = Direction.XPos;
			private UnitMoveAnimation moveAnimation;
			private final DataChangeRegister register = new DataChangeRegister();

			private static final Color HealthColorHigh = new Color(0, 206, 0);
			private static final Color HealthColorMed = new Color(255, 130, 4);
			private static final Color HealthColorLow = new Color(242, 0, 0);

			UnitComp(Unit unit) {
				super(GameArenaPanel.this, unit.getPos(), unit);

				register.register(unit.onChange(), e -> {
					pos = unit.getPos();
				});
			}

			@Override
			Unit unit() {
				return (Unit) super.unit();
			}

			@Override
			Direction getOrientation() {
				return orientation;
			}

			@Override
			int getGasture() {
				if (unit().getTeam() == game.getTurn() && !unit().isActive())
					return 0;
				int gestureNum = Images.getGestureNum(unit().type);
				if (gestureNum == 5 && !isAnimated())
					return 0;
				return gestureTask.getGesture() % gestureNum;
			}

			@Override
			void paintComponent(Graphics g) {
				final Team playerTeam = Team.Red;
				if (!isAnimated() && !game.arena().isUnitVisible(pos, playerTeam))
					return;

				/* Draw unit */
				super.paintComponent(g);

				/* Draw health bar */
				if (unit().getHealth() != unit().type.health) {
					double health = (double) unit().getHealth() / unit().type.health;

					BufferedImage healthBar = new BufferedImage(17, 9, BufferedImage.TYPE_INT_RGB);
					Graphics2D healthBarG = healthBar.createGraphics();
					healthBarG.setColor(Color.BLACK);
					healthBarG.fillRect(0, 0, 17, 9);
					Color healthColor;
					if (health > 2.0 / 3.0)
						healthColor = HealthColorHigh;
					else if (health > 1.0 / 3.0)
						healthColor = HealthColorMed;
					else
						healthColor = HealthColorLow;
					healthBarG.setColor(healthColor);
					for (int bar = 0; bar < 4; bar++) {
						double barPrec = Math.min(Math.max(0, health - bar * 0.25) * 4, 1);
						int barHeight = (int) (7 * barPrec);
						healthBarG.fillRect(1 + bar * 4, 8 - barHeight, 3, barHeight);
					}
					int x = displayedX(pos.x * TILE_SIZE_PIXEL) + 32;
					int y = displayedY(pos.y * TILE_SIZE_PIXEL) + 42;
					g.drawImage(healthBar, x, y, null);
				}
			}

			void moveAnimation(List<Position> animationPath, Runnable future) {
				if (moveAnimation != null)
					throw new IllegalStateException();
				animationTask.animateAndWait((moveAnimation = new UnitMoveAnimation(this, animationPath)), () -> {
					moveAnimation = null;
					future.run();
				});
			}

			@Override
			BufferedImage getUnitImg() {
				BufferedImage img = super.getUnitImg();
				if (unit().getTeam() == game.getTurn() && !unit().isActive())
					img = Utils.imgDarken(img, .7f);
				if (unit().type.invisible && !isAnimated())
					img = Utils.imgTransparent(img, .5f);
				return img;
			}

			public boolean isAnimated() {
				return moveAnimation != null;
			}

			@Override
			boolean isPaintDelayed() {
				return isAnimated();
			}

			@Override
			public void clear() {
				register.unregisterAll();
				super.clear();
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
						e -> window.gameAction(() -> game.unitTransport(unit, Unit.Type.AirTransporter)));

				boolean transportWaterEn = unit.type.category == Unit.Category.Land
						&& Unit.Type.ShipTransporter.canStandOn(getTerrain(unit.getPos()));
				createUnitMenuButton(Images.Label.UnitMenuTransportWater, transportWaterEn,
						e -> window.gameAction(() -> game.unitTransport(unit, Unit.Type.ShipTransporter)));
			} else {
				Unit transportedUnit = unit.getTransportedUnit();

				boolean transportFinishEn = transportedUnit.type.canStandOn(getTerrain(unit.getPos()));
				createUnitMenuButton(Images.Label.UnitMenuTransportFinish, transportFinishEn,
						e -> window.gameAction(() -> game.transportFinish(unit)));
			}

			boolean repairEn = unit.getHealth() < unit.type.health;
			createUnitMenuButton(Images.Label.UnitMenuRepair, repairEn, e -> System.out.println("UnitMenuRepair"));

			createUnitMenuButton(Images.Label.UnitMenuCancel, true, e -> {
			});
		}

		private void createUnitMenuButton(Images.Label label, boolean enable, ActionListener l) {
			BufferedImage img = Images.getImage(label);
			if (!enable)
				img = Utils.imgTransparent(img, .5f);
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

}
