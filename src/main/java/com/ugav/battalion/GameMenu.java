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
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

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
import com.ugav.battalion.util.Utils.RoundedPanel;

class GameMenu {

	private GameMenu() {
	}

	static class FactoryMenu extends Menus.Window implements Clearable {

		private final DescriptionPanel.UnitsPanel unitDesc = new DescriptionPanel.UnitsPanel();
		final Building factory;
		private final List<Pair<JButton, ActionListener>> listeners = new ArrayList<>();
		private MouseListener mouseListener;
		private final GameWindow window;

		private static final long serialVersionUID = 1L;

		FactoryMenu(GameWindow window, Building factory) {
			super(10);
			this.window = Objects.requireNonNull(window);
			if (!factory.type.canBuildUnits)
				throw new IllegalArgumentException(factory.type.toString());
			this.factory = factory;

			Menus.ColumnWithMargins column = new Menus.ColumnWithMargins();
			column.addComp(createDisplayPanel());
			column.addComp(createUnitsPanel());

			setLayout(new GridBagLayout());
			add(column, Utils.gbConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 1, 1));

			/* Dummy listener to block the mouse events reaching the arena layer */
			addMouseListener(mouseListener = new MouseAdapter() {
			});
		}

		private JPanel createDisplayPanel() {
			Menus.RowWithMargins panel = new Menus.RowWithMargins();

			unitDesc.showUnit(UnitDesc.of(Unit.Type.Rifleman, Team.Red));
			panel.addComp(unitDesc);

			JLabel img = new JLabel(new ImageIcon(Images.FactoryMenuImg));
			panel.addComp(img);

			return panel;
		}

		private JPanel createUnitsPanel() {
			Menus.ColumnWithMargins mainPanel = new Menus.ColumnWithMargins();

			Map<Unit.Type, Building.UnitSale> sales = factory.getAvailableUnits();

			List<Unit.Type> landUnits = List.of(Unit.Type.Rifleman, Unit.Type.RocketSpecialist, Unit.Type.AATank,
					Unit.Type.BattleTank, Unit.Type.Mortar, Unit.Type.Artillery, Unit.Type.TitanTank);
			List<Unit.Type> waterUnits = List.of(Unit.Type.SpeedBoat, Unit.Type.AACruiser, Unit.Type.Corvette,
					Unit.Type.Battleship, Unit.Type.Submarine);
			List<Unit.Type> airUnits = List.of(Unit.Type.FighterPlane, Unit.Type.ZeppelinBomber);

			mainPanel.addComp(createUnitsPanelSingleCategory("Ground Units", landUnits, sales));
			mainPanel.addComp(createUnitsPanelSingleCategory("Air Units", airUnits, sales));
			mainPanel.addComp(createUnitsPanelSingleCategory("Sea Units", waterUnits, sales));
			mainPanel.addComp(createAdditionalButtonsPanel());

			return mainPanel;
		}

		private JPanel createUnitsPanelSingleCategory(String title, List<Unit.Type> units,
				Map<Unit.Type, Building.UnitSale> sales) {
			Menus.ColumnWithMargins panel = new Menus.ColumnWithMargins();
			panel.addComp(new Menus.Title(title));

			Menus.RowWithMargins salesPanel = new Menus.RowWithMargins();
			for (Iter.Indexed<Unit.Type> unit : Iter.of(units).enumerate().forEach()) {
				JPanel unitComp = createUnitPanel(unit.elm, sales);
				salesPanel.addComp(unitComp);
			}
			JPanel dummyFillUnit = new JPanel();
			dummyFillUnit.setPreferredSize(new Dimension(0, 0));
			dummyFillUnit.setOpaque(false);
			salesPanel.addComp(dummyFillUnit, 1);

			panel.addComp(salesPanel);
			return panel;
		}

		private static final Color UnitBackground = new Color(84, 86, 58);
		private static final Color UnitPriceBackground = new Color(150, 150, 150);

		private JPanel createUnitPanel(Unit.Type unit, Map<Unit.Type, Building.UnitSale> sales) {
			JPanel panel = new JPanel(new GridBagLayout());
			panel.setOpaque(false);
			JComponent buttonPanel;
			JComponent price;

			Building.UnitSale unitSale = sales.get(unit);
			if (unitSale != null) {
				BufferedImage unitImg = Images.Units.getDefault(UnitDesc.of(unit, factory.getTeam()));
				unitImg = Utils.imgSubCircle(unitImg, (unitImg.getWidth() - 56) / 2, (unitImg.getHeight() - 56) / 2, 56,
						56);

				buttonPanel = new RoundedPanel();
				buttonPanel.setLayout(new GridBagLayout());
				JButton button = new JButton(new ImageIcon(unitImg));
				button.setContentAreaFilled(false);
				button.setBorder(BorderFactory.createEmptyBorder());
				buttonPanel.add(button, Utils.gbConstraints(0, 0, 1, 1, GridBagConstraints.BOTH, 1, 1));

				buttonPanel.setBackground(Color.red);
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

				button.addMouseMotionListener(new MouseAdapter() {
					@Override
					public void mouseMoved(MouseEvent e) {
						unitDesc.showUnit(UnitDesc.of(unit, Team.Red));
					}
				});
			} else {
				buttonPanel = new JButton(new ImageIcon(Images.UnitLocked));
				price = new JLabel("", SwingConstants.CENTER);
			}

			buttonPanel.setPreferredSize(new Dimension(56, 56));
			buttonPanel.setBackground(UnitBackground);
			price.setPreferredSize(new Dimension(56, 28));
			price.setOpaque(true);
			price.setBackground(UnitPriceBackground);

			panel.add(buttonPanel, Utils.gbConstraints(0, 0, 1, 1, GridBagConstraints.NONE, 0, 0));
			panel.add(price, Utils.gbConstraints(0, 1, 1, 1, GridBagConstraints.NONE, 0, 0));

			return panel;
		}

