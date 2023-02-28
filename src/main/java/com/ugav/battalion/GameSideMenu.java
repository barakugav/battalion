package com.ugav.battalion;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.ugav.battalion.core.Arena;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Direction;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.Utils;

public class GameSideMenu extends JPanel implements Clearable {

	private final GameWindow window;
	private final Map<Team, JLabel> labelMoney;
	private final DataChangeRegister register = new DataChangeRegister();

	private static final long serialVersionUID = 1L;

	GameSideMenu(GameWindow window) {
		this.window = Objects.requireNonNull(window);

		labelMoney = new HashMap<>();

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;

		c.gridy = 0;
		c.weighty = 0;
		add(createMinimapPanel(), c);
		c.gridy = 2;
		c.weighty = 2;
		add(createTeamsPanel(), c);
		c.gridy = 4;
		c.weighty = 2;
		add(new DescriptionPanel(), c);
		c.gridy = 6;
		c.weighty = 1;
		add(createButtonsPannel(), c);

		for (Team team : Team.realTeams)
			updateMoneyLabel(team, window.game.getMoney(team));
	}

	private JPanel createMinimapPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(Color.BLACK);
		panel.setOpaque(true);

		MiniMap miniMap = new MiniMap();
		panel.add(miniMap, Utils.gbConstraints(0, 0, 1, 1));

		Dimension miniMapSize = miniMap.getPreferredSize();
		if (miniMapSize.width > 150 || miniMapSize.height > 150)
			throw new IllegalArgumentException("Map too big for minimap");
		panel.setPreferredSize(new Dimension(150, 150));

