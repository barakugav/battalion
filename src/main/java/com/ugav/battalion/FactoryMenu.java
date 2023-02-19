package com.ugav.battalion;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.Unit;

class FactoryMenu extends JDialog {

	private final Game game;
	private final Building factory;
	final DataChangeNotifier<UnitBuy> onUnitBuy = new DataChangeNotifier<>();

	private static final long serialVersionUID = 1L;

	FactoryMenu(JFrame parent, Game game, Building factory) {
		super(parent);

		this.game = Objects.requireNonNull(game);
		if (!factory.type.canBuildUnits)
			throw new IllegalArgumentException(factory.type.toString());
		this.factory = factory;

		initUI();
	}

	private void initUI() {
		setTitle("Factory");

		int unitCount = Unit.Type.values().length;
		setLayout(new GridLayout(1, unitCount));

		Map<Unit.Type, Building.UnitSale> sales = factory.getAvailableUnits();

		List<Unit.Type> landUnits = List.of(Unit.Type.Soldier, Unit.Type.Bazooka, Unit.Type.TankAntiAir, Unit.Type.Tank,
				Unit.Type.Mortar, Unit.Type.Artillery, Unit.Type.TankBig);
		List<Unit.Type> waterUnits = List.of(Unit.Type.SpeedBoat, Unit.Type.ShipAntiAir, Unit.Type.Ship,
				Unit.Type.ShipArtillery, Unit.Type.Submarine);
		List<Unit.Type> airUnits = List.of(Unit.Type.Airplane, Unit.Type.Airplane);
		List<Unit.Type> unitsOrder = new ArrayList<>();
		unitsOrder.addAll(landUnits);
		unitsOrder.addAll(waterUnits);
		unitsOrder.addAll(airUnits);

		for (Unit.Type unit : unitsOrder) {
			JPanel saleComp = new JPanel();
			saleComp.setLayout(new GridBagLayout());
			JComponent upperComp;
			JComponent lowerComp;

			Building.UnitSale unitSale = sales.get(unit);
			if (unitSale != null) {
				upperComp = new JLabel(new ImageIcon(Images.getImg(UnitDesc.of(unit, factory.getTeam()))));
				lowerComp = new JLabel(Integer.toString(unitSale.price));
			} else {
				upperComp = new JLabel(new ImageIcon(Images.getImg(Images.Label.UnitLocked)));
				lowerComp = new JLabel("none");
			}

			saleComp.add(upperComp, Utils.gbConstraints(0, 0, 1, 3));
			saleComp.add(lowerComp, Utils.gbConstraints(0, 3, 1, 1));

			if (unitSale != null) {
				Building.UnitSale sale = unitSale;
				saleComp.addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						if (sale.price <= game.getMoney(factory.getTeam())) {
							onUnitBuy.notify(new UnitBuy(factory, sale));
							dispose();
						}
					}
				});
			}

			add(saleComp);
		}

		pack();
		setLocationRelativeTo(getParent());
	}

	static class UnitBuy extends DataEvent {

		final Building.UnitSale sale;

		public UnitBuy(Building source, Building.UnitSale sale) {
			super(source);
			this.sale = Objects.requireNonNull(sale);
		}

	}

}
