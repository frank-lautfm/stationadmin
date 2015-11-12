/**
 * 
 */
package de.stationadmin.gui.tasks;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import de.stationadmin.gui.util.AppUtils;

/**
 * @author korf
 * 
 */
public class ScheduledTaskStatusRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = 1479692239286273326L;
  private ImageIcon iconOk, iconError;
  
  public ScheduledTaskStatusRenderer() {
    iconOk = AppUtils.getIcon("ok.png");
    iconError = AppUtils.getIcon("failed.png");
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    if (value instanceof Boolean) {
      boolean ok = (Boolean) value;
      setText(null);
      setHorizontalAlignment(JLabel.CENTER);
      if(ok) {
        setIcon(iconOk);
      }
      else {
        setIcon(iconError);
        
      }
    }
    return comp;
  }

}
