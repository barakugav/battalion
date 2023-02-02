package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
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
	private final DebugPrintsManager debug;
	private Object menuSelectedObj;
	private Position selection;
	private final LevelSerializer serializer;

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
			Image img = Images.getImage(drawable).getScaledInstance(ImgButtonSize, ImgButtonSize,
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

			JButton buttonMainMenu = new JButton("Main Menu");
			buttonMainMenu.addActionListener(e -> {
				// TODO ask the user if he sure, does he want to save?
				gameFrame.displayMainMenu();
			});
			panel.add(buttonMainMenu);

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

			JTextField widthText = new JTextField(Integer.toString(builder.getWidth()), 12);
			JTextField heightText = new JTextField(Integer.toString(builder.getHeight()), 12);
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

	private class ArenaPanel extends AbstractArenaPanel implements Clearable {

		private final Map<Position, TileComp> tiles;
		private final DataChangeRegister register;

		private static final long serialVersionUID = 1L;

		ArenaPanel() {
			tiles = new HashMap<>();
			register = new DataChangeRegister();

			register.registerListener(builder.onTileChange, e -> {
				/* TODO find a way to repaint only the changed tile */
				// tiles.get(e.pos).repaint();
				repaint();
			});
			register.registerListener(builder.onResetChange, e -> reset());
			register.registerListener(onTileClick, e -> tileClicked(e.pos));
		}

		@Override
		int getArenaWidth() {
			return builder.getWidth();
		}

		@Override
		int getArenaHeight() {
			return builder.getHeight();
		}

		void reset() {
			for (Iterator<TileComp> it = tiles.values().iterator(); it.hasNext();) {
				TileComp tile = it.next();
				if (tile.pos.x >= builder.getWidth() || tile.pos.y >= builder.getHeight())
					it.remove();
			}
			for (Position pos : Utils.iterable(new Position.Iterator2D(builder.getWidth(), builder.getHeight())))
				tiles.computeIfAbsent(pos, p -> new TileComp(pos));

			mapViewSet(new Position(0, 0));
			repaint();
		}

		@Override
		public void clear() {
			register.unregisterAllListeners(builder.onTileChange);
			register.unregisterAllListeners(builder.onResetChange);
			register.unregisterAllListeners(onTileClick);

			for (TileComp tile : tiles.values())
				tile.clear();
			tiles.clear();

			super.clear();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			for (TileComp tile : tiles.values())
				tile.paintComponent(g);
			gameFrame.pack();

//			if (selection != null) {
//				tiles.get(selection).drawImage(g, Images.Label.Selection);
//			}
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

	}

}
