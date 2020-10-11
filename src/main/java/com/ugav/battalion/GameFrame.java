package com.ugav.battalion;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import javax.swing.JFrame;

public class GameFrame extends JFrame {

    private static final String TITLE = "Battalion";
    private static final int FRAME_WIDTH = 500;
    private static final int FRAME_HEIGHT = 500;

    private final MainMenuPanel mainMenuPanel;
    private final Collection<Component> windows;

    GameFrame() {
	super(TITLE);
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setLocationByPlatform(true);
	setResizable(false);
	setSize(FRAME_WIDTH, FRAME_HEIGHT);

	windows = new ArrayList<>();
	windows.add(mainMenuPanel = new MainMenuPanel(this));

	/* By default display main menu */
	displayMainMenu();
    }

    void displayMainMenu() {
	displayWindow(mainMenuPanel);
    }

    private void displayWindow(Component window) {
	if (!windows.contains(Objects.requireNonNull(window)))
	    throw new IllegalArgumentException();
	removeAllWindows();
	add(window);
    }

    private void removeAllWindows() {
	windows.forEach(this::remove);
    }

}
