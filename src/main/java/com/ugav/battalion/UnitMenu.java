package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Pair;
import com.ugav.battalion.util.Utils;

class UnitMenu extends JPanel implements Clearable {

	private static final long serialVersionUID = 1L;

	private final GameWindow window;
	final Unit unit;
	private final List<Pair<JButton, ActionListener>> listeners = new ArrayList<>();

	final Event.Notifier<Event> onActionChosen = new Event.Notifier<>();

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
		if (!unit.type.transportUnits) {
			boolean transportAirEn = unit.type.category == Unit.Category.Land
					&& Unit.Type.AirTransporter.canStandOn(terrain);
			createUnitMenuButton(Images.Label.UnitMenuTransportAir, transportAirEn,
					e -> window.gameAction(new Action.UnitTransport(unit.getPos(), Unit.Type.AirTransporter)));

			boolean transportWaterEn = unit.type.category == Unit.Category.Land
					&& Unit.Type.ShipTransporter.canStandOn(terrain);
			createUnitMenuButton(Images.Label.UnitMenuTransportWater, transportWaterEn,
					e -> window.gameAction(new Action.UnitTransport(unit.getPos(), Unit.Type.ShipTransporter)));

		} else {
			Unit transportedUnit = unit.getTransportedUnit();

			boolean transportFinishEn = transportedUnit.type.canStandOn(terrain);
			createUnitMenuButton(Images.Label.UnitMenuTransportFinish, transportFinishEn,
					e -> window.gameAction(new Action.UnitTransportFinish(unit.getPos())));
		}

		boolean repairEn = unit.getHealth() < unit.type.health; // TODO and has enough money
		createUnitMenuButton(Images.Label.UnitMenuRepair, repairEn,
				e -> window.gameAction(new Action.UnitRepair(unit.getPos())));

		createUnitMenuButton(Images.Label.UnitMenuCancel, true, e -> {
		});
	}

	private void createUnitMenuButton(Images.Label label, boolean enable, ActionListener l) {
		BufferedImage img = Images.getImg(label);
		if (!enable)
			img = Utils.imgTransparent(img, .5f);
		JButton button = new JButton(new ImageIcon(img));
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setContentAreaFilled(false);
		button.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
		if (enable) {
			ActionListener listener = e -> {
				onActionChosen.notify(new Event(unit));
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

}
