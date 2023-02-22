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

import com.ugav.battalion.ArenaPanelAbstract.BuildingComp;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.TileDesc;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.LevelBuilder;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.core.Unit.Category;
import com.ugav.battalion.core.Unit.Type;

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
		builder = new LevelBuilder(Level.MINIMUM_WIDTH, Level.MINIMUM_HEIGHT);
		menu = new Menu();
		arenaPanel = new ArenaPanel();

		setLayout(new FlowLayout());
		add(menu);
		add(arenaPanel);

		arenaPanel.reset();
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
		private static final Object removeBuildingObj = new Object();
		private static final Object removeUnitObj = new Object();

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
				tab.addEntityButton(new EntityButton(removeBuildingObj, Images.Label.Delete));
				buildlingsTabs.put(team, tab);
			}

			unitsTabs = new HashMap<>(Team.realTeams.size());
			for (Team team : Team.realTeams) {
				EntityTab tab = new EntityTab();
				for (Unit.Type type : Unit.Type.values()) {
					if (!type.transportUnits) {
						tab.addEntityButton(UnitDesc.of(type, team));
					} else {
						tab.addEntityButton(UnitDesc.transporter(type, UnitDesc.of(Type.Soldier, team)));
					}
				}
				tab.addEntityButton(new EntityButton(removeUnitObj, Images.Label.Delete));
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
				panelSize = Utils.max(panelSize, tab.getPreferredSize());
			}
			panel.setPreferredSize(panelSize);
			return panel;
		}

		private JButton createEntityTabButton(Object drawable) {
			final int ImgButtonSize = 20;
			Image img = Images.getImg(drawable).getScaledInstance(ImgButtonSize, ImgButtonSize, Image.SCALE_SMOOTH);
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
			private static final int IconHeight = 75;

			EntityButton(Object entity) {
				this(entity, entity);
			}

			EntityButton(Object entity, Object iconTag) {
				super();

				this.entity = Objects.requireNonNull(entity);

				setIcons(iconTag);

				setBorder(BorderFactory.createEmptyBorder());
				setContentAreaFilled(false);

				setPreferredSize(new Dimension(IconWidth + 2, IconHeight + 2));
				addActionListener(e -> selectButton(EntityButton.this));
			}

			void setIcons(Object iconTag) {
				BufferedImage img = Images.getImg(iconTag);
				BufferedImage selectImg = Images.getImg(Images.Label.Selection);
				for (BufferedImage i : List.of(img, selectImg))
					if (i.getWidth() != IconWidth || i.getHeight() > IconHeight)
						throw new IllegalArgumentException("icon too big for entity: " + iconTag);
				Graphics g;

				/* Regular icon */
				BufferedImage icon = new BufferedImage(IconWidth, IconHeight, BufferedImage.TYPE_INT_ARGB);
				g = icon.getGraphics();
				g.drawImage(img, 0, IconHeight - img.getHeight(), null);
				setIcon(new ImageIcon(icon));

				/* Selected icon */
				BufferedImage selectedIcon = new BufferedImage(IconWidth, IconHeight, BufferedImage.TYPE_INT_ARGB);
				g = selectedIcon.getGraphics();
				g.drawImage(img, 0, IconHeight - img.getHeight(), null);
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
				addEntityButton(new EntityButton(entity));

			}

			void addEntityButton(EntityButton button) {
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
				JFileChooser fileChooser = Levels.createFileChooser(globals.levelSerializer.getFileType(),
						Cookies.getCookieValue(Cookies.LEVEL_DISK_LAST_DIR));
				int result = fileChooser.showOpenDialog(globals.frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					Cookies.setCookieValue(Cookies.LEVEL_DISK_LAST_DIR,
							fileChooser.getCurrentDirectory().getAbsolutePath());
					String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
					try {
						Level level = globals.levelSerializer.levelRead(selectedFile);
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
				JFileChooser fileChooser = Levels.createFileChooser(globals.levelSerializer.getFileType(),
						Cookies.getCookieValue(Cookies.LEVEL_DISK_LAST_DIR));
				int result = fileChooser.showSaveDialog(globals.frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					Cookies.setCookieValue(Cookies.LEVEL_DISK_LAST_DIR,
							fileChooser.getCurrentDirectory().getAbsolutePath());
					String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
					try {
						globals.levelSerializer.levelWrite(builder.buildLevel(), selectedFile);
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

			JTextField widthText = new JTextField(Integer.toString(builder.width()), 12);
			JTextField heightText = new JTextField(Integer.toString(builder.height()), 12);
			JButton resetButton = new JButton("reset");
			JButton cancelButton = new JButton("cancel");

			resetButton.addActionListener(e -> {
				int width = -1, height = -1;
				try {
					width = Integer.parseInt(widthText.getText());
					height = Integer.parseInt(heightText.getText());
				} catch (NumberFormatException ex) {
				}
				if (!(Level.MINIMUM_WIDTH <= width && width < 100 && Level.MINIMUM_HEIGHT <= height && height < 100))
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

	private class ArenaPanel extends
			ArenaPanelAbstract<ArenaPanel.EntityLayer.TerrainComp, BuildingComp, ArenaPanel.EntityLayer.UnitComp>
			implements Clearable {

		private final DataChangeRegister register = new DataChangeRegister();

		private static final long serialVersionUID = 1L;

		@Override
		EntityLayer createEntityLayer() {
			return new EntityLayer(this);
		}

		ArenaPanel() {
			register.register(builder.onResetChange, e -> reset());
			register.register(entityLayer.onTileClick, e -> tileClicked(e.pos));

			tickTaskManager.start();
		}

		EntityLayer entityLayer() {
			return (EntityLayer) entityLayer;
		}

		void reset() {
			entityLayer().reset();
			updateArenaSize(builder.width(), builder.height());
			mapViewSet(Position.of(0, 0));
		}

		@Override
		public void clear() {
			register.unregisterAll();
			super.clear();
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
						if (oldBuilding.type.canBuildOn(terrain))
							building = oldBuilding;
					}

					UnitDesc unit = null;
					if (tile.hasUnit()) {
						UnitDesc oldUnit = tile.unit;
						if (oldUnit.type.canStandOn(terrain))
							unit = oldUnit;
					}

					builder.setTile(pos, terrain, building, unit);

				} else if (selectedObj instanceof BuildingDesc) {
					BuildingDesc building = BuildingDesc.copyOf((BuildingDesc) selectedObj);
					if (building.type.canBuildOn(tile.terrain))
						builder.setTile(pos, tile.terrain, building, tile.unit);
					// TODO else user message

				} else if (selectedObj instanceof UnitDesc) {
					UnitDesc unit = UnitDesc.copyOf((UnitDesc) selectedObj);

					UnitDesc oldUnit;
					if (tile.hasUnit() && (oldUnit = tile.getUnit()).team == unit.team && oldUnit.type.transportUnits
							&& !unit.type.transportUnits && unit.type.category == Category.Land)
						builder.setTile(pos, tile.terrain, tile.building, UnitDesc.transporter(oldUnit.type, unit));

					else if (unit.type.canStandOn(tile.terrain))
						builder.setTile(pos, tile.terrain, tile.building, unit);
					// TODO else user message

				} else if (selectedObj == Menu.removeBuildingObj) {
					if (tile.hasBuilding())
						builder.setTile(pos, tile.terrain, null, tile.unit);

				} else if (selectedObj == Menu.removeUnitObj) {
					if (tile.hasUnit())
						builder.setTile(pos, tile.terrain, tile.building, null);

				} else {
					throw new IllegalArgumentException("Unknown menu selected object: " + selectedObj);
				}
			}
		}

		@Override
		Terrain getTerrain(Position pos) {
			return builder.at(pos).terrain;
		}

		private class EntityLayer
				extends ArenaPanelAbstract.EntityLayer<EntityLayer.TerrainComp, BuildingComp, EntityLayer.UnitComp> {

			private static final long serialVersionUID = 1L;

			private final DataChangeRegister register = new DataChangeRegister();

			EntityLayer(ArenaPanel arena) {
				super(arena);

				register.register(builder.onTileChange, e -> {
					TerrainComp comp = (TerrainComp) comps.computeIfAbsent(terrainKey(e.pos),
							k -> new TerrainComp(e.pos));
					comp.tileUpdate();
				});
			}

			void reset() {
				removeAllArenaComps();

				for (Position pos : Position.Iterator2D.of(builder.width(), builder.height()).forEach()) {
					TerrainComp tileComp = new TerrainComp(pos);
					comps.put(terrainKey(pos), tileComp);
					tileComp.tileUpdate();
				}
			}

			private Object terrainKey(Position pos) {
				return "Terrain " + pos;
			}

			private class TerrainComp extends ArenaPanelAbstract.TerrainComp {

				BuildingComp buildingComp;
				UnitComp unitComp;

				TerrainComp(Position pos) {
					super(ArenaPanel.this, pos);
				}

				void tileUpdate() {
					TileDesc tile = builder.at(pos);

					if (buildingComp != null && tile.building != buildingComp.building()) {
						comps.remove(buildingComp.building()).clear();
						buildingComp = null;
					}
					if (tile.building != null && buildingComp == null)
						comps.put(tile.building, buildingComp = new BuildingComp(ArenaPanel.this, pos, tile.building));

					if (unitComp != null && tile.unit != unitComp.unit()) {
						comps.remove(unitComp.unit()).clear();
						unitComp = null;
					}
					if (tile.unit != null && unitComp == null)
						comps.put(tile.unit, unitComp = new UnitComp(ArenaPanel.this, pos, tile.unit));
				}

			}

			private class UnitComp extends ArenaPanelAbstract.UnitComp {

				UnitComp(ArenaPanelAbstract<?, ?, ?> arena, Position pos, UnitDesc unit) {
					super(arena, pos, unit);
				}

				@Override
				UnitDesc unit() {
					return (UnitDesc) super.unit();
				}

			}

		}

	}

}
