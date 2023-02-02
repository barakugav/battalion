package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.ugav.battalion.Images.Drawable;
import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

class LevelBuilderWindow extends JPanel implements Clearable {

	private static final long serialVersionUID = 1L;

	private final LevelBuilder builder;
	private final GameFrame gameFrame;
	private final Menu menu;
	private final ArenaPanel arenaPanel;
	private final Images images = new Images();
	private final DebugPrintsManager debug;
	private Object menuSelectedObj;
	private Position selection;
	private final LevelSerializer serializer;

	private static final int TILE_SIZE_PIXEL = 64;
	private static final int DISPLAYED_ARENA_WIDTH = 8;
	private static final int DISPLAYED_ARENA_HEIGHT = 8;

	LevelBuilderWindow(GameFrame gameFrame) {
		this.gameFrame = Objects.requireNonNull(gameFrame);
		debug = new DebugPrintsManager(true); // TODO
		builder = new LevelBuilder(8, 8); // TODO change default
		serializer = new LevelSerializerXML();
		menu = new Menu();
		arenaPanel = new ArenaPanel();

		setLayout(new FlowLayout());
		add(menu);
		add(arenaPanel);

		arenaPanel.reset();
		invalidate();
		repaint();
	}

	@Override
	public void clear() {
	}

	private void clearSelection() {
		if (selection == null)
			return;
		debug.println("clearSelection ", selection);
		selection = null;
	}

	private class Menu extends JPanel implements Clearable {

		private static final long serialVersionUID = 1L;
		private static final int ImgButtonSize = 50;

		Menu() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			add(createTerrainButtons());
			add(createBuildingButtons());
			add(createUnitButtons());
			add(createGeneralButtons());
		}

		private JButton createImgButton(Drawable drawable) {
			Image img = images.getImage(drawable).getScaledInstance(ImgButtonSize, ImgButtonSize,
					java.awt.Image.SCALE_SMOOTH);
			JButton button = new JButton(new ImageIcon(img));
//			button.setBorder(BorderFactory.createEmptyBorder());
			button.setContentAreaFilled(false);
			button.setPreferredSize(new Dimension(ImgButtonSize, ImgButtonSize));
			return button;
		}

		private JPanel createTerrainButtons() {
			JPanel panel = new JPanel(new GridLayout(0, 2));

			Terrain[] terrains = { Terrain.FLAT_LAND, Terrain.CLEAR_WATER, Terrain.MOUNTAIN };

			for (Terrain terrain : terrains) {
				JButton button = createImgButton(terrain);
				button.addActionListener(e -> selectObject(terrain));
				panel.add(button);
			}

			return panel;
		}

		private JPanel createBuildingButtons() {
			JPanel panel = new JPanel(new GridLayout(0, 2));

			List<BuildingDesc> buildings = new ArrayList<>();
			for (Team team : Team.values())
				for (Building.Type type : Building.Type.values())
					buildings.add(BuildingDesc.of(type, team));

			for (BuildingDesc building : buildings) {
				JButton button = createImgButton(building);
				button.addActionListener(e -> selectObject(building));
				panel.add(button);
			}
//			int rows = panel.getComponentCount() / 2;
//			panel.setPreferredSize(new Dimension());

			return panel;
		}

		private JPanel createUnitButtons() {
			JPanel panel = new JPanel(new GridLayout(0, 2));

			List<UnitDesc> units = new ArrayList<>();
			for (Team team : Team.values())
				for (Unit.Type type : Unit.Type.values())
					units.add(UnitDesc.of(type, team));

			for (UnitDesc unit : units) {
				JButton button = createImgButton(unit);
				button.addActionListener(e -> selectObject(unit));
				panel.add(button);
			}

			return panel;
		}

		private void selectObject(Object obj) {
			menuSelectedObj = obj;
		}

