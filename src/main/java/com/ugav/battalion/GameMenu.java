package com.ugav.battalion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ObjIntConsumer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.ugav.battalion.ArenaPanelGame.Selection;
import com.ugav.battalion.Levels.LevelHandle;
import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.Pair;
import com.ugav.battalion.util.Utils;

interface GameMenu extends Clearable {

	GameWindow window();

	void beforeClose();

	default ActionListener ifActionEnabled(ActionListener l) {
		return e -> {
			if (window().isActionEnabled())
				l.actionPerformed(e);
		};
	}

	abstract class Abstract extends JPanel implements GameMenu {

		final GameWindow window;
		private static final long serialVersionUID = 1L;

		Abstract(GameWindow window) {
			this.window = Objects.requireNonNull(window);
		}

		@Override
		public GameWindow window() {
			return window;
		}

	}

	static class FactoryMenu extends GameMenu.Abstract {

		private final DescriptionPanel.UnitPanel unitDesc = new DescriptionPanel.UnitPanel();
		final Building factory;
		private final List<Pair<JButton, ActionListener>> listeners = new ArrayList<>();
		private MouseListener mouseListener;

		private static final long serialVersionUID = 1L;

		FactoryMenu(GameWindow window, Building factory) {
			super(window);
			if (!factory.type.canBuildUnits)
				throw new IllegalArgumentException(factory.type.toString());
			this.factory = factory;

			initUI();
		}

		private void initUI() {
			setLayout(new GridBagLayout());

			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.weightx = 1;
			c.gridy = 0;
			c.weighty = 1;
			add(createDisplayPanel(), c);
			c.gridy = 1;
			c.weighty = 1;
			add(createUnitsPanel(), c);

			/* Dummy listener to block the mouse events reaching the arena layer */
			addMouseListener(mouseListener = new MouseAdapter() {
			});
		}

		private JPanel createDisplayPanel() {
			JPanel panel = new JPanel();

			unitDesc.showUnit(UnitDesc.of(Unit.Type.Soldier, Team.Red));
			panel.add(unitDesc);

			JLabel img = new JLabel(new ImageIcon(Images.FactoryMenuImg));
			panel.add(img);

			return panel;
		}

		private JPanel createUnitsPanel() {
			JPanel mainPanel = new JPanel(new GridLayout(-1, 1));

			Map<Unit.Type, Building.UnitSale> sales = factory.getAvailableUnits();

			List<Unit.Type> landUnits = List.of(Unit.Type.Soldier, Unit.Type.Bazooka, Unit.Type.TankAntiAir,
					Unit.Type.Tank, Unit.Type.Mortar, Unit.Type.Artillery, Unit.Type.TankBig);
			List<Unit.Type> waterUnits = List.of(Unit.Type.SpeedBoat, Unit.Type.ShipAntiAir, Unit.Type.Ship,
					Unit.Type.ShipArtillery, Unit.Type.Submarine);
			List<Unit.Type> airUnits = List.of(Unit.Type.Airplane, Unit.Type.Zeppelin);

			JButton close = new JButton("Close");
			ActionListener closeListener = e -> window.arenaPanel.closeOpenMenu();
			close.addActionListener(closeListener);
			listeners.add(Pair.of(close, closeListener));

			mainPanel.add(createUnitsPanelSingleCategory("Ground Units", landUnits, sales, null));
			mainPanel.add(createUnitsPanelSingleCategory("Air Units", airUnits, sales, null));
			mainPanel.add(createUnitsPanelSingleCategory("Sea Units", waterUnits, sales, close));

			return mainPanel;
		}