		return panel;
	}

	private JPanel createTeamsPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

		for (int teamIdx = 0; teamIdx < Team.realTeams.size(); teamIdx++) {
			Team team = Team.realTeams.get(teamIdx);
			JPanel teamPanel = new JPanel(new GridBagLayout());
			teamPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
			GridBagConstraints c = new GridBagConstraints();

			JPanel colorBox = new JPanel();
			colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
			colorBox.setBackground(getTeamColor(team));
			c.gridx = 0;
			c.gridy = 0;
			c.gridheight = 2;
			c.weightx = 0;
			c.fill = GridBagConstraints.BOTH;
			teamPanel.add(colorBox, c);

			// TODO images
			Image img = Images.getUnitImgStand(UnitDesc.of(Unit.Type.Soldier, team), Direction.XPos, 0);
			img = img.getScaledInstance(28, 28, Image.SCALE_SMOOTH);
			JLabel icon = new JLabel(new ImageIcon(img));
			c.gridx = 1;
			c.gridy = 0;
			c.gridheight = 2;
			c.weightx = 0;
			c.fill = GridBagConstraints.BOTH;
			teamPanel.add(icon, c);

			JLabel label = new JLabel(team.toString());
			Font labelFont = label.getFont();
			label.setFont(new Font(labelFont.getName(), labelFont.getStyle(), 10));
			c.gridx = 5;
			c.gridy = 0;
			c.gridheight = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			teamPanel.add(label, c);

			JLabel money = new JLabel("$0", SwingConstants.RIGHT);
			Font moneyFont = label.getFont();
			money.setFont(new Font(moneyFont.getName(), moneyFont.getStyle(), 10));
			labelMoney.put(team, money);
			c.gridx = 5;
			c.gridy = 1;
			c.gridheight = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			teamPanel.add(money, c);

			c = Utils.gbConstraints(0, teamIdx, 1, 1, GridBagConstraints.HORIZONTAL);
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = teamIdx;
			c.weightx = c.gridwidth = 1;
			c.gridheight = 1;
			c.weighty = 0;
			c.fill = GridBagConstraints.BOTH;
			panel.add(teamPanel, c);
		}

		/* Dummy panel to take extra vertical space */
		GridBagConstraints c = Utils.gbConstraints(0, Team.realTeams.size(), 1, 1, GridBagConstraints.BOTH);
		c.weighty = 100;
		panel.add(new JPanel(), c);

		return panel;
	}

	private static Color getTeamColor(Team team) {
		switch (team) {
		case Red:
			return Color.RED;
		case Blue:
			return Color.BLUE;
		default:
			throw new IllegalArgumentException("Unexpected value: " + team);
		}
	}

	private class DescriptionPanel extends JPanel {

		private Object shownObj;
		private final CardLayout layout;
		private final JPanel emptyPanel;
		private final DescriptionTerrainPanel terrainPanel;
		private final DescriptionBuildingPanel buildingPanel;
		private final DescriptionUnitPanel unitPanel;
		private final Map<JPanel, String> panelsNames = new IdentityHashMap<>();

		private static final long serialVersionUID = 1L;

		DescriptionPanel() {
			setLayout(layout = new CardLayout());
			setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

			panelsNames.put(emptyPanel = new JPanel(), "empty");
			panelsNames.put(terrainPanel = new DescriptionTerrainPanel(), "terrain");
			panelsNames.put(buildingPanel = new DescriptionBuildingPanel(), "building");
			panelsNames.put(unitPanel = new DescriptionUnitPanel(), "unit");
			for (JPanel panel : panelsNames.keySet())
				add(panel, panelsNames.get(panel));
			showPanel(emptyPanel);

			register.register(window.arenaPanel.onEntityClick, e -> showObject(e.obj));
			register.register(window.game.arena().onEntityChange, e -> {
				if (e.source() == shownObj)
					showObject(e.source());
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

		private void showPanel(JPanel panel) {
			layout.show(this, Objects.requireNonNull(panelsNames.get(panel)));
		}
	}

	private class DescriptionTerrainPanel extends JPanel {

		private final JLabel title;
		private final JTextArea text;
		private final JLabel image;

		private static final long serialVersionUID = 1L;

		DescriptionTerrainPanel() {
			super(new GridBagLayout());

			title = new JLabel("", SwingConstants.CENTER);
			Font titleFont = title.getFont();
			Font titleFontNew = new Font(titleFont.getName(), Font.BOLD, titleFont.getSize());
			title.setFont(titleFontNew);

			text = new JTextArea();
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

	private class DescriptionBuildingPanel extends JPanel {

		private final JLabel title;
		private final JTextArea text;
		private final JLabel image;

		private static final long serialVersionUID = 1L;

		DescriptionBuildingPanel() {
			super(new GridBagLayout());

			title = new JLabel("", SwingConstants.CENTER);
			Font titleFont = title.getFont();
			Font titleFontNew = new Font(titleFont.getName(), Font.BOLD, titleFont.getSize());
			title.setFont(titleFontNew);

			text = new JTextArea();
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

	private class DescriptionUnitPanel extends JPanel {

		private final JLabel title;
		private final JLabel image;
		private final JLabel health;
		private final JLabel damage;
		private final JLabel move;
		private final JTextArea text;

		private static final long serialVersionUID = 1L;

		DescriptionUnitPanel() {
			super(new GridBagLayout());

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

		private void showUnit(Unit unit) {
			title.setText(unit.type.toString());
			image.setIcon(new ImageIcon(Images.getUnitImgStand(unit, Direction.XPos, 0)));
			health.setText("" + unit.getHealth() + "/" + unit.type.health);
			damage.setText("" + unit.type.damage);
			move.setText("" + unit.type.moveLimit);
			text.setText("description about " + unit.type.toString());
		}

	}

	private JPanel createButtonsPannel() {
		JPanel panel = new JPanel(new GridLayout(0, 1));

		JButton buttonEndTurn = new JButton("End Turn");
		buttonEndTurn.addActionListener(onlyIfActionsEnabled(e -> window.endTurn()));
		JButton buttonMainMenu = new JButton("Main Menu");
		buttonMainMenu.addActionListener(onlyIfActionsEnabled(e -> window.globals.frame.openMainMenu()));

		panel.add(buttonEndTurn);
		panel.add(buttonMainMenu);

		return panel;
	}

	void initGame() {
		register.register(window.game.onMoneyChange(), e -> updateMoneyLabel(e.team, e.newAmount));
	}

	@Override
	public void clear() {
		register.unregisterAll();
	}

	private void updateMoneyLabel(Team team, int money) {
		labelMoney.get(team).setText("$" + money);
	}

	private ActionListener onlyIfActionsEnabled(ActionListener l) {
		return e -> {
			if (!window.isActionSuspended())
				l.actionPerformed(e);
		};
	}

	private class MiniMap extends JPanel {

		private static final long serialVersionUID = 1L;

		private final Arena arena;
		private static final int TileSize = 6;
		private static final Color CurrentMapColor = Color.YELLOW;

		MiniMap() {
			arena = window.game.arena();
			setPreferredSize(new Dimension(arena.width() * TileSize, arena.height() * TileSize));

			window.arenaPanel.tickTaskManager.addTask(1000, this::repaint);
		}

		private void drawImg(Graphics g, int cell, BufferedImage img) {
			g.drawImage(img, Cell.x(cell) * TileSize, Cell.y(cell) * TileSize, null);
		}

		@Override
		public void paintComponent(Graphics g) {
			for (Iter.Int it = arena.cells(); it.hasNext();) {
				int cell = it.next();
				Terrain terrain = arena.terrain(cell);
				drawImg(g, cell, Images.getMinimapTerrain(terrain.category));

				Building building = arena.building(cell);
				if (building != null)
					drawImg(g, cell, Images.getMinimapBuilding(building.getTeam()));

				Unit unit = arena.unit(cell);
				if (unit != null)
					drawImg(g, cell, Images.getMinimapUnit(unit.getTeam()));
			}

			Position currentMapPos = window.arenaPanel.getCurrentMapOrigin();
			int x = (int) (currentMapPos.x * TileSize);
			int y = (int) (currentMapPos.y * TileSize);
			int width = TileSize * ArenaPanelAbstract.DISPLAYED_ARENA_WIDTH - 1;
			int height = TileSize * ArenaPanelAbstract.DISPLAYED_ARENA_HEIGHT - 1;
			g.setColor(CurrentMapColor);
			g.drawRect(x, y, width, height);
		}

	}

}
