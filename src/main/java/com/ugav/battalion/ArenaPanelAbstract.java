package com.ugav.battalion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import com.ugav.battalion.Animation.MapMove.Manager.MapPosRange;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Direction;
import com.ugav.battalion.core.IBuilding;
import com.ugav.battalion.core.IUnit;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.Logger;
import com.ugav.battalion.util.Pair;
import com.ugav.battalion.util.Utils;

abstract class ArenaPanelAbstract<TerrainCompImpl extends ArenaPanelAbstract.TerrainComp, BuildingCompImpl extends ArenaPanelAbstract.BuildingComp, UnitCompImpl extends ArenaPanelAbstract.UnitComp>
		extends JLayeredPane implements Clearable {

	private int arenaWidth;
	private int arenaHeight;

	final EntityLayer<TerrainCompImpl, BuildingCompImpl, UnitCompImpl> entityLayer;

	final Animation.MapMove.Manager mapMove = new Animation.MapMove.Manager(this);
	private boolean isMapMoveByUserEnable = true;
	private final MouseAdapter mapMoveMouseListener;
	private final List<Pair<JComponent, Position>> mapPinnedComps = new ArrayList<>();

	final TickTask.Manager tickTaskManager = new TickTask.Manager();
	final Animation.Task animationTask;

	final Globals globals;
	final Event.Register register = new Event.Register();

	static final int TILE_SIZE_PIXEL = 56;

	private static final long serialVersionUID = 1L;

	ArenaPanelAbstract(Globals globals) {
		this.globals = Objects.requireNonNull(globals);

		Logger animationTaskLogger = new Logger.Enabled(globals.logger, () -> globals.debug.logAnimations);
		animationTask = new Animation.Task(animationTaskLogger);
		animationTask.setRunning(true);

		entityLayer = createEntityLayer();

		mapMove.setPos(Position.of(0, 0));

		add(entityLayer, JLayeredPane.DEFAULT_LAYER);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				onResize();
			}
		});
		onResize();

		mapMoveMouseListener = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (!isMapMoveByUserEnable)
					return;
				double x = displayedXInv(e.getX()) / (double) TILE_SIZE_PIXEL;
				double y = displayedYInv(e.getY()) / (double) TILE_SIZE_PIXEL;
				MapPosRange displayedRange = mapMove.getDisplayedRange();
				double width = displayedRange.xmax - displayedRange.xmin;
				double height = displayedRange.ymax - displayedRange.ymin;

				final double nearThreshold = 0.15;
				double nearXmin = Math.abs(x - displayedRange.xmin);
				double nearXmax = Math.abs(x - displayedRange.xmax);
				double nearX = Math.min(nearXmin, nearXmax) / (width * nearThreshold);
				boolean isNearX = nearX <= 1;
				double nearYmin = Math.abs(y - displayedRange.ymin);
				double nearYmax = Math.abs(y - displayedRange.ymax);
				double nearY = Math.min(nearYmin, nearYmax) / (height * nearThreshold);
				boolean isNearY = nearY <= 1;

				if (isNearX || isNearY) {
					final double MaxSpeed = 2;
					double speed = (1 - Math.min(nearX, nearY)) * MaxSpeed;

					double nx = x - width / 2, ny = y - height / 2;
					Position c = mapMove.getCurrent();
					double dx = (nx - c.x), dy = (ny - c.y);
					double l = (dx != 0 || dy != 0) ? Math.sqrt(dx * dx + dy * dy) : 1;
					dx /= l;
					dy /= l;
					mapMove.userMapMove(dx * speed, dy * speed);
				} else {
					mapMove.userMapMoveCancel();
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				mapMove.userMapMoveCancel();
			}
		};
		entityLayer.addMouseListener(mapMoveMouseListener);
		entityLayer.addMouseMotionListener(mapMoveMouseListener);
		register.register(mapMove.onMapMove, e -> {
			for (Pair<JComponent, Position> c : mapPinnedComps)
				updateBoundsRelativeToMapPos(c.e1, c.e2);
		});

		tickTaskManager.addTask(1000, new TickTask() {

			@Override
			public void clear() {
			}

			@Override
			public void onTick() {
				repaint();
			}
		});

		tickTaskManager.addTask(100, mapMove);
		tickTaskManager.addTask(100, animationTask);
	}

	EntityLayer<TerrainCompImpl, BuildingCompImpl, UnitCompImpl> createEntityLayer() {
		return new EntityLayer<>(this);
	}

	@Override
	public Dimension getPreferredSize() {
		return entityLayer.getPreferredSize();
	}

	double displayedArenaWidth() {
		return (double) entityLayer.getWidth() / TILE_SIZE_PIXEL;
	}

	double displayedArenaHeight() {
		return (double) entityLayer.getHeight() / TILE_SIZE_PIXEL;
	}

	private void onResize() {
		entityLayer.setBounds(0, 0, getWidth(), getHeight());
		mapMove.setPos(mapMove.getCurrent());
	}

	void updateArenaSize(int width, int height) {
		this.arenaWidth = width;
		this.arenaHeight = height;
	}

	int arenaWidth() {
		return arenaWidth;
	}

	int arenaHeight() {
		return arenaHeight;
	}

	boolean isInArena(int p) {
		return Cell.isInRect(p, arenaWidth - 1, arenaHeight - 1);
	}

	void setMapMoveByUserEnable(boolean enable) {
		isMapMoveByUserEnable = enable;
	}

	int displayedX(double x) {
		return (int) x - (int) (mapMove.getCurrent().x * TILE_SIZE_PIXEL);
	}

	int displayedY(double y) {
		return (int) y - (int) (mapMove.getCurrent().y * TILE_SIZE_PIXEL);
	}

	int displayedXCell(double x) {
		return displayedX(x * TILE_SIZE_PIXEL);
	}

	int displayedYCell(double y) {
		return displayedY(y * TILE_SIZE_PIXEL);
	}

	int displayedXInv(int x) {
		return x + (int) (mapMove.getCurrent().x * TILE_SIZE_PIXEL);
	}

	int displayedYInv(int y) {
		return y + (int) (mapMove.getCurrent().y * TILE_SIZE_PIXEL);
	}

	Position getCurrentMapOrigin() {
		return mapMove.getCurrent();
	}

	void addMapPinnedComp(JComponent c, Position pos) {
		updateBoundsRelativeToMapPos(c, pos);
		mapPinnedComps.add(Pair.of(c, pos));
	}

	void rempveMapPinnedComp(JComponent c) {
		for (Iterator<Pair<JComponent, Position>> it = mapPinnedComps.iterator(); it.hasNext();) {
			Pair<JComponent, Position> p = it.next();
			if (c.equals(p.e1)) {
				it.remove();
				return;
			}
		}
	}

	private void updateBoundsRelativeToMapPos(JComponent c, Position pos) {
		Dimension size = c.getPreferredSize();
		int x0 = displayedXCell(pos.x), y0 = displayedYCell(pos.y);
		c.setBounds(x0, y0, size.width, size.height);
	}

	@Override
	public void clear() {
		register.unregisterAll();
		tickTaskManager.stop();
		mapMove.clear();
		animationTask.clear();
		removeMouseMotionListener(mapMoveMouseListener);
		removeMouseListener(mapMoveMouseListener);
		entityLayer.clear();
	}

	static class EntityLayer<TerrainCompImpl extends ArenaPanelAbstract.TerrainComp, BuildingCompImpl extends ArenaPanelAbstract.BuildingComp, UnitCompImpl extends ArenaPanelAbstract.UnitComp>
			extends JPanel implements Clearable {

		private final ArenaPanelAbstract<TerrainCompImpl, BuildingCompImpl, UnitCompImpl> arena;
		final Map<Object, ArenaComp> comps = Collections.synchronizedMap(new IdentityHashMap<>());

		final Event.Notifier<TileClickEvent> onTileClick = new Event.Notifier<>();

		private final MouseListener mouseListener;

		private static final long serialVersionUID = 1L;

		EntityLayer(ArenaPanelAbstract<TerrainCompImpl, BuildingCompImpl, UnitCompImpl> arena) {
			this.arena = Objects.requireNonNull(arena);

			addMouseListener(mouseListener = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					requestFocusInWindow();
					int clickx = EntityLayer.this.arena.displayedXInv(e.getX()) / TILE_SIZE_PIXEL;
					int clicky = EntityLayer.this.arena.displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
					int click = Cell.of(clickx, clicky);
					if (arena.isInArena(click))
						onTileClick.notify(new TileClickEvent(EntityLayer.this.arena, click));
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			/* paint cells out of map */
			Position mapPos = arena.mapMove.getCurrent();
			int xmin = (int) Math.floor(mapPos.x);
			int xmax = (int) Math.ceil(mapPos.x + arena.displayedArenaWidth());
			int ymin = (int) Math.floor(mapPos.y);
			int ymax = (int) Math.ceil(mapPos.y + arena.displayedArenaHeight());
			g.setColor(Color.BLACK);
			for (Iter.Int it = Cell.Iter2D.of(xmin, xmax + 1, ymin, ymax + 1); it.hasNext();) {
				int cell = it.next();
				if (arena.isInArena(cell))
					continue;
				int x = arena.displayedXCell(Cell.x(cell));
				int y = arena.displayedYCell(Cell.y(cell));
				g.fillRect(x, y, TILE_SIZE_PIXEL, TILE_SIZE_PIXEL);
			}

			List<ArenaComp> comps = new ArrayList<>(this.comps.values());
			comps.sort((o1, o2) -> {
				int c;
				if ((c = Integer.compare(o1.getZOrder(), o2.getZOrder())) != 0)
					return c;
				Position p1 = o1.pos(), p2 = o2.pos();
				if ((c = Double.compare(p1.y, p2.y)) != 0)
					return c;
				if ((c = Double.compare(p1.x, p2.x)) != 0)
					return c;
				if ((o1 instanceof TerrainComp) ^ (o2 instanceof TerrainComp))
					return o1 instanceof TerrainComp ? -1 : 1;
				if ((o1 instanceof BuildingComp) ^ (o2 instanceof BuildingComp))
					return o1 instanceof BuildingComp ? -1 : 1;
				if ((o1 instanceof UnitComp) ^ (o2 instanceof UnitComp))
					return o1 instanceof UnitComp ? -1 : 1;
				return 0;
			});
			for (ArenaComp comp : comps)
				comp.paintComponent(g);

			if (arena.globals.debug.showGrid) {
				Font font = g.getFont();
				final int fontSize = 9;
				font = new Font(font.getName(), font.getStyle(), fontSize);
				g.setFont(font);
				g.setColor(Color.YELLOW);
				for (Iter.Int it = Cell.Iter2D.of(arena.arenaWidth, arena.arenaHeight); it.hasNext();) {
					int cell = it.next(), x = Cell.x(cell), y = Cell.y(cell);
					int x0 = arena.displayedXCell(x);
					int y0 = arena.displayedYCell(y);
					g.drawRect(x0, y0, TILE_SIZE_PIXEL - 1, TILE_SIZE_PIXEL - 1);
					g.drawString(Cell.toString(cell), x0 + 2, y0 + 2 + fontSize);
				}
			}
			if (arena.globals.debug.showUnitID) {
				Font font = g.getFont();
				int fontSize = 9;
				font = new Font(font.getName(), font.getStyle(), fontSize);
				g.setFont(font);
				g.setColor(Color.MAGENTA);
				for (ArenaComp comp0 : comps) {
					if (!(comp0 instanceof UnitComp))
						continue;
					UnitComp comp = (UnitComp) comp0;
					int id = arena.globals.debug.getUnitID(comp.unit);
					int x = arena.displayedXCell(comp.pos.x) + 2;
					int y = arena.displayedYCell(comp.pos.y) + TILE_SIZE_PIXEL - 2;
					g.drawString("U" + id, x, y);
				}
			}
		}

		void removeAllArenaComps() {
			for (ArenaComp comp : comps.values())
				comp.clear();
			comps.clear();
		}

		@Override
		public void clear() {
			removeMouseListener(mouseListener);
			removeAllArenaComps();
		}

	}

	void drawRelativeToMap(Graphics g, Object obj, int cell) {
		drawRelativeToMap(g, obj, Cell.x(cell), Cell.y(cell));
	}

	void drawRelativeToMap(Graphics g, Object obj, Position pos) {
		drawRelativeToMap(g, obj, pos.x, pos.y);
	}

	void drawRelativeToMap(Graphics g, Object obj, double x, double y) {
		int x0 = displayedX(x * TILE_SIZE_PIXEL);
		int y0 = displayedY(y * TILE_SIZE_PIXEL);
		drawImage(g, obj, x0, y0);
	}

	void drawImage(Graphics g, Object obj, int x, int y) {
		BufferedImage img = obj instanceof BufferedImage ? (BufferedImage) obj : Images.getImg(obj);
		assert img.getWidth() == TILE_SIZE_PIXEL : "wrong width (" + img.getWidth() + "):" + obj;
		g.drawImage(img, x, y + TILE_SIZE_PIXEL - img.getHeight(), this);
	}

	abstract Terrain getTerrain(int cell);

	static interface ArenaComp extends Clearable {

		static final int ZOrderDefault = 0;
		static final int ZOrderAnimated = 100;
		static final int ZOrderAnimation = 200;

		void paintComponent(Graphics g);

		int getZOrder();

		Position pos();
	}

	abstract static class ArenaCompAbstract implements ArenaComp {

		final ArenaPanelAbstract<?, ?, ?> arena;

		ArenaCompAbstract(ArenaPanelAbstract<?, ?, ?> arena) {
			this.arena = Objects.requireNonNull(arena);
		}

		@Override
		public int getZOrder() {
			return ZOrderDefault;
		}

		@Override
		abstract public Position pos();
	}

	static class TerrainComp extends ArenaCompAbstract {

		final int pos;

		TerrainComp(ArenaPanelAbstract<?, ?, ?> arena, int pos) {
			super(arena);
			this.pos = pos;
		}

		@Override
		public void paintComponent(Graphics g) {
			Terrain terrain = arena.getTerrain(pos);
			if (terrain == Terrain.ClearWater) {
				arena.drawRelativeToMap(g, Images.Terrains.get(terrain, getGasture()), pos);
				for (int quadrant = 0; quadrant < 4; quadrant++) {
					Pair<Direction, Direction> dirs = quadrantToDirs(quadrant);
					Direction d1 = dirs.e1, d2 = dirs.e2;
					int p1 = Cell.add(pos, d1), p2 = Cell.add(pos, d2), p3 = Cell.add(pos, d1, d2);
					IntPredicate isWater = p -> !arena.isInArena(p) || arena.getTerrain(p).hasWater();
					boolean c1 = !isWater.test(p1), c2 = !isWater.test(p2), c3 = !isWater.test(p3);
					if (c1 || c2 || c3)
						arena.drawRelativeToMap(g, Images.WaterEdges.get(quadrant, c1, c2, getGasture()), pos);
				}

			} else if (terrain == Terrain.Road) {
				Predicate<Direction> isConnect = dir -> {
					int p = Cell.add(pos, dir);
					return arena.isInArena(p) && arena.getTerrain(p).isRoad();
				};
				boolean xpos = isConnect.test(Direction.XPos);
				boolean yneg = isConnect.test(Direction.YNeg);
				boolean xneg = isConnect.test(Direction.XNeg);
				boolean ypos = isConnect.test(Direction.YPos);
				BufferedImage img = Images.Roads.get(xpos, yneg, xneg, ypos);
				arena.drawRelativeToMap(g, img, pos);

			} else if (terrain.isBridge()) {
				Set<Direction> landConnections = EnumSet.noneOf(Direction.class);
				for (Direction dir : Direction.values()) {
					int p = Cell.add(pos, dir);
					if (arena.isInArena(p) && !arena.getTerrain(p).isWater() && !arena.getTerrain(p).isBridge())
						landConnections.add(dir);
				}

				Set<Direction> orientation = isBridgeHorizontal(pos) ? Direction.xDirs() : Direction.yDirs();
				for (Direction dir : orientation) {
					BufferedImage img = Images.Bridges.get(terrain, dir, landConnections.contains(dir));
					arena.drawRelativeToMap(g, img, pos);
				}

			} else if (terrain == Terrain.Shore) {
				arena.drawRelativeToMap(g, Images.Terrains.get(Terrain.ClearWater, getGasture()), pos);
				Set<Direction> connections = EnumSet.noneOf(Direction.class);

				for (int quadrant = 0; quadrant < 4; quadrant++) {
					Pair<Direction, Direction> dirs = quadrantToDirs(quadrant);
					int p1 = Cell.add(pos, dirs.e1), p2 = Cell.add(pos, dirs.e2), p3 = Cell.add(pos, dirs.e1, dirs.e2);
					IntPredicate isWater = p -> !arena.isInArena(p) || arena.getTerrain(p).hasWater();
					Predicate<Direction> isBridge = dir -> {
						int p = Cell.add(pos, dir);
						if (!arena.isInArena(p) || !arena.getTerrain(p).isBridge())
							return false;
						return isBridgeHorizontal(p) ^ !dir.isXDir();
					};
					boolean c1 = !isWater.test(p1), c2 = !isWater.test(p2), c3 = !isWater.test(p3);
					c1 = c1 || isBridge.test(dirs.e1);
					c2 = c2 || isBridge.test(dirs.e2);
					if (c1)
						connections.add(dirs.e1);

					if (!(c1 || c2) && c3) {
						arena.drawRelativeToMap(g, Images.WaterEdges.get(quadrant, false, false, getGasture()), pos);
					} else if (c1 || c2) {
						arena.drawRelativeToMap(g, Images.Shores.get(quadrant, c1, c2, getGasture()), pos);
					}
				}
				if (connections.isEmpty()) {
					for (int quadrant = 0; quadrant < 4; quadrant++)
						arena.drawRelativeToMap(g, Images.Shores.get(quadrant, true, true, getGasture()), pos);
				}

			} else {
				arena.drawRelativeToMap(g, Images.Terrains.get(terrain, getGasture()), pos);
			}
		}

		private boolean isBridgeHorizontal(int bridgePos) {
			Boolean r = Terrain.isBridgeVertical(bridgePos, p -> arena.getTerrain(p), arena.arenaWidth,
					arena.arenaHeight);
			if (r == null)
				throw new IllegalStateException("Can't determine bridge orientation");
			return r.booleanValue();
		}

		private static Pair<Direction, Direction> quadrantToDirs(int quadrant) {
			switch (quadrant) {
			case 0:
				return Pair.of(Direction.XPos, Direction.YNeg);
			case 1:
				return Pair.of(Direction.YNeg, Direction.XNeg);
			case 2:
				return Pair.of(Direction.XNeg, Direction.YPos);
			case 3:
				return Pair.of(Direction.YPos, Direction.XPos);
			default:
				throw new IllegalArgumentException("Unexpected value: " + quadrant);
			}
		}

		@Override
		public String toString() {
			return "[" + pos + ", " + arena.getTerrain(pos) + "]";
		}

		@Override
		public void clear() {
		}

		@Override
		public Position pos() {
			return Position.fromCell(pos);
		}

		int getGasture() {
			return 0;
		}

	}

	static class BuildingComp extends ArenaCompAbstract {

		private final IBuilding building;
		private final int pos;

		BuildingComp(ArenaPanelAbstract<?, ?, ?> arena, int pos, IBuilding building) {
			super(arena);
			this.building = Objects.requireNonNull(building);
			this.pos = pos;
		}

		IBuilding building() {
			return building;
		}

		@Override
		public void paintComponent(Graphics g) {
			arena.drawRelativeToMap(g, Images.Buildings.get(building, getGasture()), pos);

			/* Draw flag */
			BufferedImage flagImg = Images.Ect.flag(building.getTeam(), getFlagGesture());
			int x = arena.displayedXCell(Cell.x(pos)) + 42;
			int y = arena.displayedYCell(Cell.y(pos)) - 3;
			g.drawImage(flagImg, x, y, arena);
		}

		@Override
		public Position pos() {
			return Position.fromCell(pos);
		}

		int getGasture() {
			return 0;
		}

		int getFlagGesture() {
			return 0;
		}

		@Override
		public void clear() {
		}

		@Override
		public String toString() {
			return "[" + pos + ", " + building() + "]";
		}

	}

	static abstract class UnitComp extends ArenaCompAbstract {

		private final IUnit unit;
		volatile Position pos;
		volatile Direction orientation = Direction.XPos;
		volatile boolean isMoving = false;

		UnitComp(ArenaPanelAbstract<?, ?, ?> arena, int pos, IUnit unit) {
			super(arena);
			this.unit = Objects.requireNonNull(unit);
			this.pos = Position.fromCell(pos);
		}

		IUnit unit() {
			return unit;
		}

		@Override
		public void paintComponent(Graphics g) {
			arena.drawRelativeToMap(g, getUnitImg(), pos);

			IUnit trasportedUnit = unit.getTransportedUnit();
			if (trasportedUnit != null) {
				BufferedImage imgBig = Images.Units.getDefault(trasportedUnit);
				final int miniWidth = 28;
				int height = imgBig.getHeight() * miniWidth / imgBig.getWidth();
				BufferedImage img = Utils
						.bufferedImageFromImage(imgBig.getScaledInstance(miniWidth, height, Image.SCALE_SMOOTH));

				int x = arena.displayedXCell(pos.x) + 1;
				int y = arena.displayedYCell(pos.y) + TILE_SIZE_PIXEL - img.getHeight() - 1;
				int w = img.getWidth(), h = img.getHeight();
				g.setColor(Color.LIGHT_GRAY);
				g.fillRect(x, y, w, h);
				g.setColor(Color.BLACK);
				g.drawRect(x, y, w, h);
				g.drawImage(img, x, y, w, h, arena);
			}
		}

		BufferedImage getUnitImg() {
			if (isMoving)
				return Images.Units.moveImg(unit, orientation, getGasture());
			else
				return Images.Units.standImg(unit, orientation, getGasture());
		}

		@Override
		public Position pos() {
			return pos;
		}

		int getGasture() {
			return 0;
		}

		@Override
		public void clear() {
		}

		@Override
		public String toString() {
			return "[" + pos + ", " + unit() + "]";
		}

	}

	static class HoverChangeEvent extends Event {

		final int cell;

		HoverChangeEvent(ArenaPanelAbstract<?, ?, ?> source, int cell) {
			super(source);
			this.cell = cell;
		}

	}

	static class TileClickEvent extends Event {

		final int cell;

		TileClickEvent(ArenaPanelAbstract<?, ?, ?> source, int cell) {
			super(source);
			this.cell = cell;
		}

	}

}
