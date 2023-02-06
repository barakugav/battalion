package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.ugav.battalion.AbstractArenaPanel.BuildingComp;
import com.ugav.battalion.AbstractArenaPanel.UnitComp;
import com.ugav.battalion.Images.Drawable;
import com.ugav.battalion.Level.BuildingDesc;
import com.ugav.battalion.Level.TileDesc;
import com.ugav.battalion.Level.UnitDesc;

class LevelBuilderWindow extends JPanel implements Clearable {

	private static final long serialVersionUID = 1L;

	private final LevelBuilder builder;
	private final Globals globals;
	private final Menu menu;
	private final ArenaPanel arenaPanel;
	private final DebugPrintsManager debug;
	private Object menuSelectedObj;

	LevelBuilderWindow(Globals globals) {
		this.globals = Objects.requireNonNull(globals);
		debug = new DebugPrintsManager(true); // TODO
		builder = new LevelBuilder(8, 8); // TODO change default
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
		menu.clear();
		arenaPanel.clear();
	}

	private class Menu extends JPanel implements Clearable {

		private static final long serialVersionUID = 1L;

		private final JPanel terrainTab;
		private final Map<Team, JPanel> buildlingsTabs;
		private final Map<Team, JPanel> unitsTabs;

		Menu() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			terrainTab = createTerrainPanel();
			buildlingsTabs = createBuildingsPanels();
			unitsTabs = createUnitsPanels();

			add(createEntitiesTabsButtons());
			add(createEntitiesPanel());
			add(createGeneralButtons());

			selectEntitiesTab(terrainTab);
		}

		private List<JPanel> getEntitiesTabs() {
			List<JPanel> tabs = new ArrayList<>(1 + buildlingsTabs.size() + unitsTabs.size());
			tabs.add(terrainTab);
			tabs.addAll(buildlingsTabs.values());
			tabs.addAll(unitsTabs.values());
			return tabs;
		}

		private JPanel createEntitiesPanel() {
			JPanel panel = new JPanel();
			Dimension panelSize = panel.getPreferredSize();
			for (JPanel tab : getEntitiesTabs()) {
				panel.add(tab);
				Dimension tabSize = tab.getPreferredSize();
				panelSize = new Dimension(Math.max(panelSize.width, tabSize.width),
						Math.max(panelSize.height, tabSize.height));
			}
			panel.setPreferredSize(panelSize);
			return panel;
		}

		private JButton createEntityTabButton(Drawable drawable) {
			final int ImgButtonSize = 20;
			Image img = Images.getImage(drawable).getScaledInstance(ImgButtonSize, ImgButtonSize, Image.SCALE_SMOOTH);
			JButton button = new JButton(new ImageIcon(img));
//			button.setBorder(BorderFactory.createEmptyBorder());
			button.setContentAreaFilled(false);
			button.setPreferredSize(new Dimension(ImgButtonSize, ImgButtonSize));
			return button;
		}

		private void selectEntitiesTab(JPanel tab) {
			for (JPanel otherTab : getEntitiesTabs())
				otherTab.setVisible(false);
			tab.setVisible(true);
			repaint();
		};

		private JPanel createEntitiesTabsButtons() {
			JPanel panel = new JPanel(new GridLayout(1, 0));

			JButton terrainButton = createEntityTabButton(Terrain.Mountain);
			terrainButton.addActionListener(e -> selectEntitiesTab(terrainTab));
			panel.add(terrainButton);

			for (Team team : Team.values()) {
				JButton buildingsButton = createEntityTabButton(BuildingDesc.of(Building.Type.Factory, team));
				buildingsButton.addActionListener(e -> selectEntitiesTab(buildlingsTabs.get(team)));
				panel.add(buildingsButton);
			}

			for (Team team : Team.realTeams) {
				JButton unitsButton = createEntityTabButton(UnitDesc.of(Unit.Type.Soldier, team));
				unitsButton.addActionListener(e -> selectEntitiesTab(unitsTabs.get(team)));
				panel.add(unitsButton);
			}

			return panel;
		}

		private JButton createEntityButton(Drawable drawable) {
			final int ImgButtonSize = 50;
			Image img = Images.getImage(drawable).getScaledInstance(ImgButtonSize, ImgButtonSize, Image.SCALE_SMOOTH);
			JButton button = new JButton(new ImageIcon(img));
//			button.setBorder(BorderFactory.createEmptyBorder());
			button.setContentAreaFilled(false);
			button.setPreferredSize(new Dimension(ImgButtonSize, ImgButtonSize));
			return button;
		}

		private JPanel createTerrainPanel() {
			JPanel panel = new JPanel(new GridLayout(0, 2));

			for (Terrain terrain : Terrain.values()) {
				JButton button = createEntityButton(terrain);
				button.addActionListener(e -> selectObject(terrain));
				panel.add(button);
			}

			return panel;
		}

		private Map<Team, JPanel> createBuildingsPanels() {
			Map<Team, JPanel> panels = new HashMap<>(Team.values().length);
			for (Team team : Team.values()) {
				JPanel panel = new JPanel(new GridLayout(0, 2));
				for (Building.Type type : Building.Type.values()) {
					BuildingDesc building = BuildingDesc.of(type, team);
					JButton button = createEntityButton(building);
					button.addActionListener(e -> selectObject(building));
					panel.add(button);
				}
				panels.put(team, panel);
			}
			return panels;
		}

