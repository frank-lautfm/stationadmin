/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.Component;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import de.stationadmin.gui.TextProvider;

/**
 * @author Frank
 *
 */
public class TagSelectionListCellRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = -86251475999681027L;
  
  private TextProvider textProvider;

  /**
   * @param textProvider
   */
  public TagSelectionListCellRenderer(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
  }


  /**
   * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList,
   *      java.lang.Object, int, boolean, boolean)
   */
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
      boolean cellHasFocus) {
    Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (value == null) {
      setText(textProvider.getString("titlelist.all"));
    } else if (value.equals(RegisteredTracksTableModel.USED_TITLES)) {
      setText(textProvider.getString("titlelist.used"));
    } else if (value.equals(RegisteredTracksTableModel.UNUSED_TITLES)) {
      setText(textProvider.getString("titlelist.unused"));
    } else if (value.equals(RegisteredTracksTableModel.TAGGED_TITLES)) {
      setText(textProvider.getString("titlelist.tagged"));
    } else if (value instanceof List) {
      setText(textProvider.getString("titlelist.custom"));
    }
    return comp;
  }

}
