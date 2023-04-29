package com.bugav.battalion;

import java.awt.Color;
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

import com.bugav.battalion.core.Building;
import com.bugav.battalion.core.Cell;
import com.bugav.battalion.core.Direction;
import com.bugav.battalion.core.Game;
import com.bugav.battalion.core.Team;
import com.bugav.battalion.core.Terrain;
import com.bugav.battalion.core.Unit;
import com.bugav.battalion.core.Level.UnitDesc;
import com.bugav.battalion.util.Event;
import com.bugav.battalion.util.Iter;
import com.bugav.battalion.util.Utils;

class GameSideMenu extends Menus.ColumnWithMargins implements Clearable {

	private final GameWindow window;
	private final DescriptionPanel descriptionPanel;
	private final Map<Team, JLabel> labelMoney;
	private final Event.Register register = new Event.Register();

	private static final Color BackgroundColor = new Color(64, 62, 64);
	private static final long serialVersionUID = 1L;

	GameSideMenu(GameWindow window) {
		this.window = Objects.requireNonNull(window);

		labelMoney = new HashMap<>();

		setBackground(BackgroundColor);
		addComp(createMinimapPanel());
		addComp(createTeamsPanel());
		addComp(descriptionPanel = new DescriptionPanel(window));
		addComp(Utils.fillerComp(), 1);
		addComp(createButtonsPannel());
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		for (Team team : Team.values())
			updateMoneyLabel(team, window.game.getMoney(team));
	}

	private JPanel createMinimapPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(Color.BLACK);
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		panel.setOpaque(true);

		MiniMap miniMap = new MiniMap();
		panel.add(miniMap, Utils.gbConstraints(0, 0, 1, 1, GridBagConstraints.NONE, 1, 1));

		JLabel lvlNameLabel = new JLabel(window.level.name, SwingConstants.CENTER);
		lvlNameLabel.setBackground(new Color(63, 0, 0));
		lvlNameLabel.setForeground(new Color(250, 250, 250));
		lvlNameLabel.setOpaque(true);
		panel.add(lvlNameLabel, Utils.gbConstraints(0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0));

		Dimension size = new Dimension(170, 170);
		panel.setPreferredSize(size);
		Dimension miniMapSize = miniMap.getPreferredSize();
		if (miniMapSize.width > size.width || miniMapSize.height > size.width)
			throw new IllegalArgumentException("Map too big for minimap");

		return panel;
	}

	private JPanel createTeamsPanel() {
		Menus.ColumnWithMargins panel = new Menus.ColumnWithMargins(3);
		panel.setBackground(new Color(80, 79, 80));
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

		for (Team team : Team.values()) {
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
			Image img = Images.Units.standImg(UnitDesc.of(Unit.Type.Rifleman, team), Direction.XPos, 0);
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

		panel.addComp(Utils.fillerComp(), 1);

		panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 130));

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

	private ExitPopup exitPopup;
	private static final int ExitPopupLayer = 90;

	private JPanel createButtonsPannel() {
		Menus.ButtonColumn panel = new Menus.ButtonColumn();
		panel.setButtonSize(new Dimension(150, 30));
		panel.addButton("End Turn", ifActionEnabled(e -> window.endTurn()));
		panel.addButton("Settings", e -> {
			synchronized (GameSideMenu.this) {
				if (exitPopup == null) {
					window.arenaPanel.showPopup(exitPopup = new ExitPopup(), ExitPopupLayer);
				} else {
					window.arenaPanel.closePopup(exitPopup);
				}
			}
		});
		return panel;
	}

	private class ExitPopup extends Menus.Window implements Clearable {

		private static final long serialVersionUID = 1L;

		ExitPopup() {
			Menus.ColumnWithMargins column = new Menus.ColumnWithMargins();
			column.addComp(new Menus.Title("Settings"));

			Menus.ButtonColumn buttonSet = new Menus.ButtonColumn();
			buttonSet.addButton("Resume", e -> window.arenaPanel.closePopup(ExitPopup.this));
			buttonSet.addButton("Options", e -> System.out.println("Options")); // TODO
			buttonSet.addButton("Quit", e -> window.globals.frame.openMainMenu());
			column.addComp(buttonSet);

			add(column);
		}

		@Override
		public void clear() {
			synchronized (GameSideMenu.this) {
				if (exitPopup != this)
					throw new IllegalStateException();
				exitPopup = null;
			}
		}

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

	private ActionListener ifActionEnabled(ActionListener l) {
		return e -> {
			if (window.isActionEnabled())
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

			window.arenaPanel.tickTaskManager.addTask(1000, new TickTask() {
				@Override
				public void clear() {
				}

				@Override
				public void onTick() {
					repaint();
				}
			});
		}

		private void drawImg(Graphics g, int cell, BufferedImage img) {
			g.drawImage(img, Cell.x(cell) * TileSize, Cell.y(cell) * TileSize, null);
		}

		@Override
		public void paintComponent(Graphics g) {
			for (Iter.Int it = game.cells(); it.hasNext();) {
				int cell = it.next();
				Terrain terrain = game.terrain(cell);
				drawImg(g, cell, Images.MiniMap.terrain(terrain.category));

				Building building = game.building(cell);
				if (building != null)
					drawImg(g, cell, Images.MiniMap.building(building.getTeam()));

				if (game.isUnitVisible(cell, ArenaPanelGame.player))
					drawImg(g, cell, Images.MiniMap.unit(game.unit(cell).getTeam()));
			}

			Position currentMapPos = window.arenaPanel.getCurrentMapOrigin();
			int arenaWidth = window.arenaPanel.arenaWidth();
			int arenaHeight = window.arenaPanel.arenaHeight();
			int xmin = (int) (Math.max(0, currentMapPos.x) * TileSize);
			int xmax = (int) (Math.min(arenaWidth, (currentMapPos.x + window.arenaPanel.displayedArenaWidth()))
					* TileSize);
			int ymin = (int) (Math.max(0, currentMapPos.y) * TileSize);
			int ymax = (int) (Math.min(arenaHeight, (currentMapPos.y + window.arenaPanel.displayedArenaHeight()))
					* TileSize);
			g.setColor(CurrentMapColor);
			g.drawRect(xmin, ymin, (xmax - xmin) - 1, (ymax - ymin) - 1);
		}

	}

}