		private JPanel createGeneralButtons() {
			JPanel panel = new JPanel(new GridLayout(0, 1));

			JButton buttonReset = new JButton("Reset");
			buttonReset.addActionListener(e -> new ResetDialog().setVisible(true));
			panel.add(buttonReset);

			JButton buttonLoad = new JButton("Load");
			buttonLoad.addActionListener(e -> {
				JFileChooser fileChooser = new JFileChooser();
				String lvlType = serializer.getFileType();
				fileChooser.setFileFilter(new FileNameExtensionFilter("Level file (*." + lvlType + ")", lvlType));
				fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				int result = fileChooser.showOpenDialog(gameFrame);
				if (result == JFileChooser.APPROVE_OPTION) {
					String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
					try {
						Level level = serializer.levelRead(selectedFile);
						builder.reset(level);
					} catch (RuntimeException ex) {
						debug.print("failed to load file from: ", selectedFile);
						ex.printStackTrace();
					}
				}
			});
			panel.add(buttonLoad);

			JButton buttonSave = new JButton("Save");
			buttonSave.addActionListener(e -> {
				JFileChooser fileChooser = new JFileChooser();
				String lvlType = serializer.getFileType();
				fileChooser.setFileFilter(new FileNameExtensionFilter("Level file (*." + lvlType + ")", lvlType));
				fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				int result = fileChooser.showSaveDialog(gameFrame);
				if (result == JFileChooser.APPROVE_OPTION) {
					String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
					try {
						serializer.levelWrite(builder.buildLevel(), selectedFile);
					} catch (RuntimeException ex) {
						debug.print("failed to save level to file: ", selectedFile);
						ex.printStackTrace();
					}
				}
			});
			panel.add(buttonSave);

			return panel;
		}

