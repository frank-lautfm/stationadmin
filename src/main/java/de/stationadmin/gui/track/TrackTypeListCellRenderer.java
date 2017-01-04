/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 *
 */
public class TrackTypeListCellRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = -1639206079106636560L;
  private TextProvider textProvider;
  
  /**
   * @param textProvider
   */
  public TrackTypeListCellRenderer(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
  }




  
  @Override
  public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (value != null) {
      setText(textProvider.getString("trackviewer.property.type." + value));
    } 
    return comp;
  }

}
