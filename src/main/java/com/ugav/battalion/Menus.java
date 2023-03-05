package com.ugav.battalion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.ugav.battalion.util.Utils;

class Menus {

	static class ButtonColumn extends ColumnWithMargins {

		private Dimension buttonSize;

		private static final Color BackgroundColor = new Color(49, 42, 41);
		private static final Color ButtonColor = new Color(156, 156, 156);
		private static final long serialVersionUID = 1L;

		ButtonColumn() {
			setMargin(3);
			setBackground(BackgroundColor);
			buttonSize = new Dimension(200, 30);
		}

		void setButtonSize(Dimension size) {
			buttonSize = Objects.requireNonNull(size);
		}

		JButton addButton(String label, ActionListener action) {
			JButton button = Utils.newButton(label, action);
			button.setPreferredSize(new Dimension(buttonSize));
			button.setBackground(ButtonColor);
			button.setOpaque(true);
			button.setFocusable(false);
			return addComp(button);
		}

	}

	static class CheckboxColumn extends ColumnWithMargins {

		private static final Color BackgroundColor = new Color(80, 79, 80);
		private static final Color CheckboxTextColor = new Color(245, 245, 245);
		private static final long serialVersionUID = 1L;

		CheckboxColumn() {
			setMargin(3);
			setBackground(BackgroundColor);
		}

		JCheckBox addCheckbox(String text, boolean selected, Consumer<Boolean> onSelectedChange) {
			JCheckBox checkbox = new JCheckBox(text, selected);
			checkbox.addActionListener(e -> onSelectedChange.accept(Boolean.valueOf(checkbox.isSelected())));
			checkbox.setPreferredSize(new Dimension(200, 30));
			checkbox.setBackground(BackgroundColor);
			checkbox.setForeground(CheckboxTextColor);
			checkbox.setFocusPainted(false);
			checkbox.setOpaque(true);
			// Set default icon for checkbox
			checkbox.setIcon(new ImageIcon(Images.getImg(Images.Label.CheckboxUnselected)));
			// Set selected icon when checkbox state is selected
			checkbox.setSelectedIcon(new ImageIcon(Images.getImg(Images.Label.CheckboxSelected)));
			// Set disabled icon for checkbox
			checkbox.setDisabledIcon(new ImageIcon(Images.getImg(Images.Label.CheckboxUnselected)));
			// Set disabled-selected icon for checkbox
			checkbox.setDisabledSelectedIcon(new ImageIcon(Images.getImg(Images.Label.CheckboxSelected)));
			// Set checkbox icon when checkbox is pressed
			checkbox.setPressedIcon(new ImageIcon(Images.getImg(Images.Label.CheckboxPressed)));
			// Set icon when a mouse is over the checkbox
			checkbox.setRolloverIcon(new ImageIcon(Images.getImg(Images.Label.CheckboxUnselectedHovered)));
			// Set icon when a mouse is over a selected checkbox
			checkbox.setRolloverSelectedIcon(new ImageIcon(Images.getImg(Images.Label.CheckboxSelectedHovered)));
			return addComp(checkbox);
		}

	}

	static class ColumnWithMargins extends JPanel {

		private int compCount;
		private int marginx;
		private int marginy;
		private boolean marginOnlyBetween = false;
		private static final long serialVersionUID = 1L;

		ColumnWithMargins() {
			super(new GridBagLayout());
		}

		void setMargin(int margin) {
			if (margin < 0)
				throw new IllegalArgumentException();
			marginx = marginy = margin;
		}

		void setMarginx(int margin) {
			if (margin < 0)
				throw new IllegalArgumentException();
			marginx = margin;
		}

		void setMarginy(int margin) {
			if (margin < 0)
				throw new IllegalArgumentException();
			marginy = margin;
		}

		void setMarginOnlyBetween(boolean marginOnlyBetween) {
			this.marginOnlyBetween = marginOnlyBetween;
		}

		<Comp extends JComponent> Comp addComp(Comp comp) {
			return addComp(comp, 0);
		}

		<Comp extends JComponent> Comp addComp(Comp comp, int weighty) {
			final int y = compCount++;
			GridBagConstraints c = Utils.gbConstraints(0, y, 1, 1, GridBagConstraints.BOTH, 1, weighty);
			if (marginOnlyBetween) {
				c.insets = new Insets(y == 0 ? 0 : marginy, marginx, 0, marginx);
			} else {
				c.insets = new Insets(y > 0 ? 0 : marginy, marginx, marginy, marginx);
			}
			add(comp, c);
			return comp;
		}
	}

}
