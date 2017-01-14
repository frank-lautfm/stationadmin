/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Frank Korf
 *
 */
public class DateTableCellRenderer extends DefaultTableCellRenderer {
  private SimpleDateFormat format;
  private int align = JLabel.RIGHT;
  
  public DateTableCellRenderer(SimpleDateFormat format) {
    super();
    this.format = format;
  }
  
  public DateTableCellRenderer(SimpleDateFormat format, int align) {
    super();
    this.format = format;
    this.align = align;
  }


  /**
   * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
   */
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    this.setHorizontalAlignment(align);
    if(value instanceof Date) {
      this.setText(this.format.format((Date)value));
    }
    else {
      this.setText(null);
    }
    return this;
  }


}