		private JPanel createUnitsPanelSingleCategory(String title, List<Unit.Type> units,
				Map<Unit.Type, Building.UnitSale> sales, JButton additionalButton) {
			JPanel panel = new JPanel(new GridBagLayout());
			JLabel titleLabel = new JLabel(title);
			panel.add(titleLabel, Utils.gbConstraints(0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 1));

			JPanel salesPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridy = 0;
			c.gridheight = 1;
			c.weighty = 1;
			c.gridwidth = 1;
			c.fill = GridBagConstraints.VERTICAL;

			for (Iter.Indexed<Unit.Type> unit : Iter.of(units).enumerate().forEach()) {
				JPanel unitComp = createUnitPanel(unit.elm, sales);
				c.gridx = unit.idx;
				salesPanel.add(unitComp, c);
			}

			JPanel dummyFillUnit = new JPanel();
			c.gridx = units.size();
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			salesPanel.add(dummyFillUnit, c);

			if (additionalButton != null) {
				c.gridx = units.size() + 1;
				c.weightx = 0;
				c.fill = GridBagConstraints.NONE;
				salesPanel.add(additionalButton, c);

			}
			panel.add(salesPanel, Utils.gbConstraints(0, 1, 1, 1, GridBagConstraints.BOTH, 1, 1));
			return panel;
		}

		private static final Color UnitBackground = new Color(84, 86, 58);
		private static final Color UnitPriceBackground = new Color(150, 150, 150);

