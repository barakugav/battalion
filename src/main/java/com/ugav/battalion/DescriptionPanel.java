package com.ugav.battalion;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Direction;
import com.ugav.battalion.core.IUnit;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Pair;

class DescriptionPanel extends JPanel implements Clearable {

	private Object shownObj;
	private final DescriptionSubPanel emptyPanel;
	private final TerrainPanel terrainPanel;
	private final BuildingPanel buildingPanel;
	private final UnitsPanel unitPanel;
	private final Event.Register register = new Event.Register();

	private static final Color BackgroundColor = new Color(80, 79, 80);
	private static final Color TextColor = new Color(245, 245, 245);
	private static final long serialVersionUID = 1L;

	DescriptionPanel(GameWindow window) {
		setLayout(new CardLayout());
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		setBackground(BackgroundColor);

		add(emptyPanel = new DescriptionSubPanel("empty"), emptyPanel.name);
		add(terrainPanel = new TerrainPanel(), terrainPanel.name);
		add(buildingPanel = new BuildingPanel(), buildingPanel.name);
		add(unitPanel = new UnitsPanel(), unitPanel.name);
		showPanel(emptyPanel);

		register.register(window.arenaPanel.onEntityClick, e -> showObject(e.obj));
		register.register(window.game.onEntityChange, e -> {
			if (e.source() == shownObj) {
				if (e.source() instanceof Unit u && u.isDead()) {
					showObject(null);
				} else {
					showObject(e.source());
				}
			}
		});
	}

	private void showObject(Object obj) {
		if (obj instanceof Terrain terrain) {
			showPanel(terrainPanel);
			terrainPanel.showTerrain(terrain);
			shownObj = terrain;

		} else if (obj instanceof Building building) {
			showPanel(buildingPanel);
			buildingPanel.showBuilding(building);
			shownObj = building;

		} else if (obj instanceof Unit unit) {
			showPanel(unitPanel);
			unitPanel.showUnit(unit);
			shownObj = unit;

		} else {
			showPanel(emptyPanel);
			shownObj = null;
		}
	}

	private void showPanel(DescriptionSubPanel panel) {
		((CardLayout) getLayout()).show(this, panel.name);
	}

	@Override
	public void clear() {
		register.unregisterAll();
	}

	private static class DescriptionSubPanel extends JPanel {

		final String name;

		private static final long serialVersionUID = 1L;

		DescriptionSubPanel(String name) {
			this.name = Objects.requireNonNull(name);
			setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			setBackground(BackgroundColor);
		}

	}

	private static class TerrainPanel extends DescriptionSubPanel {

		private final JLabel title;
		private final JTextArea text;
		private final JLabel image;

		private static final long serialVersionUID = 1L;

		TerrainPanel() {
			super("TerrainDescription");
			setLayout(new GridBagLayout());

			title = new JLabel("", SwingConstants.CENTER);
			Font titleFont = title.getFont();
			Font titleFontNew = new Font(titleFont.getName(), Font.BOLD, titleFont.getSize());
			title.setFont(titleFontNew);
			title.setForeground(TextColor);

			text = new JTextArea();
			text.setEditable(false);
			text.setWrapStyleWord(true);
			text.setLineWrap(true);
			text.setOpaque(false);
			text.setForeground(TextColor);
			image = new JLabel();
			JLabel techs = new JLabel();

			GridBagConstraints c = new GridBagConstraints();

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2;
			c.gridheight = 1;
			c.weighty = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(title, c);
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 2;
			c.gridheight = 1;
			c.weighty = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			add(text, c);
			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.weighty = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(image, c);
			c.gridx = 1;
			c.gridy = 2;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.weighty = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(techs, c);

		}

		private void showTerrain(Terrain terrain) {
			title.setText(terrain.category.toString());
			text.setText("description about " + terrain.category.toString());
			image.setIcon(new ImageIcon(Images.Terrains.getDefault(terrain)));
		}

	}

	private static class BuildingPanel extends DescriptionSubPanel {

		private final JLabel title;
		private final JTextArea text;
		private final JLabel image;

		private static final long serialVersionUID = 1L;

		BuildingPanel() {
			super("BuildingDescription");
			setLayout(new GridBagLayout());

			title = new JLabel("", SwingConstants.CENTER);
			Font titleFont = title.getFont();
			Font titleFontNew = new Font(titleFont.getName(), Font.BOLD, titleFont.getSize());
			title.setFont(titleFontNew);
			title.setForeground(TextColor);

			text = new JTextArea();
			text.setEditable(false);
			text.setWrapStyleWord(true);
			text.setLineWrap(true);
			text.setOpaque(false);
			text.setForeground(TextColor);
			image = new JLabel();
			JLabel techs = new JLabel();

			GridBagConstraints c = new GridBagConstraints();

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2;
			c.gridheight = 1;
			c.weighty = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(title, c);
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 2;
			c.gridheight = 1;
			c.weighty = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			add(text, c);
			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.weighty = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(image, c);
			c.gridx = 1;
			c.gridy = 2;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.weighty = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(techs, c);
		}

