package com.ugav.battalion;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.ugav.battalion.core.Level;

class GameFrame extends JFrame {

	private JComponent activeWindow;
	private final Globals globals;

	private static final String TITLE = "Battalion";
	private static final int FRAME_WIDTH = 750;
	private static final int FRAME_HEIGHT = 600;

	private static final long serialVersionUID = 1L;

	GameFrame() {
		super(TITLE);
		setLayout(new GridLayout(1, 1));

		this.globals = new Globals(this);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationByPlatform(true);
		setResizable(false);
		setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));

		/* By default display main menu */
		openMainMenu();
	}

	void openMainMenu() {
		displayWindow(new MainMenuPanel(globals));
	}

	void openLevelGame(Level level) {
		displayWindow(new GameWindow(globals, level));
	}

	void openLevelBuilder() {
		displayWindow(new LevelBuilderWindow(globals));
	}

	void openOptionsMenu() {
		displayWindow(new OptionsMenu(globals));
	}

	private void displayWindow(JComponent window) {
		if (activeWindow != null) {
			((Clearable) activeWindow).clear();
			remove(activeWindow);
		}
		assert window instanceof Clearable;
		add(activeWindow = window);
		revalidate();
		repaint();
	}

}