		private JPanel createUnitPanel(Unit.Type unit, Map<Unit.Type, Building.UnitSale> sales) {
			JPanel saleComp = new JPanel(new GridBagLayout());
			JButton button;
			JLabel price;

			Building.UnitSale unitSale = sales.get(unit);
			if (unitSale != null) {
				button = new JButton(new ImageIcon(Images.Units.getDefault(UnitDesc.of(unit, factory.getTeam()))));
				price = new JLabel(Integer.toString(unitSale.price), SwingConstants.CENTER);
				boolean canBuy = unitSale.price <= window.game.getMoney(factory.getTeam());
				price.setForeground(canBuy ? Color.BLACK : Color.RED);

				Building.UnitSale sale = unitSale;
				ActionListener listener = e -> {
					if (sale.price <= window.game.getMoney(factory.getTeam())) {
						window.arenaPanel.closeOpenMenu();
						window.gameAction(new Action.UnitBuild(factory.getPos(), sale.type));
					}
				};
				button.addActionListener(listener);
				listeners.add(Pair.of(button, listener));
			} else {
				button = new JButton(new ImageIcon(Images.UnitLocked));
				price = new JLabel("", SwingConstants.CENTER);
			}

			button.setPreferredSize(new Dimension(56, 56));
			button.setBackground(UnitBackground);
			price.setPreferredSize(new Dimension(56, 28));
			price.setOpaque(true);
			price.setBackground(UnitPriceBackground);

			saleComp.add(button, Utils.gbConstraints(0, 0, 1, 3));
			saleComp.add(price, Utils.gbConstraints(0, 3, 1, 1));
			saleComp.setPreferredSize(new Dimension(64, 80));

			saleComp.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					unitDesc.showUnit(UnitDesc.of(unit, Team.Red));
				}
			});

			return saleComp;
		}

		@Override
		public void clear() {
			for (Pair<JButton, ActionListener> l : listeners)
				l.e1.removeActionListener(l.e2);
			listeners.clear();
			removeMouseListener(mouseListener);
		}

		@Override
		public void beforeClose() {
			ArenaPanelGame arena = window.arenaPanel;
			if (arena.selection == Selection.FactoryBuild && arena.selectedEntity == factory)
				arena.clearSelection();
		}

	}

	static class UnitMenu extends GameMenu.Abstract {

		private static final long serialVersionUID = 1L;

		final Unit unit;
		private final List<Pair<JButton, ActionListener>> listeners = new ArrayList<>();

		UnitMenu(GameWindow window, Unit unit) {
			super(window);
			this.unit = Objects.requireNonNull(unit);

			initUI();
		}

		private void initUI() {
			setLayout(new GridLayout(1, 0));

			/* Transparent background, draw only buttons */
			setOpaque(false);

			Terrain terrain = window.game.terrain(unit.getPos());
			if (!unit.type.transportUnits) {
				boolean transportAirEn = unit.type.category == Unit.Category.Land
						&& Unit.Type.AirTransporter.canStandOn(terrain) && window.game.canBuildAirUnits(unit.getTeam());
				createUnitMenuButton(Images.UnitMenuTransportAir, transportAirEn,
						e -> window.gameAction(new Action.UnitTransport(unit.getPos(), Unit.Type.AirTransporter)));

				boolean transportWaterEn = unit.type.category == Unit.Category.Land
						&& Unit.Type.ShipTransporter.canStandOn(terrain)
						&& window.game.canBuildWaterUnits(unit.getTeam());
				createUnitMenuButton(Images.UnitMenuTransportWater, transportWaterEn,
						e -> window.gameAction(new Action.UnitTransport(unit.getPos(), Unit.Type.ShipTransporter)));

			} else {
				Unit transportedUnit = unit.getTransportedUnit();

				boolean transportFinishEn = transportedUnit.type.canStandOn(terrain);
				createUnitMenuButton(Images.UnitMenuTransportFinish, transportFinishEn,
						e -> window.gameAction(new Action.UnitTransportFinish(unit.getPos())));
			}

			boolean repairEn = unit.getHealth() < unit.type.health; // TODO and has enough money
			createUnitMenuButton(Images.UnitMenuRepair, repairEn,
					e -> window.gameAction(new Action.UnitRepair(unit.getPos())));

			createUnitMenuButton(Images.UnitMenuCancel, true, e -> {
			});
		}

		private void createUnitMenuButton(BufferedImage img, boolean enable, ActionListener l) {
			if (!enable)
				img = Utils.imgTransparent(img, .5f);
			JButton button = new JButton(new ImageIcon(img));
			button.setBorder(BorderFactory.createEmptyBorder());
			button.setContentAreaFilled(false);
			button.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
			if (enable) {
				ActionListener listener = e -> {
					window.arenaPanel.closeOpenMenu();
					l.actionPerformed(e);
				};
				button.addActionListener(listener);
				listeners.add(Pair.of(button, listener));
			}
			add(button);
		}

		@Override
		public void clear() {
			for (Pair<JButton, ActionListener> l : listeners)
				l.e1.removeActionListener(l.e2);
			listeners.clear();
		}

		@Override
		public void beforeClose() {
			ArenaPanelGame arena = window.arenaPanel;
			if (arena.selection == Selection.UnitEctActions && arena.selectedEntity == unit)
				arena.clearSelection();
		}

	}

	static class GameEndPopup extends Menus.Window implements Clearable {

		private static final long serialVersionUID = 1L;
		private static final Color NextLevelColor = new Color(96, 181, 102);
		private static final Color QuitColor = new Color(254, 106, 106);

		GameEndPopup(GameWindow window, Team winner, GameStats stats) {
			final Team player = Team.Red;
			boolean victory = player == winner;
			addTitle("Game Finished: " + (victory ? "Victory!" : "You lost..."));

			Menus.Table statsTable = new Menus.Table();
			Menus.Table.Column statsColumn = statsTable.addColumn();
//			statsColumn.setPrefWidth(120);
			statsColumn.setHorizontalAlignment(SwingConstants.LEFT);
			Menus.Table.Column valsColumn = statsTable.addColumn();
			valsColumn.setPrefWidth(60);
			valsColumn.setHorizontalAlignment(SwingConstants.RIGHT);

			ObjIntConsumer<String> addRow = (stat, val) -> statsTable.addRow(stat, Integer.toString(val));
			addRow.accept("Turns Played", stats.getTurnsPlayed());
			addRow.accept("Units Built", stats.getUnitsBuilt());
			addRow.accept("Enemies Terminated", stats.getEnemiesTerminated());
			addRow.accept("Units Casualties", stats.getUnitsCasualties());
			addRow.accept("Buildings Conquered", stats.getBuildingsConquered());
			addRow.accept("Money Gained", stats.getMoneyGained());
			addRow.accept("Money Spent", stats.getMoneySpent());
			addComp(statsTable);

			Menus.ButtonColumn buttonSet = new Menus.ButtonColumn();

			List<LevelHandle> campaign = window.globals.levels.getCampaign();
			if (window.level.isCampaignLevel() && window.level.campaignIdx + 1 < campaign.size()) {
				LevelHandle nextLevel = campaign.get(window.level.campaignIdx + 1);
				buttonSet.addButton("Next Level", e -> window.globals.frame.openLevelGame(nextLevel))
						.setBackground(NextLevelColor);
			}
			buttonSet.addButton("Quit", e -> window.globals.frame.openMainMenu()).setBackground(QuitColor);
			addComp(buttonSet);
		}

		@Override
		public void clear() {
		}

	}

}
