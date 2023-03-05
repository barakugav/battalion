package com.ugav.battalion;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Event;

class DescriptionPanel extends JPanel implements Clearable {

	private Object shownObj;
	private final TerrainPanel terrainPanel;
	private final BuildingPanel buildingPanel;
	private final UnitPanel unitPanel;
	private final Event.Register register = new Event.Register();

	private static final String EmptyPanel = "empty";
	private static final long serialVersionUID = 1L;

	DescriptionPanel(GameWindow window) {
		setLayout(new CardLayout());
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

		add(new JPanel(), EmptyPanel);
		add(terrainPanel = new TerrainPanel(), terrainPanel.name);
		add(buildingPanel = new BuildingPanel(), buildingPanel.name);
		add(unitPanel = new UnitPanel(), unitPanel.name);
		showPanel(null);

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
			showPanel(null);
			shownObj = null;
		}
	}

	private void showPanel(AbstractDescriptionSubPanel panel) {
		((CardLayout) getLayout()).show(this, panel != null ? panel.name : EmptyPanel);
	}

	@Override
	public void clear() {
		register.unregisterAll();
	}

	private static class AbstractDescriptionSubPanel extends JPanel {

		final String name;

		private static final long serialVersionUID = 1L;

		AbstractDescriptionSubPanel(String name) {
			this.name = Objects.requireNonNull(name);
		}

	}

	private static class TerrainPanel extends AbstractDescriptionSubPanel {

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

			text = new JTextArea();
			text.setEditable(false);
			text.setWrapStyleWord(true);
			text.setLineWrap(true);
			text.setOpaque(false);
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
			image.setIcon(new ImageIcon(Images.getImg(terrain)));
		}

	}

	private static class BuildingPanel extends AbstractDescriptionSubPanel {

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

			text = new JTextArea();
			text.setEditable(false);
			text.setWrapStyleWord(true);
			text.setLineWrap(true);
			text.setOpaque(false);
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
			image.setIcon(new ImageIcon(Images.getBuildingImg(building, 0)));
		}

	}

	static class UnitPanel extends AbstractDescriptionSubPanel {

		private final JLabel title;
		private final JLabel image;
		private final JLabel health;
		private final JLabel damage;
		private final JLabel move;
		private final JTextArea text;

		private static final long serialVersionUID = 1L;

		UnitPanel() {
			super("UnitDescription");
			setLayout(new GridBagLayout());

			title = new JLabel("", SwingConstants.CENTER);
			Font titleFont = title.getFont();
			Font boldFont = new Font(titleFont.getName(), Font.BOLD, titleFont.getSize());
			title.setFont(boldFont);

			image = new JLabel();

			JPanel stats = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.weighty = c.gridheight = 1;
			c.weightx = c.gridwidth = 1;
			c.fill = GridBagConstraints.BOTH;
			JLabel healthLabel = new JLabel("Health:");
			healthLabel.setFont(boldFont);
			c.gridy = 0;
			stats.add(healthLabel, c);
			health = new JLabel();
			c.gridy = 1;
			stats.add(health, c);
			JLabel damageLabel = new JLabel("Damage:");
			damageLabel.setFont(boldFont);
			c.gridy = 2;
			stats.add(damageLabel, c);
			damage = new JLabel();
			c.gridy = 3;
			stats.add(damage, c);
			JLabel moveLabel = new JLabel("Move:");
			moveLabel.setFont(boldFont);
			c.gridy = 4;
			stats.add(moveLabel, c);
			move = new JLabel();
			c.gridy = 5;
			stats.add(move, c);

			text = new JTextArea();
			text.setEditable(false);
			text.setWrapStyleWord(true);
			text.setLineWrap(true);
			text.setOpaque(false);
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
			int health0 = unit instanceof Unit u ? u.getHealth() : unit.getType().health;

			title.setText(unit.getType().toString());
			image.setIcon(new ImageIcon(Images.getUnitImgStand(unit, Direction.XPos, 0)));
			health.setText("" + health0 + "/" + unit.getType().health);
			damage.setText("" + unit.getType().damage);
			move.setText("" + unit.getType().moveLimit);
			text.setText("description about " + unit.getType().toString());
		}
	}
}