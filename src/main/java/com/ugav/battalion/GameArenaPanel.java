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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongToIntFunction;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import com.ugav.battalion.FactoryMenu.UnitBuy;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Entity;
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
	private final GameWindow window;
	private final Game game;
	private Position selection;
	private UnitMenu unitMenu;

	private final DebugPrintsManager debug = new DebugPrintsManager(true); // TODO
	private final DataChangeRegister register = new DataChangeRegister();
	final DataChangeNotifier<EntityClick> onEntityClick = new DataChangeNotifier<>();
	final DataChangeNotifier<SelectionChange> onSelectionChange = new DataChangeNotifier<>();

	private static final long serialVersionUID = 1L;

	GameArenaPanel(GameWindow window) {
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
		tickTaskManager.start();

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
			onEntityClick.notify(new EntityClick(this, pos, tile.getUnit()));
			if (!tile.getUnit().isActive())
				return;
			setSelection(pos);

		} else if (tile.hasBuilding()) {
			onEntityClick.notify(new EntityClick(this, pos, tile.getBuilding()));
			if (!tile.getBuilding().isActive())
				return;
			setSelection(pos);

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
		} else {
			onEntityClick.notify(new EntityClick(this, pos, tile.getTerrain()));
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

		Position pos = displayedTile(unit.getPos());

		int unitXMiddle = pos.xInt() + TILE_SIZE_PIXEL / 2;
		int x = unitXMiddle - unitMenuSize.width / 2;
		x = Math.max(0, Math.min(x, getWidth() - unitMenuSize.width));

		int unitY = pos.yInt();
		int yOverlap = (int) (TILE_SIZE_PIXEL * 0.25);
		int y = unitY - unitMenuSize.height + yOverlap;
		y = Math.max(0, Math.min(y, getHeight() - unitMenuSize.height));
		if (y + unitMenuSize.height > unitY + yOverlap)
			y = unitY + TILE_SIZE_PIXEL - yOverlap;

		unitMenu.setBounds(x, y, unitMenuSize.width, unitMenuSize.height);
		revalidate();
	}

	private void clearSelection() {
		setSelection(null);
	}

	private void setSelection(Position newSelection) {
		debug.println("setSelection ", newSelection);
		selection = newSelection;
		onSelectionChange.notify(new SelectionChange(this, newSelection, getSelectedEntity()));
	}

	Entity getSelectedEntity() {
		if (selection == null)
			return null;
		Tile tile = game.arena().at(selection);
		if (tile.hasUnit())
			return tile.getUnit();
		if (tile.hasBuilding())
			return tile.getBuilding();
		throw new IllegalStateException();
	}

	static class SelectionChange extends DataEvent {

		final Position pos;
		final Entity obj;

		public SelectionChange(Object source, Position pos, Entity obj) {
			super(source);
			this.pos = pos;
			this.obj = obj;
		}

	}

	static class EntityClick extends DataEvent {

		final Position pos;
		final Object obj;

		public EntityClick(Object source, Position pos, Object obj) {
			super(source);
			this.pos = pos;
			this.obj = obj;
		}

	}

	void animateUnitMove(Unit unit, List<Position> path, Runnable future) {
		EntityLayer.UnitComp comp = (EntityLayer.UnitComp) entityLayer().comps.get(unit);
		comp.animateMove(path, future);
	}

	void animateUnitMoveAndAttack(Unit unit, List<Position> path, Position target, Runnable future) {
		EntityLayer.UnitComp comp = (EntityLayer.UnitComp) entityLayer().comps.get(unit);
		comp.animateMoveAndAttack(path, target, future);
	}

	void animateUnitAttackRange(Unit unit, Position target, Runnable future) {
		EntityLayer.UnitComp comp = (EntityLayer.UnitComp) entityLayer().comps.get(unit);
		comp.animateAttackRange(target, future);
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
				if (e.unit.isDead()) {
					UnitComp unitComp = (UnitComp) comps.get(e.unit);
					unitComp.runAnimationAsync(new Animation.UnitDeath(GameArenaPanel.this, unitComp), () -> {
						comps.remove(e.unit);
						unitComp.clear();
					});
				} else {
					UnitComp unitComp = (UnitComp) comps.remove(e.unit);
					if (unitComp != null)
						unitComp.clear();
				}
			});
			register.register(onSelectionChange, e -> {
				passableMap = Position.Bitmap.Empty;
				reachableMap = Position.Bitmap.Empty;
				attackableMap = Position.Bitmap.Empty;
				movePath.clear();
				if (e.obj instanceof Unit) {
					Unit unit = (Unit) e.obj;
					passableMap = unit.getPassableMap();
					reachableMap = unit.getReachableMap();
					attackableMap = unit.getAttackableMap();
				}
			});
			register.register(game.onTurnEnd(), e -> clearSelection());

			register.register(onHoverChange, e -> hoveredUpdated(e.pos));

			register.register(animationTask.onAnimationBegin, e -> window.suspendActions());
			register.register(animationTask.onAnimationEnd, e -> window.resumeActions());

			tickTaskManager.addTask(100, animationTask);
			tickTaskManager.addTask(100, gestureTask);
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
				drawRelativeToMap(g, Images.Label.Selection, selection);

				for (Position pos : passableMap)
					drawRelativeToMap(g, Images.Label.Passable, pos);
				for (Position pos : attackableMap)
					drawRelativeToMap(g, Images.Label.Attackable, pos);
			}
			if (isUnitSelected()) {
				if (movePath.isEmpty()) {
					drawRelativeToMap(g, "MovePathSourceNone", selection);
				} else {
					LongToIntFunction calcDir = idx0 -> {
						int idx = (int) idx0;
						Position p1 = idx >= 0 ? movePath.get(idx) : selection;
						Position p2 = movePath.get(idx + 1);
						Direction dir = Direction.calc(p1, p2);
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
				comps.put("Terrain " + pos, tileComp);
				if (tile.hasUnit())
					addUnitComp(tile.getUnit());

				if (tile.hasBuilding()) {
					Building building = tile.getBuilding();
					comps.put(building, new BuildingComp(pos, building));
				}
			}
		}

		private void addUnitComp(Unit unit) {
			comps.put(unit, new UnitComp(unit));
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

		class UnitComp extends ArenaPanelAbstract.UnitComp {

			float alpha = 0.0f;
			private final AtomicInteger animationCount = new AtomicInteger();
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
				if (!isAnimated() && !game.arena().isUnitVisible(pos, playerTeam))
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
					Position pos = arena.displayedTile(this.pos);
					g.drawImage(healthBar, pos.xInt() + 32, pos.yInt() + 42, null);
				}
			}

			private synchronized void runAnimationAndWait(Animation animation, Runnable future) {
				if (!animationCount.compareAndSet(0, 1))
					throw new IllegalStateException();

				animationTask.animateAndWait(animation, () -> {
					if (!animationCount.compareAndSet(1, 0))
						throw new IllegalStateException();
					if (future != null)
						future.run();
				});
			}

			private synchronized void runAnimationAsync(Animation animation, Runnable future) {
				if (!animationCount.compareAndSet(0, 1))
					throw new IllegalStateException();

				animationTask.animate(animation, () -> {
					if (!animationCount.compareAndSet(1, 0))
						throw new IllegalStateException();
					if (future != null)
						future.run();
				});
			}

			private synchronized Animation appearDisappearAnimationWrap(Animation animation) {
				if (unit().type.invisible) {
					Animation pre = new Animation.UnitReappear(this);
					Animation post = new Animation.UnitDisappear(this);
					animation = Animation.of(pre, animation, post);
				}
				return animation;
			}

			synchronized void animateMove(List<Position> animationPath, Runnable future) {
				Animation animation = new Animation.UnitMove(this, animationPath);
				animation = appearDisappearAnimationWrap(animation);
				runAnimationAndWait(animation, future);
			}

			synchronized void animateMoveAndAttack(List<Position> animationPath, Position target, Runnable future) {
				Animation animation = Animation.of(new Animation.UnitMove(this, animationPath),
						new Animation.Attack(arena, this, target));
				animation = appearDisappearAnimationWrap(animation);
				runAnimationAndWait(animation, future);
			}

			synchronized void animateAttackRange(Position target, Runnable future) {
				Animation animation = new Animation.Attack(arena, this, target);
				animation = appearDisappearAnimationWrap(animation);
				runAnimationAndWait(animation, future);
			}

			@Override
			BufferedImage getUnitImg() {
				BufferedImage img = super.getUnitImg();

				if (unit().getTeam() == game.getTurn() && !unit().isActive())
					img = Utils.imgDarken(img, .7f);

				final Team playerTeam = Team.Red;

				float baseAlpha;
				if (unit().isDead())
					baseAlpha = 0f;
				else if (!unit().type.invisible)
					baseAlpha = 1f;
				else if (unit().getTeam() == playerTeam || game.arena().isUnitVisible(unit().getPos(), playerTeam))
					baseAlpha = .5f;
				else
					baseAlpha = 0f;

				float alpha0 = Math.max(baseAlpha, alpha);
				if (alpha0 != 1.0)
					img = Utils.imgTransparent(img, alpha0);

				return img;
			}

			public boolean isAnimated() {
				return animationCount.get() > 0;
			}

			@Override
			public int getZOrder() {
				return isAnimated() ? 100 : 0;
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
			BufferedImage img = Images.getImg(label);
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