		private Map<Team, JPanel> createUnitsPanels() {
			Map<Team, JPanel> panels = new HashMap<>(Team.realTeams.size());
			for (Team team : Team.realTeams) {
				JPanel panel = new JPanel(new GridLayout(0, 2));
				for (Unit.Type type : Unit.Type.values()) {
					UnitDesc unit = UnitDesc.of(type, team);
					JButton button = createEntityButton(unit);
					button.addActionListener(e -> selectObject(unit));
					panel.add(button);
				}
				panels.put(team, panel);
			}
			return panels;
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
				JFileChooser fileChooser = Levels.createFileChooser(globals.serializer.getFileType());
				int result = fileChooser.showOpenDialog(globals.frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					Cookies.setCookieValue(Cookies.LEVEL_DISK_LAST_DIR,
							fileChooser.getCurrentDirectory().getAbsolutePath());
					String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
					try {
						Level level = globals.serializer.levelRead(selectedFile);
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
				JFileChooser fileChooser = Levels.createFileChooser(globals.serializer.getFileType());
				int result = fileChooser.showSaveDialog(globals.frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					Cookies.setCookieValue(Cookies.LEVEL_DISK_LAST_DIR,
							fileChooser.getCurrentDirectory().getAbsolutePath());
					String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
					try {
						globals.serializer.levelWrite(builder.buildLevel(), selectedFile);
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
				globals.frame.displayMainMenu();
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

		ResetDialog() {
			super(globals.frame, "Reset level");
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
			panel.add(new JLabel("Would you like to reset the level?"), Utils.gbConstraints(0, 0, 2, 1));
			panel.add(new JLabel("new level width:"), Utils.gbConstraints(0, 1, 1, 1));
			panel.add(widthText, Utils.gbConstraints(1, 1, 1, 1));
			panel.add(new JLabel("new level height:"), Utils.gbConstraints(0, 2, 1, 1));
			panel.add(heightText, Utils.gbConstraints(1, 2, 1, 1));
			panel.add(resetButton, Utils.gbConstraints(0, 3, 1, 1));
			panel.add(cancelButton, Utils.gbConstraints(1, 3, 1, 1));
			add(panel);
			pack();
		}

		@Override
		public void clear() {
		}

	}

	private class ArenaPanel extends AbstractArenaPanel<ArenaPanel.TileComp, BuildingComp, UnitComp>
			implements Clearable {

		private final DataChangeRegister register;

		private static final long serialVersionUID = 1L;

		ArenaPanel() {
			register = new DataChangeRegister();

			register.registerListener(builder.onTileChange, e -> {
				tiles.computeIfAbsent(e.pos, TileComp::new).tileUpdate();
				repaint(); /* TODO find a way to repaint only the changed tile */
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
			removeEnteriesComp();

			for (Position pos : Utils.iterable(new Position.Iterator2D(builder.getWidth(), builder.getHeight())))
				tiles.put(pos, new TileComp(pos));

			mapViewSet(new Position(0, 0));
			repaint();
		}

		@Override
		public void clear() {
			register.unregisterAllListeners(builder.onTileChange);
			register.unregisterAllListeners(builder.onResetChange);
			register.unregisterAllListeners(onTileClick);

			super.clear();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
		}

		private void tileClicked(Position pos) {
			if (menuSelectedObj != null) {
				TileDesc tile = builder.at(pos);
				if (menuSelectedObj instanceof Terrain) {
					Terrain terrain = (Terrain) menuSelectedObj;

					BuildingDesc building = null;
					if (tile.hasBuilding()) {
						BuildingDesc oldBuilding = tile.building;
						if (oldBuilding.type.canBuildOn.contains(terrain.category))
							building = oldBuilding;
					}

					UnitDesc unit = null;
					if (tile.hasUnit()) {
						UnitDesc oldUnit = tile.unit;
						if (oldUnit.type.canStand.contains(terrain.category))
							unit = oldUnit;
					}

					builder.setTile(pos.x, pos.y, terrain, building, unit);

				} else if (menuSelectedObj instanceof BuildingDesc) {
					BuildingDesc building = new BuildingDesc((BuildingDesc) menuSelectedObj);
					if (building.type.canBuildOn.contains(tile.terrain.category))
						builder.setTile(pos.x, pos.y, tile.terrain, building, tile.unit);
					// TODO else user message

				} else if (menuSelectedObj instanceof UnitDesc) {
					UnitDesc unit = new UnitDesc((UnitDesc) menuSelectedObj);
					if (unit.type.canStand.contains(tile.terrain.category))
						builder.setTile(pos.x, pos.y, tile.terrain, tile.building, unit);
					// TODO else user message

				} else {
					throw new InternalError("Unknown menu selected object: " + menuSelectedObj);
				}
			}
		}

		@Override
		Object getTerrain(Position pos) {
			return builder.at(pos).terrain;
		}

		@Override
		Object getBuilding(Position pos) {
			return builder.at(pos).building;
		}

		@Override
		Object getUnit(Position pos) {
			return builder.at(pos).unit;
		}

		private class TileComp extends AbstractArenaPanel.TileComp {

			BuildingDesc building;
			UnitDesc unit;

			TileComp(Position pos) {
				super(ArenaPanel.this, pos);
			}

			void tileUpdate() {
				TileDesc tile = builder.at(pos);
				if (tile.building != null && building == null)
					buildings.put(building = tile.building, new BuildingComp(ArenaPanel.this, pos));
				if (tile.building == null && building != null)
					buildings.remove(building).clear();
				if (tile.unit != null && unit == null)
					units.put(unit = tile.unit, new UnitComp(ArenaPanel.this, pos));
				if (tile.unit == null && unit != null)
					units.remove(unit).clear();
			}

		}

	}

}
