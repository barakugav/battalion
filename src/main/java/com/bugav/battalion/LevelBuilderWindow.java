package com.bugav.battalion;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
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
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.bugav.battalion.ArenaPanelAbstract.BuildingComp;
import com.bugav.battalion.core.Building;
import com.bugav.battalion.core.Cell;
import com.bugav.battalion.core.Level;
import com.bugav.battalion.core.LevelBuilder;
import com.bugav.battalion.core.Team;
import com.bugav.battalion.core.Terrain;
import com.bugav.battalion.core.Unit;
import com.bugav.battalion.core.Level.BuildingDesc;
import com.bugav.battalion.core.Level.UnitDesc;
import com.bugav.battalion.core.Unit.Category;
import com.bugav.battalion.core.Unit.Type;
import com.bugav.battalion.util.Event;
import com.bugav.battalion.util.Iter;
import com.bugav.battalion.util.Utils;

class LevelBuilderWindow extends JPanel implements Clearable {

	private static final long serialVersionUID = 1L;

	private final LevelBuilder builder;
	private final Globals globals;
	private final SideMenu menu;
	private final ArenaPanel arenaPanel;

	LevelBuilderWindow(Globals globals) {
		this.globals = Objects.requireNonNull(globals);
		builder = new LevelBuilder(10, 10);
		menu = new SideMenu();
		arenaPanel = new ArenaPanel(globals);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;

		c.gridx = 0;
		c.gridwidth = 1;
		c.weightx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		add(menu, c);
		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		add(arenaPanel, c);

		arenaPanel.reset();
	}

	@Override
	public void clear() {
		menu.clear();
		arenaPanel.clear();
	}

	private class SideMenu extends JPanel implements Clearable {

		private static final long serialVersionUID = 1L;

		private final CardLayout entitiesTabsLayout = new CardLayout();
		private final JPanel entitiesTabsPanel = new JPanel(entitiesTabsLayout);

		private final EntityTab terrainTab;
		private final Map<Team, EntityTab> buildlingsTabs;
		private final Map<Team, EntityTab> unitsTabs;

		private EntityTab selectedTab;
		private EntityButton selectedButton;
		private static final Object removeBuildingObj = new Object();
		private static final Object removeUnitObj = new Object();

		SideMenu() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			terrainTab = new EntityTab("terrains");
			for (Terrain.Category terrain : Terrain.Category.values()) {
				Object terrainIcon = terrain.getTerrains().get(0);
				terrainTab.addEntityButton(new EntityButton(terrain, terrainIcon));
			}

			buildlingsTabs = new HashMap<>(Team.values().length);
			for (Team team : Team.values()) {
				EntityTab tab = new EntityTab("Buildings" + team);
				for (Building.Type type : Building.Type.values())
					tab.addEntityButton(BuildingDesc.of(type, team));
				tab.addEntityButton(new EntityButton(removeBuildingObj, Images.Delete));
				buildlingsTabs.put(team, tab);
			}

			unitsTabs = new HashMap<>(Team.values().length);
			for (Team team : Team.values()) {
				EntityTab tab = new EntityTab("Units" + team);
				for (Unit.Type type : Unit.Type.values()) {
					if (!type.transportUnits) {
						tab.addEntityButton(UnitDesc.of(type, team));
					} else {
						tab.addEntityButton(UnitDesc.transporter(type, UnitDesc.of(Type.Rifleman, team)));
					}
				}
				tab.addEntityButton(new EntityButton(removeUnitObj, Images.Delete));
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
			int width = 100;
			for (EntityTab tab : getEntitiesTabs()) {
				entitiesTabsPanel.add(tab, tab.name);
				width = Math.max(width, tab.getPreferredSize().width + 2);
				tab.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			}
			entitiesTabsPanel.setPreferredSize(new Dimension(width, 400));
			return entitiesTabsPanel;
		}

		private JButton createEntityTabButton(BufferedImage icon, ActionListener listener) {
			final int ImgButtonSize = 20;
			Image img = icon.getScaledInstance(ImgButtonSize, ImgButtonSize, Image.SCALE_SMOOTH);
			JButton button = new JButton(new ImageIcon(img));
//			button.setBorder(BorderFactory.createEmptyBorder());
			button.setContentAreaFilled(false);
			button.setPreferredSize(new Dimension(ImgButtonSize, ImgButtonSize));
			button.addActionListener(listener);
			return button;
		}

