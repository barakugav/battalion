package com.ugav.battalion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import com.ugav.battalion.util.Utils;

class Menus {

	private static final Color TitleColor = new Color(255, 234, 201);

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
			checkbox.setIcon(new ImageIcon(Images.CheckboxUnselected));
			// Set selected icon when checkbox state is selected
			checkbox.setSelectedIcon(new ImageIcon(Images.CheckboxSelected));
			// Set disabled icon for checkbox
			checkbox.setDisabledIcon(new ImageIcon(Images.CheckboxUnselected));
			// Set disabled-selected icon for checkbox
			checkbox.setDisabledSelectedIcon(new ImageIcon(Images.CheckboxSelected));
			// Set checkbox icon when checkbox is pressed
			checkbox.setPressedIcon(new ImageIcon(Images.CheckboxPressed));
			// Set icon when a mouse is over the checkbox
			checkbox.setRolloverIcon(new ImageIcon(Images.CheckboxUnselectedHovered));
			// Set icon when a mouse is over a selected checkbox
			checkbox.setRolloverSelectedIcon(new ImageIcon(Images.CheckboxSelectedHovered));
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

	static class Window extends Menus.ColumnWithMargins {

		private static final Color BackgroundColor = new Color(64, 62, 64);
		private static final long serialVersionUID = 1L;

		Window() {
			setMargin(6);
			setBorder(BorderFactory.createRaisedBevelBorder());
			setBackground(BackgroundColor);
		}

		JLabel addTitle(String title) {
			JLabel label = new JLabel(title);
			label.setForeground(TitleColor);
			return addComp(label);
		}

	}

	static class Table extends JPanel {

		private final List<Column> columns = new ArrayList<>();
		private int rowCount;

		private static final long serialVersionUID = 1L;
		private static final Color TextColor = Color.WHITE;
		private static final Color RowColor1 = new Color(85, 85, 85);
		private static final Color RowColor2 = new Color(73, 73, 73);
		private static final Color ColumnSeperatorColor = new Color(60, 60, 60);
		private static final int ColumnSeperatorWidth = 2;
		private static final int CellMargin = 6;

		Table() {
			setLayout(new GridBagLayout());
			setBorder(BorderFactory.createLineBorder(ColumnSeperatorColor, ColumnSeperatorWidth));
		}

		Column addColumn() {
			Column c = new Column();
			columns.add(c);
			return c;
		}

		void addRow(Object... row) {
			if (row.length != columns.size())
				throw new IllegalArgumentException();
			Color backgroundColor = rowCount % 2 == 0 ? RowColor1 : RowColor2;
			for (int colIdx = 0; colIdx < row.length; colIdx++) {
				Column column = columns.get(colIdx);
				String val = Objects.toString(row[colIdx]);
				JLabel cell = new JLabel(val, column.horizontalAlignment);
				if (column.prefWidthValid)
					cell.setPreferredSize(
							new Dimension(column.prefWidth, cell.getPreferredSize().height + CellMargin * 2));
				cell.setForeground(TextColor);
				cell.setBackground(backgroundColor);
				cell.setOpaque(true);
				if (colIdx > 0)
					cell.setBorder(
							BorderFactory.createMatteBorder(0, ColumnSeperatorWidth, 0, 0, ColumnSeperatorColor));
				Border margins = BorderFactory.createEmptyBorder(CellMargin, CellMargin, CellMargin, CellMargin);
				cell.setBorder(new CompoundBorder(cell.getBorder(), margins));
				add(cell, Utils.gbConstraints(colIdx, rowCount, 1, 1, GridBagConstraints.BOTH,
						column.prefWidthValid ? 0 : 1, 0));
			}
			rowCount++;
		}

		static class Column {

			private int prefWidth;
			private boolean prefWidthValid;
			private int horizontalAlignment = SwingConstants.LEADING;

			private Column() {
			}

			void setPrefWidth(int prefWidth) {
				this.prefWidth = prefWidth;
				prefWidthValid = true;
			}

			void setHorizontalAlignment(int alignment) {
				horizontalAlignment = alignment;
			}

		}

	}

}
