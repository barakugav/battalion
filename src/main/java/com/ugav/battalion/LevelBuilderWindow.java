package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.ugav.battalion.Level.TileDesc;

class LevelBuilderWindow extends JPanel implements Clearable {

	private static final long serialVersionUID = 1L;

	private final LevelBuilder builder;
	private final GameFrame gameFrame;
	private final Menu menu;
	private final ArenaPanel arenaPanel;
	private final Images images = new Images();
	private final DebugPrintsManager debug;
	private Position selection;
	private final LevelSerializer serializer;

	private static final int TILE_SIZE_PIXEL = 64;
	private static final int DISPLAYED_ARENA_WIDTH = 8;
	private static final int DISPLAYED_ARENA_HEIGHT = 8;

	LevelBuilderWindow(GameFrame gameFrame) {
		this.gameFrame = Objects.requireNonNull(gameFrame);
		menu = new Menu();
		arenaPanel = new ArenaPanel();
		debug = new DebugPrintsManager(true); // TODO
		builder = new LevelBuilder(8, 8); // TODO change default
		serializer = new LevelSerializerXML();

		setLayout(new FlowLayout());
		add(menu);
		add(arenaPanel);

		arenaPanel.initArena();
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

		Menu() {
			setLayout(new GridLayout(0, 1));

			JButton buttonReset = new JButton("Reset");
			buttonReset.addActionListener(e -> new ResetDialog().setVisible(true));
			add(buttonReset);

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
						repaint();
					} catch (RuntimeException ex) {
						debug.print("failed to load file from: ", selectedFile);
						ex.printStackTrace();
					}
				}
			});
			add(buttonLoad);

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
			add(buttonSave);
		}

		@Override
		public void clear() {
		}

	}

	private class ResetDialog extends JDialog implements Clearable {

		private static final long serialVersionUID = 1L;

		ResetDialog() {
			super(gameFrame, "Reset level");
			JPanel panel = new JPanel();
			panel.add(new JLabel("Would you like to reset the level?"));
			panel.add(new JLabel("new level width:"));
			panel.add(new JTextField());
			panel.add(new JLabel("new level height:"));
			panel.add(new JTextField());
			panel.add(new JButton("reset"));
			panel.add(new JButton("cancel"));
			add(panel);
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
		}

		void mapMove(Position.Direction dir) {
			Position mapPosNew = mapPos.add(dir);
			if (!mapPosNew.isInRect(0, 0, builder.getWidth() - DISPLAYED_ARENA_WIDTH,
					builder.getWidth() - DISPLAYED_ARENA_HEIGHT))
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

		void initArena() {
			for (Position pos : Utils.iterable(new Position.Iterator2D(builder.getWidth(), builder.getHeight())))
				tiles.put(pos, new TileComp(pos));

			setPreferredSize(getPreferredSize());

			register.registerListener(builder.onTileChange, e -> {
				repaint();
			});

			mapPos = new Position(0, 0);
			mapPosX = mapPosY = 0;
		}

		@Override
		public void clear() {
			for (TileComp tile : tiles.values())
				tile.clear();
			tiles.clear();

			register.unregisterAllListeners(builder.onTileChange);
		}

		@Override
		protected void paintComponent(Graphics g) {
			for (TileComp tile : tiles.values())
				tile.paintComponent(g);

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
			if (selection == null) {
				trySelect(pos);
			} else if (isUnitSelected()) {
				clearSelection();
			}
			repaint();
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
				drawImage(g, Images.Label.valueOf(tile().terrain.type), pos);

				if (tile().hasBuilding())
					drawImage(g, Images.Label.valueOf(builder.at(pos).building), pos);
				if (tile().hasUnit())
					drawImage(g, Images.Label.valueOf(builder.at(pos).unit), pos);

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

		private void drawImage(Graphics g, Images.Label label, Position pos) {
			BufferedImage unitImg = images.getImage(label);
			g.drawImage(unitImg, displayedX(pos.x * TILE_SIZE_PIXEL), displayedY(pos.y * TILE_SIZE_PIXEL),
					TILE_SIZE_PIXEL, TILE_SIZE_PIXEL, this);
		}

	}

}