		private void selectEntitiesTab(EntityTab tab) {
			if (tab == selectedTab)
				return;
			if (selectedTab != null)
				selectedTab.setSelect(false);
			(selectedTab = tab).setSelect(true);
			entitiesTabsLayout.show(entitiesTabsPanel, selectedTab.name);
			repaint();
		};

		private JPanel createEntitiesTabsButtons() {
			JPanel panel = new JPanel(new GridLayout(1, 0));

			BufferedImage terrainIcon = Images.Terrains.getDefault(Terrain.Mountain1);
			panel.add(createEntityTabButton(terrainIcon, e -> selectEntitiesTab(terrainTab)));

			for (Team team : Team.values()) {
				BufferedImage icon = Images.Buildings.getDefault(BuildingDesc.of(Building.Type.Factory, team));
				panel.add(createEntityTabButton(icon, e -> selectEntitiesTab(buildlingsTabs.get(team))));
			}

			for (Team team : Team.values()) {
				BufferedImage icon = Images.Units.getDefault(UnitDesc.of(Unit.Type.Rifleman, team));
				panel.add(createEntityTabButton(icon, e -> selectEntitiesTab(unitsTabs.get(team))));
			}

			return panel;
		}

		private class EntityButton extends JButton {

			private static final long serialVersionUID = 1L;

			final Object entity;
			private static final int IconWidth = 56;
			private static final int IconHeight = 56;

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
				BufferedImage img = iconTag instanceof BufferedImage im ? im : Images.getImg(iconTag);
				Graphics g;

				/* Regular icon */
				BufferedImage icon = new BufferedImage(IconWidth, IconHeight, BufferedImage.TYPE_INT_ARGB);
				g = icon.getGraphics();
				g.drawImage(img, 0, (IconHeight - img.getHeight()) / 2, null);
				setIcon(new ImageIcon(icon));

				/* Selected icon */
				BufferedImage selectedIcon = new BufferedImage(IconWidth, IconHeight, BufferedImage.TYPE_INT_ARGB);
				g = selectedIcon.getGraphics();
				g.drawImage(img, 0, (IconHeight - img.getHeight()) / 2, null);
				g.drawImage(Images.Selection, 0, 0, null);
				setSelectedIcon(new ImageIcon(selectedIcon));
			}

		}

		private class EntityTab extends JScrollPane {

			private static final long serialVersionUID = 1L;

			private final String name;
			private final JPanel panel;
			private final List<EntityButton> buttons;

			EntityTab(String name) {
				this.name = Objects.requireNonNull(name);
				panel = new JPanel(new GridBagLayout());
				buttons = new ArrayList<>();

				setViewportView(panel);
				setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

				/* dummy panel to fill extra space */
				panel.add(Utils.fillerComp(), Utils.gbConstraints(0, 100, 2, 1, GridBagConstraints.BOTH, 1, 1));
			}

			void addEntityButton(Object entity) {
				addEntityButton(new EntityButton(entity));
			}

			void addEntityButton(EntityButton button) {
				int x = buttons.size() % 2, y = buttons.size() / 2;
				GridBagConstraints c = Utils.gbConstraints(x, y, 1, 1);
				c.anchor = GridBagConstraints.NORTH;
				c.weightx = 1;
				panel.add(button, c);
				buttons.add(button);
			}

			void setSelect(boolean select) {
				if (select)
					selectButton(buttons.isEmpty() ? null : buttons.get(0));
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

			panel.add(Utils.newButton("Reset", e -> new ResetDialog().setVisible(true)));

			panel.add(Utils.newButton("Load", e -> {
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
						globals.logger.dbgln("failed to load file from: ", selectedFile);
						ex.printStackTrace();
					}
				}
			}));