		@Override
		public void clear() {
		}

	}

	private class ResetDialog extends JDialog implements Clearable {

		private static final long serialVersionUID = 1L;

		private static GridBagConstraints gbConstraints(int x, int y, int width, int height) {
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = x;
			c.gridy = y;
			c.gridwidth = width;
			c.gridheight = height;
			return c;
		}

		ResetDialog() {
			super(gameFrame, "Reset level");
			JPanel panel = new JPanel();

			JTextField widthText = new JTextField("8", 12);
			JTextField heightText = new JTextField("8", 12);
			JButton resetButton = new JButton("reset");
			JButton cancelButton = new JButton("cancel");

			resetButton.addActionListener(e -> {
				int width = -1, height = -1;
				try {
					width = Integer.parseInt(widthText.getText());
					height = Integer.parseInt(heightText.getText());
				} catch (NumberFormatException ex) {
				}
				if (!(1 <= width && width < 100 && 1 <= height && height < 100))
					return; /* TODO print message to user */
				dispose();
				builder.reset(width, height);
			});
			cancelButton.addActionListener(e -> dispose());

			panel.setLayout(new GridBagLayout());
			panel.add(new JLabel("Would you like to reset the level?"), gbConstraints(0, 0, 2, 1));
			panel.add(new JLabel("new level width:"), gbConstraints(0, 1, 1, 1));
			panel.add(widthText, gbConstraints(1, 1, 1, 1));
			panel.add(new JLabel("new level height:"), gbConstraints(0, 2, 1, 1));
			panel.add(heightText, gbConstraints(1, 2, 1, 1));
			panel.add(resetButton, gbConstraints(0, 3, 1, 1));
			panel.add(cancelButton, gbConstraints(1, 3, 1, 1));
			add(panel);
			pack();
		}

		@Override
		public void clear() {
		}

	}

	private class ArenaPanel extends JPanel implements Clearable {

		private final Map<Position, TileComp> tiles;

		private Position hovered;

		private Position mapPos;
		private double mapPosX, mapPosY;

		private final DataChangeRegister register;

		private static final int MapMoveTimerDelay = 10;
		private static final int MapMoveSpeed = 4;
		private static final long serialVersionUID = 1L;

		ArenaPanel() {
			tiles = new HashMap<>();
			register = new DataChangeRegister();

			mapPos = new Position(0, 0);
			mapPosX = mapPosY = 0;

			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					requestFocusInWindow();
					tileClicked(new Position(displayedXInv(e.getX()) / TILE_SIZE_PIXEL,
							displayedYInv(e.getY()) / TILE_SIZE_PIXEL));
				}
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					int x = displayedXInv(e.getX()) / TILE_SIZE_PIXEL, y = displayedYInv(e.getY()) / TILE_SIZE_PIXEL;
					if (hovered == null || hovered.x != x || hovered.y != y) {
						hovered = new Position(x, y);
						hoveredUpdated();
					}
				}
			});
			addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					Position.Direction dir = keyToDir(e.getKeyCode());
					if (dir != null)
						mapMove(dir);
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

			Timer mapMoveTimer = new Timer(MapMoveTimerDelay, e -> {
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

			register.registerListener(builder.onTileChange, e -> {
				/* TODO find a way to repaint only the changed tile */
				// tiles.get(e.pos).repaint();
				repaint();
			});
			register.registerListener(builder.onResetChange, e -> reset());

			setPreferredSize(getPreferredSize());
		}

		void mapMove(Position.Direction dir) {
			Position mapPosNew = mapPos.add(dir);
			if (!mapPosNew.isInRect(0, 0, builder.getWidth() - DISPLAYED_ARENA_WIDTH,
					builder.getHeight() - DISPLAYED_ARENA_HEIGHT))
				return;
			mapPos = mapPosNew;
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

		void hoveredUpdated() {

		}

		void reset() {
			for (Iterator<TileComp> it = tiles.values().iterator(); it.hasNext();) {
				TileComp tile = it.next();
				if (tile.pos.x >= builder.getWidth() || tile.pos.y >= builder.getHeight())
					it.remove();
			}
			for (Position pos : Utils.iterable(new Position.Iterator2D(builder.getWidth(), builder.getHeight())))
				tiles.computeIfAbsent(pos, p -> new TileComp(pos));

			mapPos = new Position(0, 0);
			mapPosX = mapPosY = 0;

			repaint();
		}

		@Override
		public void clear() {
			for (TileComp tile : tiles.values())
				tile.clear();
			tiles.clear();

			register.unregisterAllListeners(builder.onTileChange);
			register.unregisterAllListeners(builder.onResetChange);
		}

		@Override
		protected void paintComponent(Graphics g) {
			for (TileComp tile : tiles.values())
				tile.paintComponent(g);
			gameFrame.pack();

//			if (selection != null) {
//				tiles.get(selection).drawImage(g, Images.Label.Selection);
//			}
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(TILE_SIZE_PIXEL * DISPLAYED_ARENA_WIDTH, TILE_SIZE_PIXEL * DISPLAYED_ARENA_HEIGHT);
		}

		private boolean isUnitSelected() {
			return selection != null && builder.at(selection).hasUnit();
		}

		private void tileClicked(Position pos) {
			if (menuSelectedObj != null) {
				TileDesc tile = builder.at(pos);
				if (menuSelectedObj instanceof Terrain) {
					Terrain terrain = (Terrain) menuSelectedObj;

					BuildingDesc building = null;
					if (tile.hasBuilding()) {
						BuildingDesc oldBuilding = tile.building;
						if (oldBuilding.canBuildOnTerrain(terrain.type.category))
							building = oldBuilding;
					}

					UnitDesc unit = null;
					if (tile.hasUnit()) {
						UnitDesc oldUnit = tile.unit;
						if (oldUnit.canBuildOnTerrain(terrain.type.category))
							unit = oldUnit;
					}

					builder.setTile(pos.x, pos.y, terrain, building, unit);

				} else if (menuSelectedObj instanceof BuildingDesc) {
					BuildingDesc building = (BuildingDesc) menuSelectedObj;
					if (building.canBuildOnTerrain(tile.terrain.type.category))
						builder.setTile(pos.x, pos.y, tile.terrain, building, tile.unit);
					// TODO else user message

				} else if (menuSelectedObj instanceof UnitDesc) {
					UnitDesc unit = (UnitDesc) menuSelectedObj;
					if (unit.canBuildOnTerrain(tile.terrain.type.category))
						builder.setTile(pos.x, pos.y, tile.terrain, tile.building, unit);
					// TODO else user message

				} else {
					throw new InternalError("Unknown menu selected object: " + menuSelectedObj);
				}
			} else {
				if (selection == null) {
					trySelect(pos);
				} else if (isUnitSelected()) {
					clearSelection();
				}
			}
//			repaint();
		}

		private void trySelect(Position pos) {
			TileComp tileComp = tiles.get(pos);
			if (!tileComp.canSelect())
				return;
			debug.println("Selected ", pos);
			selection = pos;
			TileDesc tile = tileComp.tile();

			if (tile.hasUnit()) {
				// TODO
			} else if (tile.hasBuilding()) {
				// TODO
			} else {
				throw new InternalError();
			}
		}

		private class TileComp implements Clearable {

			private final Position pos;

			TileComp(Position pos) {
				this.pos = pos;
			}

			private boolean canSelect() {
				return false;
			}

			void paintComponent(Graphics g) {
				drawImage(g, tile().terrain, pos);

				if (tile().hasBuilding())
					drawImage(g, builder.at(pos).building, pos);
				if (tile().hasUnit())
					drawImage(g, builder.at(pos).unit, pos);

			}

			private TileDesc tile() {
				return builder.at(pos);
			}

			@Override
			public String toString() {
				return pos.toString();
			}

			@Override
			public void clear() {
			}

		}

		private void drawImage(Graphics g, Drawable obj, Position pos) {
			BufferedImage unitImg = images.getImage(obj);
			g.drawImage(unitImg, displayedX(pos.x * TILE_SIZE_PIXEL), displayedY(pos.y * TILE_SIZE_PIXEL),
					TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, this);
		}

	}

}
