/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * 
 * @author Frank Korf
 * 
 */
public class IntTableCellRenderer extends DefaultTableCellRenderer {
  private Integer nullValue;
  private String suffix;

  public IntTableCellRenderer(Integer nullValue) {
    super();
    this.nullValue = nullValue;
  }

  public IntTableCellRenderer(Integer nullValue, String suffix) {
    this(nullValue);
    this.suffix = suffix;
  }

  /**
   * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
   *      java.lang.Object, boolean, boolean, int, int)
   */
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    String text = null;
    if (value instanceof Integer && (this.nullValue == null || !this.nullValue.equals(value))) {
      text = Integer.toString((Integer) value);
      if (this.suffix != null) {
        text += this.suffix;
      }
    }
    setHorizontalAlignment(JLabel.RIGHT);
    setText(text);
    return this;
  }

}
