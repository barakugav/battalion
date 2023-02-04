package com.ugav.battalion;

import javax.swing.JComponent;
import javax.swing.JFrame;

class GameFrame extends JFrame {

	private JComponent activeWindow;
	private final Globals globals;

	private static final String TITLE = "Battalion";
	private static final int FRAME_WIDTH = 2500;
	private static final int FRAME_HEIGHT = 2500;

	private static final long serialVersionUID = 1L;

	GameFrame() {
		super(TITLE);

		this.globals = new Globals(this);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationByPlatform(true);
		setResizable(false);
		setSize(FRAME_WIDTH, FRAME_HEIGHT);

		/* By default display main menu */
		displayMainMenu();
	}

	void displayMainMenu() {
		displayWindow(new MainMenuPanel(globals));
	}

	void loadLevel(Level level) {
		displayWindow(new LevelPanel(globals, level));
	}

	void loadLevelBuilder() {
		displayWindow(new LevelBuilderWindow(globals));
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
