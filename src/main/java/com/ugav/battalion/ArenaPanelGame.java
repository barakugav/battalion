package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Objects;
import java.util.function.LongToIntFunction;

import javax.swing.JLayeredPane;

import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Direction;
import com.ugav.battalion.core.Entity;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ListInt;
import com.ugav.battalion.util.Utils;
import com.ugav.battalion.util.Utils.Holder;

class ArenaPanelGame extends ArenaPanelGameAbstract {

	private final GameWindow window;

	private final Event.Register register = new Event.Register();
	final Event.Notifier<EntityClick> onEntityClick = new Event.Notifier<>();
	final Event.Notifier<SelectionChange> onSelectionChange = new Event.Notifier<>();

	private static final long serialVersionUID = 1L;

	ArenaPanelGame(GameWindow window) {
		super(window.game, window.globals);
		this.window = window;

		entityLayer().initUI();

		register.register(entityLayer.onTileClick, e -> cellClicked(e.cell));

		register.register(mapMoveAnimation.onMapMove, e -> closeMenus());

		Holder<Integer> playerLastPos = new Holder<>();
		register.register(game.beforeTurnEnd, Utils.swingListener(e -> {
			final Team player = Team.Red;
			if (game.getTurn() == player)
				playerLastPos.val = Integer.valueOf(Cell.of((int) mapPos.x, (int) mapPos.y));
		}));
		register.register(game.onTurnEnd, e -> {
			final Team player = Team.Red;
			if (e.nextTurn == player) {
				Position p = Position.fromCell(playerLastPos.val.intValue());
				runAnimationAndWait(mapMoveAnimation.createAnimation(p));
			}
		});

		register.register(animationTask.onAnimationBegin, e -> window.suspendActions());
		register.register(animationTask.onAnimationEnd, e -> window.resumeActions());
	}

	@Override
	EntityLayer createEntityLayer() {
		return new EntityLayer();
	}

	@Override
	EntityLayer entityLayer() {
		return (EntityLayer) entityLayer;
	}

	private boolean isUnitSelected() {
		return selection != SelectionNone && game.unit(selection) != null;
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
		Unit unit = game.unit(cell);
		Building building = game.building(cell);
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
			onEntityClick.notify(new EntityClick(this, cell, game.terrain(cell)));
		}
	}

	private void unitSecondSelection(int target) {
		Unit targetUnit = game.unit(target);
		Unit selectedUnit = game.unit(selection);

		if (selection == target) {
			/* double click */
			openUnitMenu(selectedUnit);

		} else if (!game.isUnitVisible(target, selectedUnit.getTeam())) {
			if (selectedUnit.getReachableMap().contains(target))
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
		Unit unit = game.unit(selection);
		if (unit != null)
			return unit;
		Building building = game.building(selection);
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

	class EntityLayer extends ArenaPanelGameAbstract.EntityLayer {

		private static final long serialVersionUID = 1L;

		private final ListInt movePath;

		private final Event.Register register = new Event.Register();

		EntityLayer() {
			movePath = new ListInt.Array();
		}

		@Override
		void initUI() {
			super.initUI();

			register.register(game.onUnitRemove, Utils.swingListener(e -> {
				if (e.unit.getPos() == selection)
					clearSelection();
			}));
			register.register(onSelectionChange, e -> movePath.clear());
			register.register(game.onTurnEnd, Utils.swingListener(e -> clearSelection()));
			register.register(onHoverChange, e -> hoveredUpdated(e.cell));
		}

		@Override
		public void clear() {
			register.unregisterAll();

			super.clear();
		}

		void hoveredUpdated(int hovered) {
			if (!isUnitSelected() || window.isActionSuspended())
				return;
			Unit unit = (Unit) getSelectedEntity();

			if (unit.getAttackableMap().contains(hovered)) {
				updateAttackMovePath(hovered);

			} else if (unit.getPassableMap().contains(hovered)) {
				updateMovePath(hovered);
			}
		}

		private void updateAttackMovePath(int targetPos) {
			Unit attacker = game.unit(selection);
			switch (attacker.type.weapon.type) {
			case LongRange:
				movePath.clear();
				break;

			case CloseRange:
				int last = movePath.isEmpty() ? attacker.getPos() : movePath.last();
				if (Cell.areNeighbors(targetPos, last)
						&& (!game.isUnitVisible(last, game.getTurn()) || game.unit(last) == attacker))
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
			Unit unit = game.unit(selection);

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

				if (getSelectedEntity() instanceof Unit unit) {
					Cell.Bitmap passableMap = unit.getPassableMap();
					Cell.Bitmap attackableMap = unit.getAttackableMap();
					Cell.Bitmap potentiallyAttackableMap = unit.getPotentiallyAttackableMap();

					for (Iter.Int it = game.cells(); it.hasNext();) {
						int cell = it.next();
						if (passableMap.contains(cell))
							drawRelativeToMap(g, Images.Label.Passable, cell);
						else if (attackableMap.contains(cell))
							drawRelativeToMap(g, Images.Label.Attackable, cell);
						else if (potentiallyAttackableMap.contains(cell))
							drawRelativeToMap(g, Images.Label.PotentiallyAttackable, cell);
					}
				}
			}
			if (getSelectedEntity() instanceof Unit unit) {
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
					String destLabel = "MovePathDest" + (unit.getReachableMap().contains(dest) ? "" : "Unstand");
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
			window.gameAction(new Action.UnitMove(unit.getPos(), path));
		}

		private void unitAttack(Unit attacker, Unit target) {
			switch (attacker.type.weapon.type) {
			case CloseRange:
				int moveTarget = movePath.isEmpty() ? attacker.getPos() : movePath.last();
				int targetPos = target.getPos();

				if (!Cell.areNeighbors(moveTarget, targetPos) || !attacker.getReachableMap().contains(moveTarget)) {
					movePath.clear();
					if (!Cell.areNeighbors(attacker.getPos(), targetPos)) {
						movePath.addAll(Objects.requireNonNull(attacker.calcPathForAttack(targetPos)));
					}
				}
				ListInt path = new ListInt.Array(movePath);
				window.gameAction(new Action.UnitMoveAndAttack(attacker.getPos(), path, target.getPos()));
				break;

			case LongRange:
				window.gameAction(new Action.UnitAttackLongRange(attacker.getPos(), target.getPos()));
				break;

			case None:
				break;

			default:
				throw new IllegalArgumentException("Unexpected value: " + attacker.type.weapon.type);
			}

		}
	}

}
