package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.Graphics;
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

import javax.swing.JPanel;
import javax.swing.Timer;

import com.ugav.battalion.Position.Direction;

abstract class AbstractArenaPanel<TileCompImpl extends AbstractArenaPanel.TileComp, BuildingCompImpl extends AbstractArenaPanel.BuildingComp, UnitCompImpl extends AbstractArenaPanel.UnitComp>
		extends JPanel implements Clearable {

	final Map<Position, TileCompImpl> tiles;
	final Map<Object, BuildingCompImpl> buildings;
	final Map<Object, UnitCompImpl> units;

	private Position mapPos;
	private double mapPosX, mapPosY;
	private final Timer mapMoveTimer;
	private Position hovered;

	private final MouseListener mouseListener;
	private final MouseMotionListener mouseMotionListener;
	private final KeyListener keyListener;

	final DataChangeNotifier<HoverChangeEvent> onHoverChange;
	final DataChangeNotifier<TileClickEvent> onTileClick;

	static class HoverChangeEvent extends DataEvent {

		final Position pos;

		HoverChangeEvent(AbstractArenaPanel<?, ?, ?> source, Position pos) {
			super(source);
			this.pos = pos;
		}

	}

	static class TileClickEvent extends DataEvent {

		final Position pos;

		TileClickEvent(AbstractArenaPanel<?, ?, ?> source, Position pos) {
			super(source);
			this.pos = pos;
		}

	}

	static final int TILE_SIZE_PIXEL = 56;
	static final int DISPLAYED_ARENA_WIDTH = 8;
	static final int DISPLAYED_ARENA_HEIGHT = 8;

	private static final int MapMoveTimerDelay = 10;
	private static final int MapMoveSpeed = 4;
	private static final long serialVersionUID = 1L;

	AbstractArenaPanel() {
		tiles = new HashMap<>();
		buildings = new IdentityHashMap<>();
		units = new IdentityHashMap<>();

		onHoverChange = new DataChangeNotifier<>();
		onTileClick = new DataChangeNotifier<>();

		mapPos = Position.of(0, 0);
		mapPosX = mapPosY = 0;

		addMouseListener(mouseListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
				int clickx = displayedXInv(e.getX()) / TILE_SIZE_PIXEL;
				int clicky = displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
				onTileClick.notify(new TileClickEvent(AbstractArenaPanel.this, Position.of(clickx, clicky)));
			}
		});
		addMouseMotionListener(mouseMotionListener = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int x = displayedXInv(e.getX()) / TILE_SIZE_PIXEL, y = displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
				if (hovered == null || hovered.x != x || hovered.y != y) {
					hovered = Position.of(x, y);
					onHoverChange.notify(new HoverChangeEvent(AbstractArenaPanel.this, hovered));
				}
			}
		});
		addKeyListener(keyListener = new KeyAdapter() {
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
		setFocusable(true);
		requestFocusInWindow();

		mapMoveTimer = new Timer(MapMoveTimerDelay, e -> {
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

		setPreferredSize(getPreferredSize());
	}

	abstract int getArenaWidth();

	abstract int getArenaHeight();

	void mapViewMove(Position.Direction dir) {
		Position mapPosNew = mapPos.add(dir);
		if (!mapPosNew.isInRect(getArenaWidth() - DISPLAYED_ARENA_WIDTH, getArenaHeight() - DISPLAYED_ARENA_HEIGHT))
			return;
		mapPos = mapPosNew;
		repaint();
	}

	void mapViewSet(Position pos) {
		if (!pos.isInRect(getArenaWidth() - DISPLAYED_ARENA_WIDTH, getArenaHeight() - DISPLAYED_ARENA_HEIGHT))
			return;
		mapPos = pos;
		mapPosX = pos.x;
		mapPosY = pos.y;
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

	@Override
	public void clear() {
		mapMoveTimer.stop();

		removeMouseListener(mouseListener);
		removeMouseMotionListener(mouseMotionListener);
		removeKeyListener(keyListener);

		removeEnteriesComp();
	}

	void removeEnteriesComp() {
		for (TileCompImpl tile : tiles.values())
			tile.clear();
		tiles.clear();
		for (BuildingCompImpl building : buildings.values())
			building.clear();
		buildings.clear();
		for (UnitCompImpl unit : units.values())
			unit.clear();
		units.clear();
	}

	@Override
	protected void paintComponent(Graphics g) {
		List<EntityComp> comps = new ArrayList<>(tiles.size() + buildings.size() + units.size());
		comps.addAll(tiles.values());
		comps.addAll(buildings.values());
		comps.addAll(units.values());
		comps.sort((o1, o2) -> o1.pos.compareTo(o2.pos));
		for (EntityComp comp : comps)
			if (!comp.isPaintDelayed())
				comp.paintComponent(g);
		for (EntityComp comp : comps)
			if (comp.isPaintDelayed())
				comp.paintComponent(g);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(TILE_SIZE_PIXEL * DISPLAYED_ARENA_WIDTH, TILE_SIZE_PIXEL * DISPLAYED_ARENA_HEIGHT);
	}

	void drawImage(Graphics g, Object obj, Position pos) {
		int x = displayedX(pos.x * TILE_SIZE_PIXEL);
		int y = displayedY(pos.y * TILE_SIZE_PIXEL);
		drawImage(g, obj, x, y);
	}

	void drawImage(Graphics g, Object obj, int x, int y) {
		BufferedImage img = Images.getImage(obj);
		assert img.getWidth() == TILE_SIZE_PIXEL;
		g.drawImage(img, x, y + TILE_SIZE_PIXEL - img.getHeight(), img.getWidth(), img.getHeight(), this);
	}

	abstract Terrain getTerrain(Position pos);

	abstract Object getBuilding(Position pos);

	abstract Object getUnit(Position pos);

	abstract static class EntityComp implements Clearable {

		final AbstractArenaPanel<?, ?, ?> arena;
		Position pos;

		EntityComp(AbstractArenaPanel<?, ?, ?> arena, Position pos) {
			this.arena = Objects.requireNonNull(arena);
			this.pos = Objects.requireNonNull(pos);
		}

		abstract void paintComponent(Graphics g);

		boolean isPaintDelayed() {
			return false;
		}
	}

	static class TileComp extends EntityComp {

		TileComp(AbstractArenaPanel<?, ?, ?> arena, Position pos) {
			super(arena, pos);
		}

		private boolean inArena(Position p) {
			return p.isInRect(arena.getArenaWidth() - 1, arena.getArenaHeight() - 1);
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
					throw new InternalError();
				}
			};
			Predicate<Position> isBridgeHorizontal = bridgePos -> Objects
					.requireNonNull(Terrain.isBridgeVertical(bridgePos, p -> arena.getTerrain(p), arena.getArenaWidth(),
							arena.getArenaHeight()), "Can't determine bridge orientation")
					.booleanValue();

			Terrain terrain = arena.getTerrain(pos);
			if (terrain == Terrain.ClearWater) {
				arena.drawImage(g, terrain, pos);
				for (int quadrant = 0; quadrant < 4; quadrant++) {
					Pair<Direction, Direction> dirs = quadrantToDirs.apply(quadrant);
					Position p1 = pos.add(dirs.e1), p2 = pos.add(dirs.e2), p3 = pos.add(dirs.e1).add(dirs.e2);
					Set<Terrain.Category> waters = EnumSet.of(Terrain.Category.Water, Terrain.Category.BridgeLow,
							Terrain.Category.BridgeHigh, Terrain.Category.Shore);
					Predicate<Position> isWater = p -> !inArena(p) || waters.contains(arena.getTerrain(p).category);
					boolean c1 = !isWater.test(p1), c2 = !isWater.test(p2), c3 = !isWater.test(p3);

					if (c1 || c2 || c3) {
						int variant = (c1 ? 1 : 0) + (c2 ? 2 : 0);
						arena.drawImage(g, "WaterEdge" + quadrant + variant, pos);

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
				arena.drawImage(g, "Road_" + variant, pos);

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
					arena.drawImage(g, label, pos);
				}

			} else if (terrain == Terrain.Shore) {
				if (pos.x == 3 && pos.y == 2)
					System.out.println();
				arena.drawImage(g, Terrain.ClearWater, pos);
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
						arena.drawImage(g, "WaterEdge" + quadrant + 0, pos);
					} else if (c1 || c2) {
						int variant = (c1 ? 1 : 0) + (c2 ? 2 : 0);
						arena.drawImage(g, "Shore" + quadrant + variant, pos);
					}
				}
				if (connections.isEmpty()) {
					arena.drawImage(g, "Shore03", pos);
					arena.drawImage(g, "Shore13", pos);
					arena.drawImage(g, "Shore23", pos);
					arena.drawImage(g, "Shore33", pos);
				}

			} else {
				arena.drawImage(g, terrain, pos);
			}

		}

		void drawImage(Graphics g, Object obj) {
			arena.drawImage(g, obj, pos);
		}

		@Override
		public String toString() {
			return pos.toString();
		}

		@Override
		public void clear() {
		}

	}

	static class BuildingComp extends EntityComp {

		BuildingComp(AbstractArenaPanel<?, ?, ?> arena, Position pos) {
			super(arena, pos);
		}

		@Override
		void paintComponent(Graphics g) {
			arena.drawImage(g, arena.getBuilding(pos), pos);
		}

		@Override
		public void clear() {
		}

	}

	static class UnitComp extends EntityComp {

		UnitComp(AbstractArenaPanel<?, ?, ?> arena, Position pos) {
			super(arena, pos);
		}

		@Override
		void paintComponent(Graphics g) {
			arena.drawImage(g, arena.getUnit(pos), pos);
		}

		@Override
		public void clear() {
		}

	}

}
