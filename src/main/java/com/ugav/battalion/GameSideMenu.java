package com.ugav.battalion;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Direction;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.Utils;

class GameSideMenu extends Menus.ColumnWithMargins implements Clearable {

	private final GameWindow window;
	private final DescriptionPanel descriptionPanel;
	private final Map<Team, JLabel> labelMoney;
	private final Event.Register register = new Event.Register();

	private static final Color BackgroundColor = new Color(64, 62, 64);
	private static final long serialVersionUID = 1L;

	GameSideMenu(GameWindow window) {
		setMargin(6);
		this.window = Objects.requireNonNull(window);

		labelMoney = new HashMap<>();

		setBackground(BackgroundColor);
		addComp(createMinimapPanel());
		addComp(createTeamsPanel());
		addComp(descriptionPanel = new DescriptionPanel(window), 1);
		addComp(createButtonsPannel());

		for (Team team : Team.realTeams)
			updateMoneyLabel(team, window.game.getMoney(team));
	}

	private JPanel createMinimapPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(Color.BLACK);
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		panel.setOpaque(true);

		MiniMap miniMap = new MiniMap();
		panel.add(miniMap, Utils.gbConstraints(0, 0, 1, 1, GridBagConstraints.NONE, 1, 1));

		JLabel lvlNameLabel = new JLabel(window.levelName, SwingConstants.CENTER);
		lvlNameLabel.setBackground(new Color(63, 0, 0));
		lvlNameLabel.setForeground(new Color(250, 250, 250));
		lvlNameLabel.setOpaque(true);
		panel.add(lvlNameLabel, Utils.gbConstraints(0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0));

		Dimension size = new Dimension(170, 150);
		panel.setPreferredSize(size);
		Dimension miniMapSize = miniMap.getPreferredSize();
		if (miniMapSize.width > size.width || miniMapSize.height > size.width)
			throw new IllegalArgumentException("Map too big for minimap");

		return panel;
	}

	private JPanel createTeamsPanel() {
		Menus.ColumnWithMargins panel = new Menus.ColumnWithMargins();
		final int margin = 3;
		panel.setMarginx(0);
		panel.setMarginy(margin);
		panel.setMarginOnlyBetween(true);
		panel.setBackground(new Color(80, 79, 80));
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

		for (Team team : Team.realTeams) {
			JPanel teamPanel = new JPanel(new GridBagLayout());
			teamPanel.setBackground(new Color(156, 156, 156));
			int topBorder = panel.getComponentCount() == 0 ? 0 : 2;
			teamPanel.setBorder(BorderFactory.createMatteBorder(topBorder, 0, 2, 0, Color.BLACK));
			GridBagConstraints c = new GridBagConstraints();

			JPanel colorBox = new JPanel();
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

			JLabel label = new JLabel(team.toString() + " Team");
			Font labelFont = label.getFont();
			label.setFont(new Font(labelFont.getName(), labelFont.getStyle(), 10));
			c.gridx = 5;
			c.gridy = 0;
			c.gridheight = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			teamPanel.add(label, c);

			JLabel money = new JLabel("", SwingConstants.RIGHT);
			money.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 2));
			Font moneyFont = label.getFont();
			money.setFont(new Font(moneyFont.getName(), moneyFont.getStyle(), 10));
			labelMoney.put(team, money);
			c.gridx = 5;
			c.gridy = 1;
			c.gridheight = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			teamPanel.add(money, c);

			panel.addComp(teamPanel);
		}

		int fillerHeight = 130;
		for (Component comp : panel.getComponents())
			fillerHeight -= (comp.getPreferredSize().height + margin);
		if (fillerHeight < 0)
			throw new IllegalArgumentException();
		System.out.println(fillerHeight);
		JPanel filler = new JPanel();
		filler.setPreferredSize(new Dimension(1, fillerHeight));
		filler.setOpaque(false);
		panel.addComp(filler);

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

	private JPanel createButtonsPannel() {
		Menus.ButtonColumn panel = new Menus.ButtonColumn();
		panel.setButtonSize(new Dimension(150, 30));
		panel.addButton("End Turn", onlyIfActionsEnabled(e -> window.endTurn()));
		panel.addButton("Main Menu", onlyIfActionsEnabled(e -> window.globals.frame.openMainMenu()));
		return panel;
	}

	void initGame() {
		register.register(window.game.onMoneyChange, Utils.swingListener(e -> updateMoneyLabel(e.team, e.newAmount)));
	}

	@Override
	public void clear() {
		register.unregisterAll();
		descriptionPanel.clear();
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

		private final Game game;
		private static final int TileSize = 6;
		private static final Color CurrentMapColor = Color.YELLOW;

		MiniMap() {
			game = window.game;
			setPreferredSize(new Dimension(game.width() * TileSize, game.height() * TileSize));

			window.arenaPanel.tickTaskManager.addTask(1000, this::repaint);
		}

		private void drawImg(Graphics g, int cell, BufferedImage img) {
			g.drawImage(img, Cell.x(cell) * TileSize, Cell.y(cell) * TileSize, null);
		}

		@Override
		public void paintComponent(Graphics g) {
			for (Iter.Int it = game.cells(); it.hasNext();) {
				int cell = it.next();
				Terrain terrain = game.terrain(cell);
				drawImg(g, cell, Images.getMinimapTerrain(terrain.category));

				Building building = game.building(cell);
				if (building != null)
					drawImg(g, cell, Images.getMinimapBuilding(building.getTeam()));

				Unit unit = game.unit(cell);
				if (unit != null)
					drawImg(g, cell, Images.getMinimapUnit(unit.getTeam()));
			}

			Position currentMapPos = window.arenaPanel.getCurrentMapOrigin();
			int x = (int) (currentMapPos.x * TileSize);
			int y = (int) (currentMapPos.y * TileSize);
			int width = (int) (TileSize * window.arenaPanel.displayedArenaWidth() - 1);
			int height = (int) (TileSize * window.arenaPanel.displayedArenaHeight() - 1);
			g.setColor(CurrentMapColor);
			g.drawRect(x, y, width, height);
		}

	}

}
