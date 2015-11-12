/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Extension of {@link DefaultTableCellRenderer} that lets you control
 * the alignment of the text.
 * 
 * @author Frank
 */
public class AlignedTableCellRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = -5982812198473302120L;
  private int alignment = JLabel.LEFT;
  
  /**
   * @param alignment alignment
   * @see JLabel#setHorizontalAlignment(int)
   */
  public AlignedTableCellRenderer(int alignment) {
    super();
    this.alignment = alignment;
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    this.setHorizontalAlignment(this.alignment);
    return this;
  }

}