		private JPanel createAdditionalButtonsPanel() {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setOpaque(false);
			Menus.Button close = new Menus.Button("Close", e -> window.arenaPanel.closeOpenMenu());
			close.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
			panel.add(close);
			return panel;
		}

		@Override
		public void clear() {
			for (Pair<JButton, ActionListener> l : listeners)
				l.e1.removeActionListener(l.e2);
			listeners.clear();
			removeMouseListener(mouseListener);
		}

	}

	static class UnitMenu extends JPanel implements Clearable {

		private static final long serialVersionUID = 1L;

		final Unit unit;
		private final List<Pair<JButton, ActionListener>> listeners = new ArrayList<>();
		private final GameWindow window;

		UnitMenu(GameWindow window, Unit unit) {
			this.window = Objects.requireNonNull(window);
			this.unit = Objects.requireNonNull(unit);

			initUI();
		}

		private void initUI() {
			setLayout(new GridLayout(1, 0));

			/* Transparent background, draw only buttons */
			setOpaque(false);

			Terrain terrain = window.game.terrain(unit.getPos());
			Team team = unit.getTeam();
			if (!unit.type.transportUnits) {
				Unit.Type transportAirType = Unit.Type.TransportPlane;
				boolean transportAirEn = true;
				transportAirEn = transportAirEn && unit.type.category == Unit.Category.Land;
				transportAirEn = transportAirEn && window.game.canBuildAirUnits(team);
				transportAirEn = transportAirEn && window.game.getMoney(team) >= transportAirType.price;
				transportAirEn = transportAirEn && transportAirType.canStandOn(terrain);
				JButton transportAirButton = createUnitMenuButton(Images.UnitMenuTransportAir, transportAirEn,
						e -> window.gameAction(new Action.UnitTransport(unit.getPos(), transportAirType)));
				transportAirButton.setToolTipText("" + transportAirType.price + "$");

				Unit.Type transportWaterType = Unit.Type.LandingCraft;
				boolean transportWaterEn = true;
				transportWaterEn = transportWaterEn && unit.type.category == Unit.Category.Land;
				transportWaterEn = transportWaterEn && window.game.canBuildWaterUnits(team);
				transportWaterEn = transportWaterEn && window.game.getMoney(team) >= transportWaterType.price;
				transportWaterEn = transportWaterEn && transportWaterType.canStandOn(terrain);
				JButton transportWaterButton = createUnitMenuButton(Images.UnitMenuTransportWater, transportWaterEn,
						e -> window.gameAction(new Action.UnitTransport(unit.getPos(), transportWaterType)));
				transportWaterButton.setToolTipText("" + transportWaterType.price + "$");

			} else {
				Unit transportedUnit = unit.getTransportedUnit();

				boolean transportFinishEn = transportedUnit.type.canStandOn(terrain);
				createUnitMenuButton(Images.UnitMenuTransportFinish, transportFinishEn,
						e -> window.gameAction(new Action.UnitTransportFinish(unit.getPos())));
			}

			boolean repairEn = true;
			repairEn = repairEn && unit.getHealth() < unit.type.health; // TODO and has enough money
			repairEn = repairEn && window.game.getMoney(team) >= unit.getRepairCost();
			JButton repairButton = createUnitMenuButton(Images.UnitMenuRepair, repairEn,
					e -> window.gameAction(new Action.UnitRepair(unit.getPos())));
			repairButton.setToolTipText("" + unit.getRepairCost() + "$");

			createUnitMenuButton(Images.UnitMenuCancel, true, e -> {
			});
		}

		private JButton createUnitMenuButton(BufferedImage img, boolean enable, ActionListener l) {
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
			return button;
		}

		@Override
		public void clear() {
			for (Pair<JButton, ActionListener> l : listeners)
				l.e1.removeActionListener(l.e2);
			listeners.clear();
		}

	}

	static class GameEndPopup extends Menus.Window implements Clearable {

		private static final long serialVersionUID = 1L;
		private static final Color NextLevelColor = new Color(96, 181, 102);
		private static final Color QuitColor = new Color(254, 106, 106);

		GameEndPopup(GameWindow window, Team winner, GameStats stats) {
			Menus.ColumnWithMargins column = new Menus.ColumnWithMargins();
			final Team player = Team.Red;

			boolean victory = player == winner;
			String title = "Game Finished: " + (victory ? "Victory!" : "You lost...");
			column.addComp(new Menus.Title(title));

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
			column.addComp(statsTable);

			Menus.ButtonColumn buttonSet = new Menus.ButtonColumn();

			List<LevelHandle> campaign = window.globals.levels.getCampaign();
			if (window.level.isCampaignLevel() && window.level.campaignIdx + 1 < campaign.size()) {
				LevelHandle nextLevel = campaign.get(window.level.campaignIdx + 1);
				buttonSet.addButton("Next Level", e -> window.globals.frame.openLevelGame(nextLevel))
						.setBackground(NextLevelColor);
			}
			buttonSet.addButton("Quit", e -> window.globals.frame.openMainMenu()).setBackground(QuitColor);
			column.addComp(buttonSet);

			add(column);
		}

		@Override
		public void clear() {
		}

	}

}
