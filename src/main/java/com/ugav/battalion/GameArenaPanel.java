package com.ugav.battalion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.LongToIntFunction;

import javax.swing.JLayeredPane;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Direction;
import com.ugav.battalion.core.Entity;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ListInt;
import com.ugav.battalion.util.Logger;
import com.ugav.battalion.util.Utils;

public class GameArenaPanel extends
		ArenaPanelAbstract<ArenaPanelAbstract.TerrainComp, GameArenaPanel.EntityLayer.BuildingComp, GameArenaPanel.EntityLayer.UnitComp>
		implements Clearable {

	private final GameWindow window;
	private final Game game;

	private final Logger logger = new Logger(true); // TODO
	private final Event.Register register = new Event.Register();
	final Event.Notifier<EntityClick> onEntityClick = new Event.Notifier<>();
	final Event.Notifier<SelectionChange> onSelectionChange = new Event.Notifier<>();

	private static final long serialVersionUID = 1L;

	GameArenaPanel(GameWindow window) {
		super(window.globals);
		this.window = window;
		this.game = window.game;

		entityLayer().initUI();

		register.register(entityLayer.onTileClick, e -> cellClicked(e.cell));

		register.register(onMapMove, e -> closeMenus());

		register.register(game.beforeUnitMove, e -> {
			ListInt animationPath = new ListInt.Array(e.path.size() + 1);
			animationPath.add(e.unit.getPos());
			animationPath.addAll(e.path);
			animateUnitMove(e.unit, animationPath);
		});
		register.register(game.beforeUnitAttack, e -> animateUnitAttack(e.attacker, e.target.getPos()));

		register.register(animationTask.onAnimationBegin, e -> window.suspendActions());
		register.register(animationTask.onAnimationEnd, e -> window.resumeActions());

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
		tickTaskManager.start();

		entityLayer().reset();

		mapViewSet(Cell.of(0, 0));
	}

	@Override
	public void clear() {
		register.unregisterAll();

		super.clear();
	}

	private boolean isUnitSelected() {
		return selection != SelectionNone && game.getUnit(selection) != null;
	}

	private void cellClicked(int cell) {
		closeMenus();

		if (window.isActionSuspended())
			return;
		if (selection == SelectionNone) {
			trySelect(cell);

		} else if (isUnitSelected()) {
			unitSecondSelection(cell);
		}
	}

	private void trySelect(int cell) {
		Unit unit = game.getUnit(cell);
		Building building = game.getBuilding(cell);
		if (unit != null) {
			onEntityClick.notify(new EntityClick(this, cell, unit));
			if (!unit.isActive())
				return;
			setSelection(cell);
		} else if (building != null) {
			onEntityClick.notify(new EntityClick(this, cell, building));
			if (!building.isActive())
				return;
			setSelection(cell);

			if (building.type.canBuildUnits)
				openFactoryMenu(building);

		} else {
			onEntityClick.notify(new EntityClick(this, cell, game.getTerrain(cell)));
		}
	}

	private void unitSecondSelection(int target) {
		Unit targetUnit = game.getUnit(target);
		Unit selectedUnit = game.getUnit(selection);

		if (selection == target) {
			/* double click */
			openUnitMenu(selectedUnit);

		} else if (!game.arena.isUnitVisible(target, selectedUnit.getTeam())) {
			if (entityLayer().reachableMap.contains(target))
				entityLayer().unitMove(selectedUnit, target);
			clearSelection();

		} else if (game.isAttackValid(selectedUnit, targetUnit)) {
			entityLayer().unitAttack(selectedUnit, targetUnit);
			clearSelection();

		} else {
			clearSelection();
		}
	}

	private UnitMenu unitMenu;

	private void openUnitMenu(Unit unit) {
		closeMenus();
		clearSelection();

		unitMenu = new UnitMenu(window, unit);
		Dimension unitMenuSize = unitMenu.getPreferredSize();

		int unitX = displayedXCell(Cell.x(unit.getPos()));
		int unitY = displayedYCell(Cell.y(unit.getPos()));

		int unitXMiddle = unitX + TILE_SIZE_PIXEL / 2;
		int x = unitXMiddle - unitMenuSize.width / 2;
		x = Math.max(0, Math.min(x, getWidth() - unitMenuSize.width));

		int yOverlap = (int) (TILE_SIZE_PIXEL * 0.25);
		int y = unitY - unitMenuSize.height + yOverlap;
		y = Math.max(0, Math.min(y, getHeight() - unitMenuSize.height));
		if (y + unitMenuSize.height > unitY + yOverlap)
			y = unitY + TILE_SIZE_PIXEL - yOverlap;

		add(unitMenu, JLayeredPane.POPUP_LAYER);
		unitMenu.setBounds(x, y, unitMenuSize.width, unitMenuSize.height);
		revalidate();

		register.register(unitMenu.onActionChosen, e -> closeUnitMenu());
	}

	private void closeUnitMenu() {
		if (unitMenu == null)
			return;
		if (selection == unitMenu.unit.getPos())
			clearSelection();
		register.unregisterAll(unitMenu.onActionChosen);
		unitMenu.clear();
		remove(unitMenu);
		unitMenu = null;
	}

	private FactoryMenu factoryMenu;

	private void openFactoryMenu(Building factory) {
		closeMenus();
		clearSelection();

		factoryMenu = new FactoryMenu(window, factory);
		factoryMenu.setPreferredSize(getSize());
		Dimension factoryMenuSize = factoryMenu.getPreferredSize();

		register.register(factoryMenu.onActionChosen, e -> closeFactoryMenu());

		add(factoryMenu, JLayeredPane.POPUP_LAYER);
		int x = (getWidth() - factoryMenuSize.width) / 2;
		int y = (getHeight() - factoryMenuSize.height) / 2;
		factoryMenu.setBounds(x, y, factoryMenuSize.width, factoryMenuSize.height);
		revalidate();
	}

	private void closeFactoryMenu() {
		if (factoryMenu == null)
			return;
		if (selection == factoryMenu.factory.getPos())
			clearSelection();
		register.unregisterAll(factoryMenu.onActionChosen);
		factoryMenu.clear();
		remove(factoryMenu);
		factoryMenu = null;
	}

	void closeMenus() {
		closeUnitMenu();
		closeFactoryMenu();
	}

	private int selection = SelectionNone;
	private static final int SelectionNone = Cell.of(-1, -1);

	private void clearSelection() {
		setSelection(SelectionNone);
	}

	private void setSelection(int newSelection) {
//		logger.dbgln("setSelection ", Cell.toString(newSelection));
		selection = newSelection;
		onSelectionChange.notify(new SelectionChange(this, newSelection, getSelectedEntity()));
	}

	Entity getSelectedEntity() {
		if (selection == SelectionNone)
			return null;
		Unit unit = game.getUnit(selection);
		if (unit != null)
			return unit;
		Building building = game.getBuilding(selection);
		if (building != null)
			return building;
		throw new IllegalStateException();
	}

	static class SelectionChange extends Event {

		final int cell;
		final Entity obj;

		public SelectionChange(Object source, int cell, Entity obj) {
			super(source);
			this.cell = cell;
			this.obj = obj;
		}

	}

	static class EntityClick extends Event {

		final int cell;
		final Object obj;

		public EntityClick(Object source, int cell, Object obj) {
			super(source);
			this.cell = cell;
			this.obj = obj;
		}

	}

	private void animateUnitAdd(Unit unit) {
		EntityLayer.UnitComp comp = (EntityLayer.UnitComp) entityLayer().comps.get(unit);
		Animation animation;
		if (!unit.type.invisible)
			animation = new Animation.UnitAppear(comp);
		else
			animation = new Animation.UnitAppearDisappear(comp);
		animation = makeSureAnimationIsVisible(animation, ListInt.of(unit.getPos()));
		runAnimationAndWait(animation);
	}

	private void animateUnitMove(Unit unit, ListInt path) {
		EntityLayer.UnitComp comp = (EntityLayer.UnitComp) entityLayer().comps.get(unit);
		Animation animation = new Animation.UnitMove(comp, path);
		animation = appearDisappearAnimationWrap(comp, animation);
		animation = makeSureAnimationIsVisible(animation, path);
		runAnimationAndWait(animation);
	}

	private void animateUnitAttack(Unit unit, int target) {
		EntityLayer.UnitComp comp = (EntityLayer.UnitComp) entityLayer().comps.get(unit);
		Animation animation = new Animation.Attack(this, comp, target);
		animation = appearDisappearAnimationWrap(comp, animation);
		animation = makeSureAnimationIsVisible(animation, ListInt.of(unit.getPos(), target));
		runAnimationAndWait(animation);
	}

	private static Animation appearDisappearAnimationWrap(EntityLayer.UnitComp unitComp, Animation animation) {
		if (unitComp.unit().type.invisible)
			animation = Animation.of(new Animation.UnitAppearDisappear(unitComp), animation);
		return animation;
	}

	private Animation makeSureAnimationIsVisible(Animation animation, ListInt cells) {
		if (cells.isEmpty())
			throw new IllegalArgumentException();
		int p0 = cells.get(0);
		int xmin = Cell.x(p0), xmax = Cell.x(p0);
		int ymin = Cell.y(p0), ymax = Cell.y(p0);
		for (Iter.Int it = cells.iterator(); it.hasNext();) {
			int cell = it.next();
			xmin = Math.min(xmin, Cell.x(cell));
			xmax = Math.max(xmax, Cell.x(cell));
			ymin = Math.min(ymin, Cell.y(cell));
			ymax = Math.max(ymax, Cell.y(cell));
		}
		int topLeft = Cell.of(xmin, ymin);
		int bottomRight = Cell.of(xmax, ymax);
		boolean topLeftVisible = Cell.isInRect(topLeft, mapPos.x, mapPos.y, mapPos.x + DISPLAYED_ARENA_WIDTH,
				mapPos.y + DISPLAYED_ARENA_HEIGHT);
		boolean bottomRightVisible = Cell.isInRect(bottomRight, mapPos.x, mapPos.y, mapPos.x + DISPLAYED_ARENA_WIDTH,
				mapPos.y + DISPLAYED_ARENA_HEIGHT);
		if (topLeftVisible && bottomRightVisible)
			return animation; /* already visible */

		if (xmin > xmax + DISPLAYED_ARENA_WIDTH || ymin > ymax + DISPLAYED_ARENA_HEIGHT)
			throw new IllegalArgumentException("can't display rect [" + topLeft + ", " + bottomRight + "]");
		int x = xmin + (xmax - xmin) / 2 - DISPLAYED_ARENA_WIDTH / 2;
		x = Math.max(0, Math.min(game.width() - DISPLAYED_ARENA_WIDTH, x));
		int y = ymin + (ymax - ymin) / 2 - DISPLAYED_ARENA_HEIGHT / 2;
		y = Math.max(0, Math.min(game.height() - DISPLAYED_ARENA_WIDTH, y));

		for (Iter.Int it = cells.iterator(); it.hasNext();) {
			int cell = it.next();
			if (!Cell.isInRect(cell, x, y, x + DISPLAYED_ARENA_WIDTH - 1, y + DISPLAYED_ARENA_HEIGHT - 1))
				throw new IllegalStateException();
		}

		return Animation.of(mapMoveAnimation.createAnimation(Position.of(x, y)), animation);
	}

	class EntityLayer extends
			ArenaPanelAbstract.EntityLayer<ArenaPanelAbstract.TerrainComp, EntityLayer.BuildingComp, EntityLayer.UnitComp> {

		private static final long serialVersionUID = 1L;

		private Cell.Bitmap passableMap = Cell.Bitmap.empty();
		private Cell.Bitmap reachableMap = Cell.Bitmap.empty();
		private Cell.Bitmap attackableMap = Cell.Bitmap.empty();
		private final ListInt movePath;

		private final GestureTask gestureTask = new GestureTask();

		private final Event.Register register = new Event.Register();

		EntityLayer() {
			super(GameArenaPanel.this);

			movePath = new ListInt.Array();
		}

		void initUI() {
			register.register(game.onUnitAdd, e -> {
				Utils.swingRun(() -> addUnitComp(e.unit));
				animateUnitAdd(e.unit);
			});
			register.register(game.onUnitRemove, e -> {
				if (e.unit.isDead()) {
					UnitComp unitComp = (UnitComp) comps.get(e.unit);
					Animation animation = new Animation.UnitDeath(GameArenaPanel.this, unitComp);
					animation = makeSureAnimationIsVisible(animation, ListInt.of(e.unit.getPos()));
					runAnimationAndWait(animation);
				}
				Utils.swingRun(() -> {
					if (e.unit.getPos() == selection)
						clearSelection();
					UnitComp unitComp = (UnitComp) comps.remove(e.unit);
					if (unitComp != null)
						unitComp.clear();
				});
			});
			register.register(game.arena.onConquer, e -> {
				UnitComp unitComp = (UnitComp) comps.get(e.conquerer);
				Animation animation = new Animation.Conquer(unitComp);
				animation = makeSureAnimationIsVisible(animation, ListInt.of(e.conquerer.getPos()));
				runAnimationAndWait(animation);
			});
			register.register(onSelectionChange, e -> {
				passableMap = Cell.Bitmap.empty();
				reachableMap = Cell.Bitmap.empty();
				attackableMap = Cell.Bitmap.empty();
				movePath.clear();
				if (e.obj instanceof Unit) {
					Unit unit = (Unit) e.obj;
					passableMap = unit.getPassableMap();
					reachableMap = unit.getReachableMap();
					attackableMap = unit.getAttackableMap();
				}
			});
			register.register(game.onTurnEnd, Utils.swingListener(e -> clearSelection()));

			register.register(onHoverChange, e -> hoveredUpdated(e.cell));

			tickTaskManager.addTask(100, gestureTask);
		}

		@Override
		public void clear() {
			register.unregisterAll();

			super.clear();
		}

		void hoveredUpdated(int hovered) {
			if (!isUnitSelected() || window.isActionSuspended())
				return;

			if (attackableMap.contains(hovered)) {
				updateAttackMovePath(hovered);

			} else if (passableMap.contains(hovered)) {
				updateMovePath(hovered);
			}
		}

		private void updateAttackMovePath(int targetPos) {
			Unit attacker = game.getUnit(selection);
			switch (attacker.type.weapon.type) {
			case LongRange:
				movePath.clear();
				break;

			case CloseRange:
				int last = movePath.isEmpty() ? attacker.getPos() : movePath.last();
				if (Cell.areNeighbors(targetPos, last)
						&& (!game.arena.isUnitVisible(last, game.getTurn()) || game.getUnit(last) == attacker))
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

		private void updateMovePath(int targetPos) {
			Unit unit = game.getUnit(selection);

			/* Update move path from unit position to hovered position */
			int last = movePath.isEmpty() ? unit.getPos() : movePath.last();

			if (movePath.contains(targetPos)) {
				/* Already contained in path, remove all unnecessary steps */
				while (!movePath.isEmpty()) {
					if (movePath.last() == targetPos)
						break;
					movePath.removeIndex(movePath.size() - 1);
				}

			} else if (movePath.size() < unit.type.moveLimit && Cell.areNeighbors(last, targetPos)) {
				if (targetPos == unit.getPos())
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

			if (selection != SelectionNone) {
				drawRelativeToMap(g, Images.Label.Selection, selection);

				for (Iter.Int it = passableMap.cells(); it.hasNext();)
					drawRelativeToMap(g, Images.Label.Passable, it.next());
				for (Iter.Int it = attackableMap.cells(); it.hasNext();)
					drawRelativeToMap(g, Images.Label.Attackable, it.next());
			}
			if (isUnitSelected()) {
				if (movePath.isEmpty()) {
					drawRelativeToMap(g, "MovePathSourceNone", selection);
				} else {
					LongToIntFunction calcDir = idx0 -> {
						int idx = (int) idx0;
						int p1 = idx >= 0 ? movePath.get(idx) : selection;
						int p2 = movePath.get(idx + 1);
						Direction dir = Cell.diffDir(p1, p2);
						switch (dir) {
						case XPos:
							return 0;
						case YNeg:
							return 1;
						case XNeg:
							return 2;
						case YPos:
							return 3;
						default:
							throw new IllegalArgumentException("Unexpected value: " + dir);
						}
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

					int dest = movePath.last();
					int destDir = (calcDir.applyAsInt(movePath.size() - 2) + 2) % 4;
					String destLabel = "MovePathDest" + (reachableMap.contains(dest) ? "" : "Unstand");
					drawRelativeToMap(g, destLabel + destDir, dest);
				}
			}
		}

		private void unitMove(Unit unit, int destination) {
			if (destination != movePath.get(movePath.size() - 1)) {
				movePath.clear();
				movePath.addAll(unit.calcPath(destination));
			}
			ListInt path = new ListInt.Array(movePath);
			logger.dbgln("Move ", Cell.toString(unit.getPos()), " ", Cell.toString(destination));
			window.gameAction(() -> game.move(unit, path));
		}

		private void unitAttack(Unit attacker, Unit target) {
			logger.dbgln("Attack ", Cell.toString(attacker.getPos()), " ", Cell.toString(target.getPos()));
			switch (attacker.type.weapon.type) {
			case CloseRange:
				int moveTarget = movePath.isEmpty() ? attacker.getPos() : movePath.last();
				int targetPos = target.getPos();

				if (!Cell.areNeighbors(moveTarget, targetPos) || !reachableMap.contains(moveTarget)) {
					movePath.clear();
					if (!Cell.areNeighbors(attacker.getPos(), targetPos)) {
						movePath.addAll(Objects.requireNonNull(attacker.calcPathForAttack(targetPos)));
					}
				}
				ListInt path = new ListInt.Array(movePath);
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

			for (Iter.Int it = game.arena.cells(); it.hasNext();) {
				int cell = it.next();
				TerrainComp terrainComp = new TerrainComp(GameArenaPanel.this, cell);
				comps.put("Terrain " + cell, terrainComp);

				Unit unit = game.getUnit(cell);
				if (unit != null)
					addUnitComp(unit);

				Building building = game.getBuilding(cell);
				if (building != null)
					comps.put(building, new BuildingComp(cell, building));

			}
		}

		private UnitComp addUnitComp(Unit unit) {
			UnitComp comp = new UnitComp(unit);
			comps.put(unit, comp);
			return comp;
		}

		class BuildingComp extends ArenaPanelAbstract.BuildingComp {

			BuildingComp(int pos, Building building) {
				super(GameArenaPanel.this, pos, building);
			}

			@Override
			Building building() {
				return (Building) super.building();
			}

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);

				/* Draw flag glow */
				Team conquerTeam = building().getConquerTeam();
				if (conquerTeam != null) {
					BufferedImage flagImg = Images.getFlagGlowImg(conquerTeam, getFlagGesture());
					int x = arena.displayedXCell(pos().xInt()) + 39;
					int y = arena.displayedYCell(pos().yInt()) - 6;
					g.drawImage(flagImg, x, y, arena);
				}
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

		class UnitComp extends ArenaPanelAbstract.UnitComp {

			volatile boolean isAnimated;
			float alpha = 0.0f;
			float baseAlphaMax = 1.0f;
			private final Event.Register register = new Event.Register();

			private static final Color HealthColorHigh = new Color(0, 206, 0);
			private static final Color HealthColorMed = new Color(255, 130, 4);
			private static final Color HealthColorLow = new Color(242, 0, 0);

			UnitComp(Unit unit) {
				super(GameArenaPanel.this, unit.getPos(), unit);

				register.register(unit.onChange(), e -> pos = Position.fromCell(unit.getPos()));
			}

			@Override
			Unit unit() {
				return (Unit) super.unit();
			}

			@Override
			int getGasture() {
				if (unit().getTeam() == game.getTurn() && !unit().isActive())
					return 0;
				int gestureNum = isMoving ? Images.getGestureNumUnitMove(unit().type)
						: Images.getGestureNumUnitStand(unit().type);
				return gestureTask.getGesture() % gestureNum;
			}

			@Override
			public void paintComponent(Graphics g) {
				final Team playerTeam = Team.Red;
				if (!isAnimated && !game.arena.isUnitVisible(unit().getPos(), playerTeam))
					return;

				/* Draw unit */
				super.paintComponent(g);

				/* Draw health bar */
				if (unit().getHealth() != 0 && unit().getHealth() != unit().type.health) {
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
					int x = arena.displayedXCell(pos.x) + 32;
					int y = arena.displayedYCell(pos.y) + 42;
					g.drawImage(healthBar, x, y, null);
				}
			}

			@Override
			BufferedImage getUnitImg() {
				BufferedImage img = super.getUnitImg();

				if (unit().getTeam() == game.getTurn() && !unit().isActive())
					img = Utils.imgDarken(img, .7f);

				float alpha0 = Math.max(calcBaseAlpha(), alpha);
				if (alpha0 != 1.0)
					img = Utils.imgTransparent(img, alpha0);

				return img;
			}

			float calcBaseAlpha() {
				final Team playerTeam = Team.Red;

				float baseAlpha;
				if (unit().isDead())
					baseAlpha = 0f;
				else if (!unit().type.invisible)
					baseAlpha = 1f;
				else if (unit().getTeam() == playerTeam || game.arena.isUnitVisible(unit().getPos(), playerTeam))
					baseAlpha = .5f;
				else
					baseAlpha = 0f;
				return Math.min(baseAlpha, baseAlphaMax);
			}

			@Override
			public int getZOrder() {
				return isAnimated ? 100 : 0;
			}

			@Override
			public void clear() {
				register.unregisterAll();
				super.clear();
			}

		}

	}

	@Override
	Terrain getTerrain(int cell) {
		return game.getTerrain(cell);
	}

}
