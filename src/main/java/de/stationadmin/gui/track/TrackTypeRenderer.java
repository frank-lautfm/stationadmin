/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import de.stationadmin.gui.util.AppUtils;

/**
 * 
 * @author Frank Korf
 * 
 */
public class TrackTypeRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = 2521171071653797799L;
  private ImageIcon[] icons;

  public TrackTypeRenderer() {
    this.icons = new ImageIcon[] {
        null,
        AppUtils.getIcon("music.png"),
        AppUtils.getIcon("jingle.png"),
        AppUtils.getIcon("text.png"),
        AppUtils.getIcon("text.png"),
    };
  }

  /**
   * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
   *      java.lang.Object, boolean, boolean, int, int)
   */
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    if(value instanceof Integer) {
      int type = (Integer)value;
      setText(null);
      if(type >= 0) {
        setHorizontalAlignment(JLabel.CENTER);
        setIcon(this.icons[type]);
      }
    }
    return comp;
  }

  /**
   * @return the icons
   */
  public ImageIcon[] getIcons() {
    return icons;
  }

  /**
   * @param icons the icons to set
   */
  public void setIcons(ImageIcon[] icons) {
    this.icons = icons;
  }


}