			panel.add(Utils.newButton("Save", e -> {
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
						globals.logger.dbgln("failed to save level to file: ", selectedFile);
						ex.printStackTrace();
					}
				}
			}));

			panel.add(Utils.newButton("Main Menu", e -> {
				// TODO ask the user if he sure, does he want to save?
				globals.frame.openMainMenu();
			}));

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
				if (width < 0 || height < 0)
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

		private final Event.Register register = new Event.Register();

		private static final long serialVersionUID = 1L;

		@Override
		EntityLayer createEntityLayer() {
			return new EntityLayer(this);
		}

		ArenaPanel(Globals globals) {
			super(globals);
			register.register(builder.onResetChange, e -> reset());
			register.register(entityLayer.onTileClick, e -> cellClicked(e.cell));

			tickTaskManager.start();
		}

		EntityLayer entityLayer() {
			return (EntityLayer) entityLayer;
		}

		void reset() {
			entityLayer().reset();
			updateArenaSize(builder.width(), builder.height());
			mapMove.setPos(Position.of(0, 0));
		}

		@Override
		public void clear() {
			register.unregisterAll();
			super.clear();
		}

		private void cellClicked(int cell) {
			Object selectedObj = menu.selectedButton.entity;

			if (selectedObj instanceof Terrain.Category terrainc) {
				List<Terrain> terrains = terrainc.getTerrains();
				int oldTerrainIdx = terrains.indexOf(builder.terrain(cell)); /* might be -1 */
				Terrain terrain = terrains.get((oldTerrainIdx + 1) % terrains.size());

				builder.setTerrain(cell, terrain);

			} else if (selectedObj instanceof BuildingDesc building) {
				builder.setBuilding(cell, BuildingDesc.copyOf(building));
				// TODO else user message

			} else if (selectedObj instanceof UnitDesc unit) {
				unit = UnitDesc.copyOf(unit);
				UnitDesc oldUnit = builder.unit(cell);
				if (oldUnit != null && oldUnit.team == unit.team && oldUnit.type.transportUnits
						&& !unit.type.transportUnits && unit.type.category == Category.Land)
					unit = UnitDesc.transporter(oldUnit.type, unit);
				builder.setUnit(cell, unit);
				// TODO else user message

			} else if (selectedObj == SideMenu.removeBuildingObj) {
				builder.setBuilding(cell, null);

			} else if (selectedObj == SideMenu.removeUnitObj) {
				builder.setUnit(cell, null);

			} else {
				throw new IllegalArgumentException("Unknown menu selected object: " + selectedObj);
			}
		}

		@Override
		Terrain getTerrain(int cell) {
			return builder.terrain(cell);
		}

		private class EntityLayer
				extends ArenaPanelAbstract.EntityLayer<EntityLayer.TerrainComp, BuildingComp, EntityLayer.UnitComp> {

			private static final long serialVersionUID = 1L;

			private final Event.Register register = new Event.Register();

			EntityLayer(ArenaPanel arena) {
				super(arena);

				register.register(builder.onTileChange, e -> {
					TerrainComp comp = (TerrainComp) comps.computeIfAbsent(terrainKey(e.cell),
							k -> new TerrainComp(e.cell));
					comp.tileUpdate();
				});
			}

			void reset() {
				removeAllArenaComps();

				for (Iter.Int it = Cell.Iter2D.of(builder.width(), builder.height()); it.hasNext();) {
					int cell = it.next();
					TerrainComp tileComp = new TerrainComp(cell);
					comps.put(terrainKey(cell), tileComp);
					tileComp.tileUpdate();
				}
			}

			private final Map<Integer, Object> terrainKeys = new HashMap<>();

			private Object terrainKey(int cell) {
				return terrainKeys.computeIfAbsent(Integer.valueOf(cell), k -> new Object());
			}

			private class TerrainComp extends ArenaPanelAbstract.TerrainComp {

				BuildingComp buildingComp;
				UnitComp unitComp;

				TerrainComp(int cell) {
					super(ArenaPanel.this, cell);
				}

				void tileUpdate() {
					BuildingDesc building = builder.building(pos);
					if (buildingComp != null && building != buildingComp.building()) {
						comps.remove(buildingComp.building()).clear();
						buildingComp = null;
					}
					if (building != null && buildingComp == null)
						comps.put(building, buildingComp = new BuildingComp(ArenaPanel.this, pos, building));

					UnitDesc unit = builder.unit(pos);
					if (unitComp != null && unit != unitComp.unit()) {
						comps.remove(unitComp.unit()).clear();
						unitComp = null;
					}
					if (unit != null && unitComp == null)
						comps.put(unit, unitComp = new UnitComp(ArenaPanel.this, pos, unit));
				}

			}

			private class UnitComp extends ArenaPanelAbstract.UnitComp {

				UnitComp(ArenaPanelAbstract<?, ?, ?> arena, int pos, UnitDesc unit) {
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
