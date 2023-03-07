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

	static class Button extends JButton {
		private static final Color ButtonColor = new Color(156, 156, 156);
		private static final long serialVersionUID = 1L;

		Button(String label, ActionListener action) {
			super(label);
			addActionListener(action);
			setBackground(ButtonColor);
			setOpaque(true);
			setFocusable(false);
		}
	}

	static class ButtonColumn extends ColumnWithMargins {

		private Dimension buttonSize;

		private static final Color BackgroundColor = new Color(35, 35, 35);
		private static final long serialVersionUID = 1L;

		ButtonColumn() {
			super(3);
			setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
			setBackground(BackgroundColor);
			buttonSize = new Dimension(200, 30);
		}

		void setButtonSize(Dimension size) {
			buttonSize = Objects.requireNonNull(size);
		}

		JButton addButton(String label, ActionListener action) {
			JButton button = new Button(label, action);
			button.setPreferredSize(new Dimension(buttonSize));
			return addComp(button);
		}

	}

	static class CheckboxColumn extends ColumnWithMargins {

		private static final Color BackgroundColor = new Color(35, 35, 35);
		private static final Color CheckboxBackgroundColor = new Color(80, 80, 80);
		private static final Color CheckboxTextColor = new Color(245, 245, 245);
		private static final long serialVersionUID = 1L;

		CheckboxColumn() {
			super(3);
			setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
			setBackground(BackgroundColor);
		}

		JCheckBox addCheckbox(String text, boolean selected, Consumer<Boolean> onSelectedChange) {
			JCheckBox checkbox = new JCheckBox(text, selected);
			checkbox.addActionListener(e -> onSelectedChange.accept(Boolean.valueOf(checkbox.isSelected())));
			checkbox.setPreferredSize(new Dimension(200, 30));
			checkbox.setBackground(CheckboxBackgroundColor);
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
		final int margin;
		private static final long serialVersionUID = 1L;

		ColumnWithMargins() {
			this(6);
		}

		ColumnWithMargins(int margin) {
			super(new GridBagLayout());
			this.margin = margin;
			setBackground(new Color(64, 62, 64));
		}

		<Comp extends JComponent> Comp addComp(Comp comp) {
			return addComp(comp, 0);
		}

		<Comp extends JComponent> Comp addComp(Comp comp, int weighty) {
			final int y = compCount++;
			GridBagConstraints c = Utils.gbConstraints(0, y, 1, 1, GridBagConstraints.BOTH, 1, weighty);
			c.insets = new Insets(y == 0 ? 0 : margin, 0, 0, 0);
			add(comp, c);
			return comp;
		}
	}

	static class RowWithMargins extends JPanel {

		private int compCount;
		final int margin;
		private static final long serialVersionUID = 1L;

		RowWithMargins() {
			this(6);
		}

		RowWithMargins(int margin) {
			super(new GridBagLayout());
			this.margin = margin;
			setBackground(new Color(64, 62, 64));
		}

		<Comp extends JComponent> Comp addComp(Comp comp) {
			return addComp(comp, 0);
		}

		<Comp extends JComponent> Comp addComp(Comp comp, int weightx) {
			final int x = compCount++;
			GridBagConstraints c = Utils.gbConstraints(x, 0, 1, 1, GridBagConstraints.BOTH, weightx, 1);
			c.insets = new Insets(0, x == 0 ? 0 : margin, 0, 0);
			add(comp, c);
			return comp;
		}
	}

	static class Window extends JPanel {

		private static final Color BackgroundColor = new Color(64, 62, 64);
		private static final long serialVersionUID = 1L;

		Window() {
			this(3);
		}

		Window(int margin) {
			Border border = BorderFactory.createRaisedBevelBorder();
			border = BorderFactory.createCompoundBorder(border,
					BorderFactory.createEmptyBorder(margin, margin, margin, margin));
			setBorder(border);
			setBackground(BackgroundColor);
		}

	}

	static class Title extends JLabel {
		private static final long serialVersionUID = 1L;

		Title(String text) {
			super(text);
			setForeground(TitleColor);
			setBackground(Color.red);
			setOpaque(false);
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
