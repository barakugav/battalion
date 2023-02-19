package com.ugav.battalion;

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
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.ugav.battalion.core.Arena;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Position.Direction;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Tile;
import com.ugav.battalion.core.Unit;

public class GameSideMenu extends JPanel implements Clearable {

	private final GameWindow window;
	private final Map<Team, JLabel> labelMoney;
	private final DataChangeRegister register = new DataChangeRegister();

	private final JPanel minimapPanel;
	private final JPanel teamsPanel;
	private final JPanel descriptionPanel;
	private final JPanel buttonsPanel;

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
		c.weighty = 2;
		add(minimapPanel = createMinimapPanel(), c);
		c.gridy = 2;
		c.weighty = 2;
		add(teamsPanel = createTeamsPanel(), c);
		c.gridy = 4;
		c.weighty = 2;
		add(descriptionPanel = createDescriptionPanel(), c);
		c.gridy = 6;
		c.weighty = 1;
		add(buttonsPanel = createButtonsPannel(), c);

		for (Team team : Team.realTeams)
			updateMoneyLabel(team, window.game.getMoney(team));
	}

	private JPanel createMinimapPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

		panel.add(new MiniMap());

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

	private JPanel createDescriptionPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		panel.add(new JLabel("Description"));
		return panel;
	}

	private JPanel createButtonsPannel() {
		JPanel panel = new JPanel(new GridLayout(0, 1));
		panel.setBackground(Color.yellow);

		JButton buttonEndTurn = new JButton("End Turn");
		buttonEndTurn.addActionListener(onlyIfActionsEnabled(e -> window.endTurn()));
		JButton buttonMainMenu = new JButton("Main Menu");
		buttonMainMenu.addActionListener(onlyIfActionsEnabled(e -> window.globals.frame.displayMainMenu()));

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

		private void drawImg(Graphics g, Position pos, BufferedImage img) {
			g.drawImage(img, pos.xInt() * TileSize, pos.yInt() * TileSize, null);
		}

		@Override
		public void paintComponent(Graphics g) {
			for (Position pos : arena.positions()) {
				Tile tile = arena.at(pos);

				Terrain terrain = tile.getTerrain();
				drawImg(g, pos, Images.getMinimapTerrain(terrain.category));

				if (tile.hasBuilding()) {
					Building building = tile.getBuilding();
					drawImg(g, pos, Images.getMinimapBuilding(building.getTeam()));
				}

				if (tile.hasUnit()) {
					Unit unit = tile.getUnit();
					drawImg(g, pos, Images.getMinimapUnit(unit.getTeam()));
				}
			}

			Position currentMapPos = window.arenaPanel.getCurrentMapOrigin();
			int x = (int) (currentMapPos.x / ArenaPanelAbstract.TILE_SIZE_PIXEL * TileSize);
			int y = (int) (currentMapPos.y / ArenaPanelAbstract.TILE_SIZE_PIXEL * TileSize);
			int width = TileSize * ArenaPanelAbstract.DISPLAYED_ARENA_WIDTH;
			int height = TileSize * ArenaPanelAbstract.DISPLAYED_ARENA_HEIGHT;
			g.setColor(CurrentMapColor);
			g.drawRect(x, y, width, height);
		}

	}

}
