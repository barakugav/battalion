package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.Utils;

class FactoryMenu extends JPanel implements Clearable {

	private final GameWindow window;
	final Building factory;
	final DataChangeNotifier<DataEvent> onActionChosen = new DataChangeNotifier<>();
	private final List<Pair<JButton, ActionListener>> listeners = new ArrayList<>();
	private MouseListener mouseListener;

	private static final long serialVersionUID = 1L;

	FactoryMenu(GameWindow window, Building factory) {
		this.window = Objects.requireNonNull(window);
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

	private static JPanel createDisplayPanel() {
		JPanel panel = new JPanel();
		return panel;
	}

	private JPanel createUnitsPanel() {
		JPanel mainPanel = new JPanel(new GridLayout(-1, 1));

		Map<Unit.Type, Building.UnitSale> sales = factory.getAvailableUnits();

		List<Unit.Type> landUnits = List.of(Unit.Type.Soldier, Unit.Type.Bazooka, Unit.Type.TankAntiAir, Unit.Type.Tank,
				Unit.Type.Mortar, Unit.Type.Artillery, Unit.Type.TankBig);
		List<Unit.Type> waterUnits = List.of(Unit.Type.SpeedBoat, Unit.Type.ShipAntiAir, Unit.Type.Ship,
				Unit.Type.ShipArtillery, Unit.Type.Submarine);
		List<Unit.Type> airUnits = List.of(Unit.Type.Airplane, Unit.Type.Zeppelin);

		for (List<Unit.Type> units : List.of(landUnits, airUnits, waterUnits)) {
			JPanel panel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridy = 0;
			c.gridheight = 1;
			c.weighty = 1;
			c.gridwidth = 1;
			c.fill = GridBagConstraints.VERTICAL;

			for (Iter.Indexed<Unit.Type> unit : Iter.of(units).enumerate().forEach()) {
				JPanel unitComp = createUnitPanel(unit.elm, sales);
				c.gridx = unit.idx;
				panel.add(unitComp, c);
			}

			JPanel dummyFillUnit = new JPanel();
			c.gridx = units.size();
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			panel.add(dummyFillUnit, c);

			mainPanel.add(panel);
		}

		return mainPanel;
	}

	private JPanel createUnitPanel(Unit.Type unit, Map<Unit.Type, Building.UnitSale> sales) {
		JPanel saleComp = new JPanel(new GridBagLayout());
		JButton button;
		JLabel price;

		Building.UnitSale unitSale = sales.get(unit);
		if (unitSale != null) {
			button = new JButton(new ImageIcon(Images.getImg(UnitDesc.of(unit, factory.getTeam()))));
			price = new JLabel(Integer.toString(unitSale.price));

			Building.UnitSale sale = unitSale;
			ActionListener listener = e -> {
				if (sale.price <= window.game.getMoney(factory.getTeam())) {
					window.gameAction(() -> window.game.buildUnit(factory, sale.type));
					onActionChosen.notify(new DataEvent(factory));
				}
			};
			button.addActionListener(listener);
			listeners.add(Pair.of(button, listener));
		} else {
			button = new JButton(new ImageIcon(Images.getImg(Images.Label.UnitLocked)));
			price = new JLabel("none");
		}

		button.setPreferredSize(new Dimension(56, 56));

		saleComp.add(button, Utils.gbConstraints(0, 0, 1, 3));
		saleComp.add(price, Utils.gbConstraints(0, 3, 1, 1));
		return saleComp;
	}

	@Override
	public void clear() {
		for (Pair<JButton, ActionListener> l : listeners)
			l.e1.removeActionListener(l.e2);
		listeners.clear();
		removeMouseListener(mouseListener);
	}

}
