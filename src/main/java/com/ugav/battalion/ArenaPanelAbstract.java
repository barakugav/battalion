package com.ugav.battalion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import com.ugav.battalion.core.IBuilding;
import com.ugav.battalion.core.IUnit;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Position.Direction;
import com.ugav.battalion.core.Terrain;

abstract class ArenaPanelAbstract<TerrainCompImpl extends ArenaPanelAbstract.TerrainComp, BuildingCompImpl extends ArenaPanelAbstract.BuildingComp, UnitCompImpl extends ArenaPanelAbstract.UnitComp>
		extends JLayeredPane implements Clearable {

	private int arenaWidth;
	private int arenaHeight;
	private Position mapPos;
	private int mapPosX, mapPosY;
	final EntityLayer<TerrainCompImpl, BuildingCompImpl, UnitCompImpl> entityLayer;

	final TickTaskManager tickTaskManager = new TickTaskManager();

	private final KeyListener keyListener;
	final DataChangeNotifier<DataEvent> onMapMove = new DataChangeNotifier<>();

	static final int TILE_SIZE_PIXEL = 56;
	static final int DISPLAYED_ARENA_WIDTH = Level.MINIMUM_WIDTH;
	static final int DISPLAYED_ARENA_HEIGHT = Level.MINIMUM_WIDTH;

	private static final int MapMoveSpeed = 4;
	private static final long serialVersionUID = 1L;

	ArenaPanelAbstract() {
		mapPos = Position.of(0, 0);
		mapPosX = mapPosY = 0;

		entityLayer = createEntityLayer();
		add(entityLayer, JLayeredPane.DEFAULT_LAYER);
		Dimension entityLayerSize = entityLayer.getPreferredSize();
		entityLayer.setBounds(0, 0, entityLayerSize.width, entityLayerSize.height);

		entityLayer.addKeyListener(keyListener = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				Position.Direction dir = keyToDir(e.getKeyCode());
				if (dir != null)
					mapViewMove(dir);
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

		tickTaskManager.addTask(1000, this::repaint);

		tickTaskManager.addTask(100, () -> {
			double dx = mapPos.x * TILE_SIZE_PIXEL - mapPosX;
			double dy = mapPos.y * TILE_SIZE_PIXEL - mapPosY;
			if (dx == 0 && dy == 0)
				return;
			int speed = MapMoveSpeed;
			double cx = dx / Math.sqrt(dx * dx + dy * dy) * speed;
			double cy = dy / Math.sqrt(dx * dx + dy * dy) * speed;
			mapPosX += Math.abs(cx) >= Math.abs(dx) ? dx : cx;
			mapPosY += Math.abs(cy) >= Math.abs(dy) ? dy : cy;
		});
	}

	EntityLayer<TerrainCompImpl, BuildingCompImpl, UnitCompImpl> createEntityLayer() {
		return new EntityLayer<>(this);
	}

	@Override
	public Dimension getPreferredSize() {
		return entityLayer.getPreferredSize();
	}

	void updateArenaSize(int width, int height) {
		if (!(DISPLAYED_ARENA_WIDTH <= width && width < 100) || !(DISPLAYED_ARENA_HEIGHT <= height && height < 100))
			throw new IllegalArgumentException("illegal arena size: " + width + " " + height);
		this.arenaWidth = width;
		this.arenaHeight = height;
	}

	void mapViewMove(Position.Direction dir) {
		Position mapPosNew = mapPos.add(dir);
		if (!mapPosNew.isInRect(arenaWidth - DISPLAYED_ARENA_WIDTH, arenaHeight - DISPLAYED_ARENA_HEIGHT))
			return;
		onMapMove.notify(new DataEvent(this));
		mapPos = mapPosNew;
	}

	void mapViewSet(Position pos) {
		if (!pos.isInRect(arenaWidth - DISPLAYED_ARENA_WIDTH, arenaHeight - DISPLAYED_ARENA_HEIGHT))
			return;
		onMapMove.notify(new DataEvent(this));
		mapPos = pos;
		mapPosX = (int) (pos.x * TILE_SIZE_PIXEL);
		mapPosY = (int) (pos.y * TILE_SIZE_PIXEL);
	}

	int displayedX(double x) {
		return (int) (x - mapPosX);
	}

	int displayedY(double y) {
		return (int) (y - mapPosY);
	}

	int displayedXInv(int x) {
		return x + mapPosX;
	}

	int displayedYInv(int y) {
		return y + mapPosY;
	}

	@Override
	public void clear() {
		tickTaskManager.stop();

		entityLayer.removeKeyListener(keyListener);

		entityLayer.clear();
	}

	static class EntityLayer<TerrainCompImpl extends ArenaPanelAbstract.TerrainComp, BuildingCompImpl extends ArenaPanelAbstract.BuildingComp, UnitCompImpl extends ArenaPanelAbstract.UnitComp>
			extends JPanel implements Clearable {

		private final ArenaPanelAbstract<TerrainCompImpl, BuildingCompImpl, UnitCompImpl> arena;

		final Map<Position, TerrainCompImpl> terrains = new HashMap<>();
		final Map<IBuilding, BuildingCompImpl> buildings = new IdentityHashMap<>();
		final Map<IUnit, UnitCompImpl> units = new IdentityHashMap<>();

		private Position hovered;

		final DataChangeNotifier<HoverChangeEvent> onHoverChange = new DataChangeNotifier<>();
		final DataChangeNotifier<TileClickEvent> onTileClick = new DataChangeNotifier<>();

		private final MouseListener mouseListener;
		private final MouseMotionListener mouseMotionListener;

		private static final long serialVersionUID = 1L;

		EntityLayer(ArenaPanelAbstract<TerrainCompImpl, BuildingCompImpl, UnitCompImpl> arena) {
			this.arena = Objects.requireNonNull(arena);

			addMouseListener(mouseListener = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					requestFocusInWindow();
					int clickx = EntityLayer.this.arena.displayedXInv(e.getX()) / TILE_SIZE_PIXEL;
					int clicky = EntityLayer.this.arena.displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
					onTileClick.notify(new TileClickEvent(EntityLayer.this.arena, Position.of(clickx, clicky)));
				}
			});
			addMouseMotionListener(mouseMotionListener = new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					int x = EntityLayer.this.arena.displayedXInv(e.getX()) / TILE_SIZE_PIXEL,
							y = EntityLayer.this.arena.displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
					if (hovered == null || hovered.x != x || hovered.y != y) {
						hovered = Position.of(x, y);
						onHoverChange.notify(new HoverChangeEvent(EntityLayer.this.arena, hovered));
					}
				}
			});
			setFocusable(true);
			requestFocusInWindow();
		}

		@Override
		protected void paintComponent(Graphics g) {
			List<ArenaComp> comps = new ArrayList<>(terrains.size() + buildings.size() + units.size());
			comps.addAll(terrains.values());
			comps.addAll(buildings.values());
			comps.addAll(units.values());
			comps.sort((o1, o2) -> {
				Position p1 = o1.pos(), p2 = o2.pos();
				if (p1.y != p2.y)
					return Double.compare(p1.y, p2.y);
				if (p1.x != p2.x)
					return Double.compare(p1.x, p2.x);
				if ((o1 instanceof TerrainComp) ^ (o2 instanceof TerrainComp))
					return o1 instanceof TerrainComp ? -1 : 1;
				if ((o1 instanceof BuildingComp) ^ (o2 instanceof BuildingComp))
					return o1 instanceof BuildingComp ? -1 : 1;
				if ((o1 instanceof UnitComp) ^ (o2 instanceof UnitComp))
					return o1 instanceof UnitComp ? -1 : 1;
				return 0;
			});
			for (ArenaComp comp : comps)
				if (!comp.isPaintDelayed())
					comp.paintComponent(g);
			for (ArenaComp comp : comps)
				if (comp.isPaintDelayed())
					comp.paintComponent(g);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(TILE_SIZE_PIXEL * DISPLAYED_ARENA_WIDTH, TILE_SIZE_PIXEL * DISPLAYED_ARENA_HEIGHT);
		}

		void removeAllArenaComps() {
			for (TerrainCompImpl tile : terrains.values())
				tile.clear();
			terrains.clear();
			for (BuildingCompImpl building : buildings.values())
				building.clear();
			buildings.clear();
			for (UnitCompImpl unit : units.values())
				unit.clear();
			units.clear();
		}

		@Override
		public void clear() {
			removeMouseListener(mouseListener);
			removeMouseMotionListener(mouseMotionListener);

			removeAllArenaComps();
		}

	}

	void drawRelativeToMap(Graphics g, Object obj, Position pos) {
		int x = displayedX(pos.x * TILE_SIZE_PIXEL);
		int y = displayedY(pos.y * TILE_SIZE_PIXEL);
		drawImage(g, obj, x, y);
	}

	void drawImage(Graphics g, Object obj, int x, int y) {
		BufferedImage img = obj instanceof BufferedImage ? (BufferedImage) obj : Images.getImage(obj);
		assert img.getWidth() == TILE_SIZE_PIXEL : "wrong width (" + img.getWidth() + "):" + obj;
		g.drawImage(img, x, y + TILE_SIZE_PIXEL - img.getHeight(), img.getWidth(), img.getHeight(), this);
	}

	abstract Terrain getTerrain(Position pos);

	abstract static class ArenaComp implements Clearable {

		final ArenaPanelAbstract<?, ?, ?> arena;

		ArenaComp(ArenaPanelAbstract<?, ?, ?> arena) {
			this.arena = Objects.requireNonNull(arena);
		}

		abstract void paintComponent(Graphics g);

		boolean isPaintDelayed() {
			return false;
		}

		abstract Position pos();

		int getGasture() {
			return 0;
		}
	}

	static class TerrainComp extends ArenaComp {

		private final Position pos;

		TerrainComp(ArenaPanelAbstract<?, ?, ?> arena, Position pos) {
			super(arena);
			this.pos = Objects.requireNonNull(pos);
		}

		@Override
		Position pos() {
			return pos;
		}

		private boolean inArena(Position p) {
			return p.isInRect(arena.arenaWidth - 1, arena.arenaHeight - 1);
		}

		@Override
		void paintComponent(Graphics g) {
			IntFunction<Pair<Direction, Direction>> quadrantToDirs = quadrant -> {
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
			};
			Predicate<Position> isBridgeHorizontal = bridgePos -> Objects.requireNonNull(
					Terrain.isBridgeVertical(bridgePos, p -> arena.getTerrain(p), arena.arenaWidth, arena.arenaHeight),
					"Can't determine bridge orientation").booleanValue();

			Position pos = pos();
			Terrain terrain = arena.getTerrain(pos);
			if (terrain == Terrain.ClearWater) {
				arena.drawRelativeToMap(g, terrain, pos);
				for (int quadrant = 0; quadrant < 4; quadrant++) {
					Pair<Direction, Direction> dirs = quadrantToDirs.apply(quadrant);
					Position p1 = pos.add(dirs.e1), p2 = pos.add(dirs.e2), p3 = pos.add(dirs.e1).add(dirs.e2);
					Set<Terrain.Category> waters = EnumSet.of(Terrain.Category.Water, Terrain.Category.BridgeLow,
							Terrain.Category.BridgeHigh, Terrain.Category.Shore);
					Predicate<Position> isWater = p -> !inArena(p) || waters.contains(arena.getTerrain(p).category);
					boolean c1 = !isWater.test(p1), c2 = !isWater.test(p2), c3 = !isWater.test(p3);

					if (c1 || c2 || c3) {
						int variant = (c1 ? 1 : 0) + (c2 ? 2 : 0);
						arena.drawRelativeToMap(g, "WaterEdge" + quadrant + variant, pos);

					}
				}

			} else if (terrain == Terrain.Road) {
				String variant = "";
				for (Direction dir : List.of(Direction.XPos, Direction.YNeg, Direction.XNeg, Direction.YPos)) {
					Position p = pos.add(dir);
					Set<Terrain.Category> roads = EnumSet.of(Terrain.Category.Road, Terrain.Category.BridgeLow,
							Terrain.Category.BridgeHigh);
					variant += inArena(p) && roads.contains(arena.getTerrain(p).category) ? "v" : "x";
				}
				arena.drawRelativeToMap(g, "Road_" + variant, pos);

			} else if (EnumSet.of(Terrain.BridgeLow, Terrain.BridgeHigh).contains(terrain)) {
				Set<Direction> ends = EnumSet.noneOf(Direction.class);
				for (Direction dir : EnumSet.of(Direction.XPos, Direction.YNeg, Direction.XNeg, Direction.YPos)) {
					Position p = pos.add(dir);
					Set<Terrain.Category> endCategoties = EnumSet.of(Terrain.Category.Road, Terrain.Category.FlatLand,
							Terrain.Category.RoughLand, Terrain.Category.ExtremeLand, Terrain.Category.Shore);
					if (inArena(p) && endCategoties.contains(arena.getTerrain(p).category))
						ends.add(dir);
				}

				Set<Direction> orientation = isBridgeHorizontal.test(pos) ? EnumSet.of(Direction.XPos, Direction.XNeg)
						: EnumSet.of(Direction.YPos, Direction.YNeg);
				for (Direction dir : orientation) {
					String label = "bridge_" + (terrain == Terrain.BridgeHigh ? "high" : "low");
					label += "_"
							+ Map.of(Direction.XPos, "0", Direction.YNeg, "1", Direction.XNeg, "2", Direction.YPos, "3")
									.get(dir);
					label += ends.contains(dir) ? "x" : "v";
					arena.drawRelativeToMap(g, label, pos);
				}

			} else if (terrain == Terrain.Shore) {
				arena.drawRelativeToMap(g, Terrain.ClearWater, pos);
				Set<Direction> connections = EnumSet.noneOf(Direction.class);

				for (int quadrant = 0; quadrant < 4; quadrant++) {
					Pair<Direction, Direction> dirs = quadrantToDirs.apply(quadrant);
					Position p1 = pos.add(dirs.e1), p2 = pos.add(dirs.e2), p3 = pos.add(dirs.e1).add(dirs.e2);
					Predicate<Position> isWater = p -> (!inArena(p)
							|| EnumSet.of(Terrain.Category.Water, Terrain.Category.Shore, Terrain.Category.BridgeLow,
									Terrain.Category.BridgeHigh).contains(arena.getTerrain(p).category));
					Predicate<Direction> isBridge = dir -> {
						Position p = pos.add(dir);
						if (!inArena(p) || !EnumSet.of(Terrain.Category.BridgeLow, Terrain.Category.BridgeHigh)
								.contains(arena.getTerrain(p).category))
							return false;
						boolean bridgeHorizonal = isBridgeHorizontal.test(p);
						return bridgeHorizonal ^ EnumSet.of(Direction.YPos, Direction.YNeg).contains(dir);
					};
					boolean c1 = !isWater.test(p1), c2 = !isWater.test(p2), c3 = !isWater.test(p3);
					c1 = c1 || isBridge.test(dirs.e1);
					c2 = c2 || isBridge.test(dirs.e2);
					if (c1)
						connections.add(dirs.e1);

					if (!(c1 || c2) && c3) {
						arena.drawRelativeToMap(g, "WaterEdge" + quadrant + 0, pos);
					} else if (c1 || c2) {
						int variant = (c1 ? 1 : 0) + (c2 ? 2 : 0);
						arena.drawRelativeToMap(g, "Shore" + quadrant + variant, pos);
					}
				}
				if (connections.isEmpty()) {
					arena.drawRelativeToMap(g, "Shore03", pos);
					arena.drawRelativeToMap(g, "Shore13", pos);
					arena.drawRelativeToMap(g, "Shore23", pos);
					arena.drawRelativeToMap(g, "Shore33", pos);
				}

			} else {
				arena.drawRelativeToMap(g, terrain, pos);
			}

		}

		void drawImage(Graphics g, Object obj) {
			arena.drawRelativeToMap(g, obj, pos());
		}

		@Override
		public String toString() {
			return "[" + pos() + ", " + arena.getTerrain(pos) + "]";
		}

		@Override
		public void clear() {
		}

	}

	static class BuildingComp extends ArenaComp {

		private final Position pos;
		private final IBuilding building;

		BuildingComp(ArenaPanelAbstract<?, ?, ?> arena, Position pos, IBuilding building) {
			super(arena);
			this.pos = Objects.requireNonNull(pos);
			this.building = Objects.requireNonNull(building);
		}

		IBuilding building() {
			return building;
		}

		@Override
		Position pos() {
			return pos;
		}

		@Override
		void paintComponent(Graphics g) {
			Position pos = pos();
			arena.drawRelativeToMap(g, Images.getBuildingImage(building, getGasture()), pos);
		}

		@Override
		public void clear() {
		}

		@Override
		public String toString() {
			return "[" + pos() + ", " + building() + "]";
		}

	}

	static abstract class UnitComp extends ArenaComp {

		private final IUnit unit;

		UnitComp(ArenaPanelAbstract<?, ?, ?> arena, IUnit unit) {
			super(arena);
			this.unit = Objects.requireNonNull(unit);
		}

		IUnit unit() {
			return unit;
		}

		@Override
		void paintComponent(Graphics g) {
			Position pos = pos();
			arena.drawRelativeToMap(g, getUnitImg(), pos);

			IUnit trasportedUnit = unit.getTransportedUnit();
			if (trasportedUnit != null) {
				BufferedImage imgBig = Images.getImage(trasportedUnit);
				final int miniWidth = 28;
				int height = imgBig.getHeight() * miniWidth / imgBig.getWidth();
				BufferedImage img = Utils
						.bufferedImageFromImage(imgBig.getScaledInstance(miniWidth, height, Image.SCALE_SMOOTH));

				int x = arena.displayedX(pos.x * TILE_SIZE_PIXEL) + 1;
				int y = arena.displayedY(pos.y * TILE_SIZE_PIXEL) + TILE_SIZE_PIXEL - img.getHeight() - 1;
				int w = img.getWidth(), h = img.getHeight();
				g.setColor(Color.LIGHT_GRAY);
				g.fillRect(x, y, w, h);
				g.setColor(Color.BLACK);
				g.drawRect(x, y, w, h);
				g.drawImage(img, x, y, w, h, arena);
			}
		}

		BufferedImage getUnitImg() {
			return Images.getUnitImage(unit, getOrientation(), getGasture());
		}

		Direction getOrientation() {
			return Direction.XPos;
		}

		@Override
		public void clear() {
		}

		@Override
		public String toString() {
			return "[" + pos() + ", " + unit() + "]";
		}

	}

	static class HoverChangeEvent extends DataEvent {

		final Position pos;

		HoverChangeEvent(ArenaPanelAbstract<?, ?, ?> source, Position pos) {
			super(source);
			this.pos = pos;
		}

	}

	static class TileClickEvent extends DataEvent {

		final Position pos;

		TileClickEvent(ArenaPanelAbstract<?, ?, ?> source, Position pos) {
			super(source);
			this.pos = pos;
		}

	}

}
