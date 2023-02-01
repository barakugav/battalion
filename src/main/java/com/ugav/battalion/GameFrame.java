package com.ugav.battalion;

import javax.swing.JComponent;
import javax.swing.JFrame;

class GameFrame extends JFrame {

	private JComponent activeWindow;

	private static final String TITLE = "Battalion";
	private static final int FRAME_WIDTH = 2500;
	private static final int FRAME_HEIGHT = 2500;

	private static final long serialVersionUID = 1L;

	GameFrame() {
		super(TITLE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationByPlatform(true);
		setResizable(false);
		setSize(FRAME_WIDTH, FRAME_HEIGHT);

		/* By default display main menu */
		displayMainMenu();
	}

	void displayMainMenu() {
		displayWindow(new MainMenuPanel(this));
	}

	void loadLevel(Level level) {
		displayWindow(new LevelPanel(this, level));
	}

	private void displayWindow(JComponent window) {
		if (activeWindow != null) {
			((Clearable) activeWindow).clear();
			remove(activeWindow);
		}
		assert window instanceof Clearable;
		add(activeWindow = window);
		pack();
		invalidate();
		repaint();
	}

}
