/**
 * 
 */
package de.stationadmin.gui.tasks.status;

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
public class MessageStatusRenderer extends DefaultTableCellRenderer {
  private ImageIcon iconOk, iconError;
  
  public MessageStatusRenderer() {
    iconOk = AppUtils.getIcon("ok.png");
    iconError = AppUtils.getIcon("failed.png");
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    if (value instanceof Boolean) {
      boolean error = (Boolean) value;
      setText(null);
      setHorizontalAlignment(JLabel.CENTER);
      if(error) {
        setIcon(iconError);
      }
      else {
        setIcon(iconOk);
        
      }
    }
    return comp;
  }

}
