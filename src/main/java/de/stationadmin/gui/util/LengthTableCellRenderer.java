/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import de.stationadmin.base.util.TimeFormat;

/**
 * @author korf
 *
 */
public class LengthTableCellRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = -2324399392561620937L;
  private boolean withHours = false;
  
  public LengthTableCellRenderer(boolean withHours) {
    super();
    this.withHours = withHours;
  }


  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    Component cmp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    this.setText(TimeFormat.format((Integer)value, withHours));
    return cmp;
  }
  
  

}
