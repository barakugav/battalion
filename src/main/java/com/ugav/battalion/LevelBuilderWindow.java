package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
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

		private final EntityTab terrainTab;
		private final Map<Team, EntityTab> buildlingsTabs;
		private final Map<Team, EntityTab> unitsTabs;

		private EntityTab selectedTab;
		private EntityButton selectedButton;

		Menu() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			terrainTab = new EntityTab();
			for (Terrain terrain : Terrain.values())
				terrainTab.addEntityButton(terrain);

			buildlingsTabs = new HashMap<>(Team.values().length);
			for (Team team : Team.values()) {
				EntityTab tab = new EntityTab();
				for (Building.Type type : Building.Type.values())
					tab.addEntityButton(BuildingDesc.of(type, team));
				buildlingsTabs.put(team, tab);
			}

			unitsTabs = new HashMap<>(Team.realTeams.size());
			for (Team team : Team.realTeams) {
				EntityTab tab = new EntityTab();
				for (Unit.Type type : Unit.Type.values())
					tab.addEntityButton(UnitDesc.of(type, team));
				unitsTabs.put(team, tab);
			}

			add(createEntitiesTabsButtons());
			add(createEntitiesTabsPanel());
			add(createGeneralButtons());

			selectEntitiesTab(terrainTab);
		}

		private List<EntityTab> getEntitiesTabs() {
			List<EntityTab> tabs = new ArrayList<>(1 + buildlingsTabs.size() + unitsTabs.size());
			tabs.add(terrainTab);
			tabs.addAll(buildlingsTabs.values());
			tabs.addAll(unitsTabs.values());
			return tabs;
		}

		private JPanel createEntitiesTabsPanel() {
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

		private void selectEntitiesTab(EntityTab tab) {
			if (tab == selectedTab)
				return;
			if (selectedTab != null)
				selectedTab.setSelect(false);
			(selectedTab = tab).setSelect(true);
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

		private class EntityButton extends JButton {

			private static final long serialVersionUID = 1L;

			final Object entity;
			private static final int IconWidth = 56;
			private static final int IconHeight = 72;

			EntityButton(Object entity) {
				super();

				this.entity = Objects.requireNonNull(entity);

				setIcons();

				setBorder(BorderFactory.createEmptyBorder());
				setContentAreaFilled(false);

				setPreferredSize(new Dimension(IconWidth + 2, IconHeight + 2));
				addActionListener(e -> selectButton(EntityButton.this));
			}

			void setIcons() {
				BufferedImage img = Images.getImage(entity);
				BufferedImage selectImg = Images.getImage(Images.Label.Selection);
				for (BufferedImage i : List.of(img, selectImg))
					if (i.getWidth() != IconWidth || i.getHeight() > IconHeight)
						throw new IllegalArgumentException("icon too big for entity: " + entity);
				Graphics g;

				/* Regular icon */
				BufferedImage icon = new BufferedImage(IconWidth, IconHeight, BufferedImage.TYPE_INT_ARGB);
				g = icon.getGraphics();
				g.drawImage(img, 0, IconHeight - img.getHeight(), IconWidth, img.getHeight(), null);
				setIcon(new ImageIcon(icon));

				/* Selected icon */
				BufferedImage selectedIcon = new BufferedImage(IconWidth, IconHeight, BufferedImage.TYPE_INT_ARGB);
				g = selectedIcon.getGraphics();
				g.drawImage(img, 0, IconHeight - img.getHeight(), IconWidth, img.getHeight(), null);
				g.drawImage(selectImg, 0, IconHeight - selectImg.getHeight(), IconWidth, IconWidth, null);
				setSelectedIcon(new ImageIcon(selectedIcon));
			}

		}

		private class EntityTab extends JPanel {

			private static final long serialVersionUID = 1L;

			final List<EntityButton> buttons;

			EntityTab() {
				super(new GridLayout(0, 2));
				buttons = new ArrayList<>();
				setVisible(false);
			}

			void addEntityButton(Object entity) {
				EntityButton button = new EntityButton(entity);
				add(button);
				buttons.add(button);
			}

			void setSelect(boolean select) {
				if (select)
					selectButton(buttons.isEmpty() ? null : buttons.get(0));
				setVisible(select);
			}

		}

		private void selectButton(EntityButton button) {
			if (selectedButton != null)
				selectedButton.setSelected(false);
			if ((selectedButton = button) != null)
				button.setSelected(true);
			repaint();
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

			register.register(builder.onTileChange, e -> {
				tiles.computeIfAbsent(e.pos, TileComp::new).tileUpdate();
				repaint(); /* TODO find a way to repaint only the changed tile */
			});
			register.register(builder.onResetChange, e -> reset());
			register.register(onTileClick, e -> tileClicked(e.pos));
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
			register.unregisterAll();
			super.clear();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
		}

		private void tileClicked(Position pos) {
			Object selectedObj = menu.selectedButton.entity;

			if (selectedObj != null) {
				TileDesc tile = builder.at(pos);
				if (selectedObj instanceof Terrain) {
					Terrain terrain = (Terrain) selectedObj;

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

				} else if (selectedObj instanceof BuildingDesc) {
					BuildingDesc building = new BuildingDesc((BuildingDesc) selectedObj);
					if (building.type.canBuildOn.contains(tile.terrain.category))
						builder.setTile(pos.x, pos.y, tile.terrain, building, tile.unit);
					// TODO else user message

				} else if (selectedObj instanceof UnitDesc) {
					UnitDesc unit = new UnitDesc((UnitDesc) selectedObj);
					if (unit.type.canStand.contains(tile.terrain.category))
						builder.setTile(pos.x, pos.y, tile.terrain, tile.building, unit);
					// TODO else user message

				} else {
					throw new InternalError("Unknown menu selected object: " + selectedObj);
				}
			}
		}

		@Override
		Terrain getTerrain(Position pos) {
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