		private void showBuilding(Building building) {
			title.setText(building.type.toString());
			text.setText("description about " + building.type.toString());
			image.setIcon(new ImageIcon(Images.Buildings.get(building, 0)));
		}

	}

	static class UnitsPanel extends DescriptionSubPanel {

		private static final long serialVersionUID = 1L;
		private final Map<Unit.Type, Pair<String, UnitPanel>> panels;

		UnitsPanel() {
			super("UnitDescription");
			setLayout(new CardLayout());

			panels = new HashMap<>();
			for (Unit.Type type : Unit.Type.values()) {
				String name = type.toString();
				UnitPanel panel = new UnitPanel(type);
				add(panel, name);
				panels.put(type, Pair.of(name, panel));
			}
		}

		void showUnit(IUnit unit) {
			Pair<String, UnitPanel> panel = panels.get(unit.getType());
			panel.e2.showUnit(unit);
			((CardLayout) getLayout()).show(this, panel.e1);
		}

		private static class UnitPanel extends JPanel {

			private final Unit.Type type;

			private final JLabel image;
			private final JLabel health;

			private static final long serialVersionUID = 1L;

			UnitPanel(Unit.Type type) {
				this.type = type;

				setLayout(new GridBagLayout());
				setOpaque(false);

				JLabel title = new JLabel(type.toString(), SwingConstants.CENTER);
				Font titleFont = title.getFont();
				Font boldFont = new Font(titleFont.getName(), Font.BOLD, titleFont.getSize());
				title.setFont(boldFont);
				title.setForeground(TextColor);

				image = new JLabel();
				updateImg(Team.Red);

				JPanel stats = new JPanel(new GridBagLayout());
				stats.setOpaque(false);
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.weighty = c.gridheight = 1;
				c.weightx = c.gridwidth = 1;
				c.fill = GridBagConstraints.BOTH;
				JLabel healthLabel = new JLabel("Health:");
				healthLabel.setForeground(TextColor);
				healthLabel.setFont(boldFont);
				c.gridy = 0;
				stats.add(healthLabel, c);
				health = new JLabel("", SwingConstants.RIGHT);
				health.setForeground(TextColor);
				c.gridy = 1;
				stats.add(health, c);
				JLabel damageLabel = new JLabel("Damage:");
				damageLabel.setForeground(TextColor);
				damageLabel.setFont(boldFont);
				c.gridy = 2;
				stats.add(damageLabel, c);
				JLabel damage = new JLabel(Integer.toString(type.damage), SwingConstants.RIGHT);
				damage.setForeground(TextColor);
				c.gridy = 3;
				stats.add(damage, c);
				JLabel moveLabel = new JLabel("Move:");
				moveLabel.setForeground(TextColor);
				moveLabel.setFont(boldFont);
				c.gridy = 4;
				stats.add(moveLabel, c);
				JLabel move = new JLabel(Integer.toString(type.moveLimit), SwingConstants.RIGHT);
				move.setForeground(TextColor);
				c.gridy = 5;
				stats.add(move, c);

				JTextArea text = new JTextArea();
				text.setEditable(false);
				text.setWrapStyleWord(true);
				text.setLineWrap(true);
				text.setOpaque(false);
				text.setForeground(TextColor);
				text.setText("description about " + type);
				text.setPreferredSize(new Dimension(100, 32));
				JLabel techs = new JLabel();

				c.gridx = 0;
				c.gridy = 0;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.weighty = 0;
				c.fill = GridBagConstraints.HORIZONTAL;
				add(title, c);
				c.gridx = 0;
				c.gridy = 1;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.weighty = 0;
				c.fill = GridBagConstraints.HORIZONTAL;
				add(image, c);
				c.gridx = 1;
				c.gridy = 1;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.weighty = 0;
				c.fill = GridBagConstraints.HORIZONTAL;
				add(stats, c);
				c.gridx = 0;
				c.gridy = 2;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.weighty = 1;
				c.weightx = 1;
				c.fill = GridBagConstraints.BOTH;
				add(text, c);
				c.gridx = 0;
				c.gridy = 3;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.weighty = 0;
				c.fill = GridBagConstraints.HORIZONTAL;
				add(techs, c);
			}

			void showUnit(IUnit unit) {
				if (unit.getType() != type)
					throw new IllegalArgumentException();
				int health0 = unit instanceof Unit u ? u.getHealth() : unit.getType().health;
				health.setText("" + health0 + "/" + unit.getType().health);
				updateImg(unit.getTeam());
			}

			private void updateImg(Team team) {
				image.setIcon(new ImageIcon(Images.Units.standImg(type, team, Direction.XPos, 0)));
			}
		}
	}
}