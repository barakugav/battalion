package com.ugav.battalion;

import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.ugav.battalion.util.Utils;

class OptionsWindow extends JPanel implements Clearable {

	private static final long serialVersionUID = 1L;

	private final Globals globals;

	OptionsWindow(Globals globals) {
		this.globals = Objects.requireNonNull(globals);

		setLayout(new GridLayout(-1, 1));
		createDebugButtons();

		add(Utils.newButton("Main Menu", e -> globals.frame.openMainMenu()));
	}

	private void createDebugButtons() {
		add(new JLabel("Debug Options:"));

		JCheckBox optionShowGrid = new JCheckBox("showGrid", globals.debug.showGrid);
		optionShowGrid.addActionListener(e -> globals.debug.showGrid = optionShowGrid.isSelected());
		add(optionShowGrid);

		JCheckBox optionShowUnitID = new JCheckBox("showUnitID", globals.debug.showUnitID);
		optionShowUnitID.addActionListener(e -> globals.debug.showUnitID = optionShowUnitID.isSelected());
		add(optionShowUnitID);
	}

	@Override
	public void clear() {
	}

}
