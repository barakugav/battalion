package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;

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
	private MenuHandle openMenu;
	private final List<Popup> openPopups = new ArrayList<>();

	private Selection selection = Selection.None;
	private Entity selectedEntity;
	private final ListInt movePath = new ListInt.Array();

	private static enum Selection {
		None, UnitMoveOrAttack, UnitEctActions, UnitObserve, FactoryBuild
	}

	final Event.Notifier<EntityClick> onEntityClick = new Event.Notifier<>();
	final Event.Notifier<SelectionChange> onSelectionChange = new Event.Notifier<>();

	private static final long serialVersionUID = 1L;

	ArenaPanelGame(GameWindow window) {
		super(window.game, window.globals);
		this.window = window;

		entityLayer().initUI();

		register.register(entityLayer.onTileClick, e -> cellClicked(e.cell));
		register.register(game.beforeTurnEnd, Utils.swingListener(e -> clearSelection()));
		register.register(game.onUnitRemove, Utils.swingListener(e -> {
			if (e.unit == selectedEntity)
				clearSelection();
		}));

		Holder<Integer> playerLastPos = new Holder<>();
		register.register(game.beforeTurnEnd, Utils.swingListener(e -> {
			final Team player = Team.Red;
			if (game.getTurn() == player) {
				Position mapPos = mapMove.getCurrent();
				playerLastPos.val = Integer.valueOf(Cell.of((int) mapPos.x, (int) mapPos.y));
			}
		}));
		register.register(game.onTurnEnd, e -> {
			final Team player = Team.Red;
			if (e.nextTurn == player) {
				Position p = Position.fromCell(playerLastPos.val.intValue());
				animationTask.runAndWait(new Animation.MapMove(this, () -> p));
			}
		});
	}

	@Override
	EntityLayer createEntityLayer() {
		return new EntityLayer();
	}

	@Override
	EntityLayer entityLayer() {
		return (EntityLayer) entityLayer;
	}

	void clearSelection() {
		setSelection(Selection.None, null);
		closeOpenMenu();
	}

	private void setSelection(Selection selectionType, Entity entity) {
//		window.logger.dbgln("setSelection(" + selectionType + ", " + entity + ")");
		selection = selectionType;
		selectedEntity = entity;
		onSelectionChange.notify(new SelectionChange(this, selectionType, entity));
	}

	private void cellClicked(int cell) {
		if (!window.isActionEnabled())
			return;
		closeOpenMenu();
		if (selection == Selection.None) {
			trySelect(cell);

		} else if (selection == Selection.UnitMoveOrAttack) {
			unitSecondSelection(cell);

		} else {
			clearSelection();
		}
	}

	private void trySelect(int cell) {
		Unit unit = game.unit(cell);
		Building building = game.building(cell);
		if (unit != null) {
			onEntityClick.notify(new EntityClick(this, cell, unit));

			if (unit.isActive())
				setSelection(Selection.UnitMoveOrAttack, unit);
			else if (unit.getTeam() != window.game.getTurn())
				setSelection(Selection.UnitObserve, unit);

		} else if (building != null) {
			onEntityClick.notify(new EntityClick(this, cell, building));
			if (building.isActive()) {
				setSelection(Selection.FactoryBuild, building);
				if (building.type.canBuildUnits)
					openFactoryMenu(building);
			}

		} else {
			onEntityClick.notify(new EntityClick(this, cell, game.terrain(cell)));
		}
	}

	private void unitSecondSelection(int target) {
		Unit selectedUnit = (Unit) selectedEntity, targetUnit = game.unit(target);
		if (targetUnit == selectedUnit) {
			/* double click */
			openUnitMenu(selectedUnit);

		} else if (!game.isUnitVisible(target, selectedUnit.getTeam())) {
			if (selectedUnit.getReachableMap().contains(target))
				unitMove(selectedUnit, target);
			clearSelection();

		} else if (game.isAttackValid(selectedUnit, targetUnit)) {
			unitAttack(selectedUnit, targetUnit);
			clearSelection();

		} else {
			clearSelection();
		}
	}

	private void unitMove(Unit unit, int destination) {
		if (movePath.isEmpty() || movePath.last() != destination) {
			movePath.clear();
			movePath.addAll(unit.calcPath(destination));
		}
		window.gameAction(new Action.UnitMove(unit.getPos(), movePath));
	}

	private void unitAttack(Unit attacker, Unit target) {
		switch (attacker.type.weapon.type) {
		case CloseRange:
			int moveTarget = movePath.isEmpty() ? attacker.getPos() : movePath.last();
			int targetPos = target.getPos();

			if (!Cell.areNeighbors(moveTarget, targetPos) || !attacker.getReachableMap().contains(moveTarget)) {
				movePath.clear();
				if (!Cell.areNeighbors(attacker.getPos(), targetPos))
					movePath.addAll(attacker.calcPathForAttack(targetPos));
			}
			window.gameAction(new Action.UnitMoveAndAttack(attacker.getPos(), movePath, target.getPos()));
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

	private void openUnitMenu(Unit unit) {
		closeOpenMenu();

		setSelection(Selection.UnitEctActions, unit);
		GameMenu.UnitMenu menu = new GameMenu.UnitMenu(window, unit);

		Dimension menuSize = menu.getPreferredSize();

		int unitX = displayedXCell(Cell.x(unit.getPos()));
		int unitY = displayedYCell(Cell.y(unit.getPos()));

		int unitXMiddle = unitX + TILE_SIZE_PIXEL / 2;
		double x = unitXMiddle - menuSize.width / 2;
		x = Math.max(0, Math.min(x, getWidth() - menuSize.width));

		int yOverlap = (int) (TILE_SIZE_PIXEL * 0.25);
		double y = unitY - menuSize.height + yOverlap;
		y = Math.max(0, Math.min(y, getHeight() - menuSize.height));
		if (y + menuSize.height > unitY + yOverlap)
			y = unitY + TILE_SIZE_PIXEL - yOverlap;

		Position mapPos = mapMove.getCurrent();
		x = x / TILE_SIZE_PIXEL + mapPos.x;
		y = y / TILE_SIZE_PIXEL + mapPos.y;
		Position menuPos = Position.of(x, y);

		add(menu, JLayeredPane.PALETTE_LAYER);
		revalidate();
		addMapPinnedComp(menu, menuPos);

		addOpenMenu(() -> {
			rempveMapPinnedComp(menu);
			menu.clear();
			remove(menu);
			revalidate();
			if (selection == Selection.UnitEctActions && selectedEntity == unit)
				clearSelection();
		});
	}

	private void openFactoryMenu(Building factory) {
		closeOpenMenu();

		setSelection(Selection.FactoryBuild, factory);
		GameMenu.FactoryMenu menu = new GameMenu.FactoryMenu(window, factory);

		Dimension menuSize = menu.getPreferredSize();
		int x = (getWidth() - menuSize.width) / 2;
		int y = (getHeight() - menuSize.height) / 2;

		add(menu, JLayeredPane.PALETTE_LAYER);
		menu.setBounds(x, y, menuSize.width, menuSize.height);
		revalidate();

		addOpenMenu(() -> {
			menu.clear();
			remove(menu);
			revalidate();
			if (selection == Selection.FactoryBuild && selectedEntity == factory)
				clearSelection();
		});
	}

	private void addOpenMenu(MenuHandle menu) {
		closeOpenMenu();
		openMenu = menu;
	}

	void closeOpenMenu() {
		if (openMenu == null)
			return;
		MenuHandle m = openMenu;
		openMenu = null;
		m.close();
	}

	@FunctionalInterface
	private static interface MenuHandle {
		void close();
	}

	private static class Popup extends JPanel {
		final JPanel popup;
		private static final long serialVersionUID = 1L;

		Popup(JPanel popup) {
			this.popup = Objects.requireNonNull(popup);

			setLayout(new GridBagLayout());
			add(popup, Utils.gbConstraints(1, 1, 1, 1, GridBagConstraints.NONE, 0, 0));
			setOpaque(false);

			/* Dummy listener to block the mouse events reaching the arena layer */
			addMouseListener(new MouseAdapter() {
			});
		}
	}

	void showPopup(JPanel popup, int layer) {
		if (!(0 <= layer && layer < 100))
			throw new IllegalArgumentException();
		window.suspendActions();

		Popup popup0 = new Popup(popup);
		popup0.setPreferredSize(getSize());

		synchronized (openPopups) {
			openPopups.add(popup0);
		}

		add(popup0, Integer.valueOf(JLayeredPane.POPUP_LAYER.intValue() + layer));
		Dimension popupSize = popup0.getPreferredSize();
		int x = (getWidth() - popupSize.width) / 2;
		int y = (getHeight() - popupSize.height) / 2;
		popup0.setBounds(x, y, popupSize.width, popupSize.height);
		revalidate();
	}

	void closePopup(JPanel popup) {
		Popup popup0 = null;
		synchronized (openPopups) {
			for (Iterator<Popup> it = openPopups.iterator(); it.hasNext();) {
				Popup p = it.next();
				if (p.popup == popup) {
					it.remove();
					popup0 = p;
					break;
				}
			}
		}
		if (popup0 == null)
			throw new IllegalStateException();
		closePopup0(popup0);
	}

	private void closePopup0(Popup popup) {
		((Clearable) popup.popup).clear();
		remove(popup);
		window.resumeActions();
	}

	private void closePopupAll() {
		List<Popup> popups;
		synchronized (openPopups) {
			popups = new ArrayList<>(openPopups);
			openPopups.clear();
		}
		for (Popup popup : popups)
			closePopup0(popup);
	}

	@Override
	public void clear() {
		closePopupAll();
		closeOpenMenu();
		super.clear();
	}

	void runMapOverviewAnimation() {
		final Team player = Team.Red;
		Position startingMapPos;
		Iter<Building> capitals = game.buildings(player).filter(b -> b.getType() == Building.Type.Capital);
		Iter<Building> factories = game.buildings(player).filter(b -> b.getType() == Building.Type.Factory);
		if (capitals.hasNext()) {
			/* start at capital */
			startingMapPos = Position.fromCell(capitals.next().getPos());
		} else if (factories.hasNext()) {
			/* start at factory */
			startingMapPos = Position.fromCell(factories.next().getPos());
		} else {
			/* start near a lot of units */
			int bestPos = 0, unitsInBestArena = -1;
			for (Iter.Int it = game.cells(); it.hasNext();) {
				int pos = it.next();

				int unitsInArena = 0;
				final int near = 2;
				for (int dx = -near; dx <= near; dx++) {
					for (int dy = -near; dy <= near; dy++) {
						int n = Cell.of(Cell.x(pos) + dx, Cell.y(pos) + dy);
						if (!game.isValidCell(n))
							continue;
						Unit u = game.unit(n);
						if (u != null && u.getTeam() == player)
							unitsInArena++;
					}
				}
				if (unitsInArena > unitsInBestArena) {
					bestPos = pos;
					unitsInBestArena = unitsInArena;
				}
			}
			if (unitsInBestArena > 0) {
				startingMapPos = Position.fromCell(bestPos);
			} else {
				globals.logger.dbgln("no units, can't determine starting position");
				startingMapPos = Position.of(0, 0);
			}
		}
		animationTask.runAsync(new Animation.MapMove(this, () -> mapMove.calcMapPosCentered(startingMapPos)));
	}

	static class SelectionChange extends Event {

		final Selection selection;
		final Entity entity;

		public SelectionChange(Object source, Selection selection, Entity entity) {
			super(source);
			this.selection = selection;
			this.entity = entity;
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

		private final MouseMotionListener mouseMotionListener;

		final Event.Notifier<HoverChangeEvent> onHoverChange = new Event.Notifier<>();

		EntityLayer() {
			addMouseMotionListener(mouseMotionListener = new MouseAdapter() {

				int hovered = Cell.of(-1, -1);

				@Override
				public void mouseMoved(MouseEvent e) {
					int x = displayedXInv(e.getX()) / TILE_SIZE_PIXEL;
					int y = displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
					int newHovered = Cell.of(x, y);
					if (isInArena(newHovered) && hovered != newHovered) {
						hovered = newHovered;
						onHoverChange.notify(new HoverChangeEvent(ArenaPanelGame.this, hovered));
					}
				}
			});
		}

		@Override
		void initUI() {
			super.initUI();
			register.register(onSelectionChange, e -> movePath.clear());
			register.register(onHoverChange, e -> hoveredUpdated(e.cell));
		}

		@Override
		public void clear() {
			removeMouseMotionListener(mouseMotionListener);
			super.clear();
		}

		void hoveredUpdated(int hovered) {
			if (!window.isActionEnabled() || selection != Selection.UnitMoveOrAttack)
				return;
			Unit unit = (Unit) selectedEntity;

			if (unit.getAttackableMap().contains(hovered)) {
				updateAttackMovePath(unit, hovered);

			} else if (unit.getPassableMap().contains(hovered)) {
				updateMovePath(unit, hovered);
			}
		}

		private void updateAttackMovePath(Unit attacker, int targetPos) {
			switch (attacker.type.weapon.type) {
			case CloseRange:
				int last = movePath.isEmpty() ? attacker.getPos() : movePath.last();
				if (Cell.areNeighbors(targetPos, last)
						&& (!game.isUnitVisible(last, game.getTurn()) || game.unit(last) == attacker))
					break;
				movePath.clear();
				movePath.addAll(attacker.calcPathForAttack(targetPos));
				break;

			case LongRange:
			case None:
				movePath.clear();
				break;

			default:
				throw new IllegalArgumentException("Unexpected value: " + attacker.type.weapon.type);
			}
		}

		private void updateMovePath(Unit unit, int targetPos) {
			/* Update move path from unit position to hovered position */
			int last = movePath.isEmpty() ? unit.getPos() : movePath.last();

			if (movePath.contains(targetPos)) {
				/* Already contained in path, remove all unnecessary steps */
				while (!movePath.isEmpty() && movePath.last() != targetPos)
					movePath.removeLast();

			} else if (movePath.size() < unit.type.moveLimit && Cell.areNeighbors(last, targetPos)) {
				if (targetPos != unit.getPos()) {
					/* Append to the end of the move path */
					movePath.add(targetPos);
				} else {
					movePath.clear();
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

			if (selection != Selection.None)
				drawRelativeToMap(g, Images.Selection, selectedEntity.getPos());

			if (EnumSet.of(Selection.UnitMoveOrAttack, Selection.UnitObserve).contains(selection)) {
				Unit unit = (Unit) selectedEntity;
				Cell.Bitmap passableMap = unit.getPassableMap();
				Cell.Bitmap attackableMap = unit.getAttackableMap();
				Cell.Bitmap potentiallyAttackableMap = unit.getPotentiallyAttackableMap();

				for (Iter.Int it = game.cells(); it.hasNext();) {
					int cell = it.next();
					if (passableMap.contains(cell))
						drawRelativeToMap(g, Images.Passable, cell);
					else if (attackableMap.contains(cell))
						drawRelativeToMap(g, Images.Attackable, cell);
					else if (potentiallyAttackableMap.contains(cell))
						drawRelativeToMap(g, Images.PotentiallyAttackable, cell);
				}
			}

			if (selection == Selection.UnitMoveOrAttack) {
				Unit unit = (Unit) selectedEntity;
				if (movePath.isEmpty()) {
					drawRelativeToMap(g, Images.MovePath.None, selectedEntity.getPos());
				} else {
					IntFunction<Direction> calcDir = idx -> {
						int p1 = idx >= 0 ? movePath.get(idx) : selectedEntity.getPos();
						int p2 = movePath.get(idx + 1);
						return Cell.diffDir(p1, p2);
					};

					drawRelativeToMap(g, Images.MovePath.source(calcDir.apply(-1)), selectedEntity.getPos());

					for (int idx = 0; idx < movePath.size() - 1; idx++) {
						Direction prevDir = calcDir.apply(idx - 1);
						Direction dir = calcDir.apply(idx);
						BufferedImage img;
						if (prevDir == dir) {
							img = dir.isXDir() ? Images.MovePath.Horizontal : Images.MovePath.Vertical;
						} else {
							img = Images.MovePath.turn(prevDir.opposite(), dir);
						}
						drawRelativeToMap(g, img, movePath.get(idx));
					}

					int dest = movePath.last();
					Direction destDir = calcDir.apply(movePath.size() - 2).opposite();
					boolean destCanStand = unit.getReachableMap().contains(dest);
					BufferedImage destImg = destCanStand ? Images.MovePath.destination(destDir)
							: Images.MovePath.destinationNoStand(destDir);
					drawRelativeToMap(g, destImg, dest);
				}
			}
		}
	}

}
