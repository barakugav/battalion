package com.ugav.battalion;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import com.ugav.battalion.util.Utils;

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
			Font textFont = text.getFont();
			textFont = new Font(textFont.getName(), textFont.getStyle(), 9);
			text.setFont(textFont);
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
			text.setText(getDescriptionText(building.type));
			image.setIcon(new ImageIcon(Images.Buildings.get(building, 0)));
		}

		/**
		 * ### Input for ChatGPT:
		 *
		 * I wrote a 2d strategy turn based grid game, in which the player control an
		 * army and try to destroy the enemy units. There are many types of units, such
		 * as tanks, soldiers, airplanes and ships. There are also buildings which allow
		 * the player and the enemy to gain money and build more units. Also, buildings
		 * can be conquered from one side to another. During the game, i present a side
		 * bar including additional information for the building currently selected. Can
		 * you provide a three sentences description for each of the following building?
		 *
		 * OilRefinery - produce money each turn by extracting oil from the ground.
		 *
		 * OilProcessingPlant - a bigger version of 'OilRefinery', producing more money.
		 *
		 * OilRig - offshore building producing money each turn by extracting oil from
		 * the sea bottom.
		 *
		 * Factory - building allowing you to produce new units for the cost of dollars.
		 *
		 * Capital - a building you must protect. If the building is captured, you lose.
		 * But you can win by conquering the enemy Capital building.
		 *
		 * LandResearchFacility - building giving you the technologics to build land
		 * units in the factory.
		 *
		 * NavalControlCenter - building giving you the technologics to build water
		 * units in the factory.
		 *
		 * SkyOperationsHub - building giving you the technologics to build air units in
		 * the factory.
		 */

		private static String getDescriptionText(Building.Type type) {
			switch (type) {
			case OilRefinery:
				return "A crucial building that generates money each turn by extracting oil from the ground."
						+ " This building provides a steady source of income that allows buildings and maintaining an army.";
			case OilProcessingPlant:
				return "A larger and more advanced version of the " + Building.Type.OilRefinery
						+ ", producing even more money each turn. Controlling it allows building a larger and more diverse army.";
			case OilRig:
				return "An offshore building that produces money each turn by extracting oil from the sea bottom."
						+ " It provides the highest income building, conquer and defend it to maximize income.";
			case Factory:
				return "A building that allows producing new units using money."
						+ " This building is essential for maintaining and growing an army throughout wars.";
			case Capital:
				return "A critical building that must be protected at all cost."
						+ " If the building is captured by the enemy, the army will disassemble and lose.";
			case LandResearchFacility:
				return "A building that provides an army with the technology to build land units in factories."
						+ " By conquring this building, one can unlock new and more advanced units to use in their army.";
			case NavalControlCenter:
				return "A building that provides an army with the technology to build water units in factories."
						+ " This building is crucial for commanders who want to dominate the seas and launch amphibious assaults on their enemies.";
			case SkyOperationsHub:
				return "A building that provides an army with the technology to build air units in factories."
						+ " This building allows a commander to take to the skies and launch devastating air strikes on the enemies";
			default:
				throw new IllegalArgumentException("Unexpected value: " + type);
			}
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

			private static final int ImgWidth;
			private static final int ImgHeight;
			static {
				int maxw = 0, maxh = 0;
				for (Unit.Type type : Unit.Type.values()) {
					ImageIcon icon = unitIcon(type, Team.Red);
					maxw = Math.max(maxw, icon.getIconWidth());
					maxh = Math.max(maxh, icon.getIconHeight());
				}
				ImgWidth = maxw;
				ImgHeight = maxh;
			}

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
				image.setPreferredSize(new Dimension(ImgWidth, ImgHeight));
				image.setIcon(unitIcon(type, Team.Red));

				JPanel stats = new JPanel(new GridBagLayout());
				stats.setOpaque(false);
				JLabel healthLabel = new JLabel("Health:");
				healthLabel.setForeground(TextColor);
				healthLabel.setFont(boldFont);
				stats.add(healthLabel, Utils.gbConstraints(0, 0, 1, 1, GridBagConstraints.BOTH, 1, 0));
				health = new JLabel("100/100", SwingConstants.RIGHT);
				health.setForeground(TextColor);
				stats.add(health, Utils.gbConstraints(1, 0, 1, 1, GridBagConstraints.BOTH, 0, 0));
				JLabel damageLabel = new JLabel("Damage:");
				damageLabel.setForeground(TextColor);
				damageLabel.setFont(boldFont);
				stats.add(damageLabel, Utils.gbConstraints(0, 1, 1, 1, GridBagConstraints.BOTH, 1, 0));
				JLabel damage = new JLabel(Integer.toString(type.damage), SwingConstants.RIGHT);
				damage.setForeground(TextColor);
				stats.add(damage, Utils.gbConstraints(1, 1, 1, 1, GridBagConstraints.BOTH, 0, 0));
				JLabel moveLabel = new JLabel("Move:");
				moveLabel.setForeground(TextColor);
				moveLabel.setFont(boldFont);
				stats.add(moveLabel, Utils.gbConstraints(0, 2, 1, 1, GridBagConstraints.BOTH, 1, 0));
				JLabel move = new JLabel(Integer.toString(type.moveLimit), SwingConstants.RIGHT);
				move.setForeground(TextColor);
				stats.add(move, Utils.gbConstraints(1, 2, 1, 1, GridBagConstraints.BOTH, 0, 0));

				JTextArea text = new JTextArea();
				text.setEditable(false);
				text.setWrapStyleWord(true);
				text.setLineWrap(true);
				text.setOpaque(false);
				text.setForeground(TextColor);
				text.setText(getDescripitonText(type));
				Font textFont = text.getFont();
				textFont = new Font(textFont.getName(), textFont.getStyle(), 9);
				text.setFont(textFont);
				text.setPreferredSize(new Dimension(100, 60));
				JLabel techs = new JLabel();

				add(title, Utils.gbConstraints(0, 0, 2, 1, GridBagConstraints.HORIZONTAL, 0, 0));
				GridBagConstraints imgConstraints = Utils.gbConstraints(0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 0,
						0);
				imgConstraints.insets = new Insets(3, 3, 3, 3);
				add(image, imgConstraints);
				add(stats, Utils.gbConstraints(1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0));
				add(text, Utils.gbConstraints(0, 2, 2, 1, GridBagConstraints.BOTH, 1, 1));
				add(techs, Utils.gbConstraints(0, 3, 2, 1, GridBagConstraints.BOTH, 0, 0));
			}

			void showUnit(IUnit unit) {
				if (unit.getType() != type)
					throw new IllegalArgumentException();
				int health0 = unit instanceof Unit u ? u.getHealth() : unit.getType().health;
				health.setText("" + health0 + "/" + unit.getType().health);
				image.setIcon(unitIcon(type, unit.getTeam()));
			}

			private static ImageIcon unitIcon(Unit.Type type, Team team) {
				return new ImageIcon(Images.Units.standImg(type, team, Direction.XPos, 0));
			}

			private static String getDescripitonText(Unit.Type type) {

				/**
				 * ### Input for ChatGPT:
				 *
				 * I wrote a 2d strategy turn based grid game, in which the player control an
				 * army and try to destroy the enemy units. There are many types of units, such
				 * as tanks, soldiers, airplanes and ships. There are also buildings which allow
				 * the player and the enemy to gain money and build more units. Also, buildings
				 * can be conquered from one side to another. During the game, i present a side
				 * bar including additional information for the unit currently selected. What
				 * description should i present for each of the following units? Also please
				 * give a alternative creative name for each of them:
				 *
				 * Soldier - the most simplest soldier in the game with the smallest health and
				 * damage. Can conquer enemy buildings.
				 *
				 * Bazooka - a unit with little health that does more damage to units with heavy
				 * armor such as tanks. Can conquer enemy buildings.
				 *
				 * Tank - multi purpose unit that can travel long distance relative to a land
				 * unit and does decent damage
				 *
				 * HeavyTank - the most powerful and expensive unit and the game. High damage,
				 * high health, very costly.
				 *
				 * TankAntiAir - tank that can attack air units. has little damage compared to
				 * the other tanks types.
				 *
				 * Artillery - an artillery unit that can attack even if the distance is far.
				 * health is low.
				 *
				 * Mortar - similar to 'Artillery', but with less range and more damage.
				 *
				 * Turrent - a unit that can't move and shoot other units from a distance
				 *
				 * SpeedBoat - a unit that can't attack others, but can used to conquers areas
				 *
				 * Ship - a basic combat ship with medium damage. More expensive then a regular
				 * land unit.
				 *
				 * ShipAntiAir - a ship that can attack air units and submarines but have little
				 * damage compared to a regular ship.
				 *
				 * ShipArtillery - the most expensive sea unit available, attack units from a
				 * distance with high damage.
				 *
				 * Submarine - deep water vessel that is not visible to the enemy unless an
				 * enemy is positioned directly near it.
				 *
				 * ShipTransporter - a unit used to transport other units across the water. the
				 * unit itself can not attack other units.
				 *
				 * Airplane - an air unit able to travels above rivers and mountains. low health
				 * and medium damage. Can travel the most among all units.
				 *
				 * Zeppelin - an air unit similar to the 'Airplane' with higher damage but more
				 * expensive.
				 *
				 * AirTransporter - a unit used to transport other units across the air. the
				 * unit itself can not attack other units.
				 */

				switch (type) {
				case Rifleman:
					return "A basic infantry unit armed with a rifle."
							+ " Although it has low health and damage, it can conquer enemy buildings and hold ground.";
				case RocketSpecialist:
					return "A specialized infantry unit armed with a powerful anti-tank weapon."
							+ " They have low health but deal massive damage to heavily armored units like tanks.";
				case BattleTank:
					return "A versatile armored vehicle that can traverse long distances and deal decent damage to enemy units."
							+ " They have moderate health and can withstand some damage.";
				case TitanTank:
					return "A heavily armored tank that deals massive damage to enemy units."
							+ " They have high health and are expensive to produce.";
				case AATank:
					return "A tank equipped with anti-aircraft weapons that can take down enemy air units."
							+ " They have low damage compared to other tank types but are essential for defending against air attacks.";
				case StealthTank:
					return "??";
				case Artillery:
					return "A long-range artillery unit that can attack enemy units from a safe distance."
							+ " They have low health and are vulnerable to close-range attacks.";
				case Mortar:
					return "A shorter-range artillery unit that deals more damage than the Artillery but has a shorter range."
							+ " They have slightly higher health than Artillery units.";
				case Turrent:
					return "A stationary defensive unit that can fire at enemy units from a distance."
							+ " They have high damage and health but cannot move from their position.";
				case SpeedBoat:
					return "A fast watercraft that can be used to conquer territories and buildings."
							+ " They cannot attack enemy units.";
				case Corvette:
					return "A basic combat vessel that can deal moderate damage to enemy units."
							+ " They are more expensive than land units but provide an advantage in naval battles.";
				case AACruiser:
					return "A ship equipped with anti-aircraft weapons that can shoot down enemy air units and submarines."
							+ " They have lower damage than Corvette ships but are essential for defending against air attacks.";
				case Battleship:
					return "The most powerful naval unit, capable of dealing massive damage to enemy units from a long distance."
							+ " They are very expensive to produce and have high health.";
				case Submarine:
					return "A stealthy underwater vessel that can remain invisible to enemy units unless they are positioned directly nearby."
							+ " They have low health but can deal high damage to enemy ships.";
				case LandingCraft:
					return "A vessel used to transport land units across water."
							+ " They cannot attack enemy units but are essential for amphibious assaults.";
				case FighterPlane:
					return "A flying unit that can traverse above mountains and rivers to quickly reach different parts of the map."
							+ " They have low health and moderate damage.";
				case ZeppelinBomber:
					return "A large airship armed with powerful weapons that can deal massive damage to enemy units."
							+ " They have high health and are expensive to produce.";
				case TransportPlane:
					return "A flying unit used to transport land units across the map."
							+ " They cannot attack enemy units but are essential for airborne assaults.";
				default:
					throw new IllegalArgumentException("Unexpected value: " + type);
				}
			}
		}
	}
}