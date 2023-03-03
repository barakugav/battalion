package com.ugav.battalion;

import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;

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

		add(createOption("showGrid", globals.debug.showGrid,
				selected -> globals.debug.showGrid = selected.booleanValue()));
		add(createOption("showUnitID", globals.debug.showUnitID,
				selected -> globals.debug.showUnitID = selected.booleanValue()));
		add(createOption("playAllTeams", globals.debug.playAllTeams,
				selected -> globals.debug.playAllTeams = selected.booleanValue()));
	}

	private static JCheckBox createOption(String text, boolean selected, Consumer<Boolean> onSelectedChange) {
		JCheckBox option = new JCheckBox(text, selected);
		option.addActionListener(e -> onSelectedChange.accept(Boolean.valueOf(option.isSelected())));
		return option;
	}

	@Override
	public void clear() {
	}

}
